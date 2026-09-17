-- Learner state: what someone says they want, what they actually did, and what that is
-- worth as evidence (07-MASTERY_AND_ADAPTIVE_LEARNING).
--
-- The split matters. `mastery_events` is append-only history and is never rewritten;
-- `mastery_records` is a projection of it that the read paths use. If the scoring rules
-- change, the projection is recomputed from the events rather than patched, because the
-- events are the only thing that actually happened.

CREATE TABLE learner_profiles (
    user_id UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    target_path_id UUID REFERENCES learning_paths (id) ON DELETE SET NULL,
    hours_per_week SMALLINT,
    -- Lenses the learner prefers (visual, code, math…). A preference, not a capability.
    modalities JSONB NOT NULL DEFAULT '[]',
    -- Self-reported starting point. Deliberately kept out of mastery: saying you know
    -- linear algebra is not evidence that you do.
    experience VARCHAR(32),
    goals TEXT,
    onboarded_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_learner_profiles_hours CHECK (hours_per_week IS NULL OR hours_per_week BETWEEN 1 AND 80),
    CONSTRAINT ck_learner_profiles_experience CHECK (experience IS NULL OR experience IN (
        'none', 'some_programming', 'professional_developer', 'some_ml', 'working_in_ml'))
);

CREATE TABLE mastery_events (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    unit_id UUID NOT NULL REFERENCES learning_units (id) ON DELETE CASCADE,
    kind VARCHAR(48) NOT NULL,
    payload JSONB NOT NULL DEFAULT '{}',
    at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_mastery_events_kind CHECK (kind IN (
        'unit_opened', 'dwell', 'explanation_level', 'self_check_revealed', 'exercise_viewed'))
);

CREATE INDEX idx_mastery_events_user_at ON mastery_events (user_id, at DESC);
CREATE INDEX idx_mastery_events_unit ON mastery_events (user_id, unit_id, at DESC);

CREATE TABLE mastery_records (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    unit_id UUID NOT NULL REFERENCES learning_units (id) ON DELETE CASCADE,
    state VARCHAR(16) NOT NULL DEFAULT 'UNKNOWN',
    -- Internal ranking score. Not shown as the product's headline number.
    score NUMERIC(4, 3) NOT NULL DEFAULT 0,
    -- The four evidence chips. Each is empty/partial/solid with the date it last moved,
    -- so the UI can show what the claim rests on instead of one opaque percentage.
    read_state VARCHAR(8) NOT NULL DEFAULT 'empty',
    read_at TIMESTAMPTZ,
    practice_state VARCHAR(8) NOT NULL DEFAULT 'empty',
    practice_at TIMESTAMPTZ,
    assess_state VARCHAR(8) NOT NULL DEFAULT 'empty',
    assess_at TIMESTAMPTZ,
    apply_state VARCHAR(8) NOT NULL DEFAULT 'empty',
    apply_at TIMESTAMPTZ,
    -- Inputs the reading rules need, carried on the projection so a read path never has to
    -- replay the whole event log.
    dwell_seconds INT NOT NULL DEFAULT 0,
    levels_seen TEXT[] NOT NULL DEFAULT '{}',
    opened_count INT NOT NULL DEFAULT 0,
    first_opened_at TIMESTAMPTZ,
    last_event_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_mastery_records UNIQUE (user_id, unit_id),
    CONSTRAINT ck_mastery_records_state CHECK (state IN (
        'UNKNOWN', 'INTRODUCED', 'FAMILIAR', 'UNDERSTOOD', 'PRACTICED', 'PROFICIENT', 'MASTERED')),
    CONSTRAINT ck_mastery_records_read CHECK (read_state IN ('empty', 'partial', 'solid')),
    CONSTRAINT ck_mastery_records_practice CHECK (practice_state IN ('empty', 'partial', 'solid')),
    CONSTRAINT ck_mastery_records_assess CHECK (assess_state IN ('empty', 'partial', 'solid')),
    CONSTRAINT ck_mastery_records_apply CHECK (apply_state IN ('empty', 'partial', 'solid')),
    CONSTRAINT ck_mastery_records_score CHECK (score >= 0 AND score <= 1)
);

CREATE INDEX idx_mastery_records_user ON mastery_records (user_id);
CREATE INDEX idx_mastery_records_unit ON mastery_records (unit_id);

CREATE TABLE learning_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    unit_id UUID NOT NULL REFERENCES learning_units (id) ON DELETE CASCADE,
    mode VARCHAR(16) NOT NULL DEFAULT 'read',
    started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ended_at TIMESTAMPTZ,
    -- Counted from heartbeats the client only sends while the tab is visible, and clamped
    -- server-side, so an abandoned tab cannot accumulate a day of "reading".
    dwell_seconds INT NOT NULL DEFAULT 0,
    CONSTRAINT ck_learning_sessions_mode CHECK (mode IN ('read', 'practice', 'assess', 'apply'))
);

CREATE INDEX idx_learning_sessions_open ON learning_sessions (user_id, unit_id, ended_at);

CREATE TABLE study_plans (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    version INT NOT NULL,
    horizon VARCHAR(16) NOT NULL DEFAULT 'weekly',
    -- Why this plan exists, in words. Every mutation has to be explainable to the learner.
    reason TEXT NOT NULL,
    body JSONB NOT NULL DEFAULT '{}',
    generated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_study_plans_version UNIQUE (user_id, version),
    CONSTRAINT ck_study_plans_horizon CHECK (horizon IN ('daily', 'weekly', 'monthly', 'long_term'))
);

CREATE TABLE plan_items (
    id UUID PRIMARY KEY,
    plan_id UUID NOT NULL REFERENCES study_plans (id) ON DELETE CASCADE,
    unit_id UUID REFERENCES learning_units (id) ON DELETE CASCADE,
    kind VARCHAR(16) NOT NULL,
    due DATE,
    sort INT NOT NULL DEFAULT 0,
    reason TEXT,
    CONSTRAINT ck_plan_items_kind CHECK (kind IN ('learn', 'review', 'project', 'update'))
);

CREATE INDEX idx_plan_items_plan ON plan_items (plan_id, sort);

CREATE TABLE notes (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    -- A note can outlive the unit that prompted it, hence nullable rather than cascading.
    unit_id UUID REFERENCES learning_units (id) ON DELETE SET NULL,
    body TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_notes_body_not_blank CHECK (length(btrim(body)) > 0)
);

CREATE INDEX idx_notes_user ON notes (user_id, created_at DESC);
CREATE INDEX idx_notes_unit ON notes (user_id, unit_id, created_at DESC);
