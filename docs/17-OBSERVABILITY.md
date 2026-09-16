# 17 — Observability

## Signals

Logs (Logback JSON on JVM; optional web request logs), metrics (Micrometer + OTel), traces (OTel Java agent), errors (Sentry Java + browser), product analytics (first-party events).

## Required metrics

- HTTP: RPS, latency p50/p95/p99, 4xx/5xx by route
- Auth failures
- AI: latency, tokens, cost, error rate, cache hit, by feature/model
- RAG: retrieval empty rate, citation click (later)
- Lab: queue time, run time, timeouts, OOM
- Jobs: lag, retries, failures
- Mastery: events/min (health)
- CI: sources fetched, proposals, gate fails

## Tracing

Propagate `trace_id` from web → api → worker → executor (executor may be sampled). Span names: `tutor.turn`, `router.complete`, `lab.run`.

## Logging rules

No raw prompts in info logs if they contain learner PII — hash or truncated. No secrets. `request_id` always.

## Dashboards

1. API health  
2. AI cost burn  
3. Lab fleet  
4. Currentness (from CI reports)  
5. Learning funnel (onboard → diagnostic → first mastery)

## Alerting (P1)

- Error rate  
- Budget 80%  
- Executor dead  
- Job DLQ  
- Provider fail cascade  

## AI quality telemetry

User flags “hallucination”; sampled citation checks. Not vanity “accuracy 99%.”
