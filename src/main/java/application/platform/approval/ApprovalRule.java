package application.platform.approval;

import java.math.BigDecimal;

import org.hibernate.annotations.SQLRestriction;

import application.platform.common.BaseEntity;
import application.platform.module.AppModule;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "approval_rules")
@SQLRestriction("deleted_date is null")
public class ApprovalRule extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "module_id", nullable = false, foreignKey = @ForeignKey(name = "fk_approval_rules_module"))
    private AppModule module;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal minAmount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal maxAmount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workflow_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_approval_rules_workflow"))
    private ApprovalWorkflow workflow;

    @Column(length = 255)
    private String description;

    protected ApprovalRule() {
    }

    public ApprovalRule(String name, AppModule module, BigDecimal minAmount, BigDecimal maxAmount,
            ApprovalWorkflow workflow, String description) {
        this.name = name;
        this.module = module;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.workflow = workflow;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AppModule getModule() {
        return module;
    }

    public void setModule(AppModule module) {
        this.module = module;
    }

    public BigDecimal getMinAmount() {
        return minAmount;
    }

    public void setMinAmount(BigDecimal minAmount) {
        this.minAmount = minAmount;
    }

    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
    }

    public ApprovalWorkflow getWorkflow() {
        return workflow;
    }

    public void setWorkflow(ApprovalWorkflow workflow) {
        this.workflow = workflow;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
