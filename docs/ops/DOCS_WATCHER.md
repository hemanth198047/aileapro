# Official docs watcher (manual procedure)

Phase 15 does not ship a scraper. Until an automated watcher exists:

1. Watch release notes for frameworks and model providers you teach (PyTorch, Hugging Face, OpenAI, etc.).
2. When something matters to a published unit, create an update:

   `POST /v1/admin/updates` with slug, title, summary, lifecycle, `sourceSlugs`, and `publish: true`.

3. If curriculum text should change, open a proposal:

   `POST /v1/admin/proposals` with impact checklist flags; leave it `pending` for another admin to approve or reject.

4. Learners only see published updates on `GET /v1/evolve/feed`. Pending proposals never appear there.

5. Copy must stay honest: “Verified against N sources as of …” — never “fully up to date.”

Link health: `POST /v1/admin/jobs/link-check` (also scheduled daily when `aileapro.link-check.enabled=true`).
