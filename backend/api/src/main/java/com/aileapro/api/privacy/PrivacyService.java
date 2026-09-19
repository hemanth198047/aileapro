package com.aileapro.api.privacy;

import com.aileapro.api.UserRepository;
import com.aileapro.api.UserRoleRepository;
import com.aileapro.api.assessment.AttemptRepository;
import com.aileapro.api.lab.LabRunRepository;
import com.aileapro.api.learning.LearnerProfileRepository;
import com.aileapro.api.learning.MasteryRecordRepository;
import com.aileapro.api.learning.NoteRepository;
import com.aileapro.api.platform.ApiException;
import com.aileapro.api.tutor.ConversationRepository;
import com.aileapro.api.tutor.MessageRepository;
import com.aileapro.persistence.AttemptEntity;
import com.aileapro.persistence.ConversationEntity;
import com.aileapro.persistence.LabRunEntity;
import com.aileapro.persistence.LearnerProfileEntity;
import com.aileapro.persistence.MasteryRecordEntity;
import com.aileapro.persistence.MessageEntity;
import com.aileapro.persistence.NoteEntity;
import com.aileapro.persistence.UserEntity;
import com.aileapro.persistence.UserRoleEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * GDPR/CCPA export and anonymize. Learning aggregates stay for integrity; PII and free text go.
 */
@Service
public class PrivacyService {

    private final UserRepository users;
    private final UserRoleRepository roles;
    private final LearnerProfileRepository profiles;
    private final MasteryRecordRepository mastery;
    private final AttemptRepository attempts;
    private final NoteRepository notes;
    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final LabRunRepository labRuns;

    public PrivacyService(
            UserRepository users,
            UserRoleRepository roles,
            LearnerProfileRepository profiles,
            MasteryRecordRepository mastery,
            AttemptRepository attempts,
            NoteRepository notes,
            ConversationRepository conversations,
            MessageRepository messages,
            LabRunRepository labRuns) {
        this.users = users;
        this.roles = roles;
        this.profiles = profiles;
        this.mastery = mastery;
        this.attempts = attempts;
        this.notes = notes;
        this.conversations = conversations;
        this.messages = messages;
        this.labRuns = labRuns;
    }

    public record AnonymizeResult(UUID userId, String status, Instant at) {
    }

