package application.platform.views.users;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;

import application.platform.common.BusinessException;
import application.platform.organization.OrganizationService;
import application.platform.organization.OrganizationSummary;
import application.platform.role.RoleService;
import application.platform.role.RoleSummary;
import application.platform.user.UserForm;
import application.platform.user.UserService;
import application.platform.user.UserSummary;
import application.platform.views.MainLayout;
import application.platform.views.UrlIds;
import application.platform.views.ViewNotifications;

import jakarta.annotation.security.PermitAll;

@Route(value = "users/add", layout = MainLayout.class)
@RouteAlias(value = "users/edit", layout = MainLayout.class)
@PageTitle("Add User")
@PermitAll
public class AddUserView extends VerticalLayout implements HasUrlParameter<String> {

    private static final String ACTIVE = "Active";

    private final UserService userService;

    private final H2 title = new H2("Add User");
    private final Span subtitle = new Span("Create a new user account");
    private final TextField username = new TextField("Username");
    private final PasswordField password = new PasswordField("Password");
    private final TextField fullName = new TextField("Full Name");
    private final EmailField email = new EmailField("Email");
    private final TextField phone = new TextField("Phone");
    private final ComboBox<String> status = new ComboBox<>("Status");
    private final ComboBox<OrganizationSummary> organization = new ComboBox<>("Organization");
    private final ComboBox<UserSummary> reportsTo = new ComboBox<>("Reports To");
    private final MultiSelectComboBox<RoleSummary> roles = new MultiSelectComboBox<>("Roles");

    private final List<OrganizationSummary> organizations;
    private final List<UserSummary> users;
    private final List<RoleSummary> allRoles;

    /** The user being edited, or {@code null} when adding a new one. */
    private UserSummary editing;

    public AddUserView(UserService userService, OrganizationService organizationService,
            RoleService roleService) {
        this.userService = userService;
        this.organizations = organizationService.findAll();
        this.users = userService.findAll();
        this.allRoles = roleService.findAll();

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        subtitle.getStyle().set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout header = new VerticalLayout(title, subtitle);
        header.setPadding(false);
        header.setSpacing(false);

        // Section 1 - Account Information
        username.setRequiredIndicatorVisible(true);
        password.setRequiredIndicatorVisible(true);
        fullName.setRequiredIndicatorVisible(true);

        status.setItems(ACTIVE, "Inactive");
        status.setValue(ACTIVE);

        FlexLayout accountSection = twoColumnSection(username, password, fullName, email, phone, status);

        // Section 2 - Organization & Hierarchy
        organization.setItems(organizations.stream().filter(OrganizationSummary::active).toList());
        organization.setItemLabelGenerator(OrganizationSummary::name);
        organization.setRequiredIndicatorVisible(true);

        reportsTo.setItems(users.stream().filter(UserSummary::active).toList());
        reportsTo.setItemLabelGenerator(user -> user.fullName() + " (" + user.username() + ")");
        reportsTo.setClearButtonVisible(true);
        reportsTo.setPlaceholder("None");

        FlexLayout organizationSection = twoColumnSection(organization, reportsTo);

        // Section 3 - Role & Access
        roles.setItems(allRoles.stream().filter(RoleSummary::active).toList());
        roles.setItemLabelGenerator(RoleSummary::name);

        FlexLayout roleSection = twoColumnSection(roles);

        Button cancel = new Button("Cancel", event -> getUI().ifPresent(ui -> ui.navigate("users")));

        Button save = new Button("Save User", event -> save());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout actions = new HorizontalLayout(cancel, save);
        actions.setWidthFull();
        actions.setJustifyContentMode(HorizontalLayout.JustifyContentMode.END);

        VerticalLayout form = new VerticalLayout(
                header,
                sectionHeading("Account Information"),
                accountSection,
                sectionHeading("Organization & Hierarchy"),
                organizationSection,
                sectionHeading("Role & Access"),
                roleSection,
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
        editing = UrlIds.parse(parameter).flatMap(userService::findById).orElse(null);
        if (editing == null) {
            ViewNotifications.showError("User not found");
            event.forwardTo("users");
            return;
        }

        title.setText("Edit User");
        subtitle.setText("Update user account " + editing.username());
        password.setRequiredIndicatorVisible(false);
        password.setHelperText("Leave blank to keep the current password");

        username.setValue(editing.username());
        fullName.setValue(editing.fullName());
        email.setValue(editing.email() == null ? "" : editing.email());
        phone.setValue(editing.phone() == null ? "" : editing.phone());
        status.setValue(editing.active() ? ACTIVE : "Inactive");

        // An inactive organization or role still has to be shown if the user already has it.
        List<OrganizationSummary> organizationChoices = organizations.stream()
                .filter(org -> org.active() || org.id().equals(editing.organizationId()))
                .toList();
        organization.setItems(organizationChoices);
        organizationChoices.stream().filter(org -> org.id().equals(editing.organizationId())).findFirst()
                .ifPresent(organization::setValue);

        List<UserSummary> supervisorChoices = users.stream()
                .filter(user -> !user.id().equals(editing.id()))
                .filter(user -> user.active() || user.id().equals(editing.supervisorId()))
                .toList();
        reportsTo.setItems(supervisorChoices);
        supervisorChoices.stream().filter(user -> user.id().equals(editing.supervisorId())).findFirst()
                .ifPresent(reportsTo::setValue);

        List<RoleSummary> roleChoices = allRoles.stream()
                .filter(role -> role.active() || editing.roleIds().contains(role.id()))
                .toList();
        roles.setItems(roleChoices);
        roles.setValue(roleChoices.stream().filter(role -> editing.roleIds().contains(role.id()))
                .collect(Collectors.toSet()));
    }

    private void save() {
        OrganizationSummary selectedOrganization = organization.getValue();
        UserSummary supervisor = reportsTo.getValue();
        Set<UUID> roleIds = roles.getValue().stream().map(RoleSummary::id)
                .collect(Collectors.toCollection(HashSet::new));
        UserForm form = new UserForm(
                editing == null ? null : editing.id(),
                editing == null ? null : editing.version(),
                username.getValue(),
                password.getValue(),
                fullName.getValue(),
                email.getValue(),
                phone.getValue(),
                selectedOrganization == null ? null : selectedOrganization.id(),
                supervisor == null ? null : supervisor.id(),
                roleIds,
                ACTIVE.equals(status.getValue()));
        try {
            UserSummary saved = userService.save(form);
            ViewNotifications.showSuccess("User \"" + saved.username() + "\" saved");
            getUI().ifPresent(ui -> ui.navigate("users"));
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
