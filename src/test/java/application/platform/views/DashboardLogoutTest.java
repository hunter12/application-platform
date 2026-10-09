package application.platform.views;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.spring.security.AuthenticationContext;

/**
 * Reproduces the real "Log out" button click in the application shell header
 * (not a raw HTTP call) to catch regressions like the one where the button
 * navigated to a plain {@code GET /logout}, which Spring Security's
 * CSRF-protected logout filter does not match, resulting in a 403 instead of
 * a logout.
 * <p>
 * The actual logout mechanics (session invalidation, redirect to the login
 * page) are Vaadin/Spring Security internals already covered by
 * {@link application.platform.security.AuthenticationTests}; this test only
 * verifies that clicking the button triggers the correct, officially
 * supported logout entry point.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("dev")
class DashboardLogoutTest extends SpringBrowserlessTest {

    @MockitoBean
    private AuthenticationContext authenticationContext;

    @Test
    @WithMockUser(username = "admin")
    void clickingLogoutButtonInvokesAuthenticationContextLogout() {
        navigate(DashboardView.class);

        // The button lives in MainLayout (the app shell header), not inside
        // DashboardView itself, so it must be looked up across the whole UI.
        Button logoutButton = find(Button.class).withText("Log out").single();
        test(logoutButton).click();

        verify(authenticationContext).logout();
    }
}
