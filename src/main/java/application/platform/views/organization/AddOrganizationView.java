package application.platform.views.organization;

import java.util.List;

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
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;

import application.platform.common.BusinessException;
import application.platform.organization.OrganizationForm;
import application.platform.organization.OrganizationService;
import application.platform.organization.OrganizationSummary;
import application.platform.organization.OrganizationType;
import application.platform.views.MainLayout;
import application.platform.views.UrlIds;
import application.platform.views.ViewNotifications;

import jakarta.annotation.security.PermitAll;

@Route(value = "organizations/add", layout = MainLayout.class)
@RouteAlias(value = "organizations/edit", layout = MainLayout.class)
@PageTitle("Add Organization")
@PermitAll
public class AddOrganizationView extends VerticalLayout implements HasUrlParameter<String> {

    private static final String ACTIVE = "Active";

    private final OrganizationService organizationService;
    private final List<OrganizationSummary> organizations;

    private final H2 title = new H2("Add Organization");
    private final Span subtitle = new Span("Create a new organizational unit");
    private final TextField name = new TextField("Organization Name");
    private final TextField code = new TextField("Organization Code");
    private final ComboBox<OrganizationType> type = new ComboBox<>("Organization Type");
    private final ComboBox<OrganizationSummary> parent = new ComboBox<>("Parent Organization");
    private final ComboBox<String> status = new ComboBox<>("Status");

    /** The organization being edited, or {@code null} when adding a new one. */
    private OrganizationSummary editing;

    public AddOrganizationView(OrganizationService organizationService) {
        this.organizationService = organizationService;
        this.organizations = organizationService.findAll();
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        subtitle.getStyle().set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout header = new VerticalLayout(title, subtitle);
        header.setPadding(false);
        header.setSpacing(false);

        name.setRequiredIndicatorVisible(true);
        code.setRequiredIndicatorVisible(true);

        type.setItems(OrganizationType.values());
        type.setItemLabelGenerator(OrganizationType::getLabel);
        type.setRequiredIndicatorVisible(true);

        parent.setItems(organizations.stream().filter(OrganizationSummary::active).toList());
        parent.setItemLabelGenerator(org -> org.name() + " (" + org.code() + ")");
        parent.setClearButtonVisible(true);
        parent.setPlaceholder("None");

        status.setItems(ACTIVE, "Inactive");
        status.setValue(ACTIVE);

        FlexLayout fields = twoColumnSection(name, code, type, parent, status);

        Button cancel = new Button("Cancel", event -> getUI().ifPresent(ui -> ui.navigate("organizations")));

        Button save = new Button("Save Organization", event -> save());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout actions = new HorizontalLayout(cancel, save);
        actions.setWidthFull();
        actions.setJustifyContentMode(HorizontalLayout.JustifyContentMode.END);

        VerticalLayout form = new VerticalLayout(header, sectionHeading("Organization Information"), fields,
                actions);
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
        editing = UrlIds.parse(parameter).flatMap(organizationService::findById).orElse(null);
        if (editing == null) {
            ViewNotifications.showError("Organization not found");
            event.forwardTo("organizations");
            return;
        }

        title.setText("Edit Organization");
        subtitle.setText("Update organizational unit " + editing.name());
        name.setValue(editing.name());
        code.setValue(editing.code());
        type.setValue(editing.type());
        status.setValue(editing.active() ? ACTIVE : "Inactive");

        // A unit cannot be its own parent; the service also rejects moving it under its own sub-units.
        List<OrganizationSummary> parentChoices = organizations.stream()
                .filter(org -> !org.id().equals(editing.id()))
                .filter(org -> org.active() || org.id().equals(editing.parentId()))
                .toList();
        parent.setItems(parentChoices);
        parentChoices.stream().filter(org -> org.id().equals(editing.parentId())).findFirst()
                .ifPresent(parent::setValue);
    }

    private void save() {
        OrganizationSummary selectedParent = parent.getValue();
        OrganizationForm form = new OrganizationForm(
                editing == null ? null : editing.id(),
                editing == null ? null : editing.version(),
                code.getValue(),
                name.getValue(),
                type.getValue(),
                selectedParent == null ? null : selectedParent.id(),
                ACTIVE.equals(status.getValue()));
        try {
            OrganizationSummary saved = organizationService.save(form);
            ViewNotifications.showSuccess("Organization \"" + saved.name() + "\" saved");
            getUI().ifPresent(ui -> ui.navigate("organizations"));
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
