# [Web] Issue evaluation only ran when someone remembered to press Suggest with AI

**QA verdict — the UI half is done: the "Suggest with AI" button and the resolution-job dialog are gone, the workbench and the funnel are driven by a `resolution_processing` flag that renders an "Evaluating…" chip with a spinner, and the list auto-refreshes every 8 seconds while anything is processing. The bulk action is a dialogless "Re-evaluate (N)". But the behaviour the ticket exists to deliver did not happen on QA: an issue originated through the product and left alone received **no resolutions at all** — not the instant rule proposal, not the AI pass — and it was never even flagged as processing. The manual retrigger reports **`jobs: 0`**. Across a 100-issue sample, `resolution_processing` is false everywhere, so no job is in flight anywhere. The ticket itself says to treat exactly this as suspect rather than as "the agent found nothing".**

**Tested:** 2026-09-08 · `acme.qa.egalvanic.ai` build **V1.36**, bundle **`index-CSsDpG3c.js`** · tenant acme · Super Admin seat · live UI, the list and resolution endpoints, and the deployed bundle read directly.
**Ticket said "dev only, not yet promoted to cicd/qa".** The frontend half (#1290) is unquestionably on QA. Whether the backend sweeper (#1118/#1119) is deployed here cannot be read from the product — and the observed symptom is the same either way, which is why it is reported as unverified rather than as a confirmed regression.

---

## QA-review checklist — results

| # | Ticket step | Result |
|---|---|---|
| 1 | Confirm "Suggest with AI" no longer exists anywhere in the workbench or on Pull-Through Work, and that no resolution job dialog can be opened | ✅ **PASS, with a stale sentence** — `ResolutionJobDialog` appears **0 times** in the bundle, and no such dialog is reachable; the workbench offers only **Re-evaluate** and **Add manually**. The string "Suggest with AI" survives in exactly one place: the empty-state copy "*No resolutions yet. Suggest with AI to generate ranked options grounded in this asset's recorded devices.*" — see FINDING 2. |
| 2 | Originate an issue and do not touch it: it must be evaluated on its own. Rule-determined fixes should appear immediately (inline deterministic pass), with the AI proposals arriving later | ❌ **FAIL on QA** (note: the deterministic pass demonstrably works on this tenant — six rule-minted, method-bound proposals exist on older issues — so what failed is the automatic trigger, not the rule engine) — an SCCR Violation issue was created through the product on asset 11N-H1-1 at 14:28 and deliberately left alone. Checked again ~25 minutes later: `GET /issue/{id}/resolutions` → **`resolutions: []`**, `processing: false`, and the list row reads `resolution_counts {proposed: 0, accepted: 0, applied: 0}`, `resolution_state: null`. **No inline rule proposal, no AI proposal, and no processing flag ever set.** |
| 3 | While the AI pass is running, confirm the workbench shows "Evaluating…" and polls to completion without a manual refresh, and Pull-Through Work shows the Evaluating chip and auto-refreshes | ⚠️ **Mechanism confirmed in code; never observed live** — the grid renders, for `row.resolution_processing`, a Chip with a spinner icon and the label `issues.evaluating` → **"Evaluating…"**, and the page installs `setInterval(refetch, 8000)` whenever any row is processing. No row was ever in that state to watch (see below), so the live behaviour was not seen. |
| 4 | Confirm the indicator clears once results land, and that it also clears itself after the 20-minute staleness window rather than spinning forever | ⚠️ **NOT EXERCISED** — nothing was ever processing, so neither clearing path could be observed. |
| 5 | **This is the regression #1119 fixes — verify explicitly on dev**: after an issue is originated, the sweeper must actually run and persist proposals. Zero proposals is the exact symptom of the guard bug | ❌ **The suspect symptom is what QA shows** — the newly originated issue has zero proposals; `POST /issue-resolution/reevaluate {"issue_ids":[…]}` answered **200 `{"issues": 1, "jobs": 0}`** — one issue considered, **zero jobs created** — and produced no new proposals; and `resolution_processing` is **false on all 100 rows** sampled. Older issues carry both **agent** proposals (12 in a 15-issue survey) and **rule** proposals (6, each bound to an implementation method), so both passes have run on this tenant at some point — but nothing is being claimed or run now. Per the ticket's own instruction this must not be read as "the agent found nothing". |
| 6 | Call `POST /issue-resolution/reevaluate` on an already-evaluated issue and confirm fresh proposals supersede the prior ones rather than accumulating | ⚠️ **NO CHANGE OBSERVED** — on an SCCR issue with two existing agent proposals the call returned `{issues: 1, jobs: 0}` and, after a 12-second wait, the same two proposals were present, unchanged and still `proposed`. Nothing accumulated, but nothing was superseded either, because no job ran. |
| 7 | Check that results applied by the sweeper match what the HTTP apply path produces for the same issue | ⚠️ **NOT EXERCISED** — no sweeper result was produced to compare. |
| 8 | Concurrency: originate several issues at once and confirm each job is claimed exactly once | ⚠️ **NOT EXERCISED** — with zero jobs being created, there was nothing to contend for. |
| 9 | Timeout: confirm a job that never completes is released after 30 minutes | ⚠️ **NOT EXERCISED** — no job existed to time out. |
| 10 | Verify automatic evaluation fires from every origination path that changed — bulk issue upload, the SCCR check, and form verdicts — not only the issue controller | ❌ **NOT OBSERVED from the one path driven** — the issue-controller path (Create Issue) minted nothing. Bulk upload, the SCCR check and form verdicts were not driven. |

---

## Defects

### DEFECT 1 (High, unverified deployment) — a newly originated issue is never evaluated, and the retrigger creates no job
**Steps.** Pull-Through Work → Create Issue → class SCCR Violation, asset 11N-H1-1, title "QA-DEMO SCCR issue for needs-pick (delete me)" → Create Issue. Do not open it again. Wait.
**Actual.** After ~25 minutes: `resolutions: []`, `processing: false`, `resolution_counts {proposed:0, accepted:0, applied:0}`, `resolution_state: null`. The issue never showed an Evaluating chip. `POST /issue-resolution/reevaluate` on it and on a second, already-evaluated issue both returned **`{"issues": 1, "jobs": 0}`** and changed nothing.
**Expected.** The deterministic pass proposes inline and immediately for a rule-determined fix; the AI pass is queued as a job and lands later; while it runs the row shows Evaluating.
**Caveat that keeps this honest.** The ticket says this work is dev-only, so the backend sweeper may simply not be deployed to QA — the product cannot tell me. Two things make it worth raising anyway: `jobs: 0` means the *ledger insert* is not happening either, not just the worker; and the ticket explicitly instructs that zero proposals must be treated as suspect rather than benign. A developer with deploy visibility can settle it in a minute.

---

## Findings

### FINDING 2 (Low) — the empty state still tells users to press a button that no longer exists
With no resolutions on an issue, the workbench prints "*No resolutions yet. **Suggest with AI** to generate ranked options grounded in this asset's recorded devices.*" The button it names was removed by this very change, so the one place a user lands when evaluation has not happened is also the one place that tells them to do something impossible. It is the only surviving occurrence of the phrase in the bundle.

---

## Test data — direct links (QA)
- The issue created and left alone: https://acme.qa.egalvanic.ai/api/issue/29ec3afb-4e8d-4f67-aeb5-1097b4ec8c49/resolutions (created 14:28, still empty)
- An already-evaluated issue used for the retrigger: https://acme.qa.egalvanic.ai/api/issue/a285e5fd-1b2b-4049-92e0-7a13dde06457/resolutions (two `generated_by: "agent"` proposals, unchanged after re-evaluate)
- Pull-Through Work (Evaluating chip and the 8-second auto-refresh live here): https://acme.qa.egalvanic.ai/pull-through-work
- Issues register: https://acme.qa.egalvanic.ai/issues
- The retrigger: `POST https://acme.qa.egalvanic.ai/api/issue-resolution/reevaluate {"issue_ids":[…]}`

Evidence: `docs/bug-evidence/zp-issue-resolution-pricing-pull-through/api-captures.md` (shared with the acceptance-to-quote run — same fixtures, same session).

## Not covered / honest gaps
- **Everything downstream of a job actually running** — the Evaluating chip live, its clearing, the 20-minute staleness release, the 30-minute timeout, sweeper-vs-HTTP parity, and concurrent claiming. All blocked by no job ever being created.
- **Origination paths other than Create Issue** — bulk upload, the SCCR check and form verdicts were not driven.
- **Whether the sweeper is deployed to QA at all** — not observable from the product; this is the single fact that decides whether DEFECT 1 is a regression or an expected dev-only gap.
- **Roles other than Super Admin** — mandatory MFA on QA blocks the other seats.
