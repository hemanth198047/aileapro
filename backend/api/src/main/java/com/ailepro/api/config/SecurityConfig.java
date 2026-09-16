package com.ailepro.api.config;

import com.ailepro.security.PasswordEncoders;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoders.argon2();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CookieCsrfTokenRepository csrfRepo = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepo.setCookieName("XSRF-TOKEN");
        csrfRepo.setHeaderName("X-XSRF-TOKEN");

        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfRepo)
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers(
                                "/actuator/health",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/v1/auth/register",
                                "/v1/auth/login",
                                "/v1/auth/csrf",
                                "/v1/auth/verify-email",
                                "/v1/auth/password/forgot",
                                "/v1/auth/password/reset",
                                "/actuator/health",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .requestMatchers("/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .securityContext(sc -> sc.securityContextRepository(new HttpSessionSecurityContextRepository()))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> writeJson(res, 401, "unauthorized", "Authentication required"))
                        .accessDeniedHandler((req, res, e) -> writeJson(res, 403, "forbidden", "Not allowed")))
                .logout(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${ailepro.web.origin}") String origin
    ) {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of(origin));
        cfg.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Content-Type", "X-Request-Id", "X-XSRF-TOKEN"));
        cfg.setAllowCredentials(true);
        cfg.setExposedHeaders(List.of("X-Request-Id"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    private static void writeJson(HttpServletResponse res, int status, String code, String message) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json");
        String rid = MDC.get("requestId");
        String requestId = rid == null ? "" : rid.replace("\"", "");
        res.getWriter().write(
                "{\"error\":{\"code\":\"" + code + "\",\"message\":\"" + message + "\",\"requestId\":\"" + requestId + "\"}}"
        );
    }
}
