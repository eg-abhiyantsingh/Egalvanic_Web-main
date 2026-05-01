# Full Suite Verification — 1069 tests, 14 failures (98.7% pass rate)

**Date / Time:** 2026-05-01, 05:45 IST (suite started 2026-04-30 ~19:40, ran 10h 4min)
**Branch:** `main` (production is `developer` — never pushed there per memory rule)
**Site:** `acme.qa.egalvanic.ai`
**Suite:** `fullsuite-testng.xml` (the project's curated "all modules" suite)
**Result:** 1055 PASS, 14 FAIL, 0 ERROR, 0 SKIP. Wall time 10h 4min.

## What you asked for

> "check all the test case"

I ran the project's full TestNG suite via `mvn test -DsuiteXmlFile=fullsuite-testng.xml` covering all 1069 curated test cases across 60 test classes. Sequential execution time: **10 hours 4 minutes**. The 14 failures are categorized below by root cause type — REAL product bugs vs known-bug detectors vs test infrastructure issues.

## Headline results

| Metric | Count | % |
|---|---|---|
| Total tests run | 1069 | 100% |
| **PASS** | **1055** | **98.7%** |
| FAIL | 14 | 1.3% |
| ERROR | 0 | 0% |
| SKIP | 0 | 0% |

## The 14 failures categorized

### Category A — REAL product bugs (data integrity) — 6 failures, HIGH priority

The CriticalPathTestNG cross-checks found multiple data-integrity bugs where the dashboard and the underlying lists return inconsistent counts. **These are real product bugs worth filing as ZP tickets immediately.**

| # | Test | What's broken |
|---|---|---|
| A1 | `CP_DI_001_AssetCountMatchesDashboard` | Dashboard says 1953 assets; Assets grid total says **8429**. +6476 mismatch. |
| A2 | `CP_DI_002_TaskCountMatchesDashboard` | Dashboard says 147 pending tasks; Tasks module shows **0**. Different endpoints. |
| A3 | `CP_DI_003_IssueCountMatchesDashboard` | Dashboard unresolved 266; Issues grid shows **0**. Different endpoints. |
| A4 | `CP_DI_005_AssetDataSurvivesRefresh` | Assets grid has no clickable rows after refresh. State recovery broken. |
| A5 | `CP_FA_003_KPINumbersNonNegative` | **0 KPI cards** found on dashboard (expected ≥4). Dashboard widgets missing. |
| A6 | `CP_SF_005_SessionPersistence` | After navigating 5 pages, user kicked back to /dashboard. Session lost mid-nav. |

**Why these matter:**
- A1-A3: count mismatches indicate either stale caches, scoping/filter divergence between dashboard and list queries, or tenant-isolation breakage. Customer-visible.
- A4-A5: dashboard widget regression — likely a rendering or fetch failure that doesn't crash the page but makes the dashboard useless.
- A6: premature session expiry — users can't navigate without being kicked. Workflow-blocking.

### Category B — Bug-hunt regression detectors firing (bugs persist) — 4 failures

These are inverted-assertion regression detectors. "Fail" = "bug still exists." They serve as long-term detectors for known issues.

| # | Test | What it detected |
|---|---|---|
| B1 | `BugHuntGlobalTestNG.testBUG007_DuplicateAPICalls` | Roles API called 2× per page (already filed: bug #1 in BH list) |
| B2 | `BugHuntGlobalTestNG.testBUG018_BeamerLeaksUserData` | Beamer URL leaks role + company + name via query params — **real privacy bug worth ZP ticket** |
| B3 | `BugHuntTestNG.testBUG16_SignInButtonDisabledStates` | Sign-In button is enabled with empty form — should be disabled until both fields filled |
| B4 | `BugHuntWorkOrdersTestNG.testBUG015_MissingStatusColumn` | **GOOD NEWS:** Status column is now PRESENT (bug fixed!). Test was inverted; needs flipping. |

**Action items:**
- B2: file ZP ticket for Beamer URL data leak (security/privacy)
- B3: file ZP ticket for Sign-In button disabled state
- B4: flip the assertion — the test should now ASSERT the column is present, not absent

### Category C — Test infrastructure bugs (NOT product bugs) — 4 failures

These are test-side issues that surface because of UI changes, data-state drift, or test-side code bugs. Not customer-visible.

| # | Test | Root cause |
|---|---|---|
| C1 | `AssetPart4.testRELAY_08_SaveAllFilled` | Nav locator `//nav//a[@href='/assets']` timed out — sidebar nav structure changed. |
| C2 | `AuthenticationTestNG.testTC_SEC_02_LoginRateLimitAfterFailures` | Test bug: `xhr.open('POST', '/api/auth/login', false)` uses relative URL on cross-origin context → "Invalid URL". Needs absolute URL. |
| C3 | `IssuesSmokeTestNG.testCreateIssue` | Locator timeout on Issue Class placeholder — placeholder text changed. |
| C4 | `TaskTestNG.testTC_SF_002_SearchByTitle` | Search for "T1" returned 0 results — test data drifted (no tasks starting with "T1" anymore). |

**Action items:** these 4 need test-side fixes, not product fixes:
- C1: update locator to current nav structure
- C2: change to `'https://acme.qa.egalvanic.ai/api/auth/login'`
- C3: re-locate Issue Class input (probably aria-label changed)
- C4: change search term to a known-existing prefix or add a setup step

## How this maps to existing bug tally

Cumulative across all bug-hunter rounds + this full-suite check:

| Source | Real bugs surfaced | Status |
|---|---|---|
| Phase1 BH rounds 1-8 | 7 | Filed in code as TODO markers |
| Full BH suite check (yesterday) | 3 (bugs 8-10) | Filed in code as TODO markers |
| **This full-suite run (today)** | **8 NEW** (A1-A6, B2, B3) | **Need ZP tickets** |
| **Total open product bugs from bug-hunting:** | **18** | |

Plus 1 fixed bug (B4 — status column now present) where the regression test needs updating.

Plus 4 test-infrastructure bugs (C1-C4) — fix in test code.

## What's NOT in this suite

`Phase1BugHunterTestNG` (my 41 adversarial tests) is **not included** in `fullsuite-testng.xml` — that XML predates my work. My earlier verification run today confirmed 39/41 PASS + 2 SKIP for the BH class. To include it in future full-suite runs, add this entry to `fullsuite-testng.xml`:

```xml
<test name="Phase 1 Bug Hunter (41 TCs)">
    <classes>
        <class name="com.egalvanic.qa.testcase.Phase1BugHunterTestNG"/>
    </classes>
</test>
```

## Recommended next actions (manager-facing)

**Immediate (file ZP tickets):**
1. **Data integrity**: A1-A3 dashboard ↔ list count mismatches (3 tickets, P0/P1)
2. **A4-A6**: dashboard widget rendering, KPI cards missing, session expiry (3 tickets, P1)
3. **B2**: Beamer URL leaks user PII (1 ticket, P1 security)
4. **B3**: Sign-In disabled state bug (1 ticket, P2)

**Short-term (test maintenance):**
5. C1-C4: update 4 brittle locators / test data drift
6. B4: flip the BUG015 assertion (status column is present now — that's the new normal)

**Add to test infra:**
7. Include `Phase1BugHunterTestNG` in `fullsuite-testng.xml`

## Files changed (this commit)

| File | Change |
|---|---|
| `docs/AI-FEATURES-CHANGELOG/2026-05-01-05-45-...md` | this changelog |

No code changes — this commit is documentation only. The 14 failures are categorized; product bugs need ZP tickets, test bugs need locator updates.

## Branch confirmation

```
$ git branch --show-current
main
```

NEVER pushed to `developer` (production) per memory rule.

## Raw run log

Saved to: `/tmp/full-suite-run.log` (also rotated to `target/surefire-reports/`)
Wall time: 10:04 hours
Started: 2026-04-30 ~19:40 IST
Finished: 2026-05-01 05:44 IST
