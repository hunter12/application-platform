package application.platform.approval;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.SQLRestriction;

import application.platform.common.BaseEntity;
import application.platform.module.AppModule;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "approval_workflows")
@SQLRestriction("deleted_date is null")
public class ApprovalWorkflow extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "module_id", nullable = false, foreignKey = @ForeignKey(name = "fk_approval_workflows_module"))
    private AppModule module;

    @Column(length = 255)
    private String description;

    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepOrder")
    private List<ApprovalWorkflowStep> steps = new ArrayList<>();

    protected ApprovalWorkflow() {
    }

    public ApprovalWorkflow(String name, AppModule module, String description) {
        this.name = name;
        this.module = module;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<ApprovalWorkflowStep> getSteps() {
        return steps;
    }
}
