package application.platform.approval;

import java.math.BigDecimal;
import java.util.UUID;

public record ApprovalRuleForm(UUID id, Long version, String name, UUID moduleId, BigDecimal minAmount,
        BigDecimal maxAmount, UUID workflowId, String description, boolean active) {
}
