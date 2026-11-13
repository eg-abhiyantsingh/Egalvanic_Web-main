# ZP-5225 — Plans v2: round labor hours up to the nearest N (company default, opt-in) with per-plan override

**Verdict: PASS on QA** — build `index-C6DvLko9.js` (QA changed from `index-CDvWXKDj.js` at 13:25 IST mid-round; the
Account / PlanDetail / Materials chunks are identical after normalising hashes; every result below is from `index-C6DvLko9.js`).
Seat: SA (staff Super Admin); roles PM / AM / CP / FM / EE checked. 13 Nov 2026.

**Recommended status: READY TO RELEASE** (new bugs found in the same area are listed under ZP-5245 / ZP-5242).

## Where it lives
- Company default: Account & settings → Company → **Quote labor rounding** (Round up to [hrs] switch + field, Minimum, and
  Mobilization "Per move" + Save). API `GET/PATCH /api/company/{company_id}/plan-pricing-defaults`
  `{labor_round_hours, labor_min_hours, mobilization_minutes}` (null = off). Shown only with `company_data.manage`.
- Per plan: plan page `/plans/{id}` → Pricing tab → gear "Pricing settings" → Labor rounding / Mobilization; saved into
  `instructions.pricing.*` through `POST /api/plans/{id}/generate`; the engine echoes `content.pricing.labor_round_hours` and
  `labor_round_hours_company`. Lines carry `billable_hours`, `auto_rounded` ("rounded up to nearest Nh" caption).

## Shared setting changed and restored (EG-ACME)
| When (IST) | Value (API read-back) |
|---|---|
| 13:16 and 13:57:44 (before) | `{labor_round_hours: null, labor_min_hours: null, mobilization_minutes: null}` (screenshot 0) |
| 13:57:57 → 13:58:01 | `labor_round_hours: 0.5` (UI Save, toast "Quote labor settings saved.") |
| 13:58:31 | `0.5 / labor_min_hours 8 / mobilization_minutes 15` (ZP-5245 part) |
| **13:59:08 restored** through the UI (all three Off → Save) | `{null, null, null}`; re-read again at 14:01:15 → still all null |

Window: 71 s. Only my QA-DEMO plans were re-priced during it.

## Math (inputs → expected → actual)
Plan `QA-DEMO ZP-5245 13 Nov rounding probe` (https://acme.qa.egalvanic.ai/plans/1dfb8acc-dccd-45ce-94cd-b15bf34adaf6), raw
labor EE 25.33 h ($100 cost / $150 sell), JE 23.83 h ($10 / $100).

| Case | Expected | Actual | |
|---|---|---|---|
| Company 0.5, no plan override | 25.33 → 25.5, 23.83 → 24.0 | billed 25.5 ($2,550 / $3,825), 24 ($240 / $2,400); `instructions.pricing` has no override | PASS |
| Plan override 1.0 (company still 0.5) | 25.33 → 26, 23.83 → 24 | 26 ($2,600 / $3,900), 24; `labor_round_hours 1`, `_company 0.5` | PASS |
| Company Off, plan "Use company defaults" | raw | 25.33 / 23.83, total $6,872.50 (same as before the change) | PASS |
| Plan override 8 (other plan, 9.67 h) | 16 | 16 ($1,600 / $2,400) — see ZP-5245 | PASS |

## Checked
| Check | How | Result |
|---|---|---|
| QA row 1: set company default 0.5, opt in, save | Account → Company UI; API read-back 0.5 | PASS |
| QA row 2: v2 plan labor rounds UP to 0.5 | Pricing settings → Save Changes re-prices; 25.33 → 25.5 | PASS |
| QA row 3: per-plan override 1.0 ignores company default | 25.33 → 26 while company = 0.5 | PASS |
| QA row 4: opt the company out → raw | company Off + "Use company defaults" → 25.33 / 23.83 | PASS |
| QA row 5: plan with no labor lines saves normally | QA-DEMO plan `d20d398a` with both labor types removed: generate 200, priced materials only, no error; new empty v2 plan creates 201 | PASS |
| QA row 5b: migration (single alembic head, column) | backend-only; column proven present by the API (`labor_round_hours` stored/returned) | API-level PASS |
| Company endpoint validation | PATCH 41 / -1 / "abc" / min 41 / mob 241 / mob -5 → 400 with clear messages, value unchanged; `{}` → 400 "one of … is required" | PASS |
| UI validation | Account field max 40 h, step 0.5; plan field 0–40 | PASS |
| Roles | PM, AM, CP, FM, EE: no Company tab, PATCH → 403 "Admin access required" (GET 200, read only) | PASS |
| Tenant | GET/PATCH another company's defaults → 422 `permission_denied` for every seat incl. SA | PASS |
| Flags | no LaunchDarkly gate on the setting; plan page reads `feature-planned-work-v2` (ON). SIMULATED OFF: Account + plan pricing still render | n/a |
| Performance | Account page: 29 API calls, none > 2 s, no duplicate app calls; `plan-pricing-defaults` ~280 ms; generate 0.3–1.1 s | OK |

## Bugs found
None that fail this ticket. Related findings filed under the sibling tickets:
- ZP-5245 bug 1 (Medium, frontend): Pricing Settings "Save Changes" disabled when a plan has only a Minimum or Mobilization override.
- ZP-5245 bug 2 (Low, backend): plan-level overrides accept out-of-range values through `/generate` (company endpoint rejects them).

## Observations / questions (not filed)
1. Existing plans keep their old hours after the company default changes, until something re-prices them, and the plan's Pricing
   Settings still says "Company default: round up off" (it reads the default captured at the last re-price). Seen once, at 13:58
   on plan `1dfb8acc` (video step 3). Question for Eric: intended (quotes never change silently)?
2. Accessibility (Low): the three Account switches render without an accessible name (the code passes
   `aria-label "Round up to on/off"`, but no input carries it), so a screen reader hears three bare "switch" controls.

## Enhancement suggestions
- On a plan, when the company default differs from what the plan was priced with, show "Company default changed to 0.5 h —
  Re-price" so estimators know why numbers differ between quotes.
- Show the effective rule on each labor row tooltip ("company default 0.5 h" vs "this quote: 1 h").

## Mobile impact
None: quotes and pricing settings are web-only.

## Not covered
- Alembic single-head check (backend repo, not web-testable).

## Evidence
- Video `QA-13Nov-ZP-5225-video-steps.mp4` (103 s; seat e-mail masked; contact-sheet checked).
- `QA-13Nov-ZP-5225-0-company-default-original-off.png` (PASS, original state)
- `QA-13Nov-ZP-5225-1-company-default-round-up-0.5-saved.png` (PASS)
- `QA-13Nov-ZP-5225-2-plan-uses-company-0.5-25.33-to-25.5.png` (PASS)
- `QA-13Nov-ZP-5225-3-plan-override-1h-beats-company-0.5.png` (PASS)
- `QA-13Nov-ZP-5225-4-company-default-restored-off.png` (PASS, restored)
- `QA-13Nov-ZP-5225-5-opted-out-raw-hours-again.png` (PASS)
- Raw: `.playwright-mcp/rfq13/pricing-settings/out/v5225.json`, `r6_roles.json`, `r7_extra.json`.
