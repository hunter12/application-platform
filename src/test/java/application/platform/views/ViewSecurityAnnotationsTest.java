package application.platform.views;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import jakarta.annotation.security.PermitAll;

/**
 * Route access is enforced by the backend security layer, not by hiding UI
 * elements - these annotations are what {@code NavigationAccessControl}
 * reads to decide whether a view requires authentication.
 */
class ViewSecurityAnnotationsTest {

    @Test
    void loginViewIsPubliclyAccessible() {
        assertThat(LoginView.class.isAnnotationPresent(AnonymousAllowed.class)).isTrue();
        assertThat(LoginView.class.getAnnotation(Route.class).value()).isEqualTo("login");
    }

    @Test
    void dashboardViewRequiresAuthentication() {
        assertThat(DashboardView.class.isAnnotationPresent(PermitAll.class)).isTrue();
        assertThat(DashboardView.class.isAnnotationPresent(AnonymousAllowed.class)).isFalse();
        assertThat(DashboardView.class.getAnnotation(Route.class).value()).isEqualTo("");
    }
}
