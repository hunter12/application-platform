package application.platform.permission;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RolePermissionRepository extends JpaRepository<RolePermission, UUID> {

    /** All grants of a role, including revoked ({@code is_active = false}) ones. */
    List<RolePermission> findByRoleId(UUID roleId);

    /**
     * Permission codes a user currently holds through their active roles.
     * Every link in the chain (role assignment, role, grant, permission)
     * must be active and not deleted.
     */
    @Query("""
            select distinct rp.permission.code from RolePermission rp
            where rp.active = true
              and rp.permission.active = true and rp.permission.deletedDate is null
              and rp.role.active = true and rp.role.deletedDate is null
              and rp.role in (
                  select ur.role from application.platform.role.UserRole ur
                  where ur.user.id = :userId and ur.active = true and ur.deletedDate is null)
            """)
    List<String> findActivePermissionCodesByUserId(UUID userId);
}
