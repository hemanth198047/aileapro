# 02 — Product specification (complete)

**Product:** AI Mastery  
**Engineering name:** ailepro  
**Status:** Planning freeze. Implementation follows [20-ROADMAP.md](20-ROADMAP.md).  
**Source of requirements:** master prompt (Always-Current) + architectural defaults in this pack.

This document is the feature contract. If a screen exists without a matching capability here, it is out of scope or an ADR is required.

---

## 1. Actors and roles

| Role | Can |
|---|---|
| `anonymous` | Marketing site, limited Evolve feed (public, delayed) |
| `learner` | Full learning loop, lab (quota), tutor (quota), portfolio |
| `mentor` | Comment on shared projects, review submissions (phase 13+) |
| `author` | Content studio, drafts, critic, cannot publish major changes alone if policy requires reviewer |
| `reviewer` | Approve curriculum changes, quality gates |
| `admin` | Users, models, prompts, update center, rollback |
| `operator` | Observability, jobs, cost, security events |
| `system` | Workers, agents, schedulers |

RBAC is additive. A user may hold multiple roles. Permissions are resource-scoped (own learner data vs global curriculum).

---

## 2. Capability catalog

Capabilities are tagged: **P0** (MVP end-to-end), **P1** (first production), **P2** (scale), **P3** (advanced). P0 must be real, not mocked.

### 2.1 Identity and learner model

| ID | Capability | Priority |
|---|---|---|
| AUTH-01 | Email/password register, login, logout, session refresh | P0 |
| AUTH-02 | Email verification, password reset | P0 |
| AUTH-03 | OAuth (Google, GitHub) | P1 |
| AUTH-04 | MFA (TOTP) | P2 |
| AUTH-05 | RBAC enforcement on every API | P0 |
| PROF-01 | Profile: goals, hours/week, style prefs, target role, timezone | P0 |
| PROF-02 | Privacy controls: export, delete, visibility of portfolio | P1 |
| PROF-03 | Achievements, XP, levels (meaningful only) | P1 |
| PROF-04 | Certificates (competency-based) | P2 |
| PROF-05 | Learning version record (what curriculum/tech versions were learned) | P1 |

### 2.2 Curriculum and learning experience

| ID | Capability | Priority |
|---|---|---|
| CUR-01 | Flexible hierarchy of arbitrary depth (adjacency + type) | P0 |
| CUR-02 | Domain taxonomy A–BE seeded (see 24-SEED) | P0 |
| CUR-03 | Learning paths as graph projections | P0 |
| CUR-04 | Unit payload: objectives, explanations (6 levels), examples, math, code, mistakes, applications, references, difficulty, time, tags, tech, version, last reviewed | P0 (fields present; not all filled for every seed unit) |
| CUR-05 | Mastery gate (no complete-on-open) | P0 |
| CUR-06 | Modes: Read, Practice, Quiz, Tutor, Code, Flashcards, Revision | P0 |
| CUR-07 | Modes: Watch, Listen, Simulation, Interview, Exam, Teaching, Challenge, Research, Project, Interactive viz | P1–P2 |
| CUR-08 | Split-pane: explanation + lab/exercise | P0 |
| CUR-09 | Breadcrumbs, command palette, global search | P0 |
| CUR-10 | Dark mode, responsive, a11y baseline (WCAG 2.2 AA intent) | P0 |
| CUR-11 | i18n architecture (`en` first) | P0 architecture / P3 locales |
| CUR-12 | Version-aware education (foundational vs current vs deprecated) | P1 |
| CUR-13 | “What changed since I learned this” | P1 |

### 2.3 Adaptive engine and mastery

| ID | Capability | Priority |
|---|---|---|
| MAS-01 | Per-unit mastery state machine + composite score | P0 |
| MAS-02 | Track accuracy, attempts, time, misconceptions, retention | P0 |
| MAS-03 | Prerequisite-aware “learn next” (not “next lesson”) | P0 |
| MAS-04 | Diagnostic assessment at onboarding | P0 |
| MAS-05 | Personalized roadmap (daily/weekly/monthly/long-term) | P0 |
| MAS-06 | Daily coach: “What should I learn today?” | P0 |
| MAS-07 | Spaced repetition scheduler + auto flashcards | P0 |
| MAS-08 | Roadmap recalc on material ecosystem change | P1 |
| MAS-09 | Learning velocity, time-on-task analytics | P1 |

### 2.4 Knowledge graph

| ID | Capability | Priority |
|---|---|---|
| KG-01 | Typed edges (prerequisite, depends, related, part-of, specialization, used-by, misconception, alternative, evolution, applied-in, implemented-by) | P0 |
| KG-02 | Gap detection from graph + mastery | P0 |
| KG-03 | Visual knowledge map (node states) | P0 |
| KG-04 | Click node → open unit | P0 |
| KG-05 | Literature / evolution overlays | P2 |

