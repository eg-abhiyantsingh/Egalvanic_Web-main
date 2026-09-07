# ZP-4018 — Per-service user-checked completion check-offs (session-scoped ledger)

**QA verdict (web) — backend #1165 PASS · frontend #1336 PASS on the per-cell flow, session scope, partial/indeterminate and older-backend degrade · 1 DEFECT (bulk "Mark As.." with row selection sends grid row ids → 400, silent) · 1 FINDING (a user-checked service with no mask column — Arc Flash Label Placement — has no per-asset checkbox anywhere) · iOS #519/#520/#521 and pipeline #87 not testable from the web app**

**Artifact:** https://claude.ai/code/artifact/7f694e75-8a90-4523-94f6-fefbb0af4628
**Tested:** 2026-09-07 · `acme.qa.egalvanic.ai` build **V1.36** · tenant acme · Super Admin seat · live UI (Playwright-driven, visible browser) + live API + bundle `index-BV5BwzBG.js`.
**Ticket said "dev only (cicd/dev + release/dev); not yet promoted to QA" — wrong.** Backend #1165 and frontend #1336 are live on QA: `GET /procedures-v2/procedures/{id}` returns `methods[].user_checked: true` for Arc Flash Data Collection, Infrared Thermography and Arc Flash Label Placement (seed present); `GET /ir_session/{id}/service-registry` exposes per-service `user_checked` + per-node `by_node`/`checks`; `GET /ir_session/{id}/method-lines` and `PUT /ir_session/{id}/line-checks` exist; the bundle carries both PUT shapes and the indeterminate cell renderer.

---

## What was needed to test it
Every pre-existing AF/IR work order on QA (July–August fixtures, "test by kd", "test with ab 26"…) has **zero ledger lines** and `user_checked: false` in its registry — lines are materialised at wizard creation only. Two fresh work orders were created through the real 4-step wizard on the 10-asset site **"Addtioanl Site"**:

| WO | Services | Purpose |
|---|---|---|
| WO1 `11b924b8-f16f-49da-98d8-10ca3f2549dd` "ZP-4018 QA AF+IR check-offs (delete me)" | Arc Flash Data Collection (10 assets) + Infrared Thermography (5) → 15 lines | per-service ticks, bulk, untick, degrade, partial |
| WO2 `afea6fa4-e7a7-47e7-a0fd-9933afbc64a3` "ZP-4018 QA session-scope + partial (delete me)" | Arc Flash Data Collection (10) + Arc Flash Label Placement (2) → 12 lines | session-scoped assertion on the same assets; maskless service; **closed at the end of testing** (disabled-state check) |

Registry for WO1: `Arc Flash Data Collection | AF | user_checked true | data_mask arcFlash`, `Infrared Thermography | IR Checklist | user_checked true | data_mask ir`; 15 lines, all `open`, keys `id, node_id, service_id, implementation_method_id, method_name, status, executed_at, executed_by, user_checked, form_ids, issue_id, source`.

---

## QA-review checklist — results

