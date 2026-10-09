package application.platform.views.permissions;

import java.util.List;

/**
 * Static mock data for the Permission Management prototype page. UI
 * visualization only - not backed by any persistence layer.
 */
final class MockPermissionData {

    static final List<MockPermission> ALL = List.of(
            new MockPermission("USER_VIEW", "Users", "View", "Active", "View users"),
            new MockPermission("USER_CREATE", "Users", "Create", "Active", "Create user"),
            new MockPermission("USER_UPDATE", "Users", "Update", "Active", "Update user"),
            new MockPermission("USER_DELETE", "Users", "Delete", "Active", "Delete user"),
            new MockPermission("ROLE_VIEW", "Roles", "View", "Active", "View roles"),
            new MockPermission("ROLE_CREATE", "Roles", "Create", "Active", "Create role"),
            new MockPermission("ORG_VIEW", "Organization", "View", "Active", "View organizations"),
            new MockPermission("ORG_CREATE", "Organization", "Create", "Active", "Create organization"),
            new MockPermission("INVOICE_VIEW", "Invoice", "View", "Active", "View invoices"),
            new MockPermission("INVOICE_CREATE", "Invoice", "Create", "Active", "Create invoice"),
            new MockPermission("INVOICE_APPROVE", "Invoice", "Approve", "Active", "Approve invoice"),
            new MockPermission("INVOICE_EXPORT", "Invoice", "Export", "Active", "Export invoice data"));

    private MockPermissionData() {
    }
}
