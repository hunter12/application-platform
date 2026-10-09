package application.platform.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/**
 * Seeds a single local user for authenticating during development and
 * testing (MVP-02). Active only under the "dev" profile, so it is never
 * present in a production configuration - see application-dev.properties
 * and the Dockerfile's {@code --spring.profiles.active=prod}.
 */
@Configuration
@Profile("dev")
public class DevUserConfig {

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder,
            @Value("${app.security.dev-user.username}") String username,
            @Value("${app.security.dev-user.password}") String password) {
        var devUser = User.withUsername(username)
                .password(passwordEncoder.encode(password))
                .build();
        return new InMemoryUserDetailsManager(devUser);
    }
}
