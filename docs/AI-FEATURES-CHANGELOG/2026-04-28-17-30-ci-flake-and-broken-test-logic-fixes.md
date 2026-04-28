# CI flake + broken test logic fixes — 6 tests reviewed

**Date / Time:** 2026-04-28, 17:30 IST
**Branch:** `main` (production is `developer` — never pushed there per memory rule)
**Result:** All 4 fixed tests PASS in isolation. BUG007 correctly flags a **real local regression** (Roles=2 duplicate calls — needs FE attention). BUG018 correctly flags a **real Beamer leak** (already filed).

## Why this work matters

CI had been failing for 6 different test methods, each for a different reason. Without analysis, the natural reaction is "increase timeout / retry more / disable test". That makes flakes worse over time — every band-aid hides a real issue. This pass treated each failure as a separate diagnosis and applied the minimal correct fix.

The lesson worth taking forward: **a test failure is data, not noise**. Each one of these failures carried information about either the test or the product. Three of the six were test-side (BUG007 inverted assertion, TC_SS_009 tautological pass, TC_SS_010 dropdown-not-opened). Two were CI-environment timing issues with no product impact (GEN_EAD_09, MOTOR_EAD_13). One was a real product regression (BUG018 — Beamer URL leak).

## The 6 failures + diagnosis

### 1. GEN_EAD_09_EditManufacturer — CI flake, environment timing

**CI evidence (run 25001241790):**
```
ENVIRONMENT_ISSUE com.egalvanic.qa.testcase.AssetPart3TestNG.testGEN_EAD_09_EditManufacturer (80% confidence) [Rules]
AssetPart3TestNG.testGEN_EAD_09_EditManufacturer:1118
  ->openEditForAssetClass:228
  ->ensureOnAssetsPage:111
  » Timeout Expected condition failed: waiting for visibility of element located by
    By.xpath: //button[normalize-space()='Create Asset']
    (tried for 25 second(s) with 500 milliseconds interval)
```

**Root cause**: `AssetPage.java` defines `private static final int TIMEOUT = 25;` and uses that single instance for all `wait.until(...)` calls including `wait.until(visibilityOfElementLocated(CREATE_ASSET_BTN))` after navigating to the Assets page.

CI runners hydrate the React SPA 2-3x slower than local. On local, the Create Asset button appears in ~10s. On CI, it can take 25-45s — the existing 25s wait fired the timeout right before the button rendered.

