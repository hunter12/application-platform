package application.platform.role;

import java.util.UUID;

public record RoleSummary(UUID id, Long version, String code, String name, String description, long userCount,
        boolean active) {

    static RoleSummary of(Role role, long userCount) {
        return new RoleSummary(role.getId(), role.getVersion(), role.getCode(), role.getName(),
                role.getDescription(), userCount, role.isActive());
    }
}
