# ADR 0007 — LangChain is not the core orchestrator

## Status

Accepted.

## Decision

Explicit provider adapters + our orchestrator. Libraries may be used inside **learner labs** as curriculum subject matter, not as the platform’s control plane.

## Rationale

Avoid hiding cost, prompts, and security behind a moving framework.
