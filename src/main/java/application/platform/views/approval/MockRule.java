package application.platform.views.approval;

/**
 * Static/mock representation of an approval rule row for the Approval
 * Rules prototype page. Not a persistence entity.
 */
record MockRule(String name, String module, String condition, String workflow, int priority, String status) {
}
