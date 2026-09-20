# Launch checklist (Phase 16 / M7)

Product owner signs this before public traffic. Items without an automated proof stay manual.

| # | Item | Evidence | Owner | Date | Sign |
|---|---|---|---|---|---|
| 1 | P0 learner capabilities exercised (API + Playwright smoke) | `25-ACCEPTANCE_CRITERIA.md` + `phase16-walk.ps1` + `frontend/web/e2e/p0-flows.spec.ts` | Eng | 2026-09-20 | eng |
| 2 | Backup restore proven | Dump `.run/backups/` via `backup-restore-drill.ps1` | Eng | 2026-09-19 | eng |
| 3 | Fake AI / fake lab refused in prod | `AiStartupCheck` + `LabStartupCheck` + `application-prod.yml` | Eng | 2026-09-20 | eng |
| 4 | Feature flags live | `lab_enabled`, `tutor_enabled` in DB; admin PUT works | Eng | 2026-09-20 | eng |
| 5 | Privacy export + delete | `GET /v1/me/export`, `DELETE /v1/me`; Settings UI | Eng | 2026-09-20 | eng |
| 6 | Privacy + ToS URLs live | `/privacy`, `/terms` operational policies (counsel still reviews) | Eng | 2026-09-20 | eng |
| 7 | Session cookies secure in prod | `server.servlet.session.cookie.secure=true` | Eng | 2026-09-20 | eng |
| 8 | Lab network isolation | `DockerNetworkIsolationTest` (Phase 15) | Eng | 2026-09-20 | eng |
| 9 | Threat review skim | `15-SECURITY.md` checklist read | Eng + Product | | |
| 10 | Staging sign-off | Staging URL healthy; seed curriculum present | Product | | |

**Load smoke:** `k6 run infra/scripts/k6-smoke.js` (API must be up).

**Mail:** set `SPRING_MAIL_HOST` (+ user/password) for real SMTP; otherwise verify/reset tokens are logged by the API.

**Product owner signature:** ______________________  **Date:** __________

Remaining product-owner items: threat skim (#9), staging URL sign-off (#10), counsel stamp on Privacy/ToS, and the signature line above.
