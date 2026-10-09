package application.platform.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;

import application.platform.views.LoginView;

/**
 * Local username/password authentication for the application (MVP-02).
 * <p>
 * Delegates route protection to Vaadin's {@link VaadinSecurityConfigurer},
 * which authenticates every view unless it is explicitly annotated
 * {@code @AnonymousAllowed}, and wires the login/logout flow to
 * {@link LoginView}.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .with(VaadinSecurityConfigurer.vaadin(),
                        configurer -> configurer.loginView(LoginView.class))
                .build();
    }
}
