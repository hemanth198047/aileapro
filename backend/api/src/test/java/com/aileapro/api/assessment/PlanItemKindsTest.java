package com.aileapro.api.assessment;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The gap this covers is structural, not hypothetical.
 *
 * <p>The test database is built from the JPA entities with Flyway switched off, because the
 * migrations need pgvector and H2 does not have it. That makes every test in the suite blind
 * to CHECK constraints: a kind the code writes and the schema forbids passes the whole suite
 * and fails the first time a real learner reaches it.
 *
 * <p>Reading the constraint out of the migration is not a substitute for running the
 * migration, and it is not meant to be. It closes the one case that has actually bitten —
 * adding a plan item kind and forgetting the constraint that lists them.
 */
class PlanItemKindsTest {

    private static final Pattern ALLOWED = Pattern.compile(
            "ck_plan_items_kind\\s+CHECK\\s*\\(\\s*kind\\s+IN\\s*\\(([^)]*)\\)", Pattern.CASE_INSENSITIVE);

    @Test
    void everyKindTheDiagnosticWritesIsAllowedByTheSchema() {
        List<String> allowed = allowedKinds();
        List<String> forbidden = DiagnosticService.PLAN_ITEM_KINDS.stream()
                .filter(kind -> !allowed.contains(kind))
                .toList();
        if (!forbidden.isEmpty()) {
            throw new AssertionError("These plan item kinds would be rejected by ck_plan_items_kind: "
                    + forbidden + ". The constraint allows " + allowed);
        }
    }

    /**
     * The last constraint to be declared wins, since a later migration drops and replaces the
     * one before it. Reading only the first would check a rule the database no longer has.
     */
    private static List<String> allowedKinds() {
        List<String> kinds = new ArrayList<>();
        for (String migration : List.of("V4__learner.sql", "V5__assessment.sql")) {
            Matcher matcher = ALLOWED.matcher(read(migration));
            while (matcher.find()) {
                kinds.clear();
                for (String value : matcher.group(1).split(",")) {
                    kinds.add(value.trim().replace("'", ""));
                }
            }
        }
        if (kinds.isEmpty()) {
            throw new AssertionError("No ck_plan_items_kind constraint was found in the migrations");
        }
        return kinds;
    }

    private static String read(String migration) {
        try (InputStream in = PlanItemKindsTest.class.getResourceAsStream("/db/migration/" + migration)) {
            if (in == null) {
                throw new AssertionError("Migration " + migration + " is not on the classpath");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception unreadable) {
            throw new AssertionError("Could not read " + migration, unreadable);
        }
    }
}
