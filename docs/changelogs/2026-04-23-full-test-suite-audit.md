# 2026-04-23 17:30 — Full test-suite audit: conformance to Principal-SDET standards

## Prompt
> check all the test case are proper or not. [followed by a Principal SDET prompt template with 12 project-specific patterns + 9 anti-patterns]

## Model in use
Claude Opus 4.7 (1M context), `effortLevel: max`, always-thinking on.

## Branch safety
Audit-only. No code changed. No commits. No pushes. Findings delivered as this changelog.

## Scope of the audit

**Inventory**:
- 56 test files (51 web TestNG classes, 4 API tests, 1 BaseTest)
- **1,246 @Test methods**
- ~47,000 lines of test code

**Checks performed (14 rules from your prompt template)**:
1. Every `@Test` starts with `ExtentReportManager.createTest(MODULE, FEATURE, testName)`
2. No `Thread.sleep()` in @Test method bodies (helpers OK)
3. Screenshots via `ScreenshotUtil.captureScreenshot()`, NOT `takeScreenshot()`
4. No hardcoded URLs (all via `AppConstants.BASE_URL`)
5. No hardcoded credentials (all via `AppConstants.VALID_*`)
6. No `contains(text(), ...)` — prefer `normalize-space()`
7. No `new ChromeDriver()` in `@BeforeMethod` (unless pre-auth exception)
8. Zoom consistency across all files
9. Module/feature names use `AppConstants.MODULE_*` / `FEATURE_*`
10. Per-file @Test distribution
11. Does ReportingEngineV2TestNG wire into ExtentReportManager
12. Hardcoded MODULE/FEATURE strings in createTest() calls
13. Selector priority (id > data-testid > css > xpath)
14. ZP-323 new files (8 files) spot-check

## Findings summary

| # | Severity | Rule | Violations | Details |
|---|---|---|---|---|
| 1 | **CRITICAL** | Every `@Test` must call `ExtentReportManager.createTest()` | **39** | ALL in `ReportingEngineV2TestNG.java` |
| 2 | ✅ PASS | No `Thread.sleep()` in test bodies | 0 | 10 sleeps exist in helpers only |
| 3 | ✅ PASS | `captureScreenshot` not `takeScreenshot` | 0 | 275 correct, 0 violations |
| 4 | ✅ PASS | No hardcoded URLs | 0 | 2 grep hits, both legitimate (HTTP-redirect check + javadoc) |
| 5 | ✅ PASS | No hardcoded credentials | 0 | Grep hits are CSS selector strings, not creds |
| 6 | **MAJOR** | Prefer `normalize-space()` over `contains(text(),…)` | 206 | 168 in tests + 38 in POs. Top offender: `EgFormAITestNG` (42) |
| 7 | ✅ PASS (exception) | No ChromeDriver in @BeforeMethod | 3 | All 3 are **documented exceptions** for auth-smoke (fresh browser per role) |
| 8 | ✅ PASS | Zoom consistency | 0 | All 7 files now at **90%** (was 80%, fixed last prompt) |
| 9 | ✅ PASS | `AppConstants.MODULE_/FEATURE_` in createTest | 0 | Hardcoded strings not found |
| 13 | ⚠️ ADVISORY | Selector priority: id > data-testid > css > xpath | — | 669 xpath vs 38 id, 34 data-testid. XPath-heavy but reflects MUI reality |
| 14 | ✅ PASS | ZP-323 files conformance | 0 | All 8 files 100% conformant |

---

## Details — CRITICAL violation

### ReportingEngineV2TestNG.java — 39 @Test methods without `createTest()`

**File stats**: 1,211 lines, 39 @Tests covering TC1 through TC6.

**Evidence**: The file imports `ExtentReportManager`, has `@BeforeSuite` that calls `initReports()` and `flushReports()`, and uses `logStepWithScreenshot` / `logInfo` inside a custom `logStep()` helper. But **no method body contains `ExtentReportManager.createTest(MODULE, FEATURE, testName)`**.

**Why this is critical**: Without `createTest()`, all 39 tests' logs get attached to whatever Extent test was created last (or no test at all). In the ExtentReport output:
- Screenshots, info logs, and step logs are either orphaned or attributed to the wrong test
- Pass/fail per-test granularity is lost in the Client Report view
- The rule explicitly warns: *"the test will fail with NullPointerException if reports not initialized"* — even if it doesn't NPE here (because the helper catches `Exception`), the output is still malformed

**Example** (line 307):
```java
@Test(description = "TC-1.1: Navigate to Company Settings → Branding & Assets")
public void TC1_01_navigateToBrandingAndAssets() {
    logStep("TC-1.1: Navigate to Company Settings → Branding & Assets");
    // ^^^ should have ExtentReportManager.createTest(MODULE, FEATURE, "TC-1.1") above this
    navigateViaUrl("/settings");
    ...
}
```

