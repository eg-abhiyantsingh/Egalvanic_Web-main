# ZP-5245 — Task #274: Labor rounding mode (minimum) + mobilization as labor hours

**Verdict: PASS on QA, with 1 new Medium UI bug and 1 Low API bug** — build `index-C6DvLko9.js` (QA changed from
`index-CDvWXKDj.js` at 13:25 IST; pricing chunks identical after normalising hashes; all results re-run on `index-C6DvLko9.js`).
Seat SA; roles PM / AM / CP / FM / EE. 13 Nov 2026.

**Recommended status: READY TO RELEASE** (engine and every QA Review row pass; the Save-button bug has a workaround and is
filed separately). If the owner treats the dialog bug as in-scope, To Do with the bug in the comment is the alternative.

## Where it lives
Same places as ZP-5225: Account › Company (Round up to / **Minimum** / Mobilization **Per move** minutes, API
`/api/company/{id}/plan-pricing-defaults` `labor_min_hours`, `mobilization_minutes`), and the plan's Pricing settings dialog
(Labor rounding: Round up to + Minimum; Mobilization: Per move; "Use company defaults"). Engine lines carry `auto_minimum`
("topped up to the Nh minimum"), and `line_kind:"mobilization"` rows with `mobilization_moves` × `mobilization_minutes`
("N hrs moving between locations (M moves × K min)", MOBILIZATION chip).

## Test data (mine, QA-DEMO, site SLD `e2c57dc6…`, service Arc Flash Data Collection = rate-priced)
- P4 `QA-DEMO ZP-5245 13 Nov minimum vs round-up` (55 assets): https://acme.qa.egalvanic.ai/plans/65ccf113-2d50-4b51-bda6-9139df8ea9a0
- P2 `QA-DEMO ZP-5242 13 Nov mobilization units` (4 assets, 6 moves): https://acme.qa.egalvanic.ai/plans/d20d398a-26de-424c-b672-b34e92d1a9df
- P1 `QA-DEMO ZP-5245 13 Nov rounding probe` (141 assets, 7 moves): https://acme.qa.egalvanic.ai/plans/1dfb8acc-dccd-45ce-94cd-b15bf34adaf6
- P3 `QA-DEMO ZP-5245 13 Nov equation-priced service` (Arc Flash Label Placement, priced by equation): https://acme.qa.egalvanic.ai/plans/fed30e95-5328-48f8-bad8-47a8e3448b79
Rates: Electrical Engineer $100 cost / $150 sell; Journeyman $10 / $100.

## Math (inputs → expected → actual, from `GET /api/plans/{id}`)
| Case | Expected | Actual | |
|---|---|---|---|
| P4 raw: EE 9.67 h, JE 8.83 h | — | billed 9.67 / 8.83, $2,333.50 | baseline |
| P4 Round up 8 | next multiple of 8 → 16 / 16 (the "9.3 → 16" case) | 16 / 16; EE $1,600 / $2,400, JE $160 / $1,600, total $4,000 | PASS |
| P4 Minimum 8 | at least 8, then actual → 9.67 / 8.83 (the "9.3 → 9.3" case) | 9.67 / 8.83, $2,333.50 | PASS |
| P2 Minimum 8 (EE 0.67, JE 0.83) | topped up to 8 / 8 | 8 / 8, "topped up to the 8h minimum"; $800 / $1,200 and $80 / $800 | PASS |
| P2 Minimum 8 + mobilization 30 | moves = 2 floors + 3 rooms + 1 unlocated = 6; 6 × 30 = 3 h per tech; 0.67 + 3 = 3.67 → 8 | procedure 5 + mobilization 3 = 8 per tech | PASS |
| P1 Round up 8 + mobilization 30 (moves 7) | mobilization added BEFORE rounding: 25.33 + 3.5 = 28.83 → 32; 23.83 + 3.5 = 27.33 → 32 (after-rounding would be 35.5) | procedure 28.5 + mobilization 3.5 = 32 for each tech; total $8,000 | PASS |
| P1 mobilization 30 alone | 3.5 h per tech; EE $350 / $525, JE $35 / $350 | same | PASS |
| P1 JE subcontracted (round 8 + mob 30) | JE untouched: no rounding, no mobilization row | JE 23.83 h, Sub = Yes, "1 unquoted", no JE mobilization row | PASS |
| P1 EE typed 20 h | kept as typed | billed 20 (EDITED chip), not rounded | PASS |
| P1 hand-entered custom line 5.3 h | kept | 5.3 h, $212 / $477, no mobilization row | PASS |
| P3 equation-priced, round 8 + min 8 + mob 30 | untouched | JE 2.67 h "priced by service equation", no mobilization row, $266.67 unchanged | PASS |
| Company min 8 + mob 15 (+ round 0.5), P2 no override | 0.67 + 6 × 15 min (1.5 h) = 2.17 → 8 | procedure 6.5 + mobilization 1.5 = 8; "6 moves × 15 min" | PASS |
| P1 plan mobilization 30 vs company 15 | plan beats company → 7 × 30 | 7 × 30 = 3.5 h rows; company minimum still applies field by field | PASS |
| Company sets nothing (null) | billed = raw, no flags, no mobilization rows | P1 $6,182.50 / P4 $2,333.50, identical to before | PASS |

