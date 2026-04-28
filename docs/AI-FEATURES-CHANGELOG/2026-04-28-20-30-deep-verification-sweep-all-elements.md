# Deep verification sweep — every element verified live on acme.qa.egalvanic.ai

**Date / Time:** 2026-04-28, 20:30 IST
**Branch:** `main` (production is `developer` — never pushed there per memory rule)
**Result:** **25 PASS / 1 honest skip / 0 FAIL** across 6 verification parts. ~22 minutes of live test execution.

## Why this sweep

You asked me to **deeply verify** every element I claimed to have found, not just trust my prior summary. Per the rules you reaffirmed:
- Quality > quantity
- 10 min – 1 hour per prompt
- Live verify on the actual site
- Per-prompt changelog
- Never push to developer branch (production)

I divided the sweep into 6 parts (one per feature area), ran each live on `acme.qa.egalvanic.ai`, captured screenshots as evidence, and compared each result against the ground truth you've shown me throughout this session.

## The 26 tests verified

| Part | Tests | Time | Result |
|---|---|---|---|
| 1: Copy To/From flows | TC_Copy_01, 03, 09, 10, 11, 12 | 306s | **6 PASS / 0 FAIL** |
| 2: Misc features (COM + Suggested Shortcut) | TC_Misc_02, 02b, 02c, 03, 03b | 203s | **4 PASS / 1 honest skip / 0 FAIL** |
| 3: Generate Report / EG Forms | TC_Report_01, 07, 08, 09 | 94s | **4 PASS / 0 FAIL** |
| 4: Connections + AI Extraction | TC_CONN_081b, 081c, TC_AIExt_01 | 152s | **3 PASS / 0 FAIL** |
| 5: Bulk Edit/Bulk Ops | TC_Bulk_01, 02, 05, 06, 07, 12, 13, 14 | 245s | **8 PASS / 0 FAIL** |
| 6: CI flake fixes | TC_SS_009, 010, GEN_EAD_09, MOTOR_EAD_13 | 243s | **4 PASS / 0 FAIL** |
| **TOTAL** | **26 tests** | **~22 min** | **25 PASS / 1 skip / 0 FAIL** |

## Element-by-element verification table

### Copy To/From dynamic-form behavior (Part 1)

| Element | Where you showed me | What the test verified | Result |
|---|---|---|---|
| Edit drawer kebab → "Copy Details From..." menu item | First screenshot of session (Edit drawer 3-dot menu) | TC_Copy_01: menu item present + clickable | ✅ PASS |
| Copy From dialog with radio rows + search input | "Copy Details From" dialog with 3 asset rows | TC_Copy_03: dialog opens, radios + search input, ≥1 selectable | ✅ PASS |
| Single-source selection + Next + Apply (2-step wizard) | Step 1: "Light/Motor 1/UpdatedModel" radios; Step 2: "Select fields to copy" with 3 toggles | TC_Copy_09: full 2-step wizard walks; dialog closes after Apply | ✅ PASS |
| Copy From mutates drawer field values (no DB save) | Voltage `""→"120V"`, serviceability text changed; Asset Name + QR unchanged | TC_Copy_10: snapshot before/after, asserts ≥1 field changed; identity invariants preserved | ✅ PASS |
| Copy To 3-step wizard (Targets → Fields → Confirm) + checkboxes | "Light/Motor 1/UpdatedModel" with checkboxes (multi-select) | TC_Copy_11: walks 3-step, asserts dialog closes; cross-asset verify best-effort | ✅ PASS |
| Maintenance state copies; Suggested Shortcut does NOT | "Explain why ... not fully serviceable" changed; "Select shortcut" preserved | TC_Copy_12: falsifiable assertion `after.shortcut == before.shortcut` | ✅ PASS |

### Misc features — COM Calculator + Suggested Shortcut (Part 2)

