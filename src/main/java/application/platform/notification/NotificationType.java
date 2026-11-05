package application.platform.notification;

import java.util.Arrays;
import java.util.List;

public enum NotificationType {
    APPROVAL("Approval", false),
    DELEGATION("Delegation", false),
    SYSTEM("System", true),
    INFORMATION("Information", true);

    private final String label;
    private final boolean manual;

    NotificationType(String label, boolean manual) {
        this.label = label;
        this.manual = manual;
    }

    public String getLabel() {
        return label;
    }

    /** Whether an admin may send this type by hand; the others are only created by the system. */
    public boolean isManual() {
        return manual;
    }

    public static List<NotificationType> manualTypes() {
        return Arrays.stream(values()).filter(NotificationType::isManual).toList();
    }
}
