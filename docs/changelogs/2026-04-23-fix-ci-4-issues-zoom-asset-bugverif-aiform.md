# 2026-04-23 — Fix 4 CI issues: zoom → 90%, Asset smoke nav, bug-verif false passes, AI Form backdrop

## Prompt
> https://github.com/eg-abhiyantsingh/Egalvanic_Web-main/actions/runs/24781832617 — its very wrong. I think assertion is wrong. Smoke test should be pass and Curated Bug Verification all should fail and AI form creation you are going wrong. Then zoom to 90%.

## Model in use
Claude Opus 4.7 (1M context), xhigh effort, always-thinking on.

## Branch safety
Work on `main` of the **QA automation framework repo**. Production repo untouched.

## The 4 issues diagnosed from CI run 24781832617

| Group | Observed | Expected | Root cause |
|---|---|---|---|
| Curated Bug Verification | 5 pass / 3 fail | All 8 fail | Tests silently pass when prerequisite unmet (e.g., cookie unreadable → reported as "no bug") |
| Smoke Suites | 22 pass / 9 fail | All pass | All 9 failures = Asset smoke `Click failed for: //span[normalize-space()='Assets']` — backdrop covered sidebar |
| AI Form Creation | 44 pass / 12 fail | All pass | All 12 failures = `element not interactable` — backdrop covered AI_BUTTON |
| Browser zoom | 80% | 90% | Aesthetic + likely contributing to layout-related interactability issues |

## Fix 1 — Zoom 80% → 90% (7 files)

```
src/test/java/com/egalvanic/qa/testcase/BaseTest.java
src/test/java/com/egalvanic/qa/testcase/SiteSelectionSmokeTestNG.java
src/test/java/com/egalvanic/qa/testcase/BugHuntTestNG.java
src/test/java/com/egalvanic/qa/testcase/ReportingEngineV2TestNG.java
src/test/java/com/egalvanic/qa/testcase/EgFormAITestNG.java
src/test/java/com/egalvanic/qa/testcase/AuthSmokeTestNG.java
src/test/java/com/egalvanic/qa/testcase/MonkeyTestNG.java
```

Single line each: `js.executeScript("document.body.style.zoom='80%';")` → `...='90%';`.

## Fix 2 — Asset smoke: sidebar nav clickability

### What was failing
All 9 Asset smoke tests (`testCreateAsset`, `testReadAsset`, `testUpdateAsset`, `testAddOCPChild`, `testAssetDetailNavigation`, `testAssetSearch`, `testEditAndCancel`, `testAssetFullLifecycle`, `testDeleteAsset`) failed with:
```
Click failed for: By.xpath: //span[normalize-space()='Assets'] | //a[normalize-space()='Assets'] | //button[normalize-space()='Assets']
```

### Two root causes
1. **Tight XPath**: only matched `<span>`, `<a>`, `<button>` with EXACT text "Assets". In current MUI, the sidebar link is `<a href="/assets"><span>Assets</span></a>` wrapped in a structure where the visible text might have extra whitespace or nested elements.
2. **No backdrop dismissal**: `click()` helper in AssetPage tried regular click → JS click fallback, but never removed the MUI update-banner backdrop that covers sidebar items in CI.

### Fixes in [AssetPage.java](src/main/java/com/egalvanic/qa/pageobjects/AssetPage.java)

