package application.platform.views.users;

/**
 * Static/mock representation of a user row for the Users prototype page
 * (MVP-02.5). Not a persistence entity - no repository or database backs it.
 */
record MockUser(String username, String name, String organization, String supervisor, String status) {
}
