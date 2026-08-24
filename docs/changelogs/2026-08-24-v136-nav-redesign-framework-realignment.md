# V1.36 nav redesign — framework realignment

**Date:** 2026-08-24
**Prompt:** "check for web ui is updated so update our code too."
**App under test:** https://acme.qa.egalvanic.ai — V1.36
**Method:** live browser walk of the app first, then code changes; every claim below was
verified against the running app rather than inferred from the frontend's route table.

---

## What changed in the web UI

### 1. The sidebar is now two levels (the big one)

A narrow 76px icon **rail** with six category buttons — Site Data, Operations, Engineering,
Sales, Builder, Admin — opens a sub-panel holding that category's module links. The panel
auto-opens to whichever category owns the current route.

**Only the open category's anchors exist in the DOM.** The others are not hidden, they are
absent. Measured from `/assets`: a single sidebar read finds **8** routes; expanding all six
categories finds **33**.

This is what silently broke navigation. `//a[normalize-space()='Work Orders']` from `/assets`
matches nothing, so the click no-ops, no exception is raised, and the `isOnXPage()` guard that
follows still reports the old page — which means the direct-URL fallback is skipped too. The
test then asserts against stale data and logs a pass.

### 2. The role switcher was removed

The account menu (avatar at the foot of the rail) now renders Roles as read-only text —
"Project Manager, Account Manager, Electrical Engineer, Super Admin, Admin" — with no
Autocomplete, Select or combobox anywhere in the popover. Remaining controls: Edit Company,
Email Preferences, English/Français, Sign Out.

### 3. Routes moved

| Old | Now | Note |
|---|---|---|
| `/admin` | `/admin-dashboard` ("Setup") | `/admin` redirects to `/users`; `a[href='/admin']` gone |
| `/accounts` | `/customers` ("Customers") | detail is **still** `/accounts/{id}` |
| `/accounts/goals` | `/goals` | legacy path returns **500** on every load |
| Settings sub-sections | top-level `/users`, `/offices`, `/classes`, `/pm-plans`, `/labor`, `/materials`, `/test-equipment` | the `Sites\|Users\|Classes\|PM` switcher no longer exists |

Renamed labels: Opportunities → **Quotes** (create button now "New Quote"), Accounts →
**Customers**, Arc Flash → **Arc Flash Readiness**, Settings → **Setup**.

New modules with no coverage before: `/attachments`, `/planned-work`, `/panel-schedules`,
`/equipment-designations`, `/site-walks`, `/legacy-procedures`, `/legacy-forms`,
`/sales-overview`, `/ops-dashboard`, `/services`, `/pm-readiness`.

### 4. Login page

No Terms checkbox any more (zero checkboxes on the page) — consent is plain text with two
links. New English/Français toggle whose choice persists; in French the submit button reads
"Se connecter", which defeats every text-matched locator.

### 5. Dead routes

These resolve but render an empty shell — nav chrome, empty `<main>`, no 404:
`/admin/templates`, `/admin/forms`, `/admin/reporting`, `/admin/page-templates`,
`/admin/reporting-config`, `/admin/version-rules`, `/test-equipment-library`,
`/equipment-library`, `/equipment-insights`, `/settings`, `/work-orders`, `/schedule`,
`/calendar`, `/templates`, `/jobs`, `/release-updates`, `/zuniversity`, `/help`, `/learn`.

**Absence from the sidebar does not mean dead.** `/planning`, `/reporting`, `/maintenance`,
`/notes`, `/agent`, `/analyzer`, `/reporting/legacy` and `/goals` all render real content while
having no nav entry, so each was checked individually instead of being inferred away. Four of
them were on track to be "fixed" until the live check contradicted it.

---

## What changed in our code

### New: `utils/NavCatalog`

The live nav encoded once — route → owning category, route → label, legacy renames, dead
routes. Plus the driver-side helpers:

