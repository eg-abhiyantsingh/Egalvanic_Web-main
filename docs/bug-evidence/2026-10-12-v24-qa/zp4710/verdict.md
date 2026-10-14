# ZP-4710: [Web] Asset readings, thermal baseline, direct record entry & flat IR photo storage (Tasks #125/#132/#133)

**Verdict:** FAIL (security, in scope). The features work end to end. But the new readings and thermal routes ignore which sites a user is allowed to see: users who cannot open an asset can still read its readings and thermal data, add and edit readings, and rename its thermal key areas.
Build `index-C14cwR0U.js` (QA, checked 17:33 and 19:12 IST, 13 Oct 2026). Frontend PR #1680 is in the bundle (Infrared tab, `AssetReadingsSection`, `ir-photos/direct`, Maintenance hover group).

Test data (mine): "QA-DEMO ZP-4710 switchboard (delete me)" https://acme.qa.egalvanic.ai/assets/cccad67b-134c-4c34-9ceb-437c762b2a28 on "Test site A". It holds:
- 3 IR photos (IR_0245, IR_0246, and a different file renamed IR_0245.jpg);
- 7 key areas from one Analyze run;
- readings labelled "QA-DEMO ZP-4710 …", including one written by each role probe.

Read only: team QA-DEMO asset "QA-DEMO ZP-4532 Oil XFMR (delete me)" `913be6ee-0aac-4056-a58e-990e28198a98` (Ultrasonic/Torque/Fluid tabs).
Paid AI: one Analyze with AI run (2 photos, 21 s).

## Checked
| Check | How | Result |
|---|---|---|
| Maintenance hover menu | Hover "Maintenance" | My switchboard (only IR eligible): Maintenance, Inspections, Infrared. The 913 asset (all eligible): + Ultrasonic 1, Torque Records 2, Fluid Samples 2. Only service-eligible sections show, matching `GET /node/<id>/record-sections`. PASS (shot -1) |
| Add a reading | Infrared › Readings › Add Reading: Current A 100, B 101.5, C 99, N 2, rating 225 | `POST /node/<id>/readings` 201; row "Current · A 100 · B 101.5 · C 99 · N 2 A · 45.1% load". PASS (shot -2) |
| Edit a reading | Pencil › A = 120 › Save changes | `PUT /readings/<id>` 200; after reload "A 120 … 53.3% load". Not tied to any Thermal Anomaly issue (`source: manual`, `source_issue_id: null`). PASS (shot -3) |
| Other reading types (API) | mV drop A/B, voltage A-B/B-C/C-A | 201 each, shown in the list. PASS |
| Reading validation (API) | 100001 A, −1, `bogus` type, point "A" on voltage, no values, year 2099 and 1900, 2001-char notes, rating 0, duplicate point, "abc" | All 400 with clear messages ("… is outside 0..100000 A — check the unit", "taken_at 2099-01-01 is after tomorrow"). PASS. 1.2345 A is accepted and stored as 1.234 (the UI blocks more than 3 decimals): hardening note only |
| Direct IR upload | Add IR Photos › IR_0245 + IR_0246 | One `POST /node/<id>/ir-photos/direct` 201 + `extract_visual/batch`. Cards say "Direct entry". `ir_session_id` null, `service_id` null. PASS (shot -4) |
| Flat storage keys | API `ir_photos[]` | `storage_key = ir-photos/<company>/<photo id>/ir.jpg`, visual `…/visual.jpg`. PASS |
| Same file twice | Upload IR_0245.jpg again | "Already on this asset" → kept once, no new row. PASS (shot -7) |
| Same name, different file | Another camera file renamed IR_0245.jpg | New row with its own key (`…/d50dd47c…/ir.jpg`); the first photo is untouched. No collision. PASS (shot -8) |
| Analyze with AI | 2 photos | `POST /node/<id>/thermal/analyze` 200 in 21 s; toast "2 photos analysed"; 7 named key areas. Latest °C, Change (e.g. −12.7 °C), Photos = 2, trend line. Photo cards draw no boxes. PASS (shot -5) |
| Compare + chart | Tick both photos › Compare; chart icon | "Compare thermal photos — 2 photos, oldest first", area picker, chart. PASS (shot -6) |
| Direct ultrasonic / torque / fluid | 913 asset tabs (read only) | "Add Scan", "Add Record"/"Add by AI", "Add Sample"/"Add by AI" offered. Earlier QA-DEMO direct entries show "Direct entry"/"Manual entry". PASS (shot -9) |
| Roles: offered vs refused (UI) | PM, EE, CP on the 913 asset | PM and EE: Analyze with AI, Add IR Photos, Add Reading. Client Portal: no add buttons (view only); `POST readings` 422 "Required: nodes.manage". PASS (shot -10) |
| **Site scope (API)** | PM, FM, EE (none has "Test site A"; positive control = Super Admin, same calls JSON) on my switchboard `cccad67b…` | **Asset page refused ("This asset link doesn't look right"), and `record-sections` refused, BUT `GET /node/<id>/readings` 200 JSON, `GET /node/<id>/thermal` 200 JSON (analyses, components, temperatures), `POST /node/<id>/readings` 201 (PM, FM, EE), `PUT /readings/<id>` 200 (PM edited my reading to 121 A), `PUT /thermal/components/<id>` 200 (PM).** FAIL (shots -11 … -15) |
| Other company's asset | Find a foreign node id | Not possible this run: the staff seat's `ir_session` list came back empty from the acme host. Not covered |
| Backfill from Thermal Anomaly JSON | Look for issue-backfilled readings (`source: issue_backfill`) | None found on the QA-DEMO assets; `/issues` gives no JSON from the browser. Not web-testable (see below) |
| Flags | Bundle | No LaunchDarkly gate on the Infrared tab, readings or direct upload. Nothing to flip |
| Performance | Infrared tab | Readings list 0.25–0.44 s, thermal 0.7 s (27 KB), Analyze 21 s for 2 photos (one blocking call; fine for 2 photos, see suggestions) |

