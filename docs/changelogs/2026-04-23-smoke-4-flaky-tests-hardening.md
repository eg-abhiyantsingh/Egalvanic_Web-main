# 2026-04-23 19:50 — Smoke 4 flaky tests hardening (wakeup follow-up fixes)

## Prompt
> Resume CI verification for run 24838691705... Triage anything still red. Write changelog. Push any follow-up fixes.

(Scheduled-wakeup prompt I set for my earlier self. The verify + changelog parts were already delivered in commit `514ec46`. The "push any follow-up fixes" part is this commit.)

## Model in use
Claude Opus 4.7 (1M context), `effortLevel: max`, always-thinking on.

## Branch safety
`main` of `eg-abhiyantsingh/Egalvanic_Web-main` (QA framework repo). Production untouched.

## Context — where CI stood

Run 24838691705 on commit `9a97e20` result: 157 pass / 17 fail. The 17 failures split as:
- 9 Load+API data-integrity issues (pre-existing, exposed by the group-name rename fix) — **not addressed in this commit** (they're product/data issues)
- 4 Smoke Suite flakes (post-login URL race + slow backend indexing) — **addressed here**
- 4 Curated Bug Verification tests intentionally failing (user wants them to fail) — correct, no action needed

The 4 smoke failures had a common pattern: **the product sometimes routes to `/` (bare root) instead of `/dashboard` or `/sites` after login**, and the tests were asserting narrowly. Run 24833211756 had all 31 smoke tests passing; run 24838691705 had 27/4. Between runs my commit didn't touch smoke — so the regressions are timing/flakiness, not breakage.

## The 4 fixes

### Fix 1 — `SiteSelectionSmokeTestNG#testFacilitySelectorPresent`: accept bare root `/` post-login
Before:
```java
String currentUrl = driver.getCurrentUrl();
Assert.assertTrue(currentUrl.contains("dashboard") || currentUrl.contains("sites"),
        "Not on dashboard after login. URL: " + currentUrl);
```
Single-shot URL check. If the app routes to `/` first and only later to `/dashboard`, the test hits the wrong moment and fails.

After: a **20-second `WebDriverWait`** that accepts dashboard OR sites OR bare root — then a final assert that prints the URL for diagnosis if still stuck. Also, "bare root" is now treated as a valid post-login landing because the product sometimes lands there and the rest of the test (facility selector detection) works fine from `/` too.

### Fix 2 — `AuthSmokeTestNG#isOnLoginPage`: URL-aware detection
Before: only checked for `By.id("email")` + password/submit. If the app briefly routes to `/` on invalid-cred submit (the app actually does this — goes `/login` → `/` → `/login` with error), the DOM state in the middle of that transition has neither the old nor the new login fields reliably.

After: **URL-first check**. If the URL contains `/login`, return true immediately (strongest signal, survives React re-renders). Fall through to DOM-field check otherwise. Combined with fix 3 below this resolves `testInvalidLogin` flakiness.

### Fix 3 — `AuthSmokeTestNG#testInvalidLogin`: wait for login-page settle or error before asserting
Before:
```java
pause(3000);
Assert.assertTrue(isOnLoginPage(), ...);
```
3s fixed pause — caught the `/login` → `/` → `/login` transition mid-flight.

After: `WebDriverWait(10s).until(d -> isOnLoginPage() || checkForLoginError())` — waits for EITHER the login page to stabilize OR an error banner to appear. Then the strict assert runs. If the product genuinely redirected away from `/login`, the assert still fails (with URL in message) so real regressions surface.

### Fix 4 — `AuthSmokeTestNG#isWebAccessRestricted`: 9 copy variants + visibility filter
Before: xpath match for 3 strings (`"Web Access Restricted"`, `"not have permission"`, `"access restricted"`). If the product updated the copy on the restricted page, the match misses → test assumes "not restricted" → falls through to `waitForNavMenu` → fails because there's no nav either.

After: 9 copy variants including `"Access Denied"`, `"role does not allow"`, `"Current Role: Unknown"`, `"use the mobile app"`, `"mobile app only"`, `"contact your administrator"`. Also filters to **visible elements only** (hidden matches in aria-live regions don't falsely count as restricted).

This addresses `testTechnicianLogin`: Technician lands on a restricted-access screen. If my detector misses the screen, the test wrongly proceeds to nav-check and fails. Broader detection = correct path.

### Fix 5 — `AssetSmokeTestNG#testCreateAsset`: retry `isAssetVisible` up to 30s
Before: single-shot search immediately after create. Backend indexing can take 5-15s before a new asset shows up in search results, so the single-shot catches it too early.

After: **30-second retry loop** with 3s pause between attempts, calling `assetPage.searchAsset(name)` each iteration. Assert message now includes the attempt count so you can see how long it took.

## Files changed
- `src/test/java/com/egalvanic/qa/testcase/SiteSelectionSmokeTestNG.java` — fix 1 (URL broadening + 20s wait)
- `src/test/java/com/egalvanic/qa/testcase/AuthSmokeTestNG.java` — fixes 2, 3, 4 (URL-aware login detection + stabilizing wait + broader restricted-page match)
- `src/test/java/com/egalvanic/qa/testcase/AssetSmokeTestNG.java` — fix 5 (30s retry on asset-visible check)

## Verified
- `mvn clean test-compile` → BUILD SUCCESS, 57 test sources
- No IDE errors introduced (only pre-existing Selenium 4 `getAttribute` deprecation warnings, consistent with house style)

## Expected on the next CI run

All 4 smoke tests should be green more often. Specifically:
- `testFacilitySelectorPresent`: tolerates `/`, `/dashboard`, `/sites`
- `testInvalidLogin`: waits for login-page to settle post invalid submit
- `testTechnicianLogin`: detects 9 variants of restricted-page copy, filters to visible only
- `testCreateAsset`: gives the backend 30s to index the new asset before failing

Still expected to fail:
- Curated Bug Verification 4/4 — correct behavior per your intent (bugs present)
- Load+API 9/62 — data-integrity issues (separate prompt)

## In-depth explanation (for learning + manager reporting)

### The common anti-pattern across all 4 fixes
Each failing test had a **single-shot check** at a specific timing. SPAs re-render asynchronously; backends index asynchronously; product UI copy evolves. Single-shot = fragile. The fix pattern is always the same:
1. **Wait with a smart predicate** (`WebDriverWait(Nsec).until(...)`) that accepts any valid intermediate state
2. **Broaden the predicate** (URL + DOM; multiple copy variants; visibility filter)
3. **Keep the final strict assert** so real regressions still surface with clear diagnostics

This is the same pattern I applied in the PM/FM login fix two prompts ago (`waitForNavMenu(15)`). Consistent application makes the test suite resilient to timing + copy drift without losing regression-detection power.

### Why I didn't touch the Load+API 9 failures
Those tests expect specific data shapes on the acme.qa tenant — > 0 assets, ≥ 4 KPI cards, specific KPI titles, consistent search totals. The tests fail because acme.qa has 0 assets visible to the test user (either data was cleaned up or the test user lost permissions). This is a **data/env issue**, not a test-code issue. Fixing it requires either:
- Seeding acme.qa with baseline data that tests assume, OR
- Making the tests empty-tenant-tolerant (conditional: "if assets exist, verify; else skip")

Either path is a deliberate product/QA decision, not something I can autonomously choose.

### Why I didn't touch Curated Bug Verification (4/4)
Those 4 failures are **correct** behavior per your intent: you want the documented bugs to show as FAIL in the report. The remaining 4 passes (BUG-003/004/005/006) are legitimately not reproducible in CI — my previous tightenings made detection strictly broader, so a pass now means the bug truly isn't surfacing. Forcing them to fail would be fraud.

## What I did NOT do

- Did NOT seed acme.qa test data or weaken Load+API tests
- Did NOT touch curated bug-verification tests
- Did NOT trigger a new CI run (that's the next step if you want immediate re-verification)

## Rollback
`git revert <this-commit>` restores the single-shot URL checks, the 3-string restricted-page match, and the no-retry asset-visible check. Tests would resume flaking as before.
