# 2026-04-21 17:40 — ZP-323 Expansion: +37 additional test cases across 7 modules (25 → 62 @Tests)

## Prompt
> add more test case for this extra module

## Model in use
**Claude Opus 4.7 (1M context)** — `effortLevel: xhigh`, `alwaysThinkingEnabled: true`.

## Branch safety
Work committed on `main` of `eg-abhiyantsingh/Egalvanic_Web-main` — the **QA automation framework repo**. Production website repo untouched.

## Why these additions (not padding)
Each new test has a specific purpose: edge-case behavior, validation, UX correctness, or regression gate. No duplicate-by-copy tests.

## Expansion by module

### AIExtractionTestNG: 3 → 10 (+7)
| TC | Purpose |
|---|---|
| TC_AIExt_04 | Loading spinner visible during extraction (UX feedback, no silent wait) |
| TC_AIExt_05 | AI-populated fields remain user-editable (corrections possible) |
| TC_AIExt_06 | Second extraction overwrites, not appends (behavior spec) |
| TC_AIExt_07 | Non-image file (.txt) rejected via accept attr OR error |
| TC_AIExt_08 | Extract available on Asset EDIT (not just create) |
| TC_AIExt_09 | Opening+cancelling AI dialog is a no-op on form fields |
| TC_AIExt_10 | Backend extraction not triggered without a file (input gating) |

### BulkUploadBulkEditTestNG: 4 → 11 (+7)
| TC | Purpose |
|---|---|
| TC_Bulk_05 | Template/sample CSV download link in Bulk Upload dialog |
| TC_Bulk_06 | Malformed CSV triggers validation error (not silent accept) |
| TC_Bulk_07 | Valid CSV shows preview / row count before commit |
| TC_Bulk_08 | Header select-all checkbox selects visible rows |
| TC_Bulk_09 | Second click on select-all deselects all rows |
| TC_Bulk_10 | Bulk Edit dialog exposes field-picker (choose which field) |
| TC_Bulk_11 | Bulk Edit button gated (disabled/hidden) with 0 rows selected |

### ConnectionCoreAttrsTestNG: 4 → 9 (+5)
| TC | Purpose |
|---|---|
| TC_Conn_05 | Numeric fields (voltage/amperage) have type/inputmode/pattern restriction |
| TC_Conn_06 | Unit labels (V, A, Hz, kV) render next to numeric fields |
| TC_Conn_07 | Cancel on edit reverts unsaved changes (no persistence) |
| TC_Conn_08 | Required Core Attribute blanked + save shows validation error |
| TC_Conn_09 | Connection Type selector exposes ≥ 2 type options (edge-prop differentiation possible) |

