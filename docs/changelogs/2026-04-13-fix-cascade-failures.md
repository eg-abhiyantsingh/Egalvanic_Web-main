# Fix Connection + Location Cascade Failures

**Prompt:** "check side by side too so that you dont need to wait until its completed"
**Date:** 2026-04-13, ~5:30 PM IST
**Commit:** `341f701` — Fix Connection + Location cascade failures: login-page recovery + alert after refresh

---

## What This Prompt Was About

Continuing from the previous fixes (19 CI failures → 1 remaining), I investigated the two infrastructure cascade failures that appeared in run #24334665862:
- **ConnectionTestNG**: 14 test failures (Edit/Delete/Search cascade)
- **LocationPart2TestNG**: 42 test failures (Building/Floor/Room cascade)

These were initially dismissed as "infrastructure issues, not our code" but root cause analysis revealed they ARE our code — both caused by the same "app update" alert pattern.

---

## Root Cause Analysis

### ConnectionTestNG Cascade (14 failures)

**Chain of events:**
1. `testCL_005_SpinnerDisappears` calls `driver.navigate().refresh()`
2. Uses `dismissBackdrops()` (fire-and-forget) — misses the alert that renders asynchronously after React hydration
3. Alert survives into subsequent tests
4. `ensureConnectionExists()` → `createTestConnection()` → `openCreateConnectionDrawer()` — the Create button click lands on the alert overlay instead of the drawer button
5. "Create Connection drawer did not open after 2 attempts" → RuntimeException
6. All 14 Edit/Delete/Search tests fail because they all flow through `ensureConnectionExists()`

**Same pattern in Delete tests:** `testDC_002_ConfirmDelete` and `testDC_004_DeleteMultiple` also refresh with `dismissBackdrops()`, compounding the problem.

### LocationPart2TestNG Cascade (42 failures)

**Chain of events:**
1. `LocationPart2TestNG.classSetup()` calls `super.classSetup()` → login → lands on dashboard
2. `@BeforeMethod` calls `ensureOnLocationsPage()` → `navigateToLocations()`
3. The "app update" alert blocks sidebar navigation (Strategies 1-3 all fail silently)
4. Strategy 4: `driver.get(locationsUrl)` → full page reload → redirects to login page
5. `recoverFromErrorPage()` only checks for crash-page text ("Application Error", "something went wrong") — **login page doesn't match**
6. Browser stays on login page for all 42 remaining tests
7. Every test's `ensureOnLocationsPage()` repeats the same failed navigation

**Key insight:** `recoverFromErrorPage()` had a blind spot — it couldn't detect session loss (login-page redirect).

---

## Code Changes (6 files)

### File 1: BaseTest.java — Login-page detection in `recoverFromErrorPage()`

```java
// BEFORE (line 219):
if (!isApplicationErrorPage()) return;

// AFTER:
if (!isApplicationErrorPage()) {
    // Check for unexpected login-page redirect (session expired or alert blocked navigation)
    if (driver.findElements(By.id("email")).size() > 0) {
        System.out.println("[BaseTest] Unexpected login page detected — re-logging in...");
        loginPage.login(AppConstants.VALID_EMAIL, AppConstants.VALID_PASSWORD);
        pause(2000);
        dashboardPage.waitForDashboard();
        waitAndDismissAppAlert();
        selectTestSite();
    }
    return;
}
```

**Why this works:** The `By.id("email")` check fires on every `@BeforeMethod`. On a normal page, there's no email field → returns instantly. On the login page → detects it and re-logs in. This is the "root guard" that protects ALL test classes, not just LocationPart2.

### File 2: ConnectionTestNG.java — 4 refresh calls fixed

All 4 instances of `dismissBackdrops()` after `driver.navigate().refresh()` replaced with `waitAndDismissAppAlert()`:
- Line 439: `testCL_005_SpinnerDisappears`
- Line 619: `testDC_002_ConfirmDelete`
- Line 684: `testDC_004_DeleteMultiple` (first delete)
- Line 699: `testDC_004_DeleteMultiple` (second delete)

### File 3: ConnectionPart2TestNG.java — 3 refresh calls fixed

Same pattern:
- Line 135: `ensureConnectionExists()` recovery refresh
- Line 774: Grid row wait fallback refresh
- Line 1212: `testCONN_056_LoadPerformance` refresh

### File 4: LocationPart2TestNG.java — Alert dismissal in 2 locations

- `ensureOnLocationsPage()`: Added `waitAndDismissAppAlert()` after `navigateToLocations()` — only runs when navigation was needed
- Recovery path (line 82): Added `waitAndDismissAppAlert()` after `driver.get(dashboard)` before `navigateToLocations()`

### Files 5-6: WorkOrderTestNG.java + WorkOrderPart2TestNG.java

Added `waitAndDismissAppAlert()` after the grid-not-found refresh fallback in `ensureOnWorkOrdersPage()`.

---

## Key Concept: The Alert Timing Problem

| Event | Time | What happens |
|-------|------|--------------|
| `driver.navigate().refresh()` | 0ms | Page reload starts |
| HTML shell loads | ~500ms | `document.readyState` = complete |
| `dismissBackdrops()` fires | ~600ms | Scans DOM once — DISMISS button not yet rendered |
| React hydrates | ~1500ms | SPA re-initializes |
| "App update" alert renders | ~2000ms | DISMISS button appears — **too late, dismissBackdrops already returned** |
| Next test interaction | ~3000ms | Click hits alert overlay instead of target element |

`waitAndDismissAppAlert()` solves this by polling every 500ms for up to 10s. If the alert renders at 2s, it catches it at the 2.5s poll.

---

## Audit: Framework-Wide Refresh Pattern

Scanned all test classes for `navigate().refresh()` usage:
- **18 total instances** across 14 test classes
- **9 fixed** (4 ConnectionTestNG + 3 ConnectionPart2TestNG + 1 each WorkOrder)
- **2 already safe** (DashboardBugTestNG — already uses `waitAndDismissAppAlert()`)
- **7 remaining** in classes that currently pass CI (SLD, CriticalPath, Auth, IssuesSmoke, etc.)

The BaseTest `recoverFromErrorPage()` login-detection fix serves as a safety net for the remaining 7. If any start failing due to the alert, the recovery will catch the login-page redirect.

---

## CI Runs

| Run | Commit | Purpose | Status |
|-----|--------|---------|--------|
| #24340766639 | `47e9b00` | Verify TD_003 JS-first fix | In progress |
| (next) | `341f701` | Verify cascade fixes | Pending trigger |
