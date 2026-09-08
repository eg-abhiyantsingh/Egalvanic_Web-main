# Method-first multi-service work orders — captures (QA V1.36, bundle `index-CSsDpG3c.js`, 2026-09-08)

## Migration check — both new objects exist on QA
- **`services.wo_view` (smline_a3)** is served on every service in `GET /api/ir_session/{id}/service-registry`, fully populated per service type:
  | service | type | wo_view |
  |---|---|---|
  | Arc Flash Data Collection | AF | `{data_mask:"arcFlash", readiness_mask:"arc_flash", row_slot:"data_mask", tabs:["sld","equipment_designations"], hide_tabs:["tasks","ir_photos"], report_work_type:"AF", priority:10}` |
  | Infrared Thermography | IR Checklist | `{data_mask:"ir", readiness_mask:"asset_checks", row_slot:"data_mask", extra_columns:["asset_check"], tabs:["ir_photos"], hide_tabs:["tasks"], report_work_type:"IR Checklist", priority:30}` |
  | Infrared Thermography | IR | `{data_mask:"ir", readiness_mask:"ir_photos", row_slot:"data_mask", tabs:["ir_photos"], hide_tabs:["tasks"], report_work_type:"IR", priority:40}` |
  | Clean, Tighten, Torque | PM Forms | `{row_slot:"forms"}` (no legacy readiness mask) |
