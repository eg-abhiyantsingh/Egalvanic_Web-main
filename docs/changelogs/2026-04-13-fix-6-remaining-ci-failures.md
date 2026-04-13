# Fix 6 Remaining CI Failures from Run #24249334552

**Prompt:** "check everything in ci cd too sometimes it fail in ci cd"
**Date:** 2026-04-13, ~12:49 PM IST
**Commit:** `610ee40` — Fix 6 remaining CI failures: BugHuntTestNG alert dismissal + TaskTestNG grid render

---

## What This Prompt Was About

After fixing 13 CI failures in the previous prompt, the user asked me to verify CI/CD. I analyzed the full run #24249334552 and discovered **19 total failures** (not 17 as originally counted). The previous fix addressed 13 — this prompt fixes the remaining 6.

---

## Complete Failure Breakdown (Run #24249334552)

| Job | Failures | Tests | Status |
|-----|----------|-------|--------|
| Work Order + Issue (234 TCs) | 2 | ISS_015, ISS_046 | Fixed in previous commit |
| Dashboard + BugHunt (105 TCs) | 9 | BUGD01, BUGD60, BUG013, BUG011, BUG027, **BUG03, BUG08, BUG16, BUG29** | 5 fixed previously, **4 fixed now** |
| Auth + Site + Connection (130 TCs) | 3 | SF_001, SF_002, SF_003 | Fixed in previous commit |
| Asset Parts 1-2 (69 TCs) | 1 | ATS_ECR_17 | Fixed in previous commit |
| Location + Task (135 TCs) | 4 | ET_006, TD_002, **TD_003, TD_004** | 2 fixed previously, **2 fixed now** |
| **Total** | **19** | | **13 + 6 = all 19 addressed** |

---

## Root Cause Analysis

### BugHuntTestNG Failures (BUG03, BUG08, BUG16, BUG29)

**Error:** `TimeoutException at navigateToLogin:163` — all 4 tests timed out after exactly 15 seconds.

**What happened step by step:**
1. `@BeforeMethod` runs — tries to dismiss the "DISMISS" button on the **current** page
2. Test calls `navigateToLogin()` which does `driver.get(AppConstants.BASE_URL)`
3. Page reloads — the "A new app update is available" alert overlay appears **on the freshly loaded page**
4. `WebDriverWait(15s)` looks for `EMAIL_INPUT.isDisplayed()` — but the alert overlay blocks it
5. After 15s, `TimeoutException` is thrown

**Why BUG01 and BUG02 passed but BUG03 failed:**
- BUG01 (ConsoleErrorsOnPageLoad) and BUG02 (DevRevSRIIntegrityFailure) were the FIRST tests to run
- On the very first navigation, the alert might not appear yet (first-time load)
- By the time BUG03 runs, the alert mechanism is primed and appears immediately

**Why BUG04-BUG07 passed between BUG03 and BUG08:**
- BUG04-07 (XSS tests) do NOT call `navigateToLogin()` — they work on the already-loaded page
- So they never trigger a new alert

### TaskTestNG Failures (TD_003, TD_004)

**Error:**
- TD_003: `Grid should have Title column expected [true] but found [false]` (ran in 0.079s)
- TD_004: `Grid should show valid created dates expected [true] but found [false]` (ran in 0.272s)

