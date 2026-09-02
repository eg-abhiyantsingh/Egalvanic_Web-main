# asset-agent Lambda was down on dev — Dockerfile never copied nameplate_spec.py

**QA verdict — eg-pz-engineering-ai-pipeline #70: QA NOT AFFECTED (smoke-invoked)**
**Tested:** 2026-09-02 · acme.qa.egalvanic.ai (QA only) · fix is dev-only.
**Artifact:** https://claude.ai/code/artifact/1264dfc8-4f83-461e-a9a5-f61da942fc68

## Why this was worth a QA check at all
The nameplate code IS on QA (proved in the two prior tickets: bundle markers + apply route). If
nameplate_spec.py had reached QA's asset-agent image WITHOUT the COPY line, QA's Lambda would be down too —
the import fails at module scope, so AI Library Designation (configure) would be dead as well, silently.

## Result: QA is healthy
Smoke-invoked the collateral-damage flavor on QA: Equipment Designations → Bulk Ops → select → AI Extraction
(bulk fan-out = the path that reaches the Lambda), configure flavor, 25 assets.
- **22 done, 0 failed, ZERO ImportModuleError / "No module named" anywhere.**
- Real agent output per asset ("Reading trip settings…", "No SKM match · 4000A frame"), confidence chips.
- Backend extraction routes normal: asset-agent SSE → {"type":"error","message":"Asset not found."} for a bogus
  id; both apply routes 400 "node_id is required".
- **Zero residue:** proposals are STAGED behind "Apply all"; closed without applying; Library Designated
  unchanged at 0 of 328 (0%).

## METHOD NOTE (flagged, not hidden)
"AI Extraction" fired immediately on click with **no confirmation step** — I expected a config dialog. Harmless
here (staged proposals, closed without applying, nothing written), but worth noting as a UX observation: a bulk
AI run with real model cost has no confirm gate. I should have narrowed to one asset first.

## Not testable from QA — handed back to the pipeline repo
- Invoke the deployed **dev** Lambda for a normal response (needs dev AWS/Lambda).
- CloudWatch: no ImportModuleError after the deploy timestamp.
- **The guard test** (throwaway branch, remove COPY, confirm CI fails at check_vendored.py) — the most valuable
  check in the ticket since it tests the CLASS of bug; belongs in the pipeline repo's CI.
- Nameplate flavor bulk run on dev (dev + photo fixture).

## Carried-forward gap worth pressing
The ticket admits deploy-time smoke invoke is still not wired, and the runner already supports a __smoke__ mode.
The static guard catches a missing COPY, but only an actual invoke catches "builds and pushes but cannot start" —
which is precisely how this shipped green. Wiring __smoke__ into deploy would have caught it in minutes.
