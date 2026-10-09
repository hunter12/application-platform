package application.platform.views.approval;

import java.util.List;

/**
 * Static mock data for the Delegation prototype page. UI visualization
 * only - not backed by any persistence layer.
 */
final class MockDelegationData {

    static final List<MockDelegation> ALL = List.of(
            new MockDelegation("Manager Abu", "SPV Budi", "20 Sep 2026", "30 Sep 2026", "Annual Leave", "Scheduled"),
            new MockDelegation("Director John Smith", "Manager Jane Doe", "18 Sep 2026", "25 Sep 2026",
                    "Business Trip", "Active"));

    private MockDelegationData() {
    }
}
