# 2026-09-08 — QA: ZP-3941 [Web] Multi-section gear priced as one unit, and per-section pricing could not be set from the UI at all

**Prompt:** the ticket text with its 8-step QA review ("test this ticket too").

## What was done
1. **Read the contract that gates the control** — procedure detail now returns the available unit attributes, and it is **empty for every class on the tenant except MCC and Switchboard**, which satisfies the negative case at the contract level: an empty list is what hides the control.
2. **Checked the migration by counting, across 39 procedures** — all 15 MCC methods across nine services carry the section axis, and those services all predate this work, which is what "the migration ran and applied service-wide" looks like from outside.
3. **Compared the per-section labor figures against Switchboard's** — identical: 10 minutes for arc-flash data collection, 5 for label placement, 24 for cleaning.
4. **Looked for an MCC with a populated section count** — none exists, so the ×12 multiplication and the 1× fallback were never put to a real quote. Said so plainly rather than inferring the payoff.
5. **Left the shared procedure data alone** — the radio, the relabel and the save-reopen-clear cycle were judged from the payload the control reads rather than by mutating a live procedure.
6. Verdict, evidence, artifact page.

## Results (short)
- **Contract PASS and migration PASS** — the axis is exposed, gates correctly per class, and the MCC flip landed everywhere.
- **Multiplication unverified** — the step that would prove the feature pays off.
- **FINDING 1 (Low)** — two Switchboard methods sit off the section axis; possibly deliberate, worth a developer naming which two and why.
- **FINDING 2 (Low, data readiness)** — with no section counts populated, every MCC still prices at 1× today; the backfill is what unlocks the change.

## Deliverables
- Verdict: `docs/bug-reports/2026-09-08-QA-ZP-3941-per-section-pricing-verdict.md`
- Evidence: `docs/bug-evidence/zp-3941-per-section-pricing/`
- Artifact: https://claude.ai/code/artifact/4acd4d82-ea97-45be-8331-f5385fb33e25

## Depth notes (learning)
- **When a control is data-gated, testing the gate data is testing the control.** Reading the available-axes list for 25 classes is a stronger negative case than clicking through a handful of editors — and it costs no writes.
- **Count the migration rather than sampling it.** "15 of 15, across nine pre-existing services" answers the "did it apply everywhere" question that one spot-check cannot.
- **Name the unverified step precisely.** "The multiplication is unverified because no MCC records a section count" tells a developer exactly what fixture to make; "partially tested" tells them nothing.
