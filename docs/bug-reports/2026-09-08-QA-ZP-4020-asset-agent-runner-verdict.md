# ZP-4020 — Asset-agent breaker designation: agent-runner/Fargate mode, accuracy gates, speed levers

**QA verdict — NOT TESTABLE ON QA, by the ticket's own design. This change is gated entirely behind `ASSET_AGENT_RUNTIME=runner`, an environment variable the ticket says is unset everywhere but dev, and it states plainly that there is **no behaviour change until the env var flips**. Nothing in the product exposes the variable, and the agent-runner itself is an external Fargate task (`ecs:runTask.sync`) outside the web application. Every one of the six review steps needs either that flag flipped on dev or visibility into the runner's logs. What I can report is the classic path's own behaviour on QA, which is the revert target: the bulk extraction dialog and its per-asset flow work as they did when this repo last exercised them, and the accuracy gates the ticket describes were not observable in any QA output.**

**Tested:** 2026-09-08 · `acme.qa.egalvanic.ai` build **V1.36**, bundle **`index-CSsDpG3c.js`** · tenant acme · Super Admin seat.
**Environment note from the ticket:** "dev only (cicd/dev); classic path is the default (`ASSET_AGENT_RUNTIME` unset) so there is NO behavior change until the env var flips." QA is therefore expected to be on the classic Lambda, and that is consistent with what the product shows.

**Artifact:** https://claude.ai/code/artifact/b62c9cfe-7c23-4c7c-baf7-2b7f0d3429ce

---

## QA-review checklist — results

| # | Ticket step | Result |
|---|---|---|
| 1 | With `ASSET_AGENT_RUNTIME=runner` on dev, run a bulk OCPD extraction and confirm configure-mode OCPD items route to the Fargate runner while everything else runs classic; both harnesses answer identically | ⚫ **NOT TESTABLE FROM QA** — the flag is an environment variable on the backend, not a product setting, and QA is expected to have it unset. Routing between two harnesses cannot be observed from the browser; it would need the dev deployment plus its logs. |
| 2 | Feed a case where the nameplate interrupting kA disagrees with the library device (e.g. a 10 kA plate onto a 65 kA-only family): confirm the binding is stripped and the row stays undesignated with a spelled-out contradiction, not a false PASS | ⚫ **NOT TESTABLE FROM QA** — the gate lives in the agent-runner's recovery path. Producing the case needs a crafted nameplate photo fed through a runner-mode extraction; on the classic path there is no equivalent output to inspect. |
| 3 | Confirm no-match rows carry no `skm_oid`, keep the real manufacturer, and print an explanation (no "Custom" entries anywhere) | ⚫ **NOT TESTABLE FROM QA** — no runner-mode extraction could be run to produce a no-match row. Adjacent observation with no bearing on the gate: the equipment catalog on QA resolves real manufacturers for every type sampled, and nothing labelled "Custom" appeared in the catalog searches performed for the linkage tickets. |
| 4 | Confirm a compound-segment device requires the Setting2/I2t suffix and that the agent reasoning/assumptions/confidence block lands on the designation | ⚫ **NOT TESTABLE FROM QA** — the persisted block is written to `eqp_lib.agent` by the runner. No designation produced on QA carries it, which is expected with the flag unset. |
| 5 | Confirm the bulk dialog shows live per-tool narration during a runner run (not a frozen "Reading N photos") | ⚫ **NOT TESTABLE FROM QA** — the narration is mirrored from the runner's stream into the bulk results row, so it only appears in runner mode. The classic dialog's behaviour is the "before" this step contrasts against. |
| 6 | Revert check: flipping `ASSET_AGENT_RUNTIME` back returns everything to the classic Lambda with no deploy | ⚫ **NOT TESTABLE FROM QA** — requires flipping the variable on dev twice and watching both states. The half that *is* consistent with the claim: QA today behaves as the classic path with the variable unset, which is the state a revert is meant to return to. |

---

## Why this is reported as untestable rather than failed

Three independent reasons, each sufficient on its own:

1. **The gate is an environment variable.** `ASSET_AGENT_RUNTIME` is read by the backend at runtime. There is no product surface that reads or reports it, so from the browser I cannot tell which harness would handle a job, and I cannot flip it.
2. **The runner is not the web application.** The agent-runner is a Claude Agent SDK process launched as a Fargate task via `ecs:runTask.sync`. Its accuracy gates, its `_post_audit`, its truncation recovery and its narration stream all live there. This repo has established before that engineering-AI-pipeline work of this shape is backend/infra and reachable only through the artefacts it leaves in the product.
3. **The ticket scopes itself to dev.** It says there is no behaviour change on any environment where the variable is unset. Testing it on QA would either find nothing (as it did) or find something the ticket says should not be there.

What would make it testable: the flag flipped on dev, one bulk OCPD extraction run there, and access to the runner's log stream for the per-tool narration and the gate decisions. Steps 2, 3 and 4 additionally need crafted fixtures — a nameplate whose interrupting rating contradicts the library family, a device with no catalog match, and a compound-segment trip unit.

---

## Test data — direct links (QA)
- The classic bulk-extraction surface this change routes around: https://acme.qa.egalvanic.ai/assets (Bulk Ops → AI Extraction), mapped in this repo's earlier bulk-extraction run
- Equipment catalog used for the adjacent no-"Custom" observation: `https://acme.qa.egalvanic.ai/api/equipment-catalog/quick-search?type=circuit_breaker&q=`

## Not covered / honest gaps
- **All six review steps** — every one needs the dev deployment with the flag flipped, and most need the runner's logs.
- **The twelve pipeline PRs' internals** (interrupting-kA gate, honest no-match shape, DIP-switch reading, max_tokens recovery, the persisted reasoning block, full-resolution photos, doctrine trim, compound segments, speed levers, CSEGMISQUARETDELAY, Fargate execution, SFN retry throttles) — all external to the web product.
- **A classic-path positive control** — a fresh bulk OCPD extraction was not run on QA in this session; the classic behaviour is cited from this repo's earlier bulk-extraction verdict rather than re-observed.
