package application.platform.views.organization;

import java.util.List;

/**
 * Static/mock representation of an organization unit for the Organization
 * Hierarchy prototype page. Not a persistence entity.
 */
record OrgUnit(String name, String type, String status, List<OrgUnit> children) {

    OrgUnit(String name, String type, String status, OrgUnit... children) {
        this(name, type, status, List.of(children));
    }
}
