-- Assessment, diagnostic, and spaced review (02-PRODUCT_SPEC ASM-*, 07-MASTERY_AND_ADAPTIVE_LEARNING).
--
-- This is the phase where evidence stops being "you read it". Three things follow from that,
-- and they shape every table below.
--
-- 1. Items are authored content and belong to a unit, so `question_bank` looks like the rest
--    of the curriculum: slug, status, version, owned by the seed files.
-- 2. What an answer is *worth* depends on the assessment it was answered in, not on the item.
--    The same question can be practice on Monday and part of a quiz on Friday, which is why
--    the chip credited comes from `assessments.kind` and not from a column on the item.
-- 3. History is append-only. `attempt_answers` and `review_answers` are what happened;
--    `mastery_records` and `review_schedules` are projections that can be rebuilt.

-- ---------------------------------------------------------------------------
-- Authored items
-- ---------------------------------------------------------------------------

CREATE TABLE question_bank (
    id UUID PRIMARY KEY,
    -- Every item belongs to exactly one unit. An item nobody can trace to a page is an item
    -- nobody can revise, and the report has to be able to say "go back to this unit".
    unit_id UUID NOT NULL REFERENCES learning_units (id) ON DELETE CASCADE,
    slug VARCHAR(160) NOT NULL,
    item_type VARCHAR(24) NOT NULL,
    -- 1 easy recall … 5 transfer. Used to weight accuracy, so a right answer on a hard item
    -- counts for more than a right answer on a definition.
    difficulty SMALLINT NOT NULL DEFAULT 2,
    -- Bloom-ish dimension (ASM-02). Tagged now because the report groups by it; adaptive
    -- selection uses it in P1.
    bloom VARCHAR(16) NOT NULL DEFAULT 'understand',
    stem TEXT NOT NULL,
    -- Choices, answer key, accepted strings, rubric. Shape depends on item_type and is
    -- validated by the seeder rather than by the database, so the error can name the file.
    data JSONB NOT NULL,
    -- Teaching shown after the answer, whether right or wrong. Correctness alone teaches
    -- nothing, which is the whole complaint the product exists to fix.
    explanation TEXT NOT NULL,
    -- Codes this item can raise when answered wrongly. Checked against misconception_catalog
    -- by the seeder.
    misconception_codes TEXT[] NOT NULL DEFAULT '{}',
    -- Time the author expects, used to size a quiz rather than to enforce anything.
    estimate_seconds SMALLINT,
    status VARCHAR(16) NOT NULL DEFAULT 'draft',
    version VARCHAR(16) NOT NULL DEFAULT '1.0.0',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_question_bank_slug UNIQUE (slug),
    CONSTRAINT ck_question_bank_type CHECK (item_type IN (
        'mcq', 'multi_select', 'true_false', 'short_text', 'coding_stub')),
    CONSTRAINT ck_question_bank_difficulty CHECK (difficulty BETWEEN 1 AND 5),
    CONSTRAINT ck_question_bank_bloom CHECK (bloom IN (
        'remember', 'understand', 'apply', 'analyze', 'evaluate', 'create')),
    CONSTRAINT ck_question_bank_status CHECK (status IN ('draft', 'review', 'published', 'archived')),
    CONSTRAINT ck_question_bank_stem_not_blank CHECK (length(btrim(stem)) > 0),
    CONSTRAINT ck_question_bank_explanation_not_blank CHECK (length(btrim(explanation)) > 0)
);

CREATE INDEX idx_question_bank_unit ON question_bank (unit_id, status);
CREATE INDEX idx_question_bank_difficulty ON question_bank (status, difficulty);

