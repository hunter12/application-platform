package application.platform.views.approval;

import java.util.List;

/**
 * Static mock data for the Approval Workflows prototype page. UI
 * visualization only - not backed by any persistence layer.
 */
final class MockWorkflowData {

    static final List<MockWorkflow> ALL = List.of(
            new MockWorkflow("Invoice Approval", "Invoice", 3, "Active"),
            new MockWorkflow("Purchase Approval", "Finance", 4, "Active"),
            new MockWorkflow("Expense Approval", "Finance", 2, "Active"));

    private MockWorkflowData() {
    }
}
