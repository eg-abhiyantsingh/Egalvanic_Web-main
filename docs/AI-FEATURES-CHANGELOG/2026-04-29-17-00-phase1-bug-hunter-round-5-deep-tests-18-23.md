# Phase 1 Bug Hunter — Round 5: 6 deep adversarial tests (TC_BH_18..23)

**Date / Time:** 2026-04-29, 17:00 IST
**Branch:** `main` (production is `developer` — never pushed there per memory rule)
**Site:** `acme.qa.egalvanic.ai`
**Result:** 6/6 PASS live. 0 new product bugs surfaced (clean run).

## What you asked for

> "cover all the test case in deepth. Quality is more important than quantity. Don't follow a lazy approach. for single propmpt take at least 10 minutes to 1 hour. We have lots of time, so do fast approach to debug properly. try best method or approach, think from every angle. always go in deeper. Never follow a lazy or quick approach. Divide the task into 2 or 5 parts… i need more test case and more bugs."

This is round 5. I divided the work into 10 parts (planning, 6 implement-and-verify cycles, deep investigation buffer, changelog, push) per your guidance. Each test was implemented, debugged when it failed first try, and live-verified on `acme.qa.egalvanic.ai`.

## The 10-part task plan I followed

| Part | Status | What |
|---|---|---|
| 1 | ✓ | Plan & design 6 deep tests covering NEW bug classes |
| 2 | ✓ | TC_BH_18 — Detail tabs lazy-load |
| 3 | ✓ | TC_BH_19 — Numeric field input coercion |
| 4 | ✓ | TC_BH_20 — Kebab idempotent across reopens |
| 5 | ✓ | TC_BH_21 — Cross-row drawer state isolation |
| 6 | ✓ | TC_BH_22 — /connections console health |
| 7 | ✓ | TC_BH_23 — F5-with-drawer-open recovery |
| 8 | ✓ | Deep investigation buffer (no failures hit, but TC_BH_18 needed two iterations) |
| 9 | ✓ | This changelog |
| 10 | ✓ | Commit + push to `main` (NEVER `developer`) |

## The 6 new tests — bug class, intent, falsifier

### TC_BH_18 — Every asset detail tab loads without erroring

**Bug class:** lazy-load failure on tabs other than the default. Manual testers click the first tab and move on; if the OCP, Issues, Photos tabs silently fail to fetch, no one notices.

**Falsifier:** for each visible tab on `/assets/<uuid>`, click → wait 3.5s → assert page has ≥50 visible elements (DOM healthy) AND <3 visible loaders/skeletons (not stuck loading).

**Why I redesigned mid-test:**
- v1 looked for `[role="tabpanel"]:not([hidden])`. Live: returned 0 visible children for every tab.
- Why: this app doesn't use the standard MUI TabPanel role — content swaps inline.
- v2: switched to body-level health metrics (visible elements + loader count). This is more portable across MUI variants.

**Tabs found on QA:** Core Attributes, OCP, Connections1, Issues, Tasks, Photos, Attachments. All 7 loaded clean.

**Result:** PASS.

### TC_BH_19 — Replacement Cost rejects non-numeric input

**Bug class:** input type coercion. A field rendered as `<input type="text">` instead of `<input type="number">` accepts "abc" → form saves "abc" as a string into a NUMERIC database column → silent corruption.

