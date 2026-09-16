# 16 — Privacy and compliance

## Principles

Minimize. Purpose-limit. Learner-owns transcripts. No selling personal graphs.

## Data classes

| Class | Examples | Handling |
|---|---|---|
| Account | email, hash | necessary |
| Learning | mastery, attempts | core product |
| AI transcripts | chats | opt retention |
| Code | lab sources | retention window |
| Usage | tokens | ops / billing |
| Special | resume uploads | opt-in career, extra lock |

## Legal (plan, not advice)

- Privacy policy + ToS before public launch.
- Cookie banner if EU tracking (prefer first-party only).
- GDPR/CCPA: export (`GET /v1/me/export`), delete (anonymize events, hard-delete PII).
- DPA if B2B later.
- Children’s: **13+ or 16+ by region**; no under-13 accounts in P0. Age gate.

## AI / training

Default: **do not** send learner data to providers for their training. Contractual zero-retention where available. Document subprocessors.

## Content IP

- Seed content original.
- Ingest licensed/public only.
- User projects: learner retains IP; license to host for running the service.

## Internationalization / data residency

P0 single region. Record residency as future requirement on org.

## DPIA triggers

Continuous scraping + profiling learners: document legitimate interest vs consent for CI personalization; prefer consent for marketing-like feeds; learning-necessary processing in ToS.
