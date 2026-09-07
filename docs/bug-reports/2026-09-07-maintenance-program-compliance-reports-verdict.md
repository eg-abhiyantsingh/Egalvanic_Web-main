# QA Verdict — Maintenance Program page, Compliance section, and site-report backends

**Tickets:** eg-pz-backend #1175 / #1177, eg-pz-frontend #1340
**Environment:** acme.qa.egalvanic.ai · V1.36 · 2026-09-07 (ticket said "cicd/dev only" — **wrong, all four pages are live on QA** with 200s from every new endpoint; DDL clearly applied)
**Sites:** Android Site 2 `aadcee4c-…` (250 assets, populated) + QA-DEMO ZP3978 Site `344ddef5-…` (2 assets, negative case)
**Artifact:** https://claude.ai/code/artifact/b41e4110-eb94-4f91-985f-9298dc541686

## Verdict: 6/7 QA-review items PASS · 3 defects · 1 unconfirmable

| # | Check | Verdict |
|---|-------|---------|
| 1 | Gantt: pivots, 5-state legend, filters, PDF/CSV export, fullscreen | **PASS** — legend has all 5 states; Group by Asset/Asset class/Location/**Service**; 6-facet Filters popover; Export = PDF+CSV; fullscreen icon |
| 2 | Defer / Record-completion dialogs update state + user due date | **DEFECT** — both dialogs gate correctly, but `POST /asset-maintenance/defer` 504s silently (below) |
| 3 | Compliance: score, strict score, composition, needs-attention, pattern grid, bulk + per-row actions | **PASS** — 0% score + "0% before acknowledgements", composition bar, **589 deviations in 62 patterns**, 651 checkboxes, bulk Acknowledge/Add-to-program, filters |
| 4 | Condition Assessment: header export + evidence sweeper | **PASS** (export + WO button present; sweeper reported "no condition revised in 30 days" — nothing to prove on this site, so auto-tick unexercised) |
| 5 | Reports: 6 cards + WO launcher + ReportGenerationModal gating | **PASS** — exactly 6 cards; modal "Generate Report" disabled until a configuration is chosen; PDF/DOCX outputs |
| 6 | Deploy sanity (no f-string SyntaxError, boots on Py3.11) | **PASS on QA** (indirect: all endpoints answer, app tree imports) |
| 7 | Negative: empty site → empty states not errors | **PASS** — 2-asset site: "Nothing unscheduled comes due in the next 90 days" / 0% over 8 pairs |

## DEFECT 1 — Defer fails silently on gateway timeout (504)
**Repro:** Maintenance Program (populated site) → Defer → asset `11N-H1-1` + service + date → Defer.
**Actual:** `POST /api/asset-maintenance/defer` → **504** (HTML gateway page). Dialog stays open, fields filled, **no error message/toast** — indistinguishable from not clicking. Console-only failure.
**Positive controls (NOT an outage):** attempt 1: 504 ~5s · attempt 2: 504 4.7s · attempt 3: **200 in 14.9s**; control `GET /program` same site: 200 in 320ms; control bad-date POST: 400 in 265ms. The write path is just slow enough on a 250-asset site to cross the gateway timeout intermittently. Fix both the latency and the silent failure.
**Unconfirmable:** on the 200 attempt, the grid row didn't change and the program row JSON has no `user_due_date`/defer field (keys: cells,class_name,com,com_done,id,kind,label,location,node_class_id,overdue_count,planning_status,power_scheme,sd_done,shutdown_cadence_years,shutdown_restriction,status) — where should a deferral surface? Needs dev answer; flagged unverified, not broken.

## DEFECT 2 — Condition Assessment leaks a raw 422 to unpermitted roles
As Client Portal, `/pm-readiness` renders literal `API call failed: 422 - {"error":"permission_denied",…}` as page content. The proper Access Denied card exists (WO detail uses it). Screenshot 02.

## DEFECT 3 — Technician gets a BLANK work-order detail page
`/sessions/{id}` as Technician renders **zero characters** in main — no denial card, no content. Role holds `workorders.manage`. Looks like a crash to the user.

## Role matrix (all roles hold sessions/workorders view+manage in /auth/me)
| Role | WO nav | WO detail | Maint. Program | Condition Assessment |
|---|---|---|---|---|
| Admin | ✅ | ✅ opens | ✅ | ✅ |
| Project Manager | ✅ | ✅ opens | ❌ **Access Denied** | ✅ |
| Technician | ❌ | ❌ **BLANK** | ⚠️ not rendered | ✅ |
| Account Manager | ❌ | ❌ Access Denied | ❌ Access Denied | ✅ |
| Client Portal | ❌ | ❌ Access Denied | ✅ renders fully | ❌ raw 422 |
| Electrical Engineer | — | — | — | untestable: stored password no longer logs in (needs reset, not a bug claim) |

**Inversion for a design decision:** customer-facing CP renders Maintenance Program + Compliance in full (589-deviation drill-down) while internal PM gets Access Denied. Either exposure or an accidental lockout.
**Account assignment ≠ WO visibility:** added the CP user to the WO's account via Platform Users → Edit User → **Assigned Accounts** (200, 55→56 accounts) — WO still Access Denied. No WO surface exists for the role.

## Caveat correction for the PR body
"Compliance page is currently mock-driven" is **stale on QA**: the page calls `GET /api/program-compliance/{sld}` and UI numbers match the payload exactly (589 deviations; by_class Panelboard 140 / CB 127 / Fuse 87 / ATS 76 / Relay 56 …).

## Test data — full URLs
- Program: https://acme.qa.egalvanic.ai/maintenance/program
- Compliance: https://acme.qa.egalvanic.ai/maintenance/compliance
- Reports: https://acme.qa.egalvanic.ai/maintenance/reports
- Condition Assessment: https://acme.qa.egalvanic.ai/pm-readiness
- Defer asset: https://acme.qa.egalvanic.ai/assets/532b10d9-fba9-4a76-8f18-1f86638f7195 (11N-H1-1)
- WO used for role matrix: https://acme.qa.egalvanic.ai/sessions/1e6f95ca-0d1e-4fb6-bdad-3a8eab820550

## Evidence
- `docs/bug-evidence/zp-maintenance-program-compliance/01-program-gantt-five-state-legend.jpg`
- `docs/bug-evidence/zp-maintenance-program-compliance/02-cp-raw-422-error-condition-assessment.jpg`
