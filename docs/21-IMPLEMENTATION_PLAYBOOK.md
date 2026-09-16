# 21 — Implementation playbook

How we actually build, every cycle.

## Before a subsystem

1. Re-read the matching spec (06–16).  
2. Inspect repo (Java modules, Flyway, tests, debt).  
3. List dependencies (schema, APIs, UI).  
4. Write/adjust **Flyway** migration.  
5. Implement domain in `backend/libs` + JUnit first.  
6. Expose Spring controllers / events.  
7. Regenerate OpenAPI TS client; UI against real API.  
8. `./gradlew check` and `pnpm --filter web build`.  
9. UX checklist (doc 05).  
10. Short summary + remaining.

## Coding rules

- Java for all server logic; TypeScript only in `apps/web` and `packages/ui`.  
- No `TODO` as architecture.  
- No fake mastery.  
- `FakeChatClient` must be explicit.  
- Strong types at boundaries (Bean Validation + OpenAPI).  
- No giant React components (> ~300 lines: split).  
- No giant Spring services that span two modules — split or publish events.  
- Feature flags for risky exec/AI.

## Definition of done

- Tests for domain logic (JUnit)  
- API errors documented  
- UI empty/error  
- Observability spans/logs  
- No secrets  
- Seed/demo path works  

## Agent/team protocol

- One phase at a time.  
- Do not skip executor security.  
- Do not mass-generate 10k units.  
- Prefer extending graph over duplicating content.

## Risk of this playbook

If calendar slips: **cut P1 UI (admin studio)** and keep a Java CLI (`./gradlew :api:seed` / admin commands) so the learner loop still ships.
