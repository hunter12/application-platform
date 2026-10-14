package application.platform.approval;

import java.util.List;
import java.util.UUID;

public record ApprovalWorkflowForm(UUID id, Long version, String name, UUID moduleId, String description,
        boolean active, List<ApprovalStepForm> steps) {
}
