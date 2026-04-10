# Changelog: Fix 14 CI Failures — SiteSelection (12) + Connection (2)

**Date**: April 10, 2026  
**Time**: ~6:30 PM - 7:15 PM  
**CI Run**: #24243436666  
**Prompt**: Debug 12 SiteSelection failures (SS_001-SS_012) and 2 Connection failures (CC_006, DC_004)

---

## Root Cause Analysis

### The Problem: Fire-and-Forget JS vs Asynchronous Alert Rendering

The "App Update Available" alert in the QA environment renders **asynchronously** — React mounts the dashboard shell first, then the alert banner appears later (sometimes 1-10 seconds after page load). In CI headless Chrome, this timing gap is even larger.

The previous fix (commit `af460f6`) added a DISMISS button click inside `dismissBackdrops()`, but that method uses **fire-and-forget JavaScript**: it runs the DOM query once and returns immediately. If the DISMISS button hasn't been rendered by React yet, the query finds nothing and the alert stays active.

### Why SiteSelection Failed (12 tests)

`SiteSelectionTestNG` is a standalone class (does NOT extend BaseTest). Its `loginAsAdmin()` called `dismissBackdrops()` after a 1-second pause, but in CI the DISMISS button took longer than 1s to render. The alert stayed on screen, blocking the facility selector (`//input[@placeholder='Select facility']`), causing all 12 tests to fail.

### Why Connection Tests Failed (CC_006 and DC_004)

`ConnectionTestNG` extends BaseTest, which has the same fire-and-forget `dismissBackdrops()` issue in `loginAndSelectSite()`. The alert was NOT dismissed at login, but CC_001-CC_005 passed because they ran quickly (within ~30s of login). By the time CC_006 ran (priority 6), the alert had fully rendered and was blocking the "Create Connection" button click. `openCreateConnectionDrawer()` used JS to click the button, but the alert overlay intercepted the click — the drawer never opened, and the wait for `//input[@placeholder='Select source node']` timed out.

DC_004 (priority 43) failed at the same point: `createTestConnection()` → `openCreateConnectionDrawer()`.

---

## Solution: WebDriverWait (Polling) Instead of Fire-and-Forget

**Key insight**: `WebDriverWait` polls repeatedly (every 500ms by default) until a condition is met or timeout expires. Unlike fire-and-forget JS that runs once, WebDriverWait catches the DISMISS button even when React renders it 5-8 seconds after the dashboard shell.

### Defense-in-Depth (3 Layers)

| Layer | Where | What | Catches |
|-------|-------|------|---------|
| 1 | BaseTest `loginAndSelectSite()` | `waitAndDismissAppAlert()` — 10s WebDriverWait | Alert at login |
| 2 | ConnectionPage `openCreateConnectionDrawer()` | `dismissAppAlert()` + retry logic | Delayed alert (async) |
| 3 | ConnectionTestNG/Part2 after `driver.get()`/`.refresh()` | `waitAndDismissAppAlert()` or `dismissBackdrops()` | Alert after page reload |

---

## Files Changed & Why

### `BaseTest.java` — Parent class for all test classes extending BaseTest

