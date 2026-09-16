# 05 — UX specification

## Brand and feel

Premium **developer workstation × research library × university studio**. Calm density. No LMS card farms, no candy gradients, no mascot gimmicks.

**Visual system (implementation later, rules now):**

- Neutral canvas (light/dark), one accent (ink/amber or similar — not rainbow).
- Type: distinctive but readable. UI sans + serif for long explanations + mono for code.
- Spacing: 8px grid. Information hierarchy via type and position, not boxes inside boxes.
- Motion: 150–200ms opacity/position; honor `prefers-reduced-motion`.
- Elevation: one level of surface; avoid stacked cards.

## Layout

See [04-INFORMATION_ARCHITECTURE.md](04-INFORMATION_ARCHITECTURE.md). Breakpoints:

| Name | Width | Behavior |
|---|---|---|
| Mobile | < 768 | Single pane; context bar; companion sheet |
| Tablet | 768–1199 | Optional companion overlay |
| Desktop | ≥ 1200 | Three-pane default; user can collapse companion |
| Wide | ≥ 1600 | Map + learn side-by-side optional |

## Screen inventory (P0)

Each screen must answer the eight learner questions where relevant.

1. **Auth** — login, register, verify, reset. Quiet, fast.
2. **Onboarding** — 3–5 steps max: goal, hours, experience, diagnostic CTA.
3. **Diagnostic** — one item at a time; progress as “calibrating,” not a fake percent of mastery.
4. **Roadmap** — graph-ish timeline grouped by domain; each node: why, estimate, mastery, blocked-by.
5. **Today** — single primary CTA; secondary: due reviews; tertiary: alerts.
6. **Learn** — title, why, prereq chips, level control (1–6), body, practice dock, tutor.
7. **Exercise** — prompt, workspace, submit, teaching feedback, attempt history.
8. **Quiz** — one or clustered items; rationale after submit (exam mode hides until end).
9. **Lab** — editor, terminal, files, run/stop, AI debug panel.
10. **Map** — force/cluster graph; filters by domain, state, path; search nodes.
11. **Dashboard** — mastery by domain, weak list, velocity, next, career readiness (honest, incomplete is OK).
12. **Search** — results with type chips; retrieved answers with sources.
13. **Project** — brief, milestones checklist, submissions, reviews.
14. **Portfolio** — artifacts list, public/private toggle.
15. **Evolve** — Today / Week / Month; filters; “why I should care.”
16. **Settings** — profile, AI budget, notifications, privacy.
17. **Admin (P1 UI)** — units, graph edges, items, update queue.

## Interaction patterns

### Level and lens

Controls, not hidden menus: `Simpler` `Deeper` `Math` `Visual` `Code` `Analogy` `Challenge`.

### Mastery evidence chips

Four chips: Read, Practice, Assess, Apply. Empty / partial / solid. Tooltip = last evidence date. Completing Read alone never fills Assess.

### Next action

Always a verb + object + time: “Practice gradient descent (12 min)” not “Continue.”

### Tutor

Does not occupy the whole page. Docked. Shows: persona, whether retrieval is on, knowledge timestamp. “Sources” drawer.

### Empty and error states

- Empty map: “Diagnostic unlocks your map.”
- Lab timeout: what happened + retry + reduce dataset hint.
- Provider down: cached explanation still readable; tutor degraded banner.

### Command palette

Fuzzy + intents. Keyboard: `⌘K` / `Ctrl+K`.

## Copy rules

- Adult, precise, non-hype.
- Never “You’re an AI master!” after one quiz.
- Freshness: “Verified against {n} sources as of {datetime}” or “Conflicting updates queued for review.”
- Errors: actionable.

## Accessibility

- All icon buttons have names.
- Focus rings visible.
- Quiz inputs labeled.
- Graph has list fallback.
- Contrast AA.
- Captions for any later video.

## UX review checklist (ruthless)

From master prompt §83. Fail the PR if:

- Next action not obvious in 3 seconds.
- Screen overloaded with equal-weight widgets.
- Location unclear (no breadcrumb / context).
- Mastery not communicated.
- AI not contextual to the unit.
- Extra clicks to run the last exercise.
- Feels like Moodle/Coursera clone.

## Design system delivery (when UI starts)

`packages/ui`: tokens, Button, IconButton, Input, Select, Tabs, SplitPane, MasteryChips, GraphCanvas (wrapper), CommandPalette, Callout, EvidenceBar, Markdown+KaTeX, CodeBlock.

No random one-off colors in apps/web.
