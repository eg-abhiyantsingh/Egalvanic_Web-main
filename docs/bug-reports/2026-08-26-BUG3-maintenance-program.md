# BUG 3 — Maintenance Program: the site picker disappears

**Env:** `https://acme.qa.egalvanic.ai` · V1.36 · **Date:** 2026-08-26 · **Severity:** Medium · **Writes:** none
**Ticket pack (all 3 bugs):** https://claude.ai/code/artifact/bdca4aad-49c3-4c16-bc86-cd4dc638ddf5

Reported by the repo owner from a live session; verified across all six provisioned roles.

## 3a — WITHDRAWN: role visibility is per requirement

| Role | Rail categories |
|---|---|
| Client Portal | Dashboards · **Maintenance** · Site Data · Engineering |
| Facility Manager | Dashboards · **Maintenance** · Site Data · Operations · Engineering |
| Technician | Dashboards · — · Site Data · Operations · Engineering · Sales · Builder |
| Project Manager | Dashboards · — · Site Data · Operations · Engineering |
| Account Manager | Dashboards · — · Site Data · Operations · Engineering · Sales · Builder |
| Admin | Dashboards · — · Site Data · Operations · Engineering · Sales · Builder · Admin |

**WITHDRAWN — not a bug.** Owner confirmed 2026-08-26: **per requirement the Admin is not meant to
see the Maintenance section.** I had argued the opposite (that Admin should see everything a customer
role sees). The observed visibility — Client Portal and Facility Manager only, with Admin, Project
Manager, Account Manager and Technician excluded — is **correct**.

**Still worth a separate decision:** the route carries no permission gate. No role holds any
`maintenance.*` permission, so visibility is nav-only — every role, Admin included, can open
`/maintenance/program` by URL and it renders. Low risk, but if the section is customer-facing by
design the route should enforce it rather than relying on the nav.

**Note:** no role holds any `maintenance.*` permission, so this section is not gated by the
permission system at all. Links: `/maintenance/overview`, `/maintenance/program`.

Screenshots: `bug3_cp_maintenance.jpeg`, `bug3_admin_no_maintenance.jpeg`

## The real bug — the CURRENT SITE picker disappears on this page

Measured in the same session, same role:
```
/assets               2 site comboboxes · "CURRENT SITE" label present
/maintenance/program  0 comboboxes      · no "CURRENT SITE" label
```
Confirmed for Project Manager, Account Manager, Facility Manager, Client Portal, Technician, Admin.

**Actual:** the picker is gone, yet the page content is site-scoped — *"Android Site 2 has no active
maintenance program yet."* You are looking at one site's information with no way to switch site.

**Expected:** the site picker should remain visible, since the page shows site-specific content.
Otherwise the user must leave the page, change site elsewhere, and navigate back.

Screenshots: `bug3_picker_present.jpeg`, `bug3_picker_missing.jpeg`

**Minor, same code path:** copy differs by role — Facility Manager sees "This site has no active
maintenance program yet" where other roles see the site name.

## Method note — a gap this exposed in earlier sweeps
My earlier nav enumeration hardcoded six category names (Site Data, Operations, Engineering, Sales,
Builder, Admin) and therefore **never saw Maintenance or Dashboards at all**. Rail categories must be
discovered dynamically from the DOM, not assumed. Corrected here.

---

## Re-tested on a site that HAS a program (owner correction, 2026-08-26)

My first pass tested a site whose page read *"…has no active maintenance program yet"* — an empty
state that exercises almost no logic. Re-ran everything on **(s) Wild Goose Brewery**
(90 assets, plan `abhiyant v2 - 29 july brewery`, plan_id `2ad184b0-9337-4740-ae7e-2e91b44b26bb`).

### 3b still stands — and matters more with data
`/maintenance/program` still renders **0 comboboxes**, while `/maintenance/overview` for the same
site renders **2** (the CURRENT SITE picker). So the picker is present on Site Overview and absent on
Maintenance Program — it is specific to that one page, not to the Maintenance section.

### NO BUG in the numbers — retracted before filing
I nearly filed a third defect ("all four stat cards read 0 while the plan holds 6 released work
orders"). **That was wrong.** The cards are **window-scoped** and recompute per timeline tab:

| Tab | Header | Scheduled | Completed | Timeline |
|---|---|---|---|---|
| 1 year | 2026 service calendar | 0 | 0 of 0 | "No scheduled work falls in this window" |
| 3 year | 2026–2028 service calendar | **8** | 0 of 8 | Q3 ’27 and Q3 ’28 populated |
| 5 year | 2026–2030 service calendar | **16** | 0 of 16 | Q3 ’27 → Q3 ’30 populated |

Ground truth from `/api/plans/{id}` — scheduled occurrences are **Q3 2027, Q3 2028, Q3 2029,
Q3 2030, Q3 2031**. Today is 2026-08-26, so a 1-year window genuinely contains no work: the empty
1-year timeline and its zeroed cards are **correct**. 8 scheduled over 3 years = 4 service rows × 2
occurrences — matches the timeline dots exactly. The page is internally consistent.

### Site Overview — verified correct
| Shown | Cross-check | Result |
|---|---|---|
| TOTAL ASSETS 90 | `/assets` grid = 90; `lookup/v2/nodes` total = 90 | matches |
| ISSUES IDENTIFIED 1 | `/issues` grid = 1 | matches |
| Resolved 0 / Unresolved 1 | consistent with 1 open issue | matches |
| Breakdown 89 + 0 + 1 + 0 | = 90 | matches |
| condition index 99 / 100 | 89 good ÷ 90 = 98.9% | matches |

The page even documents its banding rule ("assets with no issue on record read as good, closed-out
issues as serviceable, one open issue as watch"). **Site Overview and the condition index are correct
— nothing to file.**

### Lesson recorded
`feedback_test_populated_not_empty_states` — empty states prove nothing, and window-scoped widgets
must be read on every tab before calling a number wrong.
