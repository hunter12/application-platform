package application.platform.delegation;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DelegationRepository extends JpaRepository<Delegation, UUID> {

    @Query("""
            select d from Delegation d
            join fetch d.delegator
            join fetch d.delegate
            order by d.startDate desc, d.createdDate desc
            """)
    List<Delegation> findAllWithUsers();

    /** Delegations not cancelled where the user gives away approvals during {@code [start, end]}. */
    @Query("""
            select d from Delegation d
            where d.cancelledDate is null and d.delegator.id = :userId
              and d.startDate <= :end and d.endDate >= :start
            """)
    List<Delegation> findOverlappingAsDelegator(UUID userId, LocalDate start, LocalDate end);

    /** Delegations not cancelled where the user acts for someone else during {@code [start, end]}. */
    @Query("""
            select d from Delegation d
            where d.cancelledDate is null and d.delegate.id = :userId
              and d.startDate <= :end and d.endDate >= :start
            """)
    List<Delegation> findOverlappingAsDelegate(UUID userId, LocalDate start, LocalDate end);
}
