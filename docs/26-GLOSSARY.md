# 26 — Glossary

| Term | Meaning |
|---|---|
| **Knowledge unit** | Atomic learnable node. Leaf of the curriculum tree. Has mastery, freshness, volatility. |
| **Learning unit** | Any node in the flexible hierarchy (domain → … → knowledge unit). |
| **Mastery state** | UNKNOWN, INTRODUCED, FAMILIAR, UNDERSTOOD, PRACTICED, PROFICIENT, MASTERED. Not a raw percent. |
| **Mastery score** | Composite of accuracy, consistency, retention, application, recency, prerequisite health. Used internally; UX shows state + evidence. |
| **Evidence** | Signals that support a mastery state: reads, exercises, assessments, projects, oral, code, spaced recall. |
| **Prerequisite graph** | Directed edges: must-know-before. |
| **Knowledge graph** | Concepts + typed relationships beyond prerequisites. |
| **Volatility** | How fast a topic’s applied layer changes (LOW/MEDIUM/HIGH). |
| **Freshness** | Domain-aware recency/verification metadata. Linear algebra is not “stale” because it is old. |
| **Provenance** | Source metadata required for external claims and ingested resources. |
| **Curriculum impact score** | Internal priority for maintainers, never a learner ranking. |
| **Living curriculum** | Curriculum that evolves under governance; never “finished on publish.” |
| **Migration lesson** | Short update for learners who mastered an older version of a topic. |
| **Human-in-the-loop** | Configurable approval for curriculum mutation. |
| **Provider router** | Chooses model/provider by task class, cost, latency, capability. |
| **Sandbox** | Isolated execution environment for untrusted code. |
| **AILEPRO** | Product name. Written `ailepro` (lowercase) for the repository and packages. |
