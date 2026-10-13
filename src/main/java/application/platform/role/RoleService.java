package application.platform.role;

import static application.platform.common.ServiceSupport.checkVersion;
import static application.platform.common.ServiceSupport.ensureUnique;
import static application.platform.common.ServiceSupport.normalizeCode;
import static application.platform.common.ServiceSupport.optionalText;
import static application.platform.common.ServiceSupport.requireText;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import application.platform.common.BusinessException;
import application.platform.permission.Permission;
import application.platform.permission.PermissionRepository;
import application.platform.permission.RolePermission;
import application.platform.permission.RolePermissionRepository;

@Service
@Transactional
public class RoleService {

    /** The role that guarantees someone can still administer the system. */
    public static final String SUPER_ADMIN = "SUPER_ADMIN";

    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public RoleService(RoleRepository roleRepository, UserRoleRepository userRoleRepository,
            PermissionRepository permissionRepository, RolePermissionRepository rolePermissionRepository) {
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    @Transactional(readOnly = true)
    public List<RoleSummary> findAll() {
        Map<UUID, Long> userCounts = new HashMap<>();
        for (Object[] row : userRoleRepository.countActiveUsersPerRole()) {
            userCounts.put((UUID) row[0], (Long) row[1]);
        }
        return roleRepository.findAll(Sort.by("name")).stream()
                .map(role -> RoleSummary.of(role, userCounts.getOrDefault(role.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<RoleSummary> findById(UUID id) {
        return roleRepository.findById(id).map(role -> RoleSummary.of(role, 0));
    }

    /**
     * Creates or updates a role. The code is fixed once created, because it
     * is what authorization checks ({@code ROLE_<code>}) refer to.
     */
    public RoleSummary save(RoleForm form) {
        String name = requireText(form.name(), "Role name");

        Role role;
        if (form.id() == null) {
            String code = normalizeCode(form.code(), "Role code");
            ensureUnique(roleRepository.countByCodeIncludingDeleted(code), false,
                    "Role code " + code + " is already used");
            role = new Role(code, name, optionalText(form.description()));
        } else {
            role = load(form.id());
            checkVersion(role, form.version());
            role.setName(name);
            role.setDescription(optionalText(form.description()));
        }
        applyActive(role, form.active());
        return RoleSummary.of(roleRepository.saveAndFlush(role), 0);
    }

    public void setActive(UUID id, boolean active) {
        applyActive(load(id), active);
    }

    /** Ids of the permissions currently granted to a role. */
    @Transactional(readOnly = true)
    public Set<UUID> findPermissionIds(UUID roleId) {
        return rolePermissionRepository.findByRoleId(roleId).stream()
                .filter(RolePermission::isActive)
                .map(grant -> grant.getPermission().getId())
                .collect(Collectors.toSet());
    }

    /**
     * Makes the role hold exactly the given permissions. Removed grants are
     * switched off rather than deleted, so re-granting reuses the same row
     * and its audit trail.
     */
    public void updatePermissions(UUID roleId, Set<UUID> permissionIds) {
        Role role = load(roleId);
        Set<UUID> existing = new HashSet<>();
        for (RolePermission grant : rolePermissionRepository.findByRoleId(roleId)) {
            UUID permissionId = grant.getPermission().getId();
            existing.add(permissionId);
            grant.setActive(permissionIds.contains(permissionId));
        }
        for (UUID permissionId : permissionIds) {
            if (!existing.contains(permissionId)) {
                Permission permission = permissionRepository.findById(permissionId)
                        .orElseThrow(() -> new BusinessException("Permission not found"));
                rolePermissionRepository.save(new RolePermission(role, permission));
            }
        }
    }

    private void applyActive(Role role, boolean active) {
        if (!active && SUPER_ADMIN.equals(role.getCode())) {
            throw new BusinessException("The Super Admin role cannot be deactivated");
        }
        role.setActive(active);
    }

    private Role load(UUID id) {
        return roleRepository.findById(id).orElseThrow(() -> new BusinessException("Role not found"));
    }
}
