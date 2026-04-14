# Fix Report Duplication + Email Toggle + Deep Quality Audit

**Date:** 2026-04-14
**Scope:** CI infrastructure fixes + live app quality investigation via Playwright

---

## Part 1: Report Duplication Bug (FIXED)

### Problem
Every test case appeared **exactly twice** in the Consolidated Client Report HTML. The report showed 1,930 total tests (actually 965 unique × 2), inflating the count and making the report unreliable.

### Root Cause
**Two copies of `testng-results.xml` per group were uploaded as artifacts.**

The data flow that caused the duplication:

1. `full-suite-dashboard.sh` line 541 copies surefire-reports into `reports/groups/`:
   ```bash
   cp -r target/surefire-reports/* "reports/groups/group-N-KEY/"
   ```
2. `parallel-suite.yml` uploaded **both** `target/surefire-reports/` and `reports/groups/` as artifacts
3. The summary job downloaded all artifacts into `all-reports/`
4. `consolidated-report.py` used `glob('**/testng-results.xml')` which found **both copies per group**
5. Result: every test parsed twice → 965 × 2 = 1,930

### Fix (3 files changed)

| File | Change |
|------|--------|
| `.github/workflows/parallel-suite.yml` | Removed `target/surefire-reports/` from artifact upload (line 281) |
| `.github/workflows/parallel-suite-2.yml` | Same fix for Suite 2 (line 233) |
| `.github/scripts/consolidated-report.py` | Added deduplication safety net: dedup by `(class_name, method_name)`, prefer FAIL > SKIP > PASS |

The `reports/groups/` directory already contains the copy (made by the dashboard script). The `target/surefire-reports/` was redundant.

---

## Part 2: Email Notification Issue (FIXED)

### Problem
Emails were sent after every CI run even when the user expected them to be disabled.

### Root Cause
In `parallel-suite.yml` line 384, the email toggle was:
```yaml
SEND_EMAIL_ENABLED: ${{ secrets.EMAIL_PASSWORD != '' && 'true' || 'false' }}
```
This evaluates to `'true'` whenever the `EMAIL_PASSWORD` secret exists — there was **no user-controllable toggle**.

### Fix
Added a `workflow_dispatch` input to both parallel suite workflows:
```yaml
inputs:
  send_email:
    description: 'Send email report after completion'
    type: boolean
    default: true
```
Changed the email env to require **both** the input toggle AND the secret:
```yaml
SEND_EMAIL_ENABLED: ${{ inputs.send_email && secrets.EMAIL_PASSWORD != '' && 'true' || 'false' }}
```
Now you can uncheck "Send email report" when triggering the workflow manually to suppress email.

---

## Part 3: Deep Quality Audit via Playwright (17 findings)

