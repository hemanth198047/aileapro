# Launch checklist (Phase 16 / M7)

Product owner signs this before public traffic. Items without an automated proof stay manual.

| # | Item | Evidence | Owner | Date | Sign |
|---|---|---|---|---|---|
| 1 | P0 learner capabilities exercised (API + manual smoke) | `25-ACCEPTANCE_CRITERIA.md` ticks + `phase16-walk.ps1` | Eng | | |
| 2 | Backup restore proven | Dump `.run/backups/aileapro-20260919-190046.sql` via `backup-restore-drill.ps1` (feature_flags=3) | Eng | 2026-09-19 | eng |
| 3 | Fake AI / fake lab refused in prod | `AiStartupCheck` + `LabStartupCheck` + `application-prod.yml` | Eng | | |
| 4 | Feature flags live | `lab_enabled`, `tutor_enabled` in DB; admin PUT works | Eng | | |
| 5 | Privacy export + delete | `GET /v1/me/export`, `DELETE /v1/me`; Settings UI | Eng | | |
| 6 | Privacy + ToS URLs live | `/privacy`, `/terms` stubs (counsel replace later) | Product | | |
| 7 | Session cookies secure in prod | `server.servlet.session.cookie.secure=true` | Eng | | |
| 8 | Lab network isolation | `DockerNetworkIsolationTest` (Phase 15) | Eng | | |
| 9 | Threat review skim | `14-SECURITY.md` checklist read | Eng + Product | | |
| 10 | Staging sign-off | Staging URL healthy; seed curriculum present | Product | | |

**Product owner signature:** ______________________  **Date:** __________

Deferred (not blocking this gate slice): full Playwright ×20, k6 load smoke, cloud secrets manager wiring beyond env vars.
