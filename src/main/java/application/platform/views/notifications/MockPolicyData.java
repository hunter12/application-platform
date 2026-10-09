package application.platform.views.notifications;

import java.util.List;

/**
 * Static mock data for the Notification Policy prototype page. UI
 * visualization only - not backed by any persistence layer.
 */
final class MockPolicyData {

    static final List<MockPolicy> ALL = List.of(
            new MockPolicy("Approval Required", "Approval Required", "In-App", "Approver", "Active"),
            new MockPolicy("Approval Completed", "Approval Completed", "In-App", "Requester", "Active"),
            new MockPolicy("Delegation Assigned", "Delegation Assigned", "In-App", "Delegate", "Active"),
            new MockPolicy("System Notification", "System Event", "In-App", "All Users", "Active"));

    private MockPolicyData() {
    }
}
