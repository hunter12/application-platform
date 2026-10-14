package application.platform.approval;

import java.util.UUID;

public record ApprovalStepSummary(int stepOrder, ApproverType approverType, UUID roleId, String roleName) {

    static ApprovalStepSummary of(ApprovalWorkflowStep step) {
        return new ApprovalStepSummary(step.getStepOrder(), step.getApproverType(),
                step.getRole() == null ? null : step.getRole().getId(),
                step.getRole() == null ? null : step.getRole().getName());
    }
}
