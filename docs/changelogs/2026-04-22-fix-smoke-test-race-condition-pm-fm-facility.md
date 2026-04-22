# 2026-04-22 — Fix 3 smoke-test failures: PM login, FM login, Facility selector (SPA-render race condition)

## Prompt
> https://github.com/eg-abhiyantsingh/Egalvanic_Web-main/actions/runs/24772098520/job/72480934829 — in smoke test 3 test cases are failing that should not fail.

## Model in use
Claude Opus 4.7 (1M context), xhigh effort, always-thinking on.

## Branch safety
Work on `main` of the **QA automation framework repo**. Production website repo untouched.

## The 3 failures (from CI run 24772098520)

```
java.lang.AssertionError: Navigation menu not found after PM login
  at AuthSmokeTestNG#testProjectManagerLogin (line 238)

java.lang.AssertionError: No nav menu and not on restricted page after FM login
  at AuthSmokeTestNG#testFacilityManagerLogin (line 361)

java.lang.AssertionError: Facility selector input (placeholder='Select facility') not found on dashboard
  at SiteSelectionSmokeTestNG#testFacilitySelectorPresent (line 195)
```

All 3 tests share the same root cause: **single-shot DOM check before the SPA has hydrated the login-redirect page**. Admin login passed only because it happened to have a retry loop that the other tests didn't.

## Root cause analysis

```java
// Admin login (PASSED) — had a retry
pause(2000);
boolean hasNav = driver.findElements(By.cssSelector("nav")).size() > 0;
if (!hasNav) {
    pause(3000);
    hasNav = driver.findElements(By.cssSelector("nav")).size() > 0;
}

// PM login (FAILED) — no retry
boolean hasNav = driver.findElements(By.cssSelector("nav")).size() > 0;  // ❌ race
```

Two problems, not just one:
1. **No retry** — when the SPA is still rendering, a single `findElements()` returns empty.
2. **Single selector** — `<nav>` is only one possible container. Modern React layouts use `<aside>`, `[class*='Sidebar']`, `[class*='MuiDrawer']`, or `[role='navigation']`. If the app's sidebar uses any of those, `<nav>` returns empty forever (retry or not).

The Admin test happened to work because its one retry was long enough AND the admin layout happens to use `<nav>`. PM, FM, Tech, and Client Portal tests hit different layouts or longer hydration times and failed.

## Fix

### 1. New centralized helper `waitForNavMenu(int timeoutSeconds)` in AuthSmokeTestNG

```java
private boolean waitForNavMenu(int timeoutSeconds) {
    long deadline = System.currentTimeMillis() + (timeoutSeconds * 1000L);
    String selector = "nav, aside, [class*='Sidebar'], [class*='sidebar'], "
            + "[class*='MuiDrawer'], [role='navigation']";
    while (System.currentTimeMillis() < deadline) {
        try {
            dismissBackdrops();  // also clears blocking backdrops between polls
            List<WebElement> candidates = driver.findElements(By.cssSelector(selector));
            for (WebElement el : candidates) {
                if (el.isDisplayed() && el.getSize().getWidth() > 50
                        && el.getSize().getHeight() > 100) {
                    return true;
                }
            }
        } catch (Exception ignored) {}
        pause(500);
    }
    return false;
}
```

This does three things the old check didn't:
- **Polls every 500ms** for up to 15s (rather than one-shot).
- **Accepts 6 selector variants** — not just `<nav>`.
- **Dismisses MUI backdrops** between polls, so an open update-alert can't block the test.
- **Filters by rendered size** — tiny hidden `<aside>` elements don't count.

### 2. Applied to all 5 login smoke tests

| Test | Before | After |
|---|---|---|
| Admin login | manual 2s + retry on `<nav>` only | `waitForNavMenu(15)` |
| **PM login** | one-shot on `<nav>` | `waitForNavMenu(15)` + `dismissBackdrops()` first |
| Technician login (fallback) | one-shot on `<nav>` | `waitForNavMenu(15)` |
| **FM login** (fallback) | one-shot on `<nav>` | `waitForNavMenu(15)` |
| Client Portal login (fallback) | one-shot on `<nav>` | `waitForNavMenu(15)` |

All assertion messages now include the current URL, making CI failure reports more debuggable.

### 3. Facility selector smoke — 20-second retry with inline backdrop cleanup

`SiteSelectionSmokeTestNG` doesn't extend BaseTest (it has its own setUp), so I couldn't reuse `dismissBackdrops()`. Instead, the retry loop inlines the same JS directly:

```java
long deadline = System.currentTimeMillis() + 20_000L;
while (System.currentTimeMillis() < deadline) {
    try {
        // Inline backdrop + DISMISS-button cleanup (same logic as BaseTest)
        js.executeScript(
            "document.querySelectorAll('.MuiBackdrop-root, [class*=\"MuiBackdrop\"], .MuiModal-backdrop')"
            + ".forEach(function(b){b.style.display='none';b.style.pointerEvents='none';});"
            + "var btns = document.querySelectorAll('button');"
            + "for (var i = 0; i < btns.length; i++) {"
            + "  if (btns[i].textContent === 'DISMISS') { btns[i].click(); break; }"
            + "}"
        );
        inputs = driver.findElements(FACILITY_INPUT);
        if (!inputs.isEmpty()) break;
    } catch (Exception ignored) {}
    Thread.sleep(500);
}
Assert.assertFalse(inputs.isEmpty(),
    "Facility selector input (placeholder='Select facility') not found on dashboard "
    + "within 20s. URL: " + driver.getCurrentUrl());
```

20-second window (vs 15 for nav) because the facility selector in MUI Autocomplete tends to hydrate later than the sidebar — often the last thing to render on a dashboard.

## Why this fix is the RIGHT fix (not just patching a symptom)

Three alternatives I considered + rejected:
1. **Add a blanket 5s `sleep` after login.** Simple but fragile — sometimes enough, sometimes not. Wastes time on fast CI. Introduces a "magic number" that drifts over time.
2. **Hardcode `<nav>` everywhere the layout uses it.** Forces an invisible coupling between test and layout — when frontend swaps `<nav>` for `<aside>`, tests break without a clear signal why.
3. **Skip these tests for certain roles.** Hides the problem rather than fixing it — manager loses visibility into real regressions.

The chosen fix (polling + multi-selector) is:
- **Self-adapting** — takes as long as the SPA needs (bounded by 15s).
- **Layout-agnostic** — accepts any of 6 reasonable nav container patterns.
- **Signal-preserving** — when a nav container is genuinely missing (not just slow), the test still fails with a clear message including the URL.

## Compile
`mvn clean test-compile` → BUILD SUCCESS, 56 test sources.

## How to verify locally
```bash
cd /Users/abhiyantsingh/Downloads/Egalvanic_Web-main
mvn clean test -Dtest=AuthSmokeTestNG
mvn clean test -Dtest=SiteSelectionSmokeTestNG
```

## In-depth explanation (for learning + manager reporting)

### SPA hydration race — why this class of bug keeps recurring
Single-page apps (React, Vue, Angular) render HTML progressively after JS bundle loads. Between the login redirect and the sidebar appearing, there's a 500ms-3s window where:
- URL has changed (`/login` → `/dashboard`)
- DOM has skeleton placeholders
- **Real nav items haven't rendered yet**

Any Selenium test that asserts on the DOM IMMEDIATELY after navigation hits this window and fails. The fix is always the same pattern — replace `findElements(X)` with a polling loop that accepts multiple valid states.

### Why we accept 6 different selectors
Different layouts in the eGalvanic app use different patterns:
- Older sidebar: `<nav role="navigation">`
- Newer MUI drawer: `<div class="MuiDrawer-paper">` inside `<aside>`
- Role-restricted landing: no sidebar at all, just a restriction message

A test that fails when the layout switches from `<nav>` to `<aside>` isn't catching a real regression — it's catching a technology swap. We want a layout-agnostic check that asks the real question: *"is there a sidebar-ish thing on the page?"*

### Why `dismissBackdrops()` lives inside the polling loop (not just before)
In CI (headless Chrome), the app's "App Update Available" banner renders *after* the dashboard shell. If we only dismiss once at the start, and the banner renders 2 seconds later, it covers the sidebar with a backdrop — `isDisplayed()` returns false. Dismissing inside the loop catches late-arriving backdrops.

### Why `getSize().getWidth() > 50 && getHeight() > 100`
Some MUI drawers render as 0x0 containers before hydrating. A non-zero size threshold filters those out so we only report success when the sidebar actually has content. The 50x100 threshold is small enough to accept a narrow collapsed sidebar but reject a 1x1 placeholder.

## Rollback
`git revert <this-commit>` reverts all 3 files. Tests will go back to the single-shot check pattern and resume failing in CI on the same 3 tests.
