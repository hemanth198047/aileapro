package com.aileapro.api.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Production refuses to start on fixtures.
 *
 * <p>This is the guard that makes the development implementation safe to have. Fixtures are
 * fine on a laptop and necessary in CI, but a production deployment that quietly served them
 * would be teaching learners from canned text while reporting a working tutor — so the
 * {@code prod} profile checks for a real provider at startup and fails loudly if there is
 * none.
 *
 * <p>It runs after the context is ready rather than during bean creation, so the failure
 * message is the first thing in the log rather than the last line of a wiring stack trace.
 */
@Component
@Profile("prod")
public class AiStartupCheck implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(AiStartupCheck.class);

    private final ModelRouter router;

    public AiStartupCheck(ModelRouter router) {
        this.router = router;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (router.anyRealProviderConfigured()) {
            log.info("A real model provider is configured; the tutor will not use fixtures.");
            return;
        }
        throw new IllegalStateException(
                "No real model provider is configured, and this is the prod profile. The tutor would "
                        + "answer from development fixtures, which must never reach a learner. Set the API "
                        + "key named by a provider row in ai/catalogue.yml, or disable the tutor.");
    }
}
