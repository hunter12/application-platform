package application.platform.views.audit;

/**
 * Static/mock representation of an audit log row for the Audit Log
 * prototype page. Not a persistence entity.
 */
record MockAuditEntry(String dateTime, String user, String module, String action, String description,
        String ipAddress, String result) {
}
