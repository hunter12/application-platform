package application.platform.views.organization;

import java.util.List;

/**
 * Static mock data for the Organizations prototype page. UI visualization
 * only - not backed by any persistence layer.
 */
final class MockOrgRowData {

    static final List<OrgRow> ALL = List.of(
            new OrgRow("HO", "Head Office", "Head Office", "-", "Active"),
            new OrgRow("REG-JKT", "Regional Jakarta", "Regional", "Head Office", "Active"),
            new OrgRow("REG-BDG", "Regional Bandung", "Regional", "Head Office", "Active"),
            new OrgRow("BR-JKT-01", "Branch Jakarta Selatan", "Branch", "Regional Jakarta", "Active"),
            new OrgRow("BR-JKT-02", "Branch Jakarta Utara", "Branch", "Regional Jakarta", "Active"),
            new OrgRow("IT", "IT Division", "Division", "Head Office", "Active"),
            new OrgRow("DEV", "Development Department", "Department", "IT Division", "Active"),
            new OrgRow("INFRA", "Infrastructure Department", "Department", "IT Division", "Active"));

    private MockOrgRowData() {
    }
}
