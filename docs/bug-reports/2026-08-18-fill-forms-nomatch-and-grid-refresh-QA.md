# Fill Forms from Photos: no-match empty state + assets-grid Forms-column refresh — QA verdict

**Tested:** 2026-08-18 · **Build:** QA **V1.36** · **Tenant:** `acme.qa.egalvanic.ai` · **Session:** `fcc37c67` (Infrared Thermography I.R)
**PRs:** eg-pz-frontend **#1176** (empty state / "Try other pages") · eg-pz-frontend **#1175** (Forms-column refresh)

---

## Verdict — cannot verify #1176/#1175 on QA: the Fill-from-Photos READ pipeline is failing, and it dumps raw AWS infrastructure into the dialog

Both PRs improve what happens *after* a "Read pages" run completes. On QA today, **the read never completes** — the agent-runner ECS/Fargate task fails on every run — so neither PR's behaviour can be reached. Worse, the failure surfaces as a **raw AWS ECS task descriptor rendered verbatim in the user dialog**, leaking internal infrastructure. This is the real, reproducible finding.

## 🔴 Finding 1 — the "Read pages" job fails on every run (2/2), read pipeline down on QA

I ran "Read pages" twice, with two different files (a UI screenshot, then an IR photo). **Both failed.** Each launched a fresh ECS task (different `subnetId` each time) that exited with `ExitCode: 1` — so this is systemic, not tied to one input.

Notably, the ticket's own premise is that uploading a **screenshot** should return *empty fills → the graceful empty state*. On QA a screenshot upload instead **crashes the read job**. Whichever layer is at fault (most likely the `eg-pz-agent-runner:qa` Fargate task itself), the effect is: **Fill from Photos cannot read any pages on QA**, which blocks the entire flow #1176 and #1175 sit on.

## 🟠 Finding 2 — the failure dumps a raw AWS ECS task descriptor into the user dialog

When the read job fails, the "Fill Forms from Photos" dialog renders the raw ECS/Fargate task JSON as its error — screenshot-proven, and confirmed at the API layer (`GET /api/form-fill/jobs/{id}/status` → 200, body contains the ECS descriptor). What's exposed to the (authenticated) user:

- **AWS account ID** `165183897698`
- **ECS cluster ARN** `arn:aws:ecs:us-east-2:165183897698:cluster/eg-pz-qa-ecs-ohio`
- **ECR image** `165183897698.dkr.ecr.us-east-2.amazonaws.com/eg-pz-agent-runner:qa`
- **VPC internals** — `subnetId`, `networkInterfaceId` (eni-…), `macAddress`, `privateIPv4Address` `10.1.1.21`, `privateDnsName` `…compute.internal`
- task ARN, runtime id, `ExitCode: 1`, `DesiredStatus: STOPPED`

This is an error-handling defect (the raw Step-Functions/ECS failure cause is passed to the client and rendered verbatim) plus a minor internal-infrastructure disclosure. It is **separate from #1176's scope** — #1176 redesigned the *no-match* (successful-but-empty) affordance; this is a *hard-failure* path #1176 didn't touch. Severity **Medium** (disclosure is to authenticated internal users, not cross-tenant/public, but the UX is broken and internal topology + account id should never render in a dialog).

![Run 1 — the Fill Forms from Photos dialog rendering the raw AWS ECS task JSON (account id, cluster ARN, ECR image, subnet, ENI, MAC, private IP, internal DNS) after the read job failed](../bug-evidence/fill-forms-nomatch/read-job-failure-raw-ecs-dump.png)

![Run 2 — a different file, the same failure: a new ECS task (different subnet) and the same raw descriptor, confirming it is systemic on QA](../bug-evidence/fill-forms-nomatch/read-job-failure-2nd-run-different-file.png)

## What each QA-review item's status is

| # | QA item | Status |
|---|---|---|
| 1 | No-match → explicit empty state naming the asset, "values only read from pages that clearly belong…", describes a usable page | ⚠️ **Blocked** — read crashes before returning empty; strings **are deployed** (bundle: "Try other pages", "No page covered", "clearly belong", "only read") but the state can't be reached |
| 2 | "Try other pages" replaces disabled Apply; returns to an empty upload step; second run completes | ⚠️ **Blocked** — never reach a completed no-match run |
| 3 | Positive control — real photos → Apply enabled and applies | ⚠️ **Blocked** — read never succeeds, so nothing to apply |
| 4 | After Apply, Forms column updates with no reload | ⚠️ **Blocked** (#1175 needs a successful apply) |
| 5 | Same column updates via the grid's refresh button | ⚠️ **Blocked** |
| 6 | Same column updates after closing the dialog | ⚠️ **Blocked** |
| 7 | Issue/asset counts + sections refresh alongside statuses | ⚠️ **Blocked** |

**#1176 is deployed** (its empty-state strings are in the shipped bundle). I just can't drive it to render, because the upstream read fails.

## Recommendation

1. **Fix the read pipeline on QA first** — the `eg-pz-agent-runner` Fargate task is exiting 1 on every read (backend/devops). Until it runs, none of #1176/#1175 is testable here, and the feature is effectively down on QA.
2. **File Finding 2 regardless** — the read/apply job's failure path must return a readable message ("We couldn't read those pages — try again"), never the raw ECS/SFN cause. The job-status endpoint should not pass the ECS descriptor to the client, and the dialog should not render an unknown error object verbatim. This is independent of whether the ECS task itself is fixed.
3. Re-run this whole QA once the read pipeline is healthy — then the no-match empty state (screenshot upload) and the three grid-refresh paths can actually be exercised.

## Method notes
- Real UI drive: IR session → Forms tab → Actions → **Fill from Photos** → upload → **Read pages** (×2, different files).
- Failure confirmed two ways: the dialog render (screenshots) and `GET /api/form-fill/jobs/{id}/status` returning the ECS JSON. Two consecutive runs, two distinct ECS tasks, both `ExitCode 1`.
- No data created; the runs produced no fills.
