package application.platform.role;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {

    /** All role assignments of a user, including revoked ({@code is_active = false}) ones. */
    List<UserRole> findByUserId(UUID userId);

    /** Active assignments of every user, with the role loaded, for list pages. */
    @Query("""
            select ur from UserRole ur join fetch ur.role r
            where ur.active = true and r.deletedDate is null
            """)
    List<UserRole> findAllActiveWithRole();

    /**
     * Roles currently granted to a user: the assignment and the role must
     * both be active and not deleted.
     */
    @Query("""
            select distinct ur.role from UserRole ur
            where ur.user.id = :userId
              and ur.active = true
              and ur.role.active = true and ur.role.deletedDate is null
            """)
    List<Role> findActiveRolesByUserId(UUID userId);

    /** Number of active users holding each role, as {@code [roleId, count]} rows. */
    @Query("""
            select ur.role.id, count(distinct ur.user.id) from UserRole ur
            where ur.active = true and ur.user.active = true and ur.user.deletedDate is null
            group by ur.role.id
            """)
    List<Object[]> countActiveUsersPerRole();
}
