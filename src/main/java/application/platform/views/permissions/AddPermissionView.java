package application.platform.views.permissions;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;

import application.platform.common.BusinessException;
import application.platform.permission.PermissionForm;
import application.platform.permission.PermissionService;
import application.platform.permission.PermissionSummary;
import application.platform.views.MainLayout;
import application.platform.views.UrlIds;
import application.platform.views.ViewNotifications;

import jakarta.annotation.security.PermitAll;

@Route(value = "permissions/add", layout = MainLayout.class)
@RouteAlias(value = "permissions/edit", layout = MainLayout.class)
@PageTitle("Add Permission")
@PermitAll
public class AddPermissionView extends VerticalLayout implements HasUrlParameter<String> {

    private static final String ACTIVE = "Active";

    private final PermissionService permissionService;

    private final H2 title = new H2("Add Permission");
    private final Span subtitle = new Span("Create a new system permission");
    private final TextField code = new TextField("Permission Code");
    private final TextField name = new TextField("Permission Name");
    private final ComboBox<String> module = new ComboBox<>("Module");
    private final ComboBox<String> action = new ComboBox<>("Action");
    private final ComboBox<String> status = new ComboBox<>("Status");
    private final TextArea description = new TextArea("Description");

    /** The permission being edited, or {@code null} when adding a new one. */
    private PermissionSummary editing;

    public AddPermissionView(PermissionService permissionService) {
        this.permissionService = permissionService;
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        subtitle.getStyle().set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout header = new VerticalLayout(title, subtitle);
        header.setPadding(false);
        header.setSpacing(false);

        code.setRequiredIndicatorVisible(true);
        code.setHelperText("e.g. INVOICE_APPROVE. Cannot be changed later.");
        name.setRequiredIndicatorVisible(true);

        module.setItems(PermissionView.MODULES);
        module.setRequiredIndicatorVisible(true);

        action.setItems(PermissionView.ACTIONS);
        action.setRequiredIndicatorVisible(true);

        status.setItems(ACTIVE, "Inactive");
        status.setValue(ACTIVE);

        FlexLayout fields = twoColumnSection(code, name, module, action, status, description);

        Button cancel = new Button("Cancel", event -> getUI().ifPresent(ui -> ui.navigate("permissions")));

        Button save = new Button("Save Permission", event -> save());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout actions = new HorizontalLayout(cancel, save);
        actions.setWidthFull();
        actions.setJustifyContentMode(HorizontalLayout.JustifyContentMode.END);

        VerticalLayout form = new VerticalLayout(header, sectionHeading("Permission Information"), fields, actions);
        form.setPadding(false);
        form.setSpacing(true);
        form.setMaxWidth("960px");

        add(form);
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
        if (parameter == null) {
            return;
        }
        editing = UrlIds.parse(parameter).flatMap(permissionService::findById).orElse(null);
        if (editing == null) {
            ViewNotifications.showError("Permission not found");
            event.forwardTo("permissions");
            return;
        }

        title.setText("Edit Permission");
        subtitle.setText("Update permission " + editing.code());
        code.setValue(editing.code());
        code.setReadOnly(true);
        name.setValue(editing.name());
        module.setValue(editing.module());
        action.setValue(editing.action());
        status.setValue(editing.active() ? ACTIVE : "Inactive");
        description.setValue(editing.description() == null ? "" : editing.description());
    }

    private void save() {
        PermissionForm form = new PermissionForm(
                editing == null ? null : editing.id(),
                editing == null ? null : editing.version(),
                code.getValue(),
                name.getValue(),
                module.getValue(),
                action.getValue(),
                description.getValue(),
                ACTIVE.equals(status.getValue()));
        try {
            PermissionSummary saved = permissionService.save(form);
            ViewNotifications.showSuccess("Permission " + saved.code() + " saved");
            getUI().ifPresent(ui -> ui.navigate("permissions"));
        } catch (BusinessException e) {
            ViewNotifications.showError(e.getMessage());
        }
    }

    private Component sectionHeading(String text) {
        H3 heading = new H3(text);
        heading.getStyle().set("margin-bottom", "0");

        VerticalLayout wrapper = new VerticalLayout(heading, new Hr());
        wrapper.setPadding(false);
        wrapper.setSpacing(false);
        return wrapper;
    }

    private FlexLayout twoColumnSection(Component... fields) {
        FlexLayout layout = new FlexLayout(fields);
        layout.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        layout.getStyle().set("gap", "1rem");
        for (Component field : fields) {
            field.getElement().getStyle()
                    .set("flex", "1 1 320px")
                    .set("min-width", "280px");
        }
        return layout;
    }
}
