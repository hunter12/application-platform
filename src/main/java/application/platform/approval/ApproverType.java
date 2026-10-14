package application.platform.approval;

public enum ApproverType {
    DIRECT_SUPERVISOR("Direct supervisor of requester", false),
    ROLE_IN_REQUESTER_ORG("Role in requester's organization", true),
    ROLE_IN_PARENT_ORG("Role in a parent organization", true);

    private final String label;
    private final boolean needsRole;

    ApproverType(String label, boolean needsRole) {
        this.label = label;
        this.needsRole = needsRole;
    }

    public String getLabel() {
        return label;
    }

    public boolean needsRole() {
        return needsRole;
    }
}
