# ZP-4801 — Task #160: IR photos `thermally_tuned` flag + Tuned checkbox in the work order (TKT-1439)

**Verdict:** FAIL. Tested on QA build `index-COeCBn0z.js`, 13 Oct 2026, about 13:40 to 14:35 IST. The build was checked at the start and again before this verdict.

Most of the ticket works: the column, tick and untick, saving after reload, pages 2 and 3 of the grid, the revert and toast when a save fails, the permission check, and checking the request body.

Two things fail:
1. On a **closed work order** the Tuned checkbox can still be clicked, and the click is saved. The QA Review asks for this column to be locked like the rest of the tab.
2. A **quick double-click** can leave the screen showing "not tuned" while the server saved "tuned".

Test data, all created by me and labelled: work order **QA-DEMO ZP-4801 Tuned column (delete me)**, https://acme.qa.egalvanic.ai/sessions/22a22b9c-4eda-43ed-b131-4b7146912441 (site Android Site 2). It has 22 IR photos (IR_0018…IR_0279). I created the photo rows with `POST /ir_photo/create` and uploaded the files with the web "Upload IR Photos" dialog. All 22 photos were set back to `thermally_tuned=false` at the end, and the work order was re-opened (it is active).

On photos I did not create, I only sent "write the same value" requests (no change to data). No other record was changed.

## Checked

| Check | How | Result |
|---|---|---|
| Column placement | Opened the work order › IR Photos tab | PASS. Columns are IR Photo · Visual Photo · **Tuned** · Linked Issues · Asset Name…; hovering the header shows "Thermal image level/span tuned for the report" (shot 1) |
| Tick saves | Clicked Tuned on IR_0279 | PASS. Ticked straight away; `PUT /api/ir_photo/<id>/thermally-tuned {"thermally_tuned":true}` (with `X-Direct-Write`) → 200 + full photo row; `thermally_tuned:true` in `photos/v2`; still ticked after reload (shot 2) |
| Untick | Clicked again | PASS. PUT false → 200; false after reload |
| Save fails (500) | Playwright route answered the PUT with 500 | PASS. The tick reverts and a red toast shows the server message (shot 3) |
| Save fails (network abort / 403) | Route abort; route 403 | PASS. Reverts; toasts "Failed to fetch (acme.qa.egalvanic.ai)" and "Forbidden: missing permission ir_photos.upload" |
| Page 2 / 3 of the grid | Rows per page = 10 (22 photos → 3 pages); ticked IR_0249 on page 2, went back and forth | PASS. Each page loads with `photos/v2?limit=10&offset=10`; the tick is still there (shot 4) |
| **Closed work order** | Closed my work order with "Close Work Order"; IR Photos tab | **FAIL.** The tab is dimmed (opacity 0.6, `pointer-events:none`), but the Tuned checkbox has its own `pointerEvents:"auto"`. It is the top element under the mouse, stays enabled, and a click sends PUT → 200 and saves `true` (shot 7). The Upload button and the rename pencil cannot be clicked, as expected. The same can be clicked on the existing closed work order "Infrared Thermography (I.R)" (fcc37c67…): I only checked what is under the mouse there and did not click. The backend also accepts the PUT on a closed work order (200); the ticket only promises a UI lock. |
| **Rapid double toggle** | Real `dblclick` on the checkbox, 6 + 10 tries | **FAIL.** The two PUTs (true, then false) run in parallel. In 3 of 16 tries the replies came back in the wrong order, so the server kept **true** while the grid shows **unticked**. Reload shows ticked (shots 5, 6). Two clicks 300 ms apart: 4/4 correct. |
| Keyboard toggle | Tab to the checkbox (focus reaches it: IR Photo → Visual Photo → Tuned), then Space / Enter | **FAIL (Low, accessibility).** Space does nothing: the DataGrid cancels the keydown (`defaultPrevented=true`), so no PUT is sent. A keyboard-only user cannot tick Tuned. |
| No permission → disabled | **No real seat can open a work order without `ir_photos.upload`** (see roles below). SIMULATED: PM seat with `ir_photos.upload` removed from `/api/features/access` and `/auth/v2/me` | PASS (SIMULATED). 9/9 checkboxes disabled, "Upload IR Photos" hidden, a click sends no PUT (shot 8). Note: SA/Admin seats are `is_admin`, so the web shows them every control whatever permissions they hold. |
| API: permission | Client Portal seat (`workorders.view` only) PUT | PASS. `422 permission_denied … Required: ir_photos.upload`. Portal Sales: 422. |
| API: body checks | On my photo: "yes", "true", 1, 0, null, `{}`, `{tuned:true}`, form-encoded text, no body, `{"thermally_tuned":{"v":true}}`, `[]` | PASS. All → `400 "thermally_tuned must be true or false"`, and the value did not change |
| API: body that is not a JSON object | JSON body `[true]`, `"true"`, `123` | **FAIL (Low).** `500 internal_error` (trace ids 81f37795…, af761236…, fe2e3159…). It should be a 400 like the others. |
| API: bad / unknown ids | `abc`, `123`, random UUID, all-zero UUID, 300-char id, SQL-ish id, a work-order id used as a photo id | PASS (no 500). The CDN shows them as 200 HTML; the staff request log (Admin › Activity Logs API) records the real status **404**. GET/POST on the route → 405. |
| Other tenant's photo id → 404 | Looked for a foreign photo id. Staff seat: `/api/sld/<other company site>/sessions` → `422 permission_denied`; the old `/api/ir_session?limit=` listing now returns HTML; the request log only holds acme traffic | **NOT COVERED.** No other company's IR photo id can be reached on QA from the acme host. The nearest proof is that unknown ids give a real 404. |
| Site scope inside the company (extra) | Customer seats sent a same-value PUT (no data change) to photos on 8 acme sites they are **not** assigned to, and read `photos/v2` of the same work orders | **Gap (Medium).** EE (4 assigned sites), FM (11 sites), PM, AM and Technician all get **200 + the full photo row** from the PUT on unassigned sites. The read of the same work orders is refused for EE/FM (HTML fallback). FM can PUT on my work order's photo although it cannot open that work order. The ticket only promises tenant scope, so this is reported as a separate gap. It is the same family as ZP-4849 and ZP-4709. |
| Activity event `ir_photo.thermally_tuned` | Activity Logs API search (thermally / ir_photo / photo id), `/entity-history/ir_photo/<id>` | NOT VISIBLE ON WEB. Activity Logs is a request log (200/400/404 rows only, no domain event); entity-history answers "unsupported entity type". The backend team needs to confirm it. |
| Roles (real seats, `/auth/v2/me`) | 8 seats signed in | Super Admin/Admin/PM/Technician: upload+delete. AM/EE/FM: `ir_photos.upload`. CP: none (PUT 422). Portal Sales: none, web restricted. **EE, AM and CP get "Access Denied" on `/sessions/<id>`, and FM gets "Unexpected token '<'" on a work order outside its sites.** So in the web only SA/Admin/PM can see the column. The API agrees with the permission list. |
| LaunchDarkly | Bundle: the Tuned column is in the column list with no `Po/Gr` flag; live evalx (Acme QA) | No flag gates this feature. The IR Photos tab is not behind a new flag. **No flip needed.** |
| Performance | IR Photos tab load; PUT timing | 38 API calls per work-order page load, no duplicates, none over 2 s; `photos/v2` 708–747 ms, 3.6 KB. PUT 230–700 ms; the reply is the full photo row (~1.2 KB). |
| Migration | Every photo row returned has `thermally_tuned` (default false on old photos too, e.g. ebff86ed… from 9 Oct) | PASS (seen through the API) |