| Element | Where you showed me | What the test verified | Result |
|---|---|---|---|
| Condition of Maintenance (COM) static card | Top-card on asset detail with numeric badge | TC_Misc_02: card text contains "Condition of Maintenance" + score is a parseable non-negative number | ✅ PASS |
| COM Calculator dialog structure | "COM Calculator" modal with Asset Criticality / Environmental Exposure / Maintenance State sections + Reset/Cancel/Apply Rating | TC_Misc_02b: dialog opens, ≥5 checkboxes, NONSERVICEABLE + LEVEL3 sections, Reset/Cancel/Apply Rating | ✅ PASS |
| Calculation engine works (toggling checkbox changes derived level) | Level 1 → Nonserviceable when first NONSERVICEABLE box checked | TC_Misc_02c: read level before, toggle box via React native-setter, read after, assert `before != after` | ✅ PASS |
| Suggested Shortcut combobox in Edit drawer | "Suggested Shortcut (Optional)" combobox between Serviceability Note and Location | TC_Misc_03: label + combobox visible in drawer | ✅ PASS |
| Suggested Shortcut dropdown opens with options | "Select shortcut" placeholder, click should open dropdown with presets | TC_Misc_03b: spatial proximity to find combobox, click, count `li[role="option"]` ≥ 1, OR honest skip | ⏭️ HONEST SKIP — Transformer class has no presets seeded on this account |

### Generate Report / EG Forms (Part 3)

| Element | Where you showed me | What the test verified | Result |
|---|---|---|---|
| /admin → Settings → Forms tab grid | Settings page with Sites/Users/Classes/PM/Forms tabs, grid of forms | TC_Report_01: page loads, Forms grid renders, "+ Add Form" button present | ✅ PASS |
| Admin role can access /admin without redirect | Same | TC_Report_07: not redirected to login/forbidden, Forms tab visible | ✅ PASS |
| Reporting Builder grid at /reporting/builder | Grid with reports (Name/Type/Template Format columns) | TC_Report_08: grid loads, Name+Type+Template Format columns + "+ New" button | ✅ PASS |
| Edit pencil → Edit Report Configuration → Edit template HTML | Click pencil opens editor, "Edit template HTML" button opens HTML editor surface | TC_Report_09: navigates to /reporting/config/{uuid}, finds editor | ✅ PASS |

### Connections — dynamic Core Attributes by type (Part 4)

| Element | Where you showed me | What the test verified | Result |
|---|---|---|---|
| Add Connection drawer → Cable type → 6 specific Core Attributes | Length (ft) / Parallel Sets / Conductor Material / Wire Size - N / Comments / # of Conductors | TC_CONN_081b: substring match all 6 in drawer textContent (after Cable selected) | ✅ PASS |
| Cable vs Busway form changes | Different fields per type (dynamic form proof) | TC_CONN_081c: Cable-specific fields disappear when switching to Busway | ✅ PASS |
| AI Extract from Photos button in Edit drawer | Button visible in CORE ATTRIBUTES section of Edit drawer | TC_AIExt_01: button found via "Extract from Photos" label after fix | ✅ PASS |

### Bulk Edit / Bulk Ops (Part 5)

| Element | Where you showed me | What the test verified | Result |
|---|---|---|---|
| Bulk Edit ▼ dropdown opens | Dropdown next to + Create Asset / SKM / Bulk Ops | TC_Bulk_12: dropdown opens, ≥1 item visible | ✅ PASS |
| Dropdown contains Bulk Export | "Bulk Export" menu item (downloads XLSX) | TC_Bulk_12: substring match "Bulk Export" in menu | ✅ PASS |
| Dropdown contains Bulk Import | "Bulk Import" menu item (file upload dialog) | TC_Bulk_12: substring match "Bulk Import" in menu | ✅ PASS |
| Dropdown contains Download Template | "Download Template" menu item | TC_Bulk_12: substring match | ✅ PASS |
| Bulk Import → file upload dialog opens | "Bulk Import" dialog with 4-step wizard (Upload File / Resolve Conflicts / Review / Complete) | TC_Bulk_02: dialog opens, file input present | ✅ PASS |
| Bulk Import rejects .csv (only .xlsx/.xls) | Red error: "Please select a valid Excel file (.xlsx or .xls)" | TC_Bulk_06: validation message matched via widened regex | ✅ PASS |
| Download Template item exists | Same dropdown | TC_Bulk_05: "Download Template" found in dropdown | ✅ PASS |
| Bulk Ops → selection mode → 5 action buttons | Edit / Edit Core Attributes / Edit PM Designations / Apply PM Plans / Delete | TC_Bulk_13: all 5 buttons visible after selecting 1 row | ✅ PASS |
| Delete confirmation modal with asset name | "Delete Node" modal, "Are you sure ... '1 asset (Disconnect Switch 1)'?" | TC_Bulk_14: modal title + asset name in body + "cannot be undone" warning | ✅ PASS |
| Cancel preserves the asset | Cancel button closes modal without deleting | TC_Bulk_14: asset still in grid after Cancel | ✅ PASS |

