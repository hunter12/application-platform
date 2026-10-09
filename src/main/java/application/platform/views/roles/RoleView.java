package application.platform.views.roles;

import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import application.platform.views.MainLayout;

import jakarta.annotation.security.PermitAll;

/**
 * Visual prototype of the Role Management page. Uses static mock data
 * only - Add/Edit/View Access/Deactivate are UI affordances that do not
 * persist anything.
 */
@Route(value = "roles", layout = MainLayout.class)
@PageTitle("Role Management")
@PermitAll
public class RoleView extends VerticalLayout {

    private static final String STATUS_ALL = "All";

    public RoleView() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Role Management");
        Span subtitle = new Span("Manage user roles and their access level");
        subtitle.getStyle().set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout header = new VerticalLayout(title, subtitle);
        header.setPadding(false);
        header.setSpacing(false);

        TextField search = new TextField();
        search.setPlaceholder("Search role...");
        search.setClearButtonVisible(true);
        search.setPrefixComponent(VaadinIcon.SEARCH.create());

        ComboBox<String> statusFilter = new ComboBox<>();
        statusFilter.setItems(STATUS_ALL, "Active", "Inactive");
        statusFilter.setValue(STATUS_ALL);
        statusFilter.setPlaceholder("Status");

        Button addRole = new Button("Add Role", VaadinIcon.PLUS.create(),
                event -> getUI().ifPresent(ui -> ui.navigate("roles/add")));

        HorizontalLayout toolbar = new HorizontalLayout(search, statusFilter, addRole);
        toolbar.setWidthFull();
        toolbar.setAlignItems(HorizontalLayout.Alignment.CENTER);
        toolbar.setFlexGrow(1, search);

        Grid<MockRole> grid = new Grid<>();
        grid.setItems(MockRoleData.ALL);
        grid.addColumn(MockRole::name).setHeader("Role Name");
        grid.addColumn(MockRole::description).setHeader("Description");
        grid.addColumn(MockRole::userCount).setHeader("Users");
        grid.addComponentColumn(role -> statusLabel(role.status())).setHeader("Status");
        grid.addComponentColumn(this::actionMenu).setHeader("Action");
        grid.setSizeFull();

        search.addValueChangeListener(event -> grid.setItems(filter(event.getValue(), statusFilter.getValue())));
        statusFilter.addValueChangeListener(event -> grid.setItems(filter(search.getValue(), event.getValue())));

        add(header, toolbar, grid);
        setFlexGrow(1, grid);
    }

    private List<MockRole> filter(String term, String status) {
        String needle = term == null ? "" : term.trim().toLowerCase();
        return MockRoleData.ALL.stream()
                .filter(role -> needle.isEmpty() || role.name().toLowerCase().contains(needle)
                        || role.description().toLowerCase().contains(needle))
                .filter(role -> status == null || STATUS_ALL.equals(status) || role.status().equals(status))
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

    private Component actionMenu(MockRole role) {
        MenuBar menuBar = new MenuBar();
        menuBar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);
        MenuItem actions = menuBar.addItem(VaadinIcon.ELLIPSIS_DOTS_V.create());
        SubMenu subMenu = actions.getSubMenu();
        subMenu.addItem("Edit", event -> showPrototypeNotice("Editing roles"));
        subMenu.addItem("View Access", event -> showPrototypeNotice("Viewing role access"));
        subMenu.addItem("Deactivate", event -> showPrototypeNotice("Deactivating roles"));
        return menuBar;
    }

    private void showPrototypeNotice(String action) {
        Notification.show(action + " is a UI prototype only - no data is persisted.", 3000,
                Notification.Position.BOTTOM_START);
    }
}