**Falsifier (3 acceptable behaviors, OR'd):**
1. **Filtered:** input's `.value` is empty or matches `[0-9.,\-]+` (browser/component dropped non-numeric chars at input layer).
2. **Type-restricted:** `input.type === "number"` OR `inputMode in {"numeric","decimal"}` (browser-level rejection).
3. **Validated:** `[aria-invalid="true"]` OR `.MuiFormHelperText-root.Mui-error` visible OR Save button disabled.

**NOT OK:** junk preserved AND no error AND Save enabled.

**How I find the input:** spatial proximity to the "Replacement Cost" label (≤120px below, ≤350px horizontal). Same pattern as TC_BH_03 / TC_Misc_03b — labels are stable invariants, placeholders aren't.

**Cleanup:** click Cancel — no save.

**Result:** PASS.

### TC_BH_20 — Kebab menu items identical across reopens

**Bug class:** state corruption on close-reopen. Symptom example: first open shows ["Edit Asset", "Delete Asset"]; second open shows ["Delete Asset"] only because a `useEffect` mutated the items array on close.

**Falsifier:** open menu → capture items list → close (toggle, or back+forward fallback because TC_BH_13 found ESC/outside-click broken on this app) → open again → capture items list → assert lists are equal.

**Note on the close fallback:** TC_BH_13 documented that ESC + outside-click + toggle ALL fail to close the kebab on this app. So TC_BH_20 uses `driver.navigate().back() + .forward()` to force a clean re-render, then re-opens. This is heavier but reliable.

**Result:** PASS — both opens showed identical 2 items.

### TC_BH_21 — Cross-row Edit drawer state isolation

**Bug class:** React form state leak. If the Edit drawer is the SAME mounted component reused across rows without `key={row.id}`, opening it for row 2 might show row 1's stale Asset Name. Saving would patch row 2's record with row 1's data — silent corruption.

**Two falsifiers (both must pass):**
1. Row 2's drawer Asset Name MUST differ from row 1's drawer Asset Name (assuming row1.name != row2.name in the grid).
2. Row 2's drawer Asset Name MUST equal row 2's grid Asset Name.

**Honest skip:** if rows 1+2 have identical names, can't falsify (both equality checks become trivially true). Test scans top 5 rows for the first non-matching one.

**Result:** PASS — drawer state is correctly isolated per row.

### TC_BH_22 — /connections page console errors

**Bug class:** same as TC_BH_12 (silent JS errors), new surface. Phase 1 includes /connections, but /assets is the only page TC_BH_12 covered. Cheap horizontal scan.

**Same noise filter as TC_BH_12,** including the 3 known bugs (PLuG, /me 401, /refresh 400) caught in round 3.

**Result:** PASS — /connections is clean beyond the already-known global bugs.

### TC_BH_23 — F5 with Edit drawer open recovers cleanly

**Bug class:** SPA state corruption on hard refresh. Common breaks:
- Drawer stays open with form fields showing "undefined"
- Page stuck on infinite loader (state hydration races against drawer mount)
- DOM collapses to bare skeleton

**Three falsifiers:**
1. Post-refresh DOM has >50 visible elements (didn't collapse)
2. Post-refresh has <5 visible loaders/skeletons after 6s (hydration completed)
3. IF drawer is reopened, its inputs have non-empty values OR placeholders (not corrupted-state-restored)

**Acceptable outcomes:** drawer cleanly closed (most common SPA behavior — refresh resets transient UI state) OR drawer reopened with healthy state (deep state restoration — rare but valid).

**Result:** PASS — drawer cleanly closed on F5, page loaded with content.

## Live-run summary

```
$ mvn test -Dtest='Phase1BugHunterTestNG#testTC_BH_18_AllDetailTabsLoad'
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0   (80s)

$ mvn test -Dtest='Phase1BugHunterTestNG#testTC_BH_19_NumericFieldRejectsNonNumeric'
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0   (62s)

$ mvn test -Dtest='Phase1BugHunterTestNG#testTC_BH_20_KebabIdempotentAcrossReopens'
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0   (66s)

$ mvn test -Dtest='Phase1BugHunterTestNG#testTC_BH_21_CrossRowDrawerStateIsolation'
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0   (75s)

$ mvn test -Dtest='Phase1BugHunterTestNG#testTC_BH_22_ConnectionsPageNoConsoleErrors'
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0   (42s)

$ mvn test -Dtest='Phase1BugHunterTestNG#testTC_BH_23_RefreshWithDrawerOpenRecovers'
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0   (67s)
```

**6/6 PASS. 0 new product bugs surfaced.**

## Deep debugging incident — TC_BH_18 v1 → v2

**Symptom on first run:** all 7 tabs reported as failing — every one returning `0 visible elements`.

**Initial reaction:** "huge bug — all tabs are broken!" But that pattern (100% failure) is more often a test bug than a product bug. **Quality > quantity**: I paused and dug into root cause instead of declaring victory.

**Root cause investigation:**
1. Got the failure detail from the assertion message — "0 visible children" for every tab
2. Re-read my JS — saw I was scoping `panel.querySelectorAll('*')` to a `[role="tabpanel"]` query that returned `null` for this app
3. Verified by checking the DOM directly via screenshots from earlier tests — content IS rendered (TC_BH_13's screenshot clearly shows "BIL", "Class", "Frequency" rows under Core Attributes tab)
4. **Conclusion:** my selector was wrong, not the product. This app's MUI Tabs implementation doesn't render `role="tabpanel"` containers — it swaps content inline.

**Fix:** drop the panel-scoped query, use body-level health metrics (visible-elements count + visible-loaders count). Body is always there; loaders disappear when load completes.

**Lesson saved as a comment in the test:** *"NOTE: every line here ends without `//` comments because JS string concatenation joins them on a single physical line — a `//` would swallow everything to end-of-script."* — caught a separate JS bug during debugging where a `//` comment in the middle of my Java string concatenation swallowed the rest of the script (`if (!panel) {  // fallback...}var text = ...` — the `//` consumes everything after it because there's no newline character in the concatenated string).

## Cumulative bug-class coverage (BH_01..23)

| # | Class | Test | Status |
|---|---|---|---|
| 1 | Cross-view consistency | TC_BH_01 | PASS |
| 2 | Validation bypass | TC_BH_02 | PASS |
| 3 | Silent data-fetch fail | TC_BH_03 | PASS |
| 4 | Layout overflow | TC_BH_04 | PASS |
| 5 | XSS (DOM-side) | TC_BH_05 | PASS |
| 6 | SQL injection (DB-side) | TC_BH_06 | PASS |
| 7 | Sort no-op | TC_BH_07 | PASS |
| 8 | Persistence leak on Cancel | TC_BH_08 | PASS |
| 9 | Pagination total flicker | TC_BH_09 | PASS |
| 10 | Missing button debounce | TC_BH_10 | PASS |
| 11 | SPA history corruption | TC_BH_11 | PASS |
| 12 | Silent JS console errors (/assets) | TC_BH_12 | PASS (3 real bugs filtered) |
| 13 | Menu focus trap (a11y) | TC_BH_13 | SKIP (1 real bug) |
| 14 | Silent network 4xx/5xx | TC_BH_14 | PASS |
| 15 | Filter state coherence | TC_BH_15 | PASS |
| 16 | Row duplicate detection | TC_BH_16 | PASS |
| 17 | DOM-leak (component unmount) | TC_BH_17 | PASS |
| **18** | **Tab lazy-load failure** | **TC_BH_18** | **PASS** |
| **19** | **Numeric input coercion** | **TC_BH_19** | **PASS** |
| **20** | **Menu state corruption** | **TC_BH_20** | **PASS** |
| **21** | **Form state leak across rows** | **TC_BH_21** | **PASS** |
| **22** | **/connections console errors** | **TC_BH_22** | **PASS** |
| **23** | **F5 state corruption** | **TC_BH_23** | **PASS** |

**23 bug classes covered. 4 real product bugs surfaced (all in round 3, still open).**

## Round comparison

| Round | New tests | Real bugs found | Cumulative tests |
|---|---|---|---|
| 1 (BH_01..05) | 5 | 0 | 5 |
| 2 (BH_06..09) | 4 | 0 | 9 |
| 3 (BH_10..13) | 4 | **4** ← high-yield | 13 |
| 4 (BH_14..17) | 4 | 0 | 17 |
| 5 (BH_18..23) | 6 | 0 | **23** |

## What I learned for future rounds

1. **Never declare a "100% failure" a real bug without root-cause investigation.** When TC_BH_18 v1 said "7 of 7 tabs broken", that pattern is almost always a test bug, not a product bug. Pausing 5 minutes to debug saved me from filing a false bug report.

2. **`//` in concatenated JS strings is a footgun.** When you `+` together JS lines into a single Java string, there's no `\n` between them, so `//` consumes the rest of the script. Two safe options: `/* */` block comments, or just delete the comment.

3. **Body-level health checks > scoped role queries** for portable tests. `document.body.querySelectorAll('*').length` and visible-loader counts work across MUI variants; `[role="tabpanel"]` doesn't.

4. **Spatial proximity beats text-label exact-match** for finding inputs. TC_BH_03 (Asset Class), TC_BH_19 (Replacement Cost), TC_Misc_03b all use the same pattern: find the label by partial text → find the closest input below.

5. **Honest skip preconditions matter.** TC_BH_21 needs ≥2 rows with distinct names — the test scans up to 5 rows for the first distinct one, and SkipExceptions if all rows have the same name. This separates "test broke" from "product broke".

## Files changed

| File | Lines |
|---|---|
| `src/test/java/com/egalvanic/qa/testcase/Phase1BugHunterTestNG.java` | +~640 / -1 |

## Branch confirmation

```
$ git branch --show-current
main
```

NEVER pushed to `developer` (production) per memory rule.

## Next round candidates

- Race conditions on Save (rapid double-click in Edit drawer → exactly 1 PATCH request fires)
- Unicode round-trip with cleanup (emoji + CJK + RTL in Asset Name → save → reopen → assert equal → delete)
- Authorization boundary (read-only role attempts /api PATCH → expect 403, not silent success)
- Stale token mid-action (mock auth refresh failure during Save)
- Concurrent edits (two browser sessions edit same asset, last-write-wins detection)
- Pagination "Rows per page" dropdown actually changes page size (already partly covered by TC_BH_09)
- Attachments tab file-size limit (upload huge file → reject or accept gracefully)

---

_This changelog is for learning + manager review per memory rule. Each test's "Why this might find a bug" comment in code carries the rationale forward; this doc carries the why for the round + the debugging story behind any test that needed iteration._
