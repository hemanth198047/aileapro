package com.aileapro.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

// Authentication is done against the users table, so the default in-memory user is excluded.
@SpringBootApplication(scanBasePackages = "com.aileapro", exclude = UserDetailsServiceAutoConfiguration.class)
@EntityScan(basePackages = "com.aileapro.persistence")
@EnableJpaRepositories(basePackages = "com.aileapro.api")
@EnableScheduling
public class AileaproApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(AileaproApiApplication.class, args);
    }
}
