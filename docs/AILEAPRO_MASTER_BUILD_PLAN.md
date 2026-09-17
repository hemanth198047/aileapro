# AILEAPRO — Master implementation plan

**Document type:** Master copy for application development  
**Product:** AILEAPRO  
**Repo name:** aileapro  
**Version:** 1.0 (planning freeze + Java backend)  
**How to use:** Complete **one phase at a time**. Do not start Phase N+1 until that phase’s **GATE** is checked. Tick the checkboxes as you go. This file is the operational script; the rest of `aileapro-docs` is the law.

**Location of this file:** `C:\Users\Admin\Downloads\aileapro-docs\AILEAPRO_MASTER_BUILD_PLAN.md`  
**Companion specs:** same folder (`00-INDEX.md` is the map).

---

## How to work this document

1. Read **Golden rules** once. Re-read them when tempted to skip a gate.
2. For the current phase, open the **Spec pointers** and skim those files (10 minutes).
3. Execute **Steps** in order. Do not skip tests.
4. Fill **Phase log** (date, commit, notes).
5. Only when **GATE** is true, write `PHASE_N_COMPLETE` in the log and start the next phase.

### Ritual at the start of every phase

1. `git status` clean (or a dedicated branch `phase-N`).
2. Re-read this phase + the named spec files.
3. Inspect existing code; do not invent a parallel stack.
4. List schema / API / UI dependencies on paper or in the PR description.

### Ritual at the end of every phase

1. `./gradlew check`
2. Web: lint + typecheck + build
3. Manual click-through of that phase’s GATE
4. UX 8-question check (Where am I? What next? …) on changed screens
5. Short written summary: what shipped, what is explicitly **not** done
6. Tag git: `phase-N-gate`

### Golden rules (never waive without a written exception)

- Java 21 + Spring Boot for **all** servers. Next.js is **UI only**.
- Never run learner code on the API or worker JVM.
- Never mark a concept complete because a page opened.
- Never fake mastery, quiz scores, or AI replies in production.
- `FakeChatClient` / fake executor must be **labeled** in the UI when used.
- Do not generate 50,000 dummy concepts. Seed per `24-SEED_CONTENT_PLAN.md`.
- Do not claim “always up to date.” Show verification timestamps.
- Secrets never in `NEXT_PUBLIC_*` or frontend bundles.
- One phase at a time.

### Progress tracker

| Phase | Name | Gate date | Commit / tag |
|------:|------|-----------|--------------|
| 0 | Inspect / docs in repo | | |
| 1 | Foundation | | |
| 2 | Shell + IA | | |
| 3 | Curriculum | | |
| 4 | Knowledge graph | | |
| 5 | Learning experience | | |
| 6 | Assessment + SRS | | |
| 7 | AI tutor | | |
| 8 | RAG + search | | |
| 9 | Lab / executor | | |
| 10 | Project Studio | | |
| 11 | Research | | |
| 12 | Career | | |
| 13 | Admin studio | | |
| 14 | Analytics | | |
| 15 | Intelligence + harden | | |
| 16 | Production launch | | |

**Milestone map:** M0 docs → M1 accounts (P1) → M2 read unit (P3) → M3 learning loop (P6) → M4 tutor (P7) → M5 lab+portfolio (P10) → M6 currentness (P15) → M7 production (P16).

---

## Prerequisites (before Phase 1 code)

### Machine

- [ ] Git for Windows **or** WSL2 (required; this PC failed `/bin/sh` git init)
- [ ] JDK 21 Temurin
- [ ] Node.js 22 + pnpm
- [ ] Docker Desktop (WSL2 backend recommended)
- [ ] Optional: IntelliJ IDEA for Java, Cursor for this repo

### Accounts (can wait until the phase that needs them)

- [ ] GitHub repo `aileapro`
- [ ] At least one model API key (Phase 7 live tutor)
- [ ] Cloud later (Phase 16)

### Specs always in force

`01-VISION`, `02-PRODUCT_SPEC`, `05-UX_SPEC`, `14-TECH_STACK`, `15-SECURITY`, ADR **0008** (Java backend).

---

# PHASE 0 — Put the law in the repo

**Goal:** Product exists as a git project with this documentation pack inside it. No feature code required.

**Spec:** `28-PHASE0_ASSESSMENT.md`

### Steps

