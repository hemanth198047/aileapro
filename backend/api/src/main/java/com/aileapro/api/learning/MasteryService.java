package com.aileapro.api.learning;

import com.aileapro.api.curriculum.CurriculumQueryService;
import com.aileapro.api.curriculum.CurriculumViews.PublishedUnit;
import com.aileapro.api.curriculum.CurriculumViews.UnitRef;
import com.aileapro.api.learning.LearningViews.Chip;
import com.aileapro.api.learning.LearningViews.Evidence;
import com.aileapro.api.learning.LearningViews.Mastery;
import com.aileapro.api.graph.GraphViews;
import com.aileapro.domain.EvidenceState;
import com.aileapro.domain.LearningEventKind;
import com.aileapro.domain.MasteryPolicy;
import com.aileapro.domain.MasteryPolicy.Snapshot;
import com.aileapro.domain.MasteryPolicy.UnitFacts;
import com.aileapro.domain.MasteryState;
import com.aileapro.persistence.LearningSessionEntity;
import com.aileapro.persistence.MasteryEventEntity;
import com.aileapro.persistence.MasteryRecordEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Records what a learner did and projects it into evidence.
 *
 * <p>Two things are deliberate here. The event log is written first and never amended, so
 * the projection can always be rebuilt if the rules change. And the decision about what an
 * event is <em>worth</em> is not made in this class at all — it is made by the pure
 * {@link MasteryPolicy}, which is what keeps "opening a page is not progress" a property of
 * the system rather than a promise in a comment.
 */
@Service
@Transactional
public class MasteryService implements GraphViews.MasteryLookup {

    /** A gap this long ends the reading session rather than stretching it. */
    private static final Duration SESSION_IDLE_GAP = Duration.ofMinutes(15);

    /**
     * Enrols a unit's review cards, supplied from outside so this class never has to know how
     * scheduling works. Called the moment reading is finished on a unit and not before:
     * enrolling material nobody has met is how a review queue becomes a chore people abandon.
     */
    public interface ReviewEnrolment {

        int enrol(UUID userId, UUID unitId);
    }

    private final CurriculumQueryService curriculum;
    private final MasteryRecordRepository records;
    private final MasteryEventRepository events;
    private final LearningSessionRepository sessions;
    private final ReviewEnrolment reviews;
    private final ObjectMapper json;

    public MasteryService(
            CurriculumQueryService curriculum,
            MasteryRecordRepository records,
            MasteryEventRepository events,
            LearningSessionRepository sessions,
            ReviewEnrolment reviews,
            ObjectMapper json) {
        this.curriculum = curriculum;
        this.records = records;
        this.events = events;
        this.sessions = sessions;
        this.reviews = reviews;
        this.json = json;
    }

    /**
     * Appends one observation and returns the learner's standing afterwards.
     *
     * <p>The unit is resolved as published, so a draft cannot collect evidence, and a
     * learner poking at an unpublished slug gets a 404 like everyone else.
     */
    public Mastery record(UUID userId, String idOrSlug, LearningEventKind kind, int seconds, String level) {
        PublishedUnit unit = curriculum.publishedUnitForProgress(idOrSlug);
        Instant now = Instant.now();

        appendEvent(userId, unit.id(), kind, seconds, level, now);
        touchSession(userId, unit.id(), kind, seconds, now);

        MasteryRecordEntity record = records
                .findByUserIdAndUnitId(userId, unit.id())
                .orElseGet(() -> newRecord(userId, unit.id(), now));

        UnitFacts facts = new UnitFacts(unit.estimateMinutes(), unit.authoredLevels());
        Snapshot before = toSnapshot(record);
        Snapshot after = MasteryPolicy.apply(before, new MasteryPolicy.Event(kind, seconds, level), facts);

        applySnapshot(record, before, after, now);
        if (kind == LearningEventKind.UNIT_OPENED && record.getFirstOpenedAt() == null) {
            record.setFirstOpenedAt(now);
        }
        record.setLastEventAt(now);
        record.setUpdatedAt(now);
        records.save(record);

        if (before.evidence().read() != EvidenceState.SOLID
                && after.evidence().read() == EvidenceState.SOLID) {
            reviews.enrol(userId, unit.id());
        }

        return view(record, unit.slug(), facts);
    }

