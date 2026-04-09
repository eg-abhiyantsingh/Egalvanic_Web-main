# Parallel CI Failure Fixes + ISS_015 / SLD_006 Root Causes

> **Author:** Abhiyant Singh (with Claude AI assistance)  
> **Date:** April 9, 2026  
> **Prompt:** "In parallel some test cases are failing but in normal test case run they are running properly — fix that"

---

## Investigation Summary

### What We Found

The parallel workflow (`parallel-suite.yml`) had **zero actual test failures** — both runs were cancelled mid-execution. Additionally, tests ISS_015 and SLD_006 were failing in **both** sequential and parallel modes. Three distinct root causes were identified.

---

## Root Cause 1: `cancel-in-progress: true` Kills Parallel Runs

### Problem

The parallel workflow had `concurrency: cancel-in-progress: true`. When a second run was triggered (either manually or by accident), GitHub Actions **immediately cancelled** the first run. Since all 10 parallel jobs share the same concurrency group, all 9 incomplete groups showed "cancelled" with 0/0/0 results — making it look like tests failed.

### Evidence

```
Run #24179242722:
  SLD Module (71 TCs)     | completed | success    <- finished in time
  Auth + Site + Connection | completed | cancelled  <- killed mid-run
  Location + Task          | completed | cancelled
  ... (all 9 other groups: cancelled)
```

### Fix

```yaml
# BEFORE: new run kills the active one
concurrency:
  group: parallel-suite
  cancel-in-progress: true

# AFTER: new run queues behind the active one
concurrency:
  group: parallel-suite
  cancel-in-progress: false
```

---

## Root Cause 2: 10 Simultaneous Login Requests

### Problem

All 10 parallel runners log in as the same user (`abhiyant.singh+admin@egalvanic.com`) at the exact same moment. The backend may:
- Invalidate earlier session tokens when a new login occurs
- Hit rate limits on authentication endpoints
- Experience lock contention on the user's session record

BaseTest already has retry logic for "Application Error" pages, which suggests the server struggles under rapid session creation.

### Fix

Added a **stagger delay** per group (0s, 10s, 20s, ... 90s):

```yaml
matrix:
  include:
    - group: auth-site-connection
      stagger: 0     # starts immediately
    - group: location-task
      stagger: 10    # waits 10s
    - group: workorder-issue
      stagger: 20    # waits 20s
    # ... up to 90s for the last group
```

With a new step before test execution:

```yaml
- name: Stagger login delay
  run: |
    if [ "${STAGGER_SECONDS}" -gt 0 ]; then
      echo "Waiting ${STAGGER_SECONDS}s before starting tests..."
      sleep "${STAGGER_SECONDS}"
    fi
```

**Total stagger: 90s** — trivial compared to 1+ hour total runtime. Each runner logs in ~10s apart, preventing session collision.

---

## Root Cause 3: ISS_015 Search Assertion Failure

### Problem

ISS_015 ("Search with invalid term should return 0") reported `got 5` in CI but passed locally.

### Debugging with Playwright

1. **`sendKeys()` in headless CI** doesn't reliably trigger MUI DataGrid's Quick Filter. The characters appear in the input, but the React synthetic event chain doesn't fire the filter debounce.

2. **`getRowCount()` counts rendered DOM rows**, which depends on viewport height due to MUI DataGrid virtualization. In headless CI (1280x1024), ~5 rows are rendered. Locally (1920x1080+), ~11 rows render. When the filter doesn't trigger, the test sees 5 "rendered" rows instead of 0.

3. **React nativeSetter works reliably.** Using `Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set` + `dispatchEvent(new Event('input'))` correctly triggers React's state update in all environments (confirmed via Playwright).

### Fix

```java
// BEFORE: sendKeys (unreliable in headless CI)
searchInput.sendKeys(searchTerm);
int results = issuePage.getRowCount();  // viewport-dependent!

// AFTER: nativeSetter + pagination text (viewport-independent)
issuePage.searchIssues(searchTerm);     // uses nativeSetter
String pagText = js().executeScript(
    "return document.querySelector('[class*=\"MuiTablePagination-displayedRows\"]')?.textContent");
// Parse "0–0 of 0" → total = 0
```

The pagination text (`"X-Y of Z"`) reflects the **actual filtered count** regardless of viewport size or virtualisation.

Same fix applied to ISS_046 (deleted issue not found in search).

---

## Root Cause 4: SLD_006 Canvas Not Rendered in CI

### Problem

SLD_006 ("Verify canvas/diagram container is rendered") failed in CI with `expected [true] but found [false]` after 20s of retrying.

### Debugging with Playwright

1. The SLD page shows **"Select a View to Load Assets"** when no view is pre-selected
2. The react-flow canvas only renders **after** a view is selected from the dropdown
3. Locally, a previously selected view is cached in localStorage — so the canvas auto-loads
4. In CI (fresh Chrome profile), localStorage is empty — no view is auto-selected
5. Without a view, the canvas/react-flow elements may not exist in the DOM at all

### Fix

Added `ensureViewSelected()` helper that clicks "All Nodes" if the prompt is showing:

```java
private void ensureViewSelected() {
    Boolean needsView = (Boolean) js().executeScript(
        "return document.body.innerText.includes('Select a View to Load Assets');");
    if (needsView != null && needsView) {
        // Click "Select View" dropdown, then "All Nodes"
        js().executeScript("/* click Select View button */");
        pause(2000);
        js().executeScript("/* click All Nodes */");
        pause(5000);
    }
}
```

Called at the start of SLD_006 before the canvas check loop.

---

## Files Modified

| File | Change |
|------|--------|
| `.github/workflows/parallel-suite.yml` | `cancel-in-progress: false`, stagger delays per group |
| `src/test/java/.../IssuePart2TestNG.java` | ISS_015 + ISS_046: nativeSetter + pagination-based assertion |
| `src/test/java/.../SLDTestNG.java` | SLD_006: `ensureViewSelected()` before canvas check |
| `docs/ai-features/015-parallel-failure-fixes.md` | This file |
