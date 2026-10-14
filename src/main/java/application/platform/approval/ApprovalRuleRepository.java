package application.platform.approval;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ApprovalRuleRepository extends JpaRepository<ApprovalRule, UUID> {

    /** Includes soft-deleted rows, which still hold the unique name. */
    @Query(value = "select count(*) from approval_rules where name = :name", nativeQuery = true)
    long countByNameIncludingDeleted(String name);

    /** Active rules of a module whose inclusive amount range intersects {@code [min, max]}. */
    @Query("""
            select r from ApprovalRule r
            where r.active = true and r.module.id = :moduleId
              and r.minAmount <= :max and r.maxAmount >= :min
            order by r.minAmount
            """)
    List<ApprovalRule> findActiveOverlapping(UUID moduleId, BigDecimal min, BigDecimal max);

    boolean existsByWorkflowId(UUID workflowId);

    boolean existsByWorkflowIdAndActiveTrue(UUID workflowId);
}
