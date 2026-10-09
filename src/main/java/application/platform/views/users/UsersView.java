package application.platform.views.users;

import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import application.platform.views.MainLayout;

import jakarta.annotation.security.PermitAll;

/**
 * Visual prototype of the Users management page (MVP-02.5). Uses static mock
 * data only - Add/Edit/Delete are UI affordances that do not persist
 * anything.
 */
@Route(value = "users", layout = MainLayout.class)
@PageTitle("Users")
@PermitAll
public class UsersView extends VerticalLayout {

    public UsersView() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Users");

        Button addUser = new Button("Add User", VaadinIcon.PLUS.create(),
                event -> getUI().ifPresent(ui -> ui.navigate("users/add")));

        TextField search = new TextField();
        search.setPlaceholder("Search");
        search.setClearButtonVisible(true);
        search.setPrefixComponent(VaadinIcon.SEARCH.create());

        HorizontalLayout toolbar = new HorizontalLayout(addUser, search);
        toolbar.setWidthFull();
        toolbar.setAlignItems(HorizontalLayout.Alignment.CENTER);

        Grid<MockUser> grid = new Grid<>();
        grid.setItems(MockUserData.ALL);
        grid.addColumn(MockUser::username).setHeader("Username");
        grid.addColumn(MockUser::name).setHeader("Name");
        grid.addColumn(MockUser::organization).setHeader("Organization");
        grid.addColumn(MockUser::supervisor).setHeader("Supervisor");
        grid.addComponentColumn(user -> statusLabel(user.status())).setHeader("Status");
        grid.addComponentColumn(this::actionButtons).setHeader("Action");
        grid.setSizeFull();

        search.addValueChangeListener(event -> grid.setItems(filter(event.getValue())));

        add(title, toolbar, grid);
        setFlexGrow(1, grid);
    }

    private List<MockUser> filter(String term) {
        if (term == null || term.isBlank()) {
            return MockUserData.ALL;
        }
        String needle = term.trim().toLowerCase();
        return MockUserData.ALL.stream()
                .filter(user -> user.username().toLowerCase().contains(needle)
                        || user.name().toLowerCase().contains(needle))
                .toList();
    }

    private Component statusLabel(String status) {
        Span label = new Span(status);
        boolean active = "Active".equals(status);
        label.getStyle()
                .set("padding", "0.15em 0.65em")
                .set("border-radius", "1em")
                .set("font-size", "0.85em")
                .set("font-weight", "600")
                .set("background", active ? "#e3f8ec" : "#eeeeee")
                .set("color", active ? "#1a7f4b" : "#555555");
        return label;
    }

    private Component actionButtons(MockUser user) {
        Button edit = new Button("Edit", event -> showPrototypeNotice("Editing users"));
        Button delete = new Button("Delete", event -> showPrototypeNotice("Deleting users"));
        return new HorizontalLayout(edit, delete);
    }

    private void showPrototypeNotice(String action) {
        Notification.show(action + " is a UI prototype only - no data is persisted.", 3000,
                Notification.Position.BOTTOM_START);
    }
}
