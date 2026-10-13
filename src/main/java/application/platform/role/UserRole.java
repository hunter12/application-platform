package application.platform.role;

import org.hibernate.annotations.SQLRestriction;

import application.platform.common.BaseEntity;
import application.platform.organization.Organization;
import application.platform.user.AppUser;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "user_roles", uniqueConstraints = @UniqueConstraint(name = "uk_user_roles_user_role_org",
        columnNames = { "user_id", "role_id", "organization_id" }))
@SQLRestriction("deleted_date is null")
public class UserRole extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_roles_user"))
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_roles_role"))
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", foreignKey = @ForeignKey(name = "fk_user_roles_organization"))
    private Organization organization;

    protected UserRole() {
    }

    public UserRole(AppUser user, Role role, Organization organization) {
        this.user = user;
        this.role = role;
        this.organization = organization;
    }

    public AppUser getUser() {
        return user;
    }

    public Role getRole() {
        return role;
    }

    public Organization getOrganization() {
        return organization;
    }
}
