-- Typed edges between learning units (08-KNOWLEDGE_GRAPH).
--
-- Containment is not stored here: a unit's parent plus unit_closure already answers
-- "what is this part of", and duplicating it as PART_OF edges would give the two
-- representations a chance to disagree.
--
-- There is no concept_closure table either. Prerequisite reach is computed with a
-- recursive CTE over this table, which is honest about the DAG being small and
-- authored. A closure table earns its keep when writes are rare and reads are wide;
-- adding one now would mean maintaining it on every edge change for no measurable gain.

CREATE TABLE concept_edges (
    id UUID PRIMARY KEY,
    from_id UUID NOT NULL REFERENCES learning_units (id) ON DELETE CASCADE,
    to_id UUID NOT NULL REFERENCES learning_units (id) ON DELETE CASCADE,
    edge_type VARCHAR(32) NOT NULL,
    weight NUMERIC(3, 2) NOT NULL DEFAULT 1.00,
    status VARCHAR(16) NOT NULL DEFAULT 'published',
    note TEXT,
    evidence_source_ids UUID[] NOT NULL DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_concept_edges UNIQUE (from_id, to_id, edge_type),
    CONSTRAINT ck_concept_edges_no_self_loop CHECK (from_id <> to_id),
    CONSTRAINT ck_concept_edges_type CHECK (edge_type IN (
        'PREREQUISITE_OF', 'DEPENDS_ON', 'RELATED_TO', 'SPECIALIZATION_OF',
        'USED_BY', 'IMPLEMENTED_BY', 'APPLIED_IN', 'COMMON_MISCONCEPTION_OF',
        'ALTERNATIVE_TO', 'EVOLUTION_OF')),
    CONSTRAINT ck_concept_edges_status CHECK (status IN (
        'draft', 'review', 'validate', 'published', 'deprecated')),
    CONSTRAINT ck_concept_edges_weight CHECK (weight > 0 AND weight <= 1)
);

-- Both directions are traversed: prerequisites walk to_id -> from_id, dependents and
-- the neighbourhood walk from_id -> to_id.
CREATE INDEX idx_concept_edges_from ON concept_edges (from_id, edge_type);
CREATE INDEX idx_concept_edges_to ON concept_edges (to_id, edge_type);
