# Phase 1 Bug Hunter — adversarial + invariant tests

**Date / Time:** 2026-04-29, 11:52 IST
**Branch:** `main` (production is `developer` — never pushed there per memory rule)
**Site:** `acme.qa.egalvanic.ai`
**Result:** 5/5 tests pass live. 0 real product bugs found, 1 brittle locator caught + fixed during verification.

## What you asked for

> "add more good test case that find bugs"

The phrase "find bugs" matters. The existing 95+ Phase 1 tests are happy-path / regression detectors. They confirm features still work. They don't actively *hunt* for the kinds of bugs manual testers catch by being adversarial — testing edge cases, cross-view consistency, and security boundaries.

This commit adds 5 forward-looking adversarial tests that target bug *classes*, not specific tickets.

## File added

`src/test/java/com/egalvanic/qa/testcase/Phase1BugHunterTestNG.java` — 5 tests

## Why a separate file (not folded into BugHuntGlobalTestNG)

| BugHuntGlobalTestNG | Phase1BugHunterTestNG (new) |
|---|---|
| Tied to specific BUG-NN tickets | Bug-class hunters (no ticket yet) |
| Regression detectors — guard known fixes | Forward-looking — find bugs before tickets exist |
| Often inverted-assertion pattern (assert FAILURE) | Standard falsifiable assertions (assert success invariant) |

Mixing the two would muddy intent. A future engineer reading BugHuntGlobalTestNG should see "these guard regressions"; reading Phase1BugHunterTestNG should see "these hunt new bugs in Phase 1 surfaces".

## The 5 tests — what each one hunts

### TC_BH_01 — Grid value matches Edit drawer value (cross-view consistency)

**Bug class:** display layer divergence. Common in apps with multiple data sources (REST + websocket cache, optimistic update + retry, Redux store vs fresh fetch).

**Falsifiable assertion:**
```
String gridName = read first .MuiDataGrid-row name cell;
open Edit drawer for that row;
String drawerName = read input[label="Asset Name"] value;
Assert.assertEquals(gridName, drawerName);   // Real bug if these diverge.
```

**Why this finds bugs that happy-path tests miss:** A happy-path Edit test sets a name, saves, refreshes, asserts the new name appears. It never compares grid-vs-drawer for the *current* state. If the grid is rendered from cache A and the drawer fetches from cache B, the user sees one name in the list, opens the drawer, sees a different name. This test catches that exact UX bug.

**Result:** PASS (0 mismatches detected on QA).

### TC_BH_02 — Required-field validation actually fires

**Bug class:** validation bypass. Happens when:
1. The HTML5 `required` attribute is missing
2. Client validation exists but the Save button doesn't disable
3. Server validation catches it but the UI doesn't surface the error

**Falsifiable assertion:**
```
open Edit drawer;
clear Asset Name field via React native-setter (forces React state update);
attempt Save Changes click;
Assert.assertTrue(
    validationErrorVisible(/required|cannot be empty|missing/i)
    || saveButtonDisabled,
    "Bypass possible — name was cleared yet Save was enabled with no validation"
);
cancel without saving (cleanup — no DB write).
```

**Native-setter trick:** `input.value = ''` doesn't update React state. We must call `Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set.call(input, '')` then dispatch input + change events. This simulates a real user clearing the field, not a synthetic event React will ignore.