1. Create folder (recommended): `C:\Users\Admin\Projects\aileapro` using Git Bash or WSL: `git init`.
2. Copy **all** of `C:\Users\Admin\Downloads\aileapro-docs\` into `aileapro/docs/`.
3. Add root `README.md` pointing at `docs/AILEAPRO_MASTER_BUILD_PLAN.md` and `docs/00-INDEX.md`.
4. Add `.gitignore` for Java (`build/`, `.gradle/`, `.idea/`), Node (`node_modules/`, `.next/`), `.env`, OS junk.
5. Commit: `docs: add planning pack and master build plan`.
6. Confirm no application source yet except README/gitignore — that is OK.

### GATE

- [ ] Git history exists
- [ ] Docs are in `docs/`
- [ ] You agree Java backend + Next.js UI (ADR 0008)

**Do not do in Phase 0:** feature implementation, fake screenshots as “the app.”

---

# PHASE 1 — Foundation (start here for the application)

**Goal:** A learner can register, log in, and see an authenticated app chrome. CI can build Java and web.

**Duration guide:** 2–3 weeks  
**Specs:** `11-ARCHITECTURE`, `12-DATA_MODEL` (users/roles/sessions only), `13-API_DESIGN` (auth + `/me`), `14-TECH_STACK`, `15-SECURITY`, `19-DEVOPS`, ADR 0001, 0005, 0008

## Stage 1.1 — Monorepo and Compose

1. Create Gradle wrapper and `backend/settings.gradle.kts` including projects: `api`, `worker` (empty boot app OK), `executor` (empty health-only OK), `libs:domain`, `libs:security`, `libs:persistence`.
2. Spring Boot 3.4+ , Java 21, Flyway on `api`.
3. `infra/compose/docker-compose.yml`: PostgreSQL 16, Redis 7, MinIO. Named volumes.
4. `apps/web`: Next.js App Router, TypeScript strict, Tailwind, pnpm workspace.
5. Root `Makefile` or `justfile`: `make up`, `make api`, `make web`.
6. `.env.example` listing `DATABASE_URL` / Spring datasource, Redis, `SERVER_PORT`. **No real secrets committed.**
7. GitHub Actions: JDK 21 `./gradlew check` (even if few tests) + pnpm web typecheck.

## Stage 1.2 — Identity

8. Flyway `V1__identity.sql`: `users`, `auth_identities`, `sessions` or Spring Session tables, `roles`, `user_roles`, `permissions`, `role_permissions`, `audit_logs`.
9. Seed roles: `learner`, `admin`.
10. Spring Security: register, login, logout, session cookie (`httpOnly`, `Secure` in prod, `SameSite`), Argon2id passwords.
11. Email verification **token table** even if outbound email is console/log in dev.
12. Password reset tokens.
13. `GET /v1/me` returns id, email, roles.
14. Tests: register duplicate email 409; login wrong password 401; `/v1/me` without cookie 401; learner cannot hit a stub `/v1/admin/ping`.

## Stage 1.3 — API platform

15. `/v1` prefix, JSON errors `{ error: { code, message, request_id } }`.
16. Filter: generate/echo `X-Request-Id`.
17. springdoc OpenAPI at `/v3/api-docs`.
18. CORS locked to web origin.
19. Actuator health (no secrets).
20. Rate-limit stub (Resilience4j or bucket4j) on `/v1/auth/login`.
21. Validation: Bean Validation on DTOs.

## Stage 1.4 — Web shell

22. Design tokens: canvas, text, accent, mono, radii — **no LMS rainbow**.
23. Light/dark.
24. Routes: `/login`, `/register`, `/app` (protected).
25. Authenticated layout: empty **orientation | work | companion** columns (companion collapsed OK).
26. Command palette stub (`Ctrl+K`) with “Go to Today” placeholder.
27. Copy: product name **AILEAPRO**.

### GATE (Phase 1)

- [ ] `docker compose up` postgres+redis
- [ ] `./gradlew :api:bootRun` + register/login via web
- [ ] Session survives refresh
- [ ] `/v1/me` works
- [ ] CI green
- [ ] No provider keys in the client

**Explicitly out:** curriculum, tutor, lab, fake dashboard numbers.

**Phase log:** date / tag `phase-1-gate` / notes:

---

# PHASE 2 — Design system and information architecture shell

**Goal:** All primary **spaces** exist as real routes with honest empty states. No fake stats.

**Specs:** `04-INFORMATION_ARCHITECTURE`, `05-UX_SPEC`

### Steps

1. `packages/ui`: Button, IconButton, Input, Tabs, SplitPane, Callout, CommandPalette, MasteryChips (empty/partial/solid, unused data OK).
2. App routes (auth required):

   | Path | Space |
   |------|--------|
   | `/app/today` | Today |
   | `/app/learn` | Learn (placeholder) |
   | `/app/map` | Map |
   | `/app/lab` | Lab |
   | `/app/studio` | Studio |
   | `/app/assess` | Assess |
   | `/app/research` | Research |
   | `/app/evolve` | Evolve |
   | `/app/career` | Career |
   | `/app/notes` | Notes |
   | `/app/search` | Search |
   | `/app/settings` | Settings |
   | `/app/admin` | Admin (403 unless admin) |

3. Each space: title, one-sentence purpose, empty state that tells the **next real action** (“Complete diagnostic” only after Phase 6 exists — until then “Curriculum arrives in Phase 3”).
4. Breadcrumb slot in orientation column.
5. Keyboard: skip link, focus rings, palette.
6. Responsive: mobile single pane + companion sheet.
7. **Ruthless UX review:** next action obvious; not a card farm; not Moodle clone.

### GATE (Phase 2)

- [x] All spaces reachable
- [x] Admin 403 for learner
- [x] Empty states do not lie about mastery or streaks
- [x] Dark mode + a11y baseline on shell

**Phase log:**

- `packages/ui` (`@aileapro/ui`) now owns the tokens and the primitives: Button, IconButton,
  Field, PasswordField, Callout, Tabs, SplitPane, MasteryChips, CommandPalette, Brand, icons,
  and the theme hook. `apps/web` holds layout only and defines no colours of its own.
- All 13 spaces are routed under `/app/*`. `/app` redirects to `/app/today`.
  `apps/web/lib/spaces.tsx` is the single registry behind the navigation, the command palette,
  the breadcrumb, and every empty state.
- The shell is three panes: orientation (breadcrumb, spaces, evidence chips, prerequisite
  health), work surface, and a docked companion with Tutor and Notes tabs.
- `/app/admin` asks `GET /v1/admin/ping` and renders the API's own 403 rather than trusting a
  client-side role check.
- Today's single action is real: unverified accounts get the working `POST /v1/auth/verify-email`
  form. Verified accounts are told plainly that nothing is due, because nothing is.
- No streaks, mastery percentages, or readiness scores appear anywhere. Evidence chips render
  empty with the reason, and Career states that readiness is not calculable yet.
- a11y: skip link, breadcrumb landmark, `aria-current` on the active space, named icon buttons,
  visible focus rings, `prefers-reduced-motion`, and a list fallback promised beside the graph.
- Responsive: three panes at ≥1200px, companion becomes a sheet below that, orientation becomes
  a drawer below 900px, both dismissable by scrim.
- Theme toggles share state through the `data-theme` attribute, so the topbar and Settings
  controls can no longer disagree.
- Verified: `pnpm --filter @aileapro/ui typecheck`, `pnpm --filter web typecheck`,
  `pnpm --filter web build` (21 routes prerendered), and a browser walk of every space in both
  themes at desktop, tablet, and mobile widths. CI now runs the package typecheck and the web
  build.

**Known gaps carried forward:** the session is fetched client-side, so a hard page load shows a
brief splash before the shell paints; email delivery is still unconfigured, so verification tokens
come from the API log.

---

# PHASE 3 — Curriculum engine

**Goal:** Browse a path and open a **real authored unit** (no AI required).

**Specs:** `06-CURRICULUM_MODEL`, `12-DATA_MODEL` (curriculum tables), `24-SEED_CONTENT_PLAN`

### Steps

1. Flyway: `learning_units`, `unit_blocks`, `unit_closure` (containment only for now), `learning_paths`, `path_waypoints`, `sources`, `learning_resources`, `unit_resources`, version/status columns.
2. Enums: `unit_kind`, `status` (draft/review/validate/published/deprecated), `volatility`, `knowledge_class`.
3. Java module `curriculum`: entities, repositories, `CurriculumQueryService` (published only for learners).
4. APIs: `GET /v1/paths`, `GET /v1/paths/:id`, `GET /v1/units/:id` (404 if not published).
5. Admin **CLI** (not UI): `./gradlew :api:seed` loads:
   - Domain stubs A–BE
   - Vertical slice: fundamentals, Python, vectors/matrices/matmul, one NN unit, one RAG unit
   - Provenance on every external URL
6. At least **one complete unit** with: why, two explanation levels, example, misconception, exercise placeholder, mastery criteria, difficulty, time, last_reviewed.
7. Web: path list → unit Learn view **read mode** (Markdown + KaTeX).
8. Tests: unpublished unit hidden from learner; seed is idempotent.

### GATE (Phase 3)

- [x] Logged-in user opens a real explanation without AI
- [x] Draft units invisible
- [x] Seed does not invent 10k empty topics

**Phase log:**

- Flyway `V2__curriculum.sql` adds `learning_units`, `unit_blocks`, `unit_closure`,
  `learning_paths`, `path_waypoints`, `sources`, `learning_resources`, `unit_resources`.
  Enumerated columns are `VARCHAR` + `CHECK` rather than Postgres enums, so adding a block type
  is a migration and not a type rewrite.
- `com.aileapro.api.curriculum` holds the entities, repositories, `CurriculumQueryService`, and
  `CatalogController` behind `GET /v1/paths`, `/v1/paths/{idOrSlug}`, `/v1/units/{idOrSlug}`, and
  `/v1/units/{idOrSlug}/resources`. Every learner query filters on `status = published`, so a
  draft unit 404s even when its slug is guessed, and draft waypoints drop out of a path.
- `gradle :api:seed` runs the app with the web server off and loads the YAML under
  `api/src/main/resources/seed/curriculum`: 16 sources, 71 units, 101 blocks, 26 resource links,
  1 path, 11 waypoints. Re-running it changes nothing, because units upsert by slug + locale and
  blocks, resource links, waypoints, and closure rows are replaced wholesale per owner.
- The 71 units are 57 domain containers plus 14 authored units — the domains are described and
  empty on purpose, and an empty domain says so on its own page instead of showing an outline
  nobody wrote. The authored slice is what a program is (2 units), Python essentials (3),
  vectors/matrices/matmul (3), one neuron unit, one RAG unit, and one deliberately unfinished
  attention unit that exists only to prove drafts stay invisible.
- Every unit carries why, the explanation levels that were actually written, example,
  misconception, exercises, a self-check, mastery criteria, difficulty, minutes, and
  `last_reviewed`. Every external URL has a `sources` row with type, licence, and a verified
  timestamp.
- Web: `/app/learn` lists the path from the API and `/app/learn/[slug]` reads a unit with
  Markdown and KaTeX. Explanation levels become a tab switcher; self-check answers are structured
  data rendered into `<details>` rather than HTML in Markdown, so raw HTML stays disabled.
- Display maths must be fenced with `$$` on its own line: the Markdown maths extension treats
  `$$x$$` as *inline* maths and text after an opening `$$` as an info string, which silently
  rendered raw LaTeX to the learner. The seeder now rejects an unbalanced `$$` line, so this
  fails the seed instead of the page.
- Markdown is imported as `@aileapro/ui/markdown`, not from the package barrel. Re-exporting it
  put KaTeX in the layout's chunk and cost every space 235 kB of first-load JS; the subpath
  leaves the shell at 110 kB and charges the 130 kB to the unit reader that renders maths.
- Verified: `gradle check`, `pnpm -r typecheck`, `pnpm --filter web build` (21 routes), a re-run
  of `:api:seed`, and a browser walk — catalog, a unit with 0 KaTeX errors and display maths
  centred, self-check reveal, an empty domain, and the draft unit's "Not published" page.

**Known gaps carried forward:** reading records nothing (evidence starts in Phase 5), unit order
is the author's rather than prerequisite-derived (Phase 4), exercises are not graded (Phase 6),
and `next lint` in `apps/web` is still unconfigured so CI relies on typecheck and build.

---

# PHASE 4 — Knowledge graph

**Goal:** Typed graph + map; prerequisites drive warnings.

**Specs:** `08-KNOWLEDGE_GRAPH`

### Steps

1. `concept_edges` (`from_id`, `to_id`, `type`, `weight`, `status`).
2. Closure table maintenance on containment + optional prereq DAG check (reject cycles on `PREREQUISITE_OF`).
3. Seed the chain: Linear Algebra → … → RAG → Agents (as far as seed units exist).
4. `GET /v1/units/:id/graph?hops=2`
5. `GET /v1/units/:id/prerequisites` + missing list (mastery still “unknown” until Phase 5/6).
6. Map UI: neighborhood graph, list fallback for a11y, click node → `/app/learn?unit=`
7. ArchUnit/tests: cycle rejected.

### GATE (Phase 4)

- [x] Click node opens unit
- [x] Gap list for a target unit works
- [x] Map does not download the entire graph

**Phase log:**

- **2026-09-17 — Phase 4 complete.** `V3__knowledge_graph.sql` adds `concept_edges`
  (`from_id`, `to_id`, `edge_type`, `weight`, `status`, `note`, `evidence_source_ids[]`) with a
  unique natural key, a no-self-loop check, a type check covering the ten stored edge types, and
  indexes both ways. Containment is **not** duplicated as `PART_OF` edges — `parent_id` plus
  `unit_closure` already answers it, and two representations of one fact would eventually
  disagree. `GENERALIZATION_OF` is not stored either; it is the inverse of `SPECIALIZATION_OF`.
- No `concept_closure` table and no recursive SQL: `GraphQueryService` walks the graph in Java,
  one indexed lookup per hop, because the authored graph is small and the node budget caps the
  work. One traversal then serves both the map and the prerequisite chain.
- API: `GET /v1/units/:id/graph?hops=&types=` and `GET /v1/units/:id/prerequisites`. Hops are
  clamped to 3 (default 2) and nodes to 60; the response reports `hops`, `maxHops`, `nodeBudget`,
  and `truncated`, so a trimmed neighbourhood can never pass as the whole graph. An unknown
  edge type is a 400 naming the valid ones, not a 500.
- Published-only, enforced by reusing `CurriculumQueryService.publishedRefs`: a draft node is
  dropped **with its edges**, since a dangling arrow discloses the draft. Edges are collected
  after the node set is final, so no edge can reference a node the client was not given — there
  is a test asserting exactly that invariant.
- Prerequisites are transitive and ordered deepest-first (study order), with `DEPENDS_ON`
  reported separately as advisory. The walk passes *through* an unpublished unit but does not
  show it, so a published ancestor behind a draft is still listed. Every step reports
  `masteryState: "unknown"` and the response carries `masteryTracked: false`.
- Cycle rejection is a pure function (`PrerequisiteDag.findCycle`) that reports the ring as a
  walk closing on itself, generic over node type so the seed loader checks slugs and the error
  names the units. It runs before any edge is written. The seeder also refuses a published
  non-domain unit with no parent, so nothing can be published unreachable.
- Seed: `80-edges.yml`, 17 authored edges with notes and cited sources, covering the chain the
  spec asks for as far as authored units exist (computer fundamentals → Python → vectors →
  matrices → matmul → neuron → RAG). It stops there: there is no transformers or agents unit,
  so there is no edge pretending otherwise. Two edges deliberately point at the draft attention
  unit, which is what proves the draft-hiding rule in the tests. `RELATED_TO` is reserved for
  pairs with no prerequisite between them, so no claim is drawn twice. The seed owns
  `concept_edges`: deleting a line from the file deletes the relationship.
- Map UI (`/app/map?unit=&hops=`): ring layout by hop distance in SVG, with the adjacency list
  beside it carrying every node and edge, phrased from each node's side ("requires", "must be
  read before", "helps with"). Nodes are real links, so clicking or tabbing to one opens the
  unit. Edge type filters only offer types present in the response. Node colour is deliberately
  meaningless and the caption says why: colouring by mastery would be a claim about the learner
  that nothing has measured.
- Unit pages gained a prerequisite panel: the chain in study order, advisory links, and labelled
  connections, all explicitly "authored, unverified". A domain that merely contains units is
  distinguished from one with nothing to read.
- Verified: `gradle check` (26 tests, 18 new), `pnpm -r typecheck`, `pnpm --filter web build`
  (map at 117 kB first load — no KaTeX), a re-run of `:api:seed` against Postgres, and a browser
  walk: canvas plus list, a node click landing on the unit, a one-hop versus three-hop
  neighbourhood, the draft unit absent, and an unmapped domain saying so plainly.

**Known gaps carried forward:** mastery colouring and real gap detection wait on evidence
(Phase 5); only units are nodes, so technologies, papers, and misconceptions are still
block-level content rather than graph nodes; edge provenance is stored but not surfaced in the
UI; and impact analysis on content change is Phase 15.

---

# PHASE 5 — Learning experience (the product heart, still without tutor)

**Goal:** Evidence-based progress on a unit; Today recommends a real next action.

**Specs:** `07-MASTERY_AND_ADAPTIVE_LEARNING`, `05-UX_SPEC` (chips, next action)

### Steps

1. Tables: `learner_profiles`, `mastery_records`, `mastery_events`, `learning_sessions`, `study_plans`, `plan_items`, `notes`.
2. Onboarding (profile): target path, hours/week, modalities — **self-report is not mastery**.
3. POST `/v1/units/:id/events` (open, dwell, explanation_level).
4. Mastery state machine in Java (pure function + tests): never MASTERED from open.
5. Evidence chips: Read / Practice / Assess / Apply — Read can go partial/solid; Practice waits for exercises (Phase 6 may still be stub submit).
6. Learn page: Why → level controls (Simpler/Deeper even if only two authored levels) → body → “Practice” dock.
7. Rule-based `GET /v1/me/next` and `/v1/me/today` (prereq fail > current waypoint > anything unknown).
8. Next action copy: verb + object + minutes.
9. Notes v0: create/list on a unit.
10. Roadmap UI from path waypoints + mastery overlay.

### GATE (Phase 5)

- [x] Opening a page does **not** complete the unit
- [x] Today shows a specific next action
- [x] Learn is useful with AI turned off

**Phase log:**

- **2026-09-17 — Phase 5 complete.** `V4__learner.sql` adds `learner_profiles`,
  `mastery_events`, `mastery_records`, `learning_sessions`, `study_plans`, `plan_items`, and
  `notes`. Evidence is stored twice on purpose: `mastery_events` is an append-only log of what
  the learner did, and `mastery_records` is a projection of what that currently means. The
  rules will change; the history must not have to. `study_plans` and `plan_items` are created
  here but have no entities yet — plans need the diagnostic in Phase 6, and a table is cheaper
  to migrate now than a table plus a rewrite later.
- The state machine is a pure function (`MasteryPolicy`) over a snapshot, an event, and the
  unit's own facts, so the rules are testable without a database and cannot quietly acquire
  side effects. Reading can fill the Read chip and nothing else, and it stops the unit at
  INTRODUCED however long the learner stays: the states above it are defined by work the
  learner produces, which does not exist until the assessment engine. The read floor is
  proportional to the author's estimate (40% of it, clamped to 90 seconds–12 minutes), a single
  heartbeat cannot claim more than two minutes so a lost tab cannot bank an hour, and a
  container unit with no authored levels is excluded from evidence entirely.
- `POST /v1/units/:id/events` takes `unit_opened`, `dwell`, `explanation_level`,
  `self_check_revealed`, and `exercise_viewed`. The last two are recorded as history and grant
  no evidence, because revealing an answer is not recalling it. An unknown kind is a 400 naming
  the accepted ones. A draft unit is a 404 and collects nothing. Dwell only accrues while the
  tab is visible, and `learning_sessions` closes a session after an idle gap rather than
  letting a forgotten tab read all night.
- Recommendations are a pure ranking (`NextActionPolicy`) with the reason attached to the
  result, rendered as verb + object + minutes. Order: no goal set → unread prerequisite of the
  current waypoint → the current waypoint → reading exhausted. Overdue spaced review ranks
  above all of these in the spec and is deliberately absent rather than faked; when every
  waypoint has been read, the action says plainly that moving past INTRODUCED needs graded
  work instead of inventing a fifth re-read. Minutes shrink as dwell accumulates, so the
  estimate stops repeating itself.
- Onboarding writes `learner_profiles` (target path, hours/week, modalities, experience,
  goals) and is used only to choose *what* to recommend. Self-report never touches evidence: a
  learner claiming ten years of experience still starts at UNKNOWN on every unit.
- UI: the unit page reports dwell and level changes, shows the four chips with the date each
  one last moved and a meter for what the Read chip still needs, and docks the exercises under
  a "not graded yet" badge. Today renders the one action with its reason and the rule that
  produced it. The roadmap lays the learner's state over the path waypoints against the
  author's required state, so a read waypoint is marked on the rail and never filled in as
  complete. The map and prerequisite panel now colour by recorded state, with a key that only
  lists the fills present and copy that distinguishes "not read" from "not understood". Notes
  v0 live in the companion pane and in a Notes space that groups by unit.
- Verified: `gradle check` (59 tests, 31 new), `pnpm -r typecheck`, `pnpm --filter web build`
  (Today 116 kB, learn unit 244 kB with KaTeX), and a browser walk of the gate as a fresh
  learner: onboarding first, then a named next action, opening the unit leaving three chips
  empty and the state UNKNOWN, reading to the floor reaching INTRODUCED and stopping there,
  the recommendation advancing to the next waypoint with prerequisites reported as read, and a
  note surviving a reload in both places it appears.

**Known gaps carried forward:** Practice, Assess, and Apply cannot move, because nothing grades
work yet; the score is a read-only placeholder; review scheduling, misconception tagging, and
`study_plans` generation all wait on Phase 6; and the tutor that would explain a recommendation
in prose is Phase 7.

---

# PHASE 6 — Assessment, diagnostic, spaced repetition

**Goal:** Diagnostic → roadmap; quizzes with **full reports**; reviews due.

**Specs:** `02-PRODUCT_SPEC` ASM-*, `07` diagnostic/SRS, `25` P0 items 2–4, 9–12, 18–19

### Steps

1. Tables: `question_bank`, `assessments`, `assessment_items`, `attempts`, `attempt_answers`, `exam_reports`, `flashcards`, `review_schedules`, misconception flags.
2. Item types P0: MCQ, multi-select, true/false, short text, coding **stub** (string compare or later lab hook).
3. Submit scoring in Java; persist attempts.
4. Report API: strengths, weaknesses, misconception codes, missing prereqs, recommended revision, readiness, next steps — **not score-only**.
5. Diagnostic assessment: 25 seed items, stop rules (max items or enough coverage).
6. After diagnostic, generate `study_plans` version 1 with **reason** field.
7. Guided + independent exercises on complete seed units; Practice chip updates.
8. Golden flashcards + SM-2-like scheduler; `GET /v1/reviews/due`; answer endpoint.
9. Web: diagnostic flow, quiz, report screen, review queue.
10. Tests: report JSON always includes the narrative sections; complete-on-open still impossible.

### GATE (Phase 6) — Milestone M3

- [x] New user: register → profile → diagnostic → roadmap
- [x] Quiz report is not “Score: 78%” alone
- [x] Due reviews appear on Today when scheduled

**Phase log:**

- **2026-09-17 — Phase 6 complete.** `V5__assessment.sql` adds `question_bank`,
  `misconception_catalog`, `assessments`, `assessment_items`, `attempts`, `attempt_answers`,
  `exam_reports`, `misconception_flags`, `flashcards`, `review_schedules`, `review_answers`,
  and `diagnostic_priors`, and gives `study_plans` a `source` and an `attempt_id` so a plan can
  cite the attempt that produced it. Answers are stored per item rather than as a score, because
  every narrative section of a report is a question about individual items and a total cannot be
  asked it later.
- Marking is a pure function (`ItemScoring`) over the item's stored key: MCQ, multi-select,
  true/false, short text with numeric tolerance, and a coding stub that compares against
  accepted solutions and marks itself *unverified* rather than guessing. Partial credit is
  fractional and a partial answer is not a correct one. Unmarkable items are excluded from both
  halves of the fraction, so a limitation of the marker neither penalises nor rewards anyone.
- `GradedEvidencePolicy` is the only place Practice and Assess can move and the only thing that
  can take a unit past INTRODUCED. It is a monotonic ladder: each rung requires every rung below
  it, so a unit whose authored items are all definitions stops at FAMILIAR however much work is
  done, and `reachableCeiling` names that as a content gap instead of letting the learner read
  it as falling short. `nextGate` returns the single unmet requirement, which is what the dock
  and the report quote. MASTERED is unreachable by design: nothing can fill Apply yet.
- Attempts start, resume, serve one item at a time, and mark on the server. A served item
  carries no key and no explanation; the explanation arrives with the mark, including for a
  right answer, because being right for the wrong reason is a real outcome. A second answer to
  the same item is refused rather than re-marked, and starting an assessment twice resumes the
  attempt rather than dealing a fresh set — otherwise the refresh key is a reroll.
- Misconceptions are named rather than inferred from a wrong total. A coded distractor raises a
  flag with the claim and a link to the block that argues against it; answering anything the
  same item can detect, correctly, clears it. An open flag holds the unit at FAMILIAR, and the
  report says so, because a belief that survives a pass keeps producing wrong answers.
- Reports are written once at submission (`exam_reports`) rather than recomputed, so the
  learner and the rules can disagree later without the report changing under them. Every
  attempt yields strengths, weaknesses, misconceptions, missing prerequisites, revision,
  readiness with its reason, the mastery state with the note explaining it, and next steps.
- `ReviewScheduler` is an SM-2 variant, pure, with the interval capped by the volatility of the
  content, leeches flagged rather than repeated forever, and credit given only for a card
  answered on or after its due date: answering early is re-reading with extra steps. Cards are
  enrolled when a unit's Read chip goes solid, so review is earned by reading.
- The diagnostic is adaptive and scores nothing. Stop rules end a domain after three right or
  two wrong, priors are recorded per domain with a confidence label and the item counts behind
  them, and study plan v1 is generated from the learner's path with a reason on every line —
  including the lines it decided to skip, which are shown rather than silently omitted. Asking
  for a plan without a goal is refused with its own `goal_required` code so the client can send
  the learner to onboarding instead of showing an error they cannot act on.
- UI: a graded practice dock on every unit that names the ceiling and the next gate, a quiz
  runner with per-item feedback and a clock, a report screen where the score is not the
  headline, a review queue that withholds the back of the card until a grade is given, the
  diagnostic flow ending in the plan, and an assess hub listing due reviews, open
  misconceptions, and the current plan. Today ranks overdue review above everything else.
- Verified: `gradle check` (110 tests, 51 new), `pnpm -r typecheck`, `pnpm --filter web build`, the API
  walk in `backend/scripts/phase6-walk.ps1` end to end on a freshly migrated database, and a
  browser walk of the gate. The walk found two things the suite could not: `plan_items` had no
  `skip` in its CHECK constraint, and the quiz progress bar added `remaining` to `servedCount`
  and so counted unanswered items twice.

**Known gaps carried forward:** the test database is built from the JPA entities with Flyway
disabled, because the migrations need pgvector and H2 does not have it — so no test in the suite
exercises a CHECK constraint, and `PlanItemKindsTest` reads the constraint out of the migration
as a narrow stand-in for the Testcontainers run this needs. Apply cannot move and MASTERED is
therefore unreachable until the lab (Phase 9) and Project Studio (Phase 10). Coding items are
string-compared and marked unverified until the executor exists. The adaptive rules are authored
priors per difficulty rather than item statistics, which need many learners' answers before they
mean anything. Nothing re-plans yet: a changed goal does not produce version 2, and repeated
failure does not trigger a recalculation.

---

# PHASE 7 — AI tutor

**Goal:** Contextual teaching companion with cost logs. CI never needs paid keys.

**Specs:** `09-AI_ARCHITECTURE`, `15-SECURITY` (injection), `13` tutor SSE

### Steps

1. Tables: `model_providers`, `models`, `model_profiles`, `prompts`, `prompt_versions`, `ai_usage`, `conversations`, `messages`.
2. Java `ChatPort` + `FakeChatClient` (deterministic, Socratic fixtures).
3. Spring AI adapters behind the port; production fails fast if only fake + `prod` profile.
4. Router profiles: `cheap_chat`, `teach`, `code`.
5. Tutor policy: hints before answers; exam-answer refusal; unit + mastery in context.
6. `POST /v1/tutor/conversations`, `POST .../messages` **SSE**.
7. Usage meter: tokens, model, latency, estimated cost, user, feature, prompt version.
8. Web dock: persona default “patient teacher”; Sources drawer (empty until Phase 8).
9. Actions: Explain simpler / deeper / math / code / analogy / quiz me / challenge me.
10. Per-user daily budget; `budget_exceeded` error.
11. Prompt regression tests on fixtures.
12. Banner if FakeChatClient.

### GATE (Phase 7) — Milestone M4

- [ ] Ask a question on a unit and get a teaching reply (fake or live)
- [ ] `ai_usage` rows exist
- [ ] PR CI green without vendor keys

**Phase log:**

---

# PHASE 8 — Search and RAG

**Goal:** Find units; tutor can cite **block ids** when retrieval is on.

**Specs:** `09` RAG, ADR 0006 (Postgres FTS first)

### Steps

1. `tsvector` on units/blocks; `GET /v1/search`.
2. Embedding table + pgvector; chunk published blocks on publish.
3. Retrieve → pack citations → tutor “foundational vs latest” flag (`latest` requires retrieval).
4. Search UI with type chips.
5. Tests: seed unit title is findable; no fabricated source ids.

### GATE (Phase 8)

- [ ] Search finds seed curriculum
- [ ] Tutor message can list `blockId` / source used
- [ ] Timestamp shown for retrieved knowledge

**Phase log:**

---

# PHASE 9 — Coding playground (executor)

**Goal:** Run Python in isolation. API JVM never executes user source.

**Specs:** `15-SECURITY` execution, `13` lab APIs

### Steps

1. `backend/executor` real service: receive run job, start **network-restricted** container or E2B, limits CPU/RAM/time, return stdout/stderr/exit.
2. Executor has **no** Postgres and **no** AI keys.
3. API: `POST /v1/lab/runs`, poll/cancel, persist `lab_runs`.
4. Web Lab: editor, run, stop, output, history.
5. AI debug uses **actual** stdout (Phase 7 port).
6. Tests: mock executor in API tests; integration test that API does not `ProcessBuilder` user code.
7. Quota per user.

### GATE (Phase 9)

- [ ] Seed Python snippet runs and shows output
- [ ] Timeout kills the job
- [ ] Documented proof API does not exec user code

**Phase log:**

---

# PHASE 10 — Project Studio and portfolio

**Goal:** Finish beginner project; Apply chip + portfolio artifact.

**Specs:** `02` PRJ-*, `24` beginner project

### Steps

1. Tables: `projects`, `project_milestones`, `submissions`, `portfolio_items`.
2. Seed: “Build a linear regression model” with milestones and automated checks (script in sandbox).
3. Stub second project “Production RAG” outline only.
4. Submission flow; mentor comments can be rule-based + tutor persona “project mentor.”
5. Portfolio page public/private flag.
6. Completing project updates Apply evidence on linked units.

### GATE (Phase 10) — Milestone M5

- [ ] Learner completes beginner project
- [ ] Portfolio shows artifact
- [ ] Mastery Apply chip can become solid

**Phase log:**

---

# PHASE 11 — Research environment

**Goal:** Papers with **forced** citations; no fabricated references.

### Steps

1. Paper metadata as `sources` type `PAPER`.
2. Seed 3–5 real papers (real titles/DOIs/URLs).
3. Explainer endpoint: retrieve chunks only from attached sources; if none, refuse to invent.
4. Tests: fixture model cannot output a fake DOI (deny-list / schema).
5. Optional PDF upload (size/type limits, private bucket) — parse later if time-boxed.
6. Literature map v0: nodes = papers + related units.

### GATE (Phase 11)

- [ ] Explainer lists only seeded sources
- [ ] Automated test fails if response contains unsourced invented paper titles

**Phase log:**

---

# PHASE 12 — Career engine

**Goal:** Skill gaps from **demonstrated** mastery only.

### Steps

1. Path defines must-master unit set.
2. `GET /v1/me/career/gaps` = required units below PROFICIENT.
3. Interview mode = tutor persona + assessment kind `interview` (text).
4. Web Career: gaps, honesty copy (“not job-ready until …”).
5. No resume parser required if time-boxed (P2).

### GATE (Phase 12)

- [ ] Gaps change only when mastery events change
- [ ] Self-reported onboarding skills do not appear as mastered

**Phase log:**

---

# PHASE 13 — Admin / content studio

**Goal:** Humans publish curriculum; drafts never leak to Learn.

**Specs:** `02` ADM-*, content workflow

### Steps

1. Admin UI (role `author`/`reviewer`/`admin`): units, blocks, questions, resources.
2. Status workflow: draft → review → validate → published → deprecated.
3. Version bump on publish; changelog row.
4. Optional: AI draft generation **creates draft only**.
5. Critic job: missing prereq, empty objectives, difficulty missing (rules first).
6. Tests: learner API cannot see draft; IDOR on other authors’ drafts.

### GATE (Phase 13)

- [ ] Draft unit 404 for learner
- [ ] Publish is audited
- [ ] Rollback of publish documented (DB previous version)

**Phase log:**

---

# PHASE 14 — Analytics

**Goal:** Learner dashboard truth + operator cost/funnel.

**Specs:** `17-OBSERVABILITY`, `02` ANA-*

### Steps

1. Learner dashboard: domain mastery, weak list, velocity, time, next, reviews — **from tables**, not random.
2. Admin: funnel register → diagnostic → first PRACTICED; AI spend from `ai_usage`.
3. Micrometer metrics already from earlier; add Grafana dashboard JSON if possible.
4. No vanity XP unless tied to evidence (XP optional; do not block).

### GATE (Phase 14)

- [ ] Dashboard matches DB for a test user
- [ ] Cost burn visible for operators

**Phase log:**

---

# PHASE 15 — Continuous intelligence + security/observability hardening

**Goal:** Honest currentness; governance; production-shaped safety.

**Specs:** `10-CONTINUOUS_INTELLIGENCE`, `15`, `17`

### Steps

1. Tables from doc 10 if not already: `ai_updates`, proposals, health reports, subscriptions, audit/rollback.
2. Manual admin entry of an update (no scraper required first).
3. Link-checker job (scheduled) on `sources.url`.
4. One official-docs watcher **or** documented manual procedure if watcher slips.
5. Curriculum impact score function + tests.
6. Proposal approve/reject; learner Evolve feed personalized by path.
7. Copy: “Verified against N sources as of timestamp” — never “fully up to date.”
8. OTel Java agent, Sentry, AI budget kill switch, prompt-injection tests, executor network deny **verified**.
9. “What changed since I learned X” using `curriculum_changes` + learner mastery dates.

### GATE (Phase 15) — Milestone M6

- [ ] Evolve shows timestamped items
- [ ] Admin can reject a proposal
- [ ] Lab cannot reach instance metadata IP
- [ ] Currentness language is honest

**Phase log:**

---

# PHASE 16 — Production hardening and launch

**Goal:** Real users can use P0 flows safely.

**Specs:** `16-PRIVACY`, `19-DEVOPS`, `18-TESTING`, `25-ACCEPTANCE`

### Steps

1. Playwright: all 20 P0 learner capabilities in `25-ACCEPTANCE_CRITERIA.md`.
2. `GET /v1/me/export` + delete/anonymize.
3. Privacy policy + ToS pages (legal text from you).
4. Backups + restore drill on staging.
5. Feature flags: `lab_enabled`, `tutor_enabled`.
6. Prod config: no FakeChatClient, HTTPS cookies, secrets manager.
7. Load smoke (k6) on read paths.
8. Threat review checklist signed.
9. Staging sign-off, then prod deploy runbook.

### GATE (Phase 16) — Milestone M7

- [ ] P0 acceptance list ticked
- [ ] Backup restore proven
- [ ] Fake providers disabled in prod
- [ ] Launch checklist signed by product owner

**Phase log:**

---

## After launch (not a phase number — rolling)

- Weekly review of HIGH-volatility units
- Add graph nodes instead of rewriting fundamentals
- Community, voice, extra lab languages, i18n locales — only after P0 is stable
- Visual labs (gradient descent, attention) as components after Phase 5+ anytime

---

## If you must cut scope (order of sacrifice)

Keep: Auth → Curriculum read → Mastery evidence → Diagnostic/quiz report → Today/next → Tutor (even fake-banner) → Isolated Python run → One project.

Cut first: Community, voice, Vaadin fantasies, 50k concepts, OpenSearch, admin polish, career resume parser, multi-agent studio.

---

## Quick command sheet (target)

```text
# deps
make up

# backend
./gradlew :api:bootRun
./gradlew :worker:bootRun
./gradlew :executor:bootRun
./gradlew check
./gradlew :api:seed

# web
pnpm --filter web dev
pnpm --filter web build
```

---

## Phase sign-off template (copy per phase)

```text
Phase: __
Date: __
Tag: phase-N-gate
GATE met: yes / no
Tests: ./gradlew check __  Playwright __
UX issues found: __
Not done (explicit): __
Ready for next phase: yes / no
Owner: __
```

---

*End of master copy. If this file conflicts with a later ADR, follow the ADR and add a one-line amendment at the top of this document (version 1.1, …).*
