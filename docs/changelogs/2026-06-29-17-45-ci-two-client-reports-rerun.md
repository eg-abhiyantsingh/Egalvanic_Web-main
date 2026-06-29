# CI: re-run failed tests and emit a SECOND ("after re-run") client report

- **Date:** 2026-06-29 17:45
- **Prompt:** "After collecting all the failed test cases, re-run them and share me an updated client report
  after they pass on re-run, so I get two client reports — one before re-run, one after re-run. Do this for
  CI/CD in web."
- **Type:** New CI/CD capability (both web parallel suites).

---

## What it does
Each parallel-suite run now produces **two client reports**:
1. **Before re-run** — the existing consolidated client report from the first pass (with any failures).
2. **After re-run** — a new report that re-runs **only** the just-collected failed tests and shows the
   **full suite** with each re-run test's fresh outcome substituted in (recovered → PASS, still-broken →
   FAIL).

## How it works
The suites already collect failures into `failed-suites/failed-tests-latest.xml` (a TestNG suite of the
failed methods). Added a new **`rerun-failed`** job to both `parallel-suite.yml` and `parallel-suite-2.yml`:
- Runs only when the first pass had failures (`needs: summary`, `if: overall_status == 'partial'`).
- Downloads the first pass's reports + the collected `failed-tests-latest.xml`.
- Re-runs them headless (`mvn test -DsuiteXmlFile=failed-suites/failed-tests-latest.xml -Dheadless=true`).
- Regenerates the client report with the re-run results **overriding** the originals.
- Uploads it as a distinct artifact (`consolidated-client-report-suite2-after-rerun` /
  `consolidated-client-report-after-rerun`) and emails it (subject suffixed " (After Re-run)") when email
  is enabled. A step-summary reports "failed in first pass → still failing after re-run".

## The override mechanism
`consolidated-report.py` gained a **`--rerun-dir <dir>`** flag: a re-run result for the same
`(class, method, params)` replaces the original status/duration, so a previously-failing test that now
passes flips FAIL→PASS in the after report. (This relies on the new stable, readable `Module.toString()`
key — see the report-accuracy changelog — so matrix cells match across the two runs.) Also added an
optional `EMAIL_SUBJECT_SUFFIX` env so the second email is distinguishable.

## Safety / non-breaking
- The first report and its flow are unchanged; the new job is additive and auto-skips on all-green runs.
- Re-run results are uploaded as a separate artifact and are **not** merged back into the committed
  failed-tests accumulator, so they can't corrupt the running tally.
- YAML validated for both workflows.