- **`session_method_lines` (smline_a1/a2)** is served by `GET /api/ir_session/{id}/method-lines` → `{lines:[{id, node_id, service_id, implementation_method_id, method_name, status, user_checked, …}]}`, one row per (session, node, method), and writes return per-line ids. `POST` on the same path adds lines and mints form instances (a2's exclusive attribution).

## Checklist 1 — the session view is the union of the performed services' contracts
Work order `11b924b8-f16f-49da-98d8-10ca3f2549dd` with 3 registered services (AF + IR Checklist + IR), 10 assets, 16 ledger lines.
- **Tabs rendered:** Assets · SLD · Engineering · Issues · IR Photos · Attachments — the union of `tabs` (`sld`, `equipment_designations`, `ir_photos`) with `tasks` hidden by every service's `hide_tabs`. Note `ir_photos` is in AF's `hide_tabs` but in IR's `tabs`, and it renders: a show beats a hide.
- **Metric columns:** `svc_mask_arcFlash` ("Arc Flash") and `svc_mask_ir` ("IR Photos") — one column per distinct `data_mask`, so the IR and IR-Checklist services share the `ir` column and the checklist's `extra_columns:["asset_check"]` is folded into it as the per-service check square.
- **Registration-scoped wheel:** `GET …/service-completion` → `{arc_flash:{5,10}, asset_checks:{4,5}, ir_photos:{1,1}}`; header **63 %** = 10/16, the sum of the rings (56 % was the value before the MOT-3836 tick recorded in the PM-programs run), and each ring's `total` is the count of nodes registered for that service (5 for the checklist, not 10).
- **Per-node cell gating:** `by_node` gives per-asset service counts (1, 2 or 3 of 3). A cell for a service the node is not registered for renders **empty with no control at all**; a registered one renders its metric plus its check square. CB-3836 later showed `forms_status: "4"` (its own form count) while its `svc_mask_ir` stayed empty.

## Checklist 2 — Add Service, no double-mint, Remove Service scope
Right-click an asset in the WO grid → **View Full Asset · What's missing? · Add Service · Manage Services · Add Procedure · Add Issue**.

**Add Service — rules evaluated, forms pre-checked.** Dialog "Add Service — CB-3836", copy "The service's rules are evaluated against this asset — its implementation methods register here and their forms come along." Picking *Clean, Tighten, Torque* showed **EVALUATES TO** with both methods listed and **both checkboxes pre-checked** (`Cleaning & Lubrication (Low-Voltage)`, `Operational / Mechanical Function (Low-Voltage)`).
`POST /api/ir_session/{id}/method-lines {"node_id":"51841f5f-…","implementation_method_ids":["768471fe-…","27a5d54f-…"]}` → 200 `{"form_instances_created":4,"lines":[{"id":"be4d4e41-…","method_name":"Cleaning & Lubrication (Low-Voltage)"},{"id":"6dcacfb8-…","method_name":"Operational / Mechanical Function (Low-Voltage)"}]}`. Ledger 16 → 18 lines. The 4 minted forms appear in a new **Forms 4** tab: Cleaning & Lubrication — Cleaning · — Lubrication · Operational / Mechanical Function · Torque Record, all on CB-3836, all Draft.
**The UI updated live:** a `forms_status` "Forms" column appeared, the Forms tab appeared, and the wheel gained a ring `"Clean, Tighten, Torque":{ready:0,total:1}` (total 1 = the one registered node). Percent sequence, correctly: the wheel stood at **10/16 = 63 %** after the earlier MOT-3836 tick; adding a 0/1 ring made it **10/17 = 59 %**; removing the service later returned it to 10/16 = 63 %.

**No double-mint.** The identical request repeated → 200 **`{"form_instances_created":0, lines:[<the same two line ids>]}`**; ledger still 18 lines, registry still 4 services, ring unchanged. Add is idempotent per (node, method).

**Remove Service takes only unsubmitted forms.** One of the four minted forms (Torque Record) was filled and submitted through the UI — `PUT /api/eg-form-instance/bulk-patch {"items":[{"id":"4745511d-…","form_submission_patch":{measurements:{torque_wrench_id:"QA-DEMO wrench 1",reference_doc:"QA-DEMO spec p.12"},verdict:{result:"pass"}}}]}` → 200 `{"results":[{"id":"4745511d-…","submitted":true,"success":true}]}`, row status **Submitted**. (The submit dialog warns "Submit with incomplete sections?" → Submit Anyway.)
Then Manage Services → Remove on *Clean, Tighten, Torque*: `POST /api/ir_session/{id}/remove-service {"node_id":"51841f5f-…","service_id":"8e578df1-…"}` → 200 **`{"instances_removed":3,"lines_removed":2}`** — **3 of the 4 instances removed, the submitted one kept**; both ledger lines gone, node's registry back to AF only, registry back to 3 services, ring gone, wheel back to 63 %.
**Caption note for `n8-after-remove-submitted-survives.png`:** its burned-in banner says the Forms tab/column stay "because the service is still registered elsewhere" — that was wrong; no node was registered any more. The tab was gone and the column that remained is the legacy EG-Forms column driven by the work order's form count (see the bundle note in the verdict).
The surviving submitted Torque Record is still there afterwards (`GET /api/eg-form-instance/4745511d-…` → 200; `GET /api/eg-form-instance/by-session/{wo}` still lists it) and is reachable in the UI through **More → Forms** ("Torque Record · CB-3836 · Submitted · Sep 8, 2026, 04:21 PM"), though the derived **Forms tab is gone** because no node is registered for that service any more (confirmed after a full page reload).
Manage Services dialog copy: "Services registered on this asset for this work order. Removing one clears its unsubmitted forms; submitted forms stay." — it lists each registered service with its type and a Remove button.

## Checklist 3 — uppercase client UUIDs (the `uuid::text` casing bug)
Both directions were sent with iOS-style **uppercase** ids from the authenticated session.
- **Add:** `POST /api/ir_session/{id}/method-lines {"node_id":"51841F5F-1460-4130-842A-867CBFB7581E","implementation_method_ids":["709644D6-0C37-4E74-A899-9CC01E482C3B","72DD2BE2-E2F6-4F5F-B873-972661E87F45"]}` → 200 `{"form_instances_created":2,"lines":[{"id":"5b867d1c-…","method_name":"Low-Voltage Breaker Visual Inspection"},{"id":"9b970068-…","method_name":"Ground-Fault Protection Inspection"}]}`. Ledger 18 → 20, registry 4 → 5 services, and a new registration-scoped ring appeared: `"De-Energized Visual Inspection":{ready:0,total:1}`. **The add applied.**
- **Remove:** `POST /api/ir_session/{id}/remove-service {"node_id":"51841F5F-1460-4130-842A-867CBFB7581E","service_id":"01AD81FF-63FE-507E-BEB0-305D7F67DAD9"}` → 200 **`{"instances_removed":2,"lines_removed":2}`**; ledger back to 18, registry back to 4, ring gone. **Not silently no-opped** — the reported casing bug is fixed on both service boundaries.
Methods for a service on a node come from `GET /api/implementation-methods/evaluate-for-node/{node_id}?service_id={id}` → `{methods:[{id,name,description,…}]}`. (`/api/ir_session/{id}/available-methods` is not a route — it returns the SPA shell.)

## Checklist 7 — negatives
**Pre-ledger work order.** `61317671-017a-4d26-8b0e-adb997dfdce3` ("WTE_DUE_1786132804183", created 8 Aug 2026, 1 asset) and `e5179624-9f51-4411-b190-3738f49cbc9e` (10 Aug 2026): `service-registry` → `{services:[], by_node:{}}`, `method-lines` → 0 lines, `service-completion` → `{completion:{},rooms:{}}`. Both open normally and degrade to the legacy view — tabs **Assets · Issues · Attachments** only, no service metric columns, **no completion wheel**, no errors in the console beyond the usual page noise. The derived type itself is not exposed in any web JSON reachable here (`summary/v2` carries only counts/stats/asset_classes; `/api/ir_session/{id}`, `/details`, `/header`, `/meta` all return the SPA shell), so "resolves its type from legacy `work_type_id`" is verified only to the extent that the legacy view renders and nothing 500s.
**Frozen / unknown method ids.** Two reachable paths, both refuse cleanly rather than 500:
| Request | Result |
|---|---|
| `POST method-lines` with only a bogus method id | **400** `{"error":"unknown or inaccessible method(s): 00000000-1111-2222-3333-444444444444"}` |
| `POST method-lines` with **one bogus + one valid** id | **400**, same message — the whole batch is rejected, the valid line is not added |
| `POST remove-service` with an unknown `service_id` | 200 `{"instances_removed":0,"lines_removed":0}` — a clean no-op |
| `POST /asset-maintenance/apply` with a bogus `implementation_method_ids` entry | **400** `{"error":"lines[0]: implementation methods do not belong to that service for this class"}` |
No 500 anywhere. But note the ticket's stated intent for frozen ids is *skip and log*: on the add path a mixed batch is refused wholesale instead of registering the valid methods and skipping the frozen one. The actual plan-release path (releasing a PM plan whose method ids predate a reseed) is not reachable from the web UI, so that exact scenario was not exercised.

## Not exercised
iOS #517 (offline Manage Services replay, Active Rooms default view, k/n bubble + chooser, nodeChecks reconciliation onto `mapping_node_session.is_checked`, ghost grouped-task rows) — native app. The web-side equivalents that exist are covered above: per-node cell gating stands in for the k/n chooser, and `PUT …/line-checks` (verified in the ZP-4018 and PM-programs runs) is the same check-off contract.

## Test data (QA)
- Multi-service work order: https://acme.qa.egalvanic.ai/sessions/11b924b8-f16f-49da-98d8-10ca3f2549dd · registry: https://acme.qa.egalvanic.ai/api/ir_session/11b924b8-f16f-49da-98d8-10ca3f2549dd/service-registry · ledger: https://acme.qa.egalvanic.ai/api/ir_session/11b924b8-f16f-49da-98d8-10ca3f2549dd/method-lines · completion: https://acme.qa.egalvanic.ai/api/ir_session/11b924b8-f16f-49da-98d8-10ca3f2549dd/service-completion
- Asset used for Add/Remove Service: CB-3836 `51841f5f-1460-4130-842a-867cbfb7581e`
- Surviving submitted form: https://acme.qa.egalvanic.ai/api/eg-form-instance/4745511d-a1a0-42d7-93bd-cec7fd543b64
- Pre-ledger work orders: https://acme.qa.egalvanic.ai/sessions/61317671-017a-4d26-8b0e-adb997dfdce3 · https://acme.qa.egalvanic.ai/sessions/e5179624-9f51-4411-b190-3738f49cbc9e
- Single-service PM Forms work order (from the same day's PM-programs run): https://acme.qa.egalvanic.ai/sessions/b2c2657a-50a0-4d69-af07-6167a0c0d39f

## Added after the adversarial pass — the shared-scope confound, resolved
The refuters pointed out that **Torque Record is the project's documented shared-scope form** (`docs/changelogs/2026-07-20-service-workorder-videos-analysis.md`), so its survival was consistent with *both* "submitted survives" and "shared-scope survives". Re-run 17:05 on the same node:
1. Re-add the service: `POST …/method-lines {node_id:"51841f5f-…", implementation_method_ids:[768471fe…, 27a5d54f…]}` → 200 **`form_instances_created: 3`** (not 4) with the **same two line ids** `be4d4e41-…` / `6dcacfb8-…` echoed back — the shared Torque Record (already present, submitted) was not minted again, the three node-scoped fragments were. The four instances on the work order: `4745511d` Torque Record (submitted), `22e0723d`, `a0275300`, `af69fc23` (drafts).
2. Submit a **node-scoped** fragment through the UI: Forms → "Cleaning & Lubrication — Cleaning" → CB-3836 → Pass → Submit → Submit Anyway → instance `22e0723d` now `submitted: true`.
3. `POST …/remove-service {node_id, service_id: 8e578df1-…}` → 200 **`{"instances_removed":2,"lines_removed":2}`**.
4. `GET /api/eg-form-instance/by-session/{wo}` afterwards → exactly two instances remain: **`4745511d` (Torque Record, submitted, shared) and `22e0723d` (Cleaning fragment, submitted, node-scoped)**; the two unsubmitted drafts are gone.
So a submitted form survives Remove Service **regardless of scope** — the rule is "submitted stays", independently of the shared-scope rule.

**smline_a2 attribution, read directly:** `GET /api/eg-form-instance/4745511d-…` carries `session_method_line_id: "be4d4e41-499b-4f22-ae10-68dd7d262bdc"` (the Cleaning & Lubrication method line), `eg_form_title: "Torque Record"`, `eg_form_type_name: "NETA Fragment"`, `submitted: true`. The attribution column exists and is populated; a surviving instance keeps pointing at its (removed) line id, and a later re-add of the same method revived the same line id.

**The derived/legacy type IS exposed — correction.** `GET /api/ir_session/{id}/full` → `data.session.work_type_id`: ledger WO `11b924b8` → `d625cfa0-…` (Arc Flash Data Collection, the wizard's "first service to perform"); PM Forms WO `b2c2657a` → `01ad81ff-…` (De-Energized Visual Inspection); **both August fixtures `61317671` and `e5179624` → `"null"`** — they are no-service ("General") work orders, so they cannot exercise "resolves its type from legacy `work_type_id`"; their bare view is the no-type view, not a legacy fallback. A pre-ledger work order with a non-null `work_type_id` was not located in this run.
**`shared` flag:** `by-session` lists both survivors with `shared: false` (Torque Record `4745511d`, Cleaning fragment `22e0723d`), so on this fixture the Torque Record is not flagged shared-scope either way; the disambiguation above does not depend on it. The by-session list omits `session_method_line_id`; the single-instance GET carries it.
