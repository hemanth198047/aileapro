# 04 — Information architecture

## Design principle

Not: left nav of courses + card grid.

Yes: a **studio shell** with a persistent **orientation column**, a **work surface**, and a **contextual companion**.

```
┌──────────────┬─────────────────────────────┬──────────────────┐
│ Orientation  │ Work surface                │ Companion        │
│ (where/why)  │ (learn / lab / map / exam)  │ (tutor / notes)  │
└──────────────┴─────────────────────────────┴──────────────────┘
 Command palette (⌘K): jump, search, ask, switch mode
```

On mobile: work surface full-width; orientation becomes top context bar; companion is a sheet; command palette remains.

## Primary spaces (top-level)

| Space | Purpose | Default next action |
|---|---|---|
| **Today** | Daily coach: one recommended action, reviews due, streak (meaningful), alerts | Start recommended block |
| **Learn** | Current unit: split explanation + practice | Continue unit |
| **Map** | Knowledge map / graph explorer | Open weak or next node |
| **Lab** | Code, datasets, experiments, RAG, agents | Resume last session |
| **Studio** | Projects, portfolio, certifications | Next milestone |
| **Assess** | Quizzes, exams, interviews, oral | Due / recommended |
| **Research** | Papers, explainer, literature map | Continue reading list |
| **Evolve** | What’s new in AI, personalized, diffs | Relevant update |
| **Career** | Gaps, interviews, resume, role readiness | Next career action |
| **Notes** | Linked notes, backlinks | Capture |
| **Search** | Universal + semantic | Query |
| **Admin** | Content + update center (role-gated) | Queue |

## Orientation column always shows

- Path + current unit breadcrumb (flexible depth).
- Why this (1–2 sentences from unit + path).
- Mastery state + evidence chips (read / practice / assess / apply).
- Prerequisites health (green/amber/red, clickable).
- Next 3 actions, ranked.
- Freshness / knowledge timestamp for current unit.

## Learning modes (switcher, not separate products)

Read, Watch, Listen, Interactive, Code, Practice, Quiz, Flashcards, Socratic, Tutor, Simulation, Project, Research, Interview, Challenge, Revision, Exam, Teaching mode, Evolution mode.

Mode is a **view over the same knowledge unit**, not a different URL silo when possible.

Deep links: `/learn/{unitId}?mode=socratic&level=2`

## Command palette intents

- Go to unit / path / project / paper
- Explain simpler / mathematically / visually / code
- Quiz me / challenge me
- Run last lab
- What should I learn today?
- What changed since I learned X?
- Search with sources

## Navigation anti-patterns to reject in review

- Nested hamburger of 40 LMS items.
- Duplicate “Courses” and “My learning” that show the same list.
- Progress bars as the only mastery cue.
- Tutor hidden on a separate page with no unit context.
