package com.aileapro.api.research;

import com.aileapro.ai.ResearchPolicy;
import com.aileapro.ai.ResearchPolicy.PaperRef;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResearchPolicyTest {

    private static final PaperRef RAG = new PaperRef(
            "rag-2020",
            "Retrieval-Augmented Generation for Knowledge-Intensive NLP Tasks",
            "10.48550/arXiv.2005.11401",
            "https://arxiv.org/abs/2005.11401",
            "AILEAPRO summary: Combines a parametric model with a dense index.");

    private static final PaperRef ATTENTION = new PaperRef(
            "attention-2017",
            "Attention Is All You Need",
            "10.48550/arXiv.1706.03762",
            "https://arxiv.org/abs/1706.03762",
            "AILEAPRO summary: Introduces the Transformer.");

    @Test
    void emptyAttachmentRefuses() {
        var decision = ResearchPolicy.decide(List.of());
        assertThat(decision.refused()).isTrue();
        assertThat(decision.refusal().code()).isEqualTo("no_sources");
    }

    @Test
    void fixtureReplyOnlyNamesAttachedPapers() {
        String reply = ResearchPolicy.fixtureReply("What is RAG?", List.of(RAG));
        assertThat(reply).contains("rag-2020");
        assertThat(reply).contains("10.48550/arXiv.2005.11401");
        assertThat(ResearchPolicy.containsFabrication(reply)).isFalse();
    }

    @Test
    void retrieveBySlugIgnoresUnrelatedQuestion() {
        List<PaperRef> hits = ResearchPolicy.retrieve("zzzz", "attention-2017", List.of(RAG, ATTENTION), 3);
        assertThat(hits).extracting(PaperRef::slug).containsExactly("attention-2017");
    }

    @Test
    void deniedTitleThrows() {
        assertThatThrownBy(() -> ResearchPolicy.enforceCitations(
                        "According to Deep Learning Will Solve Everything, AGI is solved.",
                        List.of(ATTENTION)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void allowedDoisAreKeptAndDeniedDoisAreStripped() {
        String cleaned = ResearchPolicy.enforceCitations(
                "Transformers use attention (DOI 10.48550/arXiv.1706.03762) "
                        + "unlike the fake 10.1234/invented.paper.2099 claim.",
                List.of(ATTENTION));
        assertThat(cleaned).contains("10.48550/arXiv.1706.03762");
        assertThat(cleaned).contains("[unsourced DOI removed]");
        assertThat(cleaned).doesNotContain("10.1234/invented.paper.2099");
    }
}
