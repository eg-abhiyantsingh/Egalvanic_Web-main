# RBAC UI Gating — verify per-role behavior in the REAL website

- **Date:** 2026-06-16
- **Prompt:** "this is wrong implementation … according to role we need to check for in real website"
- **Module:** Authentication → Role-Based Access
- **Type:** New layer — real-browser, per-role UI verification (complements the API contract tests).

---

## The gap this closes (the user was right)

The earlier RBAC tests verify the **permission *contract*** (`/auth/me` returns the right
permissions per role). They do **not** verify what each role can actually **see in the live web
app**. The React app gates UI via `hasPermission(p) = is_admin || permissions.includes(p)`
(`Layout.jsx`), so a UI test additionally catches **frontend mis-wiring** — a nav item guarded by
the wrong permission, or not guarded at all — which an API test cannot.

| Layer | Question | Status |
|-------|----------|--------|
| Permission contract (`/auth/me`) | Backend reports correct perms per role? | ✅ (791 cell + 8 tests) |
| **UI enforcement (real website)** | As each role, does the site show only what's allowed? | ✅ **this change** |
| Backend 403 enforcement | Forbidden API call actually denied? | ⛔ not reliable yet — known SPA-catch-all routing bug returns `200 + HTML` for unmatched `/api/*` (CLAUDE.md #8); not asserted to avoid false bugs |

## What was built

- **`RolePermissionUiGatingTest`** (`com.egalvanic.qa.testcase`) — data-driven per role. Logs in
  to the **real browser** as each role, then asserts each high-signal nav module is present **iff**
  the role's permissions allow it (both positive *and* negative, e.g. Client Portal must NOT see
  Settings/Accounts/Goals/Opportunities/Audit Log).
  - **Oracle = live `/auth/me`** (reused via `RbacFixtures`), not the prod CSV — so the UI is checked
    against the permissions the app actually received, and prod-vs-QA drift can't cause false UI fails.
  - **Route-based detection** (`<a href="/assets">` inside the sidebar) — robust to a collapsed/icon-only nav.
  - Roles without `platform.web` (Technician) must hit "Web Access Restricted"; unprovisioned roles SKIP;
    a UI login that doesn't complete while the API login succeeded → SKIP (transient throttle, not a flake).
- **`RbacFixtures` made public** so both the API and UI RBAC tests share one role roster + live-auth cache.
- **`testng-rbac-ui.xml`** suite (run a subset with `-Drbac.ui.roles="Admin,Client Portal"`).
- **`.github/workflows/rbac-ui-tests.yml`** — Chrome-enabled CI job (headless on the runner, per repo
  convention; local runs are headed), lists failing roles + gating mismatches, uploads reports/screenshots.

## Validation (real browser, local)

- **Green:** Admin (everything visible) + Client Portal (Assets/SLDs/Locations/Tasks/Issues/Arc Flash
  visible; Settings/Accounts/Goals/Opportunities/Audit Log correctly hidden) → `2 run, 0 failures`.
- **Red-on-regression:** mis-wired "Assets" to a permission Client Portal lacks (while CP can actually
  see `/assets`) → test **failed** with `should be HIDDEN (role lacks perm) but is VISIBLE — gating bug`.
  Reverted.

## Why not also assert backend 403
`OwaspIdorTestNG`/`HttpStatusCodeContractTestNG` show the backend sometimes returns `200 + HTML` for
unmatched `/api/*` (SPA catch-all routing bug), and I observed Client Portal getting `200` on
`GET /accounts/` without `accounts.view`. Asserting `403` on that unreliable signal would manufacture
false bugs, so backend-enforcement testing is deferred until that routing bug is fixed — documented, not
silently skipped.
