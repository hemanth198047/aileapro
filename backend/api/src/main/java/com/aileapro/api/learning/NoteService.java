package com.aileapro.api.learning;

import com.aileapro.api.curriculum.CurriculumQueryService;
import com.aileapro.api.curriculum.CurriculumViews.PublishedUnit;
import com.aileapro.api.curriculum.CurriculumViews.UnitRef;
import com.aileapro.api.learning.LearningViews.Note;
import com.aileapro.api.platform.ApiException;
import com.aileapro.persistence.NoteEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Notes v0: write one, list them, delete one. No linking, no rich text, no sharing — those
 * are later, and pretending otherwise would just be a worse text box.
 *
 * <p>Notes are stored verbatim and rendered as Markdown by the client. Nothing is
 * interpreted server-side, and escaping is the renderer's job.
 */
@Service
@Transactional
public class NoteService {

    private static final int MAX_BODY_LENGTH = 20_000;

    private final NoteRepository notes;
    private final CurriculumQueryService curriculum;

    public NoteService(NoteRepository notes, CurriculumQueryService curriculum) {
        this.notes = notes;
        this.curriculum = curriculum;
    }

    public record NoteRequest(String body) {
    }

    public Note create(UUID userId, String unitIdOrSlug, NoteRequest request) {
        String body = request == null || request.body() == null ? "" : request.body().trim();
        if (body.isEmpty()) {
            throw ApiException.validation("A note needs a body");
        }
        if (body.length() > MAX_BODY_LENGTH) {
            throw ApiException.validation("A note cannot exceed " + MAX_BODY_LENGTH + " characters");
        }

        PublishedUnit unit = curriculum.publishedUnitForProgress(unitIdOrSlug);
        Instant now = Instant.now();

        NoteEntity note = new NoteEntity();
        note.setId(UUID.randomUUID());
        note.setUserId(userId);
        note.setUnitId(unit.id());
        note.setBody(body);
        note.setCreatedAt(now);
        note.setUpdatedAt(now);

        NoteEntity saved = notes.save(note);
        return new Note(saved.getId(), unit.id(), unit.slug(), unit.title(), saved.getBody(), saved.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<Note> forUnit(UUID userId, String unitIdOrSlug) {
        PublishedUnit unit = curriculum.publishedUnitForProgress(unitIdOrSlug);
        return notes.findByUserIdAndUnitIdOrderByCreatedAtDesc(userId, unit.id()).stream()
                .map(note -> new Note(
                        note.getId(), unit.id(), unit.slug(), unit.title(), note.getBody(), note.getCreatedAt()))
                .toList();
    }

    /**
     * All of the learner's notes, newest first. A note whose unit was unpublished or deleted
     * still comes back — losing someone's writing because the curriculum moved would be the
     * wrong trade.
     */
    @Transactional(readOnly = true)
    public List<Note> all(UUID userId) {
        List<NoteEntity> rows = notes.findByUserIdOrderByCreatedAtDesc(userId);
        Set<UUID> unitIds = rows.stream()
                .map(NoteEntity::getUnitId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        Map<UUID, UnitRef> refs = curriculum.publishedRefs(unitIds);

        return rows.stream()
                .map(note -> {
                    UnitRef ref = note.getUnitId() == null ? null : refs.get(note.getUnitId());
                    return new Note(
                            note.getId(),
                            note.getUnitId(),
                            ref == null ? null : ref.slug(),
                            ref == null ? null : ref.title(),
                            note.getBody(),
                            note.getCreatedAt());
                })
                .toList();
    }

    public void delete(UUID userId, UUID noteId) {
        NoteEntity note = notes.findByIdAndUserId(noteId, userId)
                .orElseThrow(() -> ApiException.notFound("No note " + noteId));
        notes.delete(note);
    }
}
