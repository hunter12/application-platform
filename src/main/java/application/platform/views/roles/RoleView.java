package application.platform.views.roles;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import application.platform.common.BusinessException;
import application.platform.permission.PermissionService;
import application.platform.permission.PermissionSummary;
import application.platform.role.RoleService;
import application.platform.role.RoleSummary;
import application.platform.views.MainLayout;
import application.platform.views.ViewNotifications;

import jakarta.annotation.security.PermitAll;

@Route(value = "roles", layout = MainLayout.class)
@PageTitle("Role Management")
@PermitAll
public class RoleView extends VerticalLayout {

    private static final String STATUS_ALL = "All";

    private final RoleService roleService;
    private final PermissionService permissionService;
    private final Grid<RoleSummary> grid = new Grid<>();
    private final TextField search = new TextField();
    private final ComboBox<String> statusFilter = new ComboBox<>();
    private List<RoleSummary> roles = List.of();

    public RoleView(RoleService roleService, PermissionService permissionService) {
        this.roleService = roleService;
        this.permissionService = permissionService;
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Role Management");
        Span subtitle = new Span("Manage user roles and their access level");
        subtitle.getStyle().set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout header = new VerticalLayout(title, subtitle);
        header.setPadding(false);
        header.setSpacing(false);

        search.setPlaceholder("Search role...");
        search.setClearButtonVisible(true);
        search.setPrefixComponent(VaadinIcon.SEARCH.create());

        statusFilter.setItems(STATUS_ALL, "Active", "Inactive");
        statusFilter.setValue(STATUS_ALL);
        statusFilter.setPlaceholder("Status");

        Button addRole = new Button("Add Role", VaadinIcon.PLUS.create(),
                event -> getUI().ifPresent(ui -> ui.navigate("roles/add")));

        HorizontalLayout toolbar = new HorizontalLayout(search, statusFilter, addRole);
        toolbar.setWidthFull();
        toolbar.setAlignItems(HorizontalLayout.Alignment.CENTER);
        toolbar.setFlexGrow(1, search);

        grid.addColumn(RoleSummary::name).setHeader("Role Name");
        grid.addColumn(RoleSummary::code).setHeader("Code");
        grid.addColumn(role -> role.description() == null ? "" : role.description()).setHeader("Description");
        grid.addColumn(RoleSummary::userCount).setHeader("Users");
        grid.addComponentColumn(role -> statusLabel(role.active())).setHeader("Status");
        grid.addComponentColumn(this::actionMenu).setHeader("Action");
        grid.setSizeFull();

        search.addValueChangeListener(event -> applyFilter());
        statusFilter.addValueChangeListener(event -> applyFilter());

        add(header, toolbar, grid);
        setFlexGrow(1, grid);
        refresh();
    }

    private void refresh() {
        roles = roleService.findAll();
        applyFilter();
    }

    private void applyFilter() {
        String needle = search.getValue() == null ? "" : search.getValue().trim().toLowerCase();
        String status = statusFilter.getValue();
        grid.setItems(roles.stream()
                .filter(role -> needle.isEmpty() || role.name().toLowerCase().contains(needle)
                        || (role.description() != null && role.description().toLowerCase().contains(needle)))
                .filter(role -> status == null || STATUS_ALL.equals(status)
                        || role.active() == "Active".equals(status))
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

    private Component actionMenu(RoleSummary role) {
        MenuBar menuBar = new MenuBar();
        menuBar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);
        MenuItem actions = menuBar.addItem(VaadinIcon.ELLIPSIS_DOTS_V.create());
        SubMenu subMenu = actions.getSubMenu();
        subMenu.addItem("Edit", event -> getUI().ifPresent(ui -> ui.navigate("roles/edit/" + role.id())));
        subMenu.addItem("View Access", event -> openAccessDialog(role));
        subMenu.addItem(role.active() ? "Deactivate" : "Activate", event -> toggleActive(role));
        return menuBar;
    }

    private void toggleActive(RoleSummary role) {
        try {
            roleService.setActive(role.id(), !role.active());
            ViewNotifications.showSuccess("Role \"" + role.name() + "\" "
                    + (role.active() ? "deactivated" : "activated"));
        } catch (BusinessException e) {
            ViewNotifications.showError(e.getMessage());
        }
        refresh();
    }

    private void openAccessDialog(RoleSummary role) {
        List<PermissionSummary> permissions = permissionService.findAll();
        Set<UUID> granted = roleService.findPermissionIds(role.id());

        Grid<PermissionSummary> permissionGrid = new Grid<>();
        permissionGrid.setSelectionMode(Grid.SelectionMode.MULTI);
        permissionGrid.addColumn(PermissionSummary::code).setHeader("Permission Code");
        permissionGrid.addColumn(PermissionSummary::module).setHeader("Module");
        permissionGrid.addColumn(PermissionSummary::action).setHeader("Action");
        permissionGrid.addColumn(permission -> permission.active() ? "Active" : "Inactive").setHeader("Status");
        permissionGrid.setItems(permissions);
        permissions.stream().filter(permission -> granted.contains(permission.id()))
                .forEach(permissionGrid::select);

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Access for " + role.name());
        dialog.setWidth("800px");
        dialog.add(permissionGrid);

        Button cancel = new Button("Cancel", event -> dialog.close());
        Button save = new Button("Save Access", event -> {
            Set<UUID> selected = permissionGrid.getSelectedItems().stream().map(PermissionSummary::id)
                    .collect(Collectors.toSet());
            try {
                roleService.updatePermissions(role.id(), selected);
                ViewNotifications.showSuccess("Access for \"" + role.name() + "\" saved");
                dialog.close();
            } catch (BusinessException e) {
                ViewNotifications.showError(e.getMessage());
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(cancel, save);
        dialog.open();
    }
}
