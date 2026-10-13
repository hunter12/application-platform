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
