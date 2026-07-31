# Re-verify /planned_workorder_line/ timeout + screenshot/PDF evidence

**Date:** 2026-07-31 · **Time:** ~16:00 IST
**Prompt:** re-check the `/planned_workorder_line/` reliability/timeout ticket; deliver screenshot + PDF.

## Outcome
NOT FIXED — bare collection and the suggested `page`/`per_page` variant both still time out;
unknown/malformed ids don't fail fast with 404. Delivered a screenshot + PDF evidence pack.

## Depth explanation
1. **Measured the fix's own hypothesis, not just the symptom.** The ticket's Suggested Fix is
   pagination (`page`/`per_page`) + query-plan/index. So I probed that exact variant, not only the
   bare call — and it hangs identically, which is the load-bearing finding: the proposed fix isn't
   effective/deployed here. I also probed the two sub-requirements (fast-404 on unknown id) that are
   easy to forget when the headline symptom dominates.
2. **A bounded control on the same table.** `by-workorder/<id>` returns in ~1 s. Without that control,
   "it's slow" is ambiguous (big table vs bad query); with it, the evidence points specifically at the
   unbounded list read — matching the ticket's diagnosis and telling the backend where to look.
3. **Timeout measured cleanly, twice over.** A 30 s `AbortController` gives a fast, reproducible
   "no response within 30 s" (the ticket's exact "client read timeout" phrasing), corroborated by the
   earlier 181 s→504 and yesterday's 35 s CI socket timeout. The browser tab died during a full
   3-minute wait, so the bounded-abort approach is also more robust than waiting for the 504.
4. **Head-of-line-blocking caveat surfaced, not hidden.** A re-run inflated the fast probes' timings
   because the hanging calls block the connection; I used the unblocked run's timings and said so on
   the evidence page, so the numbers aren't quietly wrong.
