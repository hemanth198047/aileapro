package com.aileapro.api.config;

import com.aileapro.security.PasswordEncoders;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Password hashing is needed by services regardless of whether a web server is running,
 * so it is separate from the servlet-only {@link SecurityConfig}.
 */
@Configuration
public class PasswordConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoders.argon2();
    }
}
