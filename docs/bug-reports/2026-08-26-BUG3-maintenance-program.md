# BUG 3 — Maintenance Program: role visibility + missing site picker

**Env:** `https://acme.qa.egalvanic.ai` · V1.36 · **Date:** 2026-08-26 · **Severity:** Medium · **Writes:** none
**Ticket pack (all 3 bugs):** https://claude.ai/code/artifact/bdca4aad-49c3-4c16-bc86-cd4dc638ddf5

Reported by the repo owner from a live session; verified across all six provisioned roles.

## 3a — Client Portal and Facility Manager see Maintenance; Admin does not

| Role | Rail categories |
|---|---|
| Client Portal | Dashboards · **Maintenance** · Site Data · Engineering |
| Facility Manager | Dashboards · **Maintenance** · Site Data · Operations · Engineering |
| Technician | Dashboards · — · Site Data · Operations · Engineering · Sales · Builder |
| Project Manager | Dashboards · — · Site Data · Operations · Engineering |
| Account Manager | Dashboards · — · Site Data · Operations · Engineering · Sales · Builder |
| Admin | Dashboards · — · Site Data · Operations · Engineering · Sales · Builder · Admin |

**Actual:** only Client Portal (external customer role) and Facility Manager get the Maintenance
section. An **Admin cannot see a section an external customer can**. Nobody is blocked from the route
— every role opens `/maintenance/program` fine by URL.

**Expected:** Admin should see at least everything Client Portal sees; and whether an external client
should see Maintenance Program at all is a product decision.

**Note:** no role holds any `maintenance.*` permission, so this section is not gated by the
permission system at all. Links: `/maintenance/overview`, `/maintenance/program`.

Screenshots: `bug3_cp_maintenance.jpeg`, `bug3_admin_no_maintenance.jpeg`

## 3b — The CURRENT SITE picker disappears on this page

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
