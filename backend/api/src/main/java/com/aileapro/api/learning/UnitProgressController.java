package com.aileapro.api.learning;

import com.aileapro.api.auth.AileaproUserPrincipal;
import com.aileapro.api.learning.LearningViews.Mastery;
import com.aileapro.api.learning.LearningViews.Note;
import com.aileapro.api.learning.NoteService.NoteRequest;
import com.aileapro.api.platform.ApiException;
import com.aileapro.domain.LearningEventKind;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Learner state on one unit: what they did, what it is worth, and what they wrote down.
 *
 * <p>The event route is a POST that returns the resulting standing, so the client never has
 * to guess what an event earned — the server's answer is the only version of that story.
 */
@RestController
@RequestMapping("/v1/units/{idOrSlug}")
public class UnitProgressController {

    private final MasteryService mastery;
    private final NoteService notes;

    public UnitProgressController(MasteryService mastery, NoteService notes) {
        this.mastery = mastery;
        this.notes = notes;
    }

    /**
     * @param kind    one of the learning event kinds
     * @param seconds for a dwell heartbeat; clamped server-side however large it arrives
     * @param level   for an explanation level change
     */
    public record EventRequest(String kind, Integer seconds, String level) {
    }

    @PostMapping("/events")
    public Mastery event(
            @AuthenticationPrincipal AileaproUserPrincipal principal,
            @PathVariable String idOrSlug,
            @RequestBody EventRequest request) {
        if (request == null || request.kind() == null || request.kind().isBlank()) {
            throw ApiException.validation("kind is required");
        }
        LearningEventKind kind = parseKind(request.kind());
        int seconds = request.seconds() == null ? 0 : request.seconds();
        return mastery.record(principal.getId(), idOrSlug, kind, seconds, request.level());
    }

    @GetMapping("/mastery")
    public Mastery mastery(
            @AuthenticationPrincipal AileaproUserPrincipal principal, @PathVariable String idOrSlug) {
        return mastery.mastery(principal.getId(), idOrSlug);
    }

    @GetMapping("/notes")
    public List<Note> notes(
            @AuthenticationPrincipal AileaproUserPrincipal principal, @PathVariable String idOrSlug) {
        return notes.forUnit(principal.getId(), idOrSlug);
    }

    @PostMapping("/notes")
    @ResponseStatus(HttpStatus.CREATED)
    public Note createNote(
            @AuthenticationPrincipal AileaproUserPrincipal principal,
            @PathVariable String idOrSlug,
            @RequestBody NoteRequest request) {
        return notes.create(principal.getId(), idOrSlug, request);
    }

    @DeleteMapping("/notes/{noteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNote(
            @AuthenticationPrincipal AileaproUserPrincipal principal,
            @PathVariable String idOrSlug,
            @PathVariable UUID noteId) {
        notes.delete(principal.getId(), noteId);
    }

    /** Parsed by hand so an unknown kind is a 400 naming the valid ones, not a 500. */
    private static LearningEventKind parseKind(String raw) {
        try {
            return LearningEventKind.fromCode(raw.strip().toLowerCase());
        } catch (IllegalArgumentException unknown) {
            throw ApiException.validation("Unknown event kind '" + raw + "'. Valid kinds: "
                    + Arrays.stream(LearningEventKind.values()).map(LearningEventKind::code).toList());
        }
    }
}