## Bugs found (none of these is covered by an existing Jira ticket; searched 13 Oct)

1. **[Web] IR Photos: the Tuned checkbox can still be ticked on a closed work order** (Medium, Krunal/frontend; backend accept is optional for Avani)
   1. Open a work order with IR photos and click **Close Work Order** › Close Work Order.
   2. Open the **IR Photos** tab. The tab is greyed out.
   3. Click a **Tuned** checkbox.
   - Actual: it ticks; `PUT /ir_photo/<id>/thermally-tuned` → 200 and the value is saved.
   - Expected: the column is locked with the rest of the tab (QA Review row 4).
   - Cause seen in the bundle: the checkbox `sx:{pointerEvents:"auto"}` cancels the tab's `pointer-events:none`, and `disabled` checks only `ir_photos.upload`, not whether the work order is closed.
   - Evidence: `QA-13Oct-ZP-4801-7-closed-wo-tuned-still-clickable-FAIL.png`
2. **[Web] IR Photos: double-clicking Tuned can show "not tuned" while the server saved "tuned"** (Low-Medium, Krunal)
   1. Double-click a Tuned checkbox quickly.
   - Actual: two PUTs (true, false) run in parallel. When they finish in the other order, the server keeps true while the box shows unticked. Reload shows it ticked.
   - Expected: what you see matches what is saved. Ignore clicks while a save is running, or send only the last value.
   - Evidence: `…-5-double-click-shows-unticked-but-server-saved-ticked-FAIL.png`, `…-6-after-reload-row-is-ticked-FAIL.png`
3. **[Web] IR Photos: the Tuned checkbox cannot be toggled with the keyboard** (Low, Krunal)
   - Tab reaches the checkbox, but Space does nothing because the DataGrid cell keydown cancels it.
   - Expected: Space toggles it, as with any checkbox.
   - Evidence: API/DOM log (no screenshot).
4. **[API] `PUT /ir_photo/<id>/thermally-tuned` returns 500 when the JSON body is not an object** (Low, Avani)
   - Bodies `[true]`, `"true"` or `123` → `500 internal_error`.
   - Expected: 400 "thermally_tuned must be true or false".
