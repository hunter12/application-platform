package application.platform.approval;

import static application.platform.common.ServiceSupport.checkVersion;
import static application.platform.common.ServiceSupport.ensureUnique;
import static application.platform.common.ServiceSupport.optionalText;
import static application.platform.common.ServiceSupport.require;
import static application.platform.common.ServiceSupport.requireText;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import application.platform.common.BusinessException;
import application.platform.module.AppModule;
import application.platform.module.AppModuleRepository;
import application.platform.role.Role;
import application.platform.role.RoleRepository;

@Service
@Transactional
public class ApprovalWorkflowService {

    private final ApprovalWorkflowRepository workflowRepository;
    private final AppModuleRepository moduleRepository;
    private final RoleRepository roleRepository;
    private final ApprovalRuleRepository ruleRepository;

    public ApprovalWorkflowService(ApprovalWorkflowRepository workflowRepository,
            AppModuleRepository moduleRepository, RoleRepository roleRepository,
            ApprovalRuleRepository ruleRepository) {
        this.workflowRepository = workflowRepository;
        this.moduleRepository = moduleRepository;
        this.roleRepository = roleRepository;
        this.ruleRepository = ruleRepository;
    }

    @Transactional(readOnly = true)
    public List<ApprovalWorkflowSummary> findAll() {
        return workflowRepository.findAll(Sort.by("name")).stream().map(ApprovalWorkflowSummary::of).toList();
    }

    @Transactional(readOnly = true)
    public Optional<ApprovalWorkflowSummary> findById(UUID id) {
        return workflowRepository.findById(id).map(ApprovalWorkflowSummary::of);
    }

    public ApprovalWorkflowSummary save(ApprovalWorkflowForm form) {
        String name = requireText(form.name(), "Workflow name");
        AppModule module = moduleRepository.findById(require(form.moduleId(), "Module"))
                .orElseThrow(() -> new BusinessException("Module not found"));
        List<ApprovalStepForm> stepForms = form.steps() == null ? List.of() : form.steps();
        if (stepForms.isEmpty()) {
            throw new BusinessException("A workflow needs at least one approval step");
        }

        ApprovalWorkflow workflow;
        if (form.id() == null) {
            requireActiveModule(module);
            ensureUnique(workflowRepository.countByNameIncludingDeleted(name), false,
                    "Workflow name " + name + " is already used");
            workflow = new ApprovalWorkflow(name, module, optionalText(form.description()));
        } else {
            workflow = load(form.id());
            checkVersion(workflow, form.version());
            if (!module.getId().equals(workflow.getModule().getId())) {
                requireActiveModule(module);
                if (ruleRepository.existsByWorkflowId(workflow.getId())) {
                    throw new BusinessException(
                            "The module cannot be changed because approval rules use this workflow");
                }
            }
            ensureUnique(workflowRepository.countByNameIncludingDeleted(name), name.equals(workflow.getName()),
                    "Workflow name " + name + " is already used");
            workflow.setName(name);
            workflow.setModule(module);
            workflow.setDescription(optionalText(form.description()));
        }
        applyActive(workflow, form.active());
        applySteps(workflow, stepForms);
        return ApprovalWorkflowSummary.of(workflowRepository.saveAndFlush(workflow));
    }

    public void setActive(UUID id, boolean active) {
        applyActive(load(id), active);
    }

    private void applyActive(ApprovalWorkflow workflow, boolean active) {
        if (!active && workflow.getId() != null && ruleRepository.existsByWorkflowIdAndActiveTrue(workflow.getId())) {
            throw new BusinessException("Workflow " + workflow.getName()
                    + " is used by an active approval rule. Deactivate the rule first.");
        }
        workflow.setActive(active);
    }

    /**
     * Existing step rows are updated in place and only the surplus ones are
     * removed, so step numbers never collide with the unique
     * (workflow, step order) constraint while Hibernate flushes.
     */
    private void applySteps(ApprovalWorkflow workflow, List<ApprovalStepForm> stepForms) {
        List<ApprovalWorkflowStep> steps = workflow.getSteps();
        for (int i = 0; i < stepForms.size(); i++) {
            ApprovalStepForm stepForm = stepForms.get(i);
            String label = "Step " + (i + 1);
            ApproverType type = require(stepForm.approverType(), label + " approver type");
            UUID currentRoleId = i < steps.size() && steps.get(i).getRole() != null
                    ? steps.get(i).getRole().getId()
                    : null;
            Role role = type.needsRole() ? loadRole(stepForm.roleId(), currentRoleId, label) : null;
            if (i < steps.size()) {
                steps.get(i).setApproverType(type);
                steps.get(i).setRole(role);
            } else {
                steps.add(new ApprovalWorkflowStep(workflow, i + 1, type, role));
            }
        }
        while (steps.size() > stepForms.size()) {
            steps.remove(steps.size() - 1);
        }
    }

    private void requireActiveModule(AppModule module) {
        if (!module.isActive()) {
            throw new BusinessException("Module " + module.getName() + " is inactive");
        }
    }

    /** A step may keep a role that was deactivated later, but cannot newly pick an inactive one. */
    private Role loadRole(UUID roleId, UUID currentRoleId, String label) {
        Role role = roleRepository.findById(require(roleId, label + " role"))
                .orElseThrow(() -> new BusinessException(label + ": role not found"));
        if (!role.isActive() && !role.getId().equals(currentRoleId)) {
            throw new BusinessException(label + ": role " + role.getName() + " is inactive");
        }
        return role;
    }

    private ApprovalWorkflow load(UUID id) {
        return workflowRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Approval workflow not found"));
    }
}
