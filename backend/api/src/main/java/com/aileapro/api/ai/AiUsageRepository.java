package com.aileapro.api.ai;

import com.aileapro.persistence.AiUsageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AiUsageRepository extends JpaRepository<AiUsageEntity, UUID> {

    /** What this learner has cost since a moment. Zero rather than null when nothing. */
    @Query("select coalesce(sum(u.costUsd), 0) from AiUsageEntity u "
            + "where u.userId = :userId and u.at >= :since")
    BigDecimal spentSince(@Param("userId") UUID userId, @Param("since") Instant since);

    /**
     * Turns taken since a moment, refusals excluded.
     *
     * <p>A count as well as a cost, because the development provider is free: with only a
     * dollar cap, development and CI would run with no limit at all, and an abuse path that
     * happens to cost nothing is still an abuse path.
     */
    @Query("select count(u) from AiUsageEntity u where u.userId = :userId and u.at >= :since "
            + "and u.outcome in ('ok', 'provider_error')")
    long turnsSince(@Param("userId") UUID userId, @Param("since") Instant since);

    List<AiUsageEntity> findByConversationIdOrderByAtAsc(UUID conversationId);

    @Query("select coalesce(sum(u.costUsd), 0) from AiUsageEntity u where u.at >= :since")
    BigDecimal totalSpentSince(@Param("since") Instant since);

    @Query("select coalesce(sum(u.tokensIn), 0) from AiUsageEntity u where u.at >= :since")
    long tokensInSince(@Param("since") Instant since);

    @Query("select coalesce(sum(u.tokensOut), 0) from AiUsageEntity u where u.at >= :since")
    long tokensOutSince(@Param("since") Instant since);

    @Query("select count(u) from AiUsageEntity u where u.at >= :since")
    long callCountSince(@Param("since") Instant since);

    @Query("""
            select u.feature, coalesce(sum(u.costUsd), 0), coalesce(sum(u.tokensIn), 0),
                   coalesce(sum(u.tokensOut), 0), count(u)
            from AiUsageEntity u where u.at >= :since group by u.feature order by u.feature
            """)
    List<Object[]> spendByFeatureSince(@Param("since") Instant since);
}
