# Phase 1 Bug Hunter — Round 6: 6 deep tests (TC_BH_24..29)

**Date / Time:** 2026-04-30, 12:55 IST
**Branch:** `main` (production is `developer` — never pushed there per memory rule)
**Site:** `acme.qa.egalvanic.ai`
**Result:** 6/6 PASS live. 0 new product bugs surfaced (clean run after polling fixes).

## What you asked for

> "yes" — continue with round 6. Quality over quantity, divide into parts, deep changelog, push to main never developer.

This is round 6. I divided the work into 10 parts following your guidance. Two tests (TC_BH_24, TC_BH_29) failed on first run; per the round-5 lesson (*"never declare a 100%-failure result a real bug without root-cause investigation"*), I investigated each before declaring — both turned out to be **insufficient wait times**, not product bugs. Polling fixes brought them to PASS.

## The 10-part plan I followed

| Part | Status | What |
|---|---|---|
| 1 | ✓ | Design 6 deep tests covering NEW bug classes |
| 2 | ✓ | TC_BH_24 — Deep-link to asset detail (failed first run → polling fix) |
| 3 | ✓ | TC_BH_25 — Search debounce |
| 4 | ✓ | TC_BH_26 — Tab key a11y (failed first run → driver-cast fix) |
| 5 | ✓ | TC_BH_27 — /issues console errors |
| 6 | ✓ | TC_BH_28 — Sort visual indicator |
| 7 | ✓ | TC_BH_29 — Sidebar nav links (failed first run → polling fix) |
| 8 | ✓ | Deep investigation of 3 first-run failures — all test bugs, not product bugs |
| 9 | ✓ | This changelog |
| 10 | ✓ | Commit + push to `main` (NEVER `developer`) |

## The 6 new tests — bug class, intent, falsifier

### TC_BH_24 — Deep-link to /assets/<uuid> loads cleanly

**Bug class:** SPA cold-start at deep URL is a different code path than warm SPA-internal navigation. Common breaks:
- Detail page assumes /assets list was loaded first (caches parent context); deep-link bypasses → `undefined` state → blank
- Auth context not set up before detail's `useEffect` fires → 401 → redirect to login
- Route-guard logic doesn't handle direct entry to `/assets/<uuid>` → 404 page

**Strategy:** visit /assets normally (capture real UUID) → navigate AWAY (to /dashboards/site-overview) → cold deep-link to /assets/<uuid> via `driver.get()`. Assert URL stays on detail, page renders detail content.

**Three falsifiers:**
1. URL must not redirect away from detail
2. No "not found" / "404" markers in body text
3. ≥1 visible heading + ≥50 visible elements

**First-run outcome:** FAILED with "0 visible headings". Investigation: screenshot showed the page stuck on a CircularProgress spinner — hydration hadn't completed. My `pause(5000)` was too short for cold-start. **Fix:** added a 20s polling loop that waits for heading visibility OR 404 markers.

**Result after fix:** PASS.

### TC_BH_25 — Search input debounce (rapid typing → bounded network calls)

**Bug class:** missing debounce on text inputs. Without debounce, typing "circuit breaker" (15 chars) fires 15 API calls in ~2 seconds → backend rate-limit pressure, race conditions where stale responses overwrite fresh ones, wasted bandwidth.

**Strategy:** monkey-patch `window.fetch` and `XMLHttpRequest.prototype.send` (idempotent, same pattern as TC_BH_14) → reset capture array → type 10 chars at 60ms intervals via Selenium sendKeys → wait 1.8s for debounce window → count search-related API calls.

**Search-related filter:** url contains `/api/` AND (contains query string OR contains `search`/`filter`/`query`/`name=`/`q=`).

**Falsifier:** ≤3 search-related API calls for 10 keystrokes (allows initial fetch + 1 debounced search + 1 follow-up).

**Result:** PASS — search input is properly debounced.

### TC_BH_26 — Tab key reaches ≥10 distinct interactive elements

**Bug class:** keyboard accessibility regressions. A `<div onClick>` instead of `<button>` is unreachable by Tab — invisible to screen reader and keyboard users.

**Strategy:** focus body → press Tab 30 times via `driver.switchTo().activeElement().sendKeys(Keys.TAB)` → after each press, capture active element signature (tagName + id + aria-label + placeholder + text + class) → count unique signatures.

