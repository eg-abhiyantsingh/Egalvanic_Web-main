# Method-first, multi-service work orders (derived WO type + session_method_lines ledger)

**QA verdict — every web-testable step PASSES, and the ticket's "dev only, not yet promoted to QA" note is wrong: all three migrations are already in place on QA (`session_method_lines` is served by the ledger endpoint with per-line ids, and `services.wo_view` is populated per service type). The session view really is the union of the performed services' contracts — adding a fourth service made a tab, a metric column and a registration-scoped ring appear live, and removing it took them away. Forms mint exactly once (a repeated identical add returns `form_instances_created: 0`), and Remove Service took 3 of 4 form instances, keeping the one that had been submitted. The reported iOS casing bug is fixed on both boundaries: uppercase-UUID add and remove both applied. No defects; 3 Low findings.**

**Artifact:** https://claude.ai/code/artifact/18b40ab9-f75e-4820-9995-6a8b98b58830
**Tested:** 2026-09-08 · `acme.qa.egalvanic.ai` build **V1.36**, bundle **`index-CSsDpG3c.js`** · tenant acme · Super Admin seat · live UI clicks with request capture, plus direct calls on the two service boundaries to reproduce the iOS uppercase-UUID case.

> **Migration status (the ticket's own carry-forward caveat).** Both new objects are live on QA: `services.wo_view` is returned fully populated for every service (`data_mask`, `readiness_mask`, `row_slot`, `tabs`, `hide_tabs`, `extra_columns`, `report_work_type`, `priority`) and `session_method_lines` is readable and writable through `GET`/`POST /api/ir_session/{id}/method-lines`, which returns per-line ids and mints form instances. So smline_a1/a2/a3 have run here. The frontend `src/sld/` OTA note is not observable from the browser.

---

## QA-review checklist — results

| # | Ticket step | Result |
|---|---|---|
| 1 | Create a work order and register multiple services on it; confirm web SessionDetail renders one tab and metric column per service plus a registration-scoped completion wheel | ✅ **PASS** — the 3-service work order renders tabs Assets · SLD · Engineering · Issues · IR Photos · Attachments, exactly the union of the services' `tabs` with `tasks` hidden by every service's `hide_tabs` (and `ir_photos` shown even though the arc-flash service hides it — a show beats a hide). Metric columns are one per distinct `data_mask`: `svc_mask_arcFlash` and `svc_mask_ir`, with the IR-Checklist service's `extra_columns:["asset_check"]` folded into the shared `ir` column as its check square. The wheel is registration-scoped: `arc_flash 5/10`, `asset_checks 4/5` (5 = the nodes registered for the checklist, not all 10), `ir_photos 1/1`, and the header shows their sum. **Proven dynamically as well:** adding a 4th service put a `forms_status` column, a `Forms 4` tab and a ring `"Clean, Tighten, Torque": 0 of 1` on screen; removing it took the tab and the ring away again. |
| 2 | Right-click an asset → Add Service (evaluated-forms pre-checked) and Manage Services; confirm forms mint exactly once (no double-mint) and that Remove Service takes only its unsubmitted, non-shared forms while submitted and shared-scope forms survive | ✅ **PASS** — the context menu carries **Add Service** and **Manage Services**. Add Service evaluates the service's rules against the asset and lists them under **EVALUATES TO** with **every method pre-checked**; applying minted 4 form instances from 2 methods. **No double-mint:** the identical request repeated returned `form_instances_created: 0` and echoed the same two line ids, with the ledger unchanged. **Removal scope:** one of the four forms was filled and submitted through the UI, then Remove Service returned `{"instances_removed":3,"lines_removed":2}` — the three drafts went, the submitted Torque Record stayed, and it is still readable and still listed for the work order afterwards. Shared-scope forms were not present on this fixture (see gaps). |
| 3 | Send a service add and a service remove with uppercase client UUIDs; confirm the add applies and the remove is not silently no-opped (the `uuid::text` casing bug) | ✅ **PASS — the bug is fixed on both boundaries** — add with an uppercase `node_id` and uppercase method ids → 200, 2 form instances created, 2 ledger lines written, registry grew to 5 services and a new ring appeared. Remove with an uppercase `node_id` and `service_id` → 200 **`{"instances_removed":2,"lines_removed":2}`** and the ledger, registry and ring all went back. Neither call was a no-op. |
| 4 | iOS offline: run Manage Services with no network, then sync; confirm the optimistic change replays and self-heals, and the Active Rooms default view + multi-service k/n chooser behave | ⚪ **Not web-testable** — native app (#517). The server side of that replay is the same `method-lines` / `remove-service` pair verified in step 3, including its idempotency, which is what makes a replayed queue safe. |
| 5 | Confirm a single-service node shows its own metric and a direct tap action, while a multi-service node shows a k/n bubble and a chooser | ✅ **PASS in its web form** — the k/n bubble and chooser are the iOS presentation; on web the same model shows up as per-node cell gating, and it is exact. `by_node` gives each asset 1, 2 or 3 of the 3 services, and a cell for a service the node is not registered for renders **completely empty, with no control** while a registered one renders its own metric plus its check square (CB-3836 showed `Forms: 4` — its own form count — while its IR cell stayed blank). |
| 6 | Confirm iOS check-offs persist onto the 1.60 model (`mapping_node_session.is_checked` via nodeChecks) and that no ghost grouped-task rows appear | ⚪ **Not web-testable** — native app. The web half of the same check-off contract (`PUT …/line-checks`, one unit per node/service, `status → executed`) was verified in this repo's ZP-4018 and PM-programs runs and again here; no grouped-task rows appear on web because `tasks` is in every one of these services' `hide_tabs`. |
| 7 | Negative: a pre-ledger WO still resolves its type from legacy `work_type_id`; a plan released with frozen (pre-reseed) method ids skips and logs instead of returning a 500 | ✅ **PASS on "no 500"** / ⚠️ **partly unverifiable** — two pre-ledger work orders (8 and 10 Aug) return an empty registry, zero ledger lines and empty completion, open normally, and degrade to the legacy view: tabs Assets · Issues · Attachments, no service columns, no wheel, no error. The derived type itself is not exposed in any web-reachable JSON, so the legacy fallback is confirmed by the view it produces rather than by reading a `work_type_id`. On frozen ids, both reachable write paths refuse cleanly with a 400 that names the problem rather than throwing a 500 — see FINDING 2 for the part that differs from "skips and logs". |
| 8 | Migration: verify the Run Database Migrations step succeeds and that `session_method_lines` and `services.wo_view` exist | ✅ **PASS as far as the API shows** — both objects are live and serving on QA (details in the callout above). The deploy step's own log is not visible from the product. |

---

## Findings

### FINDING 1 (Low) — a service's metric column outlives its last registration
After the last node was de-registered from *Clean, Tighten, Torque*, the derived **Forms tab disappeared** and the completion ring disappeared, both correctly — but the `forms_status` **column stayed in the assets grid**, and it was still there after a full page reload. So the tab set and the wheel are recomputed from the registry while the column set is not, leaving a column that can never have a value for any row.

### FINDING 2 (Low) — a batch containing one unknown method id is rejected whole, not skipped
`POST …/method-lines` with one bogus and one valid method id returns **400 `unknown or inaccessible method(s): …`** and adds nothing, rather than registering the valid method and skipping the unknown one. That is safe (no 500, and the message names the offender) but it is not the "skips and logs" behaviour the ticket describes for frozen pre-reseed ids. The PM-plan apply path behaves the same way: 400 `lines[0]: implementation methods do not belong to that service for this class`. The actual plan-release path is not reachable from the web UI, so the exact frozen-id scenario stays unverified.

### FINDING 3 (Low, discoverability) — the surviving submitted form leaves the derived tab set
The submitted form correctly survives Remove Service, but because the Forms tab is derived from current registrations, that tab is gone once the service has no registered node, and the form is only reachable through **More → Forms**. It is not lost, and it is still returned for the work order by the API — but the promise "submitted forms stay" is easier to trust if they stay somewhere the tab bar admits exists.

---

## Test data — direct links (QA)
- Multi-service work order used throughout: https://acme.qa.egalvanic.ai/sessions/11b924b8-f16f-49da-98d8-10ca3f2549dd
- Its registry / ledger / completion: https://acme.qa.egalvanic.ai/api/ir_session/11b924b8-f16f-49da-98d8-10ca3f2549dd/service-registry · https://acme.qa.egalvanic.ai/api/ir_session/11b924b8-f16f-49da-98d8-10ca3f2549dd/method-lines · https://acme.qa.egalvanic.ai/api/ir_session/11b924b8-f16f-49da-98d8-10ca3f2549dd/service-completion
- Asset used for Add / Manage / Remove Service: CB-3836 `51841f5f-1460-4130-842a-867cbfb7581e`
- The submitted form that survived removal: https://acme.qa.egalvanic.ai/api/eg-form-instance/4745511d-a1a0-42d7-93bd-cec7fd543b64
- Pre-ledger work orders: https://acme.qa.egalvanic.ai/sessions/61317671-017a-4d26-8b0e-adb997dfdce3 · https://acme.qa.egalvanic.ai/sessions/e5179624-9f51-4411-b190-3738f49cbc9e
- Single-service PM Forms work order created earlier the same day: https://acme.qa.egalvanic.ai/sessions/b2c2657a-50a0-4d69-af07-6167a0c0d39f

Evidence: `docs/bug-evidence/zp-method-first-multi-service-wo/` — screenshots plus `api-captures.md` with every request and response quoted.

---

## Not covered / honest gaps
- **All of iOS #517** — offline Manage Services replay, the sync-queue target, the Active Rooms default view, the k/n bubble and chooser, and the nodeChecks reconciliation cherry-pick. Native app only.
- **Shared-scope forms surviving a removal** — the fixture's four minted forms were all node-scoped, so "shared-scope forms survive" was not exercised; only "submitted survives" was.
- **The frozen-method-id plan release** — the release path is not in the web UI; the two write paths that are reachable both refuse with a 400 instead of skipping.
- **The derived work-order type as a value** — no web-reachable endpoint returns it, so the legacy-vs-derived distinction was judged from the rendered view.
- **The migration step's deploy log** and the `src/sld/` OTA republish — outside the product.
- **Roles other than Super Admin** — mandatory MFA on QA blocks fresh logins of the other seats.
- Test data added and left in place on the shared QA work order: one submitted "Torque Record" form on CB-3836 (labelled with QA-DEMO values).
