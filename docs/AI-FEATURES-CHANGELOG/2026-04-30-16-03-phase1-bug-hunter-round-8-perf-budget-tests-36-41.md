# Phase 1 Bug Hunter — Round 8: Performance/budget silent-state tests (TC_BH_36..41)

**Date / Time:** 2026-04-30, 16:03 IST
**Branch:** `main` (production is `developer` — never pushed there per memory rule)
**Site:** `acme.qa.egalvanic.ai`
**Result:** 6/6 PASS live, FIRST TRY (no debugging iterations needed). 0 new product bugs surfaced.

## What you asked for

> "yes" — continue with round 8 covering silent-state tests

Round 8 follows the round-3+7 high-yield pattern (silent state) but with a more specific lens: **performance budgets and resource budgets**. These are bugs that don't crash the app — they just make it slower / leakier / more wasteful over time.

## The 10-part plan I followed

| Part | Status | What |
|---|---|---|
| 1 | ✓ | Design 6 budget/perf silent-state tests |
| 2 | ✓ | TC_BH_36 — Duplicate API requests detection |
| 3 | ✓ | TC_BH_37 — JS heap leak across navigation |
| 4 | ✓ | TC_BH_38 — Initial DOM size budget |
| 5 | ✓ | TC_BH_39 — First Contentful Paint <3s |
| 6 | ✓ | TC_BH_40 — No mixed content (HTTP on HTTPS) |
| 7 | ✓ | TC_BH_41 — Cookie size budget |
| 8 | ✓ | All 6 PASS first try — no investigation needed |
| 9 | ✓ | This changelog |
| 10 | ✓ | Commit + push to `main` (NEVER `developer`) |

## The 6 new tests

### TC_BH_36 — Duplicate API requests on /assets load

**Bug class:** useEffect double-mount in production. React 18 StrictMode mounts effects twice in dev to catch cleanup bugs; in prod that doesn't happen, BUT mishandled dependency arrays cause the same fetch to fire twice anyway. Wasteful at best, race-condition source at worst.

**Strategy:** install fetch+XHR monkey-patch BEFORE navigating to /assets (we navigate to /dashboards/site-overview first to install the patch). Then navigate to /assets, capture all calls during first 5s. Group by URL+method (stripping query string). Anything appearing >2 times is a finding.

**Filter known dups:** `/api/auth/v2/me`, `/api/auth/v2/refresh`, `/api/connections/roles` — all three are documented in earlier rounds as known multi-call endpoints.

**Result:** PASS — no new duplicate endpoints found.

### TC_BH_37 — JS heap doesn't leak across navigation cycles

**Bug class:** event-listener leaks, lingering websockets, never-cleared `setInterval` callbacks. TC_BH_17 covered DOM-node leaks across drawer cycles; this covers heap leak across PAGE navigation cycles — different leak pattern.

**Strategy:** read `performance.memory.usedJSHeapSize` (Chromium-only) at baseline → cycle through /assets, /connections, /issues 3 times → measure heap after.

**Threshold:** 50MB growth. Generous because each page legitimately allocates memory; what we're catching is UNBOUNDED growth.

**Honest skip:** if `performance.memory` is unavailable, skip with explanation.

**Result:** PASS — heap stays bounded across nav cycles.

### TC_BH_38 — Initial DOM size budget

**Bug class:** render-the-world anti-pattern. Pagination broken (rendering all 1951 rows instead of 25), or each row's cells rendered N times due to a mapping bug.

**Strategy:** count `document.querySelectorAll('*').length`. Lighthouse flags >800 nodes as a perf concern; >1500 as strict. Our threshold: 5000 (generous, can be tightened).

**Also measured:** DOM tree depth (Lighthouse strict threshold: 32).

**Result:** PASS — DOM well within budget.

### TC_BH_39 — First Contentful Paint <3s

**Bug class:** Core Web Vital regression. Google RAIL classifies FCP:
- <1.0s = Good
- 1.0-3.0s = Needs Improvement
- >3.0s = Poor

**Strategy:** read `performance.getEntriesByType('paint')` → find `first-contentful-paint` entry → its `startTime` (ms relative to navigation start).

**Result:** PASS — FCP under 3s threshold.

### TC_BH_40 — No mixed content (HTTP on HTTPS)

**Bug class:** browser security policy violation. HTTPS page loading HTTP resources triggers browser warnings (or blocks active content). Strips secure padlock from URL bar.

**Strategy:** scan all DOM `<img src>`, `<script src>`, `<link href>`, `<iframe src>`, `<audio/video/source src>` AND `performance.getEntriesByType('resource')` URLs. Filter to `http://` (excluding localhost/127.0.0.1).

**Threshold:** 0 (hard security rule).

**Result:** PASS — all resources loaded over HTTPS.

### TC_BH_41 — Cookie size budget (<4KB)

**Bug class:** session cookie bloat. Cookies sent on EVERY same-origin request. An 8KB auth cookie × hundreds of API calls per session = serious bandwidth tax.

**Strategy:** read all cookies via WebDriver `manage().getCookies()`. Sum `name.length() + value.length() + 1` per cookie. Compare to RFC 2109 limit (4096 bytes).

**Result:** PASS — total cookie size under 4KB.

## Live-run summary

```
$ mvn test -Dtest='Phase1BugHunterTestNG#testTC_BH_36_NoDuplicateApiRequests'
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0   (50s)

$ mvn test -Dtest='Phase1BugHunterTestNG#testTC_BH_37_HeapDoesntLeakAcrossNavigation'
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0   (81s)

$ mvn test -Dtest='Phase1BugHunterTestNG#testTC_BH_38_InitialDomSizeBudget'
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0   (54s)

$ mvn test -Dtest='Phase1BugHunterTestNG#testTC_BH_39_FirstContentfulPaintFast'
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0   (45s)

$ mvn test -Dtest='Phase1BugHunterTestNG#testTC_BH_40_NoMixedContent'
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0   (53s)

$ mvn test -Dtest='Phase1BugHunterTestNG#testTC_BH_41_CookieSizeBudget'
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0   (52s)
```

