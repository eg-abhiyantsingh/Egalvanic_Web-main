# [Bug | HIGH] App-wide WCAG 2 A/AA violations on all 28 pages (shared components)

**Test:** `com.egalvanic.qa.testcase.Phase4QualityGatesTestNG.testRouteAccessibility`
**Date:** 2026-06-03
**Classification:** REAL_BUG (99% confidence — axe-core, reproduced on every navigable page)
**Severity:** HIGH (accessibility/compliance; blocks keyboard + screen-reader users; legal/ADA exposure)
**Scope:** Every one of the 28 navigable routes (nav + standalone) on `acme.qa.egalvanic.ai`

## Summary

An axe-core (WCAG 2 A/AA) sweep across **all 28 pages** found **critical/serious
violations on every single page** — 28/28 routes failed. The violation *types*
repeat across pages, which means they live in **shared components** (the
nav/AppBar, the MUI DataGrid toolbar, loading spinners), not in any one screen.
Fixing the handful of shared components fixes them everywhere.

## Violations by rule (aggregated across the 28-page run)

| Count | Impact   | Rule                          | Meaning / likely source |
|-------|----------|-------------------------------|-------------------------|
| 54    | critical | `button-name`                 | Icon-only buttons with no accessible name (AppBar/grid actions) — invisible to screen readers |
| 54    | serious  | `color-contrast`              | Text/!icon contrast below 4.5:1 (secondary text, placeholders, disabled states) |
| 20    | serious  | `aria-progressbar-name`       | Loading spinners (`role=progressbar`) with no label |
| 4     | critical | `aria-required-children`      | An ARIA role missing its required child roles (likely a custom list/grid) |
| 4     | serious  | `scrollable-region-focusable` | Scrollable container not keyboard-focusable |
| 2     | serious  | `listitem`                    | `<li>` not contained in a `<ul>`/`<ol>` |
| 2     | serious  | `aria-input-field-name`       | Input with an ARIA role but no accessible name |

## Steps to Reproduce

1. Login to `https://acme.qa.egalvanic.ai` (standard QA admin).
2. Open any page (e.g. `/dashboard`, `/assets`, `/planning`).
3. Run an axe-core WCAG 2 A/AA scan (DevTools axe extension, or the automated
   `testRouteAccessibility` gate).
4. Observe ≥2 critical/serious violations per page; the same rule IDs recur
   across pages.

## Expected Result

Zero critical/serious WCAG 2 A/AA violations. Every interactive control has an
accessible name, text meets 4.5:1 contrast, progress indicators are labeled.

## Suggested Fix (high leverage — shared components)

1. **`button-name` (54):** add `aria-label` to every icon-only `IconButton`
   (e.g. `<IconButton aria-label="Filter">`), especially in the AppBar and the
   DataGrid toolbar/row-action buttons. One fix per shared button → fixes all pages.
2. **`color-contrast` (54):** raise the theme's secondary/disabled text and
   placeholder colors to ≥4.5:1 against their backgrounds in the MUI theme.
3. **`aria-progressbar-name` (20):** give the shared loading spinner a label
   (`<CircularProgress aria-label="Loading" />`).
4. **`aria-required-children` / `listitem` / `aria-input-field-name` /
   `scrollable-region-focusable`:** fix the specific custom list/grid/scroll
   components flagged (fewer instances, page-specific).

## Coverage note (this run)

All 28 routes were reachable for the QA admin role (0 skipped). Routes a role
*can't* reach are skipped (not failed) by `skipIfNotRendered()`, so this gate is
safe to run for any permission set. SLD (`/slds`,`/sld`) and Connections
(`/connections`) are excluded — hidden/deprecated in the May 2026 release.

## Per-page violation counts (run 2026-06-03)

dashboard 2, sales-overview 2, ops-dashboard 2, arc-flash 3, pm-readiness 3,
reporting 2, assets 3, locations 3, tasks 3, issues 2, attachments 2, planning 3,
emps 2, sessions 3, scheduling 2, goals 4, opportunities 2, accounts 2, admin 2,
admin/audit-log 3, eg-forms 2, equipment-library 4, maintenance 3, notes 2,
panel-schedules 3, release-updates 2, z-university 2, reporting/builder 2.

---

_Authored from the live all-pages quality-gates run on 2026-06-03. Review/edit
before filing. **Do not** push this file's contents to Jira via tooling — per
project rule, Jira modifications need explicit per-ticket approval._
