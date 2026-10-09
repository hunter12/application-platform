package application.platform.views.notifications;

/**
 * Static/mock representation of a notification for the Notifications
 * prototype page. Not a persistence entity.
 */
record MockNotification(String title, String message, String type, String status, String date) {
}
