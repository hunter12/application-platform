package application.platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import application.platform.common.BusinessException;
import application.platform.organization.OrganizationForm;
import application.platform.organization.OrganizationRepository;
import application.platform.organization.OrganizationService;
import application.platform.organization.OrganizationSummary;
import application.platform.organization.OrganizationType;
import application.platform.permission.PermissionForm;
import application.platform.permission.PermissionRepository;
import application.platform.permission.PermissionService;
import application.platform.role.RoleForm;
import application.platform.role.RoleRepository;
import application.platform.role.RoleService;
import application.platform.role.RoleSummary;
import application.platform.security.DatabaseUserDetailsService;
import application.platform.user.UserForm;
import application.platform.user.UserService;
import application.platform.user.UserSummary;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("dev")
@Transactional
@WithMockUser(username = "admin")
class AdministrationServicesTests {

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private DatabaseUserDetailsService userDetailsService;

    @Test
    void organizationCodeIsNormalizedAndMustBeUnique() {
        OrganizationSummary saved = organizationService.save(new OrganizationForm(null, null, "br bgr 01",
                "Branch Bogor", OrganizationType.BRANCH, idOfOrg("REG-JKT"), true));

        assertThat(saved.code()).isEqualTo("BR_BGR_01");
        assertThat(saved.parentName()).isEqualTo("Regional Jakarta");
        assertThatThrownBy(() -> organizationService.save(new OrganizationForm(null, null, "BR_BGR_01",
                "Duplicate", OrganizationType.BRANCH, null, true)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("already used");
    }

    @Test
    void organizationCannotBeMovedUnderItsOwnSubUnit() {
        OrganizationSummary regional = organizationService.findById(idOfOrg("REG-JKT")).orElseThrow();

        assertThatThrownBy(() -> organizationService.save(new OrganizationForm(regional.id(), regional.version(),
                regional.code(), regional.name(), regional.type(), idOfOrg("BR-JKT-01"), true)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("under itself");
    }

    @Test
    void staleEditIsRejected() {
        OrganizationSummary org = organizationService.findById(idOfOrg("IT")).orElseThrow();

        assertThatThrownBy(() -> organizationService.save(new OrganizationForm(org.id(), org.version() + 1,
                org.code(), "Renamed", org.type(), org.parentId(), true)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("changed by someone else");
    }

    @Test
    void newUserGetsHashedPasswordAndSeveralRoles() {
        UserSummary saved = userService.save(newUserForm("budi", "secret123",
                Set.of(idOfRole("MANAGER"), idOfRole("SPV"))));

        assertThat(saved.roleNames()).containsExactly("Manager", "SPV");
        assertThat(userDetailsService.loadUserByUsername("budi").getPassword()).startsWith("$2");
    }

    @Test
    void removedRoleIsRevokedAndCanBeGrantedAgain() {
        UserSummary user = userService.save(newUserForm("sari", "secret123", Set.of(idOfRole("MANAGER"))));

        user = userService.save(editForm(user, Set.of(idOfRole("STAFF"))));
        assertThat(authoritiesOf("sari")).containsExactly("ROLE_STAFF");

        user = userService.save(editForm(user, Set.of(idOfRole("MANAGER"))));
        assertThat(authoritiesOf("sari")).containsExactly("ROLE_MANAGER");
    }

    @Test
    void userValidationRules() {
        assertThatThrownBy(() -> userService.save(newUserForm("nopass", null, Set.of())))
                .isInstanceOf(BusinessException.class).hasMessageContaining("Password is required");
        assertThatThrownBy(() -> userService.save(newUserForm("short", "1234", Set.of())))
                .isInstanceOf(BusinessException.class).hasMessageContaining("at least");
        assertThatThrownBy(() -> userService.save(newUserForm("admin", "secret123", Set.of())))
                .isInstanceOf(BusinessException.class).hasMessageContaining("already used");
    }

    @Test
    void deletedUsernameStaysReserved() {
        UserSummary user = userService.save(newUserForm("temp", "secret123", Set.of()));
        userService.delete(user.id());

        assertThat(userService.findAll()).extracting(UserSummary::username).doesNotContain("temp");
        assertThatThrownBy(() -> userService.save(newUserForm("temp", "secret123", Set.of())))
                .isInstanceOf(BusinessException.class).hasMessageContaining("already used");
    }

    @Test
    void userCannotReportToTheirOwnSubordinate() {
        UserSummary boss = userService.save(newUserForm("boss", "secret123", Set.of()));
        UserSummary staff = userService.save(new UserForm(null, null, "staff", "secret123", "Staff", null, null,
                idOfOrg("HO"), boss.id(), Set.of(), true));

        assertThatThrownBy(() -> userService.save(new UserForm(boss.id(), boss.version(), "boss", null, "Boss",
                null, null, idOfOrg("HO"), staff.id(), Set.of(), true)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("report to");
    }

    @Test
    void loggedInAdminCannotDeleteOrDeactivateThemselves() {
        UserSummary admin = userService.findAll().stream().filter(user -> user.username().equals("admin"))
                .findFirst().orElseThrow();

        assertThatThrownBy(() -> userService.delete(admin.id()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("own account");
        assertThatThrownBy(() -> userService.save(new UserForm(admin.id(), admin.version(), "admin", null,
                admin.fullName(), null, null, admin.organizationId(), null, Set.copyOf(admin.roleIds()), false)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("own account");
    }

    @Test
    void roleCodeIsFixedAndSuperAdminCannotBeDeactivated() {
        RoleSummary role = roleService.save(new RoleForm(null, null, "branch manager", "Branch Manager", null, true));
        RoleSummary renamed = roleService.save(new RoleForm(role.id(), role.version(), "IGNORED", "Branch Head",
                "Leads a branch", true));

        assertThat(renamed.code()).isEqualTo("BRANCH_MANAGER");
        assertThat(renamed.name()).isEqualTo("Branch Head");
        assertThatThrownBy(() -> roleService.setActive(idOfRole("SUPER_ADMIN"), false))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rolePermissionsCanBeChangedAndReachTheUser() {
        UUID staffRole = idOfRole("STAFF");
        userService.save(newUserForm("ani", "secret123", Set.of(staffRole)));
        UUID invoiceView = permissionRepository.findByCode("INVOICE_VIEW").orElseThrow().getId();

        roleService.updatePermissions(staffRole, Set.of(invoiceView));
        assertThat(roleService.findPermissionIds(staffRole)).containsExactly(invoiceView);
        assertThat(authoritiesOf("ani")).contains("INVOICE_VIEW");

        roleService.updatePermissions(staffRole, Set.of());
        assertThat(authoritiesOf("ani")).doesNotContain("INVOICE_VIEW");
    }

    @Test
    void roleListCountsActiveUsers() {
        userService.save(newUserForm("dewi", "secret123", Set.of(idOfRole("DIRECTOR"))));

        RoleSummary director = roleService.findAll().stream().filter(role -> role.code().equals("DIRECTOR"))
                .findFirst().orElseThrow();
        assertThat(director.userCount()).isEqualTo(1);
    }

    @Test
    void permissionCodeIsNormalizedAndUnique() {
        assertThat(permissionService.save(new PermissionForm(null, null, "stock view", "View stock", "Stock",
                "View", null, true)).code()).isEqualTo("STOCK_VIEW");
        assertThatThrownBy(() -> permissionService.save(new PermissionForm(null, null, "USER_VIEW", "Dup",
                "Users", "View", null, true)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("already used");
    }

    private UserForm newUserForm(String username, String password, Set<UUID> roleIds) {
        return new UserForm(null, null, username, password, username + " Test", null, null, idOfOrg("HO"), null,
                roleIds, true);
    }

    private UserForm editForm(UserSummary user, Set<UUID> roleIds) {
        return new UserForm(user.id(), user.version(), user.username(), null, user.fullName(), user.email(),
                user.phone(), user.organizationId(), user.supervisorId(), roleIds, user.active());
    }

    private Set<String> authoritiesOf(String username) {
        return userDetailsService.loadUserByUsername(username).getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).collect(java.util.stream.Collectors.toSet());
    }

    private UUID idOfOrg(String code) {
        return organizationRepository.findByCode(code).orElseThrow().getId();
    }

    private UUID idOfRole(String code) {
        return roleRepository.findByCode(code).orElseThrow().getId();
    }
}