### 2.5 AI tutor and companion

| ID | Capability | Priority |
|---|---|---|
| TUT-01 | Persistent tutor with session memory scoped to learner + unit | P0 |
| TUT-02 | Teach, don’t dump: Socratic default, reveal ladder | P0 |
| TUT-03 | Explanation levels 1–6 + simplify/deeper/math/visual/code/analogy/challenge | P0 |
| TUT-04 | Misconception detection and reteach | P0 |
| TUT-05 | Generate exercises, evaluate answers, review/debug code | P0 |
| TUT-06 | Flashcards, revision packs | P0 |
| TUT-07 | Mentor personas (patient, strict, coding, research, interview, architect, project) — non-gimmicky | P1 |
| TUT-08 | Oral exam / mock interview (voice later) | P1 text / P2 voice |
| TUT-09 | Paper explain, approach compare, doc summarize with sources | P1 |
| TUT-10 | Selection actions on any text/code: explain, quiz, connect, apply | P1 |
| TUT-11 | Latest vs foundational routing (retrieve current sources for “latest”) | P1 |
| TUT-12 | Citations inspectable: “What sources did you use?” | P0 for retrieved answers |
| TUT-13 | Voice tutor architecture | P3 |

### 2.6 Assessment, exams, revision

| ID | Capability | Priority |
|---|---|---|
| ASM-01 | Item types: MCQ, multi-select, T/F, fill-blank, coding, debug, math, scenario, open-ended | P0 subset: MCQ, multi, T/F, short, coding |
| ASM-02 | Bloom-ish dimensions: knowledge → system design | P0 tagging |
| ASM-03 | Module/chapter exams | P1 |
| ASM-04 | Domain + comprehensive mastery exam | P2 |
| ASM-05 | Timed, adaptive exams | P1 adaptive quiz / P2 full |
| ASM-06 | Reports: strengths, weaknesses, misconceptions, missing prereqs, revision, readiness, next steps — never score-only | P0 |
| ASM-07 | Pre-exam revision engine | P1 |
| ASM-08 | Oral / interview assessments | P1 |
| ASM-09 | Project assessment + automated tests | P1 |

### 2.7 Lab, playground, projects

| ID | Capability | Priority |
|---|---|---|
| LAB-01 | Python editor: highlight, run, stop, output, history | P0 |
| LAB-02 | Error explanation, AI debug, hints, solution comparison | P0 |
| LAB-03 | Autocomplete (practical) | P1 |
| LAB-04 | Language interface for JS/TS/SQL/Java/C++/Rust later | P0 interface / P2 langs |
| LAB-05 | Secure remote execution, quotas, kill switch | P0 |
| LAB-06 | Dataset + simple ML experiment notebooks-lite | P1 |
| LAB-07 | Embedding / RAG / prompt / model playgrounds | P1 |
| LAB-08 | Agent studio (tools, memory, planning, eval, traces) | P2 |
| PRJ-01 | Project studio: objective, prereqs, milestones, hints, criteria | P0 one seed project |
| PRJ-02 | AI mentor + code/architecture review | P1 |
| PRJ-03 | Portfolio artifacts | P0 |
| PRJ-04 | Research reproduction projects | P2 |

### 2.8 Search, RAG, ingestion, provenance

| ID | Capability | Priority |
|---|---|---|
| SRCH-01 | Keyword search: curriculum, concepts, resources, exercises, projects | P0 |
| SRCH-02 | Semantic + hybrid + rerank + filters | P1 |
| RAG-01 | Provider-swappable vector store; citations; query rewrite; metadata filter | P1 |
| ING-01 | Ingest pipeline: source → parse → chunk → extract → classify → link → embed → index → validate → publish | P1 |
| ING-02 | PDFs, web, papers, transcripts where legally permitted | P1–P2 |
| PROV-01 | Every external resource: title, author, publisher, URL, date, type, license, last verified, relevance, difficulty | P0 on seed resources |
| PROV-02 | Distinguish primary / official docs / paper / book / course / video / blog / community | P0 |

### 2.9 Continuous intelligence (always-current)

See [10-CONTINUOUS_INTELLIGENCE.md](10-CONTINUOUS_INTELLIGENCE.md). Summary:

| ID | Capability | Priority |
|---|---|---|
| CI-01 | Discovery agents (research, models, frameworks, OSS, tools, benchmarks, security, governance, industry, curriculum impact) | P1 stubs / P2 live |
| CI-02 | Daily brief + personalized feed | P1 |
| CI-03 | News → knowledge pipeline with provenance | P1 |
| CI-04 | Curriculum impact score + proposed updates | P1 |
| CI-05 | Human-in-the-loop governance + rollback + simulation | P1 |
| CI-06 | Learner changelog, diffs, migration lessons | P1 |
| CI-07 | Technology lifecycle + deprecation + resource health | P1 |
| CI-08 | Admin Update Center + Currentness dashboard | P1 |
| CI-09 | Never claim “always fully up to date”; show evidence timestamps | P0 copy + metadata |

