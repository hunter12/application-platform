package application.platform.views.permissions;

import java.util.List;
import java.util.stream.Stream;

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
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import application.platform.common.BusinessException;
import application.platform.permission.PermissionService;
import application.platform.permission.PermissionSummary;
import application.platform.views.MainLayout;
import application.platform.views.ViewNotifications;

import jakarta.annotation.security.PermitAll;

@Route(value = "permissions", layout = MainLayout.class)
@PageTitle("Permission Management")
@PermitAll
public class PermissionView extends VerticalLayout {

    static final List<String> MODULES = List.of("Users", "Roles", "Organization", "Approval", "Notification",
            "Audit", "Finance", "Stock", "Invoice", "Report");
    static final List<String> ACTIONS = List.of("View", "Create", "Update", "Delete", "Approve", "Export");

    private static final String FILTER_ALL = "All";

    private final PermissionService permissionService;
    private final Grid<PermissionSummary> grid = new Grid<>();
    private final TextField search = new TextField();
    private final ComboBox<String> moduleFilter = new ComboBox<>();
    private final ComboBox<String> actionFilter = new ComboBox<>();
    private final ComboBox<String> statusFilter = new ComboBox<>();
    private List<PermissionSummary> permissions = List.of();

    public PermissionView(PermissionService permissionService) {
        this.permissionService = permissionService;
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Permission Management");
        Span subtitle = new Span("Manage system permissions");
        subtitle.getStyle().set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout header = new VerticalLayout(title, subtitle);
        header.setPadding(false);
        header.setSpacing(false);

        search.setPlaceholder("Search permission...");
        search.setClearButtonVisible(true);
        search.setPrefixComponent(VaadinIcon.SEARCH.create());

        moduleFilter.setItems(withAll(MODULES));
        moduleFilter.setValue(FILTER_ALL);
        moduleFilter.setPlaceholder("Module");

        actionFilter.setItems(withAll(ACTIONS));
        actionFilter.setValue(FILTER_ALL);
        actionFilter.setPlaceholder("Action");

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

        grid.addColumn(PermissionSummary::code).setHeader("Permission Code");
        grid.addColumn(PermissionSummary::name).setHeader("Name");
        grid.addColumn(PermissionSummary::module).setHeader("Module");
        grid.addColumn(PermissionSummary::action).setHeader("Action");
        grid.addComponentColumn(permission -> statusLabel(permission.active())).setHeader("Status");
        grid.addColumn(permission -> permission.description() == null ? "" : permission.description())
                .setHeader("Description");
        grid.addComponentColumn(this::actionMenu).setHeader("Action");
        grid.setSizeFull();

        search.addValueChangeListener(event -> applyFilter());
        moduleFilter.addValueChangeListener(event -> applyFilter());
        actionFilter.addValueChangeListener(event -> applyFilter());
        statusFilter.addValueChangeListener(event -> applyFilter());

        add(header, toolbar, grid);
        setFlexGrow(1, grid);
        refresh();
    }

    private static List<String> withAll(List<String> values) {
        return Stream.concat(Stream.of(FILTER_ALL), values.stream()).toList();
    }

    private void refresh() {
        permissions = permissionService.findAll();
        applyFilter();
    }

    private void applyFilter() {
        String needle = search.getValue() == null ? "" : search.getValue().trim().toLowerCase();
        String module = moduleFilter.getValue();
        String action = actionFilter.getValue();
        String status = statusFilter.getValue();
        grid.setItems(permissions.stream()
                .filter(permission -> needle.isEmpty() || permission.code().toLowerCase().contains(needle)
                        || permission.name().toLowerCase().contains(needle)
                        || (permission.description() != null
                                && permission.description().toLowerCase().contains(needle)))
                .filter(permission -> module == null || FILTER_ALL.equals(module)
                        || permission.module().equals(module))
                .filter(permission -> action == null || FILTER_ALL.equals(action)
                        || permission.action().equals(action))
                .filter(permission -> status == null || FILTER_ALL.equals(status)
                        || permission.active() == "Active".equals(status))
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

    private Component actionMenu(PermissionSummary permission) {
        MenuBar menuBar = new MenuBar();
        menuBar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);
        MenuItem actions = menuBar.addItem(VaadinIcon.ELLIPSIS_DOTS_V.create());
        SubMenu subMenu = actions.getSubMenu();
        subMenu.addItem("Edit",
                event -> getUI().ifPresent(ui -> ui.navigate("permissions/edit/" + permission.id())));
        subMenu.addItem(permission.active() ? "Deactivate" : "Activate", event -> toggleActive(permission));
        return menuBar;
    }

    private void toggleActive(PermissionSummary permission) {
        try {
            permissionService.setActive(permission.id(), !permission.active());
            ViewNotifications.showSuccess("Permission " + permission.code() + " "
                    + (permission.active() ? "deactivated" : "activated"));
        } catch (BusinessException e) {
            ViewNotifications.showError(e.getMessage());
        }
        refresh();
    }
}
