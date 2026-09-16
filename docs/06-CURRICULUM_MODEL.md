# 06 — Curriculum model

## Principle

**Do not hard-code a fixed depth.** The published hierarchy (domain → path → module → … → knowledge unit) is a *recommended taxonomy*, not a schema constraint.

## Node model

`LearningUnit`

| Field | Type | Notes |
|---|---|---|
| id | UUID | Stable |
| slug | string | Unique per locale optional |
| title | string | |
| description | text | |
| unit_kind | enum | `domain`, `path`, `module`, `chapter`, `section`, `subsection`, `topic`, `subtopic`, `micro_concept`, `knowledge_unit`, `custom` |
| parent_id | UUID? | Tree convenience; graph is source of truth for relations |
| depth | int | Cached |
| path_ids[] | UUID | Paths that include this unit |
| objectives[] | text | |
| difficulty | 1–5 | Authoritative after review |
| estimate_minutes | int | |
| tags[] | string | |
| technologies[] | string | Versioned via TechnologyVersion FKs |
| volatility | enum | LOW MEDIUM HIGH |
| knowledge_class | enum | FOUNDATIONAL STABLE VERSION_SPECIFIC CURRENT EMERGING EXPERIMENTAL DEPRECATED |
| status | enum | draft review validate published deprecated |
| version | semver-like | `major.minor.patch` |
| effective_at / superseded_at | timestamptz | |
| last_reviewed_at | timestamptz | |
| last_verified_at | timestamptz | CI engine |
| freshness | jsonb | see 10 |
| locale | string | default `en` |
| created_by / reviewed_by | user id | |

### Content blocks (normalized, not one giant JSON blob)

`UnitBlock`: ordered, typed.

Types: `why`, `objectives`, `prereq_check`, `eli5`, `beginner`, `intermediate`, `advanced`, `expert`, `research`, `formal_definition`, `math_intuition`, `math_formal`, `visual`, `code`, `interactive`, `example`, `counterexample`, `application`, `common_mistake`, `misconception`, `exercise_set`, `quiz_set`, `challenge`, `project_link`, `interview`, `assessment_criteria`, `mastery_criteria`, `references`, `related`.

JSON allowed **inside a block** for diagram specs, not for the whole unit.

### References

`LearningResource` + `UnitResource` join with relevance score, optional excerpt. Provenance required (see product spec PROV-01).

## Hierarchy vs graph

- **Containment** (`PART_OF`): used for TOC and breadcrumbs.
- **Prerequisite** (`PREREQUISITE_OF` / `DEPENDS_ON`): used for gating and “learn next.”
- Other typed edges: see [08-KNOWLEDGE_GRAPH.md](08-KNOWLEDGE_GRAPH.md).

A unit may have multiple parents in the graph (shared math under ML and CV). Breadcrumb picks the path-contextual parent.

## Learning path

`LearningPath`: role, description, admission diagnostic profile, ordered **waypoints** (units or clusters), optional must-master set for “path ready.”

Customization: learner path instance clones waypoint list, then CI/adaptive engine inserts migration lessons and skips.

## Versioning and changelog

Every publish increments version. `CurriculumChange` stores reason, sources, impact score, author, reviewer.

Learners see: “What changed since I learned this?” via diff of blocks + technology versions.

## Scale target (architecture, not fake content)

100+ modules, 1000+ chapters, 5000+ sections, 10k+ topics, 50k+ micro-concepts.

Indexes: slug, gin(tags), parent, status, volatility, last_verified. Partition `unit_blocks` by unit_id hash if needed later. Don’t partition at P0.

## Authoring pipeline

DOMAIN → MODULE → CHAPTER → SECTION → TOPIC → CONCEPT → EXPLANATION → EXERCISES → ASSESSMENT → PROJECT

AI assists drafts; critic + gates; never auto-publish high-impact educational mutations without policy.

## Seed policy

See [24-SEED_CONTENT_PLAN.md](24-SEED_CONTENT_PLAN.md). Real units only. Enough to demonstrate: foundations slice, Python slice, one ML concept fully loaded (all block types), one LLM/RAG concept, one project.

## Domain taxonomy (minimum)

A Computer Fundamentals  
B Programming Fundamentals  
C Python  
D DSA  
E Mathematics for AI  
F Linear Algebra  
G Calculus  
H Probability  
I Statistics  
J Optimization  
K Data Engineering  
L SQL  
M Data Science  
N Machine Learning  
O Classical ML  
P Feature Engineering  
Q Model Evaluation  
R Ensembles  
S Unsupervised  
T Reinforcement Learning  
U Deep Learning  
V Neural Networks  
W CNNs  
X RNNs  
Y Attention  
Z Transformers  
AA NLP  
AB Computer Vision  
AC Speech AI  
AD Generative AI  
AE LLMs  
AF Prompt Engineering  
AG Embeddings  
AH Vector Databases  
AI RAG  
AJ Fine-Tuning  
AK PEFT/LoRA  
AL Multimodal  
AM AI Agents  
AN Tool Calling  
AO Agent Memory  
AP Multi-Agent  
AQ AI Evaluation  
AR MLOps  
AS LLMOps  
AT AI Infrastructure  
AU AI System Design  
AV AI Security  
AW Responsible AI  
AX AI Governance  
AY AI Research  
AZ Frontier AI  
BA AI Product Development  
BB AI Entrepreneurship  
BC AI Architecture  
BD AI Economics  
BE AI Applications  

Each exists as a `domain` unit in seed even if children are sparse.