-- The named misconception catalog. Populated from the `misconception` blocks the units
-- already carry, so a code always has a page that explains it and there is exactly one
-- place where the claim is authored.
CREATE TABLE misconception_catalog (
    code VARCHAR(96) PRIMARY KEY,
    unit_id UUID NOT NULL REFERENCES learning_units (id) ON DELETE CASCADE,
    claim TEXT NOT NULL,
    -- The block a learner is sent back to before a flag can be cleared: "reteach, not cram".
    reteach_block_id UUID REFERENCES unit_blocks (id) ON DELETE SET NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_misconception_catalog_unit ON misconception_catalog (unit_id);

-- ---------------------------------------------------------------------------
-- Assessments: a named, authored selection of items
-- ---------------------------------------------------------------------------

CREATE TABLE assessments (
    id UUID PRIMARY KEY,
    slug VARCHAR(160) NOT NULL,
    title VARCHAR(200) NOT NULL,
    kind VARCHAR(16) NOT NULL,
    -- A quiz or practice set belongs to a unit; the diagnostic spans the curriculum and
    -- leaves this null.
    unit_id UUID REFERENCES learning_units (id) ON DELETE CASCADE,
    description TEXT,
    -- Selection and stop rules for assessments whose items are chosen per attempt:
    -- max_items, per-domain targets, difficulty ladder. Fixed assessments leave it empty
    -- and list their items in assessment_items instead.
    blueprint JSONB NOT NULL DEFAULT '{}',
    time_limit_seconds INT,
    -- Fraction of points needed for the Assess chip to count this as a pass. Authored per
    -- assessment because a five-item practice set and a chapter exam are not the same bar.
    pass_mark NUMERIC(3, 2) NOT NULL DEFAULT 0.70,
    status VARCHAR(16) NOT NULL DEFAULT 'draft',
    version VARCHAR(16) NOT NULL DEFAULT '1.0.0',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_assessments_slug UNIQUE (slug),
    CONSTRAINT ck_assessments_kind CHECK (kind IN ('practice', 'quiz', 'exam', 'diagnostic')),
    CONSTRAINT ck_assessments_status CHECK (status IN ('draft', 'review', 'published', 'archived')),
    CONSTRAINT ck_assessments_pass_mark CHECK (pass_mark > 0 AND pass_mark <= 1),
    -- Everything except the diagnostic is about one unit, and the diagnostic is about none.
    CONSTRAINT ck_assessments_unit_scope CHECK (
        (kind = 'diagnostic' AND unit_id IS NULL) OR (kind <> 'diagnostic' AND unit_id IS NOT NULL))
);

CREATE INDEX idx_assessments_unit ON assessments (unit_id, kind, status);

CREATE TABLE assessment_items (
    assessment_id UUID NOT NULL REFERENCES assessments (id) ON DELETE CASCADE,
    question_id UUID NOT NULL REFERENCES question_bank (id) ON DELETE CASCADE,
    sort INT NOT NULL DEFAULT 0,
    points NUMERIC(4, 2) NOT NULL DEFAULT 1,
    PRIMARY KEY (assessment_id, question_id),
    CONSTRAINT ck_assessment_items_points CHECK (points > 0)
);

CREATE INDEX idx_assessment_items_sort ON assessment_items (assessment_id, sort);

-- ---------------------------------------------------------------------------
-- Attempts: what a learner actually did
-- ---------------------------------------------------------------------------

CREATE TABLE attempts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    assessment_id UUID NOT NULL REFERENCES assessments (id) ON DELETE CASCADE,
    -- Copied from the assessment at start. An attempt is history: if the assessment is later
    -- rescoped or retired, what this attempt was must not change underneath it.
    kind VARCHAR(16) NOT NULL,
    unit_id UUID REFERENCES learning_units (id) ON DELETE SET NULL,
    assessment_version VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'in_progress',
    timed BOOLEAN NOT NULL DEFAULT false,
    time_limit_seconds INT,
    started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    submitted_at TIMESTAMPTZ,
    -- Points, not a percentage: the percentage is derived so it cannot drift from the parts.
    score NUMERIC(6, 2),
    max_score NUMERIC(6, 2),
    passed BOOLEAN,
    -- Why the attempt stopped: 'completed', 'item_budget_reached', 'all_domains_settled',
    -- or 'time_expired'. Worth recording because an adaptive attempt that ends early is not
    -- an abandoned one, and the report should not have to guess which it was.
    stop_reason VARCHAR(24),
    CONSTRAINT ck_attempts_status CHECK (status IN ('in_progress', 'submitted', 'abandoned')),
    CONSTRAINT ck_attempts_kind CHECK (kind IN ('practice', 'quiz', 'exam', 'diagnostic')),
    -- A submitted attempt has been scored; an open one has not. Nothing in between.
    CONSTRAINT ck_attempts_submitted CHECK (
        (status = 'submitted' AND submitted_at IS NOT NULL AND score IS NOT NULL AND max_score IS NOT NULL)
        OR (status <> 'submitted' AND score IS NULL))
);

