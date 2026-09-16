# ADR 0003 — Isolated code executor

## Status

Accepted.

## Decision

Learner code runs only in `apps/executor` (containers with gVisor/Firecracker or E2B). API never `eval`s or `spawn`s user code.

## Rationale

Compromise of a notebook must not yield DB or AI keys.
