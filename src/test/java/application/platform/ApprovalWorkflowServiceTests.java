package application.platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import application.platform.approval.ApprovalStepForm;
import application.platform.approval.ApprovalStepSummary;
import application.platform.approval.ApprovalWorkflowForm;
import application.platform.approval.ApprovalWorkflowService;
import application.platform.approval.ApprovalWorkflowSummary;
import application.platform.approval.ApproverType;
import application.platform.common.BusinessException;
import application.platform.module.AppModuleRepository;
import application.platform.role.RoleRepository;
import application.platform.role.RoleService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("dev")
@Transactional
@WithMockUser(username = "admin")
class ApprovalWorkflowServiceTests {

    @Autowired
    private ApprovalWorkflowService workflowService;

    @Autowired
    private AppModuleRepository moduleRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RoleService roleService;

    @Test
    void savesStepsInOrderAndDropsRoleForDirectSupervisor() {
        ApprovalWorkflowSummary saved = workflowService.save(newWorkflow("Invoice Approval",
                new ApprovalStepForm(ApproverType.DIRECT_SUPERVISOR, role("MANAGER")),
                new ApprovalStepForm(ApproverType.ROLE_IN_REQUESTER_ORG, role("MANAGER")),
                new ApprovalStepForm(ApproverType.ROLE_IN_PARENT_ORG, role("DIRECTOR"))));

        assertThat(saved.moduleName()).isEqualTo("Invoice");
        assertThat(saved.steps()).extracting(ApprovalStepSummary::stepOrder).containsExactly(1, 2, 3);
        assertThat(saved.steps()).extracting(ApprovalStepSummary::roleName)
                .containsExactly(null, "Manager", "Director");
    }

    @Test
    void editingCanShrinkAndGrowTheSteps() {
        ApprovalWorkflowSummary saved = workflowService.save(newWorkflow("Purchase Approval",
                new ApprovalStepForm(ApproverType.DIRECT_SUPERVISOR, null),
                new ApprovalStepForm(ApproverType.ROLE_IN_REQUESTER_ORG, role("MANAGER")),
                new ApprovalStepForm(ApproverType.ROLE_IN_PARENT_ORG, role("DIRECTOR"))));

        ApprovalWorkflowSummary shrunk = workflowService.save(edit(saved,
                new ApprovalStepForm(ApproverType.ROLE_IN_PARENT_ORG, role("DIRECTOR"))));
        assertThat(shrunk.steps()).hasSize(1);
        assertThat(shrunk.steps().get(0).roleName()).isEqualTo("Director");

        ApprovalWorkflowSummary grown = workflowService.save(edit(shrunk,
                new ApprovalStepForm(ApproverType.DIRECT_SUPERVISOR, null),
                new ApprovalStepForm(ApproverType.ROLE_IN_REQUESTER_ORG, role("SPV"))));
        assertThat(grown.steps()).extracting(ApprovalStepSummary::stepOrder).containsExactly(1, 2);
        assertThat(grown.steps()).extracting(ApprovalStepSummary::approverType)
                .containsExactly(ApproverType.DIRECT_SUPERVISOR, ApproverType.ROLE_IN_REQUESTER_ORG);
    }

    @Test
    void rejectsMissingStepsRolesAndDuplicateNames() {
        assertThatThrownBy(() -> workflowService.save(newWorkflow("No Steps")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("at least one");
        assertThatThrownBy(() -> workflowService.save(newWorkflow("No Role",
                new ApprovalStepForm(ApproverType.ROLE_IN_REQUESTER_ORG, null))))
                .isInstanceOf(BusinessException.class).hasMessageContaining("Step 1 role");

        ApprovalWorkflowSummary saved = workflowService.save(newWorkflow("Expense Approval",
                new ApprovalStepForm(ApproverType.DIRECT_SUPERVISOR, null)));
        assertThatThrownBy(() -> workflowService.save(newWorkflow("Expense Approval",
                new ApprovalStepForm(ApproverType.DIRECT_SUPERVISOR, null))))
                .isInstanceOf(BusinessException.class).hasMessageContaining("already used");
        assertThat(workflowService.save(edit(saved, new ApprovalStepForm(ApproverType.DIRECT_SUPERVISOR, null)))
                .name()).isEqualTo("Expense Approval");
    }

    @Test
    void inactiveRoleCannotBeNewlyPickedButCanBeKept() {
        ApprovalWorkflowSummary saved = workflowService.save(newWorkflow("Stock Approval",
                new ApprovalStepForm(ApproverType.ROLE_IN_REQUESTER_ORG, role("SPV"))));
        roleService.setActive(role("SPV"), false);

        assertThat(workflowService.save(edit(saved,
                new ApprovalStepForm(ApproverType.ROLE_IN_REQUESTER_ORG, role("SPV")))).steps()).hasSize(1);
        assertThatThrownBy(() -> workflowService.save(newWorkflow("Another Stock Approval",
                new ApprovalStepForm(ApproverType.ROLE_IN_REQUESTER_ORG, role("SPV")))))
                .isInstanceOf(BusinessException.class).hasMessageContaining("inactive");
    }

    private ApprovalWorkflowForm newWorkflow(String name, ApprovalStepForm... steps) {
        UUID invoice = moduleRepository.findByCode("INVOICE").orElseThrow().getId();
        return new ApprovalWorkflowForm(null, null, name, invoice, null, true, List.of(steps));
    }

    private ApprovalWorkflowForm edit(ApprovalWorkflowSummary workflow, ApprovalStepForm... steps) {
        return new ApprovalWorkflowForm(workflow.id(), workflow.version(), workflow.name(), workflow.moduleId(),
                workflow.description(), workflow.active(), List.of(steps));
    }

    private UUID role(String code) {
        return roleRepository.findByCode(code).orElseThrow().getId();
    }
}
