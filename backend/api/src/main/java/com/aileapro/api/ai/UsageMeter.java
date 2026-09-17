package com.aileapro.api.ai;

import com.aileapro.persistence.AiUsageEntity;
import com.aileapro.persistence.ModelEntity;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * Writes one row per call, successful or not.
 *
 * <p>In its own transaction, and that is the whole reason this is a separate service. A turn
 * that fails must still be metered: if the usage row rolled back with the failure, the cost
 * table would only ever show the calls that went well, and the first thing anybody would ask
 * it — "what is going wrong and what is it costing us?" — is exactly what it could not answer.
 */
@Service
public class UsageMeter {

    private static final BigDecimal PER_MILLION = new BigDecimal("1000000");

    private final AiUsageRepository usage;

    public UsageMeter(AiUsageRepository usage) {
        this.usage = usage;
    }

    /** What happened, in the words the {@code ai_usage} check constraint accepts. */
    public enum Outcome {
        OK("ok"),
        PROVIDER_ERROR("provider_error"),
        REFUSED("refused"),
        BUDGET_EXCEEDED("budget_exceeded"),
        CANCELLED("cancelled");

        private final String wire;

        Outcome(String wire) {
            this.wire = wire;
        }

        public String wire() {
            return wire;
        }
    }

    public record Call(
            UUID userId,
            String feature,
            String profileSlug,
            String providerSlug,
            String modelSlug,
            ModelEntity model,
            UUID promptVersionId,
            UUID conversationId,
            int tokensIn,
            int tokensOut,
            int latencyMs,
            Outcome outcome) {
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AiUsageEntity record(Call call) {
        AiUsageEntity row = new AiUsageEntity();
        row.setId(UUID.randomUUID());
        row.setRequestId(MDC.get("requestId"));
        row.setUserId(call.userId());
        row.setFeature(call.feature());
        row.setProfileSlug(call.profileSlug());
        row.setProviderSlug(call.providerSlug());
        row.setModelSlug(call.modelSlug());
        row.setPromptVersionId(call.promptVersionId());
        row.setConversationId(call.conversationId());
        row.setTokensIn(Math.max(0, call.tokensIn()));
        row.setTokensOut(Math.max(0, call.tokensOut()));
        row.setLatencyMs(Math.max(0, call.latencyMs()));
        row.setCostUsd(cost(call));
        row.setOutcome(call.outcome().wire());
        return usage.save(row);
    }

    /**
     * Estimated cost from the prices stored on the model.
     *
     * <p>Six decimal places because a single cheap turn costs a few thousandths of a cent, and
     * rounding those to zero would make the daily budget unenforceable by design.
     */
    private static BigDecimal cost(Call call) {
        ModelEntity model = call.model();
        if (model == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal in = model.getPriceInPerMtok()
                .multiply(BigDecimal.valueOf(Math.max(0, call.tokensIn())))
                .divide(PER_MILLION, 6, RoundingMode.HALF_UP);
        BigDecimal out = model.getPriceOutPerMtok()
                .multiply(BigDecimal.valueOf(Math.max(0, call.tokensOut())))
                .divide(PER_MILLION, 6, RoundingMode.HALF_UP);
        return in.add(out);
    }
}
