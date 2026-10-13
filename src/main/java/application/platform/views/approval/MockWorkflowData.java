package application.platform.views.approval;

import java.util.List;

final class MockWorkflowData {

    static final List<MockWorkflow> ALL = List.of(
            new MockWorkflow("Invoice Approval", "Invoice", 3, "Active"),
            new MockWorkflow("Purchase Approval", "Finance", 4, "Active"),
            new MockWorkflow("Expense Approval", "Finance", 2, "Active"));

    private MockWorkflowData() {
    }
}