CREATE INDEX idx_attempts_user ON attempts (user_id, started_at DESC);
CREATE INDEX idx_attempts_user_unit ON attempts (user_id, unit_id, status);
CREATE INDEX idx_attempts_open ON attempts (user_id, assessment_id, status);

-- One row per item served, created when it is served and filled in when it is answered.
-- Serving and answering are separate facts: an item shown and skipped is not the same as an
-- item never shown, and an adaptive attempt needs to know what it has already asked.
CREATE TABLE attempt_answers (
    id UUID PRIMARY KEY,
    attempt_id UUID NOT NULL REFERENCES attempts (id) ON DELETE CASCADE,
    question_id UUID NOT NULL REFERENCES question_bank (id) ON DELETE CASCADE,
    sort INT NOT NULL DEFAULT 0,
    points NUMERIC(4, 2) NOT NULL DEFAULT 1,
    -- Exactly what the learner submitted, in the item's own shape. Kept verbatim so a
    -- re-scored item can be re-marked from the original response.
    response JSONB,
    -- Null means the item could not be marked automatically, which currently only happens to
    -- coding items: they are compared as text, and text comparison cannot tell a different
    -- correct solution from a wrong one. Calling that "wrong" would be a lie the learner
    -- cannot argue with, so it is recorded as unmarked and excluded from the score.
    correct BOOLEAN,
    -- Partial credit is possible on multi-select, so this is not just 0 or points.
    score NUMERIC(4, 2),
    -- Feedback as shown, frozen with the answer: if the explanation is later rewritten, the
    -- report still says what the learner was actually told.
    feedback TEXT,
    misconception_codes TEXT[] NOT NULL DEFAULT '{}',
    served_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    answered_at TIMESTAMPTZ,
    -- Seconds between serving and answering, used for the frustration signals in 07.
    elapsed_seconds INT,
    -- True when the item was marked as text rather than run, so the UI can say so.
    unverified BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT uq_attempt_answers UNIQUE (attempt_id, question_id),
    CONSTRAINT ck_attempt_answers_scored CHECK (
        (answered_at IS NULL AND score IS NULL AND correct IS NULL)
        OR (answered_at IS NOT NULL AND score IS NOT NULL)),
    -- An unmarked answer scores nothing and claims nothing.
    CONSTRAINT ck_attempt_answers_unmarked CHECK (
        correct IS NOT NULL OR score IS NULL OR score = 0)
);

CREATE INDEX idx_attempt_answers_attempt ON attempt_answers (attempt_id, sort);
CREATE INDEX idx_attempt_answers_question ON attempt_answers (question_id);

-- The report is a generated artifact, stored so that reopening it a month later shows what
-- the learner was told at the time rather than what today's rules would say.
--
-- ASM-06 is a hard requirement: never score-only. The NOT NULL columns below are the
-- enforcement — a report cannot be written without the narrative sections.
CREATE TABLE exam_reports (
    attempt_id UUID PRIMARY KEY REFERENCES attempts (id) ON DELETE CASCADE,
    template_version VARCHAR(16) NOT NULL DEFAULT '1.0.0',
    -- One paragraph a human wrote the shape of, filled with this attempt's facts.
    summary TEXT NOT NULL,
    readiness VARCHAR(16) NOT NULL,
    readiness_reason TEXT NOT NULL,
    -- [{unitId, unitSlug, unitTitle, detail}] — what held up, with the evidence for saying so.
    strengths JSONB NOT NULL DEFAULT '[]',
    weaknesses JSONB NOT NULL DEFAULT '[]',
    -- Codes raised by this attempt, each with the unit that explains it.
    misconceptions JSONB NOT NULL DEFAULT '[]',
    -- Prerequisites of the assessed units that the learner has no evidence on. A wrong answer
    -- is often a gap one level down, and saying so is more useful than "revise this unit".
    missing_prereqs JSONB NOT NULL DEFAULT '[]',
    -- What to re-read, in order, with a reason per entry.
    revision JSONB NOT NULL DEFAULT '[]',
    -- The same shape the recommendation engine uses elsewhere: verb, object, minutes, reason.
    next_steps JSONB NOT NULL DEFAULT '[]',
    generated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_exam_reports_readiness CHECK (readiness IN ('not_ready', 'borderline', 'ready', 'unscored')),
    CONSTRAINT ck_exam_reports_summary_not_blank CHECK (length(btrim(summary)) > 0),
    CONSTRAINT ck_exam_reports_reason_not_blank CHECK (length(btrim(readiness_reason)) > 0)
);

