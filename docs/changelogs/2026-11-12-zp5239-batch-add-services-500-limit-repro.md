# 2026-11-12 — ZP-5239 "Limit on batch adding services": reproduced on QA

**Prompt:** "https://egalvanic.atlassian.net/browse/ZP-5239 check that if we are able to Reproduce this or not"

**Ticket:** ZP-5239 (Backlog, customer issue from BCES IQ, labels DevRev / bces-iq / customer-issue).
- A site with 967 assets shows an error when services are added to a work order covering all of them.
- The customer's screenshot: `Failed to add assets: API call failed: 400 - { "error": "at most 500 assets per request", "success": false }` above a "Back to Work Orders" button.
- Shubham's comment (11 Nov): "as this is already implemented we're not able to reproduce it".

## Result: REPRODUCED on QA (web V1.36, bundle `index-49__nsGq.js`, 12 Nov 2026)

Site **test site** (Default EG-ACME Account, `21bd10d4-cb0e-4e21-9722-9cd6219cedfc`). Test work order "QA-DEMO ZP-5239 repro 12 Nov - add 1032 assets by service":
https://acme.qa.egalvanic.ai/sessions/dea52ea7-cb1b-4a9a-85e5-bf53b2eee035

| # | Step | Result |
|---|------|--------|
| 1 | Create Work Order wizard on "test site", service **Cleaning**, all eligible assets (1032) → Create | OK: one `POST /api/ir_session/create` → 201. The assets are resolved server-side, so the wizard is not affected. |
| 2 | On the work order: **Actions → Add Existing by Service** → Service "De-Energized Visual Inspection" → "1033 of 1033 qualifying assets selected" → **Add 1033 Assets** | **FAIL:** the page is replaced by `Failed to add assets: API call failed: 400 - { "error": "at most 500 assets per request", "success": false }` + "Back to Work Orders", the same as the customer's screenshot. Network: a single `POST /api/ir_session/<id>/add-assets` → 400. None of the 1033 were added (the service is not in the WO's service-registry). |
| 3 | Control: same dialog, narrowed to location "test / test k / test" → **Add 136 Assets** | OK: `add-assets` → 200, forms added (the asset rows go from 1 to 3 forms). |

Evidence: `docs/bug-evidence/2026-11-12-zp5239/` (1-create-wo-review, 2-dialog-1033-selected, 3-error, 4-control-136-ok).

## Root cause (front-end code, same on QA `index-49__nsGq.js` and prod `index-CS4X_UFQ.js`)

- The backend refuses more than 500 `node_ids` per `POST /api/ir_session/{id}/add-assets`.
- The front end has a `SESSION_NODE_CHUNK = 500` constant. Two bulk paths in `SessionDetail` use it, looping in chunks of 500:
  - Bulk Ops → selected assets → manage services → "Add a service" → **Add**;
  - Manage Forms → create forms.
  
  These are probably what "already implemented" refers to.
- The **"Add Existing Assets by Service"** dialog (`Xp` handler, button "Add N Assets") is **not** chunked. It calls `addAssetsToSession(sessionId, allSelectedIds, [serviceId], preloadForms)` once with every selected id. So any service matching more than 500 assets fails.
- Prod has the same unchunked call, so the customer hits it there.
- Workaround for the customer until it is fixed: in that dialog, narrow the scope (Location / Asset class) so each add is 500 assets or fewer, and repeat.

## Not done (needs the owner's OK)
- No Jira comment posted and no status change. The draft comment is ready (same content as above, with the screenshots).

## Also this prompt: owner's rule on ticket statuses
- New memory: never test or move **Resolved / To Do / In Progress** tickets unless the owner names them.
- Jira check: my account moved 136 tickets from "Resolved (migrated)" to "Ready for QA" over time. 30 are still open, all Eric Ehlert's, mostly moved on 2 Nov:
  - 22 In QA: ZP-4830..4835, 4885, 4886, 4940, 4942, 4943, 4947, 5026, 5031, 5100, 5105..5111;
  - 8 READY TO RELEASE: ZP-4884, 4906, 4907, 4919, 4955, 4956, 5029, 5030.
- Nothing was moved back; that waits for the owner.

## Follow-up: comment posted (owner: "add comment in ticket")
- Recorded a 79 s step video on QA with `.playwright-mcp/v25/video/zp5239/v.js`:
  - Infrared Thermography: "1100 of 1100 qualifying assets selected" → Add 1100 Assets → 400 error.
  - Control: one location, 4 assets → 200.
- Posted comment **46114** on ZP-5239 from abhiyant.singh@egalvanic.com: the video plus 3 screenshots (`QA-12Nov-ZP-5239-*` attachments). The render check was clean. The status stays Backlog.
- The comment is written as an update to comment 46113 (posted earlier from the owner's account), which said "Already fixed in ZP-4840". ZP-4840 batched only Bulk Ops → Services; Actions → Add Existing by Service is still unbatched.

## Stage sign-off: PASS → READY TO RELEASE (owner: "start testing this in stage and give sign off if everything is fine")
- Krunal moved the ticket To Do → In Progress → Resolved (migrated) → Ready for QA at 03:50 CST. I moved it to **In QA** (Start Testing) first.
- Stage build `index-BaASlqvW.js`. The "Add Existing by Service" handler now loops in `SESSION_NODE_CHUNK` (500) batches. It shows "Added X of N assets, then stopped: … Close and add again to finish." inside the dialog instead of replacing the page.
- Stage WO "QA-DEMO ZP-5239 stage 12 Nov - add existing by service" (test site, General, started empty): https://acme.stage.egalvanic.ai/sessions/2cfc0583-e537-449f-bbbe-75afc55ce78d

| Check | Result |
|---|---|
| Cleaning: "1907 of 1907 selected" → Add 1907 Assets (video, STAGE_OPS seat) | 4 add-assets calls 500+500+500+407, all 200; dialog closed in 13.9 s; no error; WO "1–20 of 1907" |
| Reopen with the same service | "1907 qualifying assets are already on this work order…"; 0 extra calls |
| De-Energized, 2nd batch forced to fail (fetch override in my tab only) | dialog stays open: "Added 500 of 1907 assets, then stopped: Something went wrong. Close and add again to finish."; page not replaced |
| Add again (the dialog re-evaluates to "500 already … 1407 of 1407") | 3 calls 500+500+407, all 200; both services on all 1907 nodes; 3816 method lines, 0 duplicates |
| Small add: Infrared Thermography narrowed to ATS (102) | 1 call, 200 |
| Not covered | stage services have "No forms" for these classes, so form minting across batches is not checked |

- Comment **46119** posted (video + 4 screenshots, render clean). Transition QA-Complete → **READY TO RELEASE**.
- Evidence: `docs/bug-evidence/2026-11-12-zp5239-stage/` (the mp4 is gitignored; it is attached in Jira).

## 15:48 IST: sign-off posted in eg-internal-dev (owner: "if it working as expected then give sign off in group dev")
- Context in the space: the owner had asked Krunal to fix ZP-5239 as a hotfix ("merge in stage then production after my sign off") and Shubham to tell the customer once it is on production; Krunal replied "@Abhiyant Singh verify in stage".
- Re-checked just before posting: stage still on the tested build index-BaASlqvW.js, /api/health 200.
- New message (not a thread reply) with @Krunal Lunagariya and @Shubham Goswami: PASS on stage, 4 batches 500+500+500+407, no duplicates, inline batch-failure message, forms not covered on stage, moved to Ready to Release, Krunal to merge to production, Shubham to tell the customer after that; links to the ticket and comment 46119.
