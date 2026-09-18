-- aileapro Phase 12: career gaps and interview practice.
--
-- Interview is a text practice mode for the Career space. It does not fill mastery
-- chips — self-reported readiness and interview rehearsal are not demonstrated
-- competence. Gaps come only from mastery_records against the path.

ALTER TABLE assessments DROP CONSTRAINT ck_assessments_kind;
ALTER TABLE assessments ADD CONSTRAINT ck_assessments_kind CHECK (kind IN (
    'practice', 'quiz', 'exam', 'diagnostic', 'interview'));

ALTER TABLE conversations DROP CONSTRAINT ck_conversations_persona;
ALTER TABLE conversations ADD CONSTRAINT ck_conversations_persona CHECK (persona IN (
    'patient_teacher', 'strict_examiner', 'coding_mentor', 'interview_coach'));
