# Connection Type → Cable: dynamic CORE ATTRIBUTES verification

**Date / Time:** 2026-04-28, 17:50 IST
**Branch:** `main` (production is `developer` — never pushed there per memory rule)
**Result:** **2 PASS / 0 skip / 0 FAIL** for the two new tests added to ConnectionPart2TestNG.

## What you showed me

Screenshot of the Connections page → "+ Create Connection" → Add Connection drawer with Connection Type set to **Cable**. The CORE ATTRIBUTES section dynamically renders 6 cable-specific fields (5 required, 1 optional):

1. Length (ft) *
2. Parallel Sets *
3. Conductor Material *
4. Wire Size - N *
5. Comments
6. # of Conductors *

This is dynamic-form behavior driven by the Connection Type contract — picking a different type (e.g., Busway) should swap in a different field set.

## What was already covered (and what wasn't)

`ConnectionPart2TestNG.testCONN_081_CoreAttributesSection` (priority 73) does mention these fields, but it has two big problems:

1. **Wrong surface**: it uses the **Edit drawer** (`clickEditOnRow(0)`), not the **Add Connection drawer** that you showed.
2. **Tautological-pass**: the assertion is `logPass("BASIC INFO=" + hasBasicInfo + ", CORE ATTRIBUTES=" + hasCoreAttrs + ", fields=" + cableFields)` — there's NO `Assert.assertX` call. The test passes regardless of which fields are missing. This is the same anti-pattern I caught earlier today in TC_Misc_03 (Suggested Shortcuts) and TC_SS_010 (Search No Results) — log-only "tests" that just describe state.

So the Cable dynamic-form behavior was effectively uncovered. This commit adds two falsifiable tests.

## What I added

### `TC_CONN_081b` — Add Connection + Cable type renders all 6 specific fields

```
Open /connections
→ + Create Connection (page-level button)
→ Add Connection drawer opens
→ Select Connection Type = "Cable"
→ Snapshot drawer textContent
→ Assert: each of the 6 expected field labels is present in textContent
→ Cleanup: close drawer (no submission, no DB writes)
```

**Falsifiable assertion**: `Assert.assertTrue(missing.isEmpty(), ...)` where `missing` is a list of expected fields NOT found in the drawer's textContent. If the FE renames "Wire Size - N" to "Wire Size" (or removes "# of Conductors"), this fires immediately with the exact list of missing fields.

**Required-asterisk count**: I attempted a strict count of required-marker elements (5 of 6 fields are required), but MUI's required-indicator class names are inconsistent in this app — counting them reliably needs a custom FE-aware selector. Downgraded to informational-only logging to avoid brittleness. Assertion 1 (all 6 fields present) is the load-bearing falsifiable claim.

### `TC_CONN_081c` — Connection Type changes Core Attributes (Cable vs Busway)

The dynamic-form proof: a form that renders the SAME fields regardless of type isn't dynamic — it's broken. This test:

```
Open Add Connection drawer
→ Select "Cable" → snapshot drawer textContent
→ Select "Busway" (same drawer, different type) → snapshot again
→ Diff: count Cable-specific fields that DISAPPEAR when switching to Busway
→ Assert: at least one Cable-specific field must disappear
```

**Falsifiable assertion**: if all 5 Cable-specific fields persist into the Busway view, the FE's type-driven re-render is broken. The test fires immediately with the exact list of Cable fields that incorrectly survived the type switch.

**Honest skip**: if Busway is no longer a valid type (FE removed it OR renamed it), the test SKIPs with a clear reason rather than fake-passes. Empty == empty isn't a real signal.

## Live verification

```
$ mvn test -Dtest='ConnectionPart2TestNG#testCONN_081b...+testCONN_081c...'
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Total runtime: 90 seconds for both. Both tests open the Add Connection drawer, exercise the dropdown, snapshot textContent, close the drawer cleanly without submitting (no DB writes / no test data pollution).

## Why I switched from `<label>` selection to `textContent` matching

First attempt at TC_CONN_081b used `Array.from(d.querySelectorAll('label'))` — natural choice for form labels. **It returned only 3 labels: `[Source Node, Target Node, Connection Type *]`** — the 6 Cable Core Attribute labels were nowhere to be found.

Investigation: this app's FE doesn't always wrap form labels in semantic `<label>` tags. The CORE ATTRIBUTES section uses styled `<div>`/`<span>` elements that look like labels visually but aren't semantic labels in the DOM. The pre-existing TC_CONN_081 (priority 73) confirms this pattern by also using `drawer.textContent.includes(...)` for field detection.

**Lesson worth taking forward**: the project's FE is partially non-semantic. When a `querySelectorAll('label')` test seems to "miss" obvious labels, switch to `textContent.includes(...)` pattern matching. This costs some specificity (textContent-search has a small false-positive risk) but matches the FE's actual rendering behavior.

This is a separate, deeper issue worth flagging for the FE team:
- **A11y concern**: screen readers rely on semantic `<label>` association. If the Cable Core Attributes don't have proper `<label for=id>` or `aria-labelledby` wiring, that's a real accessibility regression.
- **Test brittleness**: this entire test suite would benefit from semantic labels — current tests rely on textContent fragility.

## Files changed

| File | Change | Lines added |
|---|---|---|
| `src/test/java/com/egalvanic/qa/testcase/ConnectionPart2TestNG.java` | Added `TC_CONN_081b` + `TC_CONN_081c` after the existing class body | ~145 |

## Test outcomes (local)

| Test | Result | What it asserts |
|---|---|---|
| TC_CONN_081 (existing, priority 73) | PASS (always) | Tautology — only logs field presence, never asserts |
| **TC_CONN_081b (new, priority 90)** | **PASS** | All 6 Cable Core Attributes present in Add drawer (falsifiable) |
| **TC_CONN_081c (new, priority 91)** | **PASS** | Switching Cable → Busway changes the field set (dynamic-form proof) |

## Action items (NOT for me to fix)

1. **A11y / semantic labels**: investigate why CORE ATTRIBUTES fields aren't wrapped in `<label>` tags. This affects screen readers and the test brittleness story. Worth filing as a low-priority FE refactor.
2. **TC_CONN_081 (existing test, priority 73)**: convert from log-only to falsifiable assertions, similar to the new b/c variants. Today's session has now caught FOUR log-only tautologies (TC_Misc_02, TC_Misc_03, TC_SS_010, TC_CONN_081) — there are likely more. Worth a sweep.

## Pattern summary across today's session

This is the FIFTH "wrong surface OR tautological-pass" issue caught today. The catalog:

| Test | Was wrong about | Fixed by |
|---|---|---|
| TC_Misc_02 (Maintenance State) | label "Maintenance State" + Calculations tab | Real label is "Condition of Maintenance (COM)" on top card |
| TC_Misc_03 (Suggested Shortcuts) | /slds page panel | Edit Asset drawer combobox |
| TC_Copy_09 (drawer kebab) | step-1 dialog title only | Step-2 title also needs recognition |
| TC_Report_01 (Generate Report) | Asset detail page kebab | /admin Settings → Forms tab |
| **TC_CONN_081b/c (Cable Core Attrs)** | **Edit drawer + log-only assertions** | **Add Connection drawer + falsifiable textContent assertions** |

Pattern: **a test that has never been live-verified at write-time is just a future failure waiting**. The fix recipe is consistent: open the page in a real browser, dump the DOM, write the selector, encode falsifiable assertions tied to real signals.

---

_Per memory rule: this changelog is for learning + manager review. The fix is in the diff; this doc is the why._
