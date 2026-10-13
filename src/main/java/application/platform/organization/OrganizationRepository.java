package application.platform.organization;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    Optional<Organization> findByCode(String code);

    /** Includes soft-deleted rows, which still hold the unique code. */
    @Query(value = "select count(*) from organizations where code = :code", nativeQuery = true)
    long countByCodeIncludingDeleted(String code);
}
