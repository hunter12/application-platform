package application.platform.views.approval;

/**
 * Static/mock representation of an approval workflow row for the Approval
 * Workflows prototype page. Not a persistence entity.
 */
record MockWorkflow(String name, String module, int steps, String status) {
}
