# Web v2.2 re-check — running notes (2026-09-22, bundle index-C9NJAR1x.js, admin seat, user Chrome session)

## Scope
- Jira fixVersion 14156 now 69 tickets (was 42 on 21 Sep). 27 added overnight (Krunal 23:56, Avani 23:58, misc to 05:11).
- Bundle: index-BOaMecwk.js (21 Sep) -> index-C9NJAR1x.js (22 Sep).

## Probes
- ZP-4042: GET /api/reporting/history?limit=5 -> 500 internal_error trace 2de0ac923d4f48288f8c36026a9d5ec8; /api/reporting/configs?limit=1 -> 200. 4th day.
- /api/lookup/node-classes -> 200, 47 rows.
- well-known: apple-app-site-association 200 json 607B; assetlinks.json 200 json 2098B (ZP-4030 backend half present).

## Asset Classes regression — SURVIVES the new bundle (index-C9NJAR1x.js)
- Fresh full load of /asset-classes in a second tab, "Set up later" clicked, grid = "No rows", "0–0 of 0". GET /api/lookup/node-classes in the same session = 200, 47 rows.
- Resource timing after mount: /api/users/{id}/slds, /api/entitlements, /api/action-items/counts, sales-attention, ops-attention, issues/open-by-site, devrev/session-token, timezone — NO classes call. Same as 21 Sep on BOaMecwk. Two bundles, two days.

## ZP-4189 LCP (In Progress; PR #1500 merged 03:15 today)
- Dashboard reload, PerformanceObserver buffered LCP = 7176 ms, element = greeting H5 "Good afternoon, abhiyant!". TTFB 1363 ms, DCL 1728, load 1735. Ticket measured 5.8 s on BTBN1KL5. Still Poor (>4 s). Site = Android Site 2 (343 assets / 52 WOs / $9,505).

## Fill-from-Photos jobs (ZP-3834 / 3783)
- GET /api/form-fill/jobs -> 405 (no list). Job 6293a4f4 (proposal used 14 Sep) is now status "applied": proposal.gaps = 0, so ZP-3783 needs a fresh review-state job to re-test.
- Job 2617c325 status "applied": status body STILL contains "arn:aws" + AWS account 165183897698 (not in job.error; locating the field).

## Login page (ZP-3919 / ZP-3920 passwordless) — isolated context, logged out
- Form = Email Address + Password + Sign In + Forgot your password? + Language EN/FR. No "Email -> Continue" step, no passkey, no "Continue with Google", no backup-code entry. Bundle carries 5 passkey/webauthn strings, so the UI exists behind per-company auth config; acme QA is not configured for it. Cannot be settled on acme. -> 04

## ZP-4138 Portal Sales gate (admin seat = Super Admin + EE + PM + AM + Admin, NO Portal Sales)
- Rail still shows the "Maintenance Portal" tile for this staff seat; clicking it -> /maintenance-portal renders "Access Denied — You do not have permission to access this page". Route half gated, nav half leaks (link to a denied page). Same as 21 Sep. Bundle: R7o="Portal Sales" constant present.

## ZP-4165 Customers tree (Ready for QA)
- API: GET /api/customers/tree/v2?limit=5&offset=0 -> 200 {accounts, has_more, limit, offset, q, success, total}; &q=android -> 200 filtered to "Android" account. Legacy /api/customers/tree still 200 (171 KB, unpaged).
- UI /customers: paged grid "1–25 of 102", search placeholder "Search customers and sites...", frontend calls /customers/tree/v2 (bundle: limit/offset + q set on the URL).
- ZP-4165 UI: typed "android" into "Search customers and sites..." -> 2 calls to /customers/tree/v2 (0 to legacy /tree), pager "1–25 of 102" -> "1–7 of 7"; results include the "Android" account AND accounts whose SITES match (Default EG-ACME Account expanded: abhiyant Android, Android data loss offline, Android Fr new, Android QA Site 1, Android old site, android SLD V3, Android Test nauu, Test Site Android (Dev Only)). Server-side search on account+site names, auto-expand on site match. PASS. -> 06
- ZP-4111 probe 1: /api/equipment-catalog/quick-search?q=FA&limit=100 (no type) -> 400 "type must be one of panelboard, switchboard, disconnect_switch, circuit_breaker, fuse, relay, transformer, cable, busway" for every sources value; re-probing with type=circuit_breaker.

