-- aileapro Phase 16: feature flags for launch hardening.
--
-- lab_enabled / tutor_enabled gate learner-facing AI and sandbox surfaces.
-- ci_agents reserved for continuous-intelligence jobs.

CREATE TABLE feature_flags (
    code VARCHAR(64) PRIMARY KEY,
    enabled BOOLEAN NOT NULL DEFAULT true,
    description TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO feature_flags (code, enabled, description) VALUES
    ('lab_enabled', true, 'Learner Python lab runs'),
    ('tutor_enabled', true, 'AI tutor conversations and turns'),
    ('ci_agents', false, 'Automated continuous-intelligence watchers (manual updates still work)');
