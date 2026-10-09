package application.platform.views.organization;

/**
 * Static/mock representation of an organization row for the Organizations
 * prototype page. Not a persistence entity - no repository or database
 * backs it.
 */
record OrgRow(String code, String name, String type, String parent, String status) {
}
