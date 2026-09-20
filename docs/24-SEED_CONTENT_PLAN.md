# 24 — Seed content plan

**Rule:** no fake inflation. Seed must be *real teaching material* sufficient to prove architecture and the P0 loop.

## Inventory

### A. Taxonomy stubs

Create all domain units A–BE with title, description, volatility, knowledge_class, empty children OK.

### B. Vertical slice (must be complete)

**Path:** AI Engineer (abridged)

1. **Computer Fundamentals** — 1 chapter: “What a program is,” 2 knowledge units.  
2. **Python** — 1 chapter: types, functions, 3 units with code blocks + exercises (P0 lab language). Java as a **sandbox language** is P2; the **platform** is already Java.  
3. **Linear Algebra** — Vectors, Matrices, Matmul (3 units). Math + intuition. LOW volatility.  
4. **Neural Networks** — one unit with why, ELI5→intermediate, misconception (loss vs accuracy), tiny numpy-style example.  
5. **RAG** — one HIGH volatility unit: embeddings, retrieve, generate; version field; official doc resource links.  
6. **Project:** “Build a linear regression model” (beginner) with milestones and tests runnable in lab.  
7. **Project stub:** “Production RAG” outline only (P1 fill).

### C. Graph edges

Connect the example chain in doc 08. Each PREREQUISITE_OF asserted.

### D. Assessments

- Diagnostic bank: 25 items (Python, vectors, ML intuition).  
- Per complete unit: 5 MCQ + 1 short + 1 coding where applicable.  
- Reports consume tagged weaknesses.

### E. Resources (provenance filled)

For RAG unit: official docs URLs (OpenAI/Anthropic/LangChain or vendor-neutral papers + FAISS/pgvector docs) with last_verified dates.  
For linear algebra: a public-domain or openly licensed reference (e.g. 3Blue1Brown as *video resource* with URL, not ripped transcript).

### F. Flashcards

Golden cards for vectors/matmul/backprop/RAG terms (author-locked).

### G. Intelligence

2 manual `AIUpdate` examples (one model release, one deprecation) to drive Evolve UI without live scrapers.

## Wave 2 — curriculum expansion (2026-09-19)

Wave 1 above is the vertical slice: 10 published knowledge units, enough to prove the architecture
and the P0 loop. Wave 2 extends it to a route someone could actually study, still authored one
page at a time and still held to the quality bar below.

**32 published knowledge units, 3 chapters, 2 paths.** `scaled-dot-product-attention` remains a
draft on purpose and is the next thing in the queue.

### New units

| File | Parent domain | Units |
|------|---------------|-------|
| `41-calculus.yml` | `calculus` | chapter `calculus-for-ml`; `derivatives-as-rates`, `chain-rule-for-gradients` |
| `42-probability.yml` | `probability` | chapter `probability-for-ml`; `random-variables-and-expectation`, `conditioning-and-bayes` |
| `45-classical-ml.yml` | `classical-ml` | chapter `supervised-learning-core`; `supervised-learning-framing`, `linear-regression-intuition`, `train-validation-test`, `overfitting-and-regularization` |
| `46-evaluation.yml` | `model-evaluation` | `precision-recall-and-f1`, `holdout-vs-cross-validation` |
| `52-deep-learning.yml` | `neural-networks`, `deep-learning` | `multilayer-perceptron`, `activations-and-nonlinearity`, `vanishing-gradients-intuition` |
| `71-transformers.yml` | `transformers` | `transformer-block-overview`, `positional-encoding-intuition` |
| `72-llms.yml` | `large-language-models`, `prompt-engineering` | `next-token-prediction`, `context-window-limits`, `prompting-as-specification` |
| `73-embeddings.yml` | `embeddings` | `what-embeddings-capture` |
| `74-agents.yml` | `ai-agents` | `tool-calling-loop`, `agent-failure-modes` |
| `75-responsible-ai.yml` | `responsible-ai` | `training-data-and-harm` |

### Everything else Wave 2 touched