Browsed the live app (https://acme.qa.egalvanic.ai) as a manual tester would, checking every major module. The 965 passing tests verify "does the button work?" but miss "what breaks when users do the unexpected?"

### Critical Findings

| # | Severity | Finding | Page | Screenshot |
|---|----------|---------|------|------------|
| 1 | **CRITICAL** | **404 routing broken**: Unknown routes (`/nonexistent-page-test`) show blank page with NO error message, different navigation sidebar, and a **different app version** (V1.21 vs V1.18.2). Suggests SPA routing falls through to a different frontend build. | Any invalid URL | `17-404-error-page.png` |
| 2 | **HIGH** | **Form validation doesn't auto-scroll**: Clicking "Create Issue" with empty required fields shows errors in DOM but they're **below the fold**. User sees no feedback — the form appears to do nothing. | Issues > Create | `09-create-issue-form.png`, `12-validation-errors-scrolled.png` |
| 3 | **HIGH** | **Issue Class\* has no validation error message**: Marked required (\*) in the form but submitting empty shows NO error — only Title and Proposed Resolution show "is required" messages. Some existing issues in the grid have "—" in Issue Class column, confirming data leaked through. | Issues | `12-validation-errors-scrolled.png` |

### Console Errors (Every Page Load)

| # | Severity | Finding | Details |
|---|----------|---------|---------|
| 4 | **HIGH** | **DevRev PLuG SDK fails on every page** | `net::ERR_FAILED` loading `plug-platform.devrev.ai/static/plug.js`. 3 console errors on every single page load. |
| 5 | **MEDIUM** | **Beamer tracking 400 + PII leak** | `backend.getbeamer.com/track` returns 400. URL contains `firstname=abhiyant&lastname=admin&custom_user_id=77e99d86-...` but `email=` is empty (causing the 400). User PII sent in a failing request URL. |

### UI/UX Issues

| # | Severity | Finding | Page | Screenshot |
|---|----------|---------|------|------------|
| 6 | **HIGH** | **Date format inconsistency across modules**: Issues/Tasks use DD/MM/YYYY, Work Orders use "MMM DD, YYYY", Scheduling uses M/D/YY | Cross-module | Multiple |
| 7 | **MEDIUM** | **Time format inconsistency**: "5:37 PM" vs "4:04 pm" (uppercase vs lowercase) in the same Scheduling list | Scheduling | `14-scheduling-page.png` |
| 8 | **MEDIUM** | **Update banner doesn't persist dismissal**: Dismissed on Dashboard, reappears on every subsequent page navigation | All pages | `13-locations-page.png` |
| 9 | **MEDIUM** | **"Equipment at Risk" card value overflow**: `$4010.4k` overflows card width, truncated at right edge | Dashboard | `01-dashboard-overview.png` |
| 10 | **LOW** | **Grammar bug**: "1 connections" should be "1 connection" (plural for singular count) | Connections | `07-connections-page.png` |
| 11 | **LOW** | **Column/header truncation across pages**: "Schedu..." (Scheduling header), "Attem..." (Audit Log column), "Oppo..." (Opportunities header), Issue Title column too narrow to read | Multiple | Multiple |
| 12 | **LOW** | **"Mutation Audit Log"** exposes GraphQL technical terminology to end users | Audit Log | `16-audit-log-page.png` |

### Data Quality / Test Environment Issues

| # | Severity | Finding | Page |
|---|----------|---------|------|
| 13 | **HIGH** | **Test data pollution**: Assets grid dominated by `AutoTest_` entries. Issues full of `Smoke Test Issue` entries. Work Orders include `ShouldNotExist_767...` (failed cleanup). Opportunities have `tesyfd b`. Test automation creates data but never cleans up. | All data modules |
| 14 | **MEDIUM** | **122/122 pending tasks are overdue**: Every single pending task has a November 2025 due date (5+ months overdue). Due Soon = 0. | Tasks |
| 15 | **MEDIUM** | **Work order `ShouldNotExist_767...`** still exists — test tried to delete it but cleanup failed | Work Orders |

### Coverage Gaps (Zero Automation)

| # | Finding |
|---|---------|
| 16 | **6 modules have ZERO test coverage**: Scheduling, Opportunities, Accounts, Service Agreements, Attachments, Work Order Planning |
| 17 | **Form validation is not tested**: No test verifies error messages, required field enforcement, or validation UX (auto-scroll, inline errors). The Issue Class required field validation is actually broken. |

### All Screenshots

Saved in `research-screenshots/` directory:
```
01-dashboard-overview.png         — Dashboard KPI cards, Equipment at Risk overflow
02-assets-test-data-pollution.png — Assets grid full of AutoTest_ entries
03-assets-search-panel.png        — Search results for "Panel"
04-issues-page.png                — Issues grid with truncated titles
05-work-orders-page.png           — Work Orders with ShouldNotExist entry
06-tasks-page.png                 — Tasks with 122/122 overdue
07-connections-page.png           — "1 connections" grammar bug
08-create-issue-form.png          — Add Issue drawer (top visible)
09-create-issue-validation.png    — After submit: no visible errors
10-create-issue-validation-scrolled.png — Attempted scroll
11-validation-errors-visible.png  — Errors visible after manual scroll
12-validation-errors-scrolled.png — "Issue title is required" in red
13-locations-page.png             — Update banner returned after dismiss
14-scheduling-page.png            — Empty calendar, time format mismatch
15-opportunities-page.png         — Duplicate names, KPI labels hidden
16-audit-log-page.png             — "Mutation Audit Log" technical term
17-404-error-page.png             — Blank page, wrong nav, V1.21
```

---

## Summary

| Category | Count |
|----------|-------|
| CI infrastructure bugs fixed (code changes) | 2 (report duplication, email toggle) |
| Critical app issues found via Playwright | 3 |
| High severity issues | 5 |
| Medium severity issues | 5 |
| Low severity issues | 4 |
| Modules with zero test coverage identified | 6 |
| **Total findings** | **19** |
