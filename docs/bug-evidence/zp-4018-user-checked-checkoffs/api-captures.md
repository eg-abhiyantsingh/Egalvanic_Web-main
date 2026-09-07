# ZP-4018 — API captures (QA V1.36, 2026-09-07, tenant acme)

Fixture work orders (wizard-created on site "Addtioanl Site" `fd1e25c4-4e04-48d7-8ef3-bfa0cba28c56`):
- WO1 `11b924b8-f16f-49da-98d8-10ca3f2549dd` — "ZP-4018 QA AF+IR check-offs (delete me)": Arc Flash Data Collection (10 assets) + Infrared Thermography (5 assets)
- WO2 `afea6fa4-e7a7-47e7-a0fd-9933afbc64a3` — "ZP-4018 QA session-scope + partial (delete me)": Arc Flash Data Collection (10) + Arc Flash Label Placement (2) (+ "Test" corrective, 0 issues → no lines)

## Deployment fingerprint
- Bundle `index-BV5BwzBG.js`: `user_checked` ×7, `line-checks` ×2 (PUT helpers `{checks:[{line_id,checked}]}` and `{node_ids, service_id, checked}`), per-cell renderer `checked: cl===Go.length, indeterminate: cl>0 && cl<Go.length`.
- `GET /api/procedures-v2/procedures/{id}` → `methods[].user_checked: true` for Arc Flash Data Collection, Infrared Thermography, Arc Flash Label Placement (seed uchk_a2 present).

## Registry / ledger (WO1 right after creation)
```
GET /api/ir_session/11b924b8…/service-registry
data.services = [
  {id d625cfa0-5447-52c5-858e-9ecd5c84d0fb, name "Arc Flash Data Collection", type AF, user_checked true, wo_view.data_mask "arcFlash"},
  {id 4d5ea9cb-985e-45ea-9f92-39cde8d06046, name "Infrared Thermography", type "IR Checklist", user_checked true, wo_view.data_mask "ir"}]
data.by_node = {node_id: [service_id,…]} (10 nodes) ; data.checks = {node_id: {service_id: bool}}

GET /api/ir_session/11b924b8…/method-lines → data.lines[15]
line keys: created_at, executed_at, executed_by, form_ids, id, implementation_method_id, is_deleted, issue_id,
           method_key, method_name, modified_at, node_id, service_id, session_id, source ("materialize"), status ("open"|"executed"), user_checked
```

## Single in-cell tick (MAIN-BUS-3836, Arc Flash cell)
```
PUT /api/ir_session/11b924b8…/line-checks
{"checks":[{"line_id":"260f3c79-b0e5-44e2-9e61-95e9d75a5c06","checked":true}]}
→ 200 {"data":{"node_checks":{"6bbb66ac-42ce-419b-9ce6-065f29693565":false},"updated":1},"success":true}
method-lines afterwards: id 260f3c79… status "executed", executed_at "2026-09-07T16:09:25Z", executed_by "77e99d86-7f0a-4345-b056-6f470bb668ec"
GET service-completion → completion.arc_flash {ready 1, total 10}; rooms.Unassigned {ready 1, total 15}; header 13% after two ticks (2/15)
```
Untick (TX-HAND-3836 IR): `{"checks":[{"line_id":"ed972b59…","checked":false}]}` → 200 → line status "open", executed_at null, executed_by null.

## Bulk "Mark As.." — row-checkbox selection (DEFECT)
```
PUT /api/ir_session/11b924b8…/line-checks
{"node_ids":["loc-0-node-51841f5f-1460-4130-842a-867cbfb7581e","loc-0-node-0f421a11-8def-48e9-a834-3a7b02b53970"],
 "service_id":"d625cfa0-5447-52c5-858e-9ecd5c84d0fb","checked":true}
→ 400 {"error":"invalid id","success":false}      (UI: no change, console "[SessionDetail] bulk mark-as failed")
```
Same action via **Select all 10** sends raw uuids → 200 `{"node_checks":{…},"updated":4}`.
Same body with raw uuids sent by hand → 200 `{"node_checks":{"0f421a11…":true,"51841f5f…":true},"updated":2}` (CB-3836 / CBL-HAND-3836 have only the AF line, so the node rollup flips true).

## Compat rollup observed
`node_checks[node]` / `asset-checks.checks[node]` is true only when every flagged line on the node is executed (MAIN-BUS-3836 became true only after both its AF and IR lines were done).

## Session scope
WO2 (same site, same 10 assets) right after creation: 12 lines all `open`, `asset-checks` all false, every Arc Flash cell unchecked while WO1 has CB-3836 / CBL-HAND-3836 / MAIN-BUS-3836 checked.

