package com.aileapro.api.lab;

import com.aileapro.lab.ExecutorPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Production refuses the fake lab executor the same way {@code AiStartupCheck} refuses FakeChat.
 */
@Component
@Profile("prod")
public class LabStartupCheck implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(LabStartupCheck.class);

    private final ExecutorPort executor;

    public LabStartupCheck(ExecutorPort executor) {
        this.executor = executor;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (!executor.development()) {
            log.info("Lab executor is a real sandbox ({})", executor.kind());
            return;
        }
        throw new IllegalStateException(
                "aileapro.lab.executor is still fake under the prod profile. Point "
                        + "AILEAPRO_LAB_EXECUTOR=http at the Docker executor, or refuse to launch.");
    }
}
