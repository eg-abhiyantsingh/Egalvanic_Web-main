# ZP-4580: Extract from AI on the asset page Schedule and Engineering tabs

**Verdict:** FAIL on one QA Review row ("the page reloads after the write"). Everything else passes.
Build `index-C14cwR0U.js` (QA, checked 17:33 and 19:12 IST, 13 Oct 2026). Frontend PR #1616 is in the bundle (`AssetDetails-FLQnZJoG.js`: `AiExtractButton`, "Extract from AI", "Attributes + Schedule/Library").

Test data (mine, site "Test site A"):
- Panelboard https://acme.qa.egalvanic.ai/assets/35f4d760-3d89-4c64-bf6e-55667fccc0d2 "QA-DEMO ZP-4580 panelboard (delete me)", with a nameplate photo and a panel-schedule photo (fixtures `QA4580-panelboard-nameplate.jpg`, `QA4580-panel-schedule.jpg`).
- Panelboard B https://acme.qa.egalvanic.ai/assets/827ec60f-0a38-4639-8cf9-28c58356c66a "QA-DEMO ZP-4580 panelboard B replay (delete me)".
- Breaker `d77347c2-aa19-4818-adf0-f5e65d5f51db` (Square D nameplate photo).
- Transformer (3-Winding) `e79f0f16-28f4-4080-bdc0-b4a5f33653ef`.
- Motor `75fe893b-f1e6-40a8-9509-07c683fee52b`.

Paid AI: **one** real "Extract from AI" run (a nameplate job and a configure/schedule job). Every other run was free:
- The re-attach checks used fake job answers in the browser.
- The refresh check replayed the real job's stored result (status calls only, no new AI run).

## Checked
| Check | How | Result |
|---|---|---|
| Schedule tab button | Panelboard › Schedule | "Extract from AI" at the top. PASS (shot -4) |
| Engineering split button, enclosure | Panelboard › Engineering › arrow "More extraction options" | Menu "Attributes" / "Attributes + Schedule" (with the schedule hint). PASS (shot -1) |
| Engineering split button, device | Circuit Breaker | "Attributes" / "Attributes + Library". PASS (shot -2) |
| Not eligible: 3-winding transformer, Motor | Engineering tab | Plain "Extract from Photos", no arrow, no Library/Schedule option. PASS (shot -3) |
| Real run: read + schedule | Panelboard › Schedule › Extract from AI | 1. Nameplate job about 25 s. 2. "Bulk Extraction Job" dialog, schedule job about 30 s. 3. Review row "35 kA SCCR (label) · MCB · 11 circuits · High" with Apply. 4. Apply → "Applied". PASS (shots -5, -9) |
| Values written | API + a manual reload | Eaton/Cutler-Hammer, 225 A, MCB, 3P4W, SCCR 35 kA on the label, panel type PRL1a, 18 positions, circuits 1–6, 8, 10, 12, 14, 16, 18 match the photo. PASS (shot -9) |
| Values not written | Job assumptions | Both 3-pole feeders (ckt 7 AHU-2 40 A, ckt 13 LP-2 feeder 60 A) and the 225 A main were dropped on purpose. Reason: no library frame at 480 V. The reasons are in the job result, but the review row does not show them. Suggestion, not a bug |
| **Page refresh after the write** | Same run, then a free replay on panelboard B | **After Apply, and still after Close, the Schedule tab says "This panel doesn't have any circuits yet" and "Label: label not captured" (SCCR "not established" in the real run). Only the asset record is re-fetched; the panel view (`panel-editor/view`) and SCCR (`panel/<id>/sccr`) are not. A manual browser reload shows the circuits.** FAIL (shots -6, -7, -8) |
| Same run started in the editor re-attaches | Edit Asset › Extract from Photos › Attributes (fake job left running) → open the asset page | Asset page shows "Extracting…", polls the same job id, **no second submit**. A page reload during a run also re-attaches. When the fake job ended the pending entry was cleared. PASS (shot -10) |
| Flag `feature-eng-lib` | Live evalx ON for Acme. SIMULATED OFF | Panelboard and breaker show a plain "Extract from Photos" (no arrow); the Schedule tab has no "Extract from AI". PASS, SIMULATED (shots -11, -12) |
| Roles | PM, EE, CP, FM on a transformer asset they can open | PM and EE: split button "Extract from Photos" + "More extraction options" (transformer = library class). Client Portal: no Engineering tab, no extract button. FM: cannot open that site's assets ("This asset link doesn't look right"). PASS (shots -13, -14) |
| API | `bulk-job/submit` → `status` (3 s polling) → `nameplate-agent/apply` → `asset-agent/apply` | All 200; 2 submits for one "Extract from AI" (nameplate + configure), 17 status polls. PASS |

