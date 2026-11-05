package application.platform.notification;

import static application.platform.common.ServiceSupport.require;
import static application.platform.common.ServiceSupport.requireText;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import application.platform.common.BusinessException;
import application.platform.common.JpaAuditingConfig;
import application.platform.user.AppUser;
import application.platform.user.AppUserRepository;

/**
 * Every read and change works on the signed-in user's own inbox, so nobody,
 * including admins, can see or touch another user's notifications.
 */
@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final AppUserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, AppUserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<NotificationSummary> findMine() {
        return notificationRepository.findInbox(currentUsername()).stream().map(NotificationSummary::of).toList();
    }

    @Transactional(readOnly = true)
    public long countMyUnread() {
        return notificationRepository.countUnread(currentUsername());
    }

    /** Sends an announcement from the signed-in user to one recipient. */
    public void send(NotificationForm form) {
        AppUser recipient = userRepository.findById(require(form.recipientId(), "Recipient"))
                .orElseThrow(() -> new BusinessException("User not found"));
        NotificationType type = require(form.type(), "Notification type");
        String title = requireText(form.title(), "Title");
        String message = requireText(form.message(), "Message");

        if (!type.isManual()) {
            throw new BusinessException(type.getLabel() + " notifications are sent by the system");
        }
        if (!recipient.isActive()) {
            throw new BusinessException("Recipient " + recipient.getFullName() + " is inactive");
        }
        checkLength(title, Notification.TITLE_MAX_LENGTH, "Title");
        checkLength(message, Notification.MESSAGE_MAX_LENGTH, "Message");

        AppUser sender = userRepository.findByUsername(currentUsername()).orElse(null);
        notificationRepository.save(new Notification(recipient, sender, type, title, message));
    }

    public void markAsRead(UUID id) {
        loadMine(id).markRead();
    }

    public void markAllAsRead() {
        notificationRepository.findUnread(currentUsername()).forEach(Notification::markRead);
    }

    public void delete(UUID id) {
        loadMine(id).markDeleted(currentUsername());
    }

    private Notification loadMine(UUID id) {
        return notificationRepository.findInInbox(id, currentUsername())
                .orElseThrow(() -> new BusinessException("Notification not found"));
    }

    private static void checkLength(String value, int maxLength, String label) {
        if (value.length() > maxLength) {
            throw new BusinessException(label + " must be at most " + maxLength + " characters");
        }
    }

    private static String currentUsername() {
        return JpaAuditingConfig.currentAuditor();
    }
}