## Checked
| Check | How | Result |
|---|---|---|
| QA row: minimum 8 → 9.3-style bills actual, round-up → 16 | P4 math above, UI captions | PASS |
| QA row: mobilization minutes add moves × minutes before rounding | P1 / P2 math above | PASS |
| QA row: subcontracted, equation-priced, hand-added lines untouched (+ typed hours) | P1 / P3, screenshot 8 | PASS |
| QA row: plan-level override beats company default | company 15 min vs plan 30 min; company 0.5 h vs plan 1 h (ZP-5225) | PASS |
| QA row: company that sets nothing bills as before | baselines above | PASS |
| Moves formula | 141 assets: 2 floors + 4 rooms + 1 unlocated = 7; 4 assets: 2 + 3 + 1 = 6 (rooms counted per floor: R1 under C1 and under F1 count twice) | PASS |
| UI | Pricing settings switches/fields, "Use company defaults", captions, MOBILIZATION chip, Billed column, Restore… link | PASS except bug 1 |
| API contract | company endpoint validates (400s, see ZP-5225); `/generate` 200 0.3–1.1 s | bug 2 (plan side) |
| Roles / tenant | see ZP-5225: only SA can change company settings (others 403), other company 422; Pricing settings shown to SA / PM / AM / EE on quotes | PASS |
| Flags | no LD gate for these settings; `feature-planned-work-v2` ON; SIMULATED OFF still renders plan pricing | n/a |
| Performance | plan page 38 API calls, no duplicate app calls, only `plan_preview_html` 2.2 s (report preview) | OK |

## Bugs found
### Bug 1 (Medium, frontend — suggested Krunal): Pricing Settings "Save Changes" is disabled when a quote has only a Minimum or Mobilization override
Steps (re-proven twice: mobilization-only at ~13:38 IST on P2, minimum-only at ~13:46 IST on P2, same SA seat):
1. Open a v2 quote with no pricing overrides → gear "Pricing settings".
2. Turn on Minimum = 8 (leave Round up alone) → Save Changes. Works.
3. Open Pricing settings again.
4. Change anything (for example Mobilization 30 min, or the inflation rate).
- Actual: Save Changes is greyed out from the moment the dialog opens and stays disabled. Workaround: toggle Round up on and
  off; Save then works but writes an explicit `labor_round_hours: 0`, so that quote silently stops following the company
  round-up default.
- Expected: Save enabled; Round up shows the company default (or Off) and is not sent unless changed.
- Cause (bundle `PlanDetail-*.js`): the Round up field is initialised with `${hasAnyOverride ? plan.labor_round_hours : company}`,
  which becomes the string "undefined" when only Minimum/Mobilization are overridden; its validator `isFinite(Number(m))` then fails
  and disables Save. Minimum and Mobilization use a null-safe fallback; Round up does not.
- Evidence: `QA-13Nov-ZP-5245-5-BUG-save-disabled-after-minimum-only-override.png`, video step 5.

### Bug 2 (Low, backend — suggested Avani): plan-level overrides accept out-of-range values
Steps (re-proven twice, 13:50 and 14:01, SA, my P3): `POST /api/plans/{id}/generate` with `pricing.labor_round_hours: 41`,
`-1`, `"abc"`, `labor_min_hours: 41`, `mobilization_minutes: 241` or `-5`.
- Actual: 200; the raw value is stored in `instructions.pricing`; the engine silently clamps (41 → 40, 241 → 240, negative/"abc" → off).
- Expected: 400 with the same messages the company endpoint returns ("labor_round_hours must be between 0 and 40",
  "mobilization_minutes must be between 0 and 240"). The UI blocks these values, so users are not affected today.

JQL run (no existing ticket):
`project = ZP AND created >= -45d AND (text ~ "Pricing Settings" OR text ~ "labor rounding" OR text ~ "mobilization" OR text ~ "Save Changes disabled")`
and `project = ZP AND (summary ~ "Pricing Settings" OR summary ~ "rate missing" OR summary ~ "unpriced" OR summary ~ "requires scheduling")`.

## Observations / questions (not filed)
1. Mobilization rows appear for every in-house rate-priced tech, but on QA **no** labor type has `requires_scheduling = true`
   (all 9 global types are false). The Account and dialog hint says "Every tech whose labor type requires scheduling gets a
   Mobilization row". Either the hint or the rule is off — question for Eric.
2. Round up and Minimum can both be on at once (the ticket calls it a mode). With both on, round-up decides above N and both give N
   below it. Fine, but a "Round up / Minimum" choice would match the wording.

## Enhancement suggestions
- Make Labor rounding a single choice (Off / Round up to N / Minimum N), matching "mode" in the ticket and hint.
- Show the moves breakdown on hover of a MOBILIZATION row ("2 floors + 3 rooms + 1 unlocated = 6"), so estimators can check it.

## Mobile impact
None: quotes and pricing settings are web-only.

## Not covered
- Site walks honour the mode (ticket note): not exercised this round (no QA-DEMO walk with rate-priced labor).

## Evidence
- Video `QA-13Nov-ZP-5245-video-steps.mp4` (82 s, contact-sheet checked). Company-default steps are in the ZP-5225 video (step 5).
- `QA-13Nov-ZP-5245-1-round-up-8-bills-16h.png` (PASS)
- `QA-13Nov-ZP-5245-2-minimum-8-keeps-9.67h.png` (PASS)
- `QA-13Nov-ZP-5245-3-minimum-topup-plus-mobilization-rows.png` (PASS)
- `QA-13Nov-ZP-5245-4-equation-priced-line-untouched.png` (PASS)
- `QA-13Nov-ZP-5245-5-BUG-save-disabled-after-minimum-only-override.png` (FAIL, bug 1)
- `QA-13Nov-ZP-5245-6-company-default-minimum-8-mobilization-15.png` (PASS)
- `QA-13Nov-ZP-5245-7-plan-mobilization-30-beats-company-15.png` (PASS)
- `QA-13Nov-ZP-5245-8-subcontracted-typed-handentered-lines-untouched.png` (PASS)
- Raw: `.playwright-mcp/rfq13/pricing-settings/out/r5_engine.json`, `v5245.json`, `v5225.json`, `r7_extra.json`, `e1_explore.json`.
