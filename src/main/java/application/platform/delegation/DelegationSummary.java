package application.platform.delegation;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record DelegationSummary(UUID id, Long version, UUID delegatorId, String delegatorName, UUID delegateId,
        String delegateName, LocalDate startDate, LocalDate endDate, String reason, DelegationStatus status,
        String createdBy, Instant cancelledDate, String cancelledBy) {

    static DelegationSummary of(Delegation delegation, LocalDate today) {
        return new DelegationSummary(delegation.getId(), delegation.getVersion(),
                delegation.getDelegator().getId(), delegation.getDelegator().getFullName(),
                delegation.getDelegate().getId(), delegation.getDelegate().getFullName(),
                delegation.getStartDate(), delegation.getEndDate(), delegation.getReason(),
                delegation.statusOn(today), delegation.getCreatedBy(), delegation.getCancelledDate(),
                delegation.getCancelledBy());
    }
}
