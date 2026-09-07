# 2026-09-07 — QA: Maintenance Program / Compliance / site-report backends (#1175, #1177, #1340) + full role matrix

**Prompt:** test the maintenance-program/compliance/site-reports ticket; mid-flow: "cp cant see work order test for other roles", "work faster".

## Results
- 6/7 QA-review items PASS on QA (ticket's "dev only" note wrong — all endpoints live: asset-maintenance/program+upcoming, program-compliance, condition-assessment/*).
- **DEFECT 1:** `POST /asset-maintenance/defer` intermittently 504s (2/3 attempts; success took 14.9s) and the UI fails SILENTLY — dialog stays open, no error. Positive controls prove slow-endpoint not outage (program GET 320ms, validation 400 in 265ms).
- **DEFECT 2:** `/pm-readiness` as CP prints raw `422 permission_denied` JSON instead of the Access Denied card.
- **DEFECT 3:** Technician gets a **blank** WO detail page (0 chars) — no card, no content, despite holding workorders.manage.
- Role matrix (Admin/PM/Tech/AM/CP; EE seat password stale → untestable): PM **denied** the new maintenance pages while customer-facing **CP renders them fully** (inversion → design decision); AM denied WOs; all roles hold identical sessions/workorders perms in /auth/me → route/nav gating disagrees with the permission model everywhere.
- Account assignment (Platform Users → Edit User → Assigned Accounts, owner's pointer) does NOT grant WO visibility — CP still denied after 55→56 accounts.
- PR caveat stale: Compliance is NOT mock-driven on QA (UI 589 deviations == API payload).
- Compliance engine data is real and rich: 589 deviations → 62 patterns, 651 row checkboxes, bulk Acknowledge/Add-to-program.
- Empty states clean on a 2-asset site.

## Deliverables
- Artifact: https://claude.ai/code/artifact/b41e4110-eb94-4f91-985f-9298dc541686
- `docs/bug-reports/2026-09-07-maintenance-program-compliance-reports-verdict.md`
- 2 evidence screenshots in `docs/bug-evidence/zp-maintenance-program-compliance/`

## Depth notes (learning)
- **Positive control turned a "defer is down" claim into "defer is slow":** three timed attempts + a fast GET + a fast validation-400 bracket the failure to the write path's latency crossing the gateway timeout. Severity and fix change completely.
- **Same perms, four different outcomes:** every role holds sessions/workorders manage+view, yet gets opens/blank/denied/no-nav respectively — V1.36 route gates key on features.*, not the permission grants (consistent with the earlier RBAC memory).
- **Batched role sweep:** 4 logins × 4 pages in ONE background browser call (~3 min wall) instead of per-step calls.
