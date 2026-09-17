package com.aileapro.api.assessment;

import com.aileapro.api.assessment.AssessmentViews.Choice;
import com.aileapro.domain.ItemScoring;
import com.aileapro.domain.ItemType;
import com.aileapro.persistence.QuestionEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Turns a stored item into the three things the rest of the code needs: the choices to show,
 * the key to mark against, and the misconception attached to each distractor.
 *
 * <p>Parsing lives here rather than in the service so that the display path and the marking
 * path read the same field names. The failure this prevents is specific and nasty: a
 * mismatch would show one set of options and mark against another.
 */
final class ItemKeys {

    private ItemKeys() {
    }

    /**
     * @param choices        options as shown, answer flags stripped
     * @param key            what {@link ItemScoring} marks against
     * @param byChoice       misconception code per choice id, for diagnostic distractors
     */
    record Parsed(List<Choice> choices, ItemScoring.Key key, Map<String, String> byChoice) {
    }

    static Parsed parse(QuestionEntity item, ObjectMapper json) {
        JsonNode data;
        try {
            data = json.readTree(item.getData() == null || item.getData().isBlank() ? "{}" : item.getData());
        } catch (com.fasterxml.jackson.core.JsonProcessingException broken) {
            throw new IllegalStateException("Unreadable key for item '" + item.getSlug() + "'", broken);
        }

        ItemType type = item.getItemType();
        if (type == ItemType.SHORT_TEXT) {
            if (data.hasNonNull("answer")) {
                return new Parsed(
                        List.of(),
                        ItemScoring.Key.number(
                                data.get("answer").asDouble(), data.path("tolerance").asDouble(0.0001)),
                        Map.of());
            }
            return new Parsed(List.of(), ItemScoring.Key.text(strings(data, "accepted")), Map.of());
        }
        if (type == ItemType.CODING_STUB) {
            return new Parsed(List.of(), ItemScoring.Key.code(strings(data, "accepted")), Map.of());
        }

        List<Choice> choices = new ArrayList<>();
        Set<String> correct = new LinkedHashSet<>();
        Map<String, String> byChoice = new LinkedHashMap<>();
        for (JsonNode choice : data.path("choices")) {
            String id = choice.path("id").asText();
            choices.add(new Choice(id, choice.path("text").asText()));
            if (choice.path("correct").asBoolean(false)) {
                correct.add(id);
            }
            if (choice.hasNonNull("misconception")) {
                byChoice.put(id, choice.get("misconception").asText());
            }
        }
        return new Parsed(
                List.copyOf(choices),
                new ItemScoring.Key(type, correct, List.of(), null, null),
                Map.copyOf(byChoice));
    }

    /** The submitted answer, in the form the scorer expects. */
    static ItemScoring.Response response(ItemType type, List<String> selectedIds, String text) {
        if (type == ItemType.SHORT_TEXT || type == ItemType.CODING_STUB) {
            return ItemScoring.Response.text(text);
        }
        return new ItemScoring.Response(
                selectedIds == null ? Set.of() : new LinkedHashSet<>(selectedIds), null);
    }

    private static String[] strings(JsonNode node, String field) {
        List<String> values = new ArrayList<>();
        for (JsonNode value : node.path(field)) {
            values.add(value.asText());
        }
        return values.toArray(String[]::new);
    }
}
