# ZP-5242 — Task #272: Mobilization material unit + labor-priced material add-ons

**Verdict: PASS on QA (Plans v2), with 1 new UI bug** — build `index-C6DvLko9.js` (QA changed from `index-CDvWXKDj.js` at
13:25 IST; Materials / PlanDetail chunks identical after normalising hashes; all results re-run on `index-C6DvLko9.js`).
Seat SA; roles PM / AM / CP / FM / EE. 13 Nov 2026.

**Recommended status: READY TO RELEASE** (the "$0 with no marker" defect is filed separately; v1 engine / legacy quote parity
was not covered, see below).

## Where it lives
- Builder → Materials (`/materials`, Admin / Super Admin only) → Create Material / click a row → modal "Material Information":
  Unit picker (calculated units carry an orange icon), **Priced by: Fixed price | Labor time**, "Minutes per unit" (1–1440),
  Price + Default Margin hidden for Labor time. List Price column shows "N min @ blended".
- API: `GET/POST /api/materials-library`, `PUT /api/materials-library/{id}` with `labor_minutes`; unit
  `a1b2c3d4-0001-4000-8000-00000000000c` "Mobilization Units", `is_calculated: true`, global.
- Plan: Pricing tab → MATERIALS table (header Material / Qty / Cost / Sell; customer view Material / Qty / Price) and Manage
  Materials; engine material lines carry `quantity`, `calculated`, `labor_minutes`, `blended_rate`, `unpriced`, `rate_missing`.

## Test data (mine, QA-DEMO)
- Plan `QA-DEMO ZP-5242 13 Nov mobilization units`: https://acme.qa.egalvanic.ai/plans/d20d398a-26de-424c-b672-b34e92d1a9df
  (Arc Flash Data Collection, rate-priced; assets in B1/C1/R1, B1/F1/R1, B1/F1/Test1, plus one asset with no location).
- Materials: `QA-DEMO ZP-5242 13 Nov trip charge 10 min` (Mobilization Units, labor 10 min, default margin 50 % set via API to
  prove it is ignored), `QA-DEMO ZP-5242 13 Nov trip charge fixed 25` (Mobilization Units, $25, 20 % margin), and
  `QA-DEMO ZP-5242 13 Nov UI labor add-on 15 min` (created through the UI modal).

## Math (inputs → expected → actual)
| Case | Expected | Actual | |
|---|---|---|---|
| Qty, 3 located assets | distinct floors {C1, F1} = 2 + distinct rooms 3 = **5** | ×5 Mobilization Units | PASS |
| Qty, + 1 asset with no location | 5 + 1 = **6** | ×6 | PASS |
| Labor-priced, 3 assets: EE 0.5 h ($50 / $75), JE 0.5 h ($5 / $50) | blended cost 55/1 = $55, sell 125/1 = $125; 5 × 10/60 = 0.833 h → cost $45.83, sell $104.17 (50 % margin would give $156.25) | $45.83 / $104.17, `blended_rate 125` | PASS |
| Labor-priced, 4 assets: EE 0.67 h ($67 / $100.50), JE 0.83 h ($8.30 / $83) | blended cost 75.3/1.5 = $50.20, sell 183.5/1.5 = $122.33; 6 × 10/60 = 1 h → $50.20 / $122.33 | $50.20 / $122.33 (screen $50 / $122) | PASS |
| With mobilization rows (min 8 + 30 min) | blended over all rows: cost 405.3/7.5 = $54.04, sell 933.5/7.5 = $124.47 | $54.04 / $124.47 | PASS |
| Fixed $25 Mobilization Units, 20 % margin, qty 6 | $150 cost, $180 sell (margin applies to fixed-price) | $150 / $180 | PASS |
| No rated labor (both labor types removed) | stays on BOM, unpriced / rate missing, no fabricated price | API `cost 0, sell 0, unpriced: true, rate_missing: true` | PASS (API) — screen: bug 1 |
| All labor subcontracted | same as above | `unpriced + rate_missing` | PASS (API) |

## Checked
| Check | How | Result |
|---|---|---|
| Materials Library: create material with unit Mobilization Units, flagged calculated | UI modal; unit option has the calculated icon; created via UI (unit Mobilization Units, labor 15 min, price null) | PASS |
| Price basis Labor time, minutes validation | UI: blank / 0 / 1441 → "Enter minutes greater than 0, up to 1440"; API: 0 / -5 / 1441 → 400 "labor_minutes must be greater than 0 and at most 1440", "abc" → 400; 1 and 1440 → 200 | PASS |
| Switching to Labor time warns about the fixed price | edit the $25 material → "Saving as labor time clears the $25.00 fixed price." (cancelled) | PASS |
| List shows "N min @ blended" | "10 min @ blended", "15 min @ blended" | PASS |
| Qty = floors + rooms (+1 unlocated) | 5 → 6 above; rooms with the same name on different floors count separately (7 moves on the 141-asset site) | PASS |
| Cost/sell = qty × minutes/60 × blended rate, no default margin | math above | PASS |
| No rated labor → unpriced / rate_missing, no fabricated price | API yes; plan screen shows $0 / $0 with no marker | PASS (API), bug 1 (UI) |
| Rename "Mobilization Units" everywhere | unit picker, list Unit column, plan BOM "×6 Mobilization Units", `/api/material-units` | PASS |
| Materials table header row + one-line quantity | admin: Material / Qty / Cost / Sell; customer view: Material / Qty / Price (no Cost); qty cell `white-space: nowrap`, `title="×6 Mobilization Units"`, one line | PASS |
| Totals row under its columns | "Materials total $200 / $302" aligned | PASS |
| Calculated quantity "not hand-editable" | Manage Materials shows an editable box with placeholder "auto"; a typed qty (3) overrode the computed 6 (API) | question (see below) |
| v1 engine / legacy quote parity | — | NOT COVERED |
| Roles | `/materials` page: SA yes; PM / AM / EE "Access denied"; CP / FM land in the portal. API `PUT /materials-library/{id}` (my material, identical body): PM / AM / EE / FM 200, CP 422 | page PASS; FM API: bug 2 |
| Tenant | CP (no library access) gets 422; another company's pricing defaults 422 (ZP-5225). Per-row company_id of the library list not checked | partial |
| Flags | no LD gate; `feature-bulk-pm-settings` only gates Bulk Manage on the Materials page | n/a |
| Performance | `GET /materials-library` ~0.7 s, 22 rows; generate 0.3–1.1 s | OK |