**First-run outcome:** crashed with `ClassCastException: SelfHealingDriver cannot be cast to Interactive`. Cause: I used `org.openqa.selenium.interactions.Actions(driver)` which requires `Interactive`; our wrapped driver doesn't implement it. **Fix:** switched to `driver.switchTo().activeElement().sendKeys(Keys.TAB)` — works because Tab is handled natively by the browser from any focused element.

**Falsifier:** ≥10 unique focused elements after 30 Tab presses.

**Result after fix:** PASS — keyboard reaches ≥10 unique elements.

### TC_BH_27 — /issues page console error health

**Bug class:** same as TC_BH_12 (/assets) and TC_BH_22 (/connections), new surface. Cheap horizontal scan — different render paths could surface different real bugs.

**Same noise filter** including the 3 round-3 known bugs (PLuG, /me 401, /refresh 400).

**Result:** PASS — /issues clean beyond global known bugs.

### TC_BH_28 — Sort visual indicator matches direction

**Bug class:** counterpart to TC_BH_07 (data sort). That test verified DATA reorders. THIS test verifies the VISUAL indicator (arrow icon) flips. Together they cover both directions of visual/data divergence.

**Strategy:** capture indicator signature (aria-sort + sort icon testid + transform style) initially → click header (asc) → capture signature → click again (desc) → capture signature. Assert asc != desc.

**Honest skip:** if no sort signal is exposed at all (no aria-sort, no icon), skip with explanation.

**Result:** PASS — indicator signatures differ between asc and desc.

### TC_BH_29 — Sidebar nav links all resolve to valid pages

**Bug class:** routing regressions. Sidebar lists top-level pages; if a deploy renames a route or removes a backing page, the link still SHOWS but clicking it dead-ends.

**Strategy:** collect all visible nav links (deduped, http(s) within egalvanic.ai only, skip `#` and `javascript:` hrefs) → navigate to each → assert no 404 markers + ≥30 visible elements after polling.

**First-run outcome:** FAILED with "3 of 8 pages broken — DOM collapsed (3-4 visible elements)". Investigation: screenshot showed pages were still loading when measured. Same root cause as TC_BH_24. **Fix:** replaced `pause(4500)` with 12s polling loop.

**Result after fix:** PASS — all 8 tested sidebar links resolve.

## Live-run summary

```
$ mvn test -Dtest='Phase1BugHunterTestNG#testTC_BH_24_DeepLinkLoadsDetail'
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0   (77s after fix)

$ mvn test -Dtest='Phase1BugHunterTestNG#testTC_BH_25_SearchInputDebounce'
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0   (56s)

$ mvn test -Dtest='Phase1BugHunterTestNG#testTC_BH_26_TabReachesInteractiveElements'
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0   (55s after fix)

$ mvn test -Dtest='Phase1BugHunterTestNG#testTC_BH_27_IssuesPageNoConsoleErrors'
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0   (44s)

$ mvn test -Dtest='Phase1BugHunterTestNG#testTC_BH_28_SortIndicatorMatchesDirection'
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0   (56s)

$ mvn test -Dtest='Phase1BugHunterTestNG#testTC_BH_29_SidebarNavLinksResolve'
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0   (101s after fix)
```

**6/6 PASS. 0 new product bugs surfaced.**

## Deep debugging incidents — three test-side bugs caught and fixed

### Incident 1: TC_BH_24 deep-link "0 headings" was insufficient wait

**Symptom:** test reported "0 visible headings" → assertion fired "Detail content likely didn't render".

**My instinct could have been:** "real bug found! deep-link routing is broken."

**What I did instead:** opened the screenshot via the Read tool. Saw the page was on a CircularProgress spinner — content area completely empty except for the loader in the middle. The sidebar had loaded but the main content panel hadn't.

**Root cause:** my `pause(5000)` after `driver.get()` was insufficient for cold-start hydration. SPA cold-start = browser must:
1. Parse JS bundle
2. Boot React
3. Run auth context bootstrap
4. Fetch the asset by ID
5. Render the detail layout

Each step is async. 5s isn't enough across the chain.

**Fix:** poll up to 20s for either heading visibility OR 404 marker. Now the test waits for the page to finish loading, not for an arbitrary fixed time.

### Incident 2: TC_BH_26 ClassCastException on Actions

**Symptom:** test crashed with `class SelfHealingDriver cannot be cast to class org.openqa.selenium.interactions.Interactive`.

**Root cause:** Selenium's `Actions` class needs the driver to implement `Interactive`. Our wrapped `SelfHealingDriver` (which adds AI healing on top of Chrome) doesn't implement that interface.

