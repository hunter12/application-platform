package application.platform.organization;

import java.util.UUID;

public record OrganizationSummary(UUID id, Long version, String code, String name, OrganizationType type,
        UUID parentId, String parentName, boolean active) {

    static OrganizationSummary of(Organization organization) {
        Organization parent = organization.getParent();
        return new OrganizationSummary(organization.getId(), organization.getVersion(), organization.getCode(),
                organization.getName(), organization.getType(), parent == null ? null : parent.getId(),
                parent == null ? null : parent.getName(), organization.isActive());
    }
}
