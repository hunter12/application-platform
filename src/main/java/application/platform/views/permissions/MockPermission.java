package application.platform.views.permissions;

/**
 * Static/mock representation of a permission row for the Permission
 * Management prototype page. Not a persistence entity - no repository or
 * database backs it.
 */
record MockPermission(String code, String module, String action, String status, String description) {
}
