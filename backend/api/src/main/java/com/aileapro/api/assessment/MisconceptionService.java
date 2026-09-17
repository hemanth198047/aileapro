package com.aileapro.api.assessment;

import com.aileapro.api.assessment.AssessmentViews.Misconception;
import com.aileapro.api.curriculum.CurriculumQueryService;
import com.aileapro.api.curriculum.CurriculumViews.UnitRef;
import com.aileapro.api.learning.GradedMasteryService;
import com.aileapro.domain.LearningEventKind;
import com.aileapro.domain.Volatility;
import com.aileapro.persistence.MisconceptionCatalogEntity;
import com.aileapro.persistence.MisconceptionFlagEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Raises and clears named false beliefs.
 *
 * <p>Two rules make this useful rather than punitive. A flag always resolves to the block
 * that teaches against it, so being told you hold a misconception comes with the page that
 * addresses it. And a flag is cleared by answering an item that carries the same code
 * correctly — the same evidence that raised it, in the other direction — rather than by
 * waiting, reading, or clicking "understood".
 *
 * <p>Cleared flags are kept. That someone once held a belief is useful for teaching them,
 * and a record that vanishes cannot be questioned.
 */
@Service
@Transactional
public class MisconceptionService {

    private final MisconceptionCatalogRepository catalog;
    private final MisconceptionFlagRepository flags;
    private final CurriculumQueryService curriculum;
    private final UnitFactsService unitFacts;
    private final GradedMasteryService graded;

    public MisconceptionService(
            MisconceptionCatalogRepository catalog,
            MisconceptionFlagRepository flags,
            CurriculumQueryService curriculum,
            UnitFactsService unitFacts,
            GradedMasteryService graded) {
        this.catalog = catalog;
        this.flags = flags;
        this.curriculum = curriculum;
        this.unitFacts = unitFacts;
        this.graded = graded;
    }

    /**
     * Raises each code, then re-syncs the open count on every unit a raised code belongs to.
     *
     * <p>The count lands on the unit that <em>teaches</em> the misconception, not on the unit
     * whose quiz surfaced it. Someone who gets a matrix question wrong because they think
     * multiplication commutes has a problem on the matmul page, and pinning it there is what
     * makes the next recommendation point at the right thing.
     */
    public List<Misconception> raise(UUID userId, Collection<String> codes, UUID attemptId) {
        if (codes.isEmpty()) {
            return List.of();
        }
        List<MisconceptionCatalogEntity> known = catalog.findByCodeIn(codes);
        List<Misconception> raised = new ArrayList<>(known.size());
        for (MisconceptionCatalogEntity entry : known) {
            MisconceptionFlagEntity flag = flags.findByUserIdAndCode(userId, entry.getCode())
                    .orElseGet(() -> {
                        MisconceptionFlagEntity fresh = new MisconceptionFlagEntity();
                        fresh.setId(UUID.randomUUID());
                        fresh.setUserId(userId);
                        fresh.setCode(entry.getCode());
                        fresh.setTimesRaised(0);
                        return fresh;
                    });
            flag.setUnitId(entry.getUnitId());
            flag.setOpen(true);
            flag.setClearedAt(null);
            flag.setRaisedAt(Instant.now());
            flag.setTimesRaised(flag.getTimesRaised() + 1);
            flag.setLastAttemptId(attemptId);
            flags.save(flag);
            raised.add(view(entry));
        }
        known.stream().map(MisconceptionCatalogEntity::getUnitId).distinct()
                .forEach(unitId -> sync(userId, unitId, LearningEventKind.MISCONCEPTION_RAISED, codes.iterator().next()));
        return raised;
    }

    /** Clears codes the learner has now answered correctly. Unknown or already-clear codes are ignored. */
    public void clear(UUID userId, Collection<String> codes) {
        if (codes.isEmpty()) {
            return;
        }
        List<UUID> touched = new ArrayList<>();
        for (String code : codes) {
            Optional<MisconceptionFlagEntity> found = flags.findByUserIdAndCode(userId, code);
            if (found.isEmpty() || !found.get().isOpen()) {
                continue;
            }
            MisconceptionFlagEntity flag = found.get();
            flag.setOpen(false);
            flag.setClearedAt(Instant.now());
            flags.save(flag);
            touched.add(flag.getUnitId());
        }
        touched.stream().distinct().forEach(
                unitId -> sync(userId, unitId, LearningEventKind.MISCONCEPTION_CLEARED, codes.iterator().next()));
    }

    @Transactional(readOnly = true)
    public List<Misconception> openFor(UUID userId) {
        List<MisconceptionFlagEntity> open = flags.findByUserIdAndOpen(userId, true);
        if (open.isEmpty()) {
            return List.of();
        }
        List<String> codes = open.stream().map(MisconceptionFlagEntity::getCode).toList();
        return describe(catalog.findByCodeIn(codes));
    }

    @Transactional(readOnly = true)
    public List<Misconception> openFor(UUID userId, Collection<UUID> unitIds) {
        if (unitIds.isEmpty()) {
            return List.of();
        }
        List<MisconceptionFlagEntity> open = flags.findByUserIdAndUnitIdInAndOpen(userId, unitIds, true);
        if (open.isEmpty()) {
            return List.of();
        }
        return describe(catalog.findByCodeIn(open.stream().map(MisconceptionFlagEntity::getCode).toList()));
    }

    @Transactional(readOnly = true)
    public List<Misconception> describe(Collection<String> codes) {
        return codes.isEmpty() ? List.of() : describe(catalog.findByCodeIn(codes));
    }

    private List<Misconception> describe(List<MisconceptionCatalogEntity> entries) {
        if (entries.isEmpty()) {
            return List.of();
        }
        Map<UUID, UnitRef> refs = curriculum.publishedRefs(
                entries.stream().map(MisconceptionCatalogEntity::getUnitId).distinct().toList());
        Map<String, Misconception> out = new LinkedHashMap<>();
        for (MisconceptionCatalogEntity entry : entries) {
            UnitRef unit = refs.get(entry.getUnitId());
            out.put(entry.getCode(), new Misconception(
                    entry.getCode(),
                    entry.getClaim(),
                    unit,
                    unit == null ? null : "/app/learn/" + unit.slug() + "#misconception-" + entry.getCode()));
        }
        return List.copyOf(out.values());
    }

    private Misconception view(MisconceptionCatalogEntity entry) {
        return describe(List.of(entry)).getFirst();
    }

    /**
     * Pushes the current open count onto the unit's projection, which is the one input that
     * can move a mastery state <em>down</em>.
     */
    private void sync(UUID userId, UUID unitId, LearningEventKind kind, String code) {
        int open = flags.countByUserIdAndUnitIdAndOpen(userId, unitId, true);
        graded.setOpenMisconceptions(
                userId, unitId, unitFacts.factsFor(unitId), volatilityOf(unitId), open, kind, code);
    }

    /**
     * Volatility only affects the recency half-life in the composite score. Reading the unit
     * for it on every flag would be a query per flag for a term that moves the third decimal
     * place, so the middle setting stands in.
     */
    private Volatility volatilityOf(UUID unitId) {
        return Volatility.MEDIUM;
    }
}
