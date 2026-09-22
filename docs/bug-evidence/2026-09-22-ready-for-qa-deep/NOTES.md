# Ready-for-QA deep pass — 2026-09-22 (bundle index-C9NJAR1x.js, +admin seat)

48 tickets in Ready for QA at the start of this pass.

## ZP-4218 — Quotes: RFQ button does not generate when a labor line item is marked subcontracted
**VERDICT: OPEN, and the real cause is one level below the ticket.** A labor line cannot be marked subcontracted at all on web, so the RFQ flow it gates can never appear.

Steps (own QA-DEMO quote, nothing else touched):
1. Sales -> Quotes -> All (/opportunities, 1-10 of 205).
2. Opened "QA-DEMO ZP4220 v2 EMP priced (delete me)" -> /plans/b567d2a6-a8e6-43d4-b411-5c88d5d510f0.
3. LABOR table: columns Type / Sub / Rate / Est / Billed / Cost / Sell. Rows "Journeyman Electrician" and "NETA Technician", Sub = **No** on both.
4. Clicked the Sub cell on Journeyman Electrician -> dialog "Convert to subcontracted labor", whose own body says: "As subcontracted labor, Journeyman Electrician needs per-work-order quotes - use the **Generate RFQ** flow in the **Rate** column to request and record them."
5. Clicked **Convert**.

What happens: the dialog closes, POST /api/plans/{id}/generate returns **200**, the plan reprices - and Sub stays **No**. Reloaded the page: still No. No RFQ control anywhere in the Rate column or the page (case-insensitive search for "RFQ" in the rendered page = 0 hits). Repeated 3x.

ROOT CAUSE (captured off the wire):
- Request `pricing` block: `{"rate_overrides":{}, "subcontracted":[], "subcontracted_overrides":{"e08f8c64-7f48-47db-928a-b23946aab5ab":true}}`
- Response for that line: `"subcontracted": false`
The frontend records the conversion ONLY in `subcontracted_overrides`; the `subcontracted` array it sends alongside is **empty**, and the server answers with the flag false. So the conversion never lands.

For the developer: POST /api/plans/{plan_id}/generate, `pricing.subcontracted` vs `pricing.subcontracted_overrides`. Plan b567d2a6-a8e6-43d4-b411-5c88d5d510f0, labor rate id e08f8c64-7f48-47db-928a-b23946aab5ab. -> 01

## ZP-4207 — 502 when re-interpreting a Journal Site Walk created on mobile
**VERDICT: the 502 does NOT reproduce. A different, blocking problem sits in front of it.**

/site-walks has 50 walks, 3 carry the JOURNAL chip:
- `ac758601-6cb0-49a7-8992-b1ff4806d167` "14 augest abhiyant" — open walk, HAS an **Interpret** button plus Add location / Add note / Add photos.
- `5b85ca51-833a-4622-8146-83e6ec2c9a73` "14 augest abhiyant" — header "Read 15/09/2026", real content (typed note "Hhhh" + 2 photos). **No Interpret button.**
- `125fef95-a472-410b-b30f-97be099fd807` "18 aug abhiyant" — header "Read 17/09/2026", real content (1 photo + 0:01 voice "Hi hello" + typed note). **No Interpret button.**

1. Clicked **Interpret** on the only walk that offers it:
   POST /api/site-walk/{id}/interpret -> **400** `{"code":"nothing_to_interpret","error":"this walk has no entries to interpret yet"}`,
   toast "Add a note or a photo first — there's nothing to interpret yet." No 502, and the error copy is good.
   That walk's entries are all "PHOTOS 0 photos", so the server is right and this is not a valid test of the ticket.
2. The two walks that DO hold entries are the ones with no Interpret control, so a re-interpret cannot be started from web at all on this tenant.

**Separate finding worth a ticket:** on a walk marked Read, the INTERPRETED MODEL panel still tells the user "Nothing has been read out of Unnamed location yet. **Run Interpret** to turn its entries into assets, quantities and condition." while the page offers no Interpret control anywhere (3 clickable elements in `main`: back, list toggle, graph toggle). The instruction points at a button that is not there. -> 02

To settle ZP-4207 QA needs a journal walk created on the mobile app, with entries, left in a state where web still offers Re-interpret.

## ZP-3675 — Panel Schedule assets missing from the exported PDF
**VERDICT: cannot reproduce on acme QA — no panel on this tenant has circuits to lose.**

