-- aileapro Phase 3 curriculum engine.
--
-- Enumerated values are VARCHAR + CHECK rather than Postgres ENUM types: the set of
-- unit kinds and block types is expected to grow, and adding a value to a CHECK is a
-- plain migration whereas ALTER TYPE is not transactional-friendly. JPA maps these
-- with @Enumerated(STRING) either way.
--
-- Depth is not constrained anywhere: per 06-CURRICULUM_MODEL the taxonomy
-- (domain -> path -> module -> ... -> knowledge unit) is a recommendation, not a schema rule.

CREATE TABLE learning_units (
    id UUID PRIMARY KEY,
    slug VARCHAR(160) NOT NULL,
    locale VARCHAR(16) NOT NULL DEFAULT 'en',
    title VARCHAR(300) NOT NULL,
    description TEXT,
    unit_kind VARCHAR(32) NOT NULL,
    parent_id UUID REFERENCES learning_units (id),
    depth INT NOT NULL DEFAULT 0,
    objectives TEXT[] NOT NULL DEFAULT '{}',
    tags TEXT[] NOT NULL DEFAULT '{}',
    difficulty SMALLINT,
    estimate_minutes INT,
    volatility VARCHAR(16) NOT NULL DEFAULT 'LOW',
    knowledge_class VARCHAR(32) NOT NULL DEFAULT 'STABLE',
    status VARCHAR(16) NOT NULL DEFAULT 'draft',
    version VARCHAR(32) NOT NULL DEFAULT '0.1.0',
    effective_at TIMESTAMPTZ,
    superseded_at TIMESTAMPTZ,
    last_reviewed_at TIMESTAMPTZ,
    last_verified_at TIMESTAMPTZ,
    freshness JSONB,
    created_by UUID REFERENCES users (id),
    reviewed_by UUID REFERENCES users (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_learning_units_slug_locale UNIQUE (slug, locale),
    CONSTRAINT ck_learning_units_kind CHECK (unit_kind IN (
        'domain', 'path', 'module', 'chapter', 'section', 'subsection',
        'topic', 'subtopic', 'micro_concept', 'knowledge_unit', 'custom')),
    CONSTRAINT ck_learning_units_status CHECK (status IN (
        'draft', 'review', 'validate', 'published', 'deprecated')),
    CONSTRAINT ck_learning_units_volatility CHECK (volatility IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT ck_learning_units_knowledge_class CHECK (knowledge_class IN (
        'FOUNDATIONAL', 'STABLE', 'VERSION_SPECIFIC', 'CURRENT',
        'EMERGING', 'EXPERIMENTAL', 'DEPRECATED')),
    CONSTRAINT ck_learning_units_difficulty CHECK (difficulty IS NULL OR difficulty BETWEEN 1 AND 5)
);

CREATE INDEX idx_learning_units_status ON learning_units (status);
CREATE INDEX idx_learning_units_parent ON learning_units (parent_id);
CREATE INDEX idx_learning_units_kind ON learning_units (unit_kind);
CREATE INDEX idx_learning_units_tags ON learning_units USING GIN (tags);
CREATE INDEX idx_learning_units_last_verified ON learning_units (last_verified_at);

-- Content is normalized into ordered typed blocks, never one JSON blob per unit.
-- payload holds the block body (markdown, code, diagram spec) after its shape is known.
CREATE TABLE unit_blocks (
    id UUID PRIMARY KEY,
    unit_id UUID NOT NULL REFERENCES learning_units (id) ON DELETE CASCADE,
    sort INT NOT NULL,
    block_type VARCHAR(32) NOT NULL,
    locale VARCHAR(16) NOT NULL DEFAULT 'en',
    payload JSONB NOT NULL,
    version VARCHAR(32) NOT NULL DEFAULT '0.1.0',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_unit_blocks_order UNIQUE (unit_id, locale, sort),
    CONSTRAINT ck_unit_blocks_type CHECK (block_type IN (
        'why', 'objectives', 'prereq_check', 'eli5', 'beginner', 'intermediate', 'advanced',
        'expert', 'research', 'formal_definition', 'math_intuition', 'math_formal', 'visual',
        'code', 'interactive', 'example', 'counterexample', 'application', 'common_mistake',
        'misconception', 'exercise_set', 'quiz_set', 'challenge', 'project_link', 'interview',
        'assessment_criteria', 'mastery_criteria', 'references', 'related'))
);

CREATE INDEX idx_unit_blocks_unit ON unit_blocks (unit_id, sort);

-- Containment closure only in Phase 3. Typed edges (PREREQUISITE_OF and friends)
-- arrive with the knowledge graph in Phase 4.
CREATE TABLE unit_closure (
    ancestor_id UUID NOT NULL REFERENCES learning_units (id) ON DELETE CASCADE,
    descendant_id UUID NOT NULL REFERENCES learning_units (id) ON DELETE CASCADE,
    depth INT NOT NULL,
    PRIMARY KEY (ancestor_id, descendant_id)
);

CREATE INDEX idx_unit_closure_descendant ON unit_closure (descendant_id, depth);

CREATE TABLE learning_paths (
    id UUID PRIMARY KEY,
    slug VARCHAR(160) NOT NULL UNIQUE,
    title VARCHAR(300) NOT NULL,
    description TEXT,
    role_target VARCHAR(160),
    status VARCHAR(16) NOT NULL DEFAULT 'draft',
    version VARCHAR(32) NOT NULL DEFAULT '0.1.0',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_learning_paths_status CHECK (status IN (
        'draft', 'review', 'validate', 'published', 'deprecated'))
);

CREATE TABLE path_waypoints (
    id UUID PRIMARY KEY,
    path_id UUID NOT NULL REFERENCES learning_paths (id) ON DELETE CASCADE,
    unit_id UUID NOT NULL REFERENCES learning_units (id) ON DELETE CASCADE,
    sort INT NOT NULL,
    required_mastery_state VARCHAR(32),
    CONSTRAINT uq_path_waypoints_unit UNIQUE (path_id, unit_id),
    CONSTRAINT uq_path_waypoints_order UNIQUE (path_id, sort)
);

CREATE INDEX idx_path_waypoints_path ON path_waypoints (path_id, sort);

-- Provenance is mandatory for anything we point a learner at (product spec PROV-01).
CREATE TABLE sources (
    id UUID PRIMARY KEY,
    slug VARCHAR(160) NOT NULL UNIQUE,
    title VARCHAR(500) NOT NULL,
    authors TEXT,
    publisher VARCHAR(300),
    url VARCHAR(1000) NOT NULL UNIQUE,
    published_on DATE,
    source_type VARCHAR(32) NOT NULL,
    license VARCHAR(160),
    last_verified_at TIMESTAMPTZ,
    http_status INT,
    hash VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_sources_type CHECK (source_type IN (
        'PRIMARY', 'OFFICIAL_DOC', 'PAPER', 'BOOK', 'COURSE', 'VIDEO', 'BLOG', 'COMMUNITY'))
);

CREATE TABLE learning_resources (
    id UUID PRIMARY KEY,
    source_id UUID NOT NULL REFERENCES sources (id) ON DELETE CASCADE,
    difficulty SMALLINT,
    relevance_default NUMERIC(3, 2) NOT NULL DEFAULT 1.00,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_learning_resources_source UNIQUE (source_id),
    CONSTRAINT ck_learning_resources_difficulty CHECK (difficulty IS NULL OR difficulty BETWEEN 1 AND 5)
);

CREATE TABLE unit_resources (
    id UUID PRIMARY KEY,
    unit_id UUID NOT NULL REFERENCES learning_units (id) ON DELETE CASCADE,
    resource_id UUID NOT NULL REFERENCES learning_resources (id) ON DELETE CASCADE,
    purpose VARCHAR(64),
    relevance NUMERIC(3, 2),
    excerpt TEXT,
    sort INT NOT NULL DEFAULT 0,
    CONSTRAINT uq_unit_resources UNIQUE (unit_id, resource_id)
);

CREATE INDEX idx_unit_resources_unit ON unit_resources (unit_id, sort);
