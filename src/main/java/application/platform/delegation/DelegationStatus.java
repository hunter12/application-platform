package application.platform.delegation;

public enum DelegationStatus {
    SCHEDULED("Scheduled"),
    ACTIVE("Active"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    private final String label;

    DelegationStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
