package application.platform.user;

import java.util.Set;
import java.util.UUID;

public record UserForm(UUID id, Long version, String username, String password, String fullName, String email,
        String phone, UUID organizationId, UUID supervisorId, Set<UUID> roleIds, boolean active) {
}
