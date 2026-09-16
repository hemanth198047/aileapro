# 08 — Knowledge graph

## Purpose

Represent AI knowledge as a **typed graph** so the product can find gaps, recommend, connect research, and apply curriculum impact — not just a folder tree.

## Nodes

Primary node: `LearningUnit` / concept. Additional node types (same `nodes` table with `node_kind` or separate tables with shared id space):

- Concept / knowledge unit
- Technology / library / model family
- Person (optional, papers)
- Paper / source
- Skill (career)
- Misconception
- Project
- Assessment blueprint

P0: units + technologies + misconceptions + resources. Papers as resources.

## Edge types (required)

| Type | Semantics | Constraints |
|---|---|---|
| PREREQUISITE_OF | A before B | Acyclic; DAG check on write |
| DEPENDS_ON | Weaker than hard prereq | |
| RELATED_TO | Symmetric optional | |
| PART_OF | Containment | |
| SPECIALIZATION_OF | ISA | |
| GENERALIZATION_OF | Inverse specialization | Store one direction |
| USED_BY | Technique used in system | |
| IMPLEMENTED_BY | Concept → code/pattern | |
| APPLIED_IN | Concept → domain/project | |
| COMMON_MISCONCEPTION_OF | Misconception → concept | |
| ALTERNATIVE_TO | Competing approaches | |
| EVOLUTION_OF | Historical succession | |

Store: `from_id`, `to_id`, `type`, `weight`, `evidence_source_ids[]`, `created_at`, `status`.

## Storage (ADR)

**Postgres adjacency list + `concept_closure` table** for ancestor queries. Recompute closure on edge change (incremental P1).

Why not Neo4j at P0: operational simplicity, transactional unity with mastery. Interface `GraphStore` so Neo4j/AGE can replace later.

## Algorithms (P0)

- Shortest prereq path from mastered set to target.
- Missing prereqs: ancestors with S < τ.
- Related panel: RELATED + APPLIED_IN + ALTERNATIVE limited by k.
- Impact: dependents of a changed node (for CI).

## Visualization

- Cluster by domain.
- Node color = mastery state.
- Edge filter by type.
- Accessibility: adjacent list view.
- Performance: fetch neighborhood (2 hops) not entire 50k graph; server-side simplification.

## Integrity

- Cycle detection on PREREQUISITE_OF.
- No published unit without `PART_OF` domain.
- Orphan detection job.

## Example chain (seed)

Linear Algebra → Vectors → Matrices → Matmul → Neural Nets → Backprop → Attention → Transformers → LLMs → RAG → Agents.

Each link typed and used in Map UI.