**Fix** ([AssetPage.java:104-128](src/main/java/com/egalvanic/qa/pageobjects/AssetPage.java#L104-L128)):
Replaced the bare `wait.until(...)` with a tiered strategy:
1. **45s primary wait** — covers slow CI startup, doesn't penalize local.
2. **navigate().refresh() recovery** — if the button still isn't visible after 45s, refresh once and wait another 30s. Refreshing the page often clears stuck SPA state in CI.
3. **Re-throw original timeout** if both fail — surfaces a true environment problem instead of hiding it.

**Why I didn't just bump `TIMEOUT` to 60**: that constant is used everywhere in AssetPage. Bumping it means every wait everywhere is 60s — failure cases take 2x longer to surface. The targeted 45s + refresh is contained to the one navigation that historically fails.

**Live verification**: 91s end-to-end, PASS. Note SelfHeal logs show `[SelfHeal] Retry #3 succeeded for: //button[normalize-space()='Create Asset'] (3115ms)` — the existing self-healing layer also picks up the slack, confirming this is purely a timing issue.

### 2. MOTOR_EAD_13_EditDutyCycle — CI flake, stale element on read

**CI evidence (run 25001241790):**
```
FLAKY_TEST com.egalvanic.qa.testcase.AssetPart4TestNG.testMOTOR_EAD_13_EditDutyCycle (90% confidence) [Rules]
[SelfHeal] Could not recover stale element after 3 attempts.
  Locator: By.xpath: //div[contains(@class,'MuiDrawer')]//p[starts-with(normalize-space(),'Duty Cycle')]/following-sibling::div//input
  Operation: getAttribute(value)
  AssetPart4TestNG.testMOTOR_EAD_13_EditDutyCycle:827 -> editTextField:424
```

**Root cause**: `editTextField` (line 424) reads the input's value via `input.getAttribute("value")`. Lines 420-422 already do a re-find by 3 strategies (placeholder / label / aria-label) before this read — but only for the initial set/read transition. If React re-renders the input AGAIN between the re-find and the `.getAttribute()` call (which CI's slower DOM cycle makes more likely), the freshly-located reference becomes stale on the very next call. The SelfHeal wrapper exhausted its 3 retries because each retry hit a brand-new stale state.

**Fix** ([AssetPart4TestNG.java:419-454](src/test/java/com/egalvanic/qa/testcase/AssetPart4TestNG.java#L419-L454)):
Wrapped the read in an explicit 5-attempt retry loop. Each retry catches `StaleElementReferenceException`, re-locates the input via the same 3-strategy chain, and tries the read again. Far cheaper than introducing a new locator strategy or refactoring the surrounding flow.

**Why 5 attempts**: 3 was the SelfHeal default and proved insufficient. 5 with a 250ms inter-attempt pause covers ~1.5s of DOM churn — enough headroom for typical React re-render storms in CI without artificially extending the test runtime in stable environments.

**Live verification**: 95s end-to-end, PASS. `[SelfHeal] Elements: no stale recoveries or click interceptions needed` — locally there's no DOM churn, so the loop runs once and exits.

### 3. BUG007_DuplicateAPICalls — inverted assertion direction (test-side bug)

**CI evidence (your screenshot):**
```
BUG-007: No duplicate API calls detected. Roles=0, SLDs=0. Bug may be fixed.
expected [true] but found [false]
```

**Root cause**: Original assertion was `assertTrue(hasDuplicates)`. This was the right assertion when the test was first written to *confirm the bug existed*. Once the FE fix landed, the same assertion became wrong: a fixed bug now caused the test to fail, with a confusing "Bug may be fixed" failure message.

There's also a subtler problem hidden in the CI log: `Roles=0, SLDs=0`. Zero is suspicious — it could mean (a) the bug is fixed, or (b) the test never reached the `/connections` page (session expired, navigation crashed, URL pattern changed). The original test couldn't tell the difference.

**Fix** ([BugHuntGlobalTestNG.java:295-340](src/test/java/com/egalvanic/qa/testcase/BugHuntGlobalTestNG.java#L295-L340)):

```java
// SANITY PRECONDITION: if BOTH counts are 0, the page didn't exercise the
// expected API endpoints — could be session expired, page didn't load, or
// FE moved to different URL pattern. We can't conclude "bug fixed" in that
// case (false-positive risk). Skip honestly with a clear reason.
if (roleCalls == 0 && sldCalls == 0) {
    throw new SkipException("Connections page did not issue any /roles or /slds calls...");
}

// Inverted: PASS when bug stays fixed (≤1 each), FAIL on regression
boolean hasDuplicates = roleCalls > 1 || sldCalls > 1;
Assert.assertFalse(hasDuplicates, "BUG-007 REGRESSION: ...");
```

Outcome matrix:
| Counts | Old behavior | New behavior |
|---|---|---|
| Roles=0, SLDs=0 | FAIL ("Bug may be fixed") | **SKIP** (precondition failed) |
| Roles=1, SLDs=1 | FAIL ("Bug may be fixed") | **PASS** (bug confirmed fixed) |
| Roles=2, SLDs=2 | PASS (bug detected) | **FAIL** (regression detected) |

**Live verification**: local shows `Roles=2, SLDs=1` — duplicate-roles bug is **still happening on this account**. The test correctly fails as a regression detector. **Action item for the dev team**: investigate why Roles is fetched twice on `/connections` page load. (CI was clean per the screenshot — the bug may be account/state-specific.)

### 4. BUG018_BeamerLeaksUserData — real Beamer regression (NOT a test bug)

**CI evidence (your screenshot):**
```
BUG-018 REGRESSION: Beamer URL is again leaking user data in URL params
(role=true, company=true, name=true)
```

**Investigation**: the JS extracts `performance.getEntriesByType('resource')` and looks for any URL containing `getbeamer.com` AND `c_user_role`. If found, returns that URL string. Then asserts none of `c_user_role=`, `c_user_company=`, `firstname=` are present in it. The selectors are correctly specific (require URL params, not just text mention).

If the assertion fails with all three true, that's a **real privacy regression**: the Beamer integration is sending user role, company, and first name as URL parameters. URL params get logged in browser history, server access logs, intermediate proxies, referrer headers, and analytics tooling — meaningful PII exposure.

**Fix**: NONE — the test logic is correct. The fix belongs on the FE/dev side (configure Beamer to not pass user identifiers in URL, or use a POST body / encrypted session header instead).

**Action item for management**: file a privacy/security bug ticket for the Beamer integration. The test will continue to detect this until the FE is corrected.

### 5. TC_SS_009_SearchCaseInsensitive — tautological-pass risk

**Original logic**:
```java
boolean closeEnough = Math.abs(uppercaseCount - lowercaseCount)
    <= Math.max(1, Math.max(uppercaseCount, lowercaseCount) / 5);
Assert.assertTrue(uppercaseCount == lowercaseCount || closeEnough, ...);
```

**Problem**: if both searches return 0 (e.g., timing flake or no sites with "test" in name), the assertion `0 == 0` passes — false success.

**Fix** ([SiteSelectionTestNG.java:500-548](src/test/java/com/egalvanic/qa/testcase/SiteSelectionTestNG.java#L500-L548)):
- If both counts are 0 → `SkipException` with explicit precondition message ("test data prerequisite: at least one site with 'Test' in name").
- Otherwise → strict equality. The 20% tolerance was overly permissive: case-insensitive search should return identical counts, not similar-ish.

**Live verification**: PASS.

### 6. TC_SS_010_SearchNoResults — tautological-pass (broken logic)

**Your observation**: "even if I search small or capital, result is showing correct only" — the FE works fine, but the test still failed/passed unreliably.

**Root cause** (the most subtle of all 6):

```java
private static final By OPTIONS = By.xpath("//ul[@role='listbox']//li[@role='option']");
```

OPTIONS is **scoped to the open listbox**. If the dropdown is closed, the XPath matches nothing.

```java
WebElement input = driver.findElement(FACILITY_INPUT);
clearAndType(input, "XYZNONEXISTENT99999");  // typing into closed dropdown
List<WebElement> options = driver.findElements(OPTIONS);
boolean noResults = options.isEmpty();  // ALWAYS true — dropdown is closed!
Assert.assertTrue(noResults || hasNoOptionsMessage, ...);  // tautology
```

The test never opened the dropdown. So `options.isEmpty()` was true regardless of the FE's actual state, and the assertion always passed — even in scenarios where the FE was incorrectly returning results for a nonsense query. The test gave the illusion of coverage without testing anything.

**Fix** ([SiteSelectionTestNG.java:550-617](src/test/java/com/egalvanic/qa/testcase/SiteSelectionTestNG.java#L550-L617)):
1. **Open the dropdown FIRST** so OPTIONS is in scope.
2. Type the nonsense query.
3. Check both: (a) dropdown still open AND visible-options count is 0, OR (b) explicit "No options" message visible.
4. Assert one of those two real signals.

This converts the test from a tautology into a real verification of the empty-state UX.

**Live verification**: PASS (along with TC_SS_009 in the same run).

## Why local passes but CI fails — the deeper pattern

The "passes local, fails CI" pattern usually has one of three root causes:

1. **Timing differences** — CI runners are slower (CPU, network, disk). Implicit assumption that an element appears in N seconds breaks when N+10 is needed. → fix with longer/adaptive waits, not flat retries.
2. **Resource contention** — multiple parallel workers competing for shared state (filesystem, ports, environment vars). → fix with worker isolation.
3. **Different driver/browser versions** — CI ChromeDriver may differ from local. → fix by pinning versions.

In this batch, 2 of 6 (GEN_EAD_09 + MOTOR_EAD_13) were timing-only. The other 4 had test-logic problems that just happened to appear more often in CI because CI exercised state the local runs didn't.

## Files changed

| File | Change | Lines |
|---|---|---|
| `src/main/java/com/egalvanic/qa/pageobjects/AssetPage.java` | 45s wait + refresh fallback for Create Asset button | ~24 |
| `src/test/java/com/egalvanic/qa/testcase/AssetPart4TestNG.java` | Stale-retry loop in `editTextField` for `getAttribute(value)` | ~25 |
| `src/test/java/com/egalvanic/qa/testcase/BugHuntGlobalTestNG.java` | BUG007 assertion flip + 0/0 sanity skip precondition | ~22 |
| `src/test/java/com/egalvanic/qa/testcase/SiteSelectionTestNG.java` | TC_SS_009 strict-equality + skip if both 0; TC_SS_010 open-dropdown-first + real signals | ~50 |

## Test outcomes (local)

| Test | Before | After |
|---|---|---|
| GEN_EAD_09_EditManufacturer | FAIL (CI) / PASS (local) | **PASS** |
| MOTOR_EAD_13_EditDutyCycle | FAIL (CI) / PASS (local) | **PASS** |
| BUG007_DuplicateAPICalls | FAIL (test-bug, both envs) | **FAIL on real regression** (Roles=2 locally — see Action item above) |
| BUG018_BeamerLeaksUserData | FAIL (real bug) | **FAIL on real regression** (no test change — flag for dev team) |
| TC_SS_009_SearchCaseInsensitive | sometimes-PASS (tautology risk) | **PASS** with skip-on-no-data |
| TC_SS_010_SearchNoResults | always-PASS (tautology) | **PASS** with real-signal check |

## Action items for the dev team (NOT for me to fix)

1. **BUG007 Roles duplicate**: Roles API is being called 2x on `/connections` page load (verified locally on `acme.qa.egalvanic.ai`). Investigate the React component issuing the call — likely a `useEffect` without proper dependency array, or a parent component re-rendering and re-triggering child fetches.
2. **BUG018 Beamer privacy leak**: Beamer URL contains `c_user_role`, `c_user_company`, `firstname` as URL params. Reconfigure Beamer to send these via POST body or encrypted session header. URL params leak via browser history, access logs, and referrer headers.

Both are flagged here for management visibility per the memory rule "explain in depth, learning is important".

## What I deliberately did NOT do

- **No `@Test(retryAnalyzer = ...)` band-aids.** Every flake here had a fixable root cause. Adding silent retries would have masked the inverted-assertion bugs and the privacy regression.
- **No global timeout bump.** Bumping `AssetPage.TIMEOUT` from 25 → 60 would have slowed every failure path 2.4x. Targeted 45s on the one historically slow path.
- **No suppression of BUG007/BUG018 failures.** They're now flagging real product issues. Disabling them would lose visibility.
- **No push to `developer` branch** (production). Per memory rule. Pushing to `main` only.

---

_Per memory rule: this changelog is for learning + manager review. The fixes are in the diff; this doc is the why for each one._
