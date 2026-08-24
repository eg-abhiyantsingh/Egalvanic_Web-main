# Deep frontend inventory — every module, every tab, by clicking

**Date:** 2026-08-24
**Prompt:** "check everything in depth everything is present. check all tabs" + "go by front end not by url or api"
**Method:** full click-through of the live V1.36 UI — rail → category → module link → every
`role=tab` → first grid row → detail tabs. Zero URL-jumping for the inventory itself; the one
URL load (`/quotes/{id}`) was a deliberate legacy-route probe.

## What was walked (all present and rendering)

- 7 rail items — the 6 categories **plus the logo button** (`img alt="Egalvanic — Dashboards"`),
  which is the only sidebar way back to the Dashboards panel from a module page.
- 3 dashboards, 29 modules — every one reached by real sidebar clicks.
- **20 list-page tabs** across 7 routes (pm-readiness 2, arc-flash 4, customers 2, labor 3,
  materials 4, test-equipment 2, classes 3) — each clicked, each rendering its own grid.
- **24 detail-page tabs**: Asset 8, Work Order 6, Account 6, Issue 4 — each clicked.
- Quotes status sub-links (5), Planned Work buckets (4), Reporting Branding toggle,
  Scheduling calendar toolbar, Updates (Beamer), account popover.

Full inventory: `docs/product-knowledge/v136-frontend-map.md`.

## Bugs this found in OUR code (fixed)

**The `/plans/{id}` cluster.** The quote/EMP editor moved from `/quotes/{id}` to `/plans/{id}`.
Proved live: a quote row-click lands on `/plans/{id}`, and loading `/quotes/{same-id}` renders
"Quote not found". The killer detail: `"/plans/x".contains("/plan/")` is **false** in Java, so
`PlanningPage.isOnEditorPage()` could never return true —

- `PlanningPage.isOnEditorPage()` now accepts `/plans/` (hard-failed TC_WOP_019–022 before).
- `OpportunitiesPage.openFirstQuoteRow()` (10 hard-assert call sites), `openFirstQuoteFromDetail()`
  (`a[href*='/quotes/']` selector), `quoteRowCountOnDetail()` — all now match `/plans/` first.
- `OpportunitiesTestNG` TC_OPP_63 editor-URL assertion + two assert messages.
- `QuoteLaborInflationRepro` deep-linked the dead route — now `/plans/{id}`.

**Vacuous or mis-aimed tests.**
- `AdminPmSettingsTestNG` TC12 hopped to a "Sites" section button that no longer exists — the
  roundtrip never left the page. Now a real Offices → Users → Offices sidebar roundtrip.
- `EgFormAITestNG` TC19 walked pre-V1.36 tabs {Forms, PM, Classes, Sites} — every click missed.
  Now walks the live /classes tabs.
- `ReportingEngineV2TestNG` TC-1 hunted "Company Settings → Branding & Assets" on
  /admin-dashboard — the live Branding surface is the toggle on /reporting/builder. Repointed.
- /locations is a master-detail TREE: removed it from Phase4's grid-drill and
  UiApiDataConsistency's paginated-grid list, where it silently skipped every run.
- `MiscFeaturesTestNG` dropped the `/schedule` + `/calendar` fallbacks (dead shells that could
  mask a real /scheduling failure).

## New coverage

- **`TabbedModulesSmokeTestNG`** — 18 tests, data-driven from `NavCatalog`: every catalogued
  list tab must exist, click, and render; all 5 quote-status params (incl. the non-obvious
  Closed Won→`accepted`, Closed Lost→`rejected`); all 4 planned-work buckets; the Branding
  toggle roundtrip; the Scheduling toolbar + Week view. **Ran live: 18/18 green.**
- `NavCatalog` gained: the logo/Dashboards affordance (`openDashboards()`, used by
  `navigateTo()` and the nav harvest — harvest now finds 36 routes, was 33), the verified TAB
  catalog (`tabsFor()`, `clickTab()`), and `/accounts/goals→/goals`, `/jobs-v2→/emps`,
  `/sites→/customers` renames.
- `NavCatalogSelfValidation` extended: dashboards-via-logo check + all-20-tabs live check.
  **All checks passed.**

## Product regressions to raise

1. `/accounts/goals` — API 500 on every load (was already flagged; still reproducible).
2. **`/quotes/{id}` renders "Quote not found" for a valid plan id** — users' old quote
   bookmarks/links get a hard dead-end instead of a redirect to `/plans/{id}`.

## Validation evidence

- `TabbedModulesSmokeTestNG`: Tests run: 18, Failures: 0, Errors: 0, Skipped: 0 (live QA).
- `NavCatalogSelfValidation`: ALL CHECKS PASSED — 36 routes harvested, 20/20 tabs clicked.
- Coverage audit ran as a 27-agent workflow (4 auditors + adversarial verifiers); every
  wrong-route finding above was independently confirmed against the code before fixing.
  (Some verifier agents hit a transient credit error; their findings were re-verified inline.)