**What happened:**
1. `@BeforeMethod.testSetup()` calls `ensureOnTasksPage()`
2. `ensureOnTasksPage()` calls `driver.get(TASKS_URL)` + `pause(6000)` + `waitForGrid()` — but **NO alert dismissal**
3. The "app update" alert overlay appears and blocks full grid rendering
4. `waitForGrid()` finds the MUI DataGrid container (it's in the DOM behind the overlay) but column headers have empty text
5. TD_003 finds `headers` (non-empty list) but `getText()` returns empty strings → `headerText` has no "Title"
6. Assertion fails instantly (0.079s execution time = no reload needed, just empty header text)

---

## Code Changes

### File 1: BugHuntTestNG.java (2 changes)

**Change 1: `navigateToLogin()` — Add alert dismissal inside navigation and wait loop**

```java
// BEFORE:
private void navigateToLogin() {
    driver.get(AppConstants.BASE_URL);
    new WebDriverWait(driver, Duration.ofSeconds(15))
            .until(d -> {
                try {
                    return d.findElement(EMAIL_INPUT).isDisplayed()
                            || d.findElement(By.tagName("body")).getText().contains("Error");
                } catch (Exception e) { return false; }
            });
}

// AFTER:
private void navigateToLogin() {
    driver.get(AppConstants.BASE_URL);
    dismissAppAlert();  // <-- NEW: dismiss alert immediately after navigation
    new WebDriverWait(driver, Duration.ofSeconds(15))
            .until(d -> {
                try {
                    dismissAppAlert();  // <-- NEW: keep dismissing each poll cycle
                    return d.findElement(EMAIL_INPUT).isDisplayed()
                            || d.findElement(By.tagName("body")).getText().contains("Error");
                } catch (Exception e) { return false; }
            });
}
```

**Why dismiss inside the loop?** The alert doesn't appear instantly — it renders asynchronously after React hydration. The initial `dismissAppAlert()` catches early-rendering alerts. The one inside the loop catches late-rendering alerts.

**Change 2: New `dismissAppAlert()` helper method**

```java
private void dismissAppAlert() {
    try {
        js.executeScript(
            "var btns = document.querySelectorAll('button');" +
            "for (var i = 0; i < btns.length; i++) {" +
            "  var txt = btns[i].textContent.trim();" +
            "  if (txt === 'DISMISS' || txt === 'Dismiss') { btns[i].click(); break; }" +
            "};" +
            "document.querySelectorAll('.MuiBackdrop-root, [class*=\"MuiBackdrop\"]').forEach(" +
            "  function(b) { b.style.display = 'none'; b.style.pointerEvents = 'none'; }" +
            ");"
        );
    } catch (Exception ignored) {}
}
```

**Why a separate method?** BugHuntTestNG doesn't extend `BaseTest` — it has its own browser instance. So it can't use `waitAndDismissAppAlert()` from BaseTest. This lightweight helper does the same fire-and-forget dismissal.

### File 2: TaskTestNG.java (3 changes)

**Change 1: `ensureOnTasksPage()` — Add alert dismissal after both `driver.get()` calls**

```java
// BEFORE:
driver.get(TASKS_URL);
pause(6000); // Headless Chrome SPA hydration needs more than 4s

// AFTER:
driver.get(TASKS_URL);
pause(3000);
waitAndDismissAppAlert(); // driver.get() triggers "app update" alert in CI
pause(3000);
```

**Why split 6000ms into 3000 + alert + 3000?** The original 6s pause was there because the developer noticed CI needed more time. But the real blocker was the alert, not slow rendering. By dismissing the alert at the 3s mark, the remaining 3s gives the grid time to render unblocked.

**Change 2: TD_003 (`testTC_TD_003_WorkOrderColumn`) — Replace `dismissBackdrops()` with `waitAndDismissAppAlert()`**

Three replacements in this test:
- Start of test: `dismissBackdrops()` → `waitAndDismissAppAlert()`
- First reload path: `dismissBackdrops()` → `waitAndDismissAppAlert()`
- Second reload path: `dismissBackdrops()` → `waitAndDismissAppAlert()`

**Why?** `dismissBackdrops()` is fire-and-forget — it tries to click DISMISS once and returns immediately. If the button hasn't rendered yet (common in CI), it misses it. `waitAndDismissAppAlert()` waits up to 10 seconds for the DISMISS button to be clickable, then clicks it. Much more reliable.

**Change 3: TD_004 (`testTC_TD_004_CreatedDateColumn`) — Same pattern**

Two replacements: start of test and reload path.

---

## Key Concept: `dismissBackdrops()` vs `waitAndDismissAppAlert()`

| | `dismissBackdrops()` | `waitAndDismissAppAlert()` |
|---|---|---|
| **Wait for button?** | No — fire-and-forget | Yes — up to 10 seconds |
| **Reliable in CI?** | No — alert often renders late | Yes — WebDriverWait polls |
| **Handles backdrops?** | Yes (MUI + Beamer) | Yes (calls dismissBackdrops() at end) |
| **When to use** | Cleanup after known-good state | After `driver.get()` or page reload |

**Rule of thumb:** After any `driver.get()` call, always use `waitAndDismissAppAlert()`. For quick cleanup when you're sure there's no alert, `dismissBackdrops()` is fine.

---

## CI Run Triggered

**Run:** [#24330787699](https://github.com/eg-abhiyantsingh/Egalvanic_Web-main/actions/runs/24330787699)
**Workflow:** Parallel Full Suite — Core Regression (961 TCs)
**Status:** In progress

This run includes ALL 19 fixes (13 from previous commit + 6 from this commit).

---

## Files Modified (2 source files)

| File | Changes |
|------|---------|
| BugHuntTestNG.java | `navigateToLogin()` alert dismissal + new `dismissAppAlert()` helper |
| TaskTestNG.java | `ensureOnTasksPage()` alert dismissal + TD_003/TD_004 `waitAndDismissAppAlert()` |
