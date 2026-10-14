package application.platform.views.approval;

import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;

import application.platform.approval.ApprovalRuleForm;
import application.platform.approval.ApprovalRuleService;
import application.platform.approval.ApprovalRuleSummary;
import application.platform.approval.ApprovalWorkflowService;
import application.platform.approval.ApprovalWorkflowSummary;
import application.platform.common.BusinessException;
import application.platform.module.ModuleService;
import application.platform.module.ModuleSummary;
import application.platform.views.MainLayout;
import application.platform.views.UrlIds;
import application.platform.views.ViewNotifications;

import jakarta.annotation.security.PermitAll;

@Route(value = "approval/rules/add", layout = MainLayout.class)
@RouteAlias(value = "approval/rules/edit", layout = MainLayout.class)
@PageTitle("Add Approval Rule")
@PermitAll
public class AddApprovalRuleView extends VerticalLayout implements HasUrlParameter<String> {

    private static final String ACTIVE = "Active";

    private final ApprovalRuleService ruleService;
    private final List<ModuleSummary> allModules;
    private final List<ApprovalWorkflowSummary> allWorkflows;

    private final H2 title = new H2("Add Approval Rule");
    private final Span subtitle = new Span("Choose which workflow handles an amount range");
    private final TextField name = new TextField("Rule Name");
    private final ComboBox<ModuleSummary> module = new ComboBox<>("Module");
    private final BigDecimalField minimumAmount = new BigDecimalField("Minimum Amount");
    private final BigDecimalField maximumAmount = new BigDecimalField("Maximum Amount");
    private final ComboBox<ApprovalWorkflowSummary> workflow = new ComboBox<>("Workflow");
    private final ComboBox<String> status = new ComboBox<>("Status");
    private final TextArea description = new TextArea("Description");

    /** The rule being edited, or {@code null} when adding a new one. */
    private ApprovalRuleSummary editing;

    public AddApprovalRuleView(ApprovalRuleService ruleService, ApprovalWorkflowService workflowService,
            ModuleService moduleService) {
        this.ruleService = ruleService;
        this.allModules = moduleService.findAll();
        this.allWorkflows = workflowService.findAll();
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        subtitle.getStyle().set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout header = new VerticalLayout(title, subtitle);
        header.setPadding(false);
        header.setSpacing(false);

        name.setRequiredIndicatorVisible(true);

        module.setRequiredIndicatorVisible(true);
        module.setItems(allModules.stream().filter(ModuleSummary::active).toList());
        module.setItemLabelGenerator(ModuleSummary::name);
        module.addValueChangeListener(event -> refreshWorkflowChoices());

        for (BigDecimalField amount : List.of(minimumAmount, maximumAmount)) {
            amount.setRequiredIndicatorVisible(true);
            amount.setPrefixComponent(new Span("Rp"));
        }
        minimumAmount.setHelperText("Inclusive, e.g. 10000000.01");
        maximumAmount.setHelperText("Inclusive, up to 9999999999999.99");

        workflow.setRequiredIndicatorVisible(true);
        workflow.setItemLabelGenerator(ApprovalWorkflowSummary::name);
        workflow.setHelperText("Only active workflows of the selected module");

        status.setItems(ACTIVE, "Inactive");
        status.setValue(ACTIVE);

        FlexLayout fields = twoColumnSection(name, module, minimumAmount, maximumAmount, workflow, status,
                description);

        Button cancel = new Button("Cancel", event -> getUI().ifPresent(ui -> ui.navigate("approval/rules")));

        Button save = new Button("Save Rule", event -> save());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout actions = new HorizontalLayout(cancel, save);
        actions.setWidthFull();
        actions.setJustifyContentMode(HorizontalLayout.JustifyContentMode.END);

        VerticalLayout form = new VerticalLayout(header, sectionHeading("Rule Information"), fields, actions);
        form.setPadding(false);
        form.setSpacing(true);
        form.setMaxWidth("960px");

        add(form);
        refreshWorkflowChoices();
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
        if (parameter == null) {
            return;
        }
        editing = UrlIds.parse(parameter).flatMap(ruleService::findById).orElse(null);
        if (editing == null) {
            ViewNotifications.showError("Approval rule not found");
            event.forwardTo("approval/rules");
            return;
        }

        title.setText("Edit Approval Rule");
        subtitle.setText("Update rule " + editing.name());
        name.setValue(editing.name());
        List<ModuleSummary> moduleChoices = allModules.stream()
                .filter(choice -> choice.active() || choice.id().equals(editing.moduleId()))
                .toList();
        module.setItems(moduleChoices);
        moduleChoices.stream().filter(choice -> choice.id().equals(editing.moduleId())).findFirst()
                .ifPresent(module::setValue);
        minimumAmount.setValue(editing.minAmount());
        maximumAmount.setValue(editing.maxAmount());
        refreshWorkflowChoices();
        allWorkflows.stream().filter(choice -> choice.id().equals(editing.workflowId())).findFirst()
                .ifPresent(workflow::setValue);
        status.setValue(editing.active() ? ACTIVE : "Inactive");
        description.setValue(editing.description() == null ? "" : editing.description());
    }

    private void refreshWorkflowChoices() {
        ModuleSummary selectedModule = module.getValue();
        ApprovalWorkflowSummary selected = workflow.getValue();
        List<ApprovalWorkflowSummary> choices = selectedModule == null ? List.of() : allWorkflows.stream()
                .filter(choice -> choice.moduleId().equals(selectedModule.id()))
                .filter(choice -> choice.active() || (editing != null && choice.id().equals(editing.workflowId())))
                .toList();
        workflow.setItems(choices);
        if (selected != null && choices.contains(selected)) {
            workflow.setValue(selected);
        }
    }

    private void save() {
        ApprovalRuleForm form = new ApprovalRuleForm(
                editing == null ? null : editing.id(),
                editing == null ? null : editing.version(),
                name.getValue(),
                module.getValue() == null ? null : module.getValue().id(),
                minimumAmount.getValue(),
                maximumAmount.getValue(),
                workflow.getValue() == null ? null : workflow.getValue().id(),
                description.getValue(),
                ACTIVE.equals(status.getValue()));
        try {
            ApprovalRuleSummary saved = ruleService.save(form);
            ViewNotifications.showSuccess("Rule \"" + saved.name() + "\" saved");
            getUI().ifPresent(ui -> ui.navigate("approval/rules"));
        } catch (BusinessException e) {
            ViewNotifications.showError(e.getMessage());
        }
    }

    private Component sectionHeading(String text) {
        H3 heading = new H3(text);
        heading.getStyle().set("margin-bottom", "0");

        VerticalLayout wrapper = new VerticalLayout(heading, new Hr());
        wrapper.setPadding(false);
        wrapper.setSpacing(false);
        return wrapper;
    }

    private FlexLayout twoColumnSection(Component... fields) {
        FlexLayout layout = new FlexLayout(fields);
        layout.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        layout.getStyle().set("gap", "1rem");
        for (Component field : fields) {
            field.getElement().getStyle()
                    .set("flex", "1 1 320px")
                    .set("min-width", "280px");
        }
        return layout;
    }
}
