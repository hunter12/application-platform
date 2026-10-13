package application.platform.views.audit;

record MockAuditEntry(String dateTime, String user, String module, String action, String description,
        String ipAddress, String result) {
}
