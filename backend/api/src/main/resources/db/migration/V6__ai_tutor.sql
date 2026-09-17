-- The AI tutor (09-AI_ARCHITECTURE, 02-PRODUCT_SPEC TUT-*, 23-COST_MODEL).
--
-- Four commitments shape these tables, and all four are about not being at the mercy of a
-- vendor or of our own future carelessness.
--
-- 1. No feature names a model. Features ask for a *profile* (`teach`, `cheap_chat`, `code`)
--    and the profile decides which model answers, so switching provider is a row change
--    rather than a code change.
-- 2. No prompt is inlined across the repo. `prompts` and `prompt_versions` hold the authored
--    text with its hash, and every logged call records which version produced it. Without
--    that, a regression is untraceable after the next edit.
-- 3. Every call is metered. `ai_usage` is the cost model made real: tokens, latency, and
--    estimated dollars per user and per feature, so budgets are enforced from measurements
--    rather than from hope.
-- 4. A conversation belongs to the learner, and the transcript is data they can read and
--    delete. Messages are append-only, and retrieval references are stored per message so
--    that "what sources did you use?" has an answer which cannot be reconstructed later.

-- ---------------------------------------------------------------------------
-- Providers, models, profiles
-- ---------------------------------------------------------------------------

CREATE TABLE model_providers (
    id UUID PRIMARY KEY,
    -- 'fake' is a first-class provider rather than a hidden fallback: a development answer
    -- must be visibly a development answer, in the database and on the screen.
    slug VARCHAR(48) NOT NULL,
    title VARCHAR(120) NOT NULL,
    -- OpenAI-compatible chat completions is the wire format we speak. Anything that speaks it
    -- (OpenAI, Azure, OpenRouter, vLLM, Ollama) is a base_url and a key away.
    api_kind VARCHAR(32) NOT NULL DEFAULT 'openai_compatible',
    base_url VARCHAR(255),
    -- Which environment variable holds the key. The key itself is never in the database.
    api_key_env VARCHAR(64),
    status VARCHAR(16) NOT NULL DEFAULT 'enabled',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_model_providers_slug UNIQUE (slug),
    CONSTRAINT ck_model_providers_api_kind CHECK (api_kind IN ('openai_compatible', 'fake')),
    CONSTRAINT ck_model_providers_status CHECK (status IN ('enabled', 'disabled'))
);

CREATE TABLE models (
    id UUID PRIMARY KEY,
    provider_id UUID NOT NULL REFERENCES model_providers (id) ON DELETE CASCADE,
    slug VARCHAR(96) NOT NULL,
    -- What to send as the model name on the wire, which is not always what we call it.
    wire_name VARCHAR(120) NOT NULL,
    -- 'chat', 'embed'. Embedding models live here so Phase 8 has somewhere to put them.
    capabilities TEXT[] NOT NULL DEFAULT '{chat}',
    context_tokens INT NOT NULL DEFAULT 8192,
    max_output_tokens INT NOT NULL DEFAULT 1024,
    -- Prices are per million tokens, in USD, as published. Stored rather than hardcoded so a
    -- price change is an operator action and old usage rows keep the cost they were charged.
    price_in_per_mtok NUMERIC(10, 4) NOT NULL DEFAULT 0,
    price_out_per_mtok NUMERIC(10, 4) NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL DEFAULT 'enabled',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_models_slug UNIQUE (slug),
    CONSTRAINT ck_models_status CHECK (status IN ('enabled', 'disabled')),
    CONSTRAINT ck_models_prices CHECK (price_in_per_mtok >= 0 AND price_out_per_mtok >= 0)
);

CREATE INDEX idx_models_provider ON models (provider_id, status);

-- A routing profile: the task class a feature asks for, and the ordered models that can serve
-- it. `sort` is the fallback chain, so a provider outage degrades rather than fails.
CREATE TABLE model_profiles (
    id UUID PRIMARY KEY,
    slug VARCHAR(48) NOT NULL,
    title VARCHAR(120) NOT NULL,
    purpose TEXT NOT NULL,
    -- Temperature, top_p, max output. JSONB because every provider has its own dialect of
    -- "be less random", and that is not worth modelling as columns.
    params JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_model_profiles_slug UNIQUE (slug)
);

CREATE TABLE model_profile_models (
    profile_id UUID NOT NULL REFERENCES model_profiles (id) ON DELETE CASCADE,
    model_id UUID NOT NULL REFERENCES models (id) ON DELETE CASCADE,
    sort INT NOT NULL DEFAULT 0,
    PRIMARY KEY (profile_id, model_id)
);

CREATE INDEX idx_model_profile_models_order ON model_profile_models (profile_id, sort);

-- ---------------------------------------------------------------------------
-- Prompts
-- ---------------------------------------------------------------------------

CREATE TABLE prompts (
    id UUID PRIMARY KEY,
    -- 'tutor.socratic'. Features ask by name and never by id.
    name VARCHAR(96) NOT NULL,
    purpose TEXT NOT NULL,
    owner VARCHAR(96) NOT NULL DEFAULT 'platform',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_prompts_name UNIQUE (name)
);

