package application.platform.organization;

public enum OrganizationType {
    HEAD_OFFICE("Head Office"),
    REGIONAL("Regional"),
    BRANCH("Branch"),
    DIVISION("Division"),
    DEPARTMENT("Department"),
    SECTION("Section"),
    UNIT("Unit");

    private final String label;

    OrganizationType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
