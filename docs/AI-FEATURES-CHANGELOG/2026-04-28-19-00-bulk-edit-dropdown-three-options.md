# Bulk Edit ▼ dropdown — Export / Import / Template menu verification

**Date / Time:** 2026-04-28, 19:00 IST
**Branch:** `main` (production is `developer` — never pushed there per memory rule)
**Result:** TC_Bulk_12 PASSES live (~62s). Verifies all 3 dropdown items.

## What you showed me

Screenshot of the **Bulk Edit ▼** dropdown opened on the Assets page toolbar — the menu reveals exactly 3 options:
1. **Bulk Export** — downloads an XLSX file
2. **Bulk Import** — opens import flow
3. **Download Template** — downloads the import template

Plus your hint: *"bulk export will download xlsx file and import will import help to select bulk import download templete"* — confirming the function of each item.

## What was wrong with the existing tests

`BulkUploadBulkEditTestNG.java` has 11 tests (TC_Bulk_01 through TC_Bulk_11), most of which look for top-level buttons:

```java
WebElement entry = findByText("Bulk Upload", "Bulk Import", "Import Assets", "Upload CSV");
```

But none of those labels exist as top-level buttons in production — they're **nested inside the Bulk Edit ▼ dropdown** as "Bulk Import" and "Download Template". The existing tests fall through to a `null`-handling branch (some skip silently with `logWarning`, some fail noisily). 

This is the **8th wrong-label/wrong-surface anti-pattern** caught this session. The pattern is universal: tests written without live verification at write-time end up looking in the wrong place.

## What I added

### TC_Bulk_12 — Bulk Edit dropdown reveals all 3 expected items

```
Navigate to /assets
→ Find "Bulk Edit" button on toolbar (next to + Create Asset, SKM, Bulk Ops)
→ Click to open dropdown
→ Dump all visible menu items
→ Assert: each of {Bulk Export, Bulk Import, Download Template} appears
→ Cleanup: ESC to close menu
```

**Falsifiable assertions** with substring matching (so minor label tweaks like "Bulk Export Assets" still pass, but renames or removals fire immediately):

```java
String[] expectedItems = {
    "Bulk Export",
    "Bulk Import",
    "Download Template"
};
// Each must be present in the discovered menu items
```

If the FE removes one (or adds new items in a way that breaks this contract), the test fires with a clear list of what's missing.

## Live verification

```
$ mvn test -Dtest='BulkUploadBulkEditTestNG#testTC_Bulk_12_BulkEditDropdownItems'
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Total runtime: ~62 seconds for the single test. Opens the dropdown, snapshots menu items, asserts, closes cleanly.

## Files changed

| File | Change | Lines added |
|---|---|---|
| `src/test/java/com/egalvanic/qa/testcase/BulkUploadBulkEditTestNG.java` | Added TC_Bulk_12 before the `// -------- helpers --------` divider | ~80 |

## What I deliberately did NOT do (in scope for follow-up, not this commit)

1. **Update existing TC_Bulk_01..07 to use the dropdown surface**. Those 7 tests all look for top-level buttons that don't exist. Each needs to be refactored to first click "Bulk Edit ▼" then look for the nested item. That's a 7-test refactor with risk of breaking other things — better as its own commit.

2. **Click each menu item and verify the action**. The natural follow-up tests:
   - **TC_Bulk_13**: click Bulk Export → verify XLSX download starts (file appears in download dir within 10s)
   - **TC_Bulk_14**: click Download Template → verify template download
   - **TC_Bulk_15**: click Bulk Import → verify file picker dialog opens

   These need download-dir setup (Chrome download path config) which is its own setup story. Out of scope for this commit but worth filing.

3. **The Bulk Ops AI Extract question is still open**. I confirmed Bulk Ops opens selection mode (not a dropdown), but couldn't reliably locate where the AI Extract action lives. Marked TC_AIExt_08 disabled in the previous commit. Still need a live walkthrough from you to encode it correctly.

## Pattern catalog now at 8 instances

| # | Test | Was wrong about | Real surface |
|---|---|---|---|
| 1 | TC_Misc_02 | "Maintenance State" + Calculations tab | "Condition of Maintenance (COM)" top card |
| 2 | TC_Misc_03 | /slds page label | Edit drawer combobox |
| 3 | TC_Copy_09 | step-1 dialog title only | Step-2 ("Select fields to copy") |
| 4 | TC_Report_01 | Asset detail kebab | /admin Settings → Forms tab |
| 5 | TC_CONN_081 | Edit drawer + log-only | Add drawer + falsifiable textContent |
| 6 | TC_AIExt_01..06 | "AI Extraction" + Create form | "Extract from Photos" + Edit drawer |
| 7 | TC_AIExt_08 (open) | Assumed in-drawer was the AI extract | Bulk Ops / Bulk Edit on Assets page |
| **8** | **TC_Bulk_01..07** | **"Bulk Upload" as top-level button** | **Inside Bulk Edit ▼ dropdown** |

Same root cause every time: tests committed without ever running live + screenshot-captured. Same recipe every time to fix: open the page, observe what's there, encode falsifiable assertions tied to real signals.

The total cost of these 8 broken tests across the codebase is significantly more than the cost of running each test once before commit. Worth proposing as a CI / PR policy.

## Action items

For the team:
- **Sweep TC_Bulk_01..07** to use the dropdown surface. They're currently log-only (TC_Bulk_05 returns early with `logWarning` instead of failing). 7 tests, ~30min refactor.
- **Add TC_Bulk_13/14/15** for the 3 dropdown actions (Export download, Template download, Import file picker). Needs Chrome download dir config in `BaseTest` first.

For me (open follow-up I owe you):
- **TC_AIExt_08 Bulk Ops/Bulk Edit AI Extract** — I still don't have certainty on where this lives. A short walkthrough from you (which exact button, what happens, next click) would unblock me in ~15min.

---

_Per memory rule: this changelog is for learning + manager review. The fix is in the diff; this doc is the why._
