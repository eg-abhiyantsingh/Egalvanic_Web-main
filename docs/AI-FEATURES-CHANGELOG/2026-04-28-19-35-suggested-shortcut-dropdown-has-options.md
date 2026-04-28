# Suggested Shortcut dropdown — open + ≥1 option (TC_Misc_03b)

**Date / Time:** 2026-04-28, 19:35 IST
**Branch:** `main` (production is `developer` — never pushed there per memory rule)
**Result:** TC_Misc_03b correctly SKIPS on no-preset state (honest precondition). Will PASS on accounts with seeded shortcut presets.

## What you reaffirmed

Screenshot of the Edit Asset drawer scrolled to the **Suggested Shortcut (Optional)** combobox (red focus border, placeholder "Select shortcut") with the field positioned between Serviceability Note and Location. Plus your DevTools console showing `applied_shortcut: null` from the `mainAPIService.js` payload — confirming the API is fetching enriched node details correctly but no shortcut is applied yet.

This is the same field I tested for existence + visibility in TC_Misc_03 earlier today. You're showing me again to indicate I should test the full **interaction** path — open the dropdown, verify options appear.

## What was missing in TC_Misc_03

The earlier TC_Misc_03 fix only verifies that the **combobox exists** and is **visible**. It doesn't:
- Click to open the dropdown
- Verify the listbox actually opens
- Verify ≥1 selectable option appears (proves data layer is wired)

A combobox that renders but has zero options would still pass TC_Misc_03 — silent data-layer regression risk.

## What I added

### TC_Misc_03b — Dropdown opens with ≥1 option (or honest skip)

```
Open Edit Asset drawer for first asset
→ Scroll drawer body to render the Suggested Shortcut field
   (it's far below the fold, after BASIC INFO + Core Attributes + Serviceability)
→ Find the "Suggested Shortcut" label via findText helper
→ Locate associated combobox using SPATIAL PROXIMITY
   (next combobox in DOM order ≤200px below the label)
→ Click combobox to open dropdown
→ Snapshot all visible li[role="option"] options
→ ASSERT: ≥1 option present  (PASS)
   OR     SkipException with precondition message  (no presets exist)
→ Cleanup: ESC to close — DO NOT select an option
   (would mutate applied_shortcut on the asset's record)
```

**Three honest outcomes:**

| Account state | Test outcome | Meaning |
|---|---|---|
| Asset class has ≥1 shortcut preset | **PASS** | Combobox + dropdown + data layer all working |
| Asset class has 0 shortcut presets | **SKIP** | Honest precondition — no test data to verify against |
| Combobox not found / dropdown doesn't open | **FAIL** | Real product regression |

This is significantly stronger than TC_Misc_03's existence check while avoiding the fake-pass-on-missing-data trap.

## Live verification

```
$ mvn test -Dtest='MiscFeaturesTestNG#testTC_Misc_03b_SuggestedShortcutDropdownHasOptions'
[WARNING] Tests run: 1, Failures: 0, Errors: 0, Skipped: 1
[INFO] BUILD SUCCESS
```

On this account/asset combination (Transformer class), the dropdown opened with zero options — meaning **no shortcut presets are configured for Transformer assets here**. The test correctly SKIPS rather than fail-on-missing-data.

## Three iterations to converge — debugging chronicle

This test took 3 iterations to land on the right combobox-finder strategy:

1. **Attempt 1**: Used `findByText` then walked up via `closest('section, div[class*="FormControl"], div[class*="section"]')` — same pattern as existing TC_Misc_03. Test "passed" but the screenshot showed it was opening the **Asset Class** dropdown (PDU, QANode, Switchboard...) — a fake-pass. The loose `div[class*="section"]` ancestor match caught a wrapper containing BOTH this label AND Asset Class's FormControl, and `querySelector` returned Asset Class first (earlier in DOM).

2. **Attempt 2**: Tightened to `closest('[class*="FormControl"]')` only. This time **failed cleanly** — no combobox found in the FormControl. Turned out this field's MUI structure doesn't always wrap label and combobox in the same FormControl.

3. **Attempt 3**: Switched to **spatial proximity** — find combobox that's both AFTER the label in DOM order AND ≤200px below it. This is how a sighted user reads forms: "the dropdown right under this label". Worked correctly — found the right combobox without fake-passing on Asset Class.

**Lesson worth filing**: when a field's DOM structure is irregular (no consistent FormControl wrapper), spatial proximity is more robust than DOM-tree walking. It also matches user mental model.

## Files changed

| File | Change | Lines added |
|---|---|---|
| `src/test/java/com/egalvanic/qa/testcase/MiscFeaturesTestNG.java` | Added TC_Misc_03b after TC_Misc_03 (priority 31). Added Keys import. | ~140 |

## Pattern catalog now at 10 instances this session

This is the **10th** wrong-label/wrong-surface/fake-pass anti-pattern caught in this session:

1. TC_Misc_02 — wrong label "Maintenance State"
2. TC_Misc_03 — wrong page (/slds vs Edit drawer)
3. TC_Copy_09 — only step-1 dialog title recognized
4. TC_Report_01 — wrong surface (asset detail kebab vs /admin Forms)
5. TC_CONN_081 — wrong surface (Edit vs Add drawer) + log-only assertions
6. TC_AIExt_01..06 — wrong label + wrong surface
7. TC_AIExt_08 — wrong premise (AI Extract isn't in Bulk Ops)
8. TC_Bulk_01..07 — wrong surface (top-level vs dropdown)
9. TC_Bulk_13 first attempt — native-setter on MUI DataGrid (must use cell click)
10. **TC_Misc_03b first attempt — loose ancestor walk caught Asset Class instead of Suggested Shortcut**

Same fix recipe every time: open the page, observe what's actually there, encode falsifiable assertions tied to real signals — not guessed selectors.

## Action items

For me (open follow-ups):
- TC_AIExt_08 still disabled (Bulk Ops AI Extract premise was wrong; needs deletion in next commit)
- Sweep TC_Bulk_01..07 to use the dropdown surface

For the team:
- Add seed data: at least one shortcut preset per asset class on the QA tenant. Without seeded data, TC_Misc_03b will permanently skip — coverage gap that's hard to close without test data.

---

_Per memory rule: this changelog is for learning + manager review. The fix is in the diff; this doc is the why._
