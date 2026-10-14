package application.platform.delegation;

import java.time.LocalDate;
import java.util.UUID;

public record DelegationForm(UUID id, Long version, UUID delegatorId, UUID delegateId, LocalDate startDate,
        LocalDate endDate, String reason) {
}
