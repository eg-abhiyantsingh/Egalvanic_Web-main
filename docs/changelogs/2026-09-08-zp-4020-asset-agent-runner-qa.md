# 2026-09-08 — QA: ZP-4020 — Asset-agent breaker designation: agent-runner/Fargate mode, accuracy gates, speed levers

**Prompt:** the ticket text with its 6-step QA review ("test all this ticket too").

## What was done
1. **Established that the gate is not reachable from the product** — the change is behind a runtime environment variable the backend reads; no product surface exposes or reports it, so from the browser it is impossible to tell which harness would take a job, let alone flip it.
2. **Placed the runner outside the web application** — the accuracy gates, the truncation recovery, the persisted reasoning block and the per-tool narration all live in a Fargate task, reachable only through the artefacts it leaves in the product.
3. **Checked QA for artefacts of a runner run and found none** — no designation carries the reasoning block, which is exactly what the ticket predicts with the variable unset.
4. **Recorded the classic-path surface as the revert target** — the bulk AI Extraction flow on the assets grid, captured on QA, is what the change routes around and what a revert returns to.
5. **Reported all six steps as not testable, with the reason per step** rather than marking them failed — the ticket states there is no behaviour change until the variable flips, so testing here would either find nothing or find something that should not be present.
6. **Wrote down exactly what would make it testable** — the flag flipped on dev, one bulk extraction run there, the runner's log stream, plus three crafted fixtures for the gate steps.
7. Verdict, artifact page.

## Results (short)
- **NOT TESTABLE ON QA, by the ticket's own design.** Three independent reasons, each sufficient: an environment-variable gate the product cannot read, a runner outside the web app, and a dev-only scope that promises no behaviour change here.
- No defects, no findings — an untestable ticket honestly reported is not a pass and not a failure.
- Consistent with the revert claim: QA behaves as the classic path today, which is the state a revert returns to.

## Deliverables
- Verdict: `docs/bug-reports/2026-09-08-QA-ZP-4020-asset-agent-runner-verdict.md`
- Artifact: https://claude.ai/code/artifact/b62c9cfe-7c23-4c7c-baf7-2b7f0d3429ce

## Depth notes (learning)
- **"Not testable" must be argued, not asserted.** Three independent reasons plus a per-step explanation is what separates a real scope boundary from not having tried.
- **Never report an env-gated backend change as PASS because nothing happened.** Nothing happening is the documented expectation; calling it a pass would imply the feature was exercised.
- **End with the unblock list.** Naming the flag, the one run and the three fixtures turns a dead end into a handoff — the same pattern this repo already uses for the engineering-AI-pipeline tickets.
