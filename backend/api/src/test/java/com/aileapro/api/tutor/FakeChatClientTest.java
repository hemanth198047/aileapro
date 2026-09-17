package com.aileapro.api.tutor;

import com.aileapro.ai.ChatMessage;
import com.aileapro.ai.ChatRequest;
import com.aileapro.ai.ChatResult;
import com.aileapro.ai.FakeChatClient;
import com.aileapro.ai.Persona;
import com.aileapro.ai.TutorPolicy;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The development implementation, held to the two properties that make it safe to have.
 *
 * <p>It must be deterministic, or every test built on it becomes flaky. And it must announce
 * itself in its own text, because the one genuinely dangerous outcome of this phase is a
 * fixture being read as teaching.
 */
class FakeChatClientTest {

    private final FakeChatClient client = new FakeChatClient();

    private static ChatRequest requestAt(TutorPolicy.Reveal reveal, String question) {
        TutorPolicy.Ask ask = new TutorPolicy.Ask(
                question,
                null,
                Persona.PATIENT_TEACHER,
                TutorPolicy.UnitContext.none(),
                reveal == TutorPolicy.Reveal.FULL ? 3 : 0,
                reveal == TutorPolicy.Reveal.GUIDED);
        TutorPolicy.Turn turn = TutorPolicy.decide(ask);
        assertEquals(reveal, turn.reveal(), "The fixture test needs the rung it asked for");

        return new ChatRequest(
                "fixture",
                List.of(
                        ChatMessage.system("Reveal rule: " + turn.slots().get("reveal")),
                        ChatMessage.user(question)),
                0.4,
                400);
    }

    @Test
    void theSameQuestionAlwaysGetsTheSameAnswer() {
        ChatRequest request = requestAt(TutorPolicy.Reveal.HINT, "why is a list mutable");

        assertEquals(client.complete(request).text(), client.complete(request).text());
    }

    @Test
    void everyReplySaysItIsAFixture() {
        for (TutorPolicy.Reveal reveal : TutorPolicy.Reveal.values()) {
            String text = client.complete(requestAt(reveal, "explain slicing")).text();

            assertTrue(
                    text.contains("Development implementation"),
                    reveal + " produced a reply that could be mistaken for teaching");
        }
    }

    @Test
    void theFixtureObeysTheRungItWasGiven() {
        String hint = client.complete(requestAt(TutorPolicy.Reveal.HINT, "why is a tuple immutable")).text();
        String full = client.complete(requestAt(TutorPolicy.Reveal.FULL, "why is a tuple immutable")).text();

        assertTrue(hint.contains("?"), "A hint that asks nothing is not a hint");
        assertTrue(hint.length() < full.length(), "The hint should not be the long answer");
        assertTrue(full.contains("Where it stops working"), "A full answer states its own limits");
    }

    @Test
    void streamingDeliversExactlyWhatCompletingWouldHave() {
        ChatRequest request = requestAt(TutorPolicy.Reveal.GUIDED, "how does slicing copy");

        List<String> chunks = new ArrayList<>();
        ChatResult streamed = client.stream(request, chunks::add);

        assertTrue(chunks.size() > 5, "A stream of one chunk tests nothing about streaming");
        assertEquals(streamed.text(), String.join("", chunks));
        assertEquals(client.complete(request).text(), streamed.text());
    }

    @Test
    void theLearnersOwnWordsComeBackSoTheFixtureIsVisiblyAboutTheirQuestion() {
        String text = client.complete(requestAt(TutorPolicy.Reveal.HINT, "what is a reference")).text();

        assertTrue(text.contains("what is a reference"));
    }

    @Test
    void tokenCountsAreReportedAsEstimates() {
        // A cost table that cannot tell a measurement from a guess will be read as though
        // every row were a measurement.
        ChatResult result = client.complete(requestAt(TutorPolicy.Reveal.HINT, "why"));

        assertTrue(result.estimatedTokens());
        assertTrue(result.tokensOut() > 0);
    }
}