    @Transactional(readOnly = true)
    public Mastery mastery(UUID userId, String idOrSlug) {
        PublishedUnit unit = curriculum.publishedUnitForProgress(idOrSlug);
        UnitFacts facts = new UnitFacts(unit.estimateMinutes(), unit.authoredLevels());
        return records
                .findByUserIdAndUnitId(userId, unit.id())
                .map(record -> view(record, unit.slug(), facts))
                .orElseGet(() -> unknown(unit.id(), unit.slug(), facts));
    }

    /**
     * Standings for a set of units, for overlays like the roadmap. Units with no evidence
     * come back as UNKNOWN rather than missing, so callers cannot accidentally read absence
     * as something else.
     *
     * <p>The read floor here is derived from the estimate alone; the level count only affects
     * which chip state is reachable, not how long the floor is.
     */
    @Transactional(readOnly = true)
    public Map<UUID, Mastery> masteryFor(UUID userId, Collection<UnitRef> refs) {
        if (refs.isEmpty()) {
            return Map.of();
        }
        List<UUID> ids = refs.stream().map(UnitRef::id).toList();
        Map<UUID, MasteryRecordEntity> byUnit = new HashMap<>();
        for (MasteryRecordEntity record : records.findByUserIdAndUnitIdIn(userId, ids)) {
            byUnit.put(record.getUnitId(), record);
        }

        Map<UUID, Mastery> out = new LinkedHashMap<>(refs.size());
        for (UnitRef ref : refs) {
            UnitFacts facts = new UnitFacts(ref.estimateMinutes(), 0);
            MasteryRecordEntity record = byUnit.get(ref.id());
            out.put(
                    ref.id(),
                    record == null
                            ? unknown(ref.id(), ref.slug(), facts)
                            : view(record, ref.slug(), facts));
        }
        return out;
    }

    /**
     * States for the graph overlay. Only units with evidence appear; the graph reads an absent
     * id as unknown, which is the same thing said more cheaply.
     */
    @Override
    @Transactional(readOnly = true)
    public Map<UUID, String> statesFor(UUID userId, Collection<UnitRef> units) {
        if (units.isEmpty()) {
            return Map.of();
        }
        List<UUID> ids = units.stream().map(UnitRef::id).toList();
        Map<UUID, String> states = new HashMap<>();
        for (MasteryRecordEntity record : records.findByUserIdAndUnitIdIn(userId, ids)) {
            states.put(record.getUnitId(), record.getState().name());
        }
        return states;
    }

    private void appendEvent(
            UUID userId, UUID unitId, LearningEventKind kind, int seconds, String level, Instant now) {
        MasteryEventEntity event = new MasteryEventEntity();
        event.setId(UUID.randomUUID());
        event.setUserId(userId);
        event.setUnitId(unitId);
        event.setKind(kind);
        event.setPayload(payload(kind, seconds, level));
        event.setAt(now);
        events.save(event);
    }

    /**
     * Keeps the session row in step with the heartbeats. Sessions are where dwell can be
     * audited later: the record carries a total, this carries the stretches it came from.
     */
    private void touchSession(UUID userId, UUID unitId, LearningEventKind kind, int seconds, Instant now) {
        Optional<LearningSessionEntity> open =
                sessions.findFirstByUserIdAndUnitIdAndEndedAtIsNullOrderByStartedAtDesc(userId, unitId);

        if (open.isPresent()) {
            LearningSessionEntity session = open.get();
            boolean stale = session.getLastSeenAt().isBefore(now.minus(SESSION_IDLE_GAP));
            if (stale) {
                // Close it at the last sign of life, not now: the silence in between was not reading.
                session.setEndedAt(session.getLastSeenAt());
                sessions.save(session);
            } else {
                session.setLastSeenAt(now);
                if (kind == LearningEventKind.DWELL) {
                    session.setDwellSeconds(session.getDwellSeconds() + clamp(seconds));
                }
                sessions.save(session);
                return;
            }
        }

        LearningSessionEntity session = new LearningSessionEntity();
        session.setId(UUID.randomUUID());
        session.setUserId(userId);
        session.setUnitId(unitId);
        session.setMode("read");
        session.setStartedAt(now);
        session.setLastSeenAt(now);
        session.setDwellSeconds(kind == LearningEventKind.DWELL ? clamp(seconds) : 0);
        sessions.save(session);
    }

