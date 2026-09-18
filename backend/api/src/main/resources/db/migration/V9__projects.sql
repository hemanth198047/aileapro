-- aileapro Phase 10: Project Studio and portfolio.
--
-- Completing a published project is what fills the Apply evidence chip. Outline
-- projects exist as briefs only — they cannot accept submissions until authored.

CREATE TABLE projects (
    id UUID PRIMARY KEY,
    slug VARCHAR(80) NOT NULL,
    unit_id UUID REFERENCES learning_units (id) ON DELETE SET NULL,
    title VARCHAR(200) NOT NULL,
    summary TEXT NOT NULL DEFAULT '',
    objective TEXT NOT NULL DEFAULT '',
    status VARCHAR(24) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    starter_source TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_projects_slug UNIQUE (slug),
    CONSTRAINT ck_projects_status CHECK (status IN ('published', 'outline'))
);

CREATE TABLE project_milestones (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    slug VARCHAR(80) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    criteria TEXT NOT NULL DEFAULT '',
    hint TEXT NOT NULL DEFAULT '',
    sort_order INT NOT NULL DEFAULT 0,
    check_key VARCHAR(64),
    starter_source TEXT,
    CONSTRAINT uq_project_milestones_slug UNIQUE (project_id, slug),
    CONSTRAINT ck_project_milestones_check_key CHECK (
        check_key IS NULL OR check_key IN ('mse', 'fit_line', 'predict_report'))
);

CREATE TABLE project_apply_units (
    project_id UUID NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    unit_id UUID NOT NULL REFERENCES learning_units (id) ON DELETE CASCADE,
    PRIMARY KEY (project_id, unit_id)
);

CREATE TABLE submissions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    project_id UUID NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    milestone_id UUID NOT NULL REFERENCES project_milestones (id) ON DELETE CASCADE,
    source TEXT NOT NULL,
    source_hash VARCHAR(64) NOT NULL,
    lab_run_id UUID REFERENCES lab_runs (id) ON DELETE SET NULL,
    passed BOOLEAN NOT NULL DEFAULT false,
    review TEXT,
    mentor_comment TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_submissions_user_project ON submissions (user_id, project_id, created_at DESC);
CREATE INDEX idx_submissions_milestone ON submissions (user_id, milestone_id, created_at DESC);

CREATE TABLE portfolio_items (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    project_id UUID NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    submission_id UUID REFERENCES submissions (id) ON DELETE SET NULL,
    title VARCHAR(200) NOT NULL,
    summary TEXT NOT NULL DEFAULT '',
    reflection TEXT NOT NULL DEFAULT '',
    visibility VARCHAR(16) NOT NULL DEFAULT 'private',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_portfolio_user_project UNIQUE (user_id, project_id),
    CONSTRAINT ck_portfolio_visibility CHECK (visibility IN ('private', 'public'))
);

CREATE INDEX idx_portfolio_user ON portfolio_items (user_id, created_at DESC);
CREATE INDEX idx_portfolio_public ON portfolio_items (visibility) WHERE visibility = 'public';

-- Completing a project is a production event: it can fill Apply.
ALTER TABLE mastery_events DROP CONSTRAINT ck_mastery_events_kind;
ALTER TABLE mastery_events ADD CONSTRAINT ck_mastery_events_kind CHECK (kind IN (
    'unit_opened', 'dwell', 'explanation_level', 'self_check_revealed', 'exercise_viewed',
    'item_answered', 'attempt_submitted', 'review_answered', 'misconception_raised',
    'misconception_cleared', 'project_completed'));
