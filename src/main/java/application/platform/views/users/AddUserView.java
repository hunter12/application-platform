package application.platform.views.users;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import application.platform.views.MainLayout;

import jakarta.annotation.security.PermitAll;

/**
 * Visual prototype of the Add User form. Uses static dropdown data only -
 * Save is a UI affordance that does not persist anything.
 */
@Route(value = "users/add", layout = MainLayout.class)
@PageTitle("Add User")
@PermitAll
public class AddUserView extends VerticalLayout {

    public AddUserView() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Add User");
        Span subtitle = new Span("Create a new user account");
        subtitle.getStyle().set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout header = new VerticalLayout(title, subtitle);
        header.setPadding(false);
        header.setSpacing(false);

        // Section 1 - Account Information
        TextField username = new TextField("Username");
        TextField fullName = new TextField("Full Name");
        EmailField email = new EmailField("Email");
        TextField phone = new TextField("Phone");

        ComboBox<String> status = new ComboBox<>("Status");
        status.setItems("Active", "Inactive");
        status.setValue("Active");

        FlexLayout accountSection = twoColumnSection(username, fullName, email, phone, status);

        // Section 2 - Organization & Hierarchy
        ComboBox<String> organization = new ComboBox<>("Organization");
        organization.setItems("Head Office", "Regional Jakarta", "Regional Bandung", "Branch A", "Branch B",
                "Branch C");

        ComboBox<String> reportsTo = new ComboBox<>("Reports To");
        reportsTo.setItems("John Smith", "Jane Doe", "Abu Yazid", "None");

        FlexLayout organizationSection = twoColumnSection(organization, reportsTo);

        // Section 3 - Role & Access
        ComboBox<String> role = new ComboBox<>("Role");
        role.setItems("Super Admin", "Admin", "Director", "Manager", "SPV", "Staff");

        FlexLayout roleSection = twoColumnSection(role);

        Button cancel = new Button("Cancel", event -> getUI().ifPresent(ui -> ui.navigate("users")));

        Button save = new Button("Save User", event -> {
            Notification.show("Saving users is a UI prototype only - no data is persisted.", 3000,
                    Notification.Position.BOTTOM_START);
            getUI().ifPresent(ui -> ui.navigate("users"));
        });
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
