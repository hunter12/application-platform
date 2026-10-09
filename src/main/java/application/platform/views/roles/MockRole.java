package application.platform.views.roles;

/**
 * Static/mock representation of a role row for the Role Management
 * prototype page. Not a persistence entity - no repository or database
 * backs it.
 */
record MockRole(String name, String description, int userCount, String status) {
}
