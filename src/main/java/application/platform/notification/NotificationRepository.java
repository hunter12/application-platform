package application.platform.notification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    @Query("""
            select n from Notification n
            left join fetch n.sender
            where n.recipient.username = :username
            order by n.createdDate desc
            """)
    List<Notification> findInbox(String username);

    @Query("""
            select n from Notification n
            where n.recipient.username = :username and n.readDate is null
            """)
    List<Notification> findUnread(String username);

    @Query("""
            select count(n) from Notification n
            where n.recipient.username = :username and n.readDate is null
            """)
    long countUnread(String username);

    @Query("""
            select n from Notification n
            left join fetch n.sender
            where n.id = :id and n.recipient.username = :username
            """)
    Optional<Notification> findInInbox(UUID id, String username);
}
