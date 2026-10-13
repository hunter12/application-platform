package application.platform.user;

import static application.platform.common.ServiceSupport.checkVersion;
import static application.platform.common.ServiceSupport.ensureUnique;
import static application.platform.common.ServiceSupport.optionalText;
import static application.platform.common.ServiceSupport.require;
import static application.platform.common.ServiceSupport.requireText;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import application.platform.common.BusinessException;
import application.platform.common.JpaAuditingConfig;
import application.platform.organization.Organization;
import application.platform.organization.OrganizationRepository;
import application.platform.role.Role;
import application.platform.role.RoleRepository;
import application.platform.role.UserRole;
import application.platform.role.UserRoleRepository;

@Service
@Transactional
public class UserService {

    static final int MIN_PASSWORD_LENGTH = 8;

    private final AppUserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(AppUserRepository userRepository, OrganizationRepository organizationRepository,
            RoleRepository roleRepository, UserRoleRepository userRoleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserSummary> findAll() {
        Map<UUID, List<Role>> rolesByUser = userRoleRepository.findAllActiveWithRole().stream()
                .collect(Collectors.groupingBy(assignment -> assignment.getUser().getId(),
                        Collectors.mapping(UserRole::getRole, Collectors.toList())));
        return userRepository.findAllWithDetails().stream()
                .map(user -> UserSummary.of(user, rolesByUser.getOrDefault(user.getId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<UserSummary> findById(UUID id) {
        return userRepository.findById(id).map(user -> UserSummary.of(user, activeRoles(user.getId())));
    }

    public UserSummary save(UserForm form) {
        String username = requireText(form.username(), "Username");
        String fullName = requireText(form.fullName(), "Full name");
        Organization organization = organizationRepository
                .findById(require(form.organizationId(), "Organization"))
                .orElseThrow(() -> new BusinessException("Organization not found"));

        AppUser user = form.id() == null ? null : load(form.id());
        if (user != null) {
            checkVersion(user, form.version());
        }
        ensureUnique(userRepository.countByUsernameIncludingDeleted(username),
                user != null && username.equals(user.getUsername()),
                "Username " + username + " is already used");

        String password = optionalText(form.password());
        if (user == null && password == null) {
            throw new BusinessException("Password is required");
        }
        if (password != null && password.length() < MIN_PASSWORD_LENGTH) {
            throw new BusinessException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }

        AppUser supervisor = form.supervisorId() == null ? null : load(form.supervisorId());
        if (user != null) {
            // Walk up the reporting line so nobody ends up (indirectly) reporting to themselves.
            for (AppUser boss = supervisor; boss != null; boss = boss.getSupervisor()) {
                if (boss.getId().equals(user.getId())) {
                    throw new BusinessException("A user cannot report to themselves or to one of their subordinates");
                }
            }
            if (!form.active() && isCurrentUser(user)) {
                throw new BusinessException("You cannot deactivate your own account");
            }
        }

        if (user == null) {
            user = new AppUser(username, passwordEncoder.encode(password), fullName, organization, supervisor);
        } else {
            user.setUsername(username);
            user.setFullName(fullName);
            user.setOrganization(organization);
            user.setSupervisor(supervisor);
            if (password != null) {
                user.setPasswordHash(passwordEncoder.encode(password));
            }
        }
        user.setEmail(optionalText(form.email()));
        user.setPhone(optionalText(form.phone()));
        user.setActive(form.active());
        user = userRepository.saveAndFlush(user);

        updateRoles(user, form.roleIds() == null ? Set.of() : form.roleIds());
        return UserSummary.of(user, activeRoles(user.getId()));
    }

    /**
     * Soft-deletes a user. Their subordinates no longer report to anyone, so
     * no live row points at a deleted supervisor.
     */
    public void delete(UUID id) {
        AppUser user = load(id);
        if (isCurrentUser(user)) {
            throw new BusinessException("You cannot delete your own account");
        }
        for (AppUser subordinate : userRepository.findBySupervisorId(id)) {
            subordinate.setSupervisor(null);
        }
        user.markDeleted(JpaAuditingConfig.currentAuditor());
    }

    /**
     * Makes the user hold exactly the given (organization-wide) roles.
     * Removed roles are switched off rather than deleted, so re-assigning
     * reuses the same row and its audit trail.
     */
    private void updateRoles(AppUser user, Set<UUID> roleIds) {
        Set<UUID> existing = new HashSet<>();
        for (UserRole assignment : userRoleRepository.findByUserId(user.getId())) {
            if (assignment.getOrganization() != null) {
                continue; // organization-scoped assignments are not managed by this form
            }
            UUID roleId = assignment.getRole().getId();
            existing.add(roleId);
            assignment.setActive(roleIds.contains(roleId));
        }
        for (UUID roleId : roleIds) {
            if (!existing.contains(roleId)) {
                Role role = roleRepository.findById(roleId)
                        .orElseThrow(() -> new BusinessException("Role not found"));
                userRoleRepository.save(new UserRole(user, role, null));
            }
        }
        userRoleRepository.flush();
    }

    private List<Role> activeRoles(UUID userId) {
        return userRoleRepository.findByUserId(userId).stream()
                .filter(UserRole::isActive)
                .map(UserRole::getRole)
                .toList();
    }

    private boolean isCurrentUser(AppUser user) {
        return user.getUsername().equals(JpaAuditingConfig.currentAuditor());
    }

    private AppUser load(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new BusinessException("User not found"));
    }
}