-- ---------------------------------------------------------------------------
-- Misconceptions held open against a learner
-- ---------------------------------------------------------------------------

CREATE TABLE misconception_flags (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    code VARCHAR(96) NOT NULL REFERENCES misconception_catalog (code) ON DELETE CASCADE,
    unit_id UUID NOT NULL REFERENCES learning_units (id) ON DELETE CASCADE,
    open BOOLEAN NOT NULL DEFAULT true,
    raised_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- Kept when cleared rather than deleted: that the learner once held this belief is
    -- useful, and a flag that vanishes cannot be audited.
    cleared_at TIMESTAMPTZ,
    times_raised INT NOT NULL DEFAULT 1,
    -- Where it came from: an item they got wrong, or later a tutor turn.
    source VARCHAR(16) NOT NULL DEFAULT 'item',
    last_attempt_id UUID REFERENCES attempts (id) ON DELETE SET NULL,
    CONSTRAINT uq_misconception_flags UNIQUE (user_id, code),
    CONSTRAINT ck_misconception_flags_source CHECK (source IN ('item', 'tutor', 'review')),
    CONSTRAINT ck_misconception_flags_cleared CHECK (
        (open AND cleared_at IS NULL) OR (NOT open AND cleared_at IS NOT NULL))
);

CREATE INDEX idx_misconception_flags_open ON misconception_flags (user_id, open);

-- ---------------------------------------------------------------------------
-- Spaced repetition
-- ---------------------------------------------------------------------------

CREATE TABLE flashcards (
    id UUID PRIMARY KEY,
    unit_id UUID NOT NULL REFERENCES learning_units (id) ON DELETE CASCADE,
    -- Null for an authored "golden" card, which every learner shares. Set when a card was
    -- generated for one person, which is how missed items become cards.
    user_id UUID REFERENCES users (id) ON DELETE CASCADE,
    slug VARCHAR(160),
    front TEXT NOT NULL,
    back TEXT NOT NULL,
    origin VARCHAR(16) NOT NULL DEFAULT 'author',
    -- A locked card is the author's, and generation may not replace or retire it.
    locked BOOLEAN NOT NULL DEFAULT false,
    -- The item that produced this card, when it came from a wrong answer.
    question_id UUID REFERENCES question_bank (id) ON DELETE SET NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'published',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_flashcards_slug UNIQUE (slug),
    CONSTRAINT ck_flashcards_origin CHECK (origin IN ('author', 'ai', 'missed_item')),
    CONSTRAINT ck_flashcards_status CHECK (status IN ('draft', 'published', 'archived')),
    -- An authored card is shared and therefore has no owner; a generated one must have one.
    CONSTRAINT ck_flashcards_ownership CHECK (
        (origin = 'author' AND user_id IS NULL) OR (origin <> 'author' AND user_id IS NOT NULL)),
    CONSTRAINT ck_flashcards_front_not_blank CHECK (length(btrim(front)) > 0),
    CONSTRAINT ck_flashcards_back_not_blank CHECK (length(btrim(back)) > 0)
);

CREATE INDEX idx_flashcards_unit ON flashcards (unit_id, status);
CREATE INDEX idx_flashcards_user ON flashcards (user_id, status);

