# Live RBAC role sweep — all 7 role accounts

**Date:** 2026-08-25 · **Prompt:** "i have run fully automation for web check all thing are working
properly" → "ci cd" → "still your bugs are very low priority no major issue i see" → "most important
thing you are missing that you could check with different role like project manager etc"

## What changed
- `docs/bug-reports/2026-08-25-rbac-technician-web-access-and-overprivilege.md` — new report
- `docs/bug-reports/evidence/2026-08-25-rbac-technician/` — live screenshot
- Artifact: https://claude.ai/code/artifact/8ca35956-54c9-4c95-92f9-108c3705a2cd (consolidated register — the per-report artifact was superseded)

## Finding
**RBAC-1 (High)** — the Technician role holds `platform.web` and 93 permissions, more than the
Project Manager (90), including `quotes.approve`, `accounts.manage` and `data.export`. The suite's
own `technicianCannotAccessWeb` asserts the opposite. CI dates the regression to between
2026-06-18 (passing) and 2026-07-09 (failing).

## Method
The owner's steer — "check with different role like project manager" — was the unlock. Route-by-route
probing of a single role produces ambiguous results; the signal only appears when roles are compared.

1. **Log in as each role for real**, don't infer from the permission CSV. Capture `/api/auth/v2/me`
   (live permission set), the nav each role renders, and direct-URL probes of routes the nav omits.
2. **Diff the permission sets against each other.** "Technician has 93 permissions" means nothing
   alone; "Technician has more than the Project Manager, and is a superset of the Facility Manager"
   is the finding. Set arithmetic did the work no single-role sweep could.
3. **Use one role as the control for another.** Technician can open `/eg-forms`, PM cannot →
   diff their sets → `forms.manage` is the differentiator → the PM's Access Denied is *correct*.
   The same technique killed the "PM missing Planned Work" candidate (`features.planning.view`).
4. **Date the regression from CI history** rather than asserting "recent" — grep the assertion text
   across past runs of the RBAC workflow to find the pass→fail boundary.

## What was dropped, and why that matters
Five candidates were investigated and four discarded: PM gate contradictions (gates key on `.manage`,
not `.view`), PM nav omissions (keyed on a flag PM lacks), Client Portal exposure (10/13 denied, rest
empty), an audit-log "hang" (read mid-render), and an `alliance-config` 500 (12/12 clean on retest).

Earlier in this session two other reports — detail-route error handling and grid sort order — were
ruled low priority by the owner. The lesson carried forward: severity is the owner's call, and a
finding that is *technically real* is not automatically *worth filing*. Compare across roles and
environments to find things that are structurally wrong, not just locally odd.

## Coverage
6 of 7 roles swept live (Electrical Engineer has no QA account — login 401, documented in
`RbacFixtures`). 14 routes probed per role. No data created, modified or deleted.

## CI status at time of writing
`Parallel Full Suite — Core Regression (848 TCs)` (32860997278) and `Parallel Suite 2`
(32861054054) were dispatched and were still **in progress** — historically ~3.5h. Results not yet
available; this sweep was done live against QA independently of them.
`Parallel Suite 3 — API Health Check` has failed every scheduled run since 2026-08-17; its report
shows 49/50 endpoints passing, with the failure coming from the `malformed-path` error-contract probe
(12 × 5xx on junk path params) — the same class the owner ruled low priority today.

---

## Addendum — the dispatched CI runs finished (2026-08-26)

Both runs show **cancelled**, but the badge is misleading.

**`Parallel Full Suite — Core Regression` (32860997278) — 6h40m57s**
20 of 21 jobs green at the GitHub level; only `rerun-failed` was cancelled, after **4h00m51s**.
The shard jobs report green because the workflow tolerates test failures and defers to the rerun
step — so "job success" is not "tests passed". Actual test results: **83 failures**.

| Shard | Run | Failed | triaged Bugs | Test/Data | Env |
|---|---|---|---|---|---|
| Asset Eng / exhaustive options | 308 | 35 | 35 | 0 | 0 |
| Asset Eng / per-class matrix | 39 | 10 | 10 | 0 | 0 |
| Asset Eng / Trip Config | 12 | 7 | 7 | 0 | 0 |
| Asset Eng / Mains Config | 17 | 5 | 2 | 3 | 0 |
| Asset Eng / Transformer | 10 | 4 | 4 | 0 | 0 |
| Work Order + Issue | 249 | 17 | 0 | 16 | 1 |
| Auth + Site | 59 | 3 | — | — | — |
| Asset Part 5 | 76 | 1 | 0 | 1 | 0 |
| Opportunities [SALES] | 46 | 1 | 0 | 1 | 0 |

All 58 "Bugs" are in Asset Engineering, and they share one signature — empty option lists:
`Phase Configuration should list the standard configs (got: )`, `Mains Type should offer 'MCB'`,
`Subtype dropdown ... (found 0)`, `Generic should be selectable as manufacturer ... found []`.

### CI-1 — `rerun-failed` runs for 4h and is killed
It consumed 4h00m51s and was cancelled, which is what turns a run with a green summary job into a
`cancelled` badge. It also pushed total wall-clock to 6h41m against 3h23m on 2026-08-22 — roughly
double for the same suite. `Parallel Suite 2` cancels on the same pattern (4h09m here; 4h26m on
08-22; 4h19m on 08-21). **This is the actionable CI defect from the run** — a rerun step that cannot
finish masks the status of every suite behind it.

### The 58 Asset Engineering failures — NOT called a product bug
Live check against `/api/node_classes` (636 classes, 13 tenant copies of each):

```
Motor Starter    13 entries, definition[] length 0 in ALL 13   <- incl. the global (company_id null) copy
VFD              13 entries, definition[] length 0 in ALL 13
Circuit Breaker  13 entries, 9 defs (15 for ACME)
Transformer      13 entries, 17 defs (26 for ACME)
Panelboard       13 entries, 10 defs (16 for ACME)

295 of 636 classes (46%) have zero definitions — Cable, Load, DC Bus, Battery,
Capacitor Bank, Rectifier, Shunt Reactor, Motor Controller …
```

Motor Starter and VFD have no engineering attributes **in every tenant including the global copy**,
and 46% of classes are attribute-free in a way that looks deliberate (Cable and Load do not need a
phase configuration). That points at **test expectations asserting attributes the schema never
defined for those classes** — not data loss in the ACME tenant.

I could not establish a pass→fail window for these (prior runs don't carry comparable shard output,
and full-log retrieval on these runs OOMs), so the regression evidence that carried RBAC-1 is absent
here. Verdict: **test-suite correctness problem, not a filed product defect.** It still matters —
58 spurious failures bury real signal and are what keeps `rerun-failed` running for four hours.

One loose end worth a separate look: `Subtype dropdown should list options for Circuit Breaker
(found 0)`. Circuit Breaker *does* carry 9–15 definitions, so that one is not explained by the
empty-schema theory and was not verified live.
