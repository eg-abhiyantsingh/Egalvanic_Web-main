# 2026-07-03 — Fix real automation-fault tests from latest CI runs

**Prompt:** Check latest CI/CD failed tests (Parallel Suites 1 & 2) and fix only the
real automation-fault test cases before today's full automation run (~10 min budget).

## CI state reviewed

| Suite | Latest run | Result |
|---|---|---|
| Suite 3 — API Health Check | 2026-07-03 (28659578320) | 1031 tests, 0 failures ✅ |
| Suite 2 — Curated/AI/Smoke | 2026-07-02 (28597826871) | 23 failed → rerun → **11 still failing** |
| Suite 1 — Core Regression 848 | 2026-06-30 (28458718306) | 176 failed → rerun → **34 still failing** |

## Classification of Suite 2's 11 persistent failures

- **Automation faults (fixed here): 2**
  - `ArcFlashTestNG.testAF_10_InfoTooltips` — hardcoded `tips.size()==3`; the app now
    ships a 4th metric tooltip ("Percentage of PM-required fields filled out across
    all assets") added with the PM module. Fixed: accept 3–4 tooltips, still assert
    all 3 core metric texts, and when a 4th exists assert it is the PM-required one.
  - `DeepBugVerificationTestNG.testBUG002_CSPBeamerFontsBlocked` — hard-FAILED when
    Beamer (3rd-party SDK) never loaded fonts, i.e. an unverifiable precondition.
    Fixed: `throw new SkipException(...)` instead → SKIP, and rethrow it past the
    generic `catch (Exception)` so it isn't converted back into a fail.
- **Real app findings (left failing on purpose): 9**
  - BUG001 (no 404 page), BUG003 (Issue Class validation gap)
  - HttpMethodSemantics TC_MS_01/02/04 + HttpStatusCodeContract TC_HSC_01/03/04/05 —
    all rooted in the SPA catch-all serving `index.html` (HTTP 200, text/html) for
    unknown `/api/*` paths + missing OPTIONS/Allow. Genuine API-contract defects.

## Suite 1 (06-30) 34 persistent failures — not touched

Phase4/Phase5 health-gate failures (page HUNG, ERROR banner, severe console errors)
are the destructive verifiers catching real app instability, plus a handful of
WorkOrder/Task assertion fails from a 3-day-old 7h43m congested run. None are
provable automation faults without a live repro; today's full run will re-baseline.

## Files changed

- `src/test/java/com/egalvanic/qa/testcase/ArcFlashTestNG.java` — AF_10 tooltip count 3→3..4 + PM tooltip assert
- `src/test/java/com/egalvanic/qa/testcase/DeepBugVerificationTestNG.java` — BUG002 inconclusive → SkipException (+import, + rethrow guard)

Validated: `mvn -q test-compile` clean.
