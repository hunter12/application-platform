package application.platform.permission;

import java.util.UUID;

public record PermissionSummary(UUID id, Long version, String code, String name, String module, String action,
        String description, boolean active) {

    static PermissionSummary of(Permission permission) {
        return new PermissionSummary(permission.getId(), permission.getVersion(), permission.getCode(),
                permission.getName(), permission.getModule(), permission.getAction(), permission.getDescription(),
                permission.isActive());
    }
}
