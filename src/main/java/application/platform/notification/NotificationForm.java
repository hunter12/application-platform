package application.platform.notification;

import java.util.UUID;

public record NotificationForm(UUID recipientId, NotificationType type, String title, String message) {
}
