# RBAC Work-Order Edit — action enforcement, UI + API

- **Date:** 2026-06-16
- **Prompt:** "project manager try to edit the work order, check he is able to edit or not, and check with api too."
- **Module:** Authentication → Role-Based Access
- **Type:** Action-level RBAC — both backend enforcement (API) and UI capability.

---

## Ask

Go beyond nav-visibility: prove a Project Manager can actually **edit a work order**, verified in
**both** the API and the UI (and, by contrast, that a role without the permission cannot).

## What I found (grounded by live probe before writing assertions)

- A "work order" is the **`/job/`** entity (the SPA route is `/jobs/{id}`; it renders an EMP/commitment
  detail with tabs: Overview, Planned Work Orders, Change Orders, All Procedures).
- The edit endpoint is **`PUT /api/job/{id}`**, gated by **`jobs.manage`**.
- `jobs.manage` is held by Admin, PM, Technician, Electrical Engineer, Facility Manager, Account Manager;
  **Client Portal has only `jobs.view`** → clean positive/negative pair.
- **The backend enforces this cleanly** (unlike list endpoints): a no-op `PUT /job/{id}` as Client Portal
  returns `{"error":"permission_denied","message":"…Required: jobs.manage"}`; as PM it returns `200`.
  So an API enforcement assertion here is reliable (verified live).

## What was built

- **`WorkOrderEditEnforcementApiTest`** (API) — data-driven over all roles. Reads one real job, then for
  each role sends a **no-op** `PUT /job/{id}` (the job's own body, so nothing changes) and asserts:
  role has `jobs.manage` (or is admin) ⇒ **allowed** (2xx, no permission_denied); else ⇒ **denied**
  (`permission_denied`). Oracle = live `/auth/me`. Non-destructive.
- **`WorkOrderEditUiTest`** (real browser) — PM opens a real work order (`/jobs/{id}`) and asserts its
  **edit surfaces** (the *Planned Work Orders* + *Change Orders* tabs) render. There is no single "Edit"
  button on this tabbed EMP detail — editing is done through those tabs (actionable only for a
  `jobs.manage` role) — so reaching them is the meaningful UI signal. The *write being allowed/denied*
  is proven by the paired API test.
- **`RbacFixtures.LiveAuth` now carries the bearer `token`** so a single login per role serves both the
  permission oracle and authenticated API calls.
- Wired into `testng-rbac-permissions.xml` (API) and `testng-rbac-ui.xml` (UI).

## Validation (live)

- **API:** full RBAC API suite `806 run / 0 failures`; work-order edit enforcement **6 PASS / 1 SKIP**
  (Admin/PM/Technician/FM/Account Manager **allowed**; Client Portal **denied**; Electrical Engineer SKIP).
- **API red-proof:** flipping the oracle to `jobs.view` (which CP has) made the test fail with
  *"'Client Portal' HAS jobs.view but the edit was DENIED. Status 422"* — confirms it bites. Reverted.
- **UI:** PM opened work order `1008005` (Active) and the Planned Work Orders + Change Orders edit
  surfaces rendered → green (real browser).

## Honest scoping notes

- The legacy `WorkOrderPage.navigateToWorkOrders()` clicks a sidebar item that now lands on `/sessions`
  (a grid), and the EMP detail has no single "Edit" button — so the UI test navigates **directly** to
  `/jobs/{id}` (id from the API) and asserts the edit-capable tabs, rather than a brittle "Edit button".
- Backend **list** endpoints still don't give a clean enforcement signal (e.g. Client Portal gets `200`
  on `GET /accounts/` without `accounts.view`) — that remains documented and NOT asserted. The **edit**
  endpoint, by contrast, returns an explicit `permission_denied`, which is why action-enforcement is
  assertable here.
