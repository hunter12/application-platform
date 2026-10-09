package application.platform.views.organization;

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
 * Visual prototype of the Organizations page. Uses static mock data only -
 * Add/Edit/View Hierarchy/Deactivate are UI affordances that do not persist
 * anything.
 */
@Route(value = "organizations", layout = MainLayout.class)
@PageTitle("Organizations")
@PermitAll
public class OrganizationView extends VerticalLayout {

    private static final String FILTER_ALL = "All";

    public OrganizationView() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Organizations");
        Span subtitle = new Span("Manage organizational units");
        subtitle.getStyle().set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout header = new VerticalLayout(title, subtitle);
        header.setPadding(false);
        header.setSpacing(false);

        TextField search = new TextField();
        search.setPlaceholder("Search organization...");
        search.setClearButtonVisible(true);
        search.setPrefixComponent(VaadinIcon.SEARCH.create());

        ComboBox<String> typeFilter = new ComboBox<>();
        typeFilter.setItems(FILTER_ALL, "Head Office", "Regional", "Branch", "Division", "Department", "Section",
                "Unit");
        typeFilter.setValue(FILTER_ALL);
        typeFilter.setPlaceholder("Type");

        ComboBox<String> statusFilter = new ComboBox<>();
        statusFilter.setItems(FILTER_ALL, "Active", "Inactive");
        statusFilter.setValue(FILTER_ALL);
        statusFilter.setPlaceholder("Status");

        Button addOrganization = new Button("Add Organization", VaadinIcon.PLUS.create(),
                event -> getUI().ifPresent(ui -> ui.navigate("organizations/add")));

        HorizontalLayout toolbar = new HorizontalLayout(search, typeFilter, statusFilter, addOrganization);
        toolbar.setWidthFull();
        toolbar.setAlignItems(HorizontalLayout.Alignment.CENTER);
        toolbar.setFlexGrow(1, search);

        Grid<OrgRow> grid = new Grid<>();
        grid.setItems(MockOrgRowData.ALL);
        grid.addColumn(OrgRow::code).setHeader("Code");
        grid.addColumn(OrgRow::name).setHeader("Organization Name");
        grid.addColumn(OrgRow::type).setHeader("Type");
        grid.addColumn(OrgRow::parent).setHeader("Parent Organization");
        grid.addComponentColumn(row -> statusLabel(row.status())).setHeader("Status");
        grid.addComponentColumn(this::actionMenu).setHeader("Action");
        grid.setSizeFull();

        search.addValueChangeListener(
                event -> grid.setItems(filter(event.getValue(), typeFilter.getValue(), statusFilter.getValue())));
        typeFilter.addValueChangeListener(
                event -> grid.setItems(filter(search.getValue(), event.getValue(), statusFilter.getValue())));
        statusFilter.addValueChangeListener(
                event -> grid.setItems(filter(search.getValue(), typeFilter.getValue(), event.getValue())));

        add(header, toolbar, grid);
        setFlexGrow(1, grid);
    }

    private List<OrgRow> filter(String term, String type, String status) {
        String needle = term == null ? "" : term.trim().toLowerCase();
        return MockOrgRowData.ALL.stream()
                .filter(row -> needle.isEmpty() || row.name().toLowerCase().contains(needle)
                        || row.code().toLowerCase().contains(needle))
                .filter(row -> type == null || FILTER_ALL.equals(type) || row.type().equals(type))
                .filter(row -> status == null || FILTER_ALL.equals(status) || row.status().equals(status))
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

    private Component actionMenu(OrgRow row) {
        MenuBar menuBar = new MenuBar();
        menuBar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);
        MenuItem actions = menuBar.addItem(VaadinIcon.ELLIPSIS_DOTS_V.create());
        SubMenu subMenu = actions.getSubMenu();
        subMenu.addItem("Edit", event -> showPrototypeNotice("Editing organizations"));
        subMenu.addItem("View Hierarchy", event -> getUI().ifPresent(ui -> ui.navigate("organizations/hierarchy")));
        subMenu.addItem("Deactivate", event -> showPrototypeNotice("Deactivating organizations"));
        return menuBar;
    }

    private void showPrototypeNotice(String action) {
        Notification.show(action + " is a UI prototype only - no data is persisted.", 3000,
                Notification.Position.BOTTOM_START);
    }
}
