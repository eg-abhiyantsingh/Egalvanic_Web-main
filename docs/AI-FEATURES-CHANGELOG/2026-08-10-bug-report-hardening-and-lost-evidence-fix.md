# Hardening pass: lost-evidence root cause, honest reproducibility claims, all 10 suites wired

**Date:** 2026-08-10 (continuation of the same day's customer-bug-report build)
**Prompt:** "continue"

---

## 1. The defect that was silently destroying test evidence

**Symptom.** In CI run 31233370537, the Dashboard + BugHunt group produced **zero**
`reports/detail-report/*.html` while all 14 other groups produced 1–2. Its 105 TCs were
missing from every consolidated report — and the new customer bug report had to fall back
to heuristic steps for 14 failures.

**Root cause (verified with a standalone TestNG repro, not inferred).**
TestNG records a failed configuration method against its **declaring** class. Under the
default `configfailurepolicy=skip`, it then skips the remaining non-`alwaysRun`
configuration methods of that class. `classSetup` (`@BeforeClass`) and `suiteTeardown`
(`@AfterSuite`) are **both declared on `BaseTest`**, and `flushReports()` — the single
point where ExtentReports writes HTML — lived only in that `@AfterSuite`. One failing
`@BeforeClass` therefore discarded the entire run's evidence.

**The subtlety that made it look random.** A later *successful* `@BeforeClass` on the same
declaring class clears the failure record. So a mid-suite failure is "healed" and
`@AfterSuite` still runs; only a failure in the **last `<test>` block** leaves the record
dirty at suite end. Measured against this project's TestNG 7.8.0, mirroring the real shape
(base class + subclass that `@Override`s `classSetup` and throws):

| failing class position | plain `@AfterSuite` | `alwaysRun = true` |
|---|---|---|
| first | RAN | RAN |
| **last** | **did NOT run** | **RAN** |

`ArcFlashEngineeringE2ETestNG` — whose `classSetup` asserts on a hardcoded site name —
was the suite's last `<test>` block. Three independent corroborations in the job log:
surefire's two totals differ by exactly 14 (skipped config methods), zero "Detailed Report
generated" lines, and orphaned `chromedriver`/`chrome` at job cleanup (proving the sibling
`@AfterClass` was skipped too).

> This also corrected two wrong hypotheses along the way: it was **not** a killed JVM
> (surefire logged `Tests run: 271` and exited cleanly), and it was **not** the dashboard
> script deleting reports (it never touches `reports/detail-report`).

**Fix — belt and braces.**
- `BaseTest.suiteTeardown` → `@AfterSuite(alwaysRun = true)` (root-cause fix).
- `BaseTest.classTeardown` → `@AfterClass(alwaysRun = true)` + **incremental flush per
  class**, placed *before* `driver.quit()` so evidence lands even if quit hangs. `alwaysRun`
  alone cannot help a killed or cancelled JVM; the per-class flush caps the loss at one
  class instead of the whole run.
- `VisualRegressionTestNG` re-annotated its `classTeardown` override as plain `@AfterClass`,
  which **drops the base method's attributes** — so the fix would not have reached it.
  Now `@AfterClass(alwaysRun = true)`.
- `ExtentReportManager`: new `flushDetailedReportsOnly()` (same files, **no email**), and the
  flush loop now has a **per-module** try/catch — previously one module throwing would abort
  every remaining module *and* the client report, defeating the point of flushing early.

## 2. The generator was making a claim it had not earned

`reproduced_on_rerun` was computed as `bool(args.rerun_results)` — i.e. from *the CLI flag
being present*, not from the re-run actually having re-executed that test. Every bug in the
after-re-run PDF was therefore stamped **"Failed in the parallel run AND on a clean re-run
(reproducible, not flaky)"**.

Measured on the real 103-failure dataset with a partial re-run: **93 bugs claimed
reproducibility; only 10 had earned it — 83 fabricated claims**, in a document that this
same change starts emailing automatically. Config (setup) failures are never re-run at all,
so they could never earn it.

Fixed: the claim is now computed per test key (`re-run has this key AND it failed again`);
config failures always report `False`; an empty re-run directory marks nothing and prints a
warning.

Related honesty fix: a zero-bug report printed **"0 — all tests passed"** even when the
cause was that *no `testng-results.xml` was found at all*. That is a false all-clear. It now
distinguishes the two, and says explicitly that missing evidence is not a statement that the
application is defect-free.

## 3. Coverage and delivery
- Wired into the remaining **7** test workflows — smoke-tests, web-tests-smoke-repodeveloper,
  rbac (×4), rerun-failed-tests — for **10 suites total**. Each stages the correct
  `testng-results.xml` source: `reports/modules` for the dashboard-driven smoke suites (the
  dashboard wipes `target/surefire-reports` per module), `target/surefire-reports` for the
  direct-Maven RBAC suites.
- `consolidated-report.py` gained `--attach`; the after-re-run email now carries the PDF +
  DOCX in **both** parallel suites (suite 2's email step had to be reordered — it ran before
  the PDF existed).
- Attachment size cap corrected: `3/4` ignored that `encode_base64` wraps at 76 chars
  (+1.3%), so an 18.75 MB file encoded to **25.3 MB and still bounced**. Now 0.68.
- `pip install` hardened with a `--break-system-packages` fallback: on a runner image
  enforcing PEP 668 the install would fail, and `continue-on-error` would have silently
  removed the feature from every suite.

## 4. Regression protection
New `.github/scripts/test-customer-bug-report.py` — synthetic CI fixtures, **24 assertions**
covering all three evidence tiers, severity rules, re-run mode, the no-evidence case, and
the six defects found across both validation passes. Run it after touching the generator.

## Depth explanation (for learning / manager review)
- **Why the repro mattered more than the reasoning.** Two independent reviewers reached
  opposite conclusions about the TestNG mechanism, each claiming an experiment. The
  disagreement was resolved not by argument but by building a repro that mirrored the real
  code's *exact* shape — subclass `@Override` of the base `@BeforeClass`, failing class
  **last**. The position variable is what both earlier experiments missed, and it is also
  why this bug appeared intermittent for so long.
- **Why `alwaysRun` alone was not accepted as the fix.** It addresses the config-skip path
  only. A cancelled job or OOM still loses everything, because ExtentReports holds the whole
  run in memory until `flush()`. Incremental flushing is what actually bounds the loss.
- **Why the fabricated-claim bug is the most serious finding here.** Everything else
  degrades evidence quality; that one asserted something false *to a customer*, with the
  authority of automation, and the automation had just been wired to send it by email.
