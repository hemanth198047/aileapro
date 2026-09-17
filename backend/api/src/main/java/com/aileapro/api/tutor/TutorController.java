package com.aileapro.api.tutor;

import com.aileapro.api.auth.AileaproUserPrincipal;
import com.aileapro.api.platform.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

/** The tutor. Conversations, one streaming turn endpoint, and the honesty flags. */
@RestController
@RequestMapping("/v1/tutor")
public class TutorController {

    private static final Logger log = LoggerFactory.getLogger(TutorController.class);

    /**
     * Long enough for a slow provider on a long answer, short enough that a wedged connection
     * frees itself. A learner who has waited two minutes has already reloaded.
     */
    private static final long STREAM_TIMEOUT_MS = 120_000;

    private final TutorService tutor;
    private final TutorTranscript transcript;
    private final TutorContextService context;
    private final ExecutorService streams;

    public TutorController(
            TutorService tutor,
            TutorTranscript transcript,
            TutorContextService context,
            ExecutorService tutorStreamExecutor) {
        this.tutor = tutor;
        this.transcript = transcript;
        this.context = context;
        this.streams = tutorStreamExecutor;
    }

    public record StartRequest(String unitSlug, String persona) {
    }

    public record MessageRequest(String text, String action) {
    }

    @PostMapping("/conversations")
    public TutorViews.ConversationDetail start(
            @AuthenticationPrincipal AileaproUserPrincipal principal,
            @RequestBody(required = false) StartRequest request) {
        StartRequest safe = request == null ? new StartRequest(null, null) : request;
        return transcript.start(principal.getId(), safe.unitSlug(), safe.persona());
    }

    @GetMapping("/conversations")
    public List<TutorViews.ConversationSummary> list(
            @AuthenticationPrincipal AileaproUserPrincipal principal,
            @RequestParam(required = false) String unit) {
        return transcript.list(principal.getId(), unit);
    }

    @GetMapping("/conversations/{id}")
    public TutorViews.ConversationDetail detail(
            @AuthenticationPrincipal AileaproUserPrincipal principal, @PathVariable UUID id) {
        return transcript.detail(principal.getId(), id);
    }

    @DeleteMapping("/conversations/{id}")
    public Map<String, Boolean> delete(
            @AuthenticationPrincipal AileaproUserPrincipal principal, @PathVariable UUID id) {
        transcript.delete(principal.getId(), id);
        return Map.of("deleted", true);
    }

    @GetMapping("/conversations/{id}/sources")
    public Map<String, Object> sources(
            @AuthenticationPrincipal AileaproUserPrincipal principal, @PathVariable UUID id) {
        return tutor.sources(principal.getId(), id);
    }

    @GetMapping("/status")
    public TutorViews.Status status(@AuthenticationPrincipal AileaproUserPrincipal principal) {
        return tutor.status(principal.getId());
    }

    /** What the tutor knows about the learner on a unit, so the learner can see it too. */
    @GetMapping("/context")
    public TutorViews.Context context(
            @AuthenticationPrincipal AileaproUserPrincipal principal,
            @RequestParam(required = false) String unit) {
        return context.viewForSlug(principal.getId(), unit);
    }

    /**
     * One turn, streamed.
     *
     * <p>Server-sent events over a POST, which means the browser reads the response body rather
     * than using {@code EventSource} — a deliberate trade, because {@code EventSource} cannot
     * send a body, a CSRF header, or anything but a GET, and a tutor question does not belong
     * in a URL.
     *
     * <p>Three event names, and the last two are the contract. {@code token} carries text.
     * {@code done} carries the stored message id and whether a fixture wrote it. {@code error}
     * carries a code and a message, because a stream that simply stops leaves the client unable
     * to tell a finished answer from a dropped one.
     */
    @PostMapping(value = "/conversations/{id}/messages", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter message(
            @AuthenticationPrincipal AileaproUserPrincipal principal,
            @PathVariable UUID id,
            @RequestBody MessageRequest request) {
        MessageRequest safe = request == null ? new MessageRequest(null, null) : request;
        UUID userId = principal.getId();
        String requestId = MDC.get("requestId");

        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        streams.execute(() -> {
            // The worker is a different thread, so the request id has to be carried over by
            // hand or every log line and usage row from this turn loses its trail.
            if (requestId != null) {
                MDC.put("requestId", requestId);
            }
            try {
                TutorService.TurnResult result = tutor.turn(
                        userId,
                        id,
                        safe.text(),
                        safe.action(),
                        chunk -> send(emitter, "token", Map.of("text", chunk)));
                send(emitter, "done", Map.of(
                        "messageId", result.messageId().toString(),
                        "development", result.development(),
                        "refusalCode", result.refusalCode() == null ? "" : result.refusalCode()));
                emitter.complete();
            } catch (ApiException expected) {
                send(emitter, "error", Map.of(
                        "code", expected.getCode(), "message", expected.getMessage()));
                emitter.complete();
            } catch (RuntimeException unexpected) {
                log.error("Tutor turn failed", unexpected);
                send(emitter, "error", Map.of(
                        "code", "internal_error",
                        "message", "The tutor failed on our side. Nothing was charged."));
                emitter.complete();
            } finally {
                MDC.remove("requestId");
            }
        });
        return emitter;
    }

    /**
     * Sends one event, swallowing the failure that means the learner closed the tab.
     *
     * <p>A client disconnecting mid-answer is normal and is not an error worth a stack trace.
     * The turn keeps running to completion so the transcript is not left with a question and no
     * answer — which is the one thing that would make reopening the panel confusing.
     */
    private static void send(SseEmitter emitter, String name, Map<String, ?> data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data));
        } catch (IOException | IllegalStateException gone) {
            log.debug("Tutor stream closed before '{}' could be sent", name);
        }
    }
}
