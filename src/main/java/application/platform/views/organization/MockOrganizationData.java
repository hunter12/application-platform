package application.platform.views.organization;

/**
 * Static mock organization tree for the Organization Hierarchy prototype
 * page. UI visualization only - not backed by any persistence layer.
 */
final class MockOrganizationData {

    static final OrgUnit ROOT = new OrgUnit("Head Office", "Head Office", "Active",
            new OrgUnit("Regional Jakarta", "Regional", "Active",
                    new OrgUnit("Branch Jakarta Selatan", "Branch", "Active"),
                    new OrgUnit("Branch Jakarta Utara", "Branch", "Active")),
            new OrgUnit("Regional Bandung", "Regional", "Active",
                    new OrgUnit("Branch Bandung 1", "Branch", "Active"),
                    new OrgUnit("Branch Bandung 2", "Branch", "Active")),
            new OrgUnit("IT Division", "Division", "Active",
                    new OrgUnit("Development Department", "Department", "Active"),
                    new OrgUnit("Infrastructure Department", "Department", "Active")));

    private MockOrganizationData() {
    }
}
