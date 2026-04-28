# CopyToCopyFromTestNG — drawer kebab found via aria-label="Copy Details"

**Date / Time:** 2026-04-28, 13:15 IST
**Branch:** `main`
**Result:** 5 PASS / 3 honest-skip / 0 FAIL (was 0 PASS / 0 skip / 8 FAIL before)

## TL;DR

User pointed out (via screenshot) that the "Copy Details From..." / "Copy Details To..." feature DOES exist — it lives inside the Edit Asset drawer's 3-dot kebab menu, not on the asset detail page. The 3 ready-bug files I generated yesterday were **false alarms** caused by the test looking in the wrong place.

Fixed by:
1. Routing through `assetPage.clickKebabMenuItem("Edit Asset")` (the established helper) to open the Edit drawer.
2. Targeting `button[aria-label="Copy Details"]` in the drawer header to open the second-level menu.
3. Searching for menu items by the actual labels: `Copy Details From` / `Copy Details To`.

Stale ready-bug files (3 from yesterday + 6 intermediate from today's selector debugging) deleted.

## What I learned mid-debug — the diagnostic dump that cracked it

The drawer's kebab is **not** a generic `MoreVertIcon`. Its SVG path is the three-dots shape, but the team gave it `aria-label="Copy Details"` instead of `aria-label="more"`. Hunting by shape (`data-testid*=MoreVert`) silently missed it. I added a one-shot JS dump of every right-side header icon and the result was conclusive:

```
[CopyTest] Right-side header icons:
  {aria=,             pathStart=,                                       x=1114, y=11}   // close X (page level?)
  {aria=,             pathStart=M17.65 6.35... (refresh),               x=1240, y=82}   // page-level Refresh
  {aria=,             pathStart=m12 8-6 6 ... (chevron up),             x=1278, y=82}   // page-level chevron
  {aria=,             pathStart=M12 8c1.1 0 2-.9 2-2... (3 dots),       x=1316, y=82}   // page-level MoreVert
  {aria=Copy Details, pathStart=M12 8c1.1 0 2-.9 2-2... (3 dots),       x=1240, y=15}   // ★ DRAWER kebab
  {aria=Refresh,      pathStart=M17.65 6.35... (refresh),               x=1278, y=15}   // drawer Refresh
  {aria=,             pathStart=M19 6.41 17.59 5 ... (close X),         x=1316, y=15}   // drawer close
```

`y=15` is the drawer header (overlay layer), `y=82` is the page header behind it. The drawer kebab carries a **semantic** aria-label (named after its function "Copy Details") rather than a generic shape-based one. That's a UX-friendly choice by the FE team but it breaks shape-based selectors.

Lesson written into memory mentally: **always search aria-label by intent, not just by icon shape**.

## Files changed

### `src/test/java/com/egalvanic/qa/testcase/CopyToCopyFromTestNG.java`

- `openDrawerKebab()` now uses CSS `button[aria-label='Copy Details']` as Strategy 1 (verified live), with a permissive fallback for resilience.
- Removed the diagnostic JS dump now that we know the answer.
- Doc comment records the live-verified positions: Copy Details (x=1240), Refresh (x=1278), Close (x=1316), all at y≈15.

### `ready-bug/` — deleted 9 stale files

```
2026-04-27-19-32-...TC_Copy_01...   (yesterday — wrong selector)
2026-04-27-20-11-...TC_Copy_02...   (yesterday — wrong selector)
2026-04-27-20-38-...TC_Copy_03...   (yesterday — wrong selector)
2026-04-28-12-37 → 12-46 (×6)        (today — selector iterations)
```

These were all "feature missing" false alarms. The feature exists; I was looking in the wrong DOM node.

`ready-bug/README.md` preserved.

## Live verification

```
$ mvn test -Dtest='CopyToCopyFromTestNG#testTC_Copy_01_CopyFromEntry+testTC_Copy_02_CopyToEntry'
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS

$ mvn test -Dtest='CopyToCopyFromTestNG'
Tests run: 8, Failures: 0, Errors: 0, Skipped: 3
[INFO] BUILD SUCCESS
```

The 3 skips are the honest-skip pattern from commit `af41ef6` (when full coverage needs a 2nd asset / a populated source record we can't guarantee). They were never bugs — better to skip with a clear reason than fake-pass.

## Why this matters for the AI bug-detection pipeline

`SmartBugDetector` + `ReadyBugGenerator` now correctly produce ZERO files for this test class. Yesterday they produced 3 false bugs because the test (the *test*, not the product) was wrong. This reinforces a design principle: **AI-generated bug reports are downstream of test correctness**. A flaky or mis-scoped test produces flaky or false-positive AI reports. Triage discipline goes:

1. Did the test fail because the product is broken? → real bug report.
2. Did the test fail because the test looks in the wrong place / uses the wrong label? → fix the test, no bug report.
3. Did the test fail because of infrastructure (timeout, missing test data)? → ENVIRONMENT_ISSUE, no bug report.

`SmartBugDetector` already classifies into REAL_BUG / FLAKY_TEST / LOCATOR_CHANGE / ENVIRONMENT_ISSUE — the right classification here would have been LOCATOR_CHANGE (or "TEST_LOGIC_ERROR" if we add that bucket). Worth considering adding that 5th classification.

## Follow-ups (for memory, not this commit)

- Consider adding `TEST_LOGIC_ERROR` as a 5th classification in `SmartBugDetector` for cases like this (test looks in wrong place / uses wrong label / wrong page object call).
- Consider auto-suppressing ready-bug generation when the same test+failure-fingerprint has fired N+ times in a row WITHOUT a corresponding product change (probably the test is wrong).

---

_Per memory rule: this changelog is for learning + manager review. The fix is the diff; this doc is the why._
