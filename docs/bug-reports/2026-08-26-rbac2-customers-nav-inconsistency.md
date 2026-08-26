# RBAC-2 — Customers nav entry gated inconsistently across roles (live-reproduced)

**Env:** `https://acme.qa.egalvanic.ai` · **Date:** 2026-08-26 · **Severity:** Medium · **Writes:** none
**Register:** https://claude.ai/code/artifact/8ca35956-54c9-4c95-92f9-108c3705a2cd

Two roles with the identical permission pair get different navigation. Reproduced live, both roles
logged in through the real login form with cookies + storage cleared between them.

## Evidence

```
GET /api/auth/v2/me

  Project Manager   roles ["Project Manager"]   90 perms
                    features.customers.view  TRUE
                    features.accounts.view   false
                    → Customers present under  Operations ▸ DATA

  Facility Manager  roles ["Facility Manager"]  75 perms
                    features.customers.view  TRUE     ← identical
                    features.accounts.view   false    ← identical
                    → Customers present in NO category (all 6 enumerated)
```

Screenshots in `evidence/2026-08-26-rbac2-customers-nav/`:
- `pm_customers_operations.jpeg` — PM's Operations panel: PLANNING (EMPs, Scheduling), OPS (Work
  Orders), **DATA (Customers)** selected; 114 sites listed.
- `fm_no_customers.jpeg` — FM's Operations panel: PLANNING (EMPs, Planned Work, Scheduling),
  OPS (Work Orders). **No DATA section, no Customers.**
- `fm_customers_direct.jpeg` — FM at `/customers`: page renders normally, Create Site available,
  2 rows. **Sidebar falls back to "Dashboards"** — the route is orphaned for this role.

## Why it matters
The Facility Manager is entitled to the page and can use it, but has no navigation path to it. Under
the V1.36 rail — where only the open category's links exist in the DOM — there is no way to discover
it; the URL must be typed.

## Corroborated by CI
`RolePermissionUiGatingTest.roleNavGating[Project Manager]` fails on the mirror-image symptom:
```
UI gating mismatch for 'Project Manager':
  • Customers (features.accounts.view): should be HIDDEN (role lacks perm) but is VISIBLE — gating bug
```
The test believes the gate is `features.accounts.view`; the PM lacks it yet sees the link. Combined
with FM's case, the likely root cause is **two candidate flags in play for one nav item**
(`features.customers.view` vs `features.accounts.view`) with the resolution differing by role.

`rbac-ui-tests.yml` is 3-pass/7-fail over its last 10 runs, largely on this.

## Explicitly NOT part of this defect
- **Row counts differ (114 vs 2)** — site visibility is scoped per role. Working as intended.
- **Heading reads "Customers" for PM and "Sites" for FM** on the same route — same root cause; the
  title appears to follow the nav label, and FM has none. Cosmetic consequence, not a separate bug.

## Fix direction
Settle which permission gates the Customers nav item, apply it uniformly, and place the item in one
category for every role. If it legitimately belongs to Operations for some roles and Sales for
others, that rule should be explicit rather than emergent.
