package application.platform.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByUsername(String username);

    List<AppUser> findBySupervisorId(UUID supervisorId);

    /** All users with organization and supervisor loaded, for list pages. */
    @Query("""
            select u from AppUser u
            join fetch u.organization
            left join fetch u.supervisor
            order by u.username
            """)
    List<AppUser> findAllWithDetails();

    /** Includes soft-deleted rows, which still hold the unique username. */
    @Query(value = "select count(*) from users where username = :username", nativeQuery = true)
    long countByUsernameIncludingDeleted(String username);
}
