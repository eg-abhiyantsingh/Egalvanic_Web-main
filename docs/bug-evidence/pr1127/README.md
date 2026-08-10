# PR #1127 — QA evidence (QA V1.36, 2026-08-10)

| File | Shows |
|---|---|
| `EVIDENCE-1-quotes-grid-spans-multiple-sites.png` | **Rendered** Quotes grid — Facility column carries *Android Site 2* AND *(s) Wild Goose Brewery* at once; "1–10 of **151**" (150 + the quote created for item 5, top row); **no site picker** in the topbar (FORCED_ALL). Reached from a site-scoped page, so the stale-`sldId` precondition held. |
| `EVIDENCE-2-invite-visible-control-empty-site-no-job.png` | The **control** for the invite tests — empty site "Test android" (grid "0–0 of 0"), no active job, invite modal *"Let's get your assets in"* present with **Upload Anything** / **Not now**. Every "invite is hidden" result is only meaningful against this. |
| `EVIDENCE-3-selenium-control-guard-caught-wrong-site.png` | `UploadInviteGatingTestNG`'s first run failing **on its own control** — the site switch had silently not taken. Without that guard the five suppression assertions would have gone green against the wrong site. |

## Not screenshotted (asserted programmatically instead)

The suppressed / fail-open states were produced by injecting responses into
`GET /api/onboarding/jobs/active` and asserted on the rendered DOM text, not captured as
images: `running` → hidden, `pending` → hidden, lookup rejects → shown, HTTP 500 → shown,
in-flight → held. These are re-run any time via `UploadInviteGatingTestNG`.
