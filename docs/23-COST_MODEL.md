# 23 — Cost model

## Build (indicative, USD, 2026)

| Item | P0 months | Note |
|---|---|---|
| Compute (1 staging + 1 small prod) | $80–250/mo | |
| Postgres managed | $30–80/mo | |
| Redis | $15–40/mo | |
| Object storage | $5–20/mo | |
| OpenSearch (can defer) | $0–120/mo | |
| Observability | $0–50/mo | Grafana cloud free tier possible |
| E2B or extra VM for sandbox | $20–200/mo | usage |
| Domains/email | $10/mo | |

## AI (dominant variable)

Assume blended $3–15 per 1M tokens. Tutor-heavy learner: 50–200k tokens/day worst; **budgets cap this**.

P0 target: **authored content is default**; AI on demand.

Monthly AI at 200 DAU × $0.15 average = ~$900. Design for $0.05 by caching.

## People

Biggest cost. Phases 1–10 ≈ 4–6 months elapsed for a strong full-stack pair.

## Cost controls in product

Usage table, per-user daily cap, cheap model default, cache, no embed-on-every-keystroke, batch CI jobs off-peak.

## Pricing (product, undecided)

Document only: likely freemium (learn authored) + quota on tutor/lab. Not blocking engineering.
