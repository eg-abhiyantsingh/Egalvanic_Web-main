# Suite 2 deep read — Create WO is now a 4-step wizard

**Date:** 2026-08-26 · **Prompt:** "did you really check ci cd? i have run full automation 3 full suite is thier"

The owner was right: I had read Suite 1 (Core Regression) and Suite 3 (API Health) in depth but only
Suite 2's badge. Reading it properly changed the picture — Suite 2 carries **~423 test failures**
(vs 83 in Suite 1), and the largest block has a single root cause.

## FW-1 — Create Work Order dialog redesigned into a 4-step wizard (framework stale)
- Work Type shards failed wholesale: Create Dialog Matrix **151/153**, Create E2E **42/42**,
  Auto-Schedule **33/33**, Detail Contract 26/102 — every failure the same timed-out
  `WorkOrderPage` wait (page objects still target the v1.35 single-form dialog).
- Live verification: dialog is now **Work order → Scope → Team → Review**, with a
  "Service to Perform" selector and live scope resolution
  ("WILL RESOLVE TO 2 assets — every ATS at Android Site 2"; step 2 lists matching assets with
  per-asset issue unticks). Wizard **works** — cancelled before create, no data written.
- Verdict: framework debt, not a product outage. Same class as the v1.35 ZP-3000 break.
  ~40% of Suite 2 is noise until `WorkOrderPage` is realigned; biggest feeder of CI-1's rerun pile-up.

## RBAC corroboration + RBAC-3 (new)
Suite 2's `RBAC — API` (606 TCs, 33 fail) independently flags RBAC-1:
`PRIVILEGE ESCALATION: matrix does NOT grant 'platform.web' to 'Technician' but /auth/me returns it`
— and shows the drift is wider: Account Manager (`ir_photos.upload`, `features.issues.view`,
`features.arc_flash.view`) and Facility Manager (`features.panel_schedules.view`,
`features.opsdb.view`) also hold grants missing from the recorded 555-grant matrix.
Filed as **RBAC-3 (Medium)**: the matrix can no longer be trusted as source of truth; re-baseline
after RBAC-1 is decided.

## Other Suite 2 blocks, explained
- Quality Gates (All Pages): 52 fail, all auto-triaged Test/Data.
- Asset Engineering ~60: same empty-schema expectations as Suite 1 (CI-4).
- Documentation-Inspired 7: all the known SPA catch-all family (200 + text/html on unknown
  /api/* paths) — same family as the deferred UI-3/UI-5.
- Arc Flash: 6 failures, 3 triaged Bugs — **unreviewed, still open**.
- Suite 2's rerun-failed was killed at 2h01m40s (CI-1 pattern, both suites).

## Register updated (same URL)
https://claude.ai/code/artifact/f343d5a5-84d0-41c5-9e63-d9a130492b7e
Totals now: 4 High, 9 Medium, 11 Low/deferred, 6 dropped. Combined across suites 1+2:
55 green jobs over ~506 actual test failures.

## Lesson
"Checked CI/CD" must mean reading every suite's *test* results, not workflow badges — the badge
lies twice here (green jobs hide failures; cancelled hides a mostly-green run).
