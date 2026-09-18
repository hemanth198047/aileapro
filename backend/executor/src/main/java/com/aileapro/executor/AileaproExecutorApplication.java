package com.aileapro.executor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ExecutorProperties.class)
public class AileaproExecutorApplication {
    public static void main(String[] args) {
        SpringApplication.run(AileaproExecutorApplication.class, args);
    }
}
