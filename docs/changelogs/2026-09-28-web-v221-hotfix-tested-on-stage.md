# Web v2.2.1 hotfix — the four "In QA" tickets tested on staging

**Date:** 2026-09-28 · **Environment:** acme.stage.egalvanic.ai, build `index-COmQBZM2.js` (deployed 16:01 IST)
**Release:** Jira version 14237 (Web v2.2.1). ZP-4422 was already Done and was not re-tested.

| Ticket | Result on staging | Jira | Artifact |
|---|---|---|---|
| ZP-4445 Suggestion Polling Off | PASS — no 5-minute polling on the three dashboards; loads once; Needs Attention reloads on return, donut reused | READY TO RELEASE | https://claude.ai/artifact/83gY5X84LrSW7PwsfSgUtz |
| ZP-4415 Report history 500 | PASS — history 200 on all 10 sites, 400 without a site; portal page + real download blocked (no Portal Sales seat, no report) | READY TO RELEASE | https://claude.ai/artifact/BpUp6ZqM1ktgbU83WbN3yr |
| ZP-4426 Condition Assessment 500 | PASS (web) — no `sld_id=all` request, no 500; backend 404 companion not deployed (still 500 on the raw API) | READY TO RELEASE | https://claude.ai/artifact/TrCykKEgDRNtK5kppSmRJV |
| ZP-4444 Work Order Loading Time | PASS for plan §1, §3, §5 — grid 0.5–2.4 s, removed calls gone, dialogs load on demand; §6 non-admin not testable | left In QA (owner to decide) | https://claude.ai/artifact/MgiUf9HoQhvUVu84nYJZSc |

## What changed in the repo
- `ZP4426ConditionAssessmentAllSitesTest` (new): picks **All Facilities** on Tasks the way a user does, opens Condition
  Assessment from the menu (no reload), records every request with its status, asserts no `sld_id=all`.
- `ZP4444WorkOrderLoadTest` (new): three opens per work order; the grid time now comes from a MutationObserver
  (the old Java polling read up to ~1.4 s late); plan §3.3 check; §5 Bulk Edit + Generate Report on-demand check.
- `ZP4444RoleProbeTest` (new): what a work order shows for the signed-in seat, every call with its status.
- `ZP4445DashboardPollingTest`: step 6 rewritten — it now proves the page really left and came back before counting.
- `ZP4415ReportHistoryTest`: `historyApiFromSignedInApp`; async-script bug in the generate test fixed.
- `BaseTest.tileWindow(...)`: un-maximizes through CDP first (macOS refuses setSize on a maximized window).
- Evidence: `docs/bug-evidence/2026-09-28-v221-hotfix-stage/`.

## Mistakes caught along the way (all fixed before any verdict)
1. ZP-4445 step 6 first "passed/failed" on a return that never happened (clicked Z University, never checked the URL).
2. ZP-4444 §3.3 first looked like a failure; it was the test's own polling lag. Precise timing shows it passes.
3. ZP-4426 "All Sites" was typed into the wrong picker; the option is "All Facilities" and only exists on list pages.
4. Non-admin seats (AM, CP, EE) get Access Denied on work orders: role setup (`features.site_visits.view`), same on QA.
