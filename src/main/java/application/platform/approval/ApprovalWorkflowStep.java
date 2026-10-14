package application.platform.approval;

import org.hibernate.annotations.SQLRestriction;

import application.platform.common.BaseEntity;
import application.platform.role.Role;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "approval_workflow_steps", uniqueConstraints = @UniqueConstraint(
        name = "uk_approval_workflow_steps_order", columnNames = { "workflow_id", "step_order" }))
@SQLRestriction("deleted_date is null")
public class ApprovalWorkflowStep extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workflow_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_approval_workflow_steps_workflow"))
    private ApprovalWorkflow workflow;

    @Column(nullable = false)
    private int stepOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ApproverType approverType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", foreignKey = @ForeignKey(name = "fk_approval_workflow_steps_role"))
    private Role role;

    protected ApprovalWorkflowStep() {
    }

    public ApprovalWorkflowStep(ApprovalWorkflow workflow, int stepOrder, ApproverType approverType, Role role) {
        this.workflow = workflow;
        this.stepOrder = stepOrder;
        this.approverType = approverType;
        this.role = role;
    }

    public ApprovalWorkflow getWorkflow() {
        return workflow;
    }

    public int getStepOrder() {
        return stepOrder;
    }

    public ApproverType getApproverType() {
        return approverType;
    }

    public void setApproverType(ApproverType approverType) {
        this.approverType = approverType;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
