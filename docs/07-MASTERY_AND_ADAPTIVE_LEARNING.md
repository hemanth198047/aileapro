# 07 — Mastery and adaptive learning

## Goal

Replace “progress %” with an **evidence-based competence model** that drives “learn next,” reviews, roadmap, career claims, and certification.

## Mastery state machine (per learner × knowledge unit)

```
UNKNOWN → INTRODUCED → FAMILIAR → UNDERSTOOD → PRACTICED → PROFICIENT → MASTERED
                              ↘ (decay) ← ← ← ← ← ← ← ← ←
```

Decay is **not** applied to LOW-volatility foundations on the same schedule as HIGH-volatility APIs.

| State | Typical evidence |
|---|---|
| UNKNOWN | No successful evidence |
| INTRODUCED | Opened why + at least one explanation level, dwell time above floor |
| FAMILIAR | Can answer easy recall (flash or MCQ) above threshold |
| UNDERSTOOD | Explanations + medium items; misconception checks passed |
| PRACTICED | Guided + independent exercises; lab run if code unit |
| PROFICIENT | Assessment set + transfer item; consistency across days |
| MASTERED | Application (project/interview/teach-back) + retention check after interval |

**Never** jump to MASTERED from a single quiz.

## Composite mastery score (internal 0–1)

Used for ranking and diagnostics, **not** as the primary UI number.

```
S = w1*accuracy + w2*consistency + w3*retention + w4*application
  + w5*prerequisite_health + w6*recency_factor − w7*misconception_penalty
```

Initial weights (tunable, versioned in config):

| Weight | Value | Signal |
|---|---|---|
| w1 | 0.22 | IRT-ish accuracy on tagged items |
| w2 | 0.15 | Variance across attempts / days |
| w3 | 0.18 | Spaced recall success |
| w4 | 0.20 | Project/lab/application rubrics |
| w5 | 0.15 | Min or product of prereq S |
| w6 | 0.10 | Recency appropriate to volatility |
| w7 | 0.25 | Open misconception flags (subtract) |

Store raw events; recompute S in worker. Do not overwrite history.

## Item response (P0 practical)

P0: exponential moving accuracy + difficulty weights.  
P1: Rasch/2PL on sufficient data.  
Never show IRT guts to beginners.

## Misconceptions

`MisconceptionCode` catalog (e.g. `BACKPROP_CONFUSES_LOSS_WITH_ACCURACY`). Items and tutor turns can raise flags. Reteach block required before clearing.

## Learn next (policy)

Priority queue:

1. Overdue spaced reviews (if forgetting risk high).
2. Unblock failed prerequisites for current waypoint.
3. Migration / security alerts affecting mastered units.
4. Current waypoint unit with highest instructional value (not mastered, prereqs OK).
5. Stretch challenge if velocity high and frustration low.

Explain the choice in one sentence on Today.

## Diagnostic

Bayesian skill groups (domains), not one global IQ. Item bank tagged. Stop when standard error < τ or max items. Output: priors on units, **not** a job title.

## Roadmap

`StudyPlan` versions: generated artifact with frozen snapshot + live adjustments.

Horizons: daily block, weekly, monthly, long-term waypoints.

Inputs: hours, goal path, diagnostic, weak units, project status, reviews.

Recalc triggers: weekly cron, major CI curriculum change, learner goal change, repeated failure.

Always store `reason` for mutations.

## Spaced repetition

SM-2 variant with concept cards **and** code/math cards. Intervals capped; leeches flagged for reteach not infinite cram.

Auto-generate cards from unit objectives + missed items; author can lock golden cards.

## Frustration and flow

Track: rapid re-submits, tutor “just give the answer,” time-on-item spikes. Policy: simplify explanation, reduce difficulty one step, offer prereq — **not** a cartoon.

## Certification

Certificates reference **demonstrated** unit set + exam/project IDs + dates + curriculum version. No certificate for “finished watching.”

## What we refuse

- Completing a unit because the page loaded.
- A single percentage as the product’s intelligence.
- Identical next lesson for all learners on a path.
