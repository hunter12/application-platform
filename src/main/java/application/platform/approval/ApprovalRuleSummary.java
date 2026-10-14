package application.platform.approval;

import java.math.BigDecimal;
import java.util.UUID;

public record ApprovalRuleSummary(UUID id, Long version, String name, UUID moduleId, String moduleName,
        BigDecimal minAmount, BigDecimal maxAmount, UUID workflowId, String workflowName, String description,
        boolean active) {

    static ApprovalRuleSummary of(ApprovalRule rule) {
        return new ApprovalRuleSummary(rule.getId(), rule.getVersion(), rule.getName(), rule.getModule().getId(),
                rule.getModule().getName(), rule.getMinAmount(), rule.getMaxAmount(), rule.getWorkflow().getId(),
                rule.getWorkflow().getName(), rule.getDescription(), rule.isActive());
    }
}
