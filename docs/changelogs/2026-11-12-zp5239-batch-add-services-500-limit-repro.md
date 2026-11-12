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