**(a) Broader ASSETS_NAV / LOCATIONS_NAV selectors** — add href-based matching as primary, with text-based as fallback:
```java
private static final By ASSETS_NAV = By.xpath(
    "//nav//a[@href='/assets' or @href='/assets/']"
    + " | //aside//a[@href='/assets' or @href='/assets/']"
    + " | //a[@href='/assets' or @href='/assets/']"
    + " | //span[normalize-space()='Assets']"
    + " | //a[normalize-space()='Assets']"
    + " | //button[normalize-space()='Assets']"
    + " | //*[@role='button' and normalize-space()='Assets']");
```
Href-based matching is more stable than text (doesn't break when a badge is added, text is translated, or child elements rearrange).

**(b) `click(By)` now calls `dismissBlockers()` before every attempt**:
```java
private void click(By by) {
    dismissBlockers();           // ← new: kills MUI backdrops + dismisses update banner
    try { wait.until(...).click(); }
    catch (Exception e) {
        dismissBlockers();        // ← new: retry after another cleanup
        WebElement el = driver.findElement(by);
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", el);
        js.executeScript("arguments[0].click();", el);
    }
}
```

`dismissBlockers()` hides all `.MuiBackdrop-root`, `.MuiModal-backdrop`, and clicks any visible "DISMISS" button on the update banner.

## Fix 3 — Curated Bug Verification: false-positive passes

### The problem
5 of the 8 bug-verification tests reported "PASS" when the user knows the bugs are still present. Looking at the test logic, the issue is that these tests silently PASS when the PREREQUISITE for verification isn't met. Example:

**BUG-007 (SameSite=None) before:**
```java
String accessSs = access != null ? access.getSameSite() : null;
boolean bugPresent = "None".equalsIgnoreCase(accessSs) || "None".equalsIgnoreCase(refreshSs);
Assert.assertFalse(bugPresent);  // accessSs=null → bugPresent=false → PASS (wrong!)
```
If the cookie didn't exist or Selenium couldn't read SameSite, `accessSs == null`, `"None".equalsIgnoreCase(null) == false`, so `bugPresent == false`, and `assertFalse(false) == PASS`. The test reported "no bug" despite verifying nothing.

### The fix — add explicit prerequisite checks

Before the actual assertion, fail explicitly if we CAN'T verify:

**BUG-002 (CSP Beamer)** — fail if Beamer didn't load any fonts at all:
```java
Assert.assertTrue(totalFonts > 0L,
    "BUG-002: Cannot verify — Beamer didn't load any fonts in this run.");
Assert.assertEquals(blocked, 0L, "BUG-002: " + blocked + " fonts CSP-blocked...");
```

**BUG-005 (no maxLength)** — fail if the JS native-setter didn't inject 1500+ chars:
```java
if (!maxLenSet && valueLen < 1500) {
    Assert.fail("BUG-005: Cannot verify — JS setter injected only " + valueLen
              + " chars (expected >= 1500). React may have prevented the write.");
}
Assert.assertFalse(bugPresent, "BUG-005: ...");
```
Also added a JS double-read (`js.executeScript("return arguments[0].value;")`) to confirm the live DOM value matches what `getAttribute("value")` reports.

**BUG-007 (SameSite=None)** — fail if cookies missing OR SameSite attribute unreadable:
```java
Assert.assertFalse(access == null && refresh == null,
    "BUG-007: Cannot verify — neither auth cookie found after login.");
if (access != null && accessSs == null && refresh != null && refreshSs == null) {
    Assert.fail("BUG-007: Cannot verify — Selenium.Cookie.getSameSite() returned null for both.");
}
Assert.assertFalse(bugPresent, "BUG-007: ...");
```

### Result
- When the bug IS present → FAIL (as before)
- When the bug is verified ABSENT → PASS (as before)
- When the test CAN'T verify → FAIL with a specific "cannot verify" message (previously: silent PASS)

## Fix 4 — AI Form Creation: all 12 failures were "element not interactable"

### The problem
Every AI Form test used raw `driver.findElement(AI_BUTTON).click()` — no backdrop dismissal, no fallback. In CI (headless Chrome), the MUI update banner renders AFTER the admin page and covers the AI button. All clicks throw `element not interactable`.

### The fix in [EgFormAITestNG.java](src/test/java/com/egalvanic/qa/testcase/EgFormAITestNG.java)
Added two helpers:
- `dismissAllBlockers()` — kills MUI backdrops + clicks "DISMISS" button
- `safeClick(By)` — calls `dismissAllBlockers()` first, scrolls into view, tries native click, falls back to JS click after another cleanup

Replaced ~15 raw `.click()` calls with `safeClick()`:
- `safeClick(AI_BUTTON)` × 7 occurrences
- `safeClick(GENERATE_BUTTON)` × 6 occurrences
- `safeClick(ADD_FORM_BUTTON)` × 1 occurrence
- `clickFormsTab()` helper now uses `safeClick(FORMS_TAB)`

## Verification
```bash
mvn clean test-compile   # → BUILD SUCCESS, 57 test sources
```

All 4 fixes compile cleanly. No new warnings beyond the pre-existing Selenium 4 `getAttribute` deprecation (consistent with the rest of the codebase — not in scope for this fix).

## Expected behavior on the next CI run

### Smoke Suites (was 22/9 fail)
All 9 Asset smoke failures should now pass because:
- `ASSETS_NAV` xpath matches `<a href="/assets">` regardless of nested structure
- `click()` helper dismisses backdrops before every attempt
- Zoom 90% gives slightly more layout room for sidebar items

### AI Form Creation (was 44/12 fail)
All 12 failures should now pass because:
- Every `AI_BUTTON`, `GENERATE_BUTTON`, `ADD_FORM_BUTTON` click now runs through `safeClick()` which dismisses blockers first
- If the update banner appears mid-test, the retry path handles it

### Curated Bug Verification (was 5/3)
Expected: **all 8 FAIL** on the next run IF the underlying bugs are still present in production. The 3 already-failing tests (BUG-001, BUG-004, BUG-008) stay failing. The 5 previously-passing tests will now either:
- Fail with the actual bug evidence (if the bug is reproducible in CI)
- Fail with a "Cannot verify — …" message (if the prerequisite for detection wasn't met)

Either way, the client report will no longer show a misleading "3 of 8 bugs are fixed" claim.

## In-depth explanation (for learning + manager reporting)

### Why href-based selectors beat text-based for sidebars
Text-based xpath (`//span[text()='Assets']`) breaks when:
- The label changes case, adds whitespace, or is translated
- A badge/count is appended (e.g., `<span>Assets<badge>42</badge></span>`)
- The sidebar re-renders with slightly different DOM structure

Href-based (`//a[@href='/assets']`) is stable because:
- The URL route is the contract between the SPA router and the link
- Changes to the visible label don't affect the href
- One xpath pattern works across multiple MUI versions

### Why "Cannot verify" > silent PASS
A silent PASS is the worst outcome for a verification test — it tells the reader "we checked, and all is well" when we actually didn't check. In a client report this becomes "bug is fixed", which is a false claim.

Explicit "Cannot verify — prerequisite X was missing" is:
- **Honest**: we didn't check; here's why
- **Actionable**: the message names the prerequisite so it can be investigated
- **Safe**: Fails the test so it shows up in CI as requiring attention

This is the same principle as "never silently swallow exceptions" — applied to verification logic.

### Why the backdrop-dismiss pattern keeps recurring
The eGalvanic SPA surfaces a "New version available" banner via `MuiBackdrop`. On every page load, this can re-render AFTER the main content, covering nav items. A single dismiss-at-start doesn't help because the backdrop returns on later React re-renders. The correct pattern is "dismiss immediately before every click", which is what AssetPage's new `click(By)` and EgFormAITestNG's `safeClick(By)` both do.

Further consolidation: both could eventually delegate to `BaseTest#dismissBackdrops()`, but EgFormAITestNG doesn't extend BaseTest (different lifecycle needs) so duplicating 5 lines of JS is cheaper than refactoring the inheritance hierarchy.

## Rollback
`git revert <this-commit>` restores the 80% zoom + the tight XPath + the silent-PASS bug verifications + the raw AI Form clicks.
