# Bulk Ops action toolbar (5 buttons) + Delete confirmation modal

**Date / Time:** 2026-04-28, 19:15 IST
**Branch:** `main` (production is `developer` — never pushed there per memory rule)
**Result:** TC_Bulk_13 + TC_Bulk_14 both PASS live (108s combined).

## What you showed me (with corrections to my prior assumption)

Two screenshots clarified Bulk Ops definitively:

**Screenshot 1**: Bulk Ops + 1 row selected → toolbar reveals **5 bulk action buttons**:
1. Edit (single-row edit)
2. Edit Core Attributes
3. Edit PM Designations
4. Apply PM Plans
5. Delete (red, opens confirmation modal)

Plus a "1 selected" counter and "Cancel" exit.

**Screenshot 2**: Click Delete → confirmation modal "Delete Node" with body:
> Are you sure you want to delete "1 asset (Disconnect Switch 1)"?
> This action cannot be undone.

Plus [Cancel] and [Delete] buttons.

**My prior assumption was wrong**: I'd suspected AI Extract was somewhere in Bulk Ops based on your earlier comment. Now confirmed: **AI Extract is NOT in Bulk Ops at all**. Bulk Ops is purely the bulk-edit/delete workflow. So my TC_AIExt_01 (in-drawer "Extract from Photos" button) IS the correct AI extraction surface — it was right all along; I just got confused interpreting your message.

## What I added

### TC_Bulk_13 — All 5 bulk action buttons appear after row selection

```
Navigate to /assets
→ Click "Bulk Ops" → selection mode active
→ Click first row's checkbox CELL (Selenium-native, NOT JS native-setter)
→ Snapshot all visible button labels
→ Falsifiable assertion: each of {Edit, Edit Core Attributes,
  Edit PM Designations, Apply PM Plans, Delete} must be present
→ Plus assert: "1 selected" counter visible (with widened regex)
→ Cleanup: Cancel exits selection mode
```

Why **Selenium-native click on the checkbox cell**, not the React native-setter pattern I used elsewhere: MUI's DataGrid binds its row-selection click handler on the `.MuiDataGrid-cellCheckbox` cell wrapper, not the hidden `<input>`. The native-setter approach (which works for plain MUI Checkbox in TC_Misc_02c) does NOT trigger DataGrid's selection-state update. Confirmed via failed first attempt — the toolbar stayed empty after the JS-setter click.

### TC_Bulk_14 — Delete confirmation modal preserves asset on Cancel

```
Capture first asset name BEFORE entering selection mode
→ Bulk Ops + select first row
→ Click Delete in bulk action toolbar
→ Verify modal opens with:
  • Title: "Delete Node"
  • Body contains the captured asset name (substring match)
  • Body contains "cannot be undone" warning
→ Click Cancel in modal (DO NOT click Delete — would mutate real QA data)
→ Verify modal closed AND asset still in grid (preserved)
```

Three falsifiable assertions:
1. Modal title is "Delete Node"
2. Modal body references the EXACT selected asset name
3. After Cancel, the asset is STILL in the grid

If any of these fires, it's a real product/UX regression: missing modal, wrong asset shown, or Cancel accidentally deleting.

## Live verification

```
$ mvn test -Dtest='BulkUploadBulkEditTestNG#testTC_Bulk_13_BulkOpsActionButtons+testTC_Bulk_14_DeleteConfirmationCancelPreserves'
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Total runtime: 108s for both. No DB writes (cancel-without-delete pattern keeps test data clean).

## Why my first TC_Bulk_13 attempt failed (debugging anecdote worth filing)

First run: discovered buttons = `[aaabhiyant admin, Cancel]` — only the user avatar and Cancel were visible. The 5 expected bulk action buttons were nowhere to be found.

Root cause: I'd used the React native-setter pattern (`Object.getOwnPropertyDescriptor(...).set.call(cb, true)` + dispatchEvent) — same approach that worked for the COM Calculator's NONSERVICEABLE checkbox (TC_Misc_02c). This pattern is great for plain MUI `<Checkbox>` components but **MUI DataGrid is special** — its row-selection handler is bound to the cell wrapper (`.MuiDataGrid-cellCheckbox`), not the underlying `<input>`. The native-setter approach updates the input's state but doesn't fire the DataGrid's selection event.

Fix: Selenium-native click on the cell wrapper. The DataGrid's React handler picks it up and triggers the selection-state cascade that reveals the action toolbar.

This is worth remembering as a project pattern: **MUI Checkbox → JS native-setter is fine. MUI DataGrid Checkbox → must click the cell wrapper natively.**

## Files changed

| File | Change | Lines |
|---|---|---|
| `src/test/java/com/egalvanic/qa/testcase/BulkUploadBulkEditTestNG.java` | Added TC_Bulk_13 + TC_Bulk_14 before the helpers section | ~205 |

## Coverage map of Bulk * features (now)

| Surface | Test | Status |
|---|---|---|
| Bulk Edit ▼ dropdown opens | TC_Bulk_12 | PASS (added in prior commit) |
| Bulk Edit ▼ has Export/Import/Template | TC_Bulk_12 | PASS |
| Bulk Ops enables selection mode | TC_Bulk_03 (existing, log-only) | unchanged |
| **Bulk Ops + 1 row → 5 action buttons** | **TC_Bulk_13** | **PASS (new)** |
| **Delete confirmation shows asset name** | **TC_Bulk_14** | **PASS (new)** |
| Bulk Export → XLSX download | _none_ | follow-up |
| Download Template → file download | TC_Bulk_05 (log-only existing) | follow-up to deepen |
| Bulk Import → file picker | TC_Bulk_02 (existing) | unchanged |

## Pattern catalog now at 9 instances (cumulative this session)

Same wrong-label/wrong-surface pattern keeps appearing. Today's catalog:

1. TC_Misc_02 — wrong label "Maintenance State" (real: COM)
2. TC_Misc_03 — wrong page /slds (real: Edit drawer)
3. TC_Copy_09 — only step-1 dialog title recognized
4. TC_Report_01 — wrong surface, asset detail kebab (real: /admin Forms)
5. TC_CONN_081 — wrong surface, Edit drawer (real: Add drawer) + log-only
6. TC_AIExt_01..06 — wrong label + Create form (real: "Extract from Photos" + Edit)
7. TC_AIExt_08 — open follow-up; mistakenly assumed AI Extract was in Bulk Ops
8. TC_Bulk_01..07 — looked for top-level buttons that live behind Bulk Edit ▼
9. **TC_Bulk_13 first attempt** — used native-setter on MUI DataGrid checkbox (must use Selenium-native cell click)

The good news: **fix recipe is consistent and quick once the right pattern is identified**. The bad news: each of these silently shipped and accumulated until forced into the open by a screenshot from you.

## Action items

For me (open follow-up):
- TC_AIExt_08 (Bulk Ops/Bulk Edit AI Extract) — confirmed today: AI Extract is NOT in Bulk Ops. The in-drawer "Extract from Photos" button (TC_AIExt_01) is the correct surface. **Mark TC_AIExt_08 for deletion** in next commit since the premise was wrong.
- Sweep TC_Bulk_01..07 to use the dropdown surface (~30min refactor).

For the team:
- File the **MUI DataGrid native-setter** caveat as a project README note. Other test authors will hit this when working with DataGrid checkboxes.

---

_Per memory rule: this changelog is for learning + manager review. The fix is in the diff; this doc is the why._
