# 11 — System architecture

## 1. System context

```
[Learner browsers / future mobile]
        │ HTTPS
[CDN + apps/web Next.js]
        │ HTTPS JSON / SSE (generated TS client)
[backend/api Spring Boot]
        ├─ PostgreSQL (+ pgvector)
        ├─ Redis (cache, Spring Session, job streams)
        ├─ OpenSearch (when enabled)
        ├─ S3/MinIO
        ├─ backend/worker (Spring jobs / Batch)
        ├─ backend/executor (sandboxed code)
        └─ Model providers (outbound only)
```

Admin uses the same web app with role gates. Workers never exposed publicly.

## 2. Domain architecture (modular monolith → extractable)

Spring Modulith applications (`backend/api` and `backend/worker` share `backend/libs`). Each bounded context is a Java module:

| Module | Owns |
|---|---|
| Identity | users, sessions, RBAC |
| Learner | profile, prefs, plans |
| Curriculum | units, blocks, paths, versions |
| Graph | edges, closure, queries |
| Mastery | events, scores, states, SRS |
| Assessment | items, attempts, exams, reports |
| Tutor | conversations, personas, tools |
| AI | router, prompts, usage |
| Retrieval | ingest, index, search, RAG |
| Lab | submissions metadata |
| Executor client | jobs to executor |
| Project | studio, milestones, portfolio |
| Intelligence | updates, health, proposals |
| Career | gaps, interviews (later) |
| Community | later |
| Notify | outbox + channels |
| Admin | studio facades |
| Analytics | events sink |
| Audit | append-only logs |

**Rules:** no importing another module’s internals; use application services / events. ArchUnit enforces package boundaries. OpenAPI is the contract with `apps/web`.

## 3. Data architecture

- **OLTP:** Postgres primary.  
- **Search:** OpenSearch copies of published units/resources.  
- **Vectors:** pgvector on `embedding` table keyed by chunk.  
- **Blob:** objects (audio, images, project zips).  
- **Jobs:** Redis.  
- **Analytics:** start with Postgres events; warehouse later (ClickHouse/BigQuery) at 100k users.

See [12-DATA_MODEL.md](12-DATA_MODEL.md).

## 4. AI architecture

See [09-AI_ARCHITECTURE.md](09-AI_ARCHITECTURE.md) and [10-CONTINUOUS_INTELLIGENCE.md](10-CONTINUOUS_INTELLIGENCE.md).

## 5. Curriculum architecture

See [06-CURRICULUM_MODEL.md](06-CURRICULUM_MODEL.md). Runtime reads **published** versions only; authors see drafts.

## 6. Security architecture

Trust boundaries:

- Browser untrusted.
- Web may hold session cookie (httpOnly, secure, samesite).
- API authenticates every request.
- Executor: separate network, no Postgres credentials, no cloud keys; artifact store scoped.
- Providers: API keys in secret manager; never to browser.
- Ingest fetcher: allowlist, SSRF protection.

Details: [15-SECURITY.md](15-SECURITY.md).

## 7. Deployment architecture

**Local:** Docker Compose — postgres, redis, opensearch, minio, api, web, worker, executor (or executor mock with warning).

**Staging/prod P1:** Compose on a VM or ECS/Fly.  
**Prod P2:** Kubernetes: deployments for web, api, worker; job for executor with gVisor/Firecracker; managed Postgres/Redis; S3.

Migrations: Flyway in CI (`backend/api`). Zero-downtime: expand/contract.

## 8. Scalability architecture

| Stage | Users | Tactics |
|---|---|---|
| P0 | ≤1k DAU | single region, one API, cache pages |
| 10k | read replicas, queue isolation, CDN |
| 100k | shard heavy event tables, search cluster, executor pool |
| 1M | multi-region later, cell-based optional |

Do not shard at P0. Keep module boundaries so we can extract `executor`, `intelligence`, `search` first.

Async: ingest, embed, health, emails, AI batch (flashcard gen).

Caching: unit published payloads, graph neighborhoods, embeddings.

## 9. Mobile

REST/JSON versioned APIs, pagination, auth bearer or cookie documented. No desktop-only payloads (avoid huge SSR-only graphs without API).

## 10. Failure modes

| Failure | Behavior |
|---|---|
| LLM provider down | authored content works; tutor banner |
| Executor down | lab disabled with queue message; learning continues |
| OpenSearch down | SQL fallback search degraded |
| Redis down | jobs pause; API still serves reads if cache optional |

## 11. Repository layout (target)

```
aileapro/
  frontend/                  # TypeScript only (pnpm workspace)
    web/                     # Next.js UI
    packages/ui              # @aileapro/ui design system
    packages/api-client      # generated from springdoc (later)
  backend/                   # Java only
    settings.gradle.kts
    api/                     # Spring Boot HTTP + SSE
    worker/                  # jobs, intelligence, emails
    executor/                # sandbox controller
    libs/domain
    libs/ai
    libs/security
    libs/persistence
    scripts/                 # API phase walks, backup drill
  docs/
  infra/
    compose/                 # Postgres, Redis, MinIO
    scripts/                 # local stack start/stop
    grafana/
    k8s/                     # later
  start.bat / start.sh
  stop.bat  / stop.sh
```
