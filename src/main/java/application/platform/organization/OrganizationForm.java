package application.platform.organization;

import java.util.UUID;

public record OrganizationForm(UUID id, Long version, String code, String name, OrganizationType type,
        UUID parentId, boolean active) {
}
