# 10 — Continuous intelligence (always-current engine)

This is **core**, not a blog widget. Goal: remain trustworthy as the field moves. Not: scrape the internet and rewrite the course.

## Product principle

DISCOVER → VERIFY → UNDERSTAND → CONNECT → TEACH → ASSESS → UPDATE → AUDIT → ROLL BACK IF NEEDED

Never: “Everything is always up to date.”

Always: evidence, timestamps, confidence, conflicts.

## Pipeline

```
DISCOVER → FETCH → DEDUPLICATE → CLASSIFY → EXTRACT → VERIFY
→ SCORE RELEVANCE → MAP TO GRAPH → DETECT CURRICULUM IMPACT
→ GENERATE PROPOSED UPDATES → VALIDATE GATES → PUBLISH OR QUEUE
```

Idempotent jobs. At-least-once with dedupe keys (canonical URL + content hash).

## Sources (priority order)

1. Official model/API/framework docs and release notes  
2. Standards, security advisories, regulators  
3. Papers (peer-reviewed first; preprints labeled)  
4. Major lab / university pages  
5. Canonical OSS repos (releases, not random issues)  
6. Reputable technical publishers  

Community blogs: **signals**, not auto-truth.

## Discovery agents (structured output only)

1. Research  
2. Model releases  
3. Framework releases  
4. Open source  
5. Developer tools  
6. Benchmarks  
7. Security  
8. Governance  
9. Industry trends  
10. Curriculum impact analysis  

Each emits `AIUpdate` candidates: what, when, who, why, sources[], affected concepts[], confidence, lifecycle hint (experimental/emerging/established/deprecated).

## Curriculum impact score (internal)

Explainable checklist (0–1 + reasons):

- New concept? change existing? obsolete technique? tool change? API change? pattern change? new prereq? new specialization? industry expectation? new exercise/project/assessment? career skill change?

Not a learner leaderboard.

## Governance modes

1. Auto: low-risk metadata, link health, last-verified dates  
2. Auto after validators: moderate (add resource, freshness bump)  
3. Human required: explanations, assessments, path waypoints, deprecations of core techniques  
4. Emergency rollback  

Simulation: “N learners, M units, K assessments affected.”

## Learner-facing conversion

NEWS → KNOWLEDGE → MICRO-LESSON → PRACTICE → ASSESSMENT → MASTERY

Micro-lesson template: what happened, why it matters, need to know, prereqs, example, code, exercise, further reading.

Migration: do not restart whole RAG path because retrieval research moved.

## Feeds and UX

- Daily brief (personalized)  
- Evolve space: today/week/month/emerging/deprecated  
- Filters by topic area  
- “Why should I care?” based on path, mastery, projects, role  
- “AI changed since you last learned X”  
- Subscriptions: “notify me on RAG / agents / CVE-like advisories”  
- Digests: daily/weekly/monthly + emergency security  

## Lifecycle tracker

EMERGING → EXPERIMENTAL → ADOPTED → ESTABLISHED → MATURE → DECLINING → DEPRECATED → RETIRED  

Evidence-based; show uncertainty.

## Resource health

Link checks, doc drift, video availability, repo status, version pins. Replace only with verified sources; queue review.

## Daily curriculum health

Stale, obsolete, missing developments, broken links, dupes, inconsistent terms, contradictory explanations, old code/exams. Output `CurriculumHealthReport`.

## Admin Update Center

Queue, sources, confidence, diffs, approvals, rejections, rollbacks, stale, broken, review SLAs.

Currentness dashboard: **not** one fake percentage. Show: % verified in window **by volatility band**, open conflicts, pending, failures.

## Data entities

AIUpdate, AIUpdateSource, AIEvent, AITrend, TechnologyLifecycle, TechnologyVersion, CurriculumVersion, CurriculumChange, CurriculumChangeProposal, CurriculumImpact, KnowledgeFreshness, SourceVerification, ResourceHealth, DeprecationNotice, MigrationGuide, LearningPathRevision, LearnerUpdate, UpdateDigest, UpdateSubscription, AIAlert, AIResearchSignal, IndustrySkillSignal, EmergingSkill, KnowledgeDiff, UpdateAuditLog, UpdateRollback, CurriculumHealthReport.

## Jobs (retryable, observable, rate-limited)

Source discovery/refresh/verify, broken links, research/model/framework discovery, impact analysis, stale detection, update generation, health, learner updates, digests, lifecycle.

## P0 vs later

**P0:** schema, freshness fields, honest UI timestamps, manual `AIUpdate` entry, no claim of full automation.  
**P1:** link checker + one official-docs watcher + proposal workflow.  
**P2:** multi-agent discovery + personalized feed + auto micro-lessons under gates.
