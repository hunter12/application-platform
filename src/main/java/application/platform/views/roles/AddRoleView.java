package application.platform.views.roles;

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
import application.platform.role.RoleForm;
import application.platform.role.RoleService;
import application.platform.role.RoleSummary;
import application.platform.views.MainLayout;
import application.platform.views.UrlIds;
import application.platform.views.ViewNotifications;

import jakarta.annotation.security.PermitAll;

@Route(value = "roles/add", layout = MainLayout.class)
@RouteAlias(value = "roles/edit", layout = MainLayout.class)
@PageTitle("Add Role")
@PermitAll
public class AddRoleView extends VerticalLayout implements HasUrlParameter<String> {

    private static final String ACTIVE = "Active";

    private final RoleService roleService;

    private final H2 title = new H2("Add Role");
    private final Span subtitle = new Span("Create a new role");
    private final TextField roleName = new TextField("Role Name");
    private final TextField roleCode = new TextField("Role Code");
    private final ComboBox<String> status = new ComboBox<>("Status");
    private final TextArea description = new TextArea("Description");

    /** The role being edited, or {@code null} when adding a new one. */
    private RoleSummary editing;

    public AddRoleView(RoleService roleService) {
        this.roleService = roleService;
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        subtitle.getStyle().set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout header = new VerticalLayout(title, subtitle);
        header.setPadding(false);
        header.setSpacing(false);

        roleName.setRequiredIndicatorVisible(true);
        roleCode.setRequiredIndicatorVisible(true);
        roleCode.setHelperText("Technical key, e.g. BRANCH_MANAGER. Cannot be changed later.");

        status.setItems(ACTIVE, "Inactive");
        status.setValue(ACTIVE);

        FlexLayout fields = twoColumnSection(roleName, roleCode, status, description);

        Button cancel = new Button("Cancel", event -> getUI().ifPresent(ui -> ui.navigate("roles")));

        Button save = new Button("Save Role", event -> save());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout actions = new HorizontalLayout(cancel, save);
        actions.setWidthFull();
        actions.setJustifyContentMode(HorizontalLayout.JustifyContentMode.END);

        VerticalLayout form = new VerticalLayout(header, sectionHeading("Role Details"), fields, actions);
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
        editing = UrlIds.parse(parameter).flatMap(roleService::findById).orElse(null);
        if (editing == null) {
            ViewNotifications.showError("Role not found");
            event.forwardTo("roles");
            return;
        }

        title.setText("Edit Role");
        subtitle.setText("Update role " + editing.name());
        roleName.setValue(editing.name());
        roleCode.setValue(editing.code());
        roleCode.setReadOnly(true);
        status.setValue(editing.active() ? ACTIVE : "Inactive");
        description.setValue(editing.description() == null ? "" : editing.description());
    }

    private void save() {
        RoleForm form = new RoleForm(
                editing == null ? null : editing.id(),
                editing == null ? null : editing.version(),
                roleCode.getValue(),
                roleName.getValue(),
                description.getValue(),
                ACTIVE.equals(status.getValue()));
        try {
            RoleSummary saved = roleService.save(form);
            ViewNotifications.showSuccess("Role \"" + saved.name() + "\" saved");
            getUI().ifPresent(ui -> ui.navigate("roles"));
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
