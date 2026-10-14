package application.platform.approval;

import static application.platform.common.ServiceSupport.checkVersion;
import static application.platform.common.ServiceSupport.ensureUnique;
import static application.platform.common.ServiceSupport.optionalText;
import static application.platform.common.ServiceSupport.require;
import static application.platform.common.ServiceSupport.requireText;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import application.platform.common.Amounts;
import application.platform.common.BusinessException;
import application.platform.module.AppModule;
import application.platform.module.AppModuleRepository;

@Service
@Transactional
public class ApprovalRuleService {

    private final ApprovalRuleRepository ruleRepository;
    private final ApprovalWorkflowRepository workflowRepository;
    private final AppModuleRepository moduleRepository;

    public ApprovalRuleService(ApprovalRuleRepository ruleRepository, ApprovalWorkflowRepository workflowRepository,
            AppModuleRepository moduleRepository) {
        this.ruleRepository = ruleRepository;
        this.workflowRepository = workflowRepository;
        this.moduleRepository = moduleRepository;
    }

    @Transactional(readOnly = true)
    public List<ApprovalRuleSummary> findAll() {
        return ruleRepository.findAll(Sort.by("module.name", "minAmount")).stream()
                .map(ApprovalRuleSummary::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<ApprovalRuleSummary> findById(UUID id) {
        return ruleRepository.findById(id).map(ApprovalRuleSummary::of);
    }

    public ApprovalRuleSummary save(ApprovalRuleForm form) {
        String name = requireText(form.name(), "Rule name");
        AppModule module = moduleRepository.findById(require(form.moduleId(), "Module"))
                .orElseThrow(() -> new BusinessException("Module not found"));
        BigDecimal min = amount(form.minAmount(), "Minimum amount");
        BigDecimal max = amount(form.maxAmount(), "Maximum amount");
        if (max.compareTo(min) < 0) {
            throw new BusinessException("Maximum amount must not be lower than the minimum amount");
        }
        ApprovalWorkflow workflow = workflowRepository.findById(require(form.workflowId(), "Workflow"))
                .orElseThrow(() -> new BusinessException("Approval workflow not found"));
        if (!workflow.getModule().getId().equals(module.getId())) {
            throw new BusinessException("Workflow " + workflow.getName() + " belongs to module "
                    + workflow.getModule().getName() + ", not " + module.getName());
        }

        ApprovalRule rule = form.id() == null ? null : load(form.id());
        if (rule == null) {
            ensureUnique(ruleRepository.countByNameIncludingDeleted(name), false,
                    "Rule name " + name + " is already used");
        } else {
            checkVersion(rule, form.version());
            ensureUnique(ruleRepository.countByNameIncludingDeleted(name), name.equals(rule.getName()),
                    "Rule name " + name + " is already used");
        }
        if ((rule == null || !rule.getModule().getId().equals(module.getId())) && !module.isActive()) {
            throw new BusinessException("Module " + module.getName() + " is inactive");
        }
        boolean workflowChanged = rule == null || !rule.getWorkflow().getId().equals(workflow.getId());
        if ((workflowChanged || form.active()) && !workflow.isActive()) {
            throw new BusinessException("Workflow " + workflow.getName() + " is inactive");
        }
        if (form.active()) {
            ensureNoOverlap(module.getId(), min, max, rule == null ? null : rule.getId());
        }

        if (rule == null) {
            rule = new ApprovalRule(name, module, min, max, workflow, optionalText(form.description()));
        } else {
            rule.setName(name);
            rule.setModule(module);
            rule.setMinAmount(min);
            rule.setMaxAmount(max);
            rule.setWorkflow(workflow);
            rule.setDescription(optionalText(form.description()));
        }
        rule.setActive(form.active());
        return ApprovalRuleSummary.of(ruleRepository.saveAndFlush(rule));
    }

    public void setActive(UUID id, boolean active) {
        ApprovalRule rule = load(id);
        if (active) {
            if (!rule.getWorkflow().isActive()) {
                throw new BusinessException("Workflow " + rule.getWorkflow().getName() + " is inactive");
            }
            ensureNoOverlap(rule.getModule().getId(), rule.getMinAmount(), rule.getMaxAmount(), rule.getId());
        }
        rule.setActive(active);
    }

    private void ensureNoOverlap(UUID moduleId, BigDecimal min, BigDecimal max, UUID ruleId) {
        ruleRepository.findActiveOverlapping(moduleId, min, max).stream()
                .filter(other -> !other.getId().equals(ruleId))
                .findFirst()
                .ifPresent(other -> {
                    throw new BusinessException("Amount range overlaps with rule \"" + other.getName() + "\" ("
                            + Amounts.format(other.getMinAmount()) + " - " + Amounts.format(other.getMaxAmount())
                            + ")");
                });
    }

    private static BigDecimal amount(BigDecimal value, String label) {
        require(value, label);
        if (value.signum() < 0) {
            throw new BusinessException(label + " must not be negative");
        }
        if (value.compareTo(Amounts.MAX) > 0) {
            throw new BusinessException(label + " must not exceed " + Amounts.format(Amounts.MAX));
        }
        if (value.stripTrailingZeros().scale() > 2) {
            throw new BusinessException(label + " may have at most 2 decimals");
        }
        return value.setScale(2);
    }

    private ApprovalRule load(UUID id) {
        return ruleRepository.findById(id).orElseThrow(() -> new BusinessException("Approval rule not found"));
    }
}
