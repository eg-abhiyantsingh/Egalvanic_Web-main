# Fix: consolidated DETAILED report dropping test cases (module-name collision)

- **Date:** 2026-06-24
- **Prompt:** "screenshot is not showing properly and all test case are not showing in consilideade
  report. consolidated-detailed-report-suite2 all test case are not showing why"
- **Type:** CI report-generation bug fix (`.github/scripts/consolidated-detailed-report.py`,
  `.github/scripts/consolidated-report.py`). No Java / test changes.

---

## Root cause (why test cases were missing)

Every asset-related test class — all **5 engineering suites** (`AssetEngineeringTestNG`,
`TransformerEngineeringTestNG`, `AssetEngineeringMatrixTestNG`, `MainsConfigEngineeringTestNG`,
`AssetEngineeringExhaustiveTestNG`) **plus** all 5 `AssetPartN` suites — calls
`createTest(AppConstants.MODULE_ASSET, …)`, i.e. module name **`"Asset Management"`**
(`ExtentReportManager` derives the detailed-report filename from the module name).

In CI each group runs as a **separate job / JVM**, so each emits its own
`Detailed_Report_Asset_Management_<timestamp>.html` into its own artifact
(`reports-s2-<group>/`). The summary job downloads them all into `all-reports/` (deliberately
**without** `merge-multiple`, so the files coexist on disk in per-group subdirs) and runs
`consolidated-detailed-report.py`.

`discover()` keyed purely by **module display name** and kept only the **newest timestamp**
("drops re-run dupes"). That assumption — *one module = one report* — is false under the
parallel-suite model: one module is split across many parallel jobs. So for "Asset Management",
**only the newest of the 5+ group reports survived; the rest were silently dropped.** With the
Engineering option enabled, 4 of 5 engineering groups (≈360 of 386 engineering test cases)
vanished from the consolidated detailed report. The same latently affected any module split
across groups (Work Orders, Issues, Connections, Bug Hunt).

**Screenshots** are inline base64 and render correctly (verified). The "screenshots not showing"
symptom was the same collision — the tests whose screenshots were wanted lived in the dropped
group reports.

## The fix

### `consolidated-detailed-report.py`
- New `group_from_relpath()` — derives the group from the artifact subdir
  (`all-reports/reports-s2-<group>/…`), stripping the `reports-s2-` / `reports-s1-` / `reports-`
  prefix. Returns `""` when the report sits directly in the input dir (local single run).
- `discover()` now keys by **`(group, module)`** instead of `module`, keeping the newest report
  per `(group, module)` (still drops genuine same-job re-run dupes). **Every group's report is
  kept.** When a module name comes from >1 group, its display name is suffixed with the group
  (e.g. `Asset Management · asset-engineering`, `… · asset-exhaustive`) so all groups' tests are
  preserved AND distinguishable. Single-group modules stay clean (no suffix).
- `main()` updated to consume the new entry list + a filename-uniqueness guard.
- **Local behavior unchanged:** running against a flat dir → group `""` → old "newest per module"
  exactly.
- Robustness bonus: each module iframe now also sets its **own** height directly via
  `window.frameElement` (in addition to the existing `postMessage`), so the single-file report is
  correctly sized even before the parent JS runs / when opened directly — no more collapse to the
  200px floor that would clip tests + screenshots below the fold.

### `consolidated-report.py` (client/summary)
- Not affected by the collision (it dedups by `(class, method)` from `testng-results.xml`), but
  the 5 engineering classes + `WorkOrderIssueTestNG` (+ `WorkOrderPlanningTestNG`) were **missing
  from `CLASS_TO_MODULE`**, so they fell into the catch-all "Other" bucket. Added them so they
  file under **Asset Management** / **Work Orders** correctly.

## Validation (proven, not just compiled)

Reproduced the exact CI layout — 5 `reports-s2-asset-*` subdirs each holding a
`Detailed_Report_Asset_Management_<ts>.html` (same module, different tests) + Work Orders + Tasks:

| | Modules in consolidated detailed report |
|---|---|
| **Before (HEAD script)** | **3** — only newest "Asset Management" survives; 4 engineering groups dropped |
| **After (fixed script)** | **7** — all 5 `Asset Management · <group>` + Tasks + Work Orders |

Rendered the fixed single-file report in Chromium (served over HTTP) and confirmed via DOM
inspection that **every** module renders fully: engineering 13 tests/19 imgs, exhaustive
310/311, matrix 2/3, mains-config 309/124, transformer 18/19, Tasks 34/67, Work Orders 10/8 —
**every base64 screenshot loaded** (`naturalWidth > 0`) and **every iframe correctly sized**
(exhaustive → 255 291px tall, fully expanded). Local flat-dir run still yields 10 clean module
names (no suffix), newest-per-module — behavior preserved. Both scripts `py_compile` clean.