| # | Ticket step | Result |
|---|---|---|
| 1 | Each service shows its own in-cell checkbox beside its mask cell (web + iOS) | ✅ **web PASS** — Assets grid columns Asset / Class / QR / **Arc Flash** / **IR Photos** / Issues; every row has a checkbox in the Arc Flash cell (`data-field="svc_mask_arcFlash"`, beside the AF readiness badge) and the 5 IR rows a second one in the IR Photos cell (`svc_mask_ir`, beside the photo count). iOS: not testable here. |
| 2 | Tick persists across reload; syncs web ↔ iOS | ✅ **web PASS** — tick MAIN-BUS-3836 Arc Flash → `PUT …/line-checks {"checks":[{"line_id":"260f3c79…","checked":true}]}` → 200 `{node_checks:{6bbb66ac…:false}, updated:1}`; ledger line → `status executed, executed_at 2026-09-07T16:09:25Z, executed_by 77e99d86…`; after reload both ticked boxes persist, header 0% → 13% (2/15), `service-completion.arc_flash 1/10`. The other service's box on the same row stays untouched. Untick → `checked:false` → line back to `open`, `executed_at/by null`. iOS sync not testable; the state lives server-side on the ledger, which is what iOS reads. |
| 3 | Return to an already-complete asset in a new session → does NOT self-complete | ✅ **PASS** — WO2 created on the same 10 assets while WO1 had CB-3836, CBL-HAND-3836 and MAIN-BUS-3836 ticked: WO2 opens with every Arc Flash box unchecked, 12 lines `open`, `asset-checks` all false, header 0%. |
| 4 | Partial completion renders an indeterminate/minus checkbox | ✅ **PASS (staged)** — no QA procedure has two user-checked methods and no two same-mask services resolve to one node naturally, so a second IR-mask line was added to MAIN-BUS-3836 with the product's own `POST …/add-assets {node_ids, service_id: 3b732d14 (Infrared Thermography, type IR — a catalog service the wizard no longer offers)}`. With one IR line executed and one open the IR cell rendered `data-indeterminate="true"` (minus). Clicking it sent both line ids `checked:true` → solid ✓, `node_checks true`. Bundle confirms the cell aggregates **all** services sharing a `data_mask` on the node. The UI-native route to this state (a procedure whose two methods are both user-checked, authored in Procedures V2) was not exercised. |
| 5 | Negative: older backend without a user_checked registry → legacy node-grain IR Checklist checkbox still works | ✅ **PASS (simulated)** — QA has one backend, so the older contract was simulated at the network layer on WO1: `service-registry` served without `user_checked`. The grid dropped the per-service boxes and rendered the **node-grain checkbox** in the IR Photos cell of the IR-Checklist rows (bundle: the legacy box renders only when an "IR Checklist" service exists AND no service carries `user_checked`); the frontend never even requests `/method-lines` in that mode (gated on the flag), so the older backend's missing endpoint is irrelevant. Ticking MOT-3836 sent `PUT …/asset-checks {"node_id":"e57f434a…","checked":true}` → 200. Back on the real contract the node-grain write had **fanned out** to both of MOT-3836's flagged lines (AF line `executed 16:20:47`, IR line executed) and `asset-checks[e57f434a] = true` — that fan-out and rollup are new-backend (#1165) behaviour, observed here for real. Network log confirms the gate: with the flag stripped the SPA requested only `service-registry` + `asset-checks`, never `method-lines`. A second variant — `service-registry` itself served as a 404 HTML shell (a pre-registry backend) — degraded to a plain asset list (columns Asset / Class / QR / Issues, no mask columns, no checkboxes of either kind, no error). |
| 6 | iOS: per-service check-off no longer 400s; app points at prod API URL | ⛔ **not testable** from the web app (no iOS build). The web equivalent — `line-checks` PUT with `Content-Type: application/json` — returns 200 on QA. |

Bulk per-service marking (not in the checklist, but shipped in #1336): Bulk Ops → **Select all** → **Mark As..** → "Infrared Thermography – done" sent `{node_ids:[10 raw uuids], service_id, checked:true}` → 200 `updated: 4` and ticked the four remaining IR boxes. The same action after **ticking individual rows** is Defect 1.

Compat rollup semantics observed: `node_checks[node]` (and `asset-checks`) turns true only when **every** flagged line on the node is executed (MAIN-BUS-3836 stayed false with AF done / IR open and flipped true once both were done) — and comes back down: unticking MOT-3836's IR line after the legacy fan-out returned `node_checks false`, `asset-checks[MOT] false`, header 53% → 50%.

Session scope, both directions: after WO2's bulk Label-Placement mark and after WO1 fully completed MAIN-BUS-3836 (rollup true), WO2's MAIN-BUS-3836 still read `Arc Flash Data Collection: open`, `asset-checks false`; WO1 was untouched by WO2's writes.

Cross-client sync could only be approximated: the tick is read back by the API and survives a full reload, and the ledger is server-side with no client-held state; a second browser tab was opened twice but its grid had not rendered within the wait window, so no second-client observation is claimed.

---

## Defects and findings

### DEFECT 1 — Bulk "Mark As.." with row-checkbox selection sends grid row ids and fails silently (400)
**Where:** `/sessions/{id}` → Assets → Bulk Ops → tick individual rows → **Mark As..** → "<service> – done / – not done".
**Repro:** open WO1 → Bulk Ops → tick CB-3836 and CBL-HAND-3836 → Mark As.. → "Arc Flash Data Collection – done".
**Actual:** `PUT /api/ir_session/11b924b8…/line-checks {"node_ids":["loc-0-node-51841f5f-1460-4130-842a-867cbfb7581e","loc-0-node-0f421a11-8def-48e9-a834-3a7b02b53970"],"service_id":"d625cfa0…","checked":true}` → **400 `{"error":"invalid id"}`**; nothing changes on screen, no toast, only a console error `[SessionDetail] bulk mark-as failed`.
**Why (bundle-verified by an independent refuter pass):** the Assets grid builds location-grouped rows with synthetic ids `loc-<locationIndex>-node-<uuid>` and keeps the real uuid in `row.node_id`; the DataGrid declares no `getRowId`, so its selection model holds the synthetic ids. The bulk handler does `const wi = Ts && Ts.length ? Ts : qs` — `Ts` is the server-side "Select all" uuid list, `qs` the DataGrid row selection — and forwards `wi` unmapped into `PUT line-checks {node_ids}`. The sibling bulk handlers in the same component (bulk delete, Services) DO map row ids to `node_id`; this one skipped that step, and no prefix-stripping exists anywhere in the bundle. The same request with bare uuids returns 200 (`updated: 2`), so the backend contract is fine.
**Second symptom of the same id mismatch:** the "Mark As.." menu filters its service list through `by_node` (keyed by uuid); with row ids the lookup finds nothing and the menu silently falls back to listing every user-checked service on the work order, not just the ones the selected assets owe.
**Expected:** strip the prefix / map row ids to `node_id` before calling `line-checks`, and surface the failure to the user.
**Severity:** High — the row-selection path is the natural way to mark a handful of assets done; it looks like it works and does nothing.

### FINDING 2 — A user-checked service without a mask column has no per-asset checkbox (Arc Flash Label Placement)
Arc Flash Label Placement is one of the three formless methods the ticket names. Its registry entry is `type Checklist, user_checked true, wo_view {hide_tabs:["ir_photos"], priority 50, readiness_mask "checklist", report_work_type "Checklist"}` — **no `data_mask`, no `row_slot`** — so the per-cell renderer (which keys on `data_mask`) never draws a checkbox for it, and on an AF work order the Tasks tab is hidden. On WO2 its two lines are invisible per asset; the only way to tick them on web is Select all → Mark As.. (header 0% → 17%, `service-completion.checklist 2/2`, grid unchanged). With Defect 1 the row-selection route is also dead. Severity: Medium — completion can be recorded but not seen or done per asset.

### Notes (not defects)
- The catalog lists **two** "Infrared Thermography" services: `4d5ea9cb` (type **IR Checklist**, the one the wizard offers and the one with the legacy node checkbox) and `3b732d14` (type **IR**, used by the July fixtures, absent from the picker but still accepted by `add-assets`). IR-type work orders show **no** checkbox at all, before or after this change — by design of the legacy path, but worth knowing when a customer asks "where is the checkbox".
- The header percentage is executed-lines / total-lines across all services (15 lines → 13%, 47%, 53%), so a single service's progress is not readable from the header.
- Fixtures are left in place (Addtioanl Site is a QA sandbox); MAIN-BUS-3836 in WO1 carries the extra IR-type line used for the indeterminate check.

---

## Test data — direct links (QA)
- WO1 (per-service ticks, bulk defect repro, indeterminate): https://acme.qa.egalvanic.ai/sessions/11b924b8-f16f-49da-98d8-10ca3f2549dd
- WO2 (session-scoped assertion, Label Placement finding): https://acme.qa.egalvanic.ai/sessions/afea6fa4-e7a7-47e7-a0fd-9933afbc64a3
- Legacy IR-type WO with no lines ("test with ab 26"): https://acme.qa.egalvanic.ai/sessions/66e7e7b1-7dc8-4998-81ee-301f39ad940f
- WO1 registry: https://acme.qa.egalvanic.ai/api/ir_session/11b924b8-f16f-49da-98d8-10ca3f2549dd/service-registry · ledger: https://acme.qa.egalvanic.ai/api/ir_session/11b924b8-f16f-49da-98d8-10ca3f2549dd/method-lines · legacy checks: https://acme.qa.egalvanic.ai/api/ir_session/11b924b8-f16f-49da-98d8-10ca3f2549dd/asset-checks · completion: https://acme.qa.egalvanic.ai/api/ir_session/11b924b8-f16f-49da-98d8-10ca3f2549dd/service-completion
- WO2 ledger: https://acme.qa.egalvanic.ai/api/ir_session/afea6fa4-e7a7-47e7-a0fd-9933afbc64a3/method-lines
- Seeded method flag: https://acme.qa.egalvanic.ai/api/procedures-v2/procedures/e52adf8a-9f44-4438-805e-d6a2f233b234 (Arc Flash Data Collection — ATS, `methods[0].user_checked: true`)
- Site: Addtioanl Site `fd1e25c4-4e04-48d7-8ef3-bfa0cba28c56` (sites have no routed detail URL; work orders above are the entry points)

Evidence: `docs/bug-evidence/zp-4018-user-checked-checkoffs/` (21 screenshots + `api-captures.md` with every request/response quoted).

---

## Not covered / honest gaps
- **iOS** (#519 per-metric columns, #520 Content-Type 400 fix, #521 prod API URL) — needs a device/build; nothing here speaks to it.
- **Web ↔ iOS sync** — only the server-side ledger was verified (state persists across reload and is read back by the API); no second client observed it.
- **Older backend** — simulated by intercepting two GETs on the QA backend; a real older backend would also return the SPA shell for `/method-lines` (masked 404) — the frontend's `.catch(() => {})` around that call makes both cases equivalent, but it was not run against real older code.
- **Pipeline #87** (spec-builder prompt/sandbox doc) — documentation change, not observable in the app.
- **Authoring side** — the Procedures V2 method editor's "User supplies completion checkmark" toggle was not exercised (whether flipping it changes lines on new/existing work orders).
- **Roles** — the Technician seat (`+tec@`) is now met by a full-page "Web Access Restricted — your account does not have permission to access the web platform" screen on QA, so the persona that does check-offs cannot be exercised on web at all; only Super Admin was tested. Backend authorisation of `line-checks` for other roles is unverified.
- **Closed work order** — after closing WO2 the per-service boxes render `disabled` and Bulk Ops is gone (checked); no re-open control was found on the page, so the reactivation path was not exercised.

## Method
Fetch-shim and Playwright route capture of every `line-checks` / `asset-checks` request the SPA sent; API cross-check (`method-lines`, `service-registry`, `asset-checks`, `service-completion`) after every UI action; positive controls beside every negative (untouched box on the same row, un-reviewed neighbour, Select-all path beside the failing row-selection path); the older-backend contract reproduced at the network layer rather than asserted from code alone.
