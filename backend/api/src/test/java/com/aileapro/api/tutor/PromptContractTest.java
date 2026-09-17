package com.aileapro.api.tutor;

import com.aileapro.ai.Persona;
import com.aileapro.ai.PromptTemplate;
import com.aileapro.ai.TutorPolicy;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Holds the authored prompts to the code that renders them.
 *
 * <p>The failure this exists to catch is quiet. Rename a slot in the policy, forget the
 * template, and the prompt renders with a hole in it; the model fills the hole with something
 * plausible and the answer reads perfectly while teaching whatever it likes. Nothing throws,
 * nothing logs, and no test that only checks HTTP status codes would notice.
 */
class PromptContractTest {

    private static JsonNode prompts() throws Exception {
        try (InputStream in = PromptContractTest.class.getResourceAsStream("/prompts/prompts.yml")) {
            assertNotNull(in, "prompts/prompts.yml is not on the classpath");
            return JsonMapper.builder(new YAMLFactory()).build().readTree(in);
        }
    }

    private static JsonNode named(String name) throws Exception {
        for (JsonNode prompt : prompts().get("prompts")) {
            if (name.equals(prompt.get("name").asText())) {
                return prompt;
            }
        }
        throw new AssertionError("No authored prompt named " + name);
    }

    @Test
    void everyPlaceholderInTheTutorPromptIsOneThePolicyFills() throws Exception {
        Set<String> filled = TutorPolicy.decide(new TutorPolicy.Ask(
                        "why",
                        null,
                        Persona.PATIENT_TEACHER,
                        TutorPolicy.UnitContext.none(),
                        0,
                        false))
                .slots()
                .keySet();

        for (JsonNode version : named(TutorPolicy.TUTOR_PROMPT).get("versions")) {
            Set<String> needed = new HashSet<>(PromptTemplate.placeholders(version.get("template").asText()));
            needed.removeAll(filled);
            assertTrue(
                    needed.isEmpty(),
                    "Version " + version.get("version").asInt() + " uses placeholders nothing fills: " + needed);
        }
    }

    @Test
    void everyProducedSlotIsActuallyUsedByTheCurrentTemplate() throws Exception {
        // The other direction, which catches the opposite mistake: a rule carefully written
        // into the policy and then never sent to the model.
        JsonNode versions = named(TutorPolicy.TUTOR_PROMPT).get("versions");
        JsonNode latest = versions.get(versions.size() - 1);
        Set<String> used = PromptTemplate.placeholders(latest.get("template").asText());

        Set<String> produced = new HashSet<>(TutorPolicy.decide(new TutorPolicy.Ask(
                        "why",
                        null,
                        Persona.PATIENT_TEACHER,
                        TutorPolicy.UnitContext.none(),
                        0,
                        false))
                .slots()
                .keySet());

        produced.removeIf(used::contains);
        assertTrue(produced.isEmpty(), "The policy builds slots the prompt never sends: " + produced);
    }

    @Test
    void renderingRefusesToLeaveAHoleInASystemPrompt() {
        IllegalArgumentException blew = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> PromptTemplate.render("Teach like this: {{persona}} and {{reveal}}",
                        java.util.Map.of("persona", "warmly")));

        assertTrue(blew.getMessage().contains("reveal"), "The error has to name the missing slot");
    }

    @Test
    void versionNumbersAreUniqueAndTemplatesAreNotBlank() throws Exception {
        for (JsonNode prompt : prompts().get("prompts")) {
            List<Integer> seen = new ArrayList<>();
            for (JsonNode version : prompt.get("versions")) {
                int number = version.get("version").asInt();
                assertFalse(
                        seen.contains(number),
                        prompt.get("name").asText() + " declares version " + number + " twice");
                seen.add(number);
                assertFalse(version.get("template").asText().isBlank());
            }
        }
    }

    @Test
    void theSystemMessageActuallySentCarriesEveryRuleThePolicyAlsoEnforces() throws Exception {
        // Asserted against the rendered message rather than the raw template, because the rules
        // arrive from two places — the authored text and the policy's slots — and what matters
        // is that the model receives all of them. Both layers, same rules: the policy is what
        // cannot be argued round, the prompt is what makes the model behave well in the cases
        // the policy cannot see.
        JsonNode versions = named(TutorPolicy.TUTOR_PROMPT).get("versions");
        String template = versions.get(versions.size() - 1).get("template").asText();
        TutorPolicy.Turn turn = TutorPolicy.decide(new TutorPolicy.Ask(
                "why does this print twice",
                null,
                Persona.PATIENT_TEACHER,
                TutorPolicy.UnitContext.none(),
                0,
                false));

        String sent = PromptTemplate.render(template, turn.slots()).toLowerCase();

        assertTrue(sent.contains("hints before answers"), "The teaching rule is missing");
        assertTrue(sent.contains("graded"), "The answer-key refusal is missing");
        assertTrue(sent.contains("untrusted-data"), "The data channel is never explained");
        assertTrue(sent.contains("do not cite"), "The grounding rule is missing");
        assertTrue(sent.contains("do not give the answer"), "The reveal rung was not sent");
    }

    @Test
    void theFixtureClientStillRecognisesTheRevealInstructions() {
        // FakeChatClient keys off phrases in these strings to answer at the right rung. If the
        // wording changes, every fixture silently becomes a full explanation, and the hint
        // tests would keep passing while testing nothing.
        String hint = TutorPolicy.decide(new TutorPolicy.Ask(
                        "why", null, Persona.PATIENT_TEACHER, TutorPolicy.UnitContext.none(), 0, false))
                .slots()
                .get("reveal");
        String guided = TutorPolicy.decide(new TutorPolicy.Ask(
                        "i think it is two",
                        null,
                        Persona.PATIENT_TEACHER,
                        TutorPolicy.UnitContext.none(),
                        0,
                        true))
                .slots()
                .get("reveal");

        assertTrue(hint.contains("Do not give the answer"));
        assertTrue(guided.contains("leaving the final step"));
        assertEquals(TutorPolicy.Reveal.values().length, 3, "A new rung needs a fixture branch");
    }
}
