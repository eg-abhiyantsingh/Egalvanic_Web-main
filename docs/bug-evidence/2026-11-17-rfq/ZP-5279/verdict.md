# ZP-5279 — Quote showed a labor-priced material as "$0 / $0" with no "rate missing" marker when the work order has no rated labor

**Verdict: PASS**

- **Build:** QA `index-oFAZQ3rK.js` (17 Nov 2026). PlanDetail chunk `PlanDetail-DTqnObBk.js`.
- **Fix:** eg-pz-frontend PR #2031 (merged to `cicd/qa` on 16 Nov, 09:05 UTC). It carries `rate_missing`/`unpriced` into the materials table, adds a "RATE MISSING"/"UNPRICED" chip, a pink row and "—" instead of $0, and adds a material line to the "PRICING NEEDS ATTENTION" banner. All of these strings are in the deployed chunk.
- **Seat:** Super Admin. Project Manager and Account Manager: see "Roles".
- **Quote:** the ticket's quote P2, https://acme.qa.egalvanic.ai/plans/d20d398a-26de-424c-b672-b34e92d1a9df (my QA-DEMO fixture).
  - Customer view: https://acme.qa.egalvanic.ai/plans/d20d398a-26de-424c-b672-b34e92d1a9df?view=customer
  - Material "QA-DEMO ZP-5242 13 Nov trip charge 10 min" (`0d80e613-e914-44a7-89f0-37cadd0c3d17`): Mobilization Units, 10 min each, ×6.
  - Control material "…trip charge fixed 25" (`f1932f82…`).

## What was run

| # | State | API (`content.workorders[0].materials[]`) | Pricing tab | Banner |
|---|---|---|---|---|
| 1 | As found: the dev had left every labor type removed, plus Minimum 8 / Mobilization 30 | cost 0, sell 0, `unpriced: true, rate_missing: true` | Row: name + **RATE MISSING** chip, "10 min each @ blended labor rate — no rated labor on this work order", pink background `rgb(253,236,234)`, Cost **—**, Sell **—** | **PRICING NEEDS ATTENTION**: "QA-DEMO ZP-5242 13 Nov trip charge 10 min (material): no rated labor on this work order — priced by labor time (10 min each), so it is $0. Restore or add own (non-subcontracted) labor, or give the material a fixed price." |
| 2 | Same, **Customer View** | — | Row "×6 Mobilization Units · —". No chip, no banner, no "$0". Fixed-price row $180 | hidden from the customer (correct) |
| 3 | Control: labor restored, no overrides | cost 50.20, sell 122.33, no flags | "$50 / $122", subtext "10 min each @ blended labor rate", no chip | none |
| 4 | Every labor type set to Sub = Yes | cost 0, sell 0, `unpriced + rate_missing` | RATE MISSING chip, "—" | lists the material (plus "Pending: 2 sub quotes to enter" for the labor) |
| 5 | **The ticket's steps 4–5 in the UI**: trash icon on both labor rows | cost 0, sell 0, flags true | RATE MISSING, pink, "—" | lists the material |
| — | After removing only ONE labor type (seen in the video) | priced from the remaining labor: $10 / $100 | normal row | none |

- The fixed-price material "trip charge fixed 25" stays at $150 / $180 with no chip in every state.
- Materials total is $150 / $180, so the unpriced line adds nothing.
- The 13 Nov repro on the same quote (before the fix): "$0 · $0", no marker, $0 line in the customer view. Now: chip, banner and "—".
- P2 was restored at the end: both labor types back, no overrides, both materials, $485.83.

## Roles
Project Manager and Account Manager seats: P2 was put into the no-rated-labor state with that seat's own session (generate → 200).
- Each seat sees the RATE MISSING chip, the "—" cells and the same PRICING NEEDS ATTENTION line.
- P2 was restored after each seat.

## Not covered / notes
- The "UNPRICED" chip variant (a material with no price on file and not labor-priced). The only library item with no price is someone else's "New materials" (no unit). I did not add it to a quote.
- The multi-work-order "All" view (one WO without labor merged with one with labor). I did not have a multi-WO quote with a labor-priced material. The PR says a partly priced merged row keeps its sum.
- Proposal preview (Short Form Proposal): it shows only the total ("The total investment for this program is $180.00 (pre-tax)") and no per-material lines, so it shows no $0 line either. Unchanged.
- Enhancement idea, not a defect: a RATE MISSING material raises no Action Item, so "Mark as Sent" stays enabled (seen in state 1). When every labor type was subcontracted (state 4), "Action Items 2" appeared and Mark as Sent was greyed because of the pending sub quotes. Counting RATE MISSING the same way would stop a quote going out with a $0 trip charge.
- Mobile not checked. The quote Pricing tab is a web Sales screen.

## New defects
None.