    @Transactional(readOnly = true)
    public Map<String, Object> export(UUID userId) {
        UserEntity user = users.findById(userId).orElseThrow(() -> ApiException.notFound("User"));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("exportedAt", Instant.now().toString());
        out.put("account", Map.of(
                "id", user.getId().toString(),
                "email", user.getEmail(),
                "status", user.getStatus(),
                "locale", user.getLocale(),
                "timezone", user.getTimezone(),
                "emailVerified", user.getEmailVerifiedAt() != null,
                "createdAt", user.getCreatedAt().toString(),
                "roles", roles.findByUserId(userId).stream().map(UserRoleEntity::getRoleCode).toList()));

        profiles.findById(userId).ifPresent(p -> out.put("profile", Map.of(
                "targetPathId", p.getTargetPathId() == null ? "" : p.getTargetPathId().toString(),
                "hoursPerWeek", p.getHoursPerWeek() == null ? 0 : p.getHoursPerWeek(),
                "modalities", p.getModalities() == null ? "[]" : p.getModalities(),
                "experience", p.getExperience() == null ? "" : p.getExperience(),
                "goals", p.getGoals() == null ? "" : p.getGoals(),
                "onboardedAt", p.getOnboardedAt() == null ? "" : p.getOnboardedAt().toString())));

        List<Map<String, Object>> masteryRows = new ArrayList<>();
        for (MasteryRecordEntity row : mastery.findByUserId(userId)) {
            masteryRows.add(Map.of(
                    "unitId", row.getUnitId().toString(),
                    "state", row.getState().name(),
                    "score", row.getScore(),
                    "updatedAt", row.getUpdatedAt().toString()));
        }
        out.put("mastery", masteryRows);

        List<Map<String, Object>> attemptRows = new ArrayList<>();
        for (AttemptEntity row : attempts.findByUserIdOrderByStartedAtDesc(userId)) {
            Map<String, Object> a = new LinkedHashMap<>();
            a.put("id", row.getId().toString());
            a.put("kind", row.getKind() == null ? null : row.getKind().name());
            a.put("status", row.getStatus());
            a.put("unitId", row.getUnitId() == null ? null : row.getUnitId().toString());
            a.put("startedAt", row.getStartedAt().toString());
            if (row.getSubmittedAt() != null) {
                a.put("submittedAt", row.getSubmittedAt().toString());
            }
            attemptRows.add(a);
        }
        out.put("attempts", attemptRows);

        List<Map<String, Object>> noteRows = new ArrayList<>();
        for (NoteEntity row : notes.findByUserIdOrderByCreatedAtDesc(userId)) {
            noteRows.add(Map.of(
                    "id", row.getId().toString(),
                    "unitId", row.getUnitId() == null ? "" : row.getUnitId().toString(),
                    "body", row.getBody(),
                    "createdAt", row.getCreatedAt().toString()));
        }
        out.put("notes", noteRows);

        List<Map<String, Object>> convRows = new ArrayList<>();
        for (ConversationEntity conv : conversations.findByUserId(userId)) {
            List<Map<String, Object>> msgRows = new ArrayList<>();
            for (MessageEntity msg : messages.findByConversationIdOrderBySortAsc(conv.getId())) {
                msgRows.add(Map.of(
                        "role", msg.getRole(),
                        "content", msg.getContent(),
                        "sort", msg.getSort(),
                        "createdAt", msg.getCreatedAt().toString()));
            }
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("id", conv.getId().toString());
            c.put("unitId", conv.getUnitId() == null ? null : conv.getUnitId().toString());
            c.put("persona", conv.getPersona());
            c.put("title", conv.getTitle());
            c.put("createdAt", conv.getCreatedAt().toString());
            c.put("messages", msgRows);
            convRows.add(c);
        }
        out.put("conversations", convRows);

        List<Map<String, Object>> labRows = new ArrayList<>();
        for (LabRunEntity run : labRuns.findTop20ByUserIdOrderByCreatedAtDesc(userId)) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("id", run.getId().toString());
            r.put("language", run.getLanguage());
            r.put("status", run.getStatus());
            r.put("source", run.getSource());
            r.put("stdout", run.getStdout());
            r.put("stderr", run.getStderr());
            r.put("createdAt", run.getCreatedAt().toString());
            labRows.add(r);
        }
        out.put("labRuns", labRows);
        return out;
    }

    @Transactional
    public AnonymizeResult anonymize(UUID userId) {
        UserEntity user = users.findById(userId).orElseThrow(() -> ApiException.notFound("User"));
        if ("deleted".equals(user.getStatus())) {
            return new AnonymizeResult(userId, "deleted", Instant.now());
        }

        for (NoteEntity note : notes.findByUserIdOrderByCreatedAtDesc(userId)) {
            note.setBody("");
            note.setUpdatedAt(Instant.now());
            notes.save(note);
        }

        for (ConversationEntity conv : conversations.findByUserId(userId)) {
            conv.setTitle(null);
            conversations.save(conv);
            for (MessageEntity msg : messages.findByConversationIdOrderBySortAsc(conv.getId())) {
                msg.setContent("[redacted]");
                messages.save(msg);
            }
        }

        for (LabRunEntity run : labRuns.findTop20ByUserIdOrderByCreatedAtDesc(userId)) {
            run.setSource("# redacted");
            run.setStdin(null);
            run.setStdout(null);
            run.setStderr(null);
            labRuns.save(run);
        }

        profiles.findById(userId).ifPresent(p -> {
            p.setGoals(null);
            p.setExperience(null);
            p.setModalities("[]");
            p.setTargetPathId(null);
            p.setHoursPerWeek(null);
            p.setUpdatedAt(Instant.now());
            profiles.save(p);
        });

        user.setEmail("deleted-" + userId + "@anonymized.invalid");
        user.setPasswordHash(null);
        user.setEmailVerifiedAt(null);
        user.setStatus("deleted");
        user.setUpdatedAt(Instant.now());
        users.save(user);

        return new AnonymizeResult(userId, "deleted", Instant.now());
    }
}
