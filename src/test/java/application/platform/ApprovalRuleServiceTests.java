package application.platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import application.platform.approval.ApprovalRuleForm;
import application.platform.approval.ApprovalRuleService;
import application.platform.approval.ApprovalRuleSummary;
import application.platform.approval.ApprovalStepForm;
import application.platform.approval.ApprovalWorkflowForm;
import application.platform.approval.ApprovalWorkflowService;
import application.platform.approval.ApprovalWorkflowSummary;
import application.platform.approval.ApproverType;
import application.platform.common.BusinessException;
import application.platform.module.AppModuleRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("dev")
@Transactional
@WithMockUser(username = "admin")
class ApprovalRuleServiceTests {

    @Autowired
    private ApprovalRuleService ruleService;

    @Autowired
    private ApprovalWorkflowService workflowService;

    @Autowired
    private AppModuleRepository moduleRepository;

    private ApprovalWorkflowSummary invoiceWorkflow;

    @BeforeEach
    void createWorkflow() {
        invoiceWorkflow = workflow("Invoice Approval", "INVOICE");
    }

    @Test
    void adjacentRangesAreAllowedButOverlapsAreRejected() {
        ruleService.save(rule("Small", "0", "10000000"));
        ApprovalRuleSummary medium = ruleService.save(rule("Medium", "10000000.01", "100000000"));

        assertThat(medium.minAmount()).isEqualByComparingTo("10000000.01");
        assertThatThrownBy(() -> ruleService.save(rule("Overlap", "10000000", "20000000")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("overlaps with rule \"Small\"");
        assertThatThrownBy(() -> ruleService.save(rule("Inside", "50000000", "60000000")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("\"Medium\"");
    }

    @Test
    void inactiveRulesDoNotBlockAndActivationRechecksOverlap() {
        ApprovalRuleSummary old = ruleService.save(rule("Old", "0", "50000000"));
        ruleService.setActive(old.id(), false);
        ruleService.save(rule("New", "0", "10000000"));

        assertThatThrownBy(() -> ruleService.setActive(old.id(), true))
                .isInstanceOf(BusinessException.class).hasMessageContaining("\"New\"");
    }

    @Test
    void editingKeepsItsOwnRangeWithoutConflict() {
        ApprovalRuleSummary saved = ruleService.save(rule("Small", "0", "10000000"));

        ApprovalRuleSummary edited = ruleService.save(new ApprovalRuleForm(saved.id(), saved.version(),
                "Small invoices", saved.moduleId(), new BigDecimal("0"), new BigDecimal("15000000"),
                saved.workflowId(), null, true));

        assertThat(edited.name()).isEqualTo("Small invoices");
        assertThat(edited.maxAmount()).isEqualByComparingTo("15000000");
    }

    @Test
    void validatesAmountsAndWorkflowModule() {
        assertThatThrownBy(() -> ruleService.save(rule("Reversed", "100", "10")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("lower than");
        assertThatThrownBy(() -> ruleService.save(rule("Negative", "-1", "10")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("negative");
        assertThatThrownBy(() -> ruleService.save(rule("Decimals", "0", "10.001")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("2 decimals");
        assertThatThrownBy(() -> ruleService.save(rule("Too big", "0", "10000000000000")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("must not exceed");

        UUID finance = moduleRepository.findByCode("FINANCE").orElseThrow().getId();
        assertThatThrownBy(() -> ruleService.save(new ApprovalRuleForm(null, null, "Wrong module", finance,
                BigDecimal.ZERO, BigDecimal.TEN, invoiceWorkflow.id(), null, true)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("belongs to module Invoice");
    }

    @Test
    void workflowUsedByActiveRuleCannotBeDeactivatedOrMoved() {
        ApprovalRuleSummary saved = ruleService.save(rule("Small", "0", "10000000"));

        assertThatThrownBy(() -> workflowService.setActive(invoiceWorkflow.id(), false))
                .isInstanceOf(BusinessException.class).hasMessageContaining("active approval rule");

        UUID finance = moduleRepository.findByCode("FINANCE").orElseThrow().getId();
        assertThatThrownBy(() -> workflowService.save(new ApprovalWorkflowForm(invoiceWorkflow.id(),
                invoiceWorkflow.version(), invoiceWorkflow.name(), finance, null, true,
                List.of(new ApprovalStepForm(ApproverType.DIRECT_SUPERVISOR, null)))))
                .isInstanceOf(BusinessException.class).hasMessageContaining("module cannot be changed");

        ruleService.setActive(saved.id(), false);
        workflowService.setActive(invoiceWorkflow.id(), false);
        assertThatThrownBy(() -> ruleService.setActive(saved.id(), true))
                .isInstanceOf(BusinessException.class).hasMessageContaining("is inactive");
    }

    private ApprovalRuleForm rule(String name, String min, String max) {
        return new ApprovalRuleForm(null, null, name, invoiceWorkflow.moduleId(), new BigDecimal(min),
                new BigDecimal(max), invoiceWorkflow.id(), null, true);
    }

    private ApprovalWorkflowSummary workflow(String name, String moduleCode) {
        UUID module = moduleRepository.findByCode(moduleCode).orElseThrow().getId();
        return workflowService.save(new ApprovalWorkflowForm(null, null, name, module, null, true,
                List.of(new ApprovalStepForm(ApproverType.DIRECT_SUPERVISOR, null))));
    }
}
