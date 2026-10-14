package application.platform.views.approval;

import java.util.List;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
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

import application.platform.approval.ApprovalStepSummary;
import application.platform.approval.ApprovalWorkflowService;
import application.platform.approval.ApprovalWorkflowSummary;
import application.platform.common.BusinessException;
import application.platform.module.ModuleService;
import application.platform.module.ModuleSummary;
import application.platform.views.MainLayout;
import application.platform.views.ViewNotifications;

import jakarta.annotation.security.PermitAll;

@Route(value = "approval/workflows", layout = MainLayout.class)
@PageTitle("Approval Workflows")
@PermitAll
public class ApprovalWorkflowView extends VerticalLayout {

    private static final String FILTER_ALL = "All";

    private final ApprovalWorkflowService workflowService;
    private final Grid<ApprovalWorkflowSummary> grid = new Grid<>();
    private final TextField search = new TextField();
    private final ComboBox<String> moduleFilter = new ComboBox<>();
    private final ComboBox<String> statusFilter = new ComboBox<>();
    private List<ApprovalWorkflowSummary> workflows = List.of();

    public ApprovalWorkflowView(ApprovalWorkflowService workflowService, ModuleService moduleService) {
        this.workflowService = workflowService;
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Approval Workflows");
        Span subtitle = new Span("Manage approval workflows");
        subtitle.getStyle().set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout header = new VerticalLayout(title, subtitle);
        header.setPadding(false);
        header.setSpacing(false);

        search.setPlaceholder("Search workflow...");
        search.setClearButtonVisible(true);
        search.setPrefixComponent(VaadinIcon.SEARCH.create());

        moduleFilter.setItems(Stream.concat(Stream.of(FILTER_ALL),
                moduleService.findAll().stream().map(ModuleSummary::name)).toList());
        moduleFilter.setValue(FILTER_ALL);
        moduleFilter.setPlaceholder("Module");

        statusFilter.setItems(FILTER_ALL, "Active", "Inactive");
        statusFilter.setValue(FILTER_ALL);
        statusFilter.setPlaceholder("Status");

        Button addWorkflow = new Button("Add Workflow", VaadinIcon.PLUS.create(),
                event -> getUI().ifPresent(ui -> ui.navigate("approval/workflows/add")));

        HorizontalLayout toolbar = new HorizontalLayout(search, moduleFilter, statusFilter, addWorkflow);
        toolbar.setWidthFull();
        toolbar.setAlignItems(HorizontalLayout.Alignment.CENTER);
        toolbar.setFlexGrow(1, search);

        grid.addColumn(ApprovalWorkflowSummary::name).setHeader("Workflow Name");
        grid.addColumn(ApprovalWorkflowSummary::moduleName).setHeader("Module");
        grid.addColumn(workflow -> workflow.steps().size()).setHeader("Steps");
        grid.addComponentColumn(workflow -> statusLabel(workflow.active())).setHeader("Status");
        grid.addComponentColumn(this::actionMenu).setHeader("Action");
        grid.setSizeFull();

        search.addValueChangeListener(event -> applyFilter());
        moduleFilter.addValueChangeListener(event -> applyFilter());
        statusFilter.addValueChangeListener(event -> applyFilter());

        add(header, toolbar, grid);
        setFlexGrow(1, grid);
        refresh();
    }

    private void refresh() {
        workflows = workflowService.findAll();
        applyFilter();
    }

    private void applyFilter() {
        String needle = search.getValue() == null ? "" : search.getValue().trim().toLowerCase();
        String module = moduleFilter.getValue();
        String status = statusFilter.getValue();
        grid.setItems(workflows.stream()
                .filter(workflow -> needle.isEmpty() || workflow.name().toLowerCase().contains(needle))
                .filter(workflow -> module == null || FILTER_ALL.equals(module)
                        || workflow.moduleName().equals(module))
                .filter(workflow -> status == null || FILTER_ALL.equals(status)
                        || workflow.active() == "Active".equals(status))
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

    private Component actionMenu(ApprovalWorkflowSummary workflow) {
        MenuBar menuBar = new MenuBar();
        menuBar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);
        MenuItem actions = menuBar.addItem(VaadinIcon.ELLIPSIS_DOTS_V.create());
        SubMenu subMenu = actions.getSubMenu();
        subMenu.addItem("Edit",
                event -> getUI().ifPresent(ui -> ui.navigate("approval/workflows/edit/" + workflow.id())));
        subMenu.addItem("View", event -> openStepsDialog(workflow));
        subMenu.addItem(workflow.active() ? "Deactivate" : "Activate", event -> toggleActive(workflow));
        return menuBar;
    }

    private void toggleActive(ApprovalWorkflowSummary workflow) {
        try {
            workflowService.setActive(workflow.id(), !workflow.active());
            ViewNotifications.showSuccess("Workflow \"" + workflow.name() + "\" "
                    + (workflow.active() ? "deactivated" : "activated"));
        } catch (BusinessException e) {
            ViewNotifications.showError(e.getMessage());
        }
        refresh();
    }

    private void openStepsDialog(ApprovalWorkflowSummary workflow) {
        Grid<ApprovalStepSummary> stepGrid = new Grid<>();
        stepGrid.addColumn(step -> "Step " + step.stepOrder()).setHeader("Step").setAutoWidth(true);
        stepGrid.addColumn(step -> step.approverType().getLabel()).setHeader("Approver Type");
        stepGrid.addColumn(step -> step.roleName() == null ? "-" : step.roleName()).setHeader("Role");
        stepGrid.setItems(workflow.steps());
        stepGrid.setAllRowsVisible(true);

        Span info = new Span(workflow.moduleName()
                + (workflow.description() == null ? "" : " - " + workflow.description()));
        info.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(workflow.name());
        dialog.setWidth("720px");
        dialog.add(new VerticalLayout(info, stepGrid));
        dialog.getFooter().add(new Button("Close", event -> dialog.close()));
        dialog.open();
    }
}
