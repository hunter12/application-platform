package application.platform.approval;

import java.util.UUID;

public record ApprovalStepForm(ApproverType approverType, UUID roleId) {
}