5. **[API] Customer users can set Tuned on IR photos of sites they are not assigned to** (Medium, Avani)
   - EE seat (4 sites) PUT on photos of 8 unassigned acme sites → 200 + full photo row.
   - The same seat cannot read those work orders' photos.
   - Same family as ZP-4849 (SKM import on an unassigned site).

## Enhancement suggestions
- Add a "Tuned" filter and a count to the IR Photos tab (for example "Tuned 5/22"), so back office can see what is left before the report.
- Add bulk "Mark tuned / Mark not tuned" for the selected photos (a 200-photo work order exists on QA).
- Let the Tuned column be sorted. The grid now has `disableColumnSorting`.
- Show who tuned the photo and when (a tooltip on the tick), because there is no visible audit for this flag.
- Show a small "Tuned" badge in the asset's Infrared tab and in the IR report, so the flag is visible outside the work order.
- Separate note, not this ticket: on a FLIR-SEP work order, "Upload IR Photos" fails with "no row in this work order names this file" for every file until photo numbers exist. Clearer text would help, for example "Enter the photo number on the asset first".

## Mobile part for Nency
- Check that the iOS/Android IR photo lists don't break on the new `thermally_tuned` field, and that they don't drop the value when the app writes a photo back (offline sync).
- Check whether the mobile apps should show Tuned at all. The ticket is web-only, so it is fine if they don't.

## Not covered
- **Other tenant's photo id → 404:** no foreign photo id is reachable on QA (see table).
- **`ir_photo.thermally_tuned` activity event:** not visible in any web screen or API.
- **Migration `irtuned_a1` linear heads:** backend only. Only the column and its default were checked.
- **Disabled state for a real seat without `ir_photos.upload`:** no QA seat both opens a work order and lacks the permission, so this was checked SIMULATED only.

## Proposed Jira comment
```
h3. QA result on QA (build index-COeCBn0z.js, 13 Oct 2026): FAIL, 2 problems

Test work order (mine): QA-DEMO ZP-4801 Tuned column (delete me), 22 IR photos.

||Check||Result||
|Tuned column after the image columns, hint on hover|PASS|
|Tick: PUT 200, still ticked after reload / untick: false|PASS|
|Save fails (500 / network / 403): tick goes back + toast|PASS|
|Page 2 and 3 of the grid|PASS|
|No ir_photos.upload: box disabled (simulated) / Client Portal API call 422|PASS|
|Bad body ("yes", null, missing key): 400; unknown id: 404|PASS|
|*Closed work order: Tuned can still be ticked and is saved*|*FAIL*|
|*Quick double-click: box shows unticked, server saved ticked*|*FAIL*|

*1. Closed work order*
# Close a work order that has IR photos.
# Open IR Photos (the tab is greyed out).
# Click a Tuned box: it ticks and PUT returns 200.
Expected: Tuned is locked like the rest of the tab. The checkbox has pointerEvents "auto", so it gets around the tab lock.
!QA-13Oct-ZP-4801-7-closed-wo-tuned-still-clickable-FAIL.png|width=720!

*2. Double-click*
# Double-click a Tuned box.
# Sometimes the box shows unticked but the server saved ticked (3 of 16 tries). Reload shows ticked.
!QA-13Oct-ZP-4801-5-double-click-shows-unticked-but-server-saved-ticked-FAIL.png|width=720!
!QA-13Oct-ZP-4801-6-after-reload-row-is-ticked-FAIL.png|width=720!

Smaller points (separate tickets): Space key does not toggle Tuned; body [true] or "true" gives 500 instead of 400; EE/FM can set Tuned on photos of sites they are not assigned to.
Not testable on QA: other company's photo id (none reachable); the ir_photo.thermally_tuned activity event (not shown anywhere on web).
```

## Recommended status
**To Do.** The closed-work-order lock fails, and I reproduced it on my own work order with a saved value.

## Evidence files (this folder)
1. `QA-13Oct-ZP-4801-1-tuned-column-after-image-columns-PASS.png`
2. `QA-13Oct-ZP-4801-2-ticked-persists-after-reload-PASS.png`
3. `QA-13Oct-ZP-4801-3-forced-500-reverts-with-toast-PASS.png`
4. `QA-13Oct-ZP-4801-4-page-2-tick-persists-PASS.png`
5. `QA-13Oct-ZP-4801-5-double-click-shows-unticked-but-server-saved-ticked-FAIL.png`
6. `QA-13Oct-ZP-4801-6-after-reload-row-is-ticked-FAIL.png`
7. `QA-13Oct-ZP-4801-7-closed-wo-tuned-still-clickable-FAIL.png`
8. `QA-13Oct-ZP-4801-8-SIMULATED-no-upload-permission-tuned-disabled-PASS.png`

Scripts and raw JSON: `.playwright-mcp/v24/zp4801-4717/` (b1, b2, c1, c2, roles r1/r2, f1).
