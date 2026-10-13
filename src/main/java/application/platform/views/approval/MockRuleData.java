package application.platform.views.approval;

import java.util.List;

final class MockRuleData {

    static final List<MockRule> ALL = List.of(
            new MockRule("Invoice <= 10M", "Invoice", "Amount <= 10,000,000", "Invoice Approval", 1, "Active"),
            new MockRule("Invoice 10M - 100M", "Invoice",
                    "Amount > 10,000,000 AND Amount <= 100,000,000", "Invoice Approval", 2, "Active"),
            new MockRule("Invoice > 100M", "Invoice", "Amount > 100,000,000", "Invoice Approval", 3, "Active"));

    private MockRuleData() {
    }
}
