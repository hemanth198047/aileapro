-- aileapro Phase 15: continuous intelligence — updates, proposals, link checks.
--
-- Learners only see published updates with verified_at. Proposals stay pending until
-- an admin approves or rejects. Never claim "fully up to date."

CREATE TABLE ai_updates (
    id UUID PRIMARY KEY,
    slug VARCHAR(128) NOT NULL UNIQUE,
    title VARCHAR(512) NOT NULL,
    summary TEXT NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    lifecycle VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    confidence NUMERIC(4, 3) NOT NULL DEFAULT 0.500,
    verified_at TIMESTAMPTZ,
    verified_source_count INT NOT NULL DEFAULT 0,
    currentness_note TEXT NOT NULL,
    created_by UUID REFERENCES users (id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_ai_updates_lifecycle CHECK (
        lifecycle IN ('experimental', 'emerging', 'established', 'deprecated')),
    CONSTRAINT ck_ai_updates_status CHECK (
        status IN ('draft', 'published', 'withdrawn')),
    CONSTRAINT ck_ai_updates_confidence CHECK (confidence >= 0 AND confidence <= 1),
    CONSTRAINT ck_ai_updates_sources CHECK (verified_source_count >= 0)
);

CREATE INDEX idx_ai_updates_feed ON ai_updates (status, occurred_at DESC);

CREATE TABLE ai_update_sources (
    update_id UUID NOT NULL REFERENCES ai_updates (id) ON DELETE CASCADE,
    source_id UUID NOT NULL REFERENCES sources (id) ON DELETE CASCADE,
    PRIMARY KEY (update_id, source_id)
);

CREATE TABLE curriculum_change_proposals (
    id UUID PRIMARY KEY,
    update_id UUID NOT NULL REFERENCES ai_updates (id) ON DELETE CASCADE,
    unit_id UUID REFERENCES learning_units (id) ON DELETE SET NULL,
    title VARCHAR(512) NOT NULL,
    diff_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    impact_score NUMERIC(4, 3) NOT NULL,
    impact_reasons JSONB NOT NULL DEFAULT '[]'::jsonb,
    status VARCHAR(32) NOT NULL,
    reject_reason TEXT,
    reviewed_by UUID REFERENCES users (id) ON DELETE SET NULL,
    reviewed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_proposals_status CHECK (
        status IN ('pending', 'approved', 'rejected')),
    CONSTRAINT ck_proposals_impact CHECK (impact_score >= 0 AND impact_score <= 1)
);

CREATE INDEX idx_proposals_status ON curriculum_change_proposals (status, created_at DESC);

CREATE TABLE source_link_checks (
    id UUID PRIMARY KEY,
    source_id UUID NOT NULL REFERENCES sources (id) ON DELETE CASCADE,
    checked_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    http_status INT,
    ok BOOLEAN NOT NULL,
    error TEXT,
    duration_ms INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_source_link_checks_source ON source_link_checks (source_id, checked_at DESC);
