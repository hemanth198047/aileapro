package com.ailepro.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.ailepro")
@EntityScan(basePackages = "com.ailepro.persistence")
@EnableJpaRepositories(basePackages = "com.ailepro.api")
public class AileproApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(AileproApiApplication.class, args);
    }
}