## Formless method with no mask column (Arc Flash Label Placement, WO2)
Registry: `{name "Arc Flash Label Placement", type Checklist, user_checked true, wo_view: {hide_tabs:["ir_photos"], priority 50, readiness_mask "checklist", report_work_type "Checklist"}}` — **no data_mask / row_slot** → no in-cell checkbox anywhere; Tasks tab hidden by the AF view.
Select all → Mark As.. → "Arc Flash Label Placement – done": `{"node_ids":[10 uuids],"service_id":"9de69871-…","checked":true}` → 200 `updated: 2`; header 0% → 17%; service-completion `checklist {ready 2, total 2}`; grid unchanged.

## Older-backend simulation (Playwright route interception on WO1)
- `GET …/service-registry` fulfilled with `user_checked` deleted from every service and `data.checks` removed; `GET …/method-lines` fulfilled 404 `{"success":false,"error":"Not found"}`; PUTs passed through to the real backend.
- Grid: Arc Flash cells lost their checkbox; IR Photos cells of the 5 IR-Checklist rows kept a **node-grain** checkbox (MAIN-BUS-3836 checked because its node rollup was already true).
- Tick MOT-3836 → `PUT /api/ir_session/11b924b8…/asset-checks {"node_id":"e57f434a-7566-4278-88f4-7c8c8a7b8139","checked":true}` → 200.
- Real contract afterwards: MOT-3836 lines `Arc Flash Data Collection: executed 2026-09-07T16:20:47Z` (fanned out by the node-grain write) + `Infrared Thermography: executed`; `asset-checks[e57f434a] = true`; grid shows both per-service boxes ✓; header 47% → 53%.

## Partial → indeterminate (WO1, MAIN-BUS-3836)
- `POST /api/ir_session/11b924b8…/add-assets {"node_ids":["6bbb66ac-42ce-419b-9ce6-065f29693565"],"service_id":"3b732d14-461c-54a7-8e30-70391bd34dd6","preload_forms":false}` → 200 `{"assets_added":1,"method_lines":1,…}`; registry now lists a third service `Infrared Thermography | IR | user_checked true | data_mask "ir"`; MAIN-BUS-3836 lines: AF executed, IR-Checklist executed, IR (3b732d14) **open**.
- Grid IR cell for MAIN-BUS-3836: MUI checkbox `data-indeterminate="true"` (minus icon), `checked=false`.
- Click it → `PUT …/line-checks {"checks":[{"line_id":"75bd6ebe-…","checked":true},{"line_id":"d3530dea-…","checked":true}]}` → 200 `{"node_checks":{"6bbb66ac…":true},"updated":1}` → all three lines executed, cell solid ✓.

## Legacy IR-type work order (no ledger lines) — "test with ab 26" `66e7e7b1-7dc8-4998-81ee-301f39ad940f`
Registry: `Infrared Thermography | type IR | user_checked false | data_mask ir`, `method-lines` → 0 lines, `asset-checks` 84 nodes all false. Grid columns Asset / Asset Class / QR Code / IR Photos / Issues — **no checkbox of any kind** (the legacy node-grain checkbox is rendered only for the "IR Checklist" service type).

## Follow-ups from the adversarial pass
- **Bundle-verified root cause of the bulk 400** (independent refuter, high confidence): rows are built as `loc-${locationIndex}-node-${uuid}` with `node_id` kept separately; the DataGrid has no `getRowId`; `onSelectionChange` sets `qs` = row ids (Ts reset to null); "Select all" sets `Ts` = server `ids_only` uuid list; the handler `An` uses `Ts && Ts.length ? Ts : qs` unmapped into `WNr` → `PUT line-checks {node_ids}`. Sibling handlers (bulk delete `Bz`, Services `My`) map row → `node_id`; no prefix stripping exists in the bundle. Same list feeds the Mark As.. menu filter (`by_node[rowId]` → empty → unfiltered list).
- **Network log with `user_checked` stripped:** requests = `GET service-registry`, `GET asset-checks` only — `method-lines` is never requested (gated on the flag), so the 404 mock in the first simulation was never hit.
- **Registry 404 (HTML shell) variant:** only `GET service-registry` fires; grid columns Asset / Asset Class / QR Code / Issues; no mask columns, no checkboxes, no error text.
- **Untick of a fanned-out line** (MOT-3836 IR): `PUT line-checks {"checks":[{"line_id":"8274c859-…","checked":false}]}` → 200 `{node_checks:{e57f434a…:false},updated:1}`; `asset-checks[e57f434a] = false`; MOT lines AF executed / IR open; `service-completion` `arc_flash 4/10, asset_checks 3/5, ir_photos 1/1`, rooms 8/16; header 50%.
- **Bidirectional session scope:** WO1 MAIN-BUS-3836 all three lines executed, `asset-checks true`; WO2 MAIN-BUS-3836 `Arc Flash Data Collection: open`, `Arc Flash Label Placement: executed` (its own bulk mark), `asset-checks false`.
