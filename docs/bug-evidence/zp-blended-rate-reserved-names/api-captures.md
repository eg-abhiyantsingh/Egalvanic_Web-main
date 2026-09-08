# Reserved formula names (blended_rate) — API captures (QA V1.36, bundle index-CSsDpG3c.js, 2026-09-08)

## Deployment fingerprint (frontend #1333)
Bundle `index-CSsDpG3c.js` (new build overnight): reserved set `new Set(["intake","rates","materials","assets","site","evaluated_hours","evaluated_labor","blended_rate", …keywords])`; row `TextField error={reserved} helperText="Reserved name"`; alert text "`<names>` is a value the system already supplies — rename it and read the value instead of computing it. Saving is blocked until you do."; Save `disabled: saving || missingTotalLaborHours || reserved.length > 0`. Default price formula constant `[{name:"service_price", expr:"MROUND(evaluated_labor * 1.25, 50)"}]`.

## Server-side reservation (PUT /api/procedures-v2/services/{id}/site-walk-config on QA-DEMO hand-built a6342955-a6e4-4269-becb-c706dd91a3a8)
| Body (pricing) | Status | Response |
|---|---|---|
| formulas `[blended_rate = evaluated_labor / evaluated_hours, service_price = blended_rate * evaluated_hours * 1.25]` | **400** | `{"error":"invalid_site_walk_config","problems":["pricing.formulas[0]: 'blended_rate' is a reserved name"]}` |
| formulas `[service_price = MROUND(evaluated_labor * 1.25, 50)]` | **200** | saved (`service_price` allowed) |
| `assumes_burden_rate:true`, formulas `[service_price = evaluated_labor * 1.5]` | **400** | `problems: ["pricing.formulas: define 'total_labor_hours' — assumes_burden_rate costs the work order against the equation's own hours"]` |
| `assumes_burden_rate:true`, formulas `[total_labor_hours = evaluated_hours * 1.1, service_price = blended_rate * total_labor_hours * 1.5]` | **200** | saved |
| formulas `[evaluated_labor = 100, service_price = evaluated_labor]` | **400** | `problems: ["pricing.formulas[0]: 'evaluated_labor' is a reserved name"]` |

Consequence: a config carrying a formula named `blended_rate` can no longer be written on QA (the server refuses at save), so the "#1022-broken config" state exists only as legacy data; none of the 20 acme services carries one (all price `service_price = evaluated_labor`).

## Pricing Setup editor (Services → QA-DEMO hand-built (delete me) → "Pricing setup ✓")
- Rows on open: `total_labor_hours`, `service_price` (both unflagged); burden checkbox checked.
- "Add value" → new row; Name `blended_rate` → field error + helper **"Reserved name"**; alert **"blended_rate is a value the system already supplies — rename it and read the value instead of computing it. Saving is blocked until you do."**; **Save disabled**.
- Name `Blended Rate` (caps + space) → still flagged (name normaliser lower-cases and snake-cases). Name `evaluated_hours` → flagged with the same alert wording.
- Rename to `rate_blend` → flag + alert gone, Save enabled → click Save → `PUT …/site-walk-config` 200 with body `{"site_walk_config":{"version":1,"intake":[],"assets":{"allow_custom":false,"default_hours":0},"pricing":{"assumes_burden_rate":true,"formulas":[{"name":"total_labor_hours","expr":"evaluated_hours * 1.1"},{"name":"service_price","expr":"blended_rate * total_labor_hours * 1.5"},{"name":"rate_blend","expr":"evaluated_labor / evaluated_hours"}]}}}` → re-read confirms all three rows persisted.

