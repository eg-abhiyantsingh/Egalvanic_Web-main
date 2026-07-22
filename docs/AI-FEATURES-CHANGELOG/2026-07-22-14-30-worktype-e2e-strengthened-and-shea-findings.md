# Work-Type E2E create flow strengthened (fill ALL fields + verify persistence) + Shea quote findings PDF

**Date:** 2026-07-22
**Time:** 14:30 UTC
**Author:** Claude Code (automation, Fable 5), for shubham.goswami@egalvanic.com

## What was asked
1. Ship a proper PDF of the Shea Electric "Cancer Center quote" labor-inflation findings — with the
   URL and honest evidence of whether it's a real bug.
2. Fix a real weakness in the Work-Type tests: the create-E2E matrix was only *selecting the Work
   Type from the dropdown and clicking Create* — it never filled the other fields or proved they
   saved. Make it a true end-to-end flow: select the type, fill all the values, create, then verify
   everything persisted.

## What shipped

### 1. Shea findings — corrected, honest PDF
`docs/bug-repro/shea-quote-labor-inflation/Shea-Quote-Labor-Findings.pdf` (4 pages, screenshots
embedded). The earlier draft overclaimed ("reproduced on QA", "$856 for 5 min = inflation"); both
were wrong and are corrected here:
- **Shea's inflated total was NOT reproduced on QA.** Scanned all 81 company quotes (6 have data);
  the customer-facing/billed total is never higher than the honest work — the two large
  multi-procedure quotes (845 & 854 lines, ~550 h) reconcile to within 0.1 h.
- **A real, narrower defect WAS found** on the inspected quote: internal labor-hour fields
  contradict each other — Line Items 16 h vs labor-line `est_hours` 8 h vs a `procedure_breakdown`
  of `14 × 420 = 98 h` on the same line (a count-times-count squaring pattern: 420 = 14 × 30), plus
  a second line whose `est_hours` is mis-derived (2.17 → 0.17) and a phantom 0-minute blank row.
  These don't corrupt the price on the quotes tested but are genuine calculation bugs and the
  likely root of Shea's issue on their own data.
- The 4-hour billed values on tiny tasks are a normal **4-hour trip minimum**, not inflation.
- Fix ownership: backend pricing/quote service (app engineering), tickets ZP-1092 / ZP-783 /
  ZP-1407. Shea's specific 219-vs-80 needs their actual quote data or the exact repro steps.

### 2. `WorkTypeCreateE2EMatrixTestNG` — real end-to-end create + persistence
Previously TC_WTC_001 filled only WO Name + Work Type, then clicked Create — it exercised the
dropdown but proved nothing about a fully-filled work order. Now, per type (all 14):
- **Create phase fills every field:** WO Name, Work Type (committed from the dropdown, verified),
  scope (full for the 6 family reps / Start-Empty for other preview services), **Due Date**
  (30 days out), **Priority** (rotated High/Medium/Low across the 14 types), **Est. Hours**,
  **WO Description**, and a **Team field technician**.
- **Verify phase now asserts persistence** (round-trip): opens the created WO, expands the detail
  header, and asserts the **Priority**, **Due Date** (via the Timeframe), **Description**, and
  **technician** we entered actually saved — on top of the existing chip / tab-strip /
  preview-vs-created-asset-count checks.
- New page-object readers to support it: `WorkOrderPage.getWoDetailHeaderField(label)` and
  `getWoDetailHeaderText()` (read the expanded detail-header label→value panel:
  PRIORITY / FACILITY / TIMEFRAME / CERTIFIER / FIELD / SCHEDULE, live-verified 2026-07-22).

## Depth explanation (for learning + manager review)

**Why the old test was weak.** A create test that fills only the two required fields and asserts
"a WO exists" passes even if Priority, Due Date, Description, and crew silently fail to save. It
proves the dropdown commits and the Create button works — nothing about whether a *complete* work
order round-trips. The strengthened version fills the whole form and then reads each value back
off the created WO's detail page, so a regression that drops a saved field now fails a test
instead of sliding through green.

**Why persistence verification had to read the collapsed header.** The WO detail header is
collapsed by default and the fields (Priority/Timeframe/Field-technician) are hidden until the
chevron is clicked — the same collapse behavior pinned earlier for the priority chip. The new
readers expand it first, then match label→value pairs, with page-body substring fallbacks so a
layout tweak doesn't make the assertion brittle.

**On the Shea investigation — the discipline that mattered.** The honest outcome (could not
reproduce; found a smaller real bug) came from *checking the falsifiable claim* rather than
defending the first theory. Scanning all quotes and finding the big ones reconcile perfectly is
what turned "it inflates on large scopes" from a guess into a disproven hypothesis — and kept a
retracted over-report from going to the customer.
