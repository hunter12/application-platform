package application.platform.views.notifications;

/**
 * Static/mock representation of a notification policy row for the
 * Notification Policy prototype page. Not a persistence entity.
 */
record MockPolicy(String name, String event, String channel, String recipient, String status) {
}
