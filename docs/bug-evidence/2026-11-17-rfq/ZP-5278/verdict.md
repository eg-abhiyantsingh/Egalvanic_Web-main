# ZP-5278 — Pricing settings "Save Changes" stays disabled when a quote only has a Minimum or Mobilization override

**Verdict: PASS**

- **Build:** QA `index-oFAZQ3rK.js` (checked 17 Nov 2026, 11:12 IST and again before writing). PlanDetail chunk `PlanDetail-DTqnObBk.js`.
- **Fix:** eg-pz-frontend PR #2030 (merged to `cicd/qa` on 16 Nov, 07:21 UTC). It is one line in `PricingView.openSettings`: Round up now falls back per key, like Minimum and Mobilization do. I confirmed the deployed chunk has the new code: `const t=\`${ht!=null&&ht!==""?ht:Ve}\``.
- **Seats:** Super Admin for every row. Project Manager and Account Manager: see "Roles" below.
- **Quotes (my own QA-DEMO fixtures):**
  - P2, the ticket's quote: https://acme.qa.egalvanic.ai/plans/d20d398a-26de-424c-b672-b34e92d1a9df
  - P4 ("minimum vs round-up"): https://acme.qa.egalvanic.ai/plans/65ccf113-2d50-4b51-bda6-9139df8ea9a0
- **Feature flag:** none. The dialog's labor rows show for any `plan-engine/v2` quote.

## What was run (UI dialog + the generate payload + GET /api/plans/{id} after every save)

| # | Starting state | Action | Save on reopen | Sent in POST /generate (`pricing`) | Stored `instructions.pricing` afterwards |
|---|---|---|---|---|---|
| 1 | No overrides | Open the dialog | Enabled; Round up, Minimum and Mobilization all Off | — | no keys |
| 2 | Mobilization only (30) | Reopen, then turn on Minimum 8 | **Enabled** (was disabled before the fix) | `labor_min_hours 8, mobilization_minutes 30`, no round key | `{min 8, mob 30}`, `labor_round_hours` absent |
| 3 | Both (Minimum 8 + Mobilization 30) | Reopen, then Save with no change | Enabled | same two keys, no round key | unchanged |
| 4 | **Minimum only (8), the ticket's steps** | Reopen, then Per move 30 | **Enabled** before and after the change | `min 8, mob 30`, no round key | `{min 8, mob 30}`, `labor_round_hours` absent |
| 5 | Minimum only | The old workaround: Round up on → off, then Save | Enabled | **no `labor_round_hours`** | absent. The side effect (a pinned 0) is gone |
| 5b | Minimum only | Change only the inflation rate (3 → 3.5) | Enabled | no round key | inflation 0.035, Minimum kept; reset to 0.03 afterwards |
| 6 | Existing Round up 8 | Reopen (shows 8), add Mobilization 30, then turn Round up off | Enabled | `round 8` kept, then `round 0` once the user turns it off | `{round 8, mob 30}` → `{round 0, mob 30}` (an explicit user change, as expected) |
| 7 | Overrides set | "Use company defaults" | Enabled | all three `null` | all `null` |
| 8 | Company Round up default set to **4 h** (temporary), quote has Minimum only | Reopen, add Mobilization 30, then turn Round up off | Enabled. Round up opens **On, 4** ("Company default: round up 4 h") | no round key, then `round 0` when turned off | absent → the quote **follows the company 4 h** (labor billed `r4`). After turning it off, an explicit 0 (opt-out) |

Video run on P2 (the ticket's quote): Minimum 8 saved ("topped up to the 8h minimum") → reopen: Save enabled → Per move 30 saved ("6 moves × 30 min").
`GET /api/plans/d20d398a…` → `labor_min_hours 8, mobilization_minutes 30`, `labor_round_hours` not saved.

Pricing stays correct across the matrix (P4, 9.67 h + 8.83 h, $150/$100 per h):
- No overrides: $2,333.50.
- Mobilization 30: $3,208.50 (+3.5 h per tech).
- Round up 8: $4,000 (16 h billed per tech).
- Company 4 h + Mobilization: $4,000 (12.5 + 3.5 = 16 h).

EG-ACME company pricing defaults were changed to Round up 4 h four times, briefly each time:
- 05:50:32–05:50:41 UTC
- 06:00:32–06:00:51 UTC
- 06:07:40–06:07:47 UTC (for the screenshot)
- 06:21:28–06:21:35 UTC (screenshot re-shot without text labels)

Each time they were restored to null, confirmed by an API read-back. P2 and P4 were reset to no overrides at the end.

## Roles
Project Manager and Account Manager seats both got the same result. Each seat signed in on its own, opened P4, and used the gear → Pricing settings: Minimum 8 → Save → reopen → **Save enabled** → Per move 30 → Save enabled → saved.
- `GET /api/plans/{id}` afterwards: `{labor_min_hours 8, mobilization_minutes 30}`, with `labor_round_hours` absent.
- P4 was reset after each seat.

## Not covered / notes
- Quotes saved with the old workaround **before** this fix still hold `labor_round_hours: 0`. There is no data migration, so they keep ignoring a company round-up default until someone changes Round up on them. This is informational. QA's own 13 Nov quote was cleaned up at the time.
- Minor note, not a defect of this ticket: the dialog's "Company default" line and the Round up value come from the quote's last generated content. If an admin changes the company default, the dialog shows the old default until the quote is regenerated (any save does that).
- Mobile not checked. Quote Pricing settings is a web Sales screen, and I did not look for an iOS or Android equivalent.

## New defects
None from this ticket.
