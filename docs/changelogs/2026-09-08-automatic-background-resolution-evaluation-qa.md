# 2026-09-08 — QA: ZP-3934 — [Web] Issue evaluation only ran when someone remembered to press Suggest with AI

**Ticket:** https://egalvanic.atlassian.net/browse/ZP-3934

**Prompt:** the ticket text with its 10-step QA review ("test this too").

## What was done
1. **Confirmed the removal half from the bundle** — the Suggest button and the resolution-job dialog appear nowhere in the deployed bundle and are unreachable in the UI.
2. **Created an issue through the product and left it alone** — SCCR Violation on a panelboard via Pull-Through Work → Create Issue, then untouched for 25 minutes: no resolutions, no processing flag, all counts zero.
3. **Retriggered twice** — on the new issue and on an already-evaluated one; both answered with one issue considered and **zero jobs created**, and changed nothing.
4. **Sampled a hundred rows for a live processing state** — nothing anywhere was processing, so the Evaluating chip and the 8-second auto-refresh could never be observed running.
5. **Distinguished the trigger from the engine** — surveyed 15 issues carrying resolutions: 6 minted by rule (every one method-bound, with real labor), 12 by the agent, 1 manual. Both passes have run on this tenant, so the engine works and the trigger is what is missing.
6. Verdict, evidence, artifact page.

## Results (short)
- **UI half PASS** — button gone, controls replaced by Re-evaluate / Add manually, indicator mechanism present in the shipped code.
- **DEFECT 1 (High, deployment unverified)** — a newly originated issue is never evaluated and the retrigger creates no job. Reported with the caveat stated in the artifact: the sweeper may simply not be deployed to QA, and the product cannot tell me.
- **FINDING (Low)** — the empty state still instructs users to press "Suggest with AI", the button this change removed. The only surviving occurrence of the phrase in the bundle.
- 5 steps not run: everything downstream of a job actually running.

## Deliverables
- Verdict: `docs/bug-reports/2026-09-08-QA-automatic-background-resolution-evaluation-verdict.md`
- Artifact: https://claude.ai/code/artifact/9cbd273c-e546-468d-b176-cd2213e00fda

## Depth notes (learning)
- **The ticket told me how to read a null result** — "treat 'the agent found nothing' as suspect until a sweeper run is confirmed." Reading the ticket's own hazard note is what turned a shrug into a finding.
- **`jobs: 0` is a stronger signal than "no proposals".** No proposals could be a worker that is down; zero jobs means the ledger insert never happened either. Naming which layer is silent is what makes the report actionable.
- **Separate the trigger from the engine before calling a feature broken.** The 15-issue survey is what let the defect say "the failure is the trigger, not the rule engine" — and that survey also corrected an earlier claim of mine that no rule-minted proposal existed on QA.
