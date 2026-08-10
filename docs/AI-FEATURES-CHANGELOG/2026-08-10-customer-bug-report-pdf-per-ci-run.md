# Customer Bug Report (PDF + DOCX) generated automatically on every CI suite run

**Date:** 2026-08-10
**Time:** ~08:00–09:30 UTC
**Prompt:** "Whatever test cases fail in the full parallel suite — collect them after every CI/CD run and share ONE PDF/Word file with all steps to reproduce, screenshots, test-case names, in the formal bug template (Title/Environment/Preconditions/Steps/Actual/Expected/Severity/Priority/Attachments), so the report can be shared with the customer. For every suite, for every run."

---

## What was built

### 1. `.github/scripts/customer-bug-report.py` (new, ~950 lines)
A per-run report generator that converts CI test failures into a **customer-ready
bug report**: one formal bug per failed test case, screenshots embedded, delivered
as `Customer_Bug_Report.pdf` + `.docx` + `.json`.

Each bug follows the agreed template exactly:

| Field | Where it comes from |
|---|---|
| **Title** `[Module] short issue` | Module from the Extent report filename (or class name); issue phrase from the assertion message head (e.g. "Plan should remain after cancelling delete"), console-error summary, or the `@Test` description |
| **Environment** | QA / Web / Chrome (CI runner) / base URL / CI run number + link |
| **Preconditions** | logged-in QA user + module access (+ setup-failure note for config failures) |
| **Steps to Reproduce** | **The REAL executed steps** parsed out of the ExtentSpark detailed-report HTML (`li.test-item` → `tr.event-row` rows). Fallback: heuristic steps from method name + `@Test` description |
| **Actual Result** | exception/assertion message, sanitized (Java lambda refs, `Build info:` blocks removed) |
| **Expected Result** | reconstructed from `expected [X] but found [Y]` or the `@Test` description |
| **Severity / Priority** | conservative heuristic — High only for real server-side signals (`status of 5xx`, `Internal Server Error`, `psycopg2`, `SQLSTATE`) or class-setup failures; timeouts/element failures stay Medium with an honest note |
| **Attachments** | failure screenshot always + last step screenshots (cap 4/bug), recompressed to JPEG ≤1300px |
| **Test reference** | `FQCN#method [params]` footer for QA traceability |

### 2. Three-tier evidence model (degrades gracefully, never empty)
1. **ExtentSpark detail HTML** — real steps + inline base64 screenshots + stack trace.
   Matched to failures via the `Test failed: <method>` caption and `at fqcn.method(`
   stack frames, then display-name/params heuristics.
2. **testng-results.xml** — exception, description, params, timing. **Retry-aware**:
   later files override earlier per `(class, method, params)` — identical merge
   semantics to `consolidated-detailed-report.py`, so all reports agree.
3. **`test-output/screenshots/<method>_FAIL_*.png`** — matched by method-name prefix
   + the invocation's time window (for tests that never reached the Extent report —
   in the validation run: ArcFlash/BugHunt/NewModulesSmoke classes).

### 3. Re-run awareness (the customer-ready edition)
With `--rerun-results` + `--rerun-detail`, only tests that failed the parallel pass
**AND** the clean re-run become bugs (flagged "Reproducibility: reproducible, not
flaky"); recovered tests move to an appendix ("failed once but recovered on re-run —
not filed as bugs"). This enforces the project rule of not over-reporting flaky
failures to the customer.

### 4. CI wiring (every suite, every run)
- **parallel-suite.yml** — `summary` job → artifact `customer-bug-report`
  (first pass); `rerun-failed` job → artifact `customer-bug-report-after-rerun`
  (reproducible failures only — the one to share).
- **parallel-suite-2.yml** — same two steps → `customer-bug-report-suite2` /
  `customer-bug-report-suite2-after-rerun`.
- **full-suite.yml** — single-job variant with staged input dir → `customer-bug-report`.
- parallel-suite-3.yml untouched (API health monitor, no UI tests/screenshots).
- Steps are `continue-on-error: true` — report generation can never fail a suite.
- Run summaries now list the new artifacts.

## Validation (real data, not just compile)
- Downloaded run **31233370537** artifacts (159 MB, 15 groups): **102 failed
  invocations + 1 classSetup config failure → 103 bugs**, PDF 1.5 MB / 210 pages,
  DOCX 0.8 MB. 88 bugs with real Extent steps, 15 via fallback; **102/103 with
  screenshots embedded** (verified by rasterizing pages — real app screenshots
  render crisply with captions).
- Synthetic re-run (flipped half the FAILs to PASS): 103 → 93 bugs, 10 recovered
  correctly listed in the appendix.
- Edge cases: all-green run → valid "0 bugs" PDF; missing `--rerun-detail` /
  `--extra-screenshots` dirs → graceful.

## Bugs found & fixed during validation (why real-data validation matters)
1. **False High severity** — `\b50[0-9]\b` matched "tried … with **500** milliseconds
   interval" in Selenium timeout text → severity regex now requires an HTTP-status
   context (`status of 5xx`, `HTTP 5xx`, …).
2. **Java noise in customer text** — `OpportunitiesPage$$Lambda$636/0x…@57b75756`
   appeared in titles → `sanitize_customer_text()` strips lambda/objref/`Build info:`
   noise; timeout titles now use the `@Test` description instead.
3. **Duplicate titles** — data-driven variants produced 4× identical titles →
   unique test-case id suffix appended when titles collide.
4. **Array refs in params** — `[Ljava.lang.String;@a22c4d8` leaked into titles →
   shared `PARAM_NOISE_PATTERN` now also matches `[L…;@hex` array refs.

## Depth explanation (for learning / manager review)
- **Why parse the Extent HTML instead of re-generating steps?** The framework
  already logs every real UI action (`ExtentReportManager.logStep…`) into the
  detailed report — those are the *true* reproduction steps, far better than
  guessing from a method name. The generator's job is extraction + reformatting,
  not new capture, so it adds ~0 runtime to test jobs (it runs in the summary job).
- **Why one bug per failed invocation (not per method)?** CI counts failures at
  invocation level (a data-driven method failing on 4 params = 4 failures), so the
  PDF's bug count matches the CI failure count the user sees — "200 fail = 200 bugs".
- **Why PDF via reportlab (not "Save as PDF" of an HTML)?** The summary job has no
  browser; reportlab+Pillow builds a deterministic, pageable document with
  embedded recompressed JPEGs (103 bugs → 1.5 MB, sharable by email).
- **Security:** defusedxml preferred, stdlib fallback refuses DTD/ENTITY docs;
  only system-controlled `${{ github.* }}` values are interpolated in workflows.
