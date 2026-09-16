package com.ailepro.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

// Authentication is done against the users table, so the default in-memory user is excluded.
@SpringBootApplication(scanBasePackages = "com.ailepro", exclude = UserDetailsServiceAutoConfiguration.class)
@EntityScan(basePackages = "com.ailepro.persistence")
@EnableJpaRepositories(basePackages = "com.ailepro.api")
public class AileproApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(AileproApiApplication.class, args);
    }
}
