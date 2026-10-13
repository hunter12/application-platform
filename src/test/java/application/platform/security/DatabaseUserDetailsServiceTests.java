package application.platform.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import application.platform.common.JpaAuditingConfig;
import application.platform.organization.OrganizationRepository;
import application.platform.role.RoleRepository;
import application.platform.role.UserRole;
import application.platform.role.UserRoleRepository;
import application.platform.user.AppUser;
import application.platform.user.AppUserRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("dev")
@Transactional
class DatabaseUserDetailsServiceTests {

    @Autowired
    private DatabaseUserDetailsService userDetailsService;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void seededAdminHasSuperAdminRoleAndAllPermissions() {
        UserDetails admin = userDetailsService.loadUserByUsername("admin");

        assertThat(admin.isEnabled()).isTrue();
        assertThat(admin.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_SUPER_ADMIN", "USER_VIEW", "INVOICE_APPROVE");
    }

    @Test
    void userWithSeveralRolesGetsAllOfThem() {
        AppUser user = newUser("multi");
        userRoleRepository.save(new UserRole(user, roleRepository.findByCode("MANAGER").orElseThrow(), null));
        userRoleRepository.save(new UserRole(user, roleRepository.findByCode("SPV").orElseThrow(), null));

        UserDetails details = userDetailsService.loadUserByUsername("multi");

        assertThat(details.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_MANAGER", "ROLE_SPV");
    }

    @Test
    void inactiveRoleAssignmentIsNotGranted() {
        AppUser user = newUser("revoked");
        UserRole assignment = new UserRole(user, roleRepository.findByCode("MANAGER").orElseThrow(), null);
        assignment.setActive(false);
        userRoleRepository.save(assignment);

        UserDetails details = userDetailsService.loadUserByUsername("revoked");

        assertThat(details.getAuthorities()).isEmpty();
    }

    @Test
    void inactiveUserIsLoadedAsDisabled() {
        AppUser user = newUser("inactive");
        user.setActive(false);
        userRepository.saveAndFlush(user);

        assertThat(userDetailsService.loadUserByUsername("inactive").isEnabled()).isFalse();
    }

    @Test
    void softDeletedUserIsNotFound() {
        AppUser user = newUser("deleted");
        user.markDeleted("admin");
        userRepository.saveAndFlush(user);

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("deleted"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void newRowsGetUuidV7AndAuditColumns() {
        AppUser user = newUser("audited");

        assertThat(user.getId().version()).isEqualTo(7);
        assertThat(user.getCreatedDate()).isNotNull();
        assertThat(user.getCreatedBy()).isEqualTo(JpaAuditingConfig.SYSTEM_AUDITOR);
        assertThat(user.getModifiedDate()).isNull();
        assertThat(user.getVersion()).isZero();
        assertThat(user.isActive()).isTrue();
    }

    private AppUser newUser(String username) {
        return userRepository.saveAndFlush(new AppUser(username, passwordEncoder.encode("secret"), username,
                organizationRepository.findByCode("HO").orElseThrow(), null));
    }
}