- `navigateTo(driver, route)` — expands the owning category, clicks the anchor, verifies the
  landing, falls back to a direct `get()` so a nav regression degrades to "still tested"
  rather than "silently skipped".
- `collectAllNavHrefs(driver)` — expands all six categories and unions the result. Required
  anywhere the full nav is inspected.
- `onRoute()` / `pathOf()` — exact-path matching. Substring matching is actively wrong here:
  `/admin` is a substring of both `/admin-dashboard` and `/admin/audit-log`, and `dashboard`
  of all three dashboards.

### Fixed

| File | Was | Why it mattered |
|---|---|---|
| `AdminPmSettingsPage` | `a[href='/admin']` + 30s hunt for a "PM" section button | Dead selector, dead route, dead switcher — `navigateToPmSection()` could not succeed at all. Now goes to `/offices`. |
| `RolePermissionUiGatingTest` | single sidebar read | Reported ~25 of 33 modules "hidden" for every role → mass false permission failures. |
| `RbacUiPermissionMatrixTest` | single read + `/accounts` | Same, plus a View cell that could only ever read "hidden". |
| `ZP1997DOMPurifyXssTestNG` | `/admin/forms` | Empty DOM on a dead route failed the "> 50 elements" check and reported it as **DOMPurify stripping content** — a fabricated sanitizer bug. |
| `GenerateReportEgFormTestNG` | `/admin` → Forms tab | Asserted "Forms tab not visible — RBAC misconfigured", diagnosing a permission bug for a route move. Now asserts the grid at `/eg-forms`. |
| `BugHuntPagesTestNG` | `/admin` for a site count | Landed on `/users` and reported the **user** count as a site count. |
| `GoalsTestNG` | `/accounts/goals` | Every Goals test ran against a 500 error page. |
| `Phase4QualityGatesTestNG` | 17 `/admin?view=` + 9 `/reporting?view=` rows | All 17 redirect to `/users` with the query dropped; all 9 render the same "Coming Soon" stub. 26 green "distinct screens" covering two. |
| `EgFormAITestNG` | 15 × `/admin/templates` | Every downstream assertion ran against a blank page. |
| `LoginPage` | `isTermsCheckboxDisplayed()` | Never checked for a checkbox — only for a label mentioning "Terms" — so it answered the wrong question. Added `selectEnglishIfOffered()`. |
| `DashboardPage` | `contains("dashboard")`, logout anchor | Matched `/admin-dashboard`; logout anchor no longer exists (now Sign Out in the account menu). |
| `BaseTest.ensureActiveRole` | role pinning | Documented as non-functional — kept (tolerant by contract) rather than deleted so callers don't silently lose the pin if the control returns. |

---

## Validation

Not just compiled — run against live QA:

- `AdminPmSettingsTestNG` — **12/12 pass**, log confirms `Offices open=true @ /offices`;
  create / rename / language / delete / search all green.
- `Phase5ModuleInteractionTestNG` — **15/15 pass, 0 skipped** (the dead `/equipment-library`
  row used to skip silently).
- `NavCatalogSelfValidation` (new `main()`, excluded from suites, mirrors
  `VerifierSelfValidation`) — all checks pass: 8 → 33 routes when categories are expanded,
  "Work Orders" confirmed absent from the DOM on `/assets`, cross-category navigation lands,
  and all 36 catalogued routes resolve to themselves.

```
mvn -q test-compile dependency:build-classpath -Dmdep.outputFile=/tmp/cp.txt
java -cp "target/classes:target/test-classes:$(cat /tmp/cp.txt)" \
  com.egalvanic.qa.testcase.NavCatalogSelfValidation
```

---

## Product issue worth raising

~~`/accounts/goals` returns 500 on every load while `/goals` serves the module normally.~~
**Withdrawn — owner ruling 2026-08-24:** the `/accounts/goals` path is not in use (nothing
links to it; the module lives at `/goals`), so this is not a bug to ticket. The observation
stands only as a reason tests must never target that path.
