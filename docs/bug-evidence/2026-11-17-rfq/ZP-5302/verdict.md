# ZP-5302 [Web] Work order › Actions › Export to Excel shows only "EG Form export failed" when the work order has no EG forms: verdict

**Result: PASS → READY TO RELEASE.** On a work order with no EG forms, Export to Excel now says "This work order has no EG forms yet. Add forms with Manage Forms, then export." Nothing downloads. The message is correct: Manage Forms is in the same Actions menu, under EG FORMS. A work order whose only form was removed gets the same message. A work order with a form still downloads the file. Every other export error still shows the generic "EG Form export failed".

- **Build:** QA `index-CkkAeA9o.js` (built 14:42 IST, deployed 14:45 IST). Re-checked at 15:35 IST, still the same build. Tested 14:47–15:35 IST.
- **Fix:** eg-pz-frontend PR #2040 (`bugfix/zp-5302/handle-no-eg-forms-export-error` → `cicd/qa`), merged 08:29 UTC. It changes only `src/pages/SessionDetail.jsx`:
  - `res.json()` gets a `.catch(() => ({}))`, so a non-JSON error body can't throw.
  - The new text shows only when the server's `error` is exactly "No EG form instances found for this session". Anything else shows "EG Form export failed".
  - I confirmed the live chunk `SessionDetail-7ClzdbUI.js` has this code.
- **Seats:** Super Admin for the UI. PM, Technician, Facility Manager and Client Portal for the API and menu.

## Deploy note (important for whoever tested this before 14:45 IST)
From 13:59 to about 14:45 IST, QA served `index-LGhrn-48.js`, which did **not** have the fix. Its `SessionDetail-Q2h74aNG.js` still had the old `"EG Form export failed"` handler.
- Two deploys of `cicd/qa` ran at the same time: ZP-4830 (commit 877b86be, run 44047076610, started 08:28:51 UTC, took 5m01s) and PR #2040 (run 44047168136, started 08:29:29 UTC, took 3m42s).
- The older ZP-4830 deploy finished last, so it replaced the #2040 deploy. The build timestamp in LGhrn-48, 08:29:45 UTC, matches the ZP-4830 run.
- The ticket went to Ready for QA at 14:05 IST, inside that window. The fix only arrived with the 14:41 IST deploy of PR #2045 (stag-to-qa merge, run 44053303879).
- Enhancement for DevOps: put the frontend deploy workflow in a concurrency group, so an older run can't overwrite a newer one.

## What was run
| # | Check | Result |
|---|---|---|
| 1 | Ticket repro: WO "QA-DEMO ZP-5244 13 Nov check-off" (no forms) › Assets › Actions › Export to Excel | PASS. `GET /api/eg-form-excel/export?session_id=d1cbacb1…` returns 404 `{"error":"No EG form instances found for this session"}`. The dialog says "Error · This work order has no EG forms yet. Add forms with Manage Forms, then export. · OK". No download. |
| 2 | Is the message correct? | PASS. The same Actions menu lists EG FORMS › Manage Forms, Export to Excel, Import from Excel. |
| 3 | Control: WO "Job - Nov 6, 12:03 PM" (1 form) › Export to Excel | PASS. 200, `eg_forms_7d2ba67a.xlsx` downloads, no dialog. |
| 4 | WO whose forms were all removed | PASS. On WO d1cbacb1 I added the form "ATS inspection" to one QA-DEMO asset ("QA-DEMO TKT-1169 sticky A 16 Nov") through Manage Forms: `bulk-forms` add, added 1. Export then downloaded `eg_forms_d1cbacb1.xlsx`. I removed the form again: `bulk-forms` remove, removed 1. Export then showed the same new message (404). The asset is back to 0 forms. |
| 5 | Other export errors (simulated in my test browser only by rewriting the export response) | PASS. All show the generic "EG Form export failed": 500 with an HTML body (the JSON parse error is caught), 403 JSON, 404 with other text ("Session not found"), 500 JSON with a traceback text, and a network failure. The server text is never shown to the user. |
| 6 | Same 404 text returned on a WO that has forms (simulated) | Shows the new message. That is expected, because the match is on the exact server text. |
| 7 | Flag `feature-eg-forms` (gates the whole EG FORMS menu section, `Lo("eg-forms")`) | Live value ON for EG-ACME on QA. OFF was simulated (evalx rewritten and LD stream blocked, in my browser only): Manage Forms, Export to Excel and Import from Excel all disappear from Actions, so the export can't be reached. PASS. |
| 8 | Roles | PM and Technician see the same Actions menu, and the API gives them the same 404 text. Facility Manager and Client Portal land in the Maintenance Portal and have no work order page. |
| 9 | Import from Excel messages (asked by the lead; not part of this fix) | See the 3 new bugs below. A file exported from the same work order imports fine: "Updated 1 form instances across 1 sheets". |

Console: the app logs `[UserError] Operation failed: {}` for each export error. The second commit says "the server text goes only to the log", but the Error is logged as `{}`, so the server text never reaches the console either. This is a minor developer note, not user-facing.

