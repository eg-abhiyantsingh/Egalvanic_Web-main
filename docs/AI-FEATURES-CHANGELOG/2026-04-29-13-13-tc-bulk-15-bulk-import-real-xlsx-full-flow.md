# TC_Bulk_15 — Bulk Import full flow with real user XLSX

**Date / Time:** 2026-04-29, 13:13 IST
**Branch:** `main` (production is `developer` — never pushed there per memory rule)
**Site:** `acme.qa.egalvanic.ai`
**Test data:** `docs/test_data_file/abhiyant-Espohio_477_Final_bulk.xlsx` (21 KB, 477 rows)
**Result:** PASS live (99s).

## What you asked for

> "click on Bulk Import Then select file abhiyant-Espohio_477_Final_bulk.xlsx then select next next cover this in bulk upload test case"

You wanted an automated walk through the actual Bulk Import flow with a real user-supplied XLSX — not just "verify entry point exists" (TC_Bulk_01) or "verify file input accepts a file" (TC_Bulk_02), but the **end-to-end** path from Bulk Edit ▼ dropdown → Bulk Import → file upload → wizard advance → reach the terminal "Process Import" step.

## File added

`src/test/java/com/egalvanic/qa/testcase/BulkUploadBulkEditTestNG.java` — added `testTC_Bulk_15_BulkImportFullFlowWithRealFile` (~150 lines).

## What the test does

| Step | Action | Falsifiable assertion |
|---|---|---|
| 0 | Verify XLSX exists at `docs/test_data_file/abhiyant-Espohio_477_Final_bulk.xlsx` | `xlsx.exists() && xlsx.canRead()` |
| 1 | Navigate to Assets, open Bulk Edit ▼ → click Bulk Import | `openBulkImportDialog()` returns true |
| 2 | Force-show hidden file inputs, sendKeys absolute path | File input present |
| 3 | Poll up to 20s for wizard with Next/Continue/Process button | (informational) |
| 4 | Walk intermediate advance buttons (Next/Continue/Validate/Proceed) | none required — single-step variant exists |
| 5 | Verify terminal button is visible: Process Import / Import / Submit / Confirm / Finish / Done / Upload | At least one terminal button visible |
| 6 | **Cleanup**: click Cancel (do NOT click Process Import — would import 477 rows into QA) | Dialog closed |

## The "Process Import" discovery — assumption-first vs diagnostic-first

**v1 assumed** the wizard has "Next" buttons (you said "next next"). Live result: `Wizard didn't advance — buttons seen: [aaabhiyant admin, Create Asset, Bulk Edit, SKM, Bulk Ops, Cancel]`.

That output was confusing — those are page-toolbar buttons, not dialog buttons. So I added a DOM diagnostic that walks `.MuiDialog-root` / `.MuiDrawer-root` / `[role=dialog]` and lists *headings + buttons inside each one*. Re-ran.

**v2 result:** the diagnostic surfaced one extra page-level button: `Process Import`. So the bulk-import wizard's terminal action button is literally named `Process Import` — not the generic `Import` / `Submit` / `Next` patterns I'd assumed.

**Lesson reinforced:** when an assertion fails, don't widen the matcher with more guesses. **Dump the DOM, see the truth, code to that.** I've now hit this same lesson 3 times this week (TC_Misc_03b combobox, TC_BH_03 Asset Class label, TC_Bulk_15 Process Import). Pattern: tests that match by *spatial proximity* or *role* survive label changes; tests that match by exact text labels are brittle.

## Why we don't click Process Import

The XLSX has 477 rows. Clicking Process Import would actually create 477 assets on the QA tenant. That's:
- Test pollution (next test runs see 477 ghost assets)
- Slow (the import itself takes minutes)
- Hard to undo (no bulk-delete-by-import-batch)

**Verify-but-don't-fire** is the right pattern: assert the terminal button is *reachable and visible*, then cancel out. If you ever want a true end-to-end test that actually imports, do it on a sandbox tenant with a teardown step that bulk-deletes by name prefix.

## Files changed

| File | Change |
|---|---|
| `src/test/java/com/egalvanic/qa/testcase/BulkUploadBulkEditTestNG.java` | +~165 lines: new TC_Bulk_15 |
| `docs/test_data_file/abhiyant-Espohio_477_Final_bulk.xlsx` | new test asset |
| `docs/AI-FEATURES-CHANGELOG/2026-04-29-13-13-...md` | this changelog |

## Live verification

```
$ mvn test -Dtest='BulkUploadBulkEditTestNG#testTC_Bulk_15_BulkImportFullFlowWithRealFile'
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0   (99s)
```

PASS. Bulk Edit ▼ → Bulk Import → file upload → terminal step reached → cleaned up. No 477 rows imported into QA.

## Branch confirmation

```
$ git branch --show-current
main
```

NEVER pushed to `developer` (production) per memory rule.