-- One schedule per learner per card: the SM-2 state, and nothing else.
CREATE TABLE review_schedules (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    card_id UUID NOT NULL REFERENCES flashcards (id) ON DELETE CASCADE,
    unit_id UUID NOT NULL REFERENCES learning_units (id) ON DELETE CASCADE,
    -- A date, not a timestamp: reviews are due on a day, and minute-precision scheduling
    -- would invite a queue that nags.
    due DATE NOT NULL,
    ease NUMERIC(4, 2) NOT NULL DEFAULT 2.50,
    interval_days INT NOT NULL DEFAULT 0,
    reps INT NOT NULL DEFAULT 0,
    lapses INT NOT NULL DEFAULT 0,
    -- A card that keeps failing is flagged for reteaching instead of being shown forever.
    leech BOOLEAN NOT NULL DEFAULT false,
    last_reviewed_at TIMESTAMPTZ,
    last_grade SMALLINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_review_schedules UNIQUE (user_id, card_id),
    CONSTRAINT ck_review_schedules_ease CHECK (ease >= 1.30 AND ease <= 3.00),
    CONSTRAINT ck_review_schedules_interval CHECK (interval_days >= 0),
    CONSTRAINT ck_review_schedules_grade CHECK (last_grade IS NULL OR last_grade BETWEEN 0 AND 5)
);

CREATE INDEX idx_review_schedules_due ON review_schedules (user_id, due);
CREATE INDEX idx_review_schedules_unit ON review_schedules (user_id, unit_id);

