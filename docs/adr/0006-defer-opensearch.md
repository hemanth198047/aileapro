# ADR 0006 — Defer OpenSearch if needed

## Status

Accepted.

## Decision

P0 search may be Postgres `tsvector` + `pg_trgm`. Add OpenSearch when hybrid/RAG retrieval quality requires it (Phase 8). Architecture still lists OpenSearch as the scale search engine.

## Rationale

Risk R13. Don’t block the learning loop on another cluster.
