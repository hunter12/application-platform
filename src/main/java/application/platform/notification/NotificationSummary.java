package application.platform.notification;

import java.time.Instant;
import java.util.UUID;

import application.platform.user.AppUser;

public record NotificationSummary(UUID id, NotificationType type, String title, String message, String senderName,
        Instant createdDate, Instant readDate) {

    public boolean read() {
        return readDate != null;
    }

    static NotificationSummary of(Notification notification) {
        AppUser sender = notification.getSender();
        return new NotificationSummary(notification.getId(), notification.getType(), notification.getTitle(),
                notification.getMessage(), sender == null ? null : sender.getFullName(),
                notification.getCreatedDate(), notification.getReadDate());
    }
}
