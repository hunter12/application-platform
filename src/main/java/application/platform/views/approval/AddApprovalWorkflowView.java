package application.platform.views.approval;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;

import application.platform.approval.ApprovalStepForm;
import application.platform.approval.ApprovalStepSummary;
import application.platform.approval.ApprovalWorkflowForm;
import application.platform.approval.ApprovalWorkflowService;
import application.platform.approval.ApprovalWorkflowSummary;
import application.platform.approval.ApproverType;
import application.platform.common.BusinessException;
import application.platform.module.ModuleService;
import application.platform.module.ModuleSummary;
import application.platform.role.RoleService;
import application.platform.role.RoleSummary;
import application.platform.views.MainLayout;
import application.platform.views.UrlIds;
import application.platform.views.ViewNotifications;

import jakarta.annotation.security.PermitAll;

@Route(value = "approval/workflows/add", layout = MainLayout.class)
@RouteAlias(value = "approval/workflows/edit", layout = MainLayout.class)
@PageTitle("Add Approval Workflow")
@PermitAll
public class AddApprovalWorkflowView extends VerticalLayout implements HasUrlParameter<String> {

    private static final String ACTIVE = "Active";

    private record StepRow(HorizontalLayout layout, Span label, ComboBox<ApproverType> approverType,
            ComboBox<RoleSummary> role) {
    }

    private final ApprovalWorkflowService workflowService;
    private final List<ModuleSummary> allModules;
    private final List<RoleSummary> allRoles;

    private final H2 title = new H2("Add Approval Workflow");
    private final Span subtitle = new Span("Create a new approval workflow");
    private final TextField name = new TextField("Workflow Name");
    private final ComboBox<ModuleSummary> module = new ComboBox<>("Module");
    private final ComboBox<String> status = new ComboBox<>("Status");
    private final TextArea description = new TextArea("Description");
    private final List<StepRow> stepRows = new ArrayList<>();
    private final VerticalLayout stepsContainer = new VerticalLayout();

    /** The workflow being edited, or {@code null} when adding a new one. */
    private ApprovalWorkflowSummary editing;

    public AddApprovalWorkflowView(ApprovalWorkflowService workflowService, ModuleService moduleService,
            RoleService roleService) {
        this.workflowService = workflowService;
        this.allModules = moduleService.findAll();
        this.allRoles = roleService.findAll();
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

        status.setItems(ACTIVE, "Inactive");
        status.setValue(ACTIVE);

        FlexLayout fields = twoColumnSection(name, module, status, description);

        stepsContainer.setPadding(false);
        stepsContainer.setSpacing(true);
        addStepRow(null);

        Button addStep = new Button("Add Step", VaadinIcon.PLUS.create(), event -> addStepRow(null));

        VerticalLayout stepsSection = new VerticalLayout(stepsContainer, addStep);
        stepsSection.setPadding(false);
        stepsSection.setSpacing(true);

        Button cancel = new Button("Cancel", event -> getUI().ifPresent(ui -> ui.navigate("approval/workflows")));

        Button save = new Button("Save Workflow", event -> save());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout actions = new HorizontalLayout(cancel, save);
        actions.setWidthFull();
        actions.setJustifyContentMode(HorizontalLayout.JustifyContentMode.END);

        VerticalLayout form = new VerticalLayout(header,
                sectionHeading("Workflow Information"), fields,
                sectionHeading("Approval Steps"), stepsSection,
                actions);
        form.setPadding(false);
        form.setSpacing(true);
        form.setMaxWidth("960px");

        add(form);
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
        if (parameter == null) {
            return;
        }
        editing = UrlIds.parse(parameter).flatMap(workflowService::findById).orElse(null);
        if (editing == null) {
            ViewNotifications.showError("Approval workflow not found");
            event.forwardTo("approval/workflows");
            return;
        }

        title.setText("Edit Approval Workflow");
        subtitle.setText("Update workflow " + editing.name());
        name.setValue(editing.name());
        List<ModuleSummary> moduleChoices = allModules.stream()
                .filter(choice -> choice.active() || choice.id().equals(editing.moduleId()))
                .toList();
        module.setItems(moduleChoices);
        moduleChoices.stream().filter(choice -> choice.id().equals(editing.moduleId())).findFirst()
                .ifPresent(module::setValue);
        status.setValue(editing.active() ? ACTIVE : "Inactive");
        description.setValue(editing.description() == null ? "" : editing.description());

        new ArrayList<>(stepRows).forEach(this::removeStepRow);
        editing.steps().forEach(this::addStepRow);
    }

    private void addStepRow(ApprovalStepSummary step) {
        ComboBox<ApproverType> approverType = new ComboBox<>();
        approverType.setItems(ApproverType.values());
        approverType.setItemLabelGenerator(ApproverType::getLabel);
        approverType.setPlaceholder("Approver Type");
        approverType.getStyle().set("flex", "1 1 260px").set("min-width", "220px");

        ComboBox<RoleSummary> role = new ComboBox<>();
        role.setItems(allRoles.stream()
                .filter(choice -> choice.active() || (step != null && choice.id().equals(step.roleId())))
                .toList());
        role.setItemLabelGenerator(RoleSummary::name);
        role.setPlaceholder("Approver Role");
        role.getStyle().set("flex", "1 1 220px").set("min-width", "200px");

        approverType.addValueChangeListener(event -> {
            boolean needsRole = event.getValue() == null || event.getValue().needsRole();
            role.setEnabled(needsRole);
            if (!needsRole) {
                role.clear();
            }
        });

        if (step != null) {
            approverType.setValue(step.approverType());
            allRoles.stream().filter(choice -> choice.id().equals(step.roleId())).findFirst()
                    .ifPresent(role::setValue);
        }

        Span stepLabel = new Span();
        stepLabel.getStyle().set("font-weight", "600").set("min-width", "70px");

        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setAlignItems(HorizontalLayout.Alignment.CENTER);

        StepRow stepRow = new StepRow(row, stepLabel, approverType, role);
        Button remove = new Button(VaadinIcon.TRASH.create(), event -> removeStepRow(stepRow));

        row.add(stepLabel, approverType, role, remove);

        stepRows.add(stepRow);
        stepsContainer.add(row);
        renumberSteps();
    }

    private void removeStepRow(StepRow stepRow) {
        stepRows.remove(stepRow);
        stepsContainer.remove(stepRow.layout());
        renumberSteps();
    }

    private void renumberSteps() {
        for (int i = 0; i < stepRows.size(); i++) {
            stepRows.get(i).label().setText("Step " + (i + 1));
        }
    }

    private void save() {
        List<ApprovalStepForm> steps = stepRows.stream()
                .map(row -> new ApprovalStepForm(row.approverType().getValue(),
                        row.role().getValue() == null ? null : row.role().getValue().id()))
                .toList();
        ApprovalWorkflowForm form = new ApprovalWorkflowForm(
                editing == null ? null : editing.id(),
                editing == null ? null : editing.version(),
                name.getValue(),
                module.getValue() == null ? null : module.getValue().id(),
                description.getValue(),
                ACTIVE.equals(status.getValue()),
                steps);
        try {
            ApprovalWorkflowSummary saved = workflowService.save(form);
            ViewNotifications.showSuccess("Workflow \"" + saved.name() + "\" saved");
            getUI().ifPresent(ui -> ui.navigate("approval/workflows"));
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
