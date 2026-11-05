package application.platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import application.platform.common.BusinessException;
import application.platform.notification.NotificationForm;
import application.platform.notification.NotificationService;
import application.platform.notification.NotificationSummary;
import application.platform.notification.NotificationType;
import application.platform.organization.OrganizationRepository;
import application.platform.user.UserForm;
import application.platform.user.UserService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("dev")
@Transactional
@WithMockUser(username = "admin")
class NotificationServiceTests {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserService userService;

    @Autowired
    private OrganizationRepository organizationRepository;

    private UUID bob;
    private UUID carol;

    @BeforeEach
    void createUsers() {
        bob = user("bob", true);
        carol = user("carol", true);
    }

    @Test
    void recipientSeesOnlyOwnNotifications() {
        notificationService.send(form(bob, "Maintenance tonight"));
        notificationService.send(form(carol, "Welcome"));

        assertThat(notificationService.findMine()).isEmpty();

        signInAs("bob");
        List<NotificationSummary> inbox = notificationService.findMine();
        assertThat(inbox).extracting(NotificationSummary::title).containsExactly("Maintenance tonight");
        assertThat(inbox.getFirst().senderName()).isEqualTo("Administrator");
        assertThat(inbox.getFirst().read()).isFalse();
        assertThat(notificationService.countMyUnread()).isEqualTo(1);
    }

    @Test
    void markAsReadAndDeleteWorkOnlyOnOwnInbox() {
        notificationService.send(form(bob, "First"));
        notificationService.send(form(bob, "Second"));
        notificationService.send(form(bob, "Third"));

        UUID bobsFirst = idOf("bob", "First");
        signInAs("carol");
        assertThatThrownBy(() -> notificationService.markAsRead(bobsFirst))
                .isInstanceOf(BusinessException.class).hasMessageContaining("not found");
        assertThatThrownBy(() -> notificationService.delete(bobsFirst))
                .isInstanceOf(BusinessException.class).hasMessageContaining("not found");

        signInAs("bob");
        notificationService.markAsRead(bobsFirst);
        assertThat(notificationService.countMyUnread()).isEqualTo(2);

        notificationService.markAllAsRead();
        assertThat(notificationService.countMyUnread()).isZero();

        notificationService.delete(bobsFirst);
        assertThat(notificationService.findMine()).extracting(NotificationSummary::title)
                .containsExactlyInAnyOrder("Second", "Third");
    }

    @Test
    void validatesAnnouncements() {
        assertThatThrownBy(() -> notificationService.send(
                new NotificationForm(bob, NotificationType.APPROVAL, "Approve", "Please approve")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("sent by the system");
        assertThatThrownBy(() -> notificationService.send(
                new NotificationForm(bob, NotificationType.SYSTEM, " ", "Body")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("Title is required");
        assertThatThrownBy(() -> notificationService.send(
                new NotificationForm(bob, NotificationType.SYSTEM, "Title", "x".repeat(1001))))
                .isInstanceOf(BusinessException.class).hasMessageContaining("at most 1000");
        assertThatThrownBy(() -> notificationService.send(form(null, "Title")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("Recipient is required");

        UUID inactive = user("dave", false);
        assertThatThrownBy(() -> notificationService.send(form(inactive, "Title")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("inactive");
    }

    private UUID idOf(String username, String title) {
        signInAs(username);
        return notificationService.findMine().stream()
                .filter(notification -> notification.title().equals(title))
                .findFirst().orElseThrow().id();
    }

    private static void signInAs(String username) {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(username, null, List.of()));
    }

    private NotificationForm form(UUID recipient, String title) {
        return new NotificationForm(recipient, NotificationType.INFORMATION, title, "Details for " + title);
    }

    private UUID user(String username, boolean active) {
        UUID organization = organizationRepository.findByCode("HO").orElseThrow().getId();
        return userService.save(new UserForm(null, null, username, "secret123", username, null, null,
                organization, null, Set.of(), active)).id();
    }
}