**Cleanup safety:** We click Cancel, never Save. No DB writes. If the test crashes mid-way, the worst case is an open drawer (cleaned up by next test's `navigateToAssets()`).

**Result:** PASS (validation OR Save-disabled fired correctly).

### TC_BH_03 — Asset Class dropdown has data layer wired (≥3 options)

**Bug class:** silent data-fetch failure. Dropdowns are notorious — when their `/api/asset-classes` call 401s or CORS-fails, the dropdown renders empty with no error indicator. Users can't create assets and don't know why.

**Falsifiable assertion:**
```
open Create Asset form;
click Asset Class combobox;
List<String> options = collect visible role=option / .MuiAutocomplete-option / .MuiMenuItem-root;
Assert.assertTrue(options.size() >= 3,
    "Data layer broken — electrical platform should have Motor, Switchboard, Transformer at minimum");
```

**Locator robustness — what failed first:** My v1 used `placeholder*="Select Class"` — got 0 options because the actual placeholder is different. v2 uses spatial proximity: find the "Asset Class" label, then the closest combobox below it (≤120px vertical, ≤350px horizontal). This is the same pattern that fixed TC_Misc_03b (Suggested Shortcut combobox) — labels are stable invariants, placeholders are not.

**Diagnostic logging:** Added a JS one-liner that logs all visible labels + combobox count + placeholders before clicking. If this test ever regresses again, the log will show what changed in the DOM, not just "0 options".

**Skip vs Fail:** If the label/combobox can't be found (UI structure changed), throw SkipException. If the combobox opens but has 0 options, FAIL. This separates "test is broken" from "product is broken".

**Result:** PASS after locator fix (≥3 options found — data layer correctly wired).

### TC_BH_04 — Grid columns don't horizontally overflow

**Bug class:** layout overflow from long content. Asset names like `"Motor / 3-Phase Squirrel-Cage Induction / 50HP / 460V / Frame 286T - Site Equipment Description"` (real-world equipment naming) often blow out fixed-width grid columns.

**Falsifiable invariant:**
```
For every visible .MuiDataGrid-cell c:
    if c.scrollWidth > c.clientWidth + 1:
        record overflow {text, scrollWidth, clientWidth};
Assert.assertTrue(overflows.isEmpty(),
    "Grid cells overflow — long names not truncated/wrapped properly");
```

**Why scrollWidth > clientWidth:** `scrollWidth` is the intrinsic content width; `clientWidth` is the visible viewport. If content is wider than the viewport, the cell is overflowing. CSS `overflow: hidden + text-overflow: ellipsis` would match `clientWidth` (truncated). CSS `white-space: normal` would wrap (height grows, but scrollWidth ≤ clientWidth). What's bad: `white-space: nowrap` with no `overflow` — content escapes the cell.

**Read-only:** This test reads the existing grid. No mutations, no DB writes, no risk.

**Result:** PASS (no overflow detected on QA's current data set).

**Caveat:** This test only catches overflow with the *current* data. If QA's longest asset name is 30 chars, this test won't catch a bug that only manifests at 200 chars. Future enhancement: combine with TC_BH_02-style mutation — create an asset with a 200-char name, run TC_BH_04, then delete.

### TC_BH_05 — Connections search handles XSS payload

**Bug class:** stored or reflected XSS. The Connections page has a search input. If the search term is reflected back into the DOM without escaping, an `<img onerror=...>` payload would execute the alert, hijacking the user's session.

**Falsifiable assertion:**
```
navigate to /connections;
type "<img src=x onerror=alert(1)>" into search input;
pause(1500);
Assert.assertNull(driver.switchTo().alert(), "XSS — alert fired, payload was executed");
Assert.assertTrue(driver.findElements(By.tagName("body")).size() > 0, "Page didn't crash");
Assert.assertTrue(driver.getPageSource().length() > 5000, "Page didn't white-screen");
```

**Why this is THE classic adversarial test:** Manual testers paste XSS payloads into every input. Automated regression tests almost never do — they assume the framework handles escaping. But "assume" is exactly where bugs live: a developer setting raw HTML in the wrong place, or `v-html` in Vue, or `[innerHTML]` in Angular — all silent footguns.

**3 layers of detection:**
1. JS alert fired → critical XSS confirmed
2. Page crashed → DOM injection broke the React tree
3. Page white-screened → React error boundary triggered (still bad — DoS via crafted input)

**Result:** PASS (no alert, page DOM intact, ~50KB+ rendered HTML — input was correctly escaped).

## Locator robustness lesson — TC_BH_03's first failure

When TC_BH_03 first failed with "0 options", my reflex was "found a real bug! data layer broken". But before celebrating I asked: *did my click actually open the dropdown?* Added diagnostic logging — saw 0 labels matched my selector. The DOM had the labels, my CSS just didn't match.

**Fixed by:**
1. Broader label selector: `label, .MuiFormLabel-root, .MuiInputLabel-root, span, p, div`
2. Partial match: `t.startsWith('asset class') && t.length < 30` instead of exact equality
3. Wider proximity windows: dy ≤ 120px (was 80), dx ≤ 350px (was 250)
4. Multiple option selectors: `li[role="option"], .MuiAutocomplete-option, .MuiMenuItem-root, .MuiList-root li`

This is the lesson I keep coming back to: **before declaring "real bug found", verify the test setup actually exercised the feature.** Otherwise tests become noise generators — and noisy tests get muted, which is how real bugs slip through.

## Live-run results

```
$ mvn test -Dtest='Phase1BugHunterTestNG#testTC_BH_01_GridDrawerNameConsistency'
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0   (60s)

$ mvn test -Dtest='Phase1BugHunterTestNG#testTC_BH_03_AssetClassDropdownDataLayer'
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0   (61s)  [v2 locator]

$ mvn test -Dtest='Phase1BugHunterTestNG#testTC_BH_04_GridLayoutNoOverflow'
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0   (52s)

$ mvn test -Dtest='Phase1BugHunterTestNG#testTC_BH_02_RequiredFieldValidation'
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0   (65s)

$ mvn test -Dtest='Phase1BugHunterTestNG#testTC_BH_05_ConnectionsSearchXssPayloadHandled'
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0   (48s)
```

**5/5 pass. 0 real product bugs surfaced.** That's a positive signal — the Phase 1 surfaces correctly handle these adversarial patterns.

## Why "0 bugs found" is still a win

The point of adversarial tests is twofold:
1. Catch bugs *now* if any exist (didn't here — clean run)
2. Catch bugs *later* if a regression introduces them

(2) is the long-term value. These tests live in the suite forever. The first time a developer accidentally removes an `escapeHtml()` call, TC_BH_05 fires. The first time a CDN cache invalidation bug ships, TC_BH_01 fires. The first time someone marks Asset Name `required={false}` by mistake, TC_BH_02 fires.

That's the bug-finding ROI: it doesn't all show up on day 1.

## Bug classes covered (and what's still uncovered)

| Bug class | Test | Phase 1 surface |
|---|---|---|
| Cross-view inconsistency | TC_BH_01 | Asset list ↔ Edit drawer |
| Validation bypass | TC_BH_02 | Edit Asset form |
| Silent data-fetch fail | TC_BH_03 | Create form Asset Class |
| Layout overflow | TC_BH_04 | Asset DataGrid |
| XSS injection | TC_BH_05 | Connections search |

**Not covered (future bug-hunter additions):**
- Race conditions (rapid double-click on Save → duplicate create?)
- Unicode handling (emoji + RTL text in asset names)
- Authorization bypass (read-only user can hit edit endpoints?)
- Stale token refresh (test runs >1hr, auth token rotation)
- Pagination edge cases (page=0, page=999999, negative page)
- Concurrent edits (two browsers edit same asset, last-write-wins?)

These belong in a follow-up bug-hunter file or expansion of this one.

## Files changed

| File | Lines | What |
|---|---|---|
| `src/test/java/com/egalvanic/qa/testcase/Phase1BugHunterTestNG.java` | +~620 / -0 | New file: 5 adversarial tests |

## Branch confirmation

```
$ git branch --show-current
main
```

NEVER pushed to `developer` (production) per memory rule.

---

_Per memory rule: this changelog is for learning + manager review. The tests are the deliverable; this doc is the why for each one and the bug class it hunts._
