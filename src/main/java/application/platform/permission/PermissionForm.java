package application.platform.permission;

import java.util.UUID;

public record PermissionForm(UUID id, Long version, String code, String name, String module, String action,
        String description, boolean active) {
}