**Recommended fix** (per @Test method):
```java
@Test(description = "TC-1.1: Navigate to Company Settings → Branding & Assets")
public void TC1_01_navigateToBrandingAndAssets() {
    ExtentReportManager.createTest(
        AppConstants.MODULE_REPORTING,            // may need new constant
        AppConstants.FEATURE_BRANDING_ASSETS,     // may need new constant
        "TC-1.1: Navigate to Branding & Assets");
    logStep("TC-1.1: Navigate to Company Settings → Branding & Assets");
    ...
}
```

**Effort**: ~30 min (39 methods × ~1 min each to add the createTest line + confirm MODULE/FEATURE constants exist; if not, add them to `AppConstants.java`).

---

## Details — MAJOR violation

### 206 `contains(text(), ...)` uses — should migrate to `normalize-space()`

**Breakdown**:
- 168 in `src/test/java/` test files
- 38 in `src/main/java/` page objects

**Top offenders (test files)**:
| File | Count |
|---|---:|
| `EgFormAITestNG.java` | 42 |
| `SiteSelectionTestNG.java` | 21 |
| `ReportingEngineV2TestNG.java` | 21 |
| `CriticalPathTestNG.java` | 20 |
| `AuthSmokeTestNG.java` | 11 |

**Why the rule says to prefer `normalize-space()`**: `contains(text(), 'X')` matches the FIRST text node's direct content. If the element has whitespace or nested elements like `<button> <span>X</span> </button>`, `text()` returns whitespace first and `contains(text(), 'X')` fails. `normalize-space()` collapses all whitespace and reads the full concatenated text, making it robust to nested spans and whitespace variations.

**Risk of leaving as-is**: flaky test failures when the app inserts whitespace or nests labels (common in MUI button upgrades).

**Effort**: Not worth a bulk fix. Migrate opportunistically when touching a test for other reasons.

---

## Details — Selector priority advisory (informational)

The rule says prefer `By.id` > `data-testid` > semantic XPath > index XPath. Actual usage:

```
By.id:          38
By.cssSelector: 194
By.xpath:       669
data-testid:    34
```

**This heavy xpath skew is NOT a test-framework problem** — it's a reflection of the eGalvanic MUI frontend. MUI generates hashed class names and doesn't provide stable `id` or `data-testid` attributes on most elements. XPath with `//button[normalize-space()='Save']` is often the ONLY stable option.

**Where you COULD improve**: Work with dev to add `data-testid` to the 20-30 most-clicked buttons (Create, Save, Delete, Submit, etc.). Each `data-testid` added typically eliminates 5-10 brittle xpath selectors. High ROI but requires frontend PR coordination.

---

## Details — ZP-323 new files (your last big batch) are perfect

| File | @Test | createTest() | Thread.sleep | AppConstants usage |
|---|---:|---:|---:|---:|
| AIExtractionTestNG | 10 | 10 ✅ | 0 ✅ | 10 ✅ |
| BulkUploadBulkEditTestNG | 11 | 11 ✅ | 0 ✅ | 11 ✅ |
| ConnectionCoreAttrsTestNG | 9 | 9 ✅ | 0 ✅ | 9 ✅ |
| CopyToCopyFromTestNG | 8 | 8 ✅ | 0 ✅ | 8 ✅ |
| GenerateReportEgFormTestNG | 7 | 7 ✅ | 0 ✅ | 7 ✅ |
| IRPhotoTestNG | 9 | 9 ✅ | 0 ✅ | 9 ✅ |
| MiscFeaturesTestNG | 8 | 8 ✅ | 0 ✅ | 8 ✅ |
| BcesIqSmokeTestNG | 3 | 3 ✅ | 0 ✅ | 3 ✅ |
| **Total** | **65** | **65** | **0** | **65** |

Every @Test in these 8 files starts with `ExtentReportManager.createTest(AppConstants.MODULE_X, AppConstants.FEATURE_Y, "TC…")`. No `Thread.sleep`. No hardcoded anything. All passed on the latest CI run.

---

## Additional context — CI results from run 24833211756 (commit `ffc2744`)

While auditing, the background-triggered CI run completed. Results confirm the 3 fixes from the prior prompt worked:

| Group | Before (run 24781832617) | After (run 24833211756) |
|---|---|---|
| AI Form Creation | 44 pass / 12 fail | **56 pass / 0 fail** ✅ |
| Smoke Suites | 22 pass / 9 fail | **31 pass / 0 fail** ✅ |
| Curated Bug Verification | 5 pass / 3 fail | **3 pass / 5 fail** (polarity flipped correctly per user intent) |
| BCES-IQ Tenant Smoke | — (new) | **3 pass / 0 fail** ✅ |
| AI Page Analyzer | 3/0 | 3/0 ✅ |
| Visual Regression | 7/0 | 7/0 ✅ |
| Monkey Exploratory | 0/4 ❌ | 0/4 ❌ (not in scope of last fix) |
| Load + API + Critical Path | 0/0 ⚠️ | 0/0 ⚠️ (never starts) |
| **TOTAL** | **81 pass / 28 fail** | **103 pass / 9 fail** |

---

## Prioritized recommendations

### P0 — Critical (do before next client report)
1. **Add `ExtentReportManager.createTest()` to all 39 tests in `ReportingEngineV2TestNG.java`**
   - Effort: 30 minutes
   - Impact: Correct per-test granularity in the client-facing ExtentReport
   - Requires: Adding `AppConstants.MODULE_REPORTING` + `FEATURE_BRANDING_ASSETS` / `FEATURE_TEMPLATES` / `FEATURE_EG_FORMS_V2` etc.

### P1 — Should fix within this sprint
2. **Investigate Monkey Exploratory (0/4 still failing in latest run)** — this is an exploratory bot test suite that fails deterministically. Either the tests have a shared environmental dependency (e.g., admin session) that's missing, or the tests themselves are stale. Need to look at `MonkeyTestNG.java` failures.
3. **Investigate Load + API + Critical Path running 0 tests** — "P:0 F:0 S:0" for 0 seconds = the job didn't execute any tests. Likely a suite-XML mismatch or classpath issue.

### P2 — Nice-to-have (no urgency)
4. **Migrate `contains(text(), ...)` → `normalize-space()`** opportunistically (168 test occurrences). Top 3 files: EgFormAITestNG (42), SiteSelectionTestNG (21), ReportingEngineV2TestNG (21). Do as part of existing touch work; no dedicated PR needed.
5. **Coordinate with frontend team to add `data-testid`** on the 20-30 most-clicked buttons — reduces xpath reliance.

### P3 — Advisory only
6. **Curated Bug Verification: 3 still pass when 8 should fail** (user intent). After my Part 5c prereq tightening last prompt, 2 tests correctly moved to FAIL (as expected). But 3 still pass — either those 3 bugs are genuinely fixed in the current build, OR my tightened logic still has false-positive-pass paths on BUG-003 (Issue Class), BUG-005 (No maxLength), BUG-006 (Page Load). Next deep-dive should verify which.

---

## In-depth explanation (for learning + manager reporting)

### How the audit was performed
I did NOT read 47K lines of test code manually. Instead, I wrote 14 automated grep + Python AST checks that programmatically verify each rule against the whole tree. Each check is ~10 lines of bash/Python that takes seconds to run.

The benefit: next time you want to audit, run the same checks — no manual line-counting. If the checks pass, the code conforms. If they fail, you get specific `file:line` citations for follow-up.

### Why the ZP-323 files are perfect (no violations)
Because those files were generated in one batch with the rules already known. The discipline is in the authoring phase — once a class is written with `ExtentReportManager.createTest()` + `AppConstants.MODULE_X` in place, it stays conformant. Retrofit is expensive; greenfield discipline is cheap.

### Why ReportingEngineV2TestNG skipped the rule
Looking at the file, the author used a custom `logStep()` helper that calls `ExtentReportManager.logInfo()` directly. This works for WRITING logs but not for GROUPING them per-test. In ExtentReports, the `createTest()` call is what creates the per-method node in the report tree. Without it, all logInfo() calls attach to whatever node is "current" — which could be the last test run, or nothing.

The fix isn't conceptually hard — add one line per @Test. But it's 39 lines of mechanical edit + possibly 3-4 new `MODULE_*`/`FEATURE_*` constants.

### What "MAJOR" vs "CRITICAL" means in this audit
- **CRITICAL** = affects correctness of output (ReportingEngineV2's broken ExtentReport grouping). A client viewing the report will see garbled output.
- **MAJOR** = latent flakiness risk (contains(text()) may fail when frontend changes whitespace). No current failures but will bite in future.
- **ADVISORY** = stylistic / architectural notes. Not a bug, but could improve if touched.

---

## What I did NOT change

Per the audit-only scope:
- No code files were modified
- No commits were made
- No pushes were attempted

When you're ready to address the P0 finding (ReportingEngineV2 missing `createTest()` calls), say the word and I'll divide that into parts (design MODULE/FEATURE constants → add createTest to each method → verify compile → run the suite) and make the changes.