## ZP-4111 — NOW ON QA (was "not deployed" on 21 Sep)
- GET /api/equipment-catalog/quick-search?q=FA&type=circuit_breaker&limit=100
  - sources omitted -> 200, 31 rows, source ∈ {skm, family} (the old blend)
  - sources=skm -> 200, 30 rows, ALL source=skm
  - sources=family -> 200, 1 row (FA · SCHNEIDER/SQUARE D, pnl_device_id 15)
  - sources=bogus -> 400 "sources must be from bus_model, cable, family, panel_type, skm, transformer"
  - (no type) -> 400 "type must be one of panelboard, switchboard, disconnect_switch, circuit_breaker, fuse, relay, transformer, cable, busway"
  Matches the ticket contract exactly (single-source full limit, unknown source 400, omitted unchanged). Bundle: device picker sets `sources` on the URL. UI walk of the picker still to do.

## WO /sessions/1a9c5d13-530d-4b7d-beb7-6a52ac84ebfc (Multiple Services) on C9NJAR1x
- Tabs: Assets 15 / SLD / Engineering / Panel Schedules / Forms 47 / Issues 1 / Attachments. Grid columns: Asset / Asset Class / QR Code / Forms / Arc Flash / Schedule / Issues (3 service-mask columns). Rows with services carry 2 checkboxes (Forms, Arc Flash). Header ring now reads 30% (was 47% "7 of 15" on 21 Sep -> ZP-4315 denominators to re-read). No "Check off" text on this WO (ZP-4280: bundle still has "Check off" x2 / "Checked off" x3 / "Mark complete" x1). -> 07
- ZP-4315 (To Do): header ring tooltip now "Overall: 6 of 20 (30%)" (21 Sep: 7 of 15, 47%). Web denominator moved 15 -> 20 with no asset-count change (Assets tab still 15) -> the web maths is unstable on its own; iOS side not re-read today. -> 08
- ZP-4272 (Ready for QA): mouse drag of the "QR Code" header onto "Asset" did not reorder (headers unchanged); no grid/column key in localStorage. Retrying with HTML5 drag events + checking the column menu.

## ZP-3990 Reactivate Work Order icon (Ready for QA) — PASS
- Operations -> Work Orders -> Closed (1–25 of 1111) -> "QA-VERIFY probe 4" (/sessions/ae730a37-f8c9-472a-b9d8-8352269eb72d, chip Closed, (s) Wild Goose Brewery) -> header ⋮ menu = Edit Work Order (pencil) / Generate Report (doc) / Reactivate Work Order (circular-arrow icon) / Delete Work Order (red bin). All four carry an icon. -> 09, 10

## ZP-4272 column arrangement retention (Ready for QA)
- WO assets grid: headers ARE draggable (MuiDataGrid draggable container draggable="true"); column menu = Pin to left / Pin to right / Filter / Hide column / Manage columns. No localStorage key for grid/column state; bundle has no preferences API path. Testing hide-then-return + synthetic drag. -> 11

## FM seat (chrome-devtools isolated context "fm") — logged in, landed on /pm-readiness
- FM rail = Site Data / Operations / Engineering / Maintenance Portal / Updates — the Maintenance Portal tile ALSO shows for the Facility Manager seat (ZP-4138 nav half, second role).
- ZP-4272 SHARPENED (real defect on this build): the WO assets grid IS reorderable and hideable, but nothing persists. Hid "QR Code" via the column menu -> header row becomes Asset/Asset Class/Forms/Arc Flash/Schedule/Issues; navigated to /sessions and back -> QR Code is BACK. No localStorage grid key, no preferences endpoint in the bundle, MUI onColumnOrderChange is wired to nothing. Exactly the customer complaint ("when you return, the column arrangement has reverted"). Ticket sits in Ready for QA. Mouse drag and synthetic HTML5 drag both left the order unchanged in this automation, so "can you reorder at all" needs a human mouse; the RETENTION half is proven either way (hide is a column-arrangement change and it is lost).

## ZP-4159 FM direct-URL access — API probes VOID, doing the frontend test
- FM seat (+fm@) signed in: roles ["Facility Manager"], accessible_sld_ids = 11 sites. Direct fetch of /api/ir_session/{foreign id} and /summary/v2 returned 200 text/html (the SPA shell), which is the known ir_session GET-returns-HTML trap, NOT an authorization answer. Re-testing by navigating the browser to the work-order URL, which is what the ticket describes.
- FM sites (11): 5d37063e, ac45e003, f1f0894c, eabf60c4, 7c4baec7, 5062f753, 97f11dbc, d0087e9f, 33d77bb0, 9f9adf1d, 8e845299.

## ZP-4309 child-asset IR checkbox — REPRODUCES ON WEB (ticket is On Hold, labelled [Backend]/iOS)
- WO "IR WO 22 09" (/sessions/bbe66d36-6537-418a-95a9-69ce25befccb, site ATEST, IR-typed, tabs Assets 2 / Issues / IR Photos / Attachments; columns Asset / Asset Class / QR Code / IR Photos / Issues).
- Top-level rows CB and PNL each show an IR Photos checkbox. Expanding PNL reveals three children: "Circuit Breaker 1" and "Circuit Breaker 2" have NO checkbox in the IR Photos cell at all, while sibling "PNL MCB" — which already carries IR photos (count badge) — DOES show one.
- Same shape as the iOS report: the control only appears once the child already has IR work. Web is affected too, so the fix must not be scoped to iOS. -> 12 (collapsed), 13 (expanded)

