package application.platform.views.approval;

/**
 * Static/mock representation of a delegation row for the Delegation
 * prototype page. Not a persistence entity.
 */
record MockDelegation(String delegator, String delegateTo, String startDate, String endDate, String reason,
        String status) {
}
