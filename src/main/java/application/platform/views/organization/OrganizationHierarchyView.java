package application.platform.views.organization;

import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import application.platform.views.MainLayout;

import jakarta.annotation.security.PermitAll;

/**
 * Visual prototype of the Organization Hierarchy page. Uses a static mock
 * tree only - no persistence.
 */
@Route(value = "organizations/hierarchy", layout = MainLayout.class)
@PageTitle("Organization Hierarchy")
@PermitAll
public class OrganizationHierarchyView extends VerticalLayout {

    public OrganizationHierarchyView() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Organization Hierarchy");
        Span subtitle = new Span("View organizational structure");
        subtitle.getStyle().set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout header = new VerticalLayout(title, subtitle);
        header.setPadding(false);
        header.setSpacing(false);

        TreeGrid<OrgUnit> treeGrid = new TreeGrid<>();
        treeGrid.setItems(List.of(MockOrganizationData.ROOT), OrgUnit::children);
        treeGrid.addHierarchyColumn(OrgUnit::name).setHeader("Organization");
        treeGrid.addColumn(OrgUnit::type).setHeader("Type");
        treeGrid.addComponentColumn(unit -> statusLabel(unit.status())).setHeader("Status");
        treeGrid.expandRecursively(List.of(MockOrganizationData.ROOT), 3);
        treeGrid.setSizeFull();

        add(header, treeGrid);
        setFlexGrow(1, treeGrid);
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
}
