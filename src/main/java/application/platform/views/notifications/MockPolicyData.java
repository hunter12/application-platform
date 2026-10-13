package application.platform.views.notifications;

import java.util.List;

final class MockPolicyData {

    static final List<MockPolicy> ALL = List.of(
            new MockPolicy("Approval Required", "Approval Required", "In-App", "Approver", "Active"),
            new MockPolicy("Approval Completed", "Approval Completed", "In-App", "Requester", "Active"),
            new MockPolicy("Delegation Assigned", "Delegation Assigned", "In-App", "Delegate", "Active"),
            new MockPolicy("System Notification", "System Event", "In-App", "All Users", "Active"));

    private MockPolicyData() {
    }
}
