# 25 — Acceptance criteria

## Documentation (this cycle)

- [x] Vision, spec, IA, UX, curriculum, mastery, graph, AI, CI, architecture, data, API, stack, security, privacy, observability, testing, devops, roadmap, playbook, risks, cost, seed, glossary, ADRs.

## P0 product (later implementation)

A learner can:

1. Register/login  
2. Select a learning goal  
3. Take a diagnostic  
4. Receive a personalized roadmap with reasons  
5. Browse curriculum  
6. Open a topic  
7. Learn the concept (authored, multi-level)  
8. Ask the AI tutor (or explicit FakeChatClient in demo with banner)  
9. Practice exercises  
10. Complete assessments with a **full** report  
11. See mastery **state + evidence**, not a fake bar  
12. Receive recommended next steps  
13. Run Python in sandbox  
14. Complete the beginner project  
15. View portfolio  
16. Search the knowledge base  
17. Explore related concepts on the map  
18. Review weak areas / due cards  
19. Follow a study plan  
20. Track overall progress on dashboard  

Plus:

- Secrets never in the client  
- No complete-on-open  
- Freshness copy honest  
- Tests + build green  
- UX checklist pass on Today, Learn, Map, Lab, Report  

## Phase gates

Each phase in [20-ROADMAP.md](20-ROADMAP.md) has a Gate line. Do not start the next phase if the gate fails unless a written exception exists.

## Launch (Phase 16)

- [x] Privacy policy + ToS stubs (`/privacy`, `/terms`)
- [x] `GET /v1/me/export` + `DELETE /v1/me` anonymize
- [x] Feature flags `lab_enabled` / `tutor_enabled` (+ admin PUT)
- [x] Prod profile: secure cookies; FakeChat + fake lab refused at startup
- [x] Backup restore drill script + runbook
- [x] Launch checklist for product-owner sign-off (`docs/LAUNCH_CHECKLIST.md`)

Privacy policy, backups, flags, error tracking, budget kill switch, executor network deny verified.
