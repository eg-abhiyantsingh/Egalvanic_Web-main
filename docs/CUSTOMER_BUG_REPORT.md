# Customer Bug Report — per-run, per-suite

Every CI suite run automatically produces a **shareable bug report**: one formal bug
per failed test case, with real reproduction steps and screenshots, as **PDF + DOCX
+ JSON**. Nothing to run by hand.

---

## Where to get it after a run

Open the run in GitHub Actions → **Artifacts** (bottom of the summary page).

| Suite | Artifact to download | When to use it |
|---|---|---|
| Parallel Full Suite | `customer-bug-report-after-rerun` | **Send this to the customer.** Only failures that reproduced on a clean re-run. |
| Parallel Full Suite | `customer-bug-report` | Internal triage — all first-pass failures, including flaky ones. |
| Parallel Suite 2 | `customer-bug-report-suite2-after-rerun` / `…-suite2` | same split as above |
| Full Suite (sequential) | `customer-bug-report` | single pass, no re-run |
| Smoke / RBAC / re-run suites | `customer-bug-report-<suite>` | per-suite editions |

Each artifact contains:

```
Customer_Bug_Report.pdf     the deliverable (send this)
Customer_Bug_Report.docx    editable copy, if you need to reword before sending
Customer_Bug_Report.json    machine-readable bug list (tooling / Jira prep)
```

> **Which file do I send?** The **after-re-run PDF**. A test that failed in the
> parallel pass but passed on a clean re-run is flaky/environmental, not a product
> bug — those are excluded from the bug list and disclosed in an appendix instead.

---

## What one bug looks like

Every bug uses the agreed template:

```
BUG-007   [Work Order] Asset name text overlaps on Asset Details page

Environment           QA
Platform              Web
Browser/App Version   Google Chrome (latest stable, GitHub Actions Linux runner)
URL                   https://acme.qa.egalvanic.ai
Device                Desktop (CI: Linux, 1920×1080)
Severity              Medium — Functional assertion failed. Reproduced on re-run (not flaky).
Priority              Medium
Reproducibility       Failed in the parallel run AND on a clean re-run

Preconditions
  • User is logged in with a standard QA account.
  • User has access to the Work Order module.

Steps to Reproduce
  1. Navigate to Work Order Planning (/planning)
  2. Open the row action menu for the first plan
  3. Click Delete, then click Cancel in the confirmation dialog
  4. Observe the failure described under "Actual Result".

Actual Result     <assertion / error evidence, cleaned of Java internals>
Expected Result   <reconstructed from the assertion or the @Test description>

Technical evidence (for the dev team)   <first 12 stack frames>
Attachments                             <failure screenshot + preceding step screenshots>
Test reference: com.egalvanic.qa.testcase.WorkOrderPlanningTestNG#testTC_WOP_025_CancelDelete
```

The report opens with a cover page (environment, run link, bug counts by severity)
and a summary table of every bug, so a reader can scan before diving in.

---

## Where the content comes from

Three evidence tiers, best available wins per test — the report is never empty:

1. **ExtentSpark detailed report** (`reports/detail-report/Detailed_Report_*.html`)
   → the **real steps the test executed** plus inline screenshots. This is the good
   tier; steps read like a human wrote them because the framework logged each action.
2. **`testng-results.xml`** → exception message, `@Test` description, parameters,
   timing. Retry-aware: later results override earlier ones for the same
   `(class, method, params)`.
3. **`test-output/screenshots/<method>_FAIL_*.png`** → matched by method name and the
   invocation's time window, for tests that never reached the Extent report. Steps
   then fall back to a heuristic derived from the test name and description.

**Severity** is deliberately conservative (we do not over-report to customers):

| Severity | Assigned when |
|---|---|
| High | Real server-side signals (`status of 5xx`, `Internal Server Error`, `psycopg2`, `SQLSTATE`) or a test-class setup failure that blocked a whole class |
| Medium | Functional assertion failures; timeouts and element failures (with an honest note that timing may be a factor) |
| Low | reserved — not currently auto-assigned |

Priority mirrors severity. **Always sanity-check severity before sending to a
customer** — the heuristic is intentionally cautious, not authoritative.

---

## Running it locally

Useful when you want the report for a past run without waiting for CI.

```bash
pip install reportlab pillow python-docx defusedxml

# 1. download a run's report artifacts
gh run download <RUN_ID> -R eg-abhiyantsingh/Egalvanic_Web-main \
   --pattern "reports-*" --dir all-reports

# 2. generate
python3 .github/scripts/customer-bug-report.py all-reports out \
   --title "Customer Bug Report — Parallel Full Suite (run <N>)" \
   --run-number <N>

open out/Customer_Bug_Report.pdf
```

Useful flags:

| Flag | Effect |
|---|---|
| `--rerun-results <dir>` | only file failures that reproduced; recovered → appendix |
| `--rerun-detail <dir>` | prefer the re-run's fresh steps/screenshots |
| `--extra-screenshots <dir>` | index an extra screenshots dir (repeatable) |
| `--max-screenshots N` | attachments per bug (default 4) |
| `--max-bugs N` | safety cap (0 = unlimited) |
| `--no-stack` | drop the technical stack-trace section |
| `--formats pdf` | skip DOCX |
| `--environment` / `--base-url` / `--app-version` | override the Environment block |

---

## Guarantees and limits

- **Cannot break a suite.** Every CI step is `continue-on-error: true` + `if: always()`,
  and runs in the summary/re-run job — test jobs are untouched.
- **Scale.** Validated at 103 bugs → 210-page, 1.5 MB PDF. Screenshots are
  re-encoded to JPEG (≤1300 px) so a large report stays emailable.
- **One bug per failed invocation**, so a data-driven test failing on 4 parameter sets
  yields 4 bugs — the bug count matches the failure count CI reports.
- **Jira is never touched automatically** (project rule). The JSON is there if you
  want to prepare tickets, but filing stays a human decision.

## Regression protection

`.github/scripts/test-customer-bug-report.py` builds synthetic CI fixtures and asserts
on the generated report — evidence tiers, severity rules, re-run mode, the all-green
case, and four specific defects found during the 2026-08-10 validation (false-High from
Selenium's "500 milliseconds" text, Java lambda refs in customer text, duplicate
data-driven titles, `[Ljava.lang.String;@…` array refs). Run it after touching the
generator:

```bash
python3 .github/scripts/test-customer-bug-report.py    # 21 checks, exit 0 = green
```
