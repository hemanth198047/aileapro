-- aileapro Phase 9: coding lab runs.
--
-- Learner source is stored and shown, but never executed in the API JVM. The
-- executor service is the only process allowed to run it, and it has no database
-- credentials and no AI keys. Rows here are the audit trail of what was asked
-- and what came back.

CREATE TABLE lab_runs (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    language VARCHAR(32) NOT NULL DEFAULT 'python',
    status VARCHAR(24) NOT NULL,
    source TEXT NOT NULL,
    source_hash VARCHAR(64) NOT NULL,
    stdin TEXT,
    stdout TEXT,
    stderr TEXT,
    exit_code INT,
    duration_ms INT,
    timed_out BOOLEAN NOT NULL DEFAULT false,
    cancelled BOOLEAN NOT NULL DEFAULT false,
    executor_kind VARCHAR(32) NOT NULL,
    executor_id VARCHAR(64),
    error_code VARCHAR(64),
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    CONSTRAINT ck_lab_runs_language CHECK (language IN ('python')),
    CONSTRAINT ck_lab_runs_status CHECK (status IN (
        'queued', 'running', 'succeeded', 'failed', 'timed_out', 'cancelled')),
    CONSTRAINT ck_lab_runs_executor_kind CHECK (executor_kind IN ('fake', 'docker', 'http'))
);

CREATE INDEX idx_lab_runs_user_created ON lab_runs (user_id, created_at DESC);
CREATE INDEX idx_lab_runs_user_day ON lab_runs (user_id, created_at);
CREATE INDEX idx_lab_runs_status ON lab_runs (status) WHERE status IN ('queued', 'running');
