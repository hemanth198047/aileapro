# 27 — Operating model, legal, and unstated factors

Factors not named in the master prompt, now decided or flagged.

## Team shape

- Product owner (you): scope cuts, curriculum truth, go-live.
- Engineering: Java/Spring backend + React/Next frontend + infra.
- Part-time: learning-science reviewer for seed units.
- Legal: before ingest and public launch (ToS, copyright, age).

## Curriculum governance

Treat authors like a **journal**: editor (reviewer role) for major changes. Automated critic is not a substitute.

## Education ethics

- Diagnostics must not shame.
- No dark patterns on streaks (allow pause).
- XP never substitutes mastery.
- Interview/resume features are optional and must not leak to public search.

## Academic integrity

Exam mode: tutor restricted, copy-paste detection later, item randomization. Assume motivated cheating; certify only with projects + live interview when it matters.

## Support

In-app “report content” and “report hallucination” from day of tutor launch.

## Localization

Architecture ready; human translation for `en`→others, not raw AI publish.

## Accessibility law

Target WCAG 2.2 AA for P0 flows; graph has list alternative.

## Windows development

This workstation failed `git init` via `/bin/sh`. **Standardize on Git for Windows or WSL2** before Phase 1. Install **JDK 21 (Temurin)** and Node 22. Compose: WSL2 backend recommended for the executor. IntelliJ IDEA is the expected Java IDE; Cursor/VS Code for the web app is fine.

## Branding

The product is **AILEAPRO** everywhere a learner can see it. Lowercase `aileapro` is reserved for the repository, packages, and infrastructure names.

## Open source

Default **proprietary** until you decide otherwise. No accidental AGPL from embeddings libs — license check in Phase 1.

## Data backup / disaster

RPO 24h P0, 1h P2. RTO 8h P0.

## Success metrics (product)

- Time-to-first-evidence (diagnostic done)  
- % units reaching PRACTICED not just INTRODUCED  
- 7-day return  
- Cost per active learner  
- Hallucination report rate  
- Curriculum verification coverage by volatility band  

Vanity: raw XP, video minutes.

## Dependencies on you (human)

- JDK 21 (Temurin) and Node 22 on developer machines.
- Model provider keys when tutor goes live.
- Cloud account for prod.
- Decision on E2B vs self-hosted gVisor.
- Trademark/domain for “AILEAPRO.”

