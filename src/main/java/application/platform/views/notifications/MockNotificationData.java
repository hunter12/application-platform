package application.platform.views.notifications;

import java.util.List;

/**
 * Static mock data for the Notifications prototype page. UI visualization
 * only - not backed by any persistence layer.
 */
final class MockNotificationData {

    static final List<MockNotification> ALL = List.of(
            new MockNotification("Approval Required", "Invoice INV-2026-001 requires your approval.", "Approval",
                    "Unread", "17 Sep 2026 10:30"),
            new MockNotification("Approval Completed", "Invoice INV-2026-002 has been approved.", "Approval", "Read",
                    "17 Sep 2026 09:15"),
            new MockNotification("Delegation Started", "You have been assigned as delegate for Manager Abu.",
                    "Delegation", "Unread", "17 Sep 2026 08:45"),
            new MockNotification("System Update", "Application Platform configuration has been updated.", "System",
                    "Read", "16 Sep 2026 17:00"));

    private MockNotificationData() {
    }
}
