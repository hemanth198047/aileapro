# 12 — Data model

PostgreSQL 16. **Flyway** SQL migrations (Java backend). UUIDv7 (time-ordered) for new PKs. `created_at`/`updated_at` everywhere unless append-only. JPA entities map this schema; graph/vector access may use `JdbcClient`.

JSON only for: block payloads, rubric details, router config, freshness metrics, structured agent output **after schema validation**.

## Identity

**users:** id, email unique, email_verified_at, password_hash nullable (OAuth), status, locale, timezone, created_at  
**auth_identities:** user_id, provider, provider_subject  
**sessions:** id, user_id, expires, ip_hash, user_agent_hash  
**roles:** code  
**user_roles:** user_id, role  
**permissions:** code  
**role_permissions**  
**audit_logs:** actor_id, action, resource_type, resource_id, ip, meta jsonb, at  (append-only)

## Curriculum

**learning_units:** see 06  
**unit_blocks:** id, unit_id, sort, type, locale, payload jsonb, version  
**unit_closure:** ancestor_id, descendant_id, depth  
**learning_paths:** id, slug, title, role_target, status  
**path_waypoints:** path_id, unit_id, sort, required_mastery_state  
**concept_edges:** from_id, to_id, type, weight, status  
**technologies:** id, slug, name  
**technology_versions:** technology_id, version, lifecycle, released_at  
**unit_technologies:** unit_id, technology_version_id

## Resources and provenance

**sources:** id, title, authors, publisher, url unique, published_on, source_type, license, last_verified_at, http_status, hash  
**learning_resources:** source_id, difficulty, relevance default  
**unit_resources:** unit_id, resource_id, purpose

source_type enum: PRIMARY, OFFICIAL_DOC, PAPER, BOOK, COURSE, VIDEO, BLOG, COMMUNITY

## Learner

**learner_profiles:** user_id PK, target_path_id, hours_per_week, modalities jsonb, goals text  
**study_plans:** id, user_id, version, horizon, generated_at, reason, body jsonb  
**plan_items:** plan_id, unit_id, due, kind (learn/review/project/update)  
**learning_sessions:** id, user_id, unit_id, started, ended, mode  
**notes:** id, user_id, unit_id nullable, body, links  
**note_links:** from_note, to_note / to_unit  
**achievements / user_achievements**  
**certificates:** user_id, blueprint_id, issued_at, evidence jsonb, curriculum_version

## Mastery

**mastery_records:** user_id, unit_id, state, score, updated_at, unique(user,unit)  
**mastery_events:** id, user_id, unit_id, kind, payload, at  
**misconception_flags:** user_id, code, unit_id, open bool  
**flashcards:** id, user_id, unit_id, front, back, origin (author/ai)  
**review_schedules:** card_id or unit_id, due, ease, interval, reps, lapses

## Assessment

**question_bank:** id, unit_id, type, difficulty, stem, data jsonb, misconception_codes[], status, version  
**assessments:** id, kind (quiz/exam/diagnostic/interview), blueprint jsonb  
**assessment_items:** assessment_id, question_id, sort  
**attempts:** id, user_id, assessment_id, started, submitted, timed  
**attempt_answers:** attempt_id, question_id, response, score, feedback  
**exam_reports:** attempt_id, strengths[], weaknesses[], missing_prereqs[], next_steps, readiness

## AI

**model_providers:** id, name, status  
**models:** id, provider_id, name, capabilities[], pricing  
**model_profiles:** name unique, model_id, params jsonb  
**prompts / prompt_versions**  
**ai_usage:** request_id, user_id, feature, provider, model, tokens_in, tokens_out, latency_ms, cost_usd, prompt_version, at  
**conversations:** id, user_id, unit_id, persona, created  
**messages:** conversation_id, role, content, retrieval_ids[], created  
**ai_caches:** key hash, value, ttl

## Lab and projects

**lab_runs:** id, user_id, language, status, source_hash, stdout, stderr, exit, duration, executor_id  
**projects:** id, slug, unit_id, title, spec jsonb, status  
**project_milestones**  
**submissions:** user_id, project_id, milestone_id, artifact_uri, passed, review  
**portfolio_items:** user_id, submission_id, visibility, reflection

## Intelligence

Tables matching entities in doc 10. Key:

**ai_updates:** id, title, occurred_at, summary, confidence, lifecycle, status  
**ai_update_sources:** update_id, source_id  
**curriculum_change_proposals:** update_id, diff jsonb, impact_score, status  
**curriculum_changes:** published snapshot  
**knowledge_diffs:** unit_id, before, after, change_id  
**resource_health:** source_id, checks jsonb  
**curriculum_health_reports:** at, payload  
**update_subscriptions:** user_id, topic  
**learner_updates:** user_id, update_id, reason  
**update_audit_logs / update_rollbacks**

## Notifications

**notifications:** user_id, type, body, read_at, ref  
**outbox:** for reliable emit

## Indexes (P0)

- mastery_records (user_id), (unit_id)  
- mastery_events (user_id, at desc)  
- concept_edges (from,type), (to,type)  
- review_schedules (user via card, due)  
- learning_units (slug), (status), (parent)  
- sources (url)  
- ai_usage (user, at), (feature, at)

## Tenancy

`org_id` nullable on users/content later. P0 single tenant. Don’t use user_id as the only isolation in queries — always scope.

## Retention

- lab stdout: 90 days default  
- AI transcripts: learner-controlled; default 365  
- audit: 2 years  
- usage: 2 years aggregated
