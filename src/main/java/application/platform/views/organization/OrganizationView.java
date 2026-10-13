package application.platform.views.organization;

import java.util.Arrays;
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
import application.platform.organization.OrganizationService;
import application.platform.organization.OrganizationSummary;
import application.platform.organization.OrganizationType;
import application.platform.views.MainLayout;
import application.platform.views.ViewNotifications;

import jakarta.annotation.security.PermitAll;

@Route(value = "organizations", layout = MainLayout.class)
@PageTitle("Organizations")
@PermitAll
public class OrganizationView extends VerticalLayout {

    private static final String FILTER_ALL = "All";

    private final OrganizationService organizationService;
    private final Grid<OrganizationSummary> grid = new Grid<>();
    private final TextField search = new TextField();
    private final ComboBox<String> typeFilter = new ComboBox<>();
    private final ComboBox<String> statusFilter = new ComboBox<>();
    private List<OrganizationSummary> organizations = List.of();

    public OrganizationView(OrganizationService organizationService) {
        this.organizationService = organizationService;
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Organizations");
        Span subtitle = new Span("Manage organizational units");
        subtitle.getStyle().set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout header = new VerticalLayout(title, subtitle);
        header.setPadding(false);
        header.setSpacing(false);

        search.setPlaceholder("Search organization...");
        search.setClearButtonVisible(true);
        search.setPrefixComponent(VaadinIcon.SEARCH.create());

        typeFilter.setItems(Stream.concat(Stream.of(FILTER_ALL),
                Arrays.stream(OrganizationType.values()).map(OrganizationType::getLabel)).toList());
        typeFilter.setValue(FILTER_ALL);
        typeFilter.setPlaceholder("Type");

        statusFilter.setItems(FILTER_ALL, "Active", "Inactive");
        statusFilter.setValue(FILTER_ALL);
        statusFilter.setPlaceholder("Status");

        Button addOrganization = new Button("Add Organization", VaadinIcon.PLUS.create(),
                event -> getUI().ifPresent(ui -> ui.navigate("organizations/add")));

        HorizontalLayout toolbar = new HorizontalLayout(search, typeFilter, statusFilter, addOrganization);
        toolbar.setWidthFull();
        toolbar.setAlignItems(HorizontalLayout.Alignment.CENTER);
        toolbar.setFlexGrow(1, search);

        grid.addColumn(OrganizationSummary::code).setHeader("Code");
        grid.addColumn(OrganizationSummary::name).setHeader("Organization Name");
        grid.addColumn(org -> org.type().getLabel()).setHeader("Type");
        grid.addColumn(org -> org.parentName() == null ? "-" : org.parentName()).setHeader("Parent Organization");
        grid.addComponentColumn(org -> statusLabel(org.active())).setHeader("Status");
        grid.addComponentColumn(this::actionMenu).setHeader("Action");
        grid.setSizeFull();

        search.addValueChangeListener(event -> applyFilter());
        typeFilter.addValueChangeListener(event -> applyFilter());
        statusFilter.addValueChangeListener(event -> applyFilter());

        add(header, toolbar, grid);
        setFlexGrow(1, grid);
        refresh();
    }

    private void refresh() {
        organizations = organizationService.findAll();
        applyFilter();
    }

    private void applyFilter() {
        String needle = search.getValue() == null ? "" : search.getValue().trim().toLowerCase();
        String type = typeFilter.getValue();
        String status = statusFilter.getValue();
        grid.setItems(organizations.stream()
                .filter(org -> needle.isEmpty() || org.name().toLowerCase().contains(needle)
                        || org.code().toLowerCase().contains(needle))
                .filter(org -> type == null || FILTER_ALL.equals(type) || org.type().getLabel().equals(type))
                .filter(org -> status == null || FILTER_ALL.equals(status)
                        || org.active() == "Active".equals(status))
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

    private Component actionMenu(OrganizationSummary org) {
        MenuBar menuBar = new MenuBar();
        menuBar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);
        MenuItem actions = menuBar.addItem(VaadinIcon.ELLIPSIS_DOTS_V.create());
        SubMenu subMenu = actions.getSubMenu();
        subMenu.addItem("Edit", event -> getUI().ifPresent(ui -> ui.navigate("organizations/edit/" + org.id())));
        subMenu.addItem("View Hierarchy", event -> getUI().ifPresent(ui -> ui.navigate("organizations/hierarchy")));
        subMenu.addItem(org.active() ? "Deactivate" : "Activate", event -> toggleActive(org));
        return menuBar;
    }

    private void toggleActive(OrganizationSummary org) {
        try {
            organizationService.setActive(org.id(), !org.active());
            ViewNotifications.showSuccess("Organization \"" + org.name() + "\" "
                    + (org.active() ? "deactivated" : "activated"));
        } catch (BusinessException e) {
            ViewNotifications.showError(e.getMessage());
        }
        refresh();
    }
}
