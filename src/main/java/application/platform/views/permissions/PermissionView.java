package application.platform.views.permissions;

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
 * Visual prototype of the Permission Management page. Uses static mock
 * data only - Add/Edit/Deactivate are UI affordances that do not persist
 * anything. Defines available permissions only; role-to-permission
 * assignment is out of scope here and belongs to the Authorization module.
 */
@Route(value = "permissions", layout = MainLayout.class)
@PageTitle("Permission Management")
@PermitAll
public class PermissionView extends VerticalLayout {

    private static final String FILTER_ALL = "All";

    public PermissionView() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Permission Management");
        Span subtitle = new Span("Manage system permissions");
        subtitle.getStyle().set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout header = new VerticalLayout(title, subtitle);
        header.setPadding(false);
        header.setSpacing(false);

        TextField search = new TextField();
        search.setPlaceholder("Search permission...");
        search.setClearButtonVisible(true);
        search.setPrefixComponent(VaadinIcon.SEARCH.create());

        ComboBox<String> moduleFilter = new ComboBox<>();
        moduleFilter.setItems(FILTER_ALL, "Users", "Roles", "Organization", "Approval", "Notification", "Audit",
                "Finance", "Stock", "Invoice", "Report");
        moduleFilter.setValue(FILTER_ALL);
        moduleFilter.setPlaceholder("Module");

        ComboBox<String> actionFilter = new ComboBox<>();
        actionFilter.setItems(FILTER_ALL, "View", "Create", "Update", "Delete", "Approve", "Export");
        actionFilter.setValue(FILTER_ALL);
        actionFilter.setPlaceholder("Action");

        ComboBox<String> statusFilter = new ComboBox<>();
        statusFilter.setItems(FILTER_ALL, "Active", "Inactive");
        statusFilter.setValue(FILTER_ALL);
        statusFilter.setPlaceholder("Status");

        Button addPermission = new Button("Add Permission", VaadinIcon.PLUS.create(),
                event -> getUI().ifPresent(ui -> ui.navigate("permissions/add")));

        HorizontalLayout toolbar = new HorizontalLayout(search, moduleFilter, actionFilter, statusFilter,
                addPermission);
        toolbar.setWidthFull();
        toolbar.setAlignItems(HorizontalLayout.Alignment.CENTER);
        toolbar.setFlexGrow(1, search);

        Grid<MockPermission> grid = new Grid<>();
        grid.setItems(MockPermissionData.ALL);
        grid.addColumn(MockPermission::code).setHeader("Permission Code");
        grid.addColumn(MockPermission::module).setHeader("Module");
        grid.addColumn(MockPermission::action).setHeader("Action");
        grid.addComponentColumn(permission -> statusLabel(permission.status())).setHeader("Status");
        grid.addColumn(MockPermission::description).setHeader("Description");
        grid.addComponentColumn(this::actionMenu).setHeader("Action");
        grid.setSizeFull();

        search.addValueChangeListener(event -> grid.setItems(
                filter(event.getValue(), moduleFilter.getValue(), actionFilter.getValue(), statusFilter.getValue())));
        moduleFilter.addValueChangeListener(event -> grid.setItems(
                filter(search.getValue(), event.getValue(), actionFilter.getValue(), statusFilter.getValue())));
        actionFilter.addValueChangeListener(event -> grid.setItems(
                filter(search.getValue(), moduleFilter.getValue(), event.getValue(), statusFilter.getValue())));
        statusFilter.addValueChangeListener(event -> grid.setItems(
                filter(search.getValue(), moduleFilter.getValue(), actionFilter.getValue(), event.getValue())));

        add(header, toolbar, grid);
        setFlexGrow(1, grid);
    }

    private List<MockPermission> filter(String term, String module, String action, String status) {
        String needle = term == null ? "" : term.trim().toLowerCase();
        return MockPermissionData.ALL.stream()
                .filter(permission -> needle.isEmpty() || permission.code().toLowerCase().contains(needle)
                        || permission.description().toLowerCase().contains(needle))
                .filter(permission -> module == null || FILTER_ALL.equals(module)
                        || permission.module().equals(module))
                .filter(permission -> action == null || FILTER_ALL.equals(action)
                        || permission.action().equals(action))
                .filter(permission -> status == null || FILTER_ALL.equals(status)
                        || permission.status().equals(status))
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

    private Component actionMenu(MockPermission permission) {
        MenuBar menuBar = new MenuBar();
        menuBar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);
        MenuItem actions = menuBar.addItem(VaadinIcon.ELLIPSIS_DOTS_V.create());
        SubMenu subMenu = actions.getSubMenu();
        subMenu.addItem("Edit", event -> showPrototypeNotice("Editing permissions"));
        subMenu.addItem("Deactivate", event -> showPrototypeNotice("Deactivating permissions"));
        return menuBar;
    }

    private void showPrototypeNotice(String action) {
        Notification.show(action + " is a UI prototype only - no data is persisted.", 3000,
                Notification.Position.BOTTOM_START);
    }
}
