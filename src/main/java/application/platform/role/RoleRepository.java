package application.platform.role;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByCode(String code);

    /** Includes soft-deleted rows, which still hold the unique code. */
    @Query(value = "select count(*) from roles where code = :code", nativeQuery = true)
    long countByCodeIncludingDeleted(String code);
}