## ZP-4212 "Defer scheduled service" raw API error — FIXED, verified end to end
- Site Data -> Maintenance Program (site "Android Site 2") -> Defer -> Asset "11N-H1-1" -> Service "Cleaning Services" -> New due date 01/01/2020 -> Defer.
- Toast now reads, verbatim: "The new due date must be in the future — pick tomorrow or later." No HTTP status, no JSON, no "API call failed" prefix. Ticket's expected wording delivered. Dialog stays open so the user can correct the date. Nothing was mutated (the server refused the past date). -> 15

## ZP-4150 Issue Suggestion "add content directly from the issues" (Ready for QA) — NOT FOUND in the UI
- Builder -> Issue Suggestions: 14 sets (1–14 of 14), columns Issue Class / Fields (Title, Description, Resolution) / Classes / Status / Actions; header buttons Create Set / Import / Export.
- Opened the NEC Violation set (31 rows; banner "You are editing a shipped master…"; class chips; per-row funnel + kebab; header Export / Update / ⋯). Header ⋯ menu = Mark as Draft / Clear all rows / Delete this set. No "add from issues" / "pull from issue" affordance anywhere in the set editor, and no such wording in the page text.
- Issues page (/issues, 48 rows) row actions = View Issue / Edit Issue / Delete Issue; issue detail (/issues/797d01ad-…) tabs = Details / Class Details 4 / Photos / Status History 1, no "add to suggestions" action. The word "suggest" does not appear on the issue detail.
- Verdict: cannot find the shipped affordance. Needs the developer to name the screen; reported as "not located", not as "not implemented". -> 16

## ZP-4159 FM direct-URL work order — BLOCKED, but the refusal is a developer error string
Both controls run in the same FM session (+fm@, roles ["Facility Manager"], 11 assigned sites: A, ab, abhiiyant 17 june site, B, C_03_07, Demo FinalSheet_01_07, DemoFinal_02_07, test k123, TestBusPanel2, TestKR, TestKR1).
- POSITIVE CONTROL: the FM's own work-order list returns exactly 5 rows, all on sites A and ab (POST /api/company/{id}/workorders/v2 -> total 5). Opening one of them by URL, /sessions/92fa5fe0-7cc7-420d-b2c6-81d650636e2a, renders the full page: "Work Order - Aug 4, 7:13 PM", Closed, site A, tabs Assets 5 / Issues 1 / Attachments, locations tree and the asset grid. -> 17
- FOREIGN WORK ORDER: /sessions/1a9c5d13-530d-4b7d-beb7-6a52ac84ebfc (site "17 July 2026", NOT in the FM's 11) renders NO work-order data. The page body is the single line `Unexpected token '<', "<!DOCTYPE "... is not valid JSON` plus a "Back to Work Orders" button. -> 18
- So the ZP-4159 hole is CLOSED (no asset, issue or customer data reaches an unassigned FM), but the user-facing refusal is a raw JSON-parse error instead of the Access Denied card the app already has (the same card /maintenance-portal shows). Worth a small follow-up on the message, not a reopen of the access bug.

## ZP-4059 connection close should not hide the screen (Ready for QA) — PASS
- Site Data -> Connections (1–25 of 166) -> clicked the first row -> "Connection Details" opens in place (Source Node Fuse2 / Target Node P34 / Connection Type Cable / Connection ID 8e1a2f55-…, Core Attributes section).
- Clicking the back control returns to Connections: the grid is there again (1–25 of 166, Create Connection, search box) and the "Overall Readiness" card re-renders its skeleton, i.e. the content refreshes rather than the screen going blank. -> 19

## ZP-4030 deep links (Ready for QA) — backend half present
- https://acme.qa.egalvanic.ai/.well-known/apple-app-site-association -> 200 application/json 607 B; /.well-known/assetlinks.json -> 200 application/json 2,098 B. Both files are served. The email-link round trip needs a real notification email, not done here.

## ZP-3919 / ZP-3920 passwordless sign-in — cannot be settled on acme QA
- The sign-in screen is email + password + Sign In only (plus Forgot your password?, EN/FR). No Email->Continue step, no passkey button, no "Continue with Google", no backup-code field. The bundle ships passkey/WebAuthn strings, and the tickets say methods are per-company config, so acme QA simply has none enabled. Needs a company with the methods switched on.
