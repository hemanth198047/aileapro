# 20 — Roadmap (phases, stages, gates)

Work **in phases**. Do not generate the entire system in one burst. Each phase: analyze → inspect → design (if new) → implement → test → build → UX pass → summarize remaining.

Staffing assumption: **1–2 engineers + this agent** unless noted. Calendar is **elapsed weeks**, not person-weeks. Adjust 1.5–2× if part-time.

---

## Phase 0 — Inspect (complete at first code repo)

- Confirm empty/new repo `aileapro`
- Record stack freeze (this pack)
- **Deliverable:** this documentation pack + `docs/` copied into repo  
- **Gate:** product owner agrees stack and P0 scope

*No existing application to preserve.*

---

## Phase 1 — Foundation (2–3 weeks)

**Stage 1.1** Gradle multi-project (`backend/`), pnpm web, Compose (Postgres, Redis, MinIO)  
**Stage 1.2** Spring Security: register/login/verify/session/RBAC  
**Stage 1.3** Spring API skeleton, springdoc OpenAPI, health, request IDs, Resilience4j rate limit stub  
**Stage 1.4** Web shell: design tokens, layout, command palette stub, dark mode, a11y baseline  
**Gate:** user can register and see authenticated shell; `./gradlew check` + web CI green

---

## Phase 2 — Design system + IA shell (1–2 weeks)

Spaces: Today, Learn, Map, Lab, Studio, Assess, Search, Settings  
Empty states real. Routing. No fake stats.  
**Gate:** ruthless UX review of shell

---

## Phase 3 — Curriculum engine (2–3 weeks)

Units, blocks, paths, versions, admin seed loader (CLI first)  
Published read APIs  
Seed domains A–BE as nodes + real child content per seed plan  
**Gate:** browse path → open unit with authored explanations (no AI required)

---

## Phase 4 — Knowledge graph (1–2 weeks)

Edges, closure, neighborhood API, Map UI v1  
Prereq checks  
**Gate:** click node opens unit; gap list for a target

---

## Phase 5 — Learning experience (2–3 weeks)

Learn page: why, levels, evidence chips, events  
Today + next action policy v1  
Study plan generation (rules first, LLM polish later)  
Notes v0  
**Gate:** journey Learn without tutor still valuable

---

## Phase 6 — Assessment + SRS (2 weeks)

Question bank, attempts, reports (full template)  
Diagnostic onboarding  
Flashcards + due reviews  
**Gate:** diagnostic → roadmap; quiz report ≠ percent-only

---

## Phase 7 — AI tutor (2–3 weeks)

Orchestrator, router, usage logs, prompt versions (Java + Spring AI ports)  
Socratic tutor + levels + citations drawer  
`FakeChatClient` for CI; live provider behind env  
**Gate:** ask/simplify/deeper/quiz-me on a unit; cost line items exist

---

## Phase 8 — RAG + search (2 weeks)

pgvector, chunk published blocks, hybrid when OpenSearch added  
Search UI  
Latest vs foundational flag  
**Gate:** search finds seed units; tutor cites block ids

---

## Phase 9 — Coding playground (2–3 weeks)

Executor MVP (container or E2B)  
Python run/stop/output/history  
AI debug using real stdout  
**Gate:** learner runs seed notebook-equivalent script; API process never execs user code

---

## Phase 10 — Project Studio (2 weeks)

One beginner + one RAG-oriented project spec  
Milestones, submission, portfolio page  
**Gate:** complete project updates Apply chip + portfolio artifact

---

## Phase 11 — Research environment (2 weeks)

Paper metadata seed, explainer with **forced** citations  
Upload PDF (permitted) explainer P1  
Literature map v0  
**Gate:** no fabricated refs in fixtures tests

---

## Phase 12 — Career engine (1–2 weeks)

Skill gap from mastery vs path  
Interview mode reuses tutor persona  
**Gate:** gaps only from evidence

---

## Phase 13 — Admin / content studio (2–3 weeks)

Editors, version workflow, critic, generation drafts  
**Gate:** draft cannot appear in Learn until published

---

## Phase 14 — Analytics (1–2 weeks)

Dashboards learner + admin; AI cost  
**Gate:** funnel + burn visible

---

## Phase 15 — Intelligence + security + observability hardening (3 weeks)

CI schema already in 3–4; now jobs: link check, one docs watcher, proposals, health report, Evolve feed, rollback  
OTel, Sentry, budgets, injection tests, lab network deny  
**Gate:** Evolve shows timestamped items; admin can reject a proposal; currentness copy honest

---

## Phase 16 — Production hardening (2 weeks)

Load, backups, flags, privacy export/delete, ToS, threat review, Playwright full P0 list  
**Gate:** launch checklist signed

---

## Parallel tracks (do not block P0)

- Visual interactive labs (attention, gradient descent) — start after Phase 5  
- Voice — after tutor quality  
- Community — after moderation plan  
- Extra languages in executor  
- i18n strings extraction ongoing  

---

## Implementation priority (master prompt §82)

Architecture → Auth → Curriculum → Learn → Tutor → Assess → Mastery → Graph → Code → Projects → RAG → Research → Career → Admin → Analytics → Community → Advanced.

Roadmap above **interleaves graph before tutor** because next-action quality depends on it; tutor still before RAG completeness (authored content first).

---

## Milestones (product)

| Milestone | User-visible |
|---|---|
| M0 Docs | this pack |
| M1 Shell | accounts |
| M2 Curriculum | read a real unit |
| M3 Loop | diagnostic, practice, mastery, next |
| M4 Tutor | teaching companion |
| M5 Lab+Project | run code, portfolio |
| M6 Current | Evolve + governance |
| M7 Harden | production |

---

## After launch (rolling)

High-volatility weekly review SLA. New domains as graph additions, not rewrites.