1. /panel-schedules lists 51 panels.
2. Opened `11N-H1-1` (532b10d9-...). Editor + View both show one off-panel breaker, "Fuse 1", and no circuit assignments.
3. Ran Export PDF (US Letter, "Include phase load columns" on) and read the generated PDF: 60,867 bytes, 104 text items. It contains `OFF-PANEL LOADS / Description / Bkr / P / Load Type / Fuse 1 / 1P`, then a `CIRCUIT SCHEDULE` grid of 42 empty positions and `Total Load 0 VA`. **The one asset on screen is present in the PDF.** Nothing missing.
4. Panels `11N-H1-2` (eabdd440), `13N-H1-1` (34e6c1f7) and `12N-H1-2` (6e4b2626): the editor states "Get this panel started — This panel doesn't have any circuits yet." They are genuinely empty.

To settle this ticket QA needs a panel whose circuits are populated. None exists on acme.

**Minor finding while here:** the read-only View Schedule page for an empty panel renders a completely blank card (main text = "Panel Schedules Export PDF Edit", 31 characters) with no empty state, while still offering **Export PDF**. The editor for the same panel shows a proper "Get this panel started" empty state. -> 03 (blank view), 04 (editor empty state)

**Note on the download:** the export writes a file. I intercepted `URL.createObjectURL` and the anchor click to keep it in memory, and the interception did not hold — `~/Downloads/11N-H1-1_Panel_Schedule.pdf` (60,867 bytes) was written. Flagged to the owner; no further exports were run.

## ZP-4261 — photo actions recorded duplicate activity-ledger rows
**VERDICT: not verifiable from the web UI.** The ledger has no page of its own. Admin -> Activity Logs is the API request log (Time / Trace ID / Origin / Activity / Request / Status / Reason / User / Facility) and Admin -> Audit Log is the offline Mutation Audit Log (3 rows, all `Mapping_user_session` / `Session` creates). Neither lists photo ledger rows. Settling this needs a photo added through the UI and a direct count of ledger rows for that action.
**Observation while there:** /admin/activity-logs shows "REQUESTS 520 — Showing the last 30 days" in its header tiles while the grid below reads "No rows / 0-0 of 0". Two numbers for the same window on one page. The page does carry a banner "Showing this server process only — not authoritative".
**Route note:** the working route is `/admin/activity-logs`. Plain `/activity-logs` renders a blank page (main innerText length 0) under the Site Data rail rather than redirecting or 404ing.

## ZP-4266 — Edit with AI on a report config does nothing and the button changes to "Try again"
**VERDICT so far: does NOT reproduce on QA. The flow starts correctly.**
1. Builder -> Reports (/reporting/builder, 1-25 of 112 configs).
2. Opened my own config `QA-DEMO fork regression ZP-staff-write (delete me)` (fd94f09c-..., Plan (EMP), HTML, 10 pages, Ready to Use). Its live preview renders.
3. **Edit with AI** is present. Dialog: "Describe the change — restructure pages, restyle a template, add a section", a request box, "Rendering against Site Walk — QA-DEMO ZP3978 Site (delete me) — the AI sees the same preview you do", optional reference attachments, Cancel / **Apply with AI**.
4. Submitted "Change the cover page heading colour to dark grey. No other changes."
5. POST /api/reporting/configs/{id}/ai-edit -> **200**, a job starts; /ai-edit/status polls return `"Starting…"` then `"Executing command…"`; the dialog shows that progress and explains the edit keeps running if closed. **No "Try again", no silent failure.**

**Important precondition the ticket does not state:** the **Edit with AI** button only appears on a config that has pages. On an empty config (`stabdard test`, 0 pages) there is no such button at all, and its preview panel reads "Preview unavailable — Data preparation failed: Report generation failed: No data available to generate this report." -> 05

**Third instance of the same infrastructure leak:** the ai-edit response body carries `execution_arn` = `arn:aws:states:us-east-2:165183897698:execution:eg-pz-qa-ai-service-spec-sfn-...`, exposing the AWS account number on a success path. Same class as ZP-3834, now on `/api/reporting/configs/{id}/ai-edit` as well as `/api/form-fill/jobs/{id}/status`.

