package application.platform.permission;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    Optional<Permission> findByCode(String code);

    /** Includes soft-deleted rows, which still hold the unique code. */
    @Query(value = "select count(*) from permissions where code = :code", nativeQuery = true)
    long countByCodeIncludingDeleted(String code);
}
