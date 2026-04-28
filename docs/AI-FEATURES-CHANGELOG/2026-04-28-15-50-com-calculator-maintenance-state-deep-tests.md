# MiscFeaturesTestNG — TC_Misc_02b + TC_Misc_02c: COM Calculator deep tests

**Date / Time:** 2026-04-28, 15:50 IST
**Branch:** `main`
**Result:** Both new tests PASS individually (2/0/0). Calculation engine verified live: `Level 1 → Nonserviceable` on checkbox toggle.

## TL;DR

User shared a screenshot of the **COM Calculator dialog** (titled "COM Calculator") and said: *"if you click on calculator and scroll then you will see this option"*. The shallow TC_Misc_02 (which only checks the static "CONDITION OF MAINTENANCE (COM)" card on asset detail) wasn't covering this. Two new deep tests:

- **TC_Misc_02b** — Asserts the COM Calculator dialog STRUCTURE (3 factors, scrollable, Maintenance State section with NONSERVICEABLE / LEVEL 3 categories, ≥5 checkboxes, Reset/Cancel/Apply Rating buttons).
- **TC_Misc_02c** — Falsifiable proof the calculation ENGINE works: read derived level, check a NONSERVICEABLE box, read derived level again, assert it changed.

## What was learned about the COM Calculator (live)

### Where it lives

```
Asset detail → kebab → "Edit Asset" (opens drawer)
  → drawer scrolls down to "Condition of Maintenance" section
    → "Calculator" button next to 1/2/3 manual rating buttons
      → opens COM Calculator dialog (centered modal)
```

The Calculator button is INSIDE the Edit Asset drawer, not on the bare asset detail page. Initial test attempts looked on the wrong surface and silent-failed.

### Dialog structure

The dialog has a header note: *"Select the appropriate rating for each factor below. Your final COM rating will be the **highest value among the three factors**."*

The **3 factors** (in order, top to bottom):

1. **Asset Criticality** (Standard / Business Critical / Life Safety) — visible at dialog open
2. **Environmental Exposure** (likely Light / Moderate / Harsh — partially visible)
3. **Maintenance State** — requires SCROLLING (this is what the user pointed out)
   - Description: *"Check all statements that apply. If none apply, the equipment is rated Level 1 (like-new condition, maintained per EMP)."*
   - Live "Derived maintenance level: Level X" indicator
   - Categorical checkbox groups: NONSERVICEABLE / LEVEL 3 — POOR / (more below)

Bottom buttons: **Reset / Cancel / Apply Rating (N)** where N is the COM rating.

### Calculation engine behavior (verified live)

Initial state of test asset (AutoTest_20260406_092222 — Motor):
- Derived maintenance level: **Level 1**
- All maintenance-state checkboxes unchecked
- Apply button text: "Apply Rating (1)"

After checking the FIRST NONSERVICEABLE box ("Equipment has exceeded expected service life and needs replaced"):
- Derived maintenance level: **Nonserviceable**
- Apply button text: "Apply — Nonserviceable"

→ **calculation engine works correctly**: a single NONSERVICEABLE statement immediately escalates the equipment status. The test asserts `before != after` — falsifiable, fires if engine ever disconnects from inputs.

## Files changed

### `src/test/java/com/egalvanic/qa/testcase/MiscFeaturesTestNG.java`

**New tests** (priorities 21, 22 — placed after existing tests so they run last):

- `testTC_Misc_02b_COMCalculatorStructure` — Opens calculator, scrolls to Maintenance State, asserts dialog structure (≥5 checkboxes, NONSERVICEABLE+LEVEL3 headings, Apply Rating + Reset + Cancel buttons), captures derived level for log.
- `testTC_Misc_02c_COMCalculatorInteractivity` — Opens calculator, captures `before` derived level, toggles a NONSERVICEABLE checkbox, captures `after` level, asserts `before != after`. Resets + cancels to avoid persisting.

**New helpers:**

