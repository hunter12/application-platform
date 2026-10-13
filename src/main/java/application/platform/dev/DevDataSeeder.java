package application.platform.dev;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import application.platform.organization.Organization;
import application.platform.organization.OrganizationRepository;
import application.platform.organization.OrganizationType;
import application.platform.permission.Permission;
import application.platform.permission.PermissionRepository;
import application.platform.permission.RolePermission;
import application.platform.permission.RolePermissionRepository;
import application.platform.role.Role;
import application.platform.role.RoleRepository;
import application.platform.role.UserRole;
import application.platform.role.UserRoleRepository;
import application.platform.user.AppUser;
import application.platform.user.AppUserRepository;

@Component
@Profile("dev")
public class DevDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    private final OrganizationRepository organizationRepository;
    private final AppUserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;

    public DevDataSeeder(OrganizationRepository organizationRepository, AppUserRepository userRepository,
            RoleRepository roleRepository, UserRoleRepository userRoleRepository,
            PermissionRepository permissionRepository, RolePermissionRepository rolePermissionRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.security.dev-user.username}") String adminUsername,
            @Value("${app.security.dev-user.password}") String adminPassword) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }
        log.info("Empty development database - seeding organizations, roles, permissions and the admin user");

        Map<String, Organization> organizations = seedOrganizations();
        List<Permission> permissions = seedPermissions();
        Map<String, Role> roles = seedRoles();

        for (Permission permission : permissions) {
            rolePermissionRepository.save(new RolePermission(roles.get("SUPER_ADMIN"), permission));
        }

        AppUser admin = userRepository.save(new AppUser(adminUsername, passwordEncoder.encode(adminPassword),
                "Administrator", organizations.get("HO"), null));
        userRoleRepository.save(new UserRole(admin, roles.get("SUPER_ADMIN"), null));
    }

    private Map<String, Organization> seedOrganizations() {
        Map<String, Organization> byCode = new HashMap<>();
        organization(byCode, "HO", "Head Office", OrganizationType.HEAD_OFFICE, null);
        organization(byCode, "REG-JKT", "Regional Jakarta", OrganizationType.REGIONAL, "HO");
        organization(byCode, "REG-BDG", "Regional Bandung", OrganizationType.REGIONAL, "HO");
        organization(byCode, "BR-JKT-01", "Branch Jakarta Selatan", OrganizationType.BRANCH, "REG-JKT");
        organization(byCode, "BR-JKT-02", "Branch Jakarta Utara", OrganizationType.BRANCH, "REG-JKT");
        organization(byCode, "BR-BDG-01", "Branch Bandung 1", OrganizationType.BRANCH, "REG-BDG");
        organization(byCode, "BR-BDG-02", "Branch Bandung 2", OrganizationType.BRANCH, "REG-BDG");
        organization(byCode, "IT", "IT Division", OrganizationType.DIVISION, "HO");
        organization(byCode, "DEV", "Development Department", OrganizationType.DEPARTMENT, "IT");
        organization(byCode, "INFRA", "Infrastructure Department", OrganizationType.DEPARTMENT, "IT");
        return byCode;
    }

    private void organization(Map<String, Organization> byCode, String code, String name, OrganizationType type,
            String parentCode) {
        Organization parent = parentCode == null ? null : byCode.get(parentCode);
        byCode.put(code, organizationRepository.save(new Organization(code, name, type, parent)));
    }

    private Map<String, Role> seedRoles() {
        Map<String, Role> byCode = new HashMap<>();
        for (Role role : List.of(
                new Role("SUPER_ADMIN", "Super Admin", "Full system access"),
                new Role("ADMIN", "Admin", "System administration"),
                new Role("DIRECTOR", "Director", "Director level"),
                new Role("MANAGER", "Manager", "Manager level"),
                new Role("SPV", "SPV", "Supervisor level"),
                new Role("STAFF", "Staff", "Operational user"))) {
            byCode.put(role.getCode(), roleRepository.save(role));
        }
        return byCode;
    }

    private List<Permission> seedPermissions() {
        return permissionRepository.saveAll(List.of(
                new Permission("USER_VIEW", "View users", "Users", "View", "View users"),
                new Permission("USER_CREATE", "Create user", "Users", "Create", "Create user"),
                new Permission("USER_UPDATE", "Update user", "Users", "Update", "Update user"),
                new Permission("USER_DELETE", "Delete user", "Users", "Delete", "Delete user"),
                new Permission("ROLE_VIEW", "View roles", "Roles", "View", "View roles"),
                new Permission("ROLE_CREATE", "Create role", "Roles", "Create", "Create role"),
                new Permission("ORG_VIEW", "View organizations", "Organization", "View", "View organizations"),
                new Permission("ORG_CREATE", "Create organization", "Organization", "Create", "Create organization"),
                new Permission("INVOICE_VIEW", "View invoices", "Invoice", "View", "View invoices"),
                new Permission("INVOICE_CREATE", "Create invoice", "Invoice", "Create", "Create invoice"),
                new Permission("INVOICE_APPROVE", "Approve invoice", "Invoice", "Approve", "Approve invoice"),
                new Permission("INVOICE_EXPORT", "Export invoice data", "Invoice", "Export", "Export invoice data")));
    }
}
