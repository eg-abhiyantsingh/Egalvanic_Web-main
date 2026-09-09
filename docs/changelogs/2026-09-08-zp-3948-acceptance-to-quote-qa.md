# 2026-09-08 — QA: ZP-3948 [Web] Accepting a resolution did not get it onto a quote, minting materials before anyone priced them, with no way to unaccept

**Prompt:** the ticket text with its 10-step QA review ("test all this ticket too").

## What was done
1. **Drove the whole gesture** — accepted a resolution and watched Add to Quote open by itself; the dialog offers a new quote or any existing quote on the same site, requires a title and an opportunity, and blocks on unpriced parts.
2. **Checked the Set-prices step names exactly the unpriced parts** — one row, the Class J fuse × 3; the $45 entered produced a $135 quote line and now reads $45.00 on the library entry. Both halves land.
3. **Counted the materials library before and after every acceptance** — held at 17 throughout, proving minting really has moved off accept to quote time.
4. **Accepted then unaccepted a rule-minted resolution** — back to `proposed` with the timestamp cleared, buttons back to Accept · Add manually, and the earlier quote's minted price untouched. That is the negative case that matters.
5. **Watched the funnel move with it** — Resolution column reads Quoted, QUOTED 27 → 28 while NO RESOLUTION SET fell 965 → 964.
6. **Read the schedule-row contract instead of guessing** — the flag round-trips and the schedule-bearing gate is correct per class, but zero methods across 39 procedures carry it, so no `schedule_update` task can be minted.
7. Verdict, evidence, artifact page.

## Results (short)
- **Core flow PASS** — accept → dialog → price → quote, and Unaccept including its negative.
- **FINDING 1 (Medium)** — the schedule-row half is inert here: the old automatic path was deleted and the new manual one cannot fire because no method carries the flag.
- **FINDING 2 (Low)** — no resolution anywhere reaches `applied`.
- 4 steps not run: the existing-quote append, the whole schedule chain, the basic-fix forms regression, a true cross-site bulk attempt.

## Deliverables
- Verdict: `docs/bug-reports/2026-09-08-QA-ZP-3948-acceptance-to-quote-flow-verdict.md`
- Evidence: `docs/bug-evidence/zp-3948-acceptance-to-quote/`
- Artifact: https://claude.ai/code/artifact/33236b33-a8c8-4cf9-923c-bde7e8e17e73

## Depth notes (learning)
- **Count the side effect, not just the happy path.** The claim was "minting no longer happens at evaluation". A before/after count of the library across accept *and* unaccept is what proved it, and it also revealed the write had moved to quote time.
- **Unaccept is only interesting with its negative.** Returning to `proposed` is easy; not deleting what a previous quote already minted is the part that could regress silently.
- **Read the tradeoff note.** The ticket says deleting the auto-execution path leaves the manual task as the only route. That sentence is what turned "no method carries the flag" from a data gap into a Medium finding.
