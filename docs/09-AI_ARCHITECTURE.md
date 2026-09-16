# 09 — AI architecture

## Principles

1. **Never couple features to one vendor.**
2. **Centralize every call** (cost, cache, policy, logs).
3. **Teach, don’t dump.**
4. **Ground factual and “latest” answers.**
5. **Never trust output blindly** (validate, cite, moderate).

## Logical components

```
Feature (tutor, critic, ingest, coach)
    → AI Orchestrator (use-case graph)
        → Policy (PII, injection, tool allowlist)
        → Router (task → model profile)
        → Cache (semantic + exact)
        → Provider adapters
        → Usage meter + OTel spans
        → Response validator
        → Conversation / artifact store
```

## Provider abstraction

Interface `ChatPort` / `EmbeddingPort` (Java): `complete`, `stream`, `embed`, `moderate` optional.

Adapters implemented with **Spring AI** where it helps, plus thin HTTP clients if a provider is missing. Never call providers from Next.js.

Config: `model_profiles` table + Spring config — not hardcoded in tutors.

### Routing profiles (examples)

| Profile | Use | Example class |
|---|---|---|
| `cheap_chat` | simple Q, flashcards | small/cheap |
| `teach` | Socratic tutoring | mid |
| `reason` | hard misconceptions, design | frontier |
| `code` | debug, review, generate exercises | code-strong |
| `vision` | diagrams, paper figures | multimodal |
| `embed` | retrieval | embedding model |
| `judge` | eval lab / critic | independent from generator |

Fallback chain + retry with jitter. If all fail: serve authored content only.

## Prompt management

`Prompt` + `PromptVersion`: name, hash, template, owner, eval snapshot.

No raw mega-prompts inlined across the repo. Features call `PromptCatalog.get("tutor.socratic.v3")` in Java.

Regression suite: golden conversations (see testing doc).

## Tutor policy

System contract:

- Stay in teaching mode; hints before solutions; solutions only after attempts or explicit request + still explain.
- Use unit context and mastery.
- If learner asks for exam answers verbatim, refuse and offer practice.
- Distinguish **foundational explain** vs **latest state** (retrieval required for the latter).
- Attach citations when retrieval used.
- No fabricated papers.

Memory: short-term session + summarized long-term learner notes (opt-in). No silent cross-user leakage.

Personas: tone/rigor knobs, same tools and safety.

## RAG

Pipeline: query rewrite → hybrid retrieve (keyword OpenSearch + pgvector) → metadata filter → rerank → pack with citations → generate.

Index per corpus: curriculum blocks, verified sources, papers metadata, learner-visible changelog.

Show: knowledge timestamp, source list, dates, versions.

Swap vector DB via `EmbeddingStore` (pgvector first).

## Tools the tutor may use (allowlist)

- Retrieve curriculum
- Retrieve verified news/docs
- Fetch unit graph neighborhood
- Run **sandbox tests** (not arbitrary network)
- Create flashcards / exercises (draft status)

Denied: arbitrary URL fetch from learner-supplied hosts without SSRF controls; secrets; other learners’ data.

## Prompt injection defenses

- Separate system vs retrieved vs user channels (wrappers, delimiters, untrusted-data tags).
- Retrieved text is data, not instructions.
- Tool execution requires structured args validated by schema.
- Strip attempts to override persona/safety.
- Security labs stay in simulated content, not live jailbreak-the-platform.

## Cost architecture

Every call logs: provider, model, tokens in/out, latency, estimated USD, user, feature, request id, prompt version.

Budgets: per user/day, per feature, global. Soft warn, hard stop.

Cache authored explanations aggressively; cache AI completions for identical (unit, level, locale, prompt version) with TTL by volatility.

## Evaluation of our own AI

- Hallucination reports from users
- Citation validity job (do sources support claims — sampled)
- Prompt regression
- LLM-as-judge only as signal, never sole publish gate

## Voice (architecture only until P3)

STT → tutor turn → TTS. Same orchestrator. Store audio in object storage with retention policy.

## Dev without keys

`FakeChatClient` implements the port with **deterministic fixtures** labeled `DEVELOPMENT IMPLEMENTATION` — not random lorem, not silent production fallback. Production refuses to boot if only fake provider configured.