## Bugs found
**B1 (Medium, frontend), in scope: after "Extract from AI" applies, the Schedule tab is not refreshed. The user still sees "no circuits".**
1. As an admin, open https://acme.qa.egalvanic.ai/assets/35f4d760-3d89-4c64-bf6e-55667fccc0d2 (a panelboard with a nameplate photo and a schedule photo) › **Schedule**.
2. Click **Extract from AI**. Wait about 1 minute for the "Bulk Extraction Job" dialog.
3. Click **Apply** on the row "35 kA SCCR (label) · MCB · 11 circuits", then **Close**.
- What happens: the Schedule tab still says "This panel doesn't have any circuits yet" and "Label: label not captured". The circuits are already saved: pressing the browser's reload button shows them (circuits 1–18, SCCR 35 kA on label, Type PRL1a).
- What should happen: the ticket says "the page reloads after the write". The panel schedule and SCCR sections should refresh as soon as Apply finishes.
- For the developer: `onApplied` only re-fetches the asset record (`graph/nodes/<id>/enriched`). The Schedule tab's own data (`panel/<id>/panel-editor/view`, `panel/<id>/sccr`) is not re-fetched, even after the dialog closes.
- Risk: a user who sees "no circuits" will press Extract from AI again, which is a second paid AI run.
- Evidence: shots -6, -7, -8. Suggested assignee: Avani (release batch rule), frontend.

## Enhancement suggestions
- Show what was NOT applied in the review row. In this run 2 three-pole feeders and the main breaker were silently skipped: "2 circuits and the main need to be added by hand".
- "11 circuits" counts the two SPACE positions too. Show "9 breakers + 2 spaces".
- Show elapsed time or progress on the "Extracting…" button: the nameplate step takes about 25 s with no visible progress.

## Mobile part for Nency
None. This is the web asset page. The AI job APIs are shared, but nothing changed in the mobile app.

## Not covered
- A real "Attributes + Library" run on the breaker. To keep paid AI to one run, the option was shown but not run. The library flow itself is ZP-4358/4463 territory.
- A user without portal edit rights but with `nodes.manage`: no such seat.

## Proposed Jira comment
h3. QA: ZP-4580 one row fails (QA build index-C14cwR0U.js, 13 Oct)

What works:
# Panelboard › Schedule has *Extract from AI*. Engineering has the split button *Extract from Photos* with "Attributes + Schedule" (panelboard) or "Attributes + Library" (breaker). A 3-winding transformer and a motor get only the plain button.
# A real run on a panelboard read the nameplate and the schedule photo, showed the review row "35 kA SCCR (label) · MCB · 11 circuits", and Apply saved the right values (Eaton, 225 A, MCB, PRL1a, 35 kA, circuits).
# A run started in Edit Asset is picked up by the asset page (same job, no second run).
# With the eng-lib flag off (simulated), only the plain button shows.
# Client Portal sees no extract button; Project Manager and Electrical Engineer do.

What fails (the ticket says "the page reloads after the write"):
# Open the panelboard https://acme.qa.egalvanic.ai/assets/35f4d760-3d89-4c64-bf6e-55667fccc0d2 › *Schedule*.
# Click *Extract from AI*, wait for the dialog, click *Apply*, then *Close*.
# *Actual:* the Schedule tab still says "This panel doesn't have any circuits yet" and "Label: label not captured".
# *Expected:* the circuits and the SCCR label show right away. Today they only show after a manual browser reload, and users will re-run the paid AI.

!QA-13Oct-ZP-4580-5-review-step-before-apply-PASS.png|width=720!
!QA-13Oct-ZP-4580-7-after-close-still-no-circuits-label-not-captured-FAIL.png|width=720!
!QA-13Oct-ZP-4580-8-only-manual-browser-reload-shows-saved-circuits-FAIL.png|width=720!

Note: the AI skipped the two 3-pole feeders and the main (no 480 V library frame). That is by design, but the review row does not say so.

*Status:* moved to To Do

## Recommended status
To Do. The ticket's own row ("page reloads after the write") fails. It was reproduced twice: with the paid run and with a free replay on a second panelboard. The fix is small: re-fetch the panel schedule and SCCR after apply.
