# 01 — Vision

## Product

**AILEPRO** is the product name. `ailepro` (lowercase) is the repository and package name.

AILEPRO is a production-grade learning environment that takes a person from zero knowledge to world-class AI practitioner, engineer, researcher, or builder. Books, videos, courses, and docs are **inputs and supplements**. The product itself is the primary classroom, lab, tutor, research desk, and career coach.

## Thesis

Static courses rot at the speed of the AI field. The differentiator is not “more modules.” It is:

1. A **living curriculum** grounded in a knowledge graph.
2. A **mastery model** based on evidence, not page views.
3. A **personal tutor** that teaches instead of dumping answers.
4. A **lab** that runs real code in isolation.
5. A **continuous intelligence engine** that discovers, verifies, and converts field change into learning — with provenance, governance, and rollback.

Trust beats pretending to be omniscient. The product never claims “everything is always up to date.” It shows **what was verified, against which sources, as of when**.

## North-star learner questions (always visible in UX)

1. Where am I?
2. What am I learning?
3. Why am I learning it?
4. What do I already know?
5. What am I missing?
6. What should I do next?
7. How deeply do I understand this?
8. How can I practice it?

## Core loop (non-negotiable)

```
LEARN → PRACTICE → ASSESS → ANALYZE → IDENTIFY GAPS
→ RETEACH → PRACTICE AGAIN → MASTER → APPLY → REVIEW → ADVANCE
```

Mastery gates require evidence from multiple modes (read + practice + assessment + application), not a single click.

## What this is not

- A conventional LMS (sidebar + cards + progress bar + video).
- A news aggregator.
- A YouTube wrapper.
- A chatbot with a curriculum skin.
- A MOOC certificate mill (completion ≠ competence).
- A childish game with XP for opening pages.

## What this is

**AI University + AI Tutor + AI Lab + Research Assistant + Career Coach + Project Studio + Knowledge Graph + Always-Current Engine.**

## Success definition (product)

A learner can, without leaving the product:

- Diagnose skill, get a personalized roadmap, learn a concept at multiple depths, practice, be assessed honestly, run code, ship a project into a portfolio, search with sources, and receive a next action that is *specific to them*.

A curriculum author / admin can:

- Author units of arbitrary depth, attach provenance, generate drafts, run critic checks, simulate learner impact, approve, version, rollback.

The platform can:

- Discover field change, score curriculum impact, propose updates, keep fundamentals stable, migrate learners with “what changed for you,” and audit every automatic change.

## Quality bars

**UX:** Distinctive information architecture. Premium, dense, calm. Command palette. Split-pane learning. Mastery visible. Contextual AI. Dramatically better than an LMS.

**Engineering:** Modular domains, strong typing, no giant monoliths, no fake APIs, no hardcoded quiz scores, tests as part of “done,” secrets never on the client.

**Education:** Socratic teaching, misconception detection, prerequisite graphs, spaced repetition, version-aware knowledge (foundational vs current vs deprecated).

## Constraints we accept

- We will not generate 50,000 fake micro-concepts. We will **architect for that scale** and **seed enough real content** to prove the loop.
- We will not couple to one model vendor.
- We will not execute untrusted learner code on the API server.
- We will not silently rewrite published educational content from unverified summaries.
- We will not over-engineer Kubernetes on day one, but domain boundaries must survive that later move.
