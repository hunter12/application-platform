package application.platform.views.roles;

import java.util.List;

/**
 * Static mock data for the Role Management prototype page. UI
 * visualization only - not backed by any persistence layer.
 */
final class MockRoleData {

    static final List<MockRole> ALL = List.of(
            new MockRole("Super Admin", "Full system access", 2, "Active"),
            new MockRole("Admin", "System administration", 5, "Active"),
            new MockRole("Director", "Director level", 12, "Active"),
            new MockRole("Manager", "Manager level", 28, "Active"),
            new MockRole("SPV", "Supervisor level", 45, "Active"),
            new MockRole("Staff", "Operational user", 156, "Active"));

    private MockRoleData() {
    }
}
