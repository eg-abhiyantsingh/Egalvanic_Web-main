# Live RBAC role sweep — all 7 role accounts

**Date:** 2026-08-25 · **Prompt:** "i have run fully automation for web check all thing are working
properly" → "ci cd" → "still your bugs are very low priority no major issue i see" → "most important
thing you are missing that you could check with different role like project manager etc"

## What changed
- `docs/bug-reports/2026-08-25-rbac-technician-web-access-and-overprivilege.md` — new report
- `docs/bug-reports/evidence/2026-08-25-rbac-technician/` — live screenshot
- Artifact: https://claude.ai/code/artifact/13c11256-99fd-4bb2-9ebf-79835691e080

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