**Change 1**: Added `waitAndDismissAppAlert()` protected method (lines 464-505)
- **What**: New method using `WebDriverWait(driver, Duration.ofSeconds(10))` to poll for the DISMISS button
- **Why**: Any subclass (ConnectionTestNG, AssetTestNG, etc.) can call this after `driver.get()` or `driver.navigate().refresh()`
- **How it works**:
  1. WebDriverWait polls for `//button[text()='DISMISS']` up to 10s
  2. If found → Selenium click → pause 1s for React re-render
  3. If not found → no-op (alert not present, that's fine)
  4. Always runs `dismissBackdrops()` as safety net (removes MUI backdrops, Beamer)

**Change 2**: Upgraded `loginAndSelectSite()` (line 339)
- **Before**: `dismissBackdrops(); pause(1000);` (fire-and-forget, misses late-rendering alert)
- **After**: `waitAndDismissAppAlert();` (polls up to 10s, catches the alert reliably)
- **Why**: This is the first moment after login where the alert can appear. Catching it here prevents cascading failures in ALL test classes that extend BaseTest.

**Change 3**: Added documentation note to `dismissBackdrops()` (lines 509-513)
- **Why**: Future developers (and AI) need to understand when to use `dismissBackdrops()` (quick, fire-and-forget) vs `waitAndDismissAppAlert()` (robust, for after login/reload).

### `SiteSelectionTestNG.java` — Standalone class (own browser, own login)

**Change 1**: `@BeforeMethod` now calls `waitAndDismissAppAlert()` instead of `dismissBackdrops()`
- **Why**: This class doesn't extend BaseTest, so it needs its own robust dismissal

**Change 2**: `loginAsAdmin()` now calls `waitAndDismissAppAlert()` instead of `dismissBackdrops()`
- **Why**: Same timing issue — fire-and-forget missed the alert after login

**Change 3**: Added `waitAndDismissAppAlert()` method (lines 1574-1614)
- **What**: Same WebDriverWait pattern as BaseTest, plus an extra step: waits up to 15s for the facility selector (`FACILITY_INPUT`) to appear
- **Why**: SiteSelection tests depend on the facility selector being available. After dismissing the alert, the selector may still take a moment to populate. The 15s wait ensures it's ready.

### `ConnectionPage.java` — Page object for /connections

**Change 1**: `openCreateConnectionDrawer()` now has retry logic (lines 104-151)
- **What**: Wraps the existing JS button click + wait in a 2-attempt loop
- **Why**: If the alert blocks attempt 1, the retry dismisses the alert and tries again
- **How**:
  1. Attempt 1: `dismissAppAlert()` → JS click → wait for SOURCE_NODE_INPUT (25s)
  2. If attempt 1 fails → `dismissOverlays()` + `dismissAppAlert()` → retry
  3. If attempt 2 fails → throw RuntimeException with descriptive message

**Change 2**: Added `dismissAppAlert()` private method (lines 1030-1048)
- **What**: `WebDriverWait(driver, Duration.ofSeconds(5))` for DISMISS button
- **Why**: Shorter timeout (5s vs 10s) because this is called inline during test actions, not just at login. If the alert is present, 5s is enough to find it. If not, we don't want to add 10s delay.
- **Note**: This is in the page object (not BaseTest) because `openCreateConnectionDrawer()` doesn't have access to BaseTest methods.

### `ConnectionTestNG.java` — Connection module tests (45 TCs)

**Change 1**: `testSetup()` recovery path (line 70): Added `waitAndDismissAppAlert()` after `driver.get()`
- **Why**: When `ensureOnConnectionsPage()` fails, the recovery does `driver.get(BASE_URL + "/dashboard")` which is a full page reload → re-triggers the alert. Without dismissal, `connectionPage.navigateToConnections()` fails because the sidebar is blocked.

**Change 2**: Added `dismissBackdrops()` after every `driver.navigate().refresh()` (lines 436, 616, 681, 696)
- **Why**: `driver.navigate().refresh()` is a full page reload in a React SPA → React re-initializes → alert re-appears. Used fire-and-forget `dismissBackdrops()` (not full `waitAndDismissAppAlert()`) because:
  1. The 3-5 second pause before these calls gives React time to render the button
  2. Keeping tests fast — 5+ refresh calls with 10s waits would add minutes

### `ConnectionPart2TestNG.java` — Connection part 2 tests (65 TCs)

Same pattern as ConnectionTestNG:
- `testSetup()` recovery: `waitAndDismissAppAlert()` after `driver.get()` (line 78)
- `ensureConnectionExists()` retry: `dismissBackdrops()` after refresh (line 135)
- Grid empty retry loop: `dismissBackdrops()` after refresh (line 774)
- Load performance test: `dismissBackdrops()` after refresh (line 1212)

---

## Key Learnings

1. **Fire-and-forget JS is unreliable for async UI elements**: If the element isn't in the DOM when the script runs, it misses it. Use `WebDriverWait` to poll.

2. **Alert timing varies between local and CI**: Locally, the DISMISS button appears within 500ms. In CI headless Chrome, it can take 5-10 seconds. Always use timeouts that account for CI.

3. **Defense-in-depth prevents cascading failures**: Dismissing the alert at login (Layer 1) catches most cases. But the alert can re-appear asynchronously, so having dismissal at the point of action (Layer 2) and after page reloads (Layer 3) ensures no single timing gap causes a failure.

4. **Standalone classes need separate fixes**: SiteSelectionTestNG doesn't inherit from BaseTest — fixing BaseTest alone wouldn't help. Always check if the failing class extends BaseTest or stands alone.

5. **`driver.get()` and `driver.navigate().refresh()` re-initialize the React app**: These trigger full page reloads, which re-run React's initialization including the app update check. Always dismiss the alert after these calls.

---

## Expected Impact

| Tests | Before | After |
|-------|--------|-------|
| SS_001-SS_012 | 12 FAIL | 12 PASS |
| CC_006 | FAIL | PASS |
| DC_004 | FAIL | PASS |
| **Total fixed** | **14** | **14 PASS** |
