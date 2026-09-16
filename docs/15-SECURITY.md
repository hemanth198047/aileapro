# 15 — Security

## Objectives

Protect learners, protect untrusted code execution, protect keys, resist prompt injection, stay OWASP-aligned. Security curriculum teaches **defense**; the platform must not become an attack kit.

## AuthN / AuthZ

- Password hashing: Argon2id.
- Session: rotating, httpOnly, Secure, SameSite=Lax/Strict as appropriate.
- CSRF: token or SameSite + origin check on mutations.
- OAuth state/nonce.
- RBAC on every handler; default deny.
- IDOR tests: learners cannot read others’ mastery, chats, lab output.

## Input / output

- Validate all inputs (length, type, language enum).
- Sanitize Markdown (no raw HTML). Mermaid/math sandboxed.
- Upload: magic-byte sniff, size limits, virus scan later, private buckets.
- SSRF: ingest allowlist, block link-local, metadata IPs.

## Secrets

- Env / secret manager only.
- Never `NEXT_PUBLIC_` for provider keys.
- Executor has **no** cloud admin credentials.

## Code execution

- Untrusted code **only** in executor.
- No network by default; allowlist optional for datasets we host.
- CPU/memory/time/pids/disk quotas.
- Drop capabilities; read-only root; tmpfs workspace.
- Separate uid; seccomp.
- Rate limit per user.
- Images pinned by digest.
- Do not run learner code in Next.js, the API JVM, or the worker JVM. Only `backend/executor`.

## AI safety

- Injection: untrusted delimiters; retrieval is data.
- Tool allowlists.
- Output filter: secrets regex, jailbreak persistence, hate/violence policy.
- Rate limit expensive models.
- Logging redaction.

## Multi-tenant (future)

Row-level `org_id`; no shared executor noisy neighbor without cgroup isolation (already).

## Abuse

- Registration throttles, captcha if needed.
- Lab crypto-mining patterns → kill.
- Tutor used as general free proxy → budget + ToS.

## Audit

Authz failures, admin publishes, rollbacks, budget kills, lab kills.

## SDLC

- Dependabot.
- Secret scanning.
- SAST on TS.
- Threat model review each phase gate.
- No exploit PoCs in repo for third-party systems.

## AI security labs (product)

Simulated apps in sandbox. Documented as defensive. No live third-party attack instructions.

## Incident

Runbook: rotate keys, disable provider, disable lab, notify. Stored in 19-DEVOPS.
