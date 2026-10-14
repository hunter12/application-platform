package application.platform.approval;

import java.util.List;
import java.util.UUID;

public record ApprovalWorkflowSummary(UUID id, Long version, String name, UUID moduleId, String moduleName,
        String description, boolean active, List<ApprovalStepSummary> steps) {

    static ApprovalWorkflowSummary of(ApprovalWorkflow workflow) {
        return new ApprovalWorkflowSummary(workflow.getId(), workflow.getVersion(), workflow.getName(),
                workflow.getModule().getId(), workflow.getModule().getName(), workflow.getDescription(),
                workflow.isActive(), workflow.getSteps().stream().map(ApprovalStepSummary::of).toList());
    }
}
