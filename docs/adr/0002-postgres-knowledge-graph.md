# ADR 0002 — Knowledge graph in PostgreSQL

## Status

Accepted.

## Decision

Store the graph in Postgres (adjacency + closure). Interface `GraphStore`. Revisit Neo4j/Neptune at >10M edges or heavy graph algorithms.

## Rationale

Single transaction with mastery and curriculum publishes. Lower ops for P0.
