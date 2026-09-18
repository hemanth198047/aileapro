package com.aileapro.api.lab;

import com.aileapro.lab.ExecutorPort;
import com.aileapro.lab.FakeExecutorClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LabExecutorConfig {

    @Bean
    @ConditionalOnProperty(name = "aileapro.lab.executor", havingValue = "fake", matchIfMissing = true)
    public ExecutorPort fakeExecutorPort() {
        return new FakeExecutorClient();
    }

    /**
     * When {@code http} is selected, {@link HttpExecutorClient} is the port. This bean exists
     * only so {@code @ConditionalOnMissingBean} callers in tests can still inject a fake if
     * they override the property after context start — not used in the default wiring.
     */
    @Bean
    @ConditionalOnMissingBean(ExecutorPort.class)
    public ExecutorPort fallbackFakeExecutorPort() {
        return new FakeExecutorClient();
    }
}