- **Sources.** 22 added to `00-sources.yml` (40 total), arXiv and open documentation only, each
  HEAD-checked on 2026-09-19 with the status recorded. Papers are cited in the version that
  carries a DOI, which is what `ResearchApiTest` requires of anything the reading list surfaces;
  a web-published article with no DOI is filed as what it is rather than as a paper.
- **Edges.** 48 added to `80-edges.yml` (65 total), extending the chain through
  maths → classical ML → evaluation → neural networks → transformers → LLMs → agents, plus
  vectors → embeddings → RAG. Gating edges stay acyclic; advisory claims are `DEPENDS_ON` with a
  weight; pairs with no ordering are `RELATED_TO`.
- **Paths.** New `ai-engineer-core`: every published knowledge unit in dependency order, 32
  waypoints. `ai-engineer-abridged` keeps its ten waypoints — it is pinned by `CatalogApiTest` —
  and its description now says it is a sampler and points at the core path.
- **Assessments.** 94 items added (156 total) across ten new files, with a practice set and a
  quiz per unit. Distractors are built from the 22 new misconception codes, so a wrong answer
  maps back to the block that reteaches it. 22 review cards.

### What Wave 2 still does not cover

Said plainly, because the map shows these domains and the UI has to be honest about them: the
attention mechanism itself (draft), CNNs, RNNs, fine-tuning and PEFT, MLOps and LLMOps, vector
database internals, AI security beyond the injection material in the agent units, multimodal, and
everything on the business side. Those domains exist in the taxonomy with a description and no
published units, which is what the catalog reports.

## Wave 3 — breadth expansion (2026-09-19)

Wave 2 left half the map empty. Wave 3 fills the engineering route a working AI engineer actually
needs: more Python/NumPy, statistics and optimisation, SQL, classical ML toolbox, CNNs and RNNs,
**published** scaled-dot-product attention, tokenization, fine-tuning and LoRA, ANN indexes and
RAG chunking quality, CLIP multimodal embeddings, agent memory, LLM eval harnesses, prompt
injection and tool exfiltration, training-serving skew, cost/latency/quality, observability, and
LLM app architecture patterns.

**~61 published knowledge units**, chapters under the newly filled domains, assessments per unit,
edges through the new chain. `mixture-of-experts-draft` is the remaining intentional draft (tests
assert draft invisibility). Business/entrepreneurship/governance domains stay honest empties.

### New / rewritten files (curriculum)

| File | Coverage |
|------|----------|
| `31-python-more.yml` | control flow, NumPy essentials |
| `35-sql.yml` | select / join / aggregate |
| `43-statistics.yml` | sampling bias, confidence intervals |
| `44-optimization.yml` | gradient descent, learning rates |
| `47-classical-ml-more.yml` | logistic regression, trees/ensembles, leakage |
| `53-cnn-rnn.yml` | convolution, pooling, RNN, LSTM |
| `70-attention-draft.yml` | **published** scaled-dot-product attention |
| `76`–`79` | tokenization, fine-tune/PEFT, ANN+chunking, CLIP+agent memory |
| `81`–`83` | MLOps/LLMOps, eval/security, system design |
| `84-moe-draft.yml` | MoE draft for invisibility tests |

Matching assessment YAMLs sit alongside under `seed/assessment/`.

## Quality bar for a “complete” unit

All of: why, 2+ explanation levels, one example, one misconception, one exercise, one quiz item, mastery criteria, at least one cited resource, difficulty, time, volatility, last_reviewed.

Wave 2 units meet this and add an `exercise_set` of five and a self-check `quiz_set` on every
page. `.run/check_seed.py` mirrors the loaders' validation — block types, display-maths balance,
parent and source resolution, prerequisite acyclicity, misconception codes, item keys — so an
authoring mistake fails in a second rather than in a Gradle run.

## What we will not do in seed

- 50k generated micro-concepts.  
- Wikipedia dumps.  
- Copyrighted book chapters.  
- Random quiz without misconception tags.