## Not covered
- A real LaunchDarkly switch of `feature-eg-forms`. It was simulated only, and the fix itself is not behind a flag.
- Production WO "EdgeCore EEM - SV01" (Mukul's report): it is not on QA, and QA work stays on QA.
- Hiding or disabling Export to Excel when the WO has no forms. The developer chose a message instead, which the ticket allows.

## Enhancement ideas
- The page already calls `/api/eg-form-instance/by-session/{id}/count`. It could disable Export to Excel with a tooltip when the count is 0.
- Match on a stable error code from the server rather than the English sentence. If the backend wording changes, the screen silently goes back to "EG Form export failed".
- The guidance shows under a red "Error" title. A neutral title such as "No EG forms yet" would fit better.

## New defects (separate from this ticket)
All three were found in Import from Excel, the menu item next to Export to Excel. Each has a `BUG-*.txt` draft, a video and screenshots in this folder. The JQL duplicate check (`"not a zip file"`, `"form instances"`, `"EG form import"`, `eg-form-excel`) found no existing ticket.
1. **BUG-import-not-xlsx-message.txt** (Low): an .xls file, which the file picker offers, or any non-.xlsx file shows only "File is not a zip file", and the server returns 500.
2. **BUG-import-nothing-matched-green-success.txt** (Low): when nothing was imported, the app shows a green "Updated 0 form instances across 1 sheets". This happens when a row's Instance ID matches no form (`rows_skipped: 1`), and for a blank workbook.
3. **BUG-import-updates-other-work-order.txt** (Medium, please confirm intended): Import from Excel on work order A writes into work order B's forms when the file came from B, and the green message gives no hint. Shown with Test Voltage 600 → 650 on "Job - Nov 6, 12:03 PM" while importing on "QA-DEMO ZP-5244 13 Nov check-off". The value was put back to 600 afterwards.

One more finding about these Excel endpoints was sent to the lead in the hand-off only, not written here, because this repo is public.

## Cross-platform
- **Twins:** none. JQL for "Export to Excel" / "Import from Excel" / `eg-form-excel` / "Excel-based bulk" finds only web tickets (ZP-5302, ZP-1724).
- **iOS:** not on this platform. I searched the iOS source (`ios-src`, release/qa 1.75, commit 34bd67e from 16 Nov) for `excel`/`xlsx`/`eg-form-excel`. The only hits are attachment file types (`Attachment.swift`, `AttachmentsTab.swift`, `AttachmentConfigurationView.swift`: preview/MIME for .xlsx attachments). There is no EG form Excel export or import.
- **Android:** not checked. The Android source is not in the org repos I can read, and the emulator lane is busy.
- **Checks for the device testers:**
  - iOS, iPhone, QA build: open work order "QA-DEMO ZP-5244 13 Nov check-off" › its actions/forms menu. Expected: no Export to Excel / Import from Excel. Report if any Excel action exists.
  - Android: the same check on the same work order.
  - iOS and Android read-back: open WO "Job - Nov 6, 12:03 PM" › asset 14N-H1-2 › "Panelboard / Switchboard Insulation Resistance" form. Expected: Test Voltage 600, the value the web Excel import wrote.
  - iOS and Android: open WO "QA-DEMO ZP-5244 13 Nov check-off" › asset "QA-DEMO TKT-1169 sticky A 16 Nov". Expected: no "ATS inspection" form, because it was added and then removed on web.

## Test data / links
- No-forms WO: https://acme.qa.egalvanic.ai/sessions/d1cbacb1-55bc-4318-b432-bda1df9ffd51
- Control WO "Job - Nov 6, 12:03 PM": https://acme.qa.egalvanic.ai/sessions/7d2ba67a-49a0-4e91-8973-f7cd5e4f2673
- API: https://acme.qa.egalvanic.ai/api/eg-form-excel/export?session_id=d1cbacb1-55bc-4318-b432-bda1df9ffd51
- Form instance used by the import checks: https://acme.qa.egalvanic.ai/api/eg-form-instance/454c2392-4806-43fe-8a99-1863c7c1e318 (Test Voltage 600 at the end)
- Import test files: `.playwright-mcp/rfq17-z5302-5283/files/` (QA-DEMO-ZP-5302-*.xlsx/.xls)

## Evidence
- Video: `QA-17Nov-ZP-5302-export-no-forms-message-video.mp4` (46 s)
- `QA-17Nov-ZP-5302-1-no-forms-clear-message.png`, `-2-only-form-removed-then-export.png`, `-3-manage-forms-in-same-actions-menu.png`, `-4-other-error-generic-message-simulated.png`
- New bugs: `BUG-import-*.txt`, `QA-17Nov-BUG-import-*-video.mp4`, `QA-17Nov-BUG-import-*.png`
- Raw: `raw/a1.json` (repro, control, simulated errors, import messages), `raw/a3.json` (forms removed, cross-WO import), `raw/vbugs.json` and `raw/vbugC2.json` (bug video runs), screenshots `raw/01…33-*.png`, downloaded files `raw/*.xlsx`