## Bugs found
**B1 (High, backend), in scope: readings and thermal routes do not check that the user may see the asset's site.**
1. Sign in as a Facility Manager or Project Manager who is not assigned to "Test site A".
2. Open https://acme.qa.egalvanic.ai/assets/cccad67b-134c-4c34-9ceb-437c762b2a28. The app correctly says "This asset link doesn't look right" (shot -11 / -14).
3. In the same browser open https://acme.qa.egalvanic.ai/api/node/cccad67b-134c-4c34-9ceb-437c762b2a28/readings and …/thermal.
- What happens: both return the asset's data: every reading with values and notes, and the thermal analyses, key areas and temperatures (shots -12, -13).
  - The same users could also add readings (`POST /node/<id>/readings` 201), edit one (`PUT /readings/<id>` 200; PM changed 120 A to 121 A, shot -15) and rename a key area (`PUT /thermal/components/<id>` 200).
- What should happen: the same refusal as the asset page and `GET /node/<id>/record-sections` (which correctly refuses these users).
- Positive control: Super Admin gets the same data. The site is not in the seats' `accessible_sld_ids` (PM 116, FM 11, EE 4 sites).
- Not checked: another company's asset (no foreign node id was reachable this run). A dev should confirm the routes check the company too.
- Suggested assignee: Avani (backend; also covers the ticket's own caveat about `/ir_photo/update` and the extract-visual routes).

## Enhancement suggestions
- Readings: show the company-timezone label next to "Taken" (Acme is Pacific/Midway, so 18:32 IST shows as "2:02 AM", which confuses people in other zones).
- Analyze with AI on many photos: show per-photo progress. It is one blocking call (21 s for 2 photos).
- Readings list: add a filter by type (Current / mV drop / Voltage) and a per-type chart; the list mixes all types by date.

## Mobile part for Nency
iOS twin ZP-4711:
- direct IR/ultrasonic/torque/fluid entry on an asset;
- the FLIR camera app's legacy upload (typed photo numbers must still link web/SD-card uploads by name: "Negative" row of this ticket);
- photos uploaded on web should show on mobile with the new `ir-photos/…` keys.

## Not covered / NOT WEB-TESTABLE
- Backfill migration `irread_a1`: re-running it must not duplicate readings, and it must leave the issue JSON intact. Dev to confirm on QA data. No asset with Thermal Anomaly readings JSON was found.
- FLIR camera app legacy request bodies (mobile app path).
- Reporting lambdas reading the new keys / NULL `ir_session_id` (ticket says "handled separately").

## Proposed Jira comment
h3. QA: ZP-4710 features work, but the new readings and thermal API is open to users of other sites (QA build index-C14cwR0U.js, 13 Oct)

What works:
# The Maintenance tab is a hover menu, and only the record types the asset is eligible for show (Infrared only, or Infrared + Ultrasonic + Torque + Fluid).
# Add Reading and edit work and persist; invalid values are refused with clear messages.
# Add IR Photos works without a work order. Files are stored as {{ir-photos/<company>/<photo id>/ir.jpg}}. The same file twice is kept once, and a different file with the same name gets its own row (no overwrite).
# Analyze with AI named 7 key areas with temperature, change and trend; Compare works; photo cards draw no boxes.
# Client Portal sees the data but no add buttons.

What fails:
# Sign in as a Facility Manager or Project Manager who is *not* assigned to "Test site A".
# Open https://acme.qa.egalvanic.ai/assets/cccad67b-134c-4c34-9ceb-437c762b2a28. The app says "This asset link doesn't look right" (correct).
# Now open {noformat}https://acme.qa.egalvanic.ai/api/node/cccad67b-134c-4c34-9ceb-437c762b2a28/readings{noformat}
# *Actual:* all readings and the thermal data of that asset come back. These users can also add a reading, edit a reading and rename a key area on it.
# *Expected:* refused, like the asset page and {{record-sections}} already are.

!QA-13Oct-ZP-4710-5-analyze-key-areas-change-trend-no-boxes-on-cards-PASS.png|width=720!
!QA-13Oct-ZP-4710-11-facility-manager-cannot-open-asset-page-PASS.png|width=720!
!QA-13Oct-ZP-4710-12-same-user-reads-readings-of-that-asset-via-api-FAIL.png|width=720!

Not web-testable, please confirm: the readings backfill from Thermal Anomaly JSON is idempotent, and the FLIR app's legacy upload still pairs by photo number (Nency, iOS ZP-4711).

*Status:* moved to To Do

## Recommended status
To Do. The new readings/thermal routes have no site check (reads and writes), proven with three seats and a staff positive control. Everything else passes and can be re-checked quickly after the fix.