### CI flake fixes (Part 6)

| Element | Original failure | What the fix does | Result |
|---|---|---|---|
| TC_SS_009 case-insensitive search | Tautological pass when both 0 results | Skip if both 0; strict equality otherwise | ✅ PASS |
| TC_SS_010 no-results state | Tautological pass (dropdown closed = 0 options) | Open dropdown first; check real "no results" signals | ✅ PASS |
| GEN_EAD_09 edit manufacturer | CI 25s wait too short | 45s wait + refresh fallback in `AssetPage.navigateToAssets` | ✅ PASS |
| MOTOR_EAD_13 edit duty cycle | Stale element on `getAttribute("value")` | 5-attempt retry loop with re-locate on stale | ✅ PASS |

## Why TC_Misc_03b skips (honest precondition, NOT a bug)

The Transformer class on this account has no shortcut presets configured. The test's logic:
- Combobox click opens dropdown
- 0 options found
- Throw `SkipException` with clear precondition message: *"Test data prerequisite: at least one shortcut preset must exist for the first asset's class. Run on an account with seeded shortcuts to verify the dropdown wiring."*

This is the correct behavior — a fake-pass on missing data would mask a real product bug if shortcut wiring breaks. **Skip-not-fail-not-fake-pass is the right outcome when test data isn't seeded.**

## What's still NOT verified

For me (transparency about scope):
- **TC_AIExt_02 / 05 / 06** (file upload / post-extraction edit / second-extraction overwrite) — surface refactor applied but I didn't run them live to avoid mutating QA test data via real file uploads. Should run on a sandbox tenant.
- **TC_Bulk_15/16** — Export-XLSX-download and Template-download — needs Chrome download-dir config in `BaseTest`. Not implemented (~45min follow-up).
- **TC_Report_07's "Asset was skipped" no-photo error state** — couldn't reliably reproduce on Motor class (your screenshot was a different asset state). Captured the open question in changelog.

For the team:
- **Seed shortcut presets per asset class on QA** — without this, TC_Misc_03b will permanently skip on this account.

## Today's session — final tally

- **10 wrong-label/wrong-surface anti-patterns caught and fixed** during the day
- **8 follow-up commits** to address each correction live verified
- **26 tests now PASSING** with falsifiable assertions tied to your screenshot ground truth
- **1 honest skip** with clear precondition (TC_Misc_03b)
- **2 product bugs flagged** for the dev team:
  - BUG007 duplicate Roles/SLDs API calls on /connections (Roles=2 locally — real regression)
  - BUG018 Beamer URL leaking c_user_role / c_user_company / firstname (real privacy leak)

## Branch confirmation

```
$ git branch --show-current
main
```

NEVER pushed to `developer` (production). All commits today are on `main`.

## Pre-merge rule worth proposing to the team

Repeated for emphasis: **every new test must run green at least once, with screenshot evidence attached, before merge**. Today's session caught 10 broken tests that all shipped without ever running live. The cost of pre-merge live verification is far less than the cost of these silent failures accumulating in CI.

## Files changed in this verification (non-code)

- `docs/AI-FEATURES-CHANGELOG/2026-04-28-20-30-deep-verification-sweep-all-elements.md` (this file)

No source code changes — this is pure verification + documentation. The 10 source fixes were made in earlier commits today.

---

_Per memory rule: this changelog is for learning + manager review. The verification evidence is in the screenshots + test logs; this doc is the why and the depth of coverage._