## ZP-3677 — Add Assets Service field does not allow multiple service selections
**VERDICT: FIXED. The field takes multiple services.**
1. Sales -> Quotes -> All -> opened plan `QA-DEMO ZP4220 v2 EMP priced (delete me)` (/plans/b567d2a6-...).
2. **Planned Work** -> clicked **2026 5-Year Shutdown** (429 lines, 321.33h) -> dialog with **Add Assets** and **Bulk edit**.
3. **Add Assets** -> "Add assets — Pull assets onto 5-Year Shutdown. They get each service's standard procedures." with `Service *` and `Assets *`.
4. The Service placeholder reads "Which services are they getting?" (plural). Picked **Clean, Tighten, Torque** -> it becomes a removable chip and the list stays open.
5. Picked **Infrared Thermography** as well -> the field now holds **two chips side by side**: `Clean, Tighten, Torque` + `Infrared Thermography`, each with its own remove ×. All six services on this plan are offered (Clean Tighten Torque / DGA Fluid Sample Analysis / De-Energized Visual Inspection / Infrared Thermography / NETA Testing / UPS Maintenance).
Cancelled without adding, so nothing was written. -> 06
**ZP-4266 outcome (same run, ~3 min later): the edit APPLIED.** Dialog: "Edits applied • Changed the cover page's main heading ("Electrical Maintenance Program") from white to dark grey (#4B5563), matching the secondary text tone already used elsewhere on the cover. No other pages or templates were touched." Status endpoint: `"message":"Ready"`, `finished_at: 2026-09-22T13:26:18`. Button reads **Close**, never "Try again". The production symptom does not occur on QA. -> 07

## ZP-4186 — Sentry: HTTP 500 at CoreAttributesEditorDialog handleExtract
**VERDICT: does NOT reproduce.** /assets -> row actions -> Edit Asset drawer -> **Extract from Photos** (the handleExtract path). Asset `11N-H1-1`, Panelboard, Nameplate (1) photo.
POST /api/extraction/extract-nameplate-data -> **200** `{"errors":[],"fields_updated":0,"lambda_failed":0,"lambda_succeeded":1,"processed":1,...}` and a readable toast "No data was extracted". No 500, no unhandled error. -> 08

## ZP-4185 / ZP-4183 — Sentry auto-files
- **ZP-4185** (`APIClient._doRefresh` 400 during `initializeAuth`): a token-refresh failure. Across this whole session (two seats, ~90 minutes, dozens of navigations) no 400 was seen on any auth route. Cannot be forced from the UI without an expired/!revoked refresh token. Description is a stack trace only.
- **ZP-4183**: the stack is Kotlin (`FlushSyncQueueUseCase`) — the Android app's sync queue. **No web surface.**

## Re-confirmed on today's bundle (index-C9NJAR1x.js) — the 21 Sep walk-throughs, re-run on a site WITH data (Android Site 2, 343 assets)
- **ZP-4043** — Equipment Health band carries the **"Assessment expired"** bucket: "0 Healthy · 0% / 0 Assessment expired · 0% / 7 At risk · 2% / 336 Not assessed". PASS.
- **ZP-4084** — Condition Distribution = `Condition 1` 297 / `Condition 2` 11 / `Condition 3` 1 / `Non-Serviceable` 6, "315 assessed". The word "Serviceability" appears nowhere on the page. Single vocabulary. PASS.
- **ZP-4041** — "8 assets had their condition revised automatically in the last 30 days" now renders (was "No condition was revised automatically" on the empty site yesterday), so the auto-revision surface works with real data. PASS.
- **ZP-4039** — Condition Assessment tabs `Overview / Findings 35 / Asset Details 343`. PASS.
- **ZP-4113** — both `Export issue report` and `Export assessment report` in the Condition Assessment header; Maintenance -> Reports still carries the dedicated **Issue Report** card alongside Condition Assessment / EMP Lite / Arc Flash Readiness and the Work order reports table. PASS.
- **ZP-4112** — Compliance tabs `Overview / Deviations / Acknowledgements / Program Elements 11 / Visualizer`. PASS.
- **ZP-4045 / ZP-4060** — Compliance shows "COMPLIANCE SCORE — 0 of 0 pairs resolved — before acknowledgements" and **Coverage by service** split into **In program / Acknowledged / Needs attention**, which is exactly the split ZP-4060 asks for. The bars are present; the site has 0 deviations under the chosen standard, so the numbers are all zero. Structure PASS, data cannot exercise the values.
- **ZP-4044** — Maintenance Program has the `Search assets…` box plus Record completion / Defer / Filters / Export and a Group-by / Axis sub-nav. PASS.
- **ZP-4109** — custom service `corrective IR` shows the **By class | All methods** toggle and `Add class`. PASS.
- **ZP-4152** — /pm-plans: 11 standards; every company-owned row exposes 3 actions (star / edit / delete) and the global `NFPA 70B 2026` (228 plans) exposes **0**. Only company-owned rows can be made default. PASS.
- **ZP-4110** — /devices filter tray = `Manufacturer / Asset class / Service / Frame / Rules`, and **Add a device** cascades Service -> "Pick a service first" -> "Pick the asset class first — it decides which part of the library to search" -> Continue to rules. Verbatim match. PASS. -> 11
- **ZP-4068** — New Site Walk dialog offers **Count** ("Tally assets by class and location. Quantities and pricing straight away.") vs **Journal** ("Talk and shoot as you go. The assets, condition and feeds are read out of it for you."), plus Services* / Site / Walk name. PASS. -> 12
- **ZP-4131** — SLD Export menu = Export PDF / Export DXF / Export Engineering XML / **Settings Verifier** / Export JSON / Export Ground Fault JSON. Settings Verifier sits directly after Export Engineering XML, as specified. PASS. -> 13
- **ZP-4066** — the SLD issues panel opens and lists 19 issues (1 error, 18 warnings): "Illegal children on DC Bus 1", "Buses connected without impedance" ×n. **No "upstream can't deliver" and no phase-config error anywhere in the panel.** The analyzer is running and producing real issues, and none of them is the false type the ticket removed. PASS. -> 14
- **ZP-4167 / ZP-4174** — positive and negative control on today's bundle. Busduct asset `2nd Asset with photo`: Engineering = "Trust the Photos" + **System Voltage only**, then CORE ATTRIBUTES (Electrode Configuration, Enclosure Depth/Height/Width). No Mains Type, no Manufacturer, no Panel Type, no SCCR. Panelboard `11N-H1-1`: all of Asset Subtype / System Voltage / Phase Configuration* / Mains Type* / Manufacturer / Panel Type / SCCR present. The hide is scoped to the bus classes only. PASS. -> 09 (Panelboard), 10 (Busduct)
- **ZP-4171** — the CORE ATTRIBUTES block renders inside the Engineering section with subtype-specific attributes. PASS (present; the per-subtype mask itself needs a class with two subtypes carrying different masks).
- **ZP-4038** — Dashboard is single-site with the **Open Issues by Type** card beside Assets by Type. PASS.