## Bugs found
### Bug 1 (Medium, frontend — suggested Krunal): a labor-priced add-on with no rated labor shows "$0 / $0" with no "rate missing" marker
Steps (re-proven: API at 13:30 IST, UI at 13:51 IST, same SA seat, my QA-DEMO plan):
1. Builder → Materials → create a material with unit Mobilization Units, Priced by Labor time, 10 min.
2. Open a v2 quote → Manage Materials → add it → Save.
3. Remove both labor types from the work order (trash icon on each labor row).
- Actual: the material stays with "×6 Mobilization Units · $0 · $0", and nothing says it is unpriced. The API marks it
  `unpriced: true, rate_missing: true`. Labor rows in the same table get a red "NO RATE" chip; materials get nothing, and the
  customer view and proposal show the $0 line too.
- Expected: a visible "rate missing / unpriced" marker on the material row (as for labor), so the quote is not sent with a silent $0
  trip charge.
- Evidence: `QA-13Nov-ZP-5242-6-no-rated-labor-0-dollars-no-rate-missing-marker.png`, video step 11. Bundle check: `PlanDetail`
  never reads `rate_missing` / `unpriced` for materials.

### Bug 2 (High, backend, OUT OF TICKET SCOPE — suggested Avani): a Facility Manager (guest-portal) seat can read and re-price internal quotes through the API
Steps (re-proven three times, 13:40, 13:51, 14:01 IST; FM seat; CP seat as control):
1. Sign in as the QA Facility Manager seat (roles: Facility Manager only). The web UI shows only the Maintenance Portal.
2. `GET /api/plans` → 200, 119 quotes. `GET /api/plans/1dfb8acc-…` → 200 with `total_cost`, `margin`, labor `cost_rate` (internal
   cost and margin).
3. `POST /api/plans/fed30e95-…/generate` with the quote's own instructions → 200, `modified_at` changes (write accepted, my QA-DEMO quote).
4. `PUT /api/materials-library/{my QA-DEMO material}` (identical body) → 200.
- Control: the Client Portal seat gets 422 `permission_denied` on the same calls.
- Expected: a guest-portal role gets no internal quote, cost, margin or material-library access (same as CP).
- Note: Facility Manager can no longer be given to new users (owner, 29 Oct), but existing FM users keep the role.

JQL run (no existing ticket):
`project = ZP AND (summary ~ "Pricing Settings" OR summary ~ "rate missing" OR summary ~ "rate_missing" OR summary ~ "unpriced" OR (summary ~ "Facility Manager" AND summary ~ "plan") OR (summary ~ "Facility Manager" AND summary ~ "quote"))`
and `project = ZP AND created >= -120d AND (summary ~ "portal" OR summary ~ "guest") AND (summary ~ "plan" OR summary ~ "quote" OR summary ~ "pricing" OR summary ~ "cost")`.

## Observations / questions (not filed)
1. The ticket says the Mobilization Units quantity is "not hand-editable". In Manage Materials the box is editable (placeholder
   "auto"), and a typed quantity overrides the computed one (6 → 3). Other calculated units behave the same. Intended override,
   or should it be read-only?
2. The blended rate is weighted by estimated hours, not billed hours: with Minimum 8 the add-on stayed at $122.33/h although the
   billed labor works out to $2,000 / 16 h = $125/h. Small difference; confirm which one is intended.
3. Ticket example "2 floors and 3 rooms → quantity = 8" does not match its own formula (2 + 3 = 5); QA observed 5.

## Enhancement suggestions
- Show the quantity breakdown on hover ("2 floors + 3 rooms + 1 unlocated = 6") and the price formula ("6 × 10 min × $122.33/h").
- In the Materials list, add a "Priced by" filter (Fixed / Labor time) once libraries grow.

## Mobile impact
None: the materials library and quotes are web-only.

## Not covered
- v1 plan engine and legacy quote parity (same quantity and labor price on all three engines): there are no v1 / legacy quote
  fixtures on QA with this site, and building a legacy quote was out of this round's time budget. Needs a legacy quote on the
  same site with the same assets plus the Mobilization Units material.

## Evidence
- Video `QA-13Nov-ZP-5242-video-steps.mp4` (71 s, contact-sheet checked).
- `QA-13Nov-ZP-5242-1-labor-time-minutes-validation.png` (PASS)
- `QA-13Nov-ZP-5242-2-materials-list-min-at-blended.png` (PASS)
- `QA-13Nov-ZP-5242-3-clear-fixed-price-warning.png` (PASS)
- `QA-13Nov-ZP-5242-4-plan-materials-header-mobilization-units-labor-priced.png` (PASS)
- `QA-13Nov-ZP-5242-5-customer-view-material-qty-price.png` (PASS)
- `QA-13Nov-ZP-5242-6-no-rated-labor-0-dollars-no-rate-missing-marker.png` (FAIL, bug 1)
- Raw: `.playwright-mcp/rfq13/pricing-settings/out/r5_engine.json`, `v5242.json`, `r6_roles.json`, `r7_extra.json`.
