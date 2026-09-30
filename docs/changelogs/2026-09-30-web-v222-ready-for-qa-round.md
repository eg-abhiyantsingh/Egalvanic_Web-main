# Web v2.2.2 — Ready-for-QA round on stage (30 Sep 2026, 16:50–17:55 IST)

**Prompt:** "in site based site should show up side … create bug assign to avani. no need to check all that bug that you
are creating … and also test v2.2.2 ready to qa ticket only and if they pass then move them to ready to release",
then "Next site expiry 21 days Oct 21, 2026 site name should be visible" (new bug) and "no need to test ready to
release ticket for now".

Release page (v10): https://claude.ai/artifact/W2LRm5JVfjhxMSM6UZiGqB

## Verdicts

| Ticket | Result | Jira now |
|---|---|---|
| ZP-4491 ended subscription says "ended N days ago" | pass on real data (Legacy ended Aug 26 → "ended 35 days ago") + replay | READY TO RELEASE (moved) |
| ZP-4493 banner in one place on every page | pass: 16 px from the top on 9 pages, full load + in-app, two builds | READY TO RELEASE (moved) |
| ZP-4358 Error on Bulk Nameplate extraction | pass: 2 QA-DEMO assets → "Extracted data for 2 assets", no false "No assets were updated" | READY TO RELEASE (moved) |
| ZP-4401 asset deletion in SLD (Primient) | pass: delete in a view sends only the selected node; server keeps the shared bus; control: orphaned bus cascaded by the server | READY TO RELEASE (QA-Complete, id 10) |
| ZP-4492 term bar after the plan ended | pass on replayed data (no Foundation plan on stage) | Ready for QA — the move was blocked by the tool's permission check; owner to decide |
| ZP-4494 Sites · Assets tile | pass on replayed data (grace/locked/ended) + locked tile "Advanced features · Locked · Since Aug 14" | Ready for QA — same |
| ZP-4463 Extract from Photos, many photos | 4 of 5 fixed; result message still 807 px down (out of view) | Ready for QA, on hold |
| ZP-4495 Ops Core Included on Foundation | blocked: fix is in the API, needs Foundation + stale Ops Core dates on stage | Ready for QA |
| ZP-4464 story | not done (open 4496–4500, 4502, 4503) | Ready for QA |

New bug: **ZP-4503** — Site-based "Next site expiry" tile shows only days + date; should name the site
(owner's screenshot attached; Avani, Web v2.2.2, sprint Z-26-09-S3, To Do).

## Code changes

### `ExtractFromPhotosTest` — follows the v2.2.2 job flow and can force failures for free
- **Why:** from Web v2.2.2 the editor no longer sends one synchronous `extract-nameplate-data` call. It runs a job:
  `POST /extraction/bulk-job/submit` → `GET /extraction/bulk-job/status?execution_arn=…` about every 4 s →
  `POST /extraction/nameplate-agent/apply`. The old test waited 240 s for a call that never comes, and reported
  "no extraction request" even when the extraction had succeeded.
- **What:** it now classifies the calls after the press (sync / submit / polls / apply), treats the run as finished
  when apply lands, the job reaches a final status, or a message appears, and records the progress text,
  press→outcome time, and the number of submits (a second submit = a second paid AI run).
- **Recorder:** it parses the job's top-level `status` itself (`st`). The kept body is cut at 600 characters, and the
  status comes after `results[]` in the JSON, so reading it from the body missed the final SUCCEEDED.
- **`-Dext.simulate=submit504|status504|statusFailed`:** a second script, installed after the recorder, answers the
  extraction calls inside the page with CloudFront's HTML 504 page or a FAILED job, so the error path can be tested
  **without calling the AI** (nothing billed, nothing written). It then asserts plain words, no apply call, and no server
  change for 45 s.
- **Retry check:** after a failure, `-Dext.afterError=retry` now counts new submits and the job ids polled, to show
  whether the retry resumed the same job.
- **Menu:** the first click on the Extract split button sometimes lands before the editor is ready (seen on the
  12-photo asset). It now retries up to 3 times.

### `AssetMenuProbe` (new, diagnostic)
Lists the icon buttons in the asset page's top band and the ⋮ menu, used when the subscription banner moved the ⋮.

## Measured

- 12 photos: submit 200 (0.4 s) → 10 polls → SUCCEEDED → apply 200; submit→apply 31.2 s (QA's old sync call: 504 at 60.5 s).
- Forced failures: submit 504 → "The server took too long to respond. Please try again."; job FAILED → "The photos could
  not be read. Please try again."; status 504 → "Extraction is still running and nothing has been changed on this asset
  yet. Try again in a few minutes to pick up its result."; retry → 0 new submits, same job polled; Library after a failure →
  1 submit, no dialog.
- ZP-4401 in view {bus, load 1}: `bulk-delete {"node_ids":[load 1]}` → `cascaded_node_ids: []`; bus + load 2 + their
  edge alive. Control in All Nodes: `bulk-delete {"node_ids":[load 2]}` → `cascaded_node_ids: [bus]`.
- Stage builds: `index-Biy5lnAh.js` (16:40) and `index-BiQjo0Z9.js` (17:10). Between them only the Subscription page and
  banner logic changed (titles, Next-expiry names, licensed sites moved up, "A, B and C" lists); the SLD, Assets,
  Edit Asset and bulk-job chunks differ only in import names.

## Side notes (not filed)
- SLD: after deleting in a view, switching to All Nodes in the same page still draws the deleted node until a reload.
- The site-plan banner read "Atest and Atest site licenses expire …" at 17:25 (stage data being edited).

## Evidence (local; screenshots with stage prices are not committed)
`test-output/extract-stage-1700`, `zp4464-deep-1715`, `zp4464-real-ended-1700`, `zp4464-banner-position-inapp-1658`,
`docs/bug-evidence/2026-09-30-zp4358-stage`, `docs/bug-evidence/2026-09-30-zp4401-stage` (+ `fixture.txt`, committed),
`docs/bug-evidence/zp4464-next-site-expiry-name`.