## Walk pricing with a formula that READS blended_rate (walk "Test_sitewalk_21" 630ba67a-fa1e-4c86-b5de-aa8a33747b3a, 7 assets, service Arc Flash Data Collection)
- Baseline `GET /api/site-walk/630ba67a…/evaluate`: `service_price = evaluated_labor` → **291.68**; inputs `asset_count 7, evaluated_hours 2.3333, evaluated_labor 291.68, blended_rate 125` (server-supplied), rates EE 150 / JE 100; `ok true`, `missing []`.
- Temporarily set the service formula to `service_price = MROUND(blended_rate * evaluated_hours * 1.25, 50)` (PUT 200) → evaluate → **350** (`125 × 2.3333 × 1.25 = 364.6 → MROUND 50 → 350`), `all_ok true`, no `formula_failed`; walk page "ESTIMATED SERVICE VALUE — ARC FLASH DATA COLLECTION US$350.00".
- NOTE: Arc Flash Data Collection is a **global** service (`is_global true, company_id null`) — the PUT was accepted from the tenant admin and the original formula was restored immediately afterwards (see below).
- Restore: PUT `{version:1, intake:[], assets:{allow_custom:true}, pricing:{formulas:[{service_price = evaluated_labor}]}}` → 200; re-evaluate → **291.68**; walk page back to "US$291.68". "Show calculation" panel while reading blended_rate: `Electrical Engineer · 1.1667 hr @ $150/hr US$175.01 · Journeyman Electrician · 1.1667 hr @ $100/hr US$116.67 · Evaluated labor · 2.3333 hr · $125/hr blended US$291.68 · service_price US$350.00`.

## Global service write (side observation)
`GET /api/procedures-v2/services/d625cfa0…` → `is_global: true, company_id: null`. The Services UI for this global service shows only **"Customize"** (fork) — no "Pricing setup" button — yet `PUT …/site-walk-config` from the tenant Super Admin returned **200** and changed the global config (restored immediately). Out of this ticket's scope; recorded as a finding.

## Spec builder trigger on web ("Update service" dialog, QA-DEMO builder-dialog test 85d67289-a267-4028-877c-1721c697712c)
Dialog: "Describe the changes — the AI revises the spec and creates the next version." textarea "What should change?", uploaders "Add sample forms" / "Add pricing docs", checkboxes "May create new forms", "Set up pricing", "Allow the AI to create new materials, presets & test equipment for my company"; button "Update Service". Endpoint per bundle: `POST /procedures-v2/services/{id}/build {…create_forms, set_pricing…}`.

## One real AI build with "Set up pricing" (QA-DEMO builder-dialog test, 2026-09-08 09:39 UTC)
`POST /api/procedures-v2/services/85d67289…/build {"revision_request":"Set up pricing for the site walk: price the service off the blended labor rate with a 25% markup, rounded to the nearest $50, and bill the crew's on-site hours rather than the method hours.","allow_catalog_creation":false,"create_forms":false,"set_pricing":true}` → 200; Step Function `eg-pz-qa-ai-service-spec-sfn-ohio:dde646c6-caa5-4db6-ae46-17a3a9654aa4`.
After ~107 s: `build_job.status "applied"`, `error null`, `applied_version_id 005a7ead-e708-46f5-96ca-7cda269250a2`, `current_version 2` — but `site_walk_config: null`, `pricing_status "draft"`, service page still "Needs pricing" / "Pricing setup" (no ✓). The build applied a new spec version without any pricing config; the pipeline's reasoning/output is not exposed to the web app.

### What the AI actually authored (GET /api/procedures-v2/services/85d67289…/versions/005a7ead…)
```
spec.site_walk = {
  "assets": {"allow_custom": true, "default_hours": 1},
  "intake":  [{"name": "On-site hours", "required": true, "type": "number"}],
  "pricing": {"formulas": [
      {"name": "blended_rate", "expr": "evaluated_labor / evaluated_hours"},      ← RESERVED NAME, re-derived
      {"name": "billed_labor", "expr": "intake.on_site_hours * blended_rate"},
      {"name": "subtotal",     "expr": "billed_labor * 1.25"},
      {"name": "service_price","expr": "MROUND(subtotal, 50)"}]},
  "version": 1 }
```
No `assumes_burden_rate`, no `total_labor_hours` (the markup equation was authored without the flag). Version 1 of the same service had no `site_walk` block at all.
`GET …/build-status` progress log (the agent's own words): "This is a revision job — I need to add site-walk pricing to an existing service…" → "Validation passes and everything else in the spec was left untouched… I've added site-walk pricing to this service. The job hours captured on the walk aren't used for billing — instead, the crew's actual on-site hours (a new intake question) get multiplied by the blended labor rate implied by the service's own methods (total method labor cost ÷ total method hours)…"; final stage "Service spec ready — applying…", `status done`.
Outcome on the service: `build_job.status "applied"`, `error null`, version 2 current, **`site_walk_config: null`, `pricing_status "draft"`**, page "Needs pricing" — the reserved-named pricing block was dropped at apply with no error shown anywhere.
