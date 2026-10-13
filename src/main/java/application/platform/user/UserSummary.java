package application.platform.user;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import application.platform.role.Role;

public record UserSummary(UUID id, Long version, String username, String fullName, String email, String phone,
        UUID organizationId, String organizationName, UUID supervisorId, String supervisorName, List<UUID> roleIds,
        List<String> roleNames, boolean active) {

    static UserSummary of(AppUser user, List<Role> roles) {
        List<Role> sorted = roles.stream().sorted(Comparator.comparing(Role::getName)).toList();
        AppUser supervisor = user.getSupervisor();
        return new UserSummary(user.getId(), user.getVersion(), user.getUsername(), user.getFullName(),
                user.getEmail(), user.getPhone(), user.getOrganization().getId(), user.getOrganization().getName(),
                supervisor == null ? null : supervisor.getId(), supervisor == null ? null : supervisor.getFullName(),
                sorted.stream().map(Role::getId).toList(), sorted.stream().map(Role::getName).toList(),
                user.isActive());
    }
}