**6/6 PASS first try. 0 debugging iterations needed.**

## Cumulative tally (rounds 1-8)

| Round | Tests | Real bugs | Theme | Cumulative |
|---|---|---|---|---|
| 1 (BH_01..05) | 5 | 0 | Visible UI invariants | 5 |
| 2 (BH_06..09) | 4 | 0 | UI behavior + injection | 9 |
| 3 (BH_10..13) | 4 | **4** ← peak | Silent (console + a11y) | 13 |
| 4 (BH_14..17) | 4 | 0 | Network + DOM leak | 17 |
| 5 (BH_18..23) | 6 | 0 | Tabs + state isolation | 23 |
| 6 (BH_24..29) | 6 | 0 | Nav + a11y reach | 29 |
| 7 (BH_30..35) | 6 | **3** | Silent (perf SLA) | 35 |
| **8 (BH_36..41)** | **6** | **0** | **Silent (perf budget)** | **41** |

**41 bug-class tripwires installed. 7 real product bugs still open** (4 from round 3 + 3 from round 7).

## Why no new bugs in round 8?

Three plausible reasons:
1. **The app is genuinely well-engineered on these dimensions.** The Egalvanic team likely runs Lighthouse / Core Web Vitals already, and these specific budgets are within industry norms for a complex SPA.
2. **The thresholds I chose were generous.** Heap 50MB, DOM 5000 nodes, FCP 3s, cookies 4KB — all permissive enough that only severe regressions trip them. Tighter thresholds might surface more findings, but at the cost of false positives.
3. **Diminishing returns.** Rounds 1-7 covered 35 bug classes. The "easy" silent-state bugs (those a single test can catch) get harder to find as the test suite expands.

This is expected behavior for a maturing test suite. The VALUE of round 8's tests is in **future regression detection**, not initial bug discovery. If a future deploy adds a new render-blocking script (FCP regresses), or removes a useEffect cleanup (heap leaks), or adds an HTTP-only CDN reference (mixed content), the relevant test will catch it on the next CI run.

## All real product bugs across 8 rounds (file 7 ZP tickets)

| # | Bug | Severity | Round | Test |
|---|---|---|---|---|
| 1 | PLuG init auth error logged 3x per page | Medium | 3 | TC_BH_12 |
| 2 | `/api/auth/v2/me` → 401 every page load | Medium | 3 | TC_BH_12 |
| 3 | `/api/auth/v2/refresh` → 400 every page load | Medium | 3 | TC_BH_12 |
| 4 | Kebab menu doesn't close on ESC/click/toggle | **HIGH** | 3 | TC_BH_13 |
| 5 | `/api/sld/<uuid>` 7.6s response | Medium (perf) | 7 | TC_BH_33 |
| 6 | `/api/node_classes/user/*` 6.5s response | Medium (perf) | 7 | TC_BH_33 |
| 7 | `/api/lookup/nodes/<uuid>` 7.1s response | Medium (perf) | 7 | TC_BH_33 |

## Files changed

| File | Lines |
|---|---|
| `src/test/java/com/egalvanic/qa/testcase/Phase1BugHunterTestNG.java` | +~570 / -1 |

## Branch confirmation

```
$ git branch --show-current
main
```

NEVER pushed to `developer` (production) per memory rule.

## What I learned for future rounds (cumulative across 8 rounds)

1. **Silent-state high-yield is a finite well.** Round 3 found 4, round 7 found 3, round 8 found 0. The classic silent-state bug classes (console errors, network 4xx/slow, focus traps) on this app are now well-covered.

2. **Performance budget tests are best evaluated by their long-term value, not first-run bug count.** Round 8 caught 0 today but will catch any future regression that pushes the app over a budget. That's a different kind of value than "find a bug today" — it's "prevent a bug 3 months from now."

3. **Browser performance APIs are the right primitive.** `performance.memory`, `performance.getEntriesByType('paint'/'resource')`, `document.cookie`, WebDriver `getCookies()` — all give precise numerical falsifiers.

4. **Pre-installation of monkey-patches matters for cold-load tests.** TC_BH_36 navigates to a different page first, installs the fetch monkey-patch, THEN navigates to /assets. Trying to install the patch ON /assets means the initial /assets requests have already fired and aren't captured.

5. **The strongest tests have a single clear falsifier.** TC_BH_39 (FCP <3000ms), TC_BH_41 (cookies <4096B), TC_BH_40 (mixed-content count = 0). One number, one comparison, no ambiguity.

## Future round candidates

Silent-state classes I haven't yet probed:
- **WebSocket disconnect/reconnect** — mock socket close, verify reconnect + state recovery
- **Cache-poisoning resilience** — mock conflicting payloads from different tabs
- **Authorization boundary** — read-only role attempts write endpoints (silent 403 vs silent success)
- **Slow-3G simulation** — CDP throttling, verify graceful degradation
- **Deep CDP heap snapshots** — instead of just `performance.memory`, take real heap snapshots and diff for retained objects
- **Service worker lifecycle** — if the app uses one, verify cache invalidation
- **Error boundary smoke test** — throw deliberate JS error, verify error boundary catches it gracefully

These are progressively more elaborate; the first 3 (websocket, cache, authz) are the most likely to find real bugs.

---

_This changelog is for learning + manager review per memory rule. Round 8's clean run reflects a maturing test suite — the bug-discovery curve naturally flattens as coverage grows. The tests' value is now primarily in regression prevention._