-- Append-only review history. The schedule is a projection of this, and retention (w3 in the
-- composite score) is measured from it rather than from the current interval.
CREATE TABLE review_answers (
    id UUID PRIMARY KEY,
    schedule_id UUID NOT NULL REFERENCES review_schedules (id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    card_id UUID NOT NULL REFERENCES flashcards (id) ON DELETE CASCADE,
    unit_id UUID NOT NULL REFERENCES learning_units (id) ON DELETE CASCADE,
    grade SMALLINT NOT NULL,
    interval_before INT NOT NULL,
    interval_after INT NOT NULL,
    ease_after NUMERIC(4, 2) NOT NULL,
    -- True when the card was answered on or after its due date, which is what makes a
    -- success a *spaced* success rather than a re-read.
    was_due BOOLEAN NOT NULL,
    at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_review_answers_grade CHECK (grade BETWEEN 0 AND 5)
);

CREATE INDEX idx_review_answers_user ON review_answers (user_id, at DESC);
CREATE INDEX idx_review_answers_unit ON review_answers (user_id, unit_id, at DESC);

-- ---------------------------------------------------------------------------
-- Graded evidence on the mastery projection
-- ---------------------------------------------------------------------------

-- Reading needed dwell and levels on the projection; grading needs counts and accuracy. Same
-- reasoning as V4: the read paths must never have to replay the event log.
ALTER TABLE mastery_records
    ADD COLUMN practice_items_correct INT NOT NULL DEFAULT 0,
    ADD COLUMN practice_items_answered INT NOT NULL DEFAULT 0,
    ADD COLUMN assess_items_correct INT NOT NULL DEFAULT 0,
    ADD COLUMN assess_items_answered INT NOT NULL DEFAULT 0,
    -- Best submitted quiz fraction on this unit, for the Assess chip and the report.
    ADD COLUMN assess_best_fraction NUMERIC(4, 3),
    ADD COLUMN assessments_passed INT NOT NULL DEFAULT 0,
    -- Correct answers on items above recall level. This is the UNDERSTOOD gate, and it is a
    -- column rather than a derivation because the rules read it on every graded answer.
    ADD COLUMN understand_items_correct INT NOT NULL DEFAULT 0,
    -- Exponential moving accuracy, difficulty-weighted (07: the P0 stand-in for IRT). Kept
    -- separate from `score` because one is a signal and the other is the composite.
    ADD COLUMN accuracy_ema NUMERIC(4, 3),
    -- Spaced recall: successes on cards answered on or after their due date.
    ADD COLUMN reviews_due_passed INT NOT NULL DEFAULT 0,
    ADD COLUMN reviews_due_failed INT NOT NULL DEFAULT 0,
    -- Distinct days with graded evidence, which is how consistency (w2) is measured without
    -- storing every timestamp on the projection.
    ADD COLUMN graded_days INT NOT NULL DEFAULT 0,
    ADD COLUMN last_graded_on DATE,
    ADD COLUMN open_misconceptions INT NOT NULL DEFAULT 0,
    ADD COLUMN last_graded_at TIMESTAMPTZ,
    ADD CONSTRAINT ck_mastery_records_accuracy CHECK (
        accuracy_ema IS NULL OR (accuracy_ema >= 0 AND accuracy_ema <= 1)),
    ADD CONSTRAINT ck_mastery_records_best_fraction CHECK (
        assess_best_fraction IS NULL OR (assess_best_fraction >= 0 AND assess_best_fraction <= 1));

-- New event kinds. The check constraint is rewritten rather than relaxed, so the set of
-- things that can be recorded stays enumerable and reviewable.
ALTER TABLE mastery_events DROP CONSTRAINT ck_mastery_events_kind;
ALTER TABLE mastery_events ADD CONSTRAINT ck_mastery_events_kind CHECK (kind IN (
    'unit_opened', 'dwell', 'explanation_level', 'self_check_revealed', 'exercise_viewed',
    'item_answered', 'attempt_submitted', 'review_answered', 'misconception_raised',
    'misconception_cleared'));

-- Plans gain a source: version 1 comes from the diagnostic, later versions from a goal
-- change or repeated failure, and the learner is entitled to know which.
ALTER TABLE study_plans
    ADD COLUMN source VARCHAR(24) NOT NULL DEFAULT 'diagnostic',
    ADD COLUMN attempt_id UUID REFERENCES attempts (id) ON DELETE SET NULL,
    ADD CONSTRAINT ck_study_plans_source CHECK (source IN (
        'diagnostic', 'goal_change', 'weekly_recalc', 'repeated_failure', 'curriculum_change'));

-- A plan item can point at an assessment or a card queue, not only at a unit to read.
ALTER TABLE plan_items
    ADD COLUMN assessment_id UUID REFERENCES assessments (id) ON DELETE SET NULL;

-- 'skip' is a decision the plan made, not an absent line: the diagnostic thinks this unit is
-- already held, and saying so with the evidence beats omitting it and leaving the learner to
-- wonder whether the plan forgot about it.
ALTER TABLE plan_items DROP CONSTRAINT ck_plan_items_kind;
ALTER TABLE plan_items ADD CONSTRAINT ck_plan_items_kind CHECK (kind IN (
    'learn', 'practice', 'assess', 'review', 'project', 'update', 'skip'));

-- The diagnostic's output: a prior per unit, stored so the plan can cite it and the learner
-- can see it was a guess rather than a measurement.
CREATE TABLE diagnostic_priors (
    id UUID PRIMARY KEY,
    attempt_id UUID NOT NULL REFERENCES attempts (id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    -- Priors are per domain, not per unit: 25 items cannot say anything trustworthy about
    -- one page, and "Bayesian skill groups, not one global IQ" is the rule in 07.
    domain_unit_id UUID NOT NULL REFERENCES learning_units (id) ON DELETE CASCADE,
    items_served INT NOT NULL DEFAULT 0,
    items_correct INT NOT NULL DEFAULT 0,
    -- 0–1 estimate with the width of the guess beside it, so thin evidence is visible as
    -- thin evidence instead of hiding behind a single number.
    estimate NUMERIC(4, 3) NOT NULL,
    confidence VARCHAR(8) NOT NULL,
    at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_diagnostic_priors UNIQUE (attempt_id, domain_unit_id),
    CONSTRAINT ck_diagnostic_priors_estimate CHECK (estimate >= 0 AND estimate <= 1),
    CONSTRAINT ck_diagnostic_priors_confidence CHECK (confidence IN ('low', 'medium', 'high'))
);

CREATE INDEX idx_diagnostic_priors_user ON diagnostic_priors (user_id, at DESC);
