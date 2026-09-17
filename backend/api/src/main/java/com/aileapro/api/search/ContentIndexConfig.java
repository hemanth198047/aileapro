package com.aileapro.api.search;

import com.aileapro.ai.EmbeddingPort;
import com.aileapro.ai.FakeEmbeddingClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Wires the search backend.
 *
 * <p>{@code aileapro.search.backend=memory} is the test default: H2 has no {@code tsvector}
 * and no {@code vector}. Local and production use {@code jdbc} against Postgres with pgvector.
 */
@Configuration
public class ContentIndexConfig {

    @Bean
    public EmbeddingPort embeddingPort() {
        // A real HTTP embedder arrives when a paid key is configured. Until then every
        // environment shares the deterministic fixture so retrieval tests stay free.
        return new FakeEmbeddingClient();
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "aileapro.search.backend", havingValue = "memory")
    public ContentIndex memoryContentIndex(EmbeddingPort embeddingPort) {
        return new MemoryContentIndex(embeddingPort);
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "aileapro.search.backend", havingValue = "jdbc", matchIfMissing = true)
    public ContentIndex jdbcContentIndex(JdbcTemplate jdbcTemplate, EmbeddingPort embeddingPort) {
        return new JdbcContentIndex(jdbcTemplate, embeddingPort);
    }
}
