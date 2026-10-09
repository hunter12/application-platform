package application.platform.views.approval;

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
 * Visual prototype of the Approval Workflows page. Uses static mock data
 * only - Add/Edit/View/Deactivate are UI affordances that do not persist
 * anything.
 */
@Route(value = "approval/workflows", layout = MainLayout.class)
@PageTitle("Approval Workflows")
@PermitAll
public class ApprovalWorkflowView extends VerticalLayout {

    private static final String FILTER_ALL = "All";

    public ApprovalWorkflowView() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Approval Workflows");
        Span subtitle = new Span("Manage approval workflows");
        subtitle.getStyle().set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout header = new VerticalLayout(title, subtitle);
        header.setPadding(false);
        header.setSpacing(false);

        TextField search = new TextField();
        search.setPlaceholder("Search workflow...");
        search.setClearButtonVisible(true);
        search.setPrefixComponent(VaadinIcon.SEARCH.create());

        ComboBox<String> moduleFilter = new ComboBox<>();
        moduleFilter.setItems(FILTER_ALL, "Invoice", "Finance", "Stock", "Product", "Report");
        moduleFilter.setValue(FILTER_ALL);
        moduleFilter.setPlaceholder("Module");

        ComboBox<String> statusFilter = new ComboBox<>();
        statusFilter.setItems(FILTER_ALL, "Active", "Inactive");
        statusFilter.setValue(FILTER_ALL);
        statusFilter.setPlaceholder("Status");

        Button addWorkflow = new Button("Add Workflow", VaadinIcon.PLUS.create(),
                event -> getUI().ifPresent(ui -> ui.navigate("approval/workflows/add")));

        HorizontalLayout toolbar = new HorizontalLayout(search, moduleFilter, statusFilter, addWorkflow);
        toolbar.setWidthFull();
        toolbar.setAlignItems(HorizontalLayout.Alignment.CENTER);
        toolbar.setFlexGrow(1, search);

        Grid<MockWorkflow> grid = new Grid<>();
        grid.setItems(MockWorkflowData.ALL);
        grid.addColumn(MockWorkflow::name).setHeader("Workflow Name");
        grid.addColumn(MockWorkflow::module).setHeader("Module");
        grid.addColumn(MockWorkflow::steps).setHeader("Steps");
        grid.addComponentColumn(workflow -> statusLabel(workflow.status())).setHeader("Status");
        grid.addComponentColumn(this::actionMenu).setHeader("Action");
        grid.setSizeFull();

        search.addValueChangeListener(
                event -> grid.setItems(filter(event.getValue(), moduleFilter.getValue(), statusFilter.getValue())));
        moduleFilter.addValueChangeListener(
                event -> grid.setItems(filter(search.getValue(), event.getValue(), statusFilter.getValue())));
        statusFilter.addValueChangeListener(
                event -> grid.setItems(filter(search.getValue(), moduleFilter.getValue(), event.getValue())));

        add(header, toolbar, grid);
        setFlexGrow(1, grid);
    }

    private List<MockWorkflow> filter(String term, String module, String status) {
        String needle = term == null ? "" : term.trim().toLowerCase();
        return MockWorkflowData.ALL.stream()
                .filter(workflow -> needle.isEmpty() || workflow.name().toLowerCase().contains(needle))
                .filter(workflow -> module == null || FILTER_ALL.equals(module) || workflow.module().equals(module))
                .filter(workflow -> status == null || FILTER_ALL.equals(status) || workflow.status().equals(status))
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

    private Component actionMenu(MockWorkflow workflow) {
        MenuBar menuBar = new MenuBar();
        menuBar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);
        MenuItem actions = menuBar.addItem(VaadinIcon.ELLIPSIS_DOTS_V.create());
        SubMenu subMenu = actions.getSubMenu();
        subMenu.addItem("Edit", event -> showPrototypeNotice("Editing workflows"));
        subMenu.addItem("View", event -> showPrototypeNotice("Viewing workflows"));
        subMenu.addItem("Deactivate", event -> showPrototypeNotice("Deactivating workflows"));
        return menuBar;
    }

    private void showPrototypeNotice(String action) {
        Notification.show(action + " is a UI prototype only - no data is persisted.", 3000,
                Notification.Position.BOTTOM_START);
    }
}
