package application.platform.views.audit;

import java.util.List;

/**
 * Static mock data for the Audit Log prototype page. UI visualization
 * only - not backed by any persistence layer.
 */
final class MockAuditData {

    static final List<MockAuditEntry> ALL = List.of(
            new MockAuditEntry("17 Sep 2026 10:30", "Abu Yazid", "Users", "CREATE",
                    "Created user \"John Smith\"", "10.0.0.12", "Success"),
            new MockAuditEntry("17 Sep 2026 10:15", "John Smith", "Roles", "UPDATE",
                    "Updated role \"Manager\"", "10.0.0.14", "Success"),
            new MockAuditEntry("17 Sep 2026 09:45", "Jane Doe", "Invoice", "APPROVE",
                    "Approved invoice INV-2026-001", "10.0.0.21", "Success"),
            new MockAuditEntry("17 Sep 2026 09:30", "Budi", "Organization", "UPDATE",
                    "Updated organization \"Branch A\"", "10.0.0.30", "Success"));

    private MockAuditData() {
    }
}
