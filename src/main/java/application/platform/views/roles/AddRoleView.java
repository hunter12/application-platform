package application.platform.views.roles;

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
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import application.platform.views.MainLayout;

import jakarta.annotation.security.PermitAll;

/**
 * Visual prototype of the Add Role form. Save is a UI affordance that
 * does not persist anything.
 */
@Route(value = "roles/add", layout = MainLayout.class)
@PageTitle("Add Role")
@PermitAll
public class AddRoleView extends VerticalLayout {

    public AddRoleView() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Add Role");
        Span subtitle = new Span("Create a new role");
        subtitle.getStyle().set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout header = new VerticalLayout(title, subtitle);
        header.setPadding(false);
        header.setSpacing(false);

        TextField roleName = new TextField("Role Name");

        ComboBox<String> status = new ComboBox<>("Status");
        status.setItems("Active", "Inactive");
        status.setValue("Active");

        TextArea description = new TextArea("Description");

        FlexLayout fields = twoColumnSection(roleName, status, description);

        Button cancel = new Button("Cancel", event -> getUI().ifPresent(ui -> ui.navigate("roles")));

        Button save = new Button("Save Role", event -> {
            Notification.show("Saving roles is a UI prototype only - no data is persisted.", 3000,
                    Notification.Position.BOTTOM_START);
            getUI().ifPresent(ui -> ui.navigate("roles"));
        });
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
