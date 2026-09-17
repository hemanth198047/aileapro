package com.aileapro.api.tutor;

import com.aileapro.ai.Persona;
import com.aileapro.api.ai.UsageMeter;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks the values the code writes against the CHECK constraints that would reject them.
 *
 * <p>This covers a structural blind spot rather than a hypothetical one. The test database is
 * built from the JPA entities with Flyway switched off, because the migrations need pgvector
 * and H2 does not have it — so no test in the suite sees a CHECK constraint. A value the code
 * writes and the schema forbids passes the entire suite and then fails on the first learner
 * who reaches it, which is exactly what happened in the previous phase with plan item kinds.
 *
 * <p>Reading the constraint out of the migration is not a substitute for running the migration
 * and is not meant to be. It closes the case that has actually bitten.
 */
class AiConstraintsTest {

    private static final String MIGRATION = "V6__ai_tutor.sql";

    @Test
    void everyOutcomeTheMeterWritesIsAllowed() {
        List<String> allowed = allowedValues("ck_ai_usage_outcome", "outcome");
        List<String> forbidden = Arrays.stream(UsageMeter.Outcome.values())
                .map(UsageMeter.Outcome::wire)
                .filter(outcome -> !allowed.contains(outcome))
                .toList();

        if (!forbidden.isEmpty()) {
            throw new AssertionError(
                    "These usage outcomes would be rejected by the database: " + forbidden
                            + ". The constraint allows " + allowed);
        }
    }

    @Test
    void everyPersonaTheUiOffersIsAllowed() {
        List<String> allowed = allowedValues("ck_conversations_persona", "persona");
        List<String> forbidden = Arrays.stream(Persona.values())
                .map(Persona::slug)
                .filter(persona -> !allowed.contains(persona))
                .toList();

        if (!forbidden.isEmpty()) {
            throw new AssertionError(
                    "These personas would be rejected by the database: " + forbidden
                            + ". The constraint allows " + allowed);
        }
    }

    @Test
    void everyRoleTheTranscriptStoresIsAllowed() {
        List<String> allowed = allowedValues("ck_messages_role", "role");

        for (String role : List.of("user", "assistant", "system")) {
            if (!allowed.contains(role)) {
                throw new AssertionError(
                        "The transcript stores the role '" + role + "' and the schema allows only " + allowed);
            }
        }
    }

    /** Pulls the {@code IN (...)} list out of a named constraint in the migration. */
    private static List<String> allowedValues(String constraint, String column) {
        Pattern pattern = Pattern.compile(
                constraint + "\\s+CHECK\\s*\\(\\s*" + column + "\\s+IN\\s*\\(([^)]*)\\)",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(read());

        List<String> values = new ArrayList<>();
        while (matcher.find()) {
            // A later declaration wins, since a migration can drop and replace an earlier one.
            values.clear();
            for (String value : matcher.group(1).split(",")) {
                values.add(value.trim().replace("'", ""));
            }
        }
        if (values.isEmpty()) {
            throw new AssertionError("No " + constraint + " constraint was found in " + MIGRATION);
        }
        return values;
    }

    private static String read() {
        try (InputStream in = AiConstraintsTest.class.getResourceAsStream("/db/migration/" + MIGRATION)) {
            if (in == null) {
                throw new AssertionError(MIGRATION + " is not on the classpath");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception unreadable) {
            throw new AssertionError("Could not read " + MIGRATION, unreadable);
        }
    }
}
