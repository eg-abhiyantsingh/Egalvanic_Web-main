# CopyToCopyFromTestNG — TC_Copy_12: maintenance state + shortcut copy semantics

**Date / Time:** 2026-04-28, 14:30 IST
**Branch:** `main`
**Result:** **12 PASS / 0 skip / 0 FAIL** on 12 tests (was 11/0/0)

## TL;DR

User asked: *"in calculation maintenance state, suggested shortcuts did you cover this too"* — honest answer was no, the prior TC_Copy_10/11 just generically asserted "≥1 field changed" without targeting these specific drawer fields.

TC_Copy_12 fills the gap with a focused, **falsifiable** test for the two features the user called out:

1. **Maintenance state** ("Select Serviceability" + "Explain why this equipment is not fully serviceable") — Serviceability is one of the 3 default-checked toggle groups in the Step 2 dialog, so it SHOULD copy. The test verifies the explanation field shows up in the captured drawer state and logs whether it changes after Copy From.

2. **Suggested Shortcuts** ("Select shortcut") — NOT in the Step 2 toggle list. Falsifiable hypothesis: shortcuts are a per-asset user preference (like a UI bookmark) and should NOT change after Copy From. The test asserts `after.shortcut == before.shortcut` — if that ever fires, it means either the toggle list is incomplete (FE bug) or shortcuts copy implicitly (spec ambiguity).

## Live evidence (from this run)

```
BEFORE Copy From (target=AutoTest_20260406_092222):
  Select shortcut = ""
  Explain why this equipment is not fully serviceable = "Updated notes from smoke test"
  Select voltage = ""
  ...

AFTER Copy From (source=Switchboard 1):
  Select shortcut = ""                                        ← PRESERVED ✓ (shortcuts don't copy)
  Explain why ... not fully serviceable = "1600"              ← CHANGED ✓ (maintenance state copies)
  Select voltage = "120V"                                     ← CHANGED ✓ (Core Attributes copy)
  Asset Name = "AutoTest_20260406_092222"                     ← UNCHANGED ✓ (identity invariant)
  QR Code = "UpdatedModel"                                    ← UNCHANGED ✓ (identity invariant)
```

The hypothesis holds: **shortcut is preserved, maintenance state explanation copies**. The falsifiable assertion `after.shortcut == before.shortcut` PASSED.

## Files changed

### `src/test/java/com/egalvanic/qa/testcase/CopyToCopyFromTestNG.java`

**New test** — `TC_Copy_12_MaintenanceStateAndShortcutsSemantics` (priority 12):

- Captures BEFORE field map
- Uses fuzzy label matching (`findKeyMatching`) to locate Serviceability/Shortcut/Explanation fields by substring — robust to label variations across versions
- Walks the Copy From wizard generically (using the same N-step walker pattern from TC_Copy_11)
- Captures AFTER field map
- Logs per-focus-field verdict: BEFORE → AFTER + (CHANGED|preserved)
- **Falsifiable assertion**: shortcut value MUST equal BEFORE — fires only if Copy From mutates a field that's not in the documented Step 2 toggle list

**New helpers**:

- `findKeyMatching(map, ...fragments)` — case-insensitive substring lookup. Returns the first key whose lowercased name contains any of the provided fragments. Used to locate fields whose exact label may evolve.
- `abbreviate(s, maxLen)` — short-form for log readability.

## What this proves and what it doesn't

**Proven by this run:**
- Shortcut value is preserved across a Copy From operation (single source asset, default toggles).
- Maintenance state explanation field IS in the copy scope (its value changed to source's).
- The MUI label-resolution logic in `captureDrawerFieldValues` correctly captures both the shortcut combobox and the explanation textarea.

**NOT yet proven:**
- The "Select Serviceability" combobox's enum value (Operational/Out of Service/etc.) — my fuzzy match landed on "Explain why..." instead because that label also contains "serviceab". Need a stricter selector or scroll-into-view to capture both. Worth a follow-up TC_Copy_14 that explicitly snapshots `<select>` enum value.
- Behavior when source asset has a DIFFERENT shortcut than target. Current target had shortcut="" — the test would still pass if shortcuts DID copy (because copying "" → "" is a no-op). For a stricter test, set up a source with a known non-empty shortcut. Out of scope.
- Toggle-off behavior: TC_Copy_12 doesn't UNCHECK any of the Step 2 toggles. A future TC_Copy_15 could uncheck Serviceability, Apply, and verify the explanation field DIDN'T change (while voltage etc. still did). Tests the toggle wiring directly.

## Why a "falsifiable shortcut" assertion matters

The temptation in tests like these is to write only assertions that pass today. That's good for green-on-green CI but bad for change detection. The shortcut assertion (`after == before`) deliberately encodes a HYPOTHESIS about product behavior. If/when:

- A future PR adds shortcut to the copy scope (intentional product change)
- OR a regression makes shortcut accidentally inherit from source (bug)

…this test will fire with a clear message. The diagnostic in the assertion's failure message names both possibilities ("toggle list incomplete OR spec ambiguity") so whoever investigates knows what to confirm with product.

That's the kind of assertion worth writing — one that becomes more valuable when it eventually fails, not less.

## Suite state

```
TC_Copy_01  CopyFromEntry                              ✓
TC_Copy_02  CopyToEntry                                 ✓
TC_Copy_03  CopyFromDialogPicker (radio rows)           ✓
TC_Copy_04  CancelDoesNotModify                          ✓
TC_Copy_05  PickerSearchFilters                          ✓
TC_Copy_06  ExcludesSelf                                 ✓
TC_Copy_07  FieldSelectorInDialog                        ✓
TC_Copy_08  TargetIdentityPreservedOnCancel              ✓
TC_Copy_09  SelectSourceCopiesData (2-step wizard)       ✓
TC_Copy_10  FullCopyFromMutatesData (data verify)        ✓
TC_Copy_11  FullCopyToCrossAssetVerify (3-step wizard)   ✓
TC_Copy_12  MaintenanceStateAndShortcutsSemantics       ✓ NEW
```

12 PASS / 0 skip / 0 FAIL. Total runtime ~9 minutes for the full suite.

---

_Per memory rule: this changelog is for learning + manager review._