### 2.10 Career, community, notifications, admin

| ID | Capability | Priority |
|---|---|---|
| CAR-01 | Skill-gap vs demonstrated mastery | P1 |
| CAR-02 | Interview prep, system design, resume (opt-in) | P2 |
| COM-01 | Discussions, Q&A, groups, peer review | P2 |
| COM-02 | AI thread summary, misconception flags | P2 |
| NOT-01 | Review reminders, goals, deadlines, prefs | P1 |
| ADM-01 | Curriculum/question/project/resource editors | P1 |
| ADM-02 | Prompt + model router config | P0 config files / P1 UI |
| ADM-03 | Content versioning: draft → review → validate → published → deprecated | P0 data model / P1 UI |
| ADM-04 | AI authoring tools + critic | P1 |
| ANA-01 | Learner dashboard (mastery, streak, weak, next, velocity) | P0 |
| ANA-02 | Admin analytics + AI cost/latency/quality | P1 |

### 2.11 Cross-cutting (from 100+ list)

All 120 numbered features in the master prompt are **in-scope for architecture**. Implementation order is [20-ROADMAP.md](20-ROADMAP.md). None may be faked as “done.”

---

## 3. Functional flows (P0 contracts)

### 3.1 Register and onboard

1. Create account → verify email.  
2. Collect: target path/role, weekly hours, prior skills (self-report), modality prefs.  
3. Run diagnostic: adaptive item bank covering foundations + claimed skills; stop when posterior confidence ≥ threshold or item cap.  
4. Persist mastery priors (never treat self-report as mastery).  
5. Generate roadmap version 1 with explainable reasons.

### 3.2 Learn a unit

1. Prerequisite check: if failing, offer reteach path (do not hard-block forever; warn + recommend).  
2. Sequence: Why → intuition → formal → visual if present → math if relevant → code → guided exercise → independent → quiz.  
3. Tutor available in-context.  
4. Mastery updates only on evidence.  
5. Recommend next from graph + plan + reviews due.

### 3.3 Practice and assess

1. Items tagged by unit, misconception codes, difficulty, volatility.  
2. Immediate feedback with teaching, not only correctness.  
3. Exam report template mandatory (section 2.6 ASM-06).

### 3.4 Lab

1. Client sends source + language + limits to API.  
2. API authorizes, records, enqueues executor.  
3. Executor runs in network-restricted sandbox, returns stdout/stderr/artifacts.  
4. AI debug uses output + source; never claims to have run code it did not run.

### 3.5 Tutor turn

1. Build context: unit, mastery, recent mistakes, selected text, retrieval (if factual/latest).  
2. Route model by task class.  
3. Log tokens, cost, latency, feature, prompt version.  
4. Filter output (injection, secrets, disallowed content).  
5. Persist messages; learner can inspect sources.

---

## 4. Non-functional requirements

| ID | Requirement |
|---|---|
| NFR-LAT-1 | Learn page TTFB < 300ms cached; < 1.2s uncached p95 (region) |
| NFR-LAT-2 | Tutor first token < 2s p95 when provider healthy |
| NFR-LAB-1 | Sandbox start < 8s p95 warm; hard timeout configurable (default 30s P0) |
| NFR-AVL | API 99.5% monthly P1, 99.9% P2 |
| NFR-SCALE | Architecture for 10k → 100k → 1M users; P0 sized for 1k DAU |
| NFR-SEC | OWASP ASVS L2 intent; see 15-SECURITY |
| NFR-A11Y | Keyboard complete for P0 flows; contrast; reduced motion |
| NFR-I18N | No concatenated sentences in UI copy; ICU messages |
| NFR-COST | Per-user AI budget + global kill switch |
| NFR-PRIV | Data minimization; learner graph not sold |

---

## 5. Content policy

- Do not fabricate references.  
- Do not present model memory as primary source for “latest.”  
- Seed content must be original explanations plus cited public resources.  
- Ingest only legally permitted material.  
- Security labs are **defensive**; no exploit PoCs for real systems.

---

## 6. Explicit non-goals (v1)

- Native iOS/Android apps (responsive web + future API clients).  
- Marketplace of third-party paid courses.  
- Social network / dating / general chat.  
- Running GPU training clusters for learners in P0 (small CPU experiments only).  
- Claiming completeness of all 50k micro-concepts.

---

## 7. Traceability

Master prompt sections 1–130 map onto this spec + docs 06–10. Roadmap phases 0–16 map onto implementation. Acceptance in [25-ACCEPTANCE_CRITERIA.md](25-ACCEPTANCE_CRITERIA.md).