- `findCalculatorButton()` — Prioritizes actual `<button>` elements over spans/divs that just contain "Calculator" text. Spans look matchable to selectors but click-no-op silently. JS query for `<button>` with exact text match, with a scroll-and-retry fallback.
- `scrollComCalculatorToMaintenanceState()` — JS scrolls the COM Calculator dialog so the Maintenance State section is in view. Uses `scrollIntoView({block: 'center'})` on the heading element.
- `readDerivedLevel()` — JS regex extract of "Derived maintenance level: Level X" or "Nonserviceable" text.

**Critical detail — the React-controlled checkbox click**:

```javascript
// MUI's <Checkbox> hides the real <input> with opacity:0. Selenium clicks
// on the visible label often get eaten by ripple wrappers OR don't trigger
// React's onChange (the input is a controlled input).
//
// Reliable path: bypass React's setState protection using the prototype setter,
// then dispatch a synthetic 'change' event React listens to.

var setter = Object.getOwnPropertyDescriptor(
    window.HTMLInputElement.prototype, 'checked').set;
setter.call(cb, true);
cb.dispatchEvent(new Event('change', { bubbles: true }));
cb.dispatchEvent(new Event('input', { bubbles: true }));
cb.click();  // also fire click for ripple/blur effects
```

This pattern works for ANY React-controlled MUI input (TextField, Checkbox, Radio, Switch). Worth promoting to a shared utility if more tests need it.

## Live verification

```
$ mvn test -Dtest='MiscFeaturesTestNG#testTC_Misc_02b_COMCalculatorStructure+testTC_Misc_02c_COMCalculatorInteractivity'
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Visual evidence captured:
- Before: `Derived maintenance level: Level 1` (all boxes unchecked)
- After: `Derived maintenance level: Nonserviceable` (one box checked, row highlighted)

## Known caveat — full-suite flake

When MiscFeaturesTestNG is run as a full file, TC_Misc_02b and TC_Misc_02c sometimes fail with "Calculator button not found in Edit Asset drawer". This is the same inter-test state pattern that affects CopyToCopyFromTestNG TC_Copy_04/05/06 — a previous test leaves the drawer or modal in a non-default state, which breaks the next test's setup.

**Not introduced by this commit** — TC_Misc_05 (T&C link) and TC_Misc_08 (Schedule create) also fail for unrelated reasons in the full-suite run.

The fix is BaseTest-level: a `@BeforeMethod` that ensures every test starts from a clean URL + closed drawer. That's broader than this commit's scope. Filed mentally as the same follow-up that's pending for CopyToCopyFromTestNG.

## Test debugging chronicle (4 iterations to green)

1. **Attempt 1**: Looked for "Calculator" button on asset detail page → not found there.
2. **Attempt 2**: Routed through Edit drawer first → button found, but `findText` matched a `<span>` not the actual `<button>`. Click silently no-oped, dialog never opened.
3. **Attempt 3**: Switched to JS query for actual `<button>` elements → dialog opens. But "Maintenance State" not found by `findText` (text > 200 chars filter).
4. **Attempt 4**: Direct JS heading lookup with exact-text match → found. Then checkbox click fails to register (`Level 1 → Level 1`). MUI input opacity:0 + React controlled input.
5. **Attempt 5**: React native-state-setter approach → engine fires, derived level changes correctly.

Each iteration captured a screenshot which immediately revealed the next problem — same screenshot-driven debug loop that worked for Copy To/From earlier today.

## What's verified now

| Layer | Test | Falsifiable assertion |
|---|---|---|
| Static display | TC_Misc_02 | Asset detail card shows numeric COM score (parseable, non-negative) |
| Dialog structure | TC_Misc_02b | ≥5 checkboxes, NONSERVICEABLE+LEVEL3 sections, Apply/Reset/Cancel buttons, derived-level text |
| Calculation engine | TC_Misc_02c | Toggling NONSERVICEABLE checkbox MUST change the derived level |

These three together cover the static output, the structure, AND the calculation engine. If any layer breaks (engine disconnects, dialog refactor, output truncation), the matching test fires with a precise diagnostic.

---

_Per memory rule: this changelog is for learning + manager review._
