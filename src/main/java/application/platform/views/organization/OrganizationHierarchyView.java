package application.platform.views.organization;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import application.platform.organization.OrganizationService;
import application.platform.organization.OrganizationSummary;
import application.platform.views.MainLayout;

import jakarta.annotation.security.PermitAll;

@Route(value = "organizations/hierarchy", layout = MainLayout.class)
@PageTitle("Organization Hierarchy")
@PermitAll
public class OrganizationHierarchyView extends VerticalLayout {

    public OrganizationHierarchyView(OrganizationService organizationService) {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Organization Hierarchy");
        Span subtitle = new Span("View organizational structure");
        subtitle.getStyle().set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout header = new VerticalLayout(title, subtitle);
        header.setPadding(false);
        header.setSpacing(false);

        List<OrganizationSummary> organizations = organizationService.findAll();
        Set<UUID> ids = organizations.stream().map(OrganizationSummary::id).collect(Collectors.toSet());
        // Units whose parent is missing (none, or no longer visible) are shown at the top level.
        List<OrganizationSummary> roots = organizations.stream()
                .filter(org -> org.parentId() == null || !ids.contains(org.parentId()))
                .toList();
        Map<UUID, List<OrganizationSummary>> childrenByParent = organizations.stream()
                .filter(org -> org.parentId() != null)
                .collect(Collectors.groupingBy(OrganizationSummary::parentId));

        TreeGrid<OrganizationSummary> treeGrid = new TreeGrid<>();
        treeGrid.setItems(roots, org -> childrenByParent.getOrDefault(org.id(), List.of()));
        treeGrid.addHierarchyColumn(OrganizationSummary::name).setHeader("Organization");
        treeGrid.addColumn(OrganizationSummary::code).setHeader("Code");
        treeGrid.addColumn(org -> org.type().getLabel()).setHeader("Type");
        treeGrid.addComponentColumn(org -> statusLabel(org.active())).setHeader("Status");
        treeGrid.expandRecursively(roots, Integer.MAX_VALUE);
        treeGrid.setSizeFull();

        add(header, treeGrid);
        setFlexGrow(1, treeGrid);
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
}
