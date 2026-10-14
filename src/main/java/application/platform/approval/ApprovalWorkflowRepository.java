package application.platform.approval;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ApprovalWorkflowRepository extends JpaRepository<ApprovalWorkflow, UUID> {

    /** Includes soft-deleted rows, which still hold the unique name. */
    @Query(value = "select count(*) from approval_workflows where name = :name", nativeQuery = true)
    long countByNameIncludingDeleted(String name);
}
