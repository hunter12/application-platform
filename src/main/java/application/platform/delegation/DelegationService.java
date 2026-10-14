package application.platform.delegation;

import static application.platform.common.ServiceSupport.checkVersion;
import static application.platform.common.ServiceSupport.require;
import static application.platform.common.ServiceSupport.requireText;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import application.platform.common.BusinessException;
import application.platform.common.Dates;
import application.platform.common.JpaAuditingConfig;
import application.platform.organization.Organization;
import application.platform.user.AppUser;
import application.platform.user.AppUserRepository;

@Service
@Transactional
public class DelegationService {

    private final DelegationRepository delegationRepository;
    private final AppUserRepository userRepository;

    public DelegationService(DelegationRepository delegationRepository, AppUserRepository userRepository) {
        this.delegationRepository = delegationRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<DelegationSummary> findAll() {
        LocalDate today = LocalDate.now();
        return delegationRepository.findAllWithUsers().stream()
                .map(delegation -> DelegationSummary.of(delegation, today))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<DelegationSummary> findById(UUID id) {
        return delegationRepository.findById(id).map(delegation -> DelegationSummary.of(delegation, LocalDate.now()));
    }

    /** Active users in the delegator's own organization or one above it. */
    @Transactional(readOnly = true)
    public Set<UUID> findEligibleDelegateIds(UUID delegatorId) {
        AppUser delegator = loadUser(delegatorId);
        return userRepository.findByOrganizationIdIn(organizationAndAncestorIds(delegator.getOrganization()))
                .stream()
                .filter(AppUser::isActive)
                .map(AppUser::getId)
                .filter(id -> !id.equals(delegatorId))
                .collect(Collectors.toSet());
    }

    /**
     * Creates a delegation or edits one that has not started yet. Once a
     * delegation is active it can only be cancelled, so approvals already
     * made on someone's behalf always match the stored period.
     */
    public DelegationSummary save(DelegationForm form) {
        LocalDate today = LocalDate.now();
        AppUser delegator = loadUser(require(form.delegatorId(), "Delegator"));
        AppUser delegate = loadUser(require(form.delegateId(), "Delegate"));
        LocalDate start = require(form.startDate(), "Start date");
        LocalDate end = require(form.endDate(), "End date");
        String reason = requireText(form.reason(), "Reason");

        Delegation delegation = null;
        if (form.id() != null) {
            delegation = load(form.id());
            checkVersion(delegation, form.version());
            if (delegation.statusOn(today) != DelegationStatus.SCHEDULED) {
                throw new BusinessException("Only scheduled delegations can be edited. "
                        + "An active delegation can only be cancelled.");
            }
        }

        if (delegator.getId().equals(delegate.getId())) {
            throw new BusinessException("Delegator and delegate must be different users");
        }
        if (!delegator.isActive()) {
            throw new BusinessException("Delegator " + delegator.getFullName() + " is inactive");
        }
        if (!delegate.isActive()) {
            throw new BusinessException("Delegate " + delegate.getFullName() + " is inactive");
        }
        if (start.isBefore(today)) {
            throw new BusinessException("Start date cannot be in the past");
        }
        if (end.isBefore(start)) {
            throw new BusinessException("End date must not be before the start date");
        }
        if (!organizationAndAncestorIds(delegator.getOrganization()).contains(delegate.getOrganization().getId())) {
            throw new BusinessException("Delegate must be in " + delegator.getFullName()
                    + "'s organization or an organization above it");
        }
        ensureNoConflict(delegator, delegate, start, end, delegation == null ? null : delegation.getId());

        if (delegation == null) {
            delegation = new Delegation(delegator, delegate, start, end, reason);
        } else {
            delegation.setDelegator(delegator);
            delegation.setDelegate(delegate);
            delegation.setStartDate(start);
            delegation.setEndDate(end);
            delegation.setReason(reason);
        }
        return DelegationSummary.of(delegationRepository.saveAndFlush(delegation), today);
    }

    public void cancel(UUID id) {
        Delegation delegation = load(id);
        DelegationStatus status = delegation.statusOn(LocalDate.now());
        if (status != DelegationStatus.SCHEDULED && status != DelegationStatus.ACTIVE) {
            throw new BusinessException("A " + status.getLabel().toLowerCase() + " delegation cannot be cancelled");
        }
        delegation.cancel(JpaAuditingConfig.currentAuditor());
    }

    /**
     * A user hands over approvals for one period at a time, and delegations
     * never chain: the delegate cannot be away themselves, and the delegator
     * cannot pass on approvals they are already handling for someone else.
     */
    private void ensureNoConflict(AppUser delegator, AppUser delegate, LocalDate start, LocalDate end,
            UUID editedId) {
        delegationRepository.findOverlappingAsDelegator(delegator.getId(), start, end).stream()
                .filter(other -> !other.getId().equals(editedId))
                .findFirst()
                .ifPresent(other -> {
                    throw new BusinessException(delegator.getFullName() + " already delegates to "
                            + other.getDelegate().getFullName() + " from " + Dates.format(other.getStartDate())
                            + " to " + Dates.format(other.getEndDate()));
                });
        delegationRepository.findOverlappingAsDelegator(delegate.getId(), start, end).stream()
                .filter(other -> !other.getId().equals(editedId))
                .findFirst()
                .ifPresent(other -> {
                    throw new BusinessException(delegate.getFullName() + " is delegating to "
                            + other.getDelegate().getFullName() + " during this period, so cannot be a delegate");
                });
        delegationRepository.findOverlappingAsDelegate(delegator.getId(), start, end).stream()
                .filter(other -> !other.getId().equals(editedId))
                .findFirst()
                .ifPresent(other -> {
                    throw new BusinessException(delegator.getFullName() + " is acting as delegate for "
                            + other.getDelegator().getFullName() + " during this period and cannot pass it on");
                });
    }

    private static Set<UUID> organizationAndAncestorIds(Organization organization) {
        Set<UUID> ids = new HashSet<>();
        for (Organization current = organization; current != null; current = current.getParent()) {
            ids.add(current.getId());
        }
        return ids;
    }

    private AppUser loadUser(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new BusinessException("User not found"));
    }

    private Delegation load(UUID id) {
        return delegationRepository.findById(id).orElseThrow(() -> new BusinessException("Delegation not found"));
    }
}
