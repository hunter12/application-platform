package application.platform.notification;

import java.time.Instant;

import org.hibernate.annotations.SQLRestriction;

import application.platform.common.BaseEntity;
import application.platform.user.AppUser;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "notifications")
@SQLRestriction("deleted_date is null")
public class Notification extends BaseEntity {

    public static final int TITLE_MAX_LENGTH = 150;
    public static final int MESSAGE_MAX_LENGTH = 1000;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_notifications_recipient"))
    private AppUser recipient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", foreignKey = @ForeignKey(name = "fk_notifications_sender"))
    private AppUser sender;

    @Column(nullable = false, length = TITLE_MAX_LENGTH)
    private String title;

    @Column(nullable = false, length = MESSAGE_MAX_LENGTH)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    private Instant readDate;

    protected Notification() {
    }

    public Notification(AppUser recipient, AppUser sender, NotificationType type, String title, String message) {
        this.recipient = recipient;
        this.sender = sender;
        this.type = type;
        this.title = title;
        this.message = message;
    }

    public boolean isRead() {
        return readDate != null;
    }

    public void markRead() {
        if (readDate == null) {
            readDate = Instant.now();
        }
    }

    public AppUser getRecipient() {
        return recipient;
    }

    /** {@code null} when the notification was created by the system. */
    public AppUser getSender() {
        return sender;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public NotificationType getType() {
        return type;
    }

    public Instant getReadDate() {
        return readDate;
    }
}