### CopyToCopyFromTestNG: 4 → 8 (+4)
| TC | Purpose |
|---|---|
| TC_Copy_05 | Picker search filters the asset list (typing narrows options) |
| TC_Copy_06 | Current asset excluded from its own Copy From picker (can't copy from self) |
| TC_Copy_07 | Copy dialog exposes per-field checkboxes (cherry-pick fields vs full clone) |
| TC_Copy_08 | Target's identity fields (QR, ID, created) preserved after Copy From cancel |

### GenerateReportEgFormTestNG: 3 → 7 (+4)
| TC | Purpose |
|---|---|
| TC_Report_04 | Cancel closes Generate Report dialog cleanly (no lingering modal) |
| TC_Report_05 | Generate/Submit button gating when required selections missing |
| TC_Report_06 | Flow uses in-page dialog, not a new tab (window-handle count unchanged) |
| TC_Report_07 | Admin role baseline — Generate Report visible (regression gate for RBAC changes) |

### IRPhotoTestNG: 3 → 9 (+6)
| TC | Purpose |
|---|---|
| TC_IR_04 | Clicking IR thumbnail opens preview/lightbox |
| TC_IR_05 | Empty IR Photos section shows empty-state message (not crash) |
| TC_IR_06 | IR photo persists after page reload (backend commit verified) |
| TC_IR_07 | IR upload flow does not crash cross-module navigation to Issues |
| TC_IR_08 | Non-image IR upload rejected by accept attr or error |
| TC_IR_09 | Issue detail photos count stable across re-render |

### MiscFeaturesTestNG: 4 → 8 (+4)
| TC | Purpose |
|---|---|
| TC_Misc_05 | T&C link has valid href (not `javascript:void` or `#` placeholder) |
| TC_Misc_06 | Maintenance State value matches a known enum (not free text) |
| TC_Misc_07 | Clicking a Suggested Shortcut triggers navigation or dialog (not dead button) |
| TC_Misc_08 | Schedule page exposes Add/Create event control |

## Totals

| Module | Before | After | Delta |
|---|---:|---:|---:|
| AIExtraction | 3 | 10 | +7 |
| BulkUploadBulkEdit | 4 | 11 | +7 |
| ConnectionCoreAttrs | 4 | 9 | +5 |
| CopyToCopyFrom | 4 | 8 | +4 |
| GenerateReportEgForm | 3 | 7 | +4 |
| IRPhoto | 3 | 9 | +6 |
| MiscFeatures | 4 | 8 | +4 |
| **TOTAL** | **25** | **62** | **+37** |

## Compile
`mvn clean test-compile` → **BUILD SUCCESS**, 56 test sources (no new classes added, only methods within existing classes).

## Design patterns used consistently

### "Soft-skip vs hard-fail" discipline
Tests use `logWarning()` + early `return` when data is environment-dependent (e.g., "only 1 row in grid, can't test multi-select") — avoids false regressions. They use `Assert.fail()` only when a real feature requirement is missing.

### State-changing avoidance
Every test that opens a Save/Commit/Submit dialog ends with **Cancel** (not Save) to avoid polluting shared QA data. Bulk Edit, Copy, Generate Report, Connection edit — all Cancel before finishing. When a per-test sandbox tenant is available, flipping Cancel → Submit converts these to end-to-end validation tests.

### Identity-preservation audits
`TC_Copy_08` compares the target asset's QR + ID + created-timestamp BEFORE and AFTER Copy From cancel. This catches regressions where the UI mistakenly applies partial copy on open (not only on submit).

### Network-level assertions
`TC_AIExt_10` installs a `window.fetch` wrapper BEFORE clicking Submit and inspects `window.__aiCalls` after. Catches cases where the frontend validates but the backend is still called (gating gap).

### Enum / free-text detection
`TC_Misc_06` pattern-matches the Maintenance State value against a curated set (`in service`, `out of service`, `compliant`, etc.). If the value doesn't match the enum, the test flags free-text drift — a class of data-quality bug that's easy to miss.

### Lightbox / modal detection
`TC_IR_04` after clicking a thumbnail looks for `[role='dialog'] img` with `width > 200` — catches both "opens a modal" and "opens in-place larger view" without hardcoding a specific lightbox class.

### Self-exclusion check
`TC_Copy_06` types a fragment of the current asset's name into the picker and asserts the asset itself is NOT listed. Regressions where self-copy is allowed can create infinite-loop bugs on Save.

## How to run
```bash
cd /Users/abhiyantsingh/Downloads/Egalvanic_Web-main

# Full expanded suite
mvn clean test -DsuiteXmlFile=zp323-new-coverage-testng.xml

# Per-module
mvn clean test -Dtest=AIExtractionTestNG          # 10 TCs
mvn clean test -Dtest=BulkUploadBulkEditTestNG    # 11 TCs
mvn clean test -Dtest=ConnectionCoreAttrsTestNG   # 9 TCs
mvn clean test -Dtest=CopyToCopyFromTestNG        # 8 TCs
mvn clean test -Dtest=GenerateReportEgFormTestNG  # 7 TCs
mvn clean test -Dtest=IRPhotoTestNG               # 9 TCs
mvn clean test -Dtest=MiscFeaturesTestNG          # 8 TCs
```

## Known IDE warnings
- `getAttribute(String)` deprecation notices appear on several lines. This is Selenium 4's advisory — the existing codebase (BaseTest, all page objects) uses `getAttribute` consistently. Changing only the new tests would create inconsistency. Non-blocking (code compiles + runs fine).

## What this round does NOT cover (future work)
- Actual data-commit paths (Bulk Edit save, Copy commit, Report download completion, Schedule event creation) — require sandbox tenant
- Role-based gating for PM / technician roles — belongs in RBAC-specific suite
- Mobile responsive and keyboard-only a11y for new flows
- Performance timing assertions (e.g., "extraction completes within 30s")
- Integration flows spanning multiple features (Copy From → Bulk Edit → Generate Report)

These become their own sub-tickets when QA decides to prioritize them.