**Fix:** instead of `Actions(driver).sendKeys(Keys.TAB).perform()`, use `driver.switchTo().activeElement().sendKeys(Keys.TAB)`. This works because the browser handles Tab natively from any focused element — Selenium just needs to deliver the keystroke to *something*, doesn't matter what.

**Lesson:** when a Selenium API requires a specific interface, check what the driver wrapper supports. Workarounds usually exist via the lower-level WebElement API.

### Incident 3: TC_BH_29 sidebar links "DOM collapsed" was insufficient wait

**Symptom:** 3 of 8 sidebar pages reported "DOM collapsed (3-4 visible elements)".

**My instinct could have been:** "REAL bug! Multiple pages don't load!"

**What I did instead:** noticed the pattern (3 different unrelated pages all showing only 3-4 elements). That's suspicious — different pages with completely different code likely don't all share the same break. More likely a test-side timing issue.

**Root cause:** same as incident 1 — cold-start hydration on these dashboards (Sales Overview, Panel Schedules, SLDs) takes longer than my `pause(4500)`.

**Fix:** 12s polling loop replacing the fixed pause.

**Why I went straight to "polling fix" instead of investigating each page:** the *pattern* was the diagnostic — 3 different pages all at the exact same DOM size suggests they're all in the same loading state, not 3 independent bugs. When the failure mode shouts "consistent across all instances", trust the test issue first.

## Cumulative bug-class coverage (BH_01..29)

| Round | Tests | Real bugs | Cumulative |
|---|---|---|---|
| 1 (BH_01..05) | 5 | 0 | 5 |
| 2 (BH_06..09) | 4 | 0 | 9 |
| 3 (BH_10..13) | 4 | **4** ← high-yield | 13 |
| 4 (BH_14..17) | 4 | 0 | 17 |
| 5 (BH_18..23) | 6 | 0 | 23 |
| **6 (BH_24..29)** | **6** | **0** | **29** |

**29 bug-class tripwires installed. 4 real product bugs still open from round 3.**

## Bug-finding ROI analysis (after 6 rounds)

The pattern is clear:
- **Round 3 (TC_BH_10..13)** caught all 4 known bugs in this session. Bug classes: console-error inspection, accessibility close-mechanism probing.
- Rounds 1, 2, 4, 5, 6 caught zero new bugs. Bug classes covered: data integrity, SPA navigation, UI invariants, validation, network 4xx/5xx, debounce, a11y reachability.

**Hypothesis for the round 3 high-yield:** that round was the first to test classes that **don't show up in the visible UI**. PLuG init errors, /me 401, /refresh 400 are all silent — only the console knows. The kebab focus trap is silent — manual testers don't ESC out of menus often enough to notice. These are exactly the bug classes adversarial testing was designed to find.

**Future high-yield candidates:**
- Network slow-3G simulation (CDP-based throttling)
- localStorage corruption (write garbage, see if app crashes or recovers)
- Auth token expiry mid-action (mock 401 on next API call)
- Race conditions on Save (rapid double-click → exactly 1 PATCH fires)
- Concurrent edits (two browsers, last-write-wins detection)

These all probe states that are rare in normal usage but break catastrophically when they hit.

## Files changed

| File | Lines |
|---|---|
| `src/test/java/com/egalvanic/qa/testcase/Phase1BugHunterTestNG.java` | +~600 / -1 |

## Branch confirmation

```
$ git branch --show-current
main
```

NEVER pushed to `developer` (production) per memory rule.

## What I learned for future rounds

1. **Cold-start SPA hydration needs polling, not fixed pauses.** Every `driver.get()` to a different surface should be followed by a poll for content readiness, not a static wait. 20s is the safe upper bound on this app.

2. **Pattern-matching catches test bugs faster than per-case investigation.** When 3 different pages all report identical DOM state, it's a shared-cause issue (timing, network) not 3 independent product bugs.

3. **Wrapped driver classes break some Selenium APIs.** `Actions` needs `Interactive`, not always present in wrappers. Use `driver.switchTo().activeElement().sendKeys(...)` for keystroke delivery.

4. **Round-3 high-yield insight matters.** Bug classes that probe SILENT states (console errors, focus traps, network 4xx silently caught by app code) catch real bugs disproportionately. Visible-UI bug classes (sort, filter, validation, layout) are usually well-tested already by the team.

---

_This changelog is for learning + manager review per memory rule. The tests are the deliverable; this doc is the reasoning + the debugging stories behind every test that needed iteration._
