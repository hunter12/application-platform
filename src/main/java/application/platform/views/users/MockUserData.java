package application.platform.views.users;

import java.util.List;

/**
 * Static mock data for the Users prototype page (MVP-02.5). UI visualization
 * only - not backed by any persistence layer.
 */
final class MockUserData {

    static final List<MockUser> ALL = List.of(
            new MockUser("jsmith", "John Smith", "Head Office", "-", "Active"),
            new MockUser("jdoe", "Jane Doe", "Regional Jakarta", "John Smith", "Active"),
            new MockUser("bwayne", "Bruce Wayne", "Branch A", "Jane Doe", "Active"),
            new MockUser("ckent", "Clark Kent", "Branch B", "Jane Doe", "Inactive"),
            new MockUser("dprince", "Diana Prince", "Regional Bandung", "John Smith", "Active"),
            new MockUser("bbanner", "Bruce Banner", "Branch C", "Diana Prince", "Active"),
            new MockUser("tstark", "Tony Stark", "Branch D", "Diana Prince", "Inactive"));

    private MockUserData() {
    }
}
