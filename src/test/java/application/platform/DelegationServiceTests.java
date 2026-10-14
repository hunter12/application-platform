package application.platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import application.platform.common.BusinessException;
import application.platform.delegation.DelegationForm;
import application.platform.delegation.DelegationService;
import application.platform.delegation.DelegationStatus;
import application.platform.delegation.DelegationSummary;
import application.platform.organization.OrganizationRepository;
import application.platform.user.UserForm;
import application.platform.user.UserService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("dev")
@Transactional
@WithMockUser(username = "admin")
class DelegationServiceTests {

    private static final LocalDate TODAY = LocalDate.now();

    @Autowired
    private DelegationService delegationService;

    @Autowired
    private UserService userService;

    @Autowired
    private OrganizationRepository organizationRepository;

    private UUID branchManager;
    private UUID branchSpv;
    private UUID regionalManager;
    private UUID otherBranchManager;

    @BeforeEach
    void createUsers() {
        branchManager = user("bm", "BR-JKT-01");
        branchSpv = user("spv", "BR-JKT-01");
        regionalManager = user("rm", "REG-JKT");
        otherBranchManager = user("obm", "BR-JKT-02");
    }

    @Test
    void statusFollowsDatesAndCancellation() {
        DelegationSummary active = delegationService.save(form(branchManager, branchSpv, TODAY, TODAY.plusDays(3)));
        DelegationSummary scheduled = delegationService.save(
                form(branchSpv, regionalManager, TODAY.plusDays(10), TODAY.plusDays(12)));

        assertThat(active.status()).isEqualTo(DelegationStatus.ACTIVE);
        assertThat(scheduled.status()).isEqualTo(DelegationStatus.SCHEDULED);

        delegationService.cancel(active.id());
        assertThat(delegationService.findById(active.id()).orElseThrow().status())
                .isEqualTo(DelegationStatus.CANCELLED);
        assertThatThrownBy(() -> delegationService.cancel(active.id()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("cancelled delegation");
    }

    @Test
    void delegateMustBeInSameOrParentOrganization() {
        assertThat(delegationService.findEligibleDelegateIds(branchManager))
                .contains(branchSpv, regionalManager)
                .doesNotContain(branchManager, otherBranchManager);

        delegationService.save(form(branchManager, regionalManager, TODAY, TODAY));
        assertThatThrownBy(() -> delegationService.save(form(branchSpv, otherBranchManager, TODAY, TODAY)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("organization above");
        UUID headOfficeUser = user("ho", "HO");
        assertThatThrownBy(() -> delegationService.save(form(regionalManager, headOfficeUser, TODAY, TODAY)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("cannot pass it on");
    }

    @Test
    void rejectsOverlapsAndChains() {
        delegationService.save(form(branchManager, branchSpv, TODAY.plusDays(1), TODAY.plusDays(5)));

        assertThatThrownBy(() -> delegationService.save(
                form(branchManager, regionalManager, TODAY.plusDays(5), TODAY.plusDays(8))))
                .isInstanceOf(BusinessException.class).hasMessageContaining("already delegates");
        assertThatThrownBy(() -> delegationService.save(
                form(branchSpv, regionalManager, TODAY.plusDays(3), TODAY.plusDays(4))))
                .isInstanceOf(BusinessException.class).hasMessageContaining("cannot pass it on");
        assertThatThrownBy(() -> delegationService.save(
                form(branchSpv, branchManager, TODAY.plusDays(4), TODAY.plusDays(5))))
                .isInstanceOf(BusinessException.class).hasMessageContaining("cannot be a delegate");

        DelegationSummary later = delegationService.save(
                form(branchManager, regionalManager, TODAY.plusDays(6), TODAY.plusDays(8)));
        assertThat(later.status()).isEqualTo(DelegationStatus.SCHEDULED);
    }

    @Test
    void validatesDatesAndOnlyScheduledCanBeEdited() {
        assertThatThrownBy(() -> delegationService.save(form(branchManager, branchSpv, TODAY.minusDays(1), TODAY)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("past");
        assertThatThrownBy(() -> delegationService.save(form(branchManager, branchSpv, TODAY, TODAY.minusDays(1))))
                .isInstanceOf(BusinessException.class).hasMessageContaining("before the start");
        assertThatThrownBy(() -> delegationService.save(form(branchManager, branchManager, TODAY, TODAY)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("different");

        DelegationSummary scheduled = delegationService.save(
                form(branchManager, branchSpv, TODAY.plusDays(1), TODAY.plusDays(2)));
        DelegationSummary edited = delegationService.save(new DelegationForm(scheduled.id(), scheduled.version(),
                branchManager, regionalManager, TODAY.plusDays(1), TODAY.plusDays(4), "Business trip"));
        assertThat(edited.delegateName()).isEqualTo("rm");
        assertThat(edited.endDate()).isEqualTo(TODAY.plusDays(4));

        DelegationSummary active = delegationService.save(form(branchSpv, regionalManager, TODAY, TODAY));
        assertThatThrownBy(() -> delegationService.save(new DelegationForm(active.id(), active.version(),
                branchSpv, regionalManager, TODAY, TODAY.plusDays(1), "Leave")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("Only scheduled");
    }

    private DelegationForm form(UUID delegator, UUID delegate, LocalDate start, LocalDate end) {
        return new DelegationForm(null, null, delegator, delegate, start, end, "Annual leave");
    }

    private UUID user(String username, String organizationCode) {
        UUID organization = organizationRepository.findByCode(organizationCode).orElseThrow().getId();
        return userService.save(new UserForm(null, null, username, "secret123", username, null, null,
                organization, null, Set.of(), true)).id();
    }
}
