package application.platform.role;

import java.util.UUID;

public record RoleForm(UUID id, Long version, String code, String name, String description, boolean active) {
}
