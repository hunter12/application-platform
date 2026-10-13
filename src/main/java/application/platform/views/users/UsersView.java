package application.platform.views.users;

import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import application.platform.common.BusinessException;
import application.platform.user.UserService;
import application.platform.user.UserSummary;
import application.platform.views.MainLayout;
import application.platform.views.ViewNotifications;

import jakarta.annotation.security.PermitAll;

@Route(value = "users", layout = MainLayout.class)
@PageTitle("Users")
@PermitAll
public class UsersView extends VerticalLayout {

    private final UserService userService;
    private final Grid<UserSummary> grid = new Grid<>();
    private final TextField search = new TextField();
    private List<UserSummary> users = List.of();

    public UsersView(UserService userService) {
        this.userService = userService;
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Users");

        Button addUser = new Button("Add User", VaadinIcon.PLUS.create(),
                event -> getUI().ifPresent(ui -> ui.navigate("users/add")));

        search.setPlaceholder("Search");
        search.setClearButtonVisible(true);
        search.setPrefixComponent(VaadinIcon.SEARCH.create());

        HorizontalLayout toolbar = new HorizontalLayout(addUser, search);
        toolbar.setWidthFull();
        toolbar.setAlignItems(HorizontalLayout.Alignment.CENTER);

        grid.addColumn(UserSummary::username).setHeader("Username");
        grid.addColumn(UserSummary::fullName).setHeader("Name");
        grid.addColumn(UserSummary::organizationName).setHeader("Organization");
        grid.addColumn(user -> user.supervisorName() == null ? "-" : user.supervisorName()).setHeader("Supervisor");
        grid.addColumn(user -> user.roleNames().isEmpty() ? "-" : String.join(", ", user.roleNames()))
                .setHeader("Roles");
        grid.addComponentColumn(user -> statusLabel(user.active())).setHeader("Status");
        grid.addComponentColumn(this::actionButtons).setHeader("Action");
        grid.setSizeFull();

        search.addValueChangeListener(event -> applyFilter());

        add(title, toolbar, grid);
        setFlexGrow(1, grid);
        refresh();
    }

    private void refresh() {
        users = userService.findAll();
        applyFilter();
    }

    private void applyFilter() {
        String term = search.getValue();
        if (term == null || term.isBlank()) {
            grid.setItems(users);
            return;
        }
        String needle = term.trim().toLowerCase();
        grid.setItems(users.stream()
                .filter(user -> user.username().toLowerCase().contains(needle)
                        || user.fullName().toLowerCase().contains(needle))
                .toList());
    }

    private Component statusLabel(boolean active) {
        Span label = new Span(active ? "Active" : "Inactive");
        label.getStyle()
                .set("padding", "0.15em 0.65em")
                .set("border-radius", "1em")
                .set("font-size", "0.85em")
                .set("font-weight", "600")
                .set("background", active ? "#e3f8ec" : "#eeeeee")
                .set("color", active ? "#1a7f4b" : "#555555");
        return label;
    }

    private Component actionButtons(UserSummary user) {
        Button edit = new Button("Edit",
                event -> getUI().ifPresent(ui -> ui.navigate("users/edit/" + user.id())));
        Button delete = new Button("Delete", event -> confirmDelete(user));
        return new HorizontalLayout(edit, delete);
    }

    private void confirmDelete(UserSummary user) {
        ConfirmDialog dialog = new ConfirmDialog("Delete user",
                "Delete user \"" + user.username() + "\"? They will no longer be able to log in.", "Delete",
                event -> {
                    try {
                        userService.delete(user.id());
                        ViewNotifications.showSuccess("User \"" + user.username() + "\" deleted");
                    } catch (BusinessException e) {
                        ViewNotifications.showError(e.getMessage());
                    }
                    refresh();
                });
        dialog.setCancelable(true);
        dialog.setConfirmButtonTheme("error primary");
        dialog.open();
    }
}