    private MasteryRecordEntity newRecord(UUID userId, UUID unitId, Instant now) {
        MasteryRecordEntity record = new MasteryRecordEntity();
        record.setId(UUID.randomUUID());
        record.setUserId(userId);
        record.setUnitId(unitId);
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        return record;
    }

    private static Snapshot toSnapshot(MasteryRecordEntity record) {
        return new Snapshot(
                record.getState(),
                // Fully qualified: the domain's Evidence and the view's Evidence are both in
                // scope here, and they are not the same thing.
                new com.aileapro.domain.Evidence(
                        record.getReadState(),
                        record.getPracticeState(),
                        record.getAssessState(),
                        record.getApplyState()),
                record.getDwellSeconds(),
                java.util.Set.of(record.getLevelsSeen()),
                record.getOpenedCount(),
                record.getScore().doubleValue());
    }

    /** Chip dates only move when the chip itself moves, so a tooltip never claims fresh evidence. */
    private static void applySnapshot(
            MasteryRecordEntity record, Snapshot before, Snapshot after, Instant now) {
        record.setState(after.state());
        record.setScore(BigDecimal.valueOf(after.score()).setScale(3, RoundingMode.HALF_UP));
        record.setDwellSeconds(after.dwellSeconds());
        record.setLevelsSeen(after.levelsSeen().toArray(String[]::new));
        record.setOpenedCount(after.openedCount());

        if (after.evidence().read() != before.evidence().read()) {
            record.setReadState(after.evidence().read());
            record.setReadAt(now);
        }
        if (after.evidence().practice() != before.evidence().practice()) {
            record.setPracticeState(after.evidence().practice());
            record.setPracticeAt(now);
        }
        if (after.evidence().assess() != before.evidence().assess()) {
            record.setAssessState(after.evidence().assess());
            record.setAssessAt(now);
        }
        if (after.evidence().apply() != before.evidence().apply()) {
            record.setApplyState(after.evidence().apply());
            record.setApplyAt(now);
        }
    }

    private static Mastery view(MasteryRecordEntity record, String slug, UnitFacts facts) {
        return new Mastery(
                record.getUnitId(),
                slug,
                record.getState().name(),
                record.getScore().doubleValue(),
                new Evidence(
                        new Chip(record.getReadState().code(), record.getReadAt()),
                        new Chip(record.getPracticeState().code(), record.getPracticeAt()),
                        new Chip(record.getAssessState().code(), record.getAssessAt()),
                        new Chip(record.getApplyState().code(), record.getApplyAt())),
                record.getDwellSeconds(),
                MasteryPolicy.readFloorSeconds(facts),
                List.of(record.getLevelsSeen()),
                record.getOpenedCount(),
                record.getFirstOpenedAt(),
                record.getLastEventAt());
    }

    private static Mastery unknown(UUID unitId, String slug, UnitFacts facts) {
        return new Mastery(
                unitId,
                slug,
                MasteryState.UNKNOWN.name(),
                0,
                Evidence.none(),
                0,
                MasteryPolicy.readFloorSeconds(facts),
                List.of(),
                0,
                null,
                null);
    }

    private String payload(LearningEventKind kind, int seconds, String level) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (kind == LearningEventKind.DWELL) {
            // Both numbers are kept: what the client asked for, and what was allowed to count.
            body.put("claimedSeconds", seconds);
            body.put("countedSeconds", clamp(seconds));
        }
        if (level != null && !level.isBlank()) {
            body.put("level", level);
        }
        try {
            return json.writeValueAsString(body);
        } catch (JsonProcessingException impossible) {
            throw new IllegalStateException("Could not serialise event payload", impossible);
        }
    }

    private static int clamp(int seconds) {
        if (seconds <= 0) {
            return 0;
        }
        return Math.min(seconds, MasteryPolicy.MAX_DWELL_SECONDS_PER_EVENT);
    }

    /** Exposed for the dashboard: how much of the evidence model is actually in use. */
    @Transactional(readOnly = true)
    public boolean hasAnyEvidence(UUID userId) {
        return records.findByUserId(userId).stream()
                .anyMatch(record -> record.getReadState() != EvidenceState.EMPTY);
    }
}