CREATE TABLE prompt_versions (
    id UUID PRIMARY KEY,
    prompt_id UUID NOT NULL REFERENCES prompts (id) ON DELETE CASCADE,
    -- Monotonic per prompt, and referenced by every usage row, so a bad edit can be traced to
    -- the calls it affected instead of being argued about.
    version INT NOT NULL,
    template TEXT NOT NULL,
    -- SHA-256 of the template, hex. The seeder refuses to change a published version's text:
    -- new text is a new version, because an edited prompt with the same number makes every
    -- historical usage row a lie.
    --
    -- VARCHAR rather than CHAR(64) even though the length is fixed. CHAR pads with spaces and
    -- compares by ignoring them, which is a quietly wrong way to compare a digest.
    hash VARCHAR(64) NOT NULL,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_prompt_versions_number UNIQUE (prompt_id, version),
    CONSTRAINT ck_prompt_versions_template_not_blank CHECK (length(btrim(template)) > 0)
);

CREATE INDEX idx_prompt_versions_prompt ON prompt_versions (prompt_id, version DESC);

-- ---------------------------------------------------------------------------
-- Conversations
-- ---------------------------------------------------------------------------

CREATE TABLE conversations (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    -- A conversation is usually about a unit, and a tutor without that context is a chatbot.
    -- Nullable because the companion also opens where there is no unit to discuss.
    unit_id UUID REFERENCES learning_units (id) ON DELETE SET NULL,
    persona VARCHAR(32) NOT NULL DEFAULT 'patient_teacher',
    title VARCHAR(200),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_message_at TIMESTAMPTZ,
    CONSTRAINT ck_conversations_persona CHECK (persona IN (
        'patient_teacher', 'strict_examiner', 'coding_mentor'))
);

CREATE INDEX idx_conversations_user ON conversations (user_id, last_message_at DESC NULLS LAST);
CREATE INDEX idx_conversations_unit ON conversations (user_id, unit_id);

CREATE TABLE messages (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES conversations (id) ON DELETE CASCADE,
    role VARCHAR(16) NOT NULL,
    content TEXT NOT NULL,
    -- The action the learner pressed, if they pressed one rather than typing: 'simpler',
    -- 'deeper', 'quiz_me'. Kept because "explain this more simply" is a measurable request.
    action VARCHAR(24),
    -- Block ids the answer was grounded in. Empty until retrieval exists in Phase 8, and empty
    -- is the honest value: an answer with no citations must not appear to have any.
    retrieval_ids UUID[] NOT NULL DEFAULT '{}',
    -- Which prompt version produced an assistant message, and which model wrote it. Null on
    -- learner messages.
    prompt_version_id UUID REFERENCES prompt_versions (id) ON DELETE SET NULL,
    model_id UUID REFERENCES models (id) ON DELETE SET NULL,
    -- True when a development implementation answered. Surfaced in the UI, because a fixture
    -- presented as teaching is the worst failure this phase could ship.
    development BOOLEAN NOT NULL DEFAULT false,
    -- Set when the tutor declined: 'exam_answer', 'off_policy'. A refusal is a teaching moment
    -- worth being able to count, not an error.
    refusal_code VARCHAR(32),
    -- Which rung of the reveal ladder an answer was allowed: HINT, GUIDED, FULL. Recorded
    -- because "the tutor gives away too much" is otherwise an argument rather than a query.
    reveal VARCHAR(16),
    sort INT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_messages_role CHECK (role IN ('system', 'user', 'assistant')),
    CONSTRAINT uq_messages_sort UNIQUE (conversation_id, sort)
);

CREATE INDEX idx_messages_conversation ON messages (conversation_id, sort);

-- ---------------------------------------------------------------------------
-- Usage and cost
-- ---------------------------------------------------------------------------

CREATE TABLE ai_usage (
    id UUID PRIMARY KEY,
    -- The request id echoed to the client, so a learner complaint can be tied to the exact
    -- call, its prompt version, and its cost.
    request_id VARCHAR(64),
    user_id UUID REFERENCES users (id) ON DELETE SET NULL,
    -- 'tutor', 'flashcards', 'ingest'. Budgets are per feature as well as per user.
    feature VARCHAR(48) NOT NULL,
    profile_slug VARCHAR(48) NOT NULL,
    provider_slug VARCHAR(48) NOT NULL,
    model_slug VARCHAR(96) NOT NULL,
    prompt_version_id UUID REFERENCES prompt_versions (id) ON DELETE SET NULL,
    conversation_id UUID REFERENCES conversations (id) ON DELETE SET NULL,
    tokens_in INT NOT NULL DEFAULT 0,
    tokens_out INT NOT NULL DEFAULT 0,
    latency_ms INT NOT NULL DEFAULT 0,
    -- Estimated, and named as such. Invoice reconciliation is an operator job, not this row.
    cost_usd NUMERIC(12, 6) NOT NULL DEFAULT 0,
    -- 'ok', 'provider_error', 'refused', 'budget_exceeded'. Failures are metered too: an
    -- outage that costs nothing still needs to be visible.
    outcome VARCHAR(24) NOT NULL DEFAULT 'ok',
    at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_ai_usage_outcome CHECK (outcome IN (
        'ok', 'provider_error', 'refused', 'budget_exceeded', 'cancelled')),
    CONSTRAINT ck_ai_usage_tokens CHECK (tokens_in >= 0 AND tokens_out >= 0)
);

-- The two questions asked of this table: what has this learner spent today, and what is this
-- feature costing us.
CREATE INDEX idx_ai_usage_user ON ai_usage (user_id, at DESC);
CREATE INDEX idx_ai_usage_feature ON ai_usage (feature, at DESC);
