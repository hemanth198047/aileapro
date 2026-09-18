-- aileapro Phase 13: content studio publish snapshots for audited rollback.
--
-- Learners never read this table. Admins use it to restore a prior published
-- version after a bad publish. Draft units remain invisible via ContentStatus.

CREATE TABLE curriculum_changes (
    id UUID PRIMARY KEY,
    unit_id UUID NOT NULL REFERENCES learning_units (id) ON DELETE CASCADE,
    actor_id UUID REFERENCES users (id) ON DELETE SET NULL,
    action VARCHAR(32) NOT NULL,
    from_version VARCHAR(32),
    to_version VARCHAR(32) NOT NULL,
    from_status VARCHAR(24),
    to_status VARCHAR(24) NOT NULL,
    reason TEXT,
    snapshot JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_curriculum_changes_action CHECK (action IN ('publish', 'rollback', 'unpublish')),
    CONSTRAINT ck_curriculum_changes_status CHECK (
        to_status IN ('draft', 'review', 'validate', 'published', 'deprecated'))
);

CREATE INDEX idx_curriculum_changes_unit ON curriculum_changes (unit_id, created_at DESC);
CREATE INDEX idx_audit_logs_action ON audit_logs (action, at DESC);
