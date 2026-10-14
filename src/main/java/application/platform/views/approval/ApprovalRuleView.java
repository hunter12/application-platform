package application.platform.views.approval;

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

import application.platform.approval.ApprovalRuleService;
import application.platform.approval.ApprovalRuleSummary;
import application.platform.common.Amounts;
import application.platform.common.BusinessException;
import application.platform.module.ModuleService;
import application.platform.module.ModuleSummary;
import application.platform.views.MainLayout;
import application.platform.views.ViewNotifications;

import jakarta.annotation.security.PermitAll;

@Route(value = "approval/rules", layout = MainLayout.class)
@PageTitle("Approval Rules")
@PermitAll
public class ApprovalRuleView extends VerticalLayout {

    private static final String FILTER_ALL = "All";

    private final ApprovalRuleService ruleService;
    private final Grid<ApprovalRuleSummary> grid = new Grid<>();
    private final TextField search = new TextField();
    private final ComboBox<String> moduleFilter = new ComboBox<>();
    private final ComboBox<String> statusFilter = new ComboBox<>();
    private List<ApprovalRuleSummary> rules = List.of();

    public ApprovalRuleView(ApprovalRuleService ruleService, ModuleService moduleService) {
        this.ruleService = ruleService;
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Approval Rules");
        Span subtitle = new Span("Choose the approval workflow by module and amount");
        subtitle.getStyle().set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout header = new VerticalLayout(title, subtitle);
        header.setPadding(false);
        header.setSpacing(false);

        search.setPlaceholder("Search rule...");
        search.setClearButtonVisible(true);
        search.setPrefixComponent(VaadinIcon.SEARCH.create());

        moduleFilter.setItems(Stream.concat(Stream.of(FILTER_ALL),
                moduleService.findAll().stream().map(ModuleSummary::name)).toList());
        moduleFilter.setValue(FILTER_ALL);
        moduleFilter.setPlaceholder("Module");

        statusFilter.setItems(FILTER_ALL, "Active", "Inactive");
        statusFilter.setValue(FILTER_ALL);
        statusFilter.setPlaceholder("Status");

        Button addRule = new Button("Add Rule", VaadinIcon.PLUS.create(),
                event -> getUI().ifPresent(ui -> ui.navigate("approval/rules/add")));

        HorizontalLayout toolbar = new HorizontalLayout(search, moduleFilter, statusFilter, addRule);
        toolbar.setWidthFull();
        toolbar.setAlignItems(HorizontalLayout.Alignment.CENTER);
        toolbar.setFlexGrow(1, search);

        grid.addColumn(ApprovalRuleSummary::name).setHeader("Rule Name");
        grid.addColumn(ApprovalRuleSummary::moduleName).setHeader("Module");
        grid.addColumn(rule -> Amounts.format(rule.minAmount()) + " - " + Amounts.format(rule.maxAmount()))
                .setHeader("Amount Range").setAutoWidth(true);
        grid.addColumn(ApprovalRuleSummary::workflowName).setHeader("Workflow");
        grid.addComponentColumn(rule -> statusLabel(rule.active())).setHeader("Status");
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
        rules = ruleService.findAll();
        applyFilter();
    }

    private void applyFilter() {
        String needle = search.getValue() == null ? "" : search.getValue().trim().toLowerCase();
        String module = moduleFilter.getValue();
        String status = statusFilter.getValue();
        grid.setItems(rules.stream()
                .filter(rule -> needle.isEmpty() || rule.name().toLowerCase().contains(needle)
                        || rule.workflowName().toLowerCase().contains(needle))
                .filter(rule -> module == null || FILTER_ALL.equals(module) || rule.moduleName().equals(module))
                .filter(rule -> status == null || FILTER_ALL.equals(status)
                        || rule.active() == "Active".equals(status))
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

    private Component actionMenu(ApprovalRuleSummary rule) {
        MenuBar menuBar = new MenuBar();
        menuBar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);
        MenuItem actions = menuBar.addItem(VaadinIcon.ELLIPSIS_DOTS_V.create());
        SubMenu subMenu = actions.getSubMenu();
        subMenu.addItem("Edit", event -> getUI().ifPresent(ui -> ui.navigate("approval/rules/edit/" + rule.id())));
        subMenu.addItem(rule.active() ? "Deactivate" : "Activate", event -> toggleActive(rule));
        return menuBar;
    }

    private void toggleActive(ApprovalRuleSummary rule) {
        try {
            ruleService.setActive(rule.id(), !rule.active());
            ViewNotifications.showSuccess("Rule \"" + rule.name() + "\" "
                    + (rule.active() ? "deactivated" : "activated"));
        } catch (BusinessException e) {
            ViewNotifications.showError(e.getMessage());
        }
        refresh();
    }
}
