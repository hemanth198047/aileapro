# 13 — API design

Base: `https://{host}/v1`  
Style: JSON REST. Compatibility: additive changes in `/v1`; breaking → `/v2`.

## Cross-cutting

| Concern | Rule |
|---|---|
| Auth | Session cookie or `Authorization: Bearer` |
| AuthZ | RBAC + resource ownership |
| Validation | Bean Validation (`jakarta.validation`) at the API boundary |
| Errors | `{ error: { code, message, details?, request_id } }` |
| Pagination | `cursor` preferred; `limit` max 100 |
| Filtering | allowlisted query params |
| Sorting | allowlisted fields |
| Rate limit | per user + per IP; stricter on `/ai/*` and `/lab/run` |
| Idempotency | `Idempotency-Key` on POSTs that enqueue money/AI/lab |
| Observability | `X-Request-Id` echoed |

Error codes: `unauthorized`, `forbidden`, `not_found`, `validation_error`, `conflict`, `rate_limited`, `provider_unavailable`, `sandbox_timeout`, `budget_exceeded`.

## Resource groups (P0)

### Auth
`POST /v1/auth/register`  
`POST /v1/auth/login`  
`POST /v1/auth/logout`  
`POST /v1/auth/verify-email`  
`POST /v1/auth/password/forgot|reset`  
`GET /v1/me`

### Catalog
`GET /v1/paths`  
`GET /v1/paths/:id`  
`GET /v1/units/:id` (published payload + learner overlays if authed)  
`GET /v1/units/:id/graph?hops=2`  
`GET /v1/units/:id/resources`

### Learner
`PATCH /v1/me/profile`  
`GET /v1/me/roadmap`  
`POST /v1/me/diagnostic/start|answer`  
`GET /v1/me/mastery`  
`GET /v1/me/next`  
`GET /v1/me/today`

### Learning activity
`POST /v1/units/:id/events` (read dwell, etc.)  
`GET /v1/units/:id/exercises`  
`POST /v1/exercises/:id/submit`

### Assessment
`POST /v1/assessments/:id/attempts`  
`POST /v1/attempts/:id/answers`  
`POST /v1/attempts/:id/submit`  
`GET /v1/attempts/:id/report`

### Tutor
`POST /v1/tutor/conversations`  
`POST /v1/tutor/conversations/:id/messages` (SSE stream)  
`GET /v1/tutor/conversations/:id/sources`

### Lab
`POST /v1/lab/runs`  
`GET /v1/lab/runs/:id`  
`POST /v1/lab/runs/:id/cancel`

### Search
`GET /v1/search?q=&types=`

### Projects
`GET /v1/projects`  
`GET /v1/projects/:id`  
`POST /v1/projects/:id/submissions`

### SRS
`GET /v1/reviews/due`  
`POST /v1/reviews/:id/answer`

### Intelligence (learner)
`GET /v1/evolve/feed`  
`GET /v1/units/:id/changelog`

### Admin (role)
`CRUD /v1/admin/units`  
`POST /v1/admin/units/:id/publish`  
`GET /v1/admin/updates`  
`POST /v1/admin/updates/:id/approve|reject|rollback`

## Streaming

SSE for tutor tokens. Reconnect with `Last-Event-Id`.

## Webhooks (later)

Outbound signed webhooks not in P0.

## Documentation

OpenAPI generated from **springdoc** on `backend/api`. Published at `/v3/api-docs` and operator Swagger UI. TypeScript client generated into `packages/api-client` on CI. Learner-facing docs subset.

## Client rules

`apps/web` uses the generated OpenAPI client only. No ad-hoc fetch to model providers from the browser.