## Not executed on purpose
- **ZP-4062** — the bulk "Mark As…" **confirm** writes completion state to a shared work order. The menu-scoping half was verified 21 Sep; the confirm half stays unexecuted. It needs a throwaway work order.
- **ZP-4082** — the ticket says the fix exists on cicd/prod only and is absent from dev/qa. Nothing to test here.

## Asset Classes regression — third independent reproduction today
Cold load of /asset-classes in a fresh tab: "No rows", pager **0–0 of 0**. Clicked the grid's own refresh icon: **1–25 of 49**. Same as the two earlier reproductions this morning and as 21 Sep on the previous bundle.

## ZP-4086 — condition-section attributes
Opened Admin -> Asset Classes -> **Circuit Breaker** (a GLOBAL class). The editor shows Name / Key / Description / Icon / Orientation / Device Role / Classification (Box OK, OCP OK, Needs Source) / **Core Attributes + Add** / PM Plans / Asset Subtypes. No `Condition section` Listed/Required control is offered — consistent with the ticket's own statement that globals are disabled. Settling this needs a **company-owned** class that already carries core attributes. Present-unexercised, unchanged from 21 Sep.

## ZP-4040 — NULL service wo_view falls back to the service type's UI
Partially confirmed: work order `IR WO 22 09` (/sessions/bbe66d36-..., service Infrared Thermography) renders the **IR** view — tabs `Assets 2 / Issues / IR Photos / Attachments` and an `IR Photos` column with per-asset check boxes — not the `General` view. That is check 1 of the ticket. The 3 PM Forms services and the unknown-type negative control were not exercised.

## ZP-4061 — Maintenance Portal read-only Site Data + Work Orders
**BLOCKED on this seat.** Every /maintenance-portal/* route answers with the Access Denied card for both staff seats tested, because the portal is gated on the Portal Sales role (ZP-4138) which no QA seat holds. None of the six checks in the ticket can be run until a Portal Sales seat exists.

## Tickets with no web surface
ZP-4067 (AI pipeline journal_fold runner) · ZP-4176 (branch merge chore) · ZP-4183 (Android Kotlin sync queue) · ZP-4190 (AI-pipeline PR review) · ZP-4216 (Alembic revision graph) · ZP-4082 (states it is on cicd/prod only) · ZP-4128 (asset-agent run).

## Tickets blocked on test data
ZP-4127 (needs a device with a continuous SKM segment, e.g. Powerpact J-Frame) · ZP-4153 (needs an interpreted journal unit) · ZP-4086 (needs a company-owned class with core attributes) · ZP-3675 (needs a panel with populated circuits) · ZP-4207 (needs a mobile-created journal walk still offering Re-interpret).

## Carried from the 21 Sep walk-through, area unchanged
ZP-4149 (IR-typed work order resolves the shared typed IR config in the report modal) · ZP-4170 (6 of 6 seeded transformer makers; CEB in the bus/panel picker with 1 designation).
