# RBAC — exclude Electrical Engineer by default (kill ~120 noise-skips/run)

- **Date:** 2026-06-18
- **Prompt:** "[CI run link] Tests run: 715, Failures: 0, Errors: 0, Skipped: 121 … why 121 skipped"
- **Module:** Authentication → Role-Based Access (`RbacFixtures`)
- **Type:** Diagnosis + cleanup (no behaviour change to what's actually asserted).

---

## The question

A Parallel-Suite-2 RBAC run reported 715 tests / **121 skipped** (the RBAC-All job: 730 / **122**),
with 0 failures/errors. Why so many skips?

## Diagnosis (from the live CI log)

5 roles logged in and were fully asserted (PM 92 perms, Technician 90, FM 76, Account Manager 77,
Client Portal 37). The skips broke down as:

| Cause | Skips | Detail |
|-------|------:|--------|
| **Electrical Engineer — no QA account** (`login status 401`) | **120** | still in the role matrix, so every EE case skipped: 113 permission-cells + 4 action-enforcement + 1 work-order-edit + 1 nav-gating + 1 contract |
| **Known prod-vs-QA permission drift** | **2** | PM `accounts.view`, FM `features.locations.view` — granted in the prod CSV but absent on the QA tenant, so those *granted* cells skip instead of false-failing |

(EE 113 cells + 2 drift = 115 cell skips ✓; +4+1+1+1 EE = 122 for RBAC-All; API-only omits the UI/nav
+ login EE cases → 121 ✓.) Admin contributes 0 — it's already excluded (mis-provisioned QA account).
Nothing was broken: the framework SKIPs rather than throwing ~120 false failures against an account
that doesn't exist.

## Fix

~98% of the skips were Electrical Engineer, which the repo owner has repeatedly said to ignore — yet
it still generated 120 noise-skips every run. Added **Electrical Engineer** to
`RbacFixtures.DEFAULT_EXCLUDED` (alongside Admin), so a default run no longer enumerates it. Still
forceable via `-Drbac.roles="Electrical Engineer"` if/when a QA account is created.

## Validation

Ran `testng-rbac-permissions.xml` locally after the change:
- Contract test: **6 passed / 0 skipped** (was 1 EE skip).
- Full API suite: **594 passed / 0 failed / 2 skipped** (was 715 / 121).
- The only 2 remaining skips are both `permissionCell` — the documented PM/FM drift cells. EE no
  longer appears in the live-provisioning log.

So a green RBAC run now shows a clean **2 skips** (meaningful, documented) instead of 121–122.
