# 2026-08-10 — Customer Bug Report PDF/DOCX on every CI suite run

**Request:** after every CI/CD suite run, collect all failed test cases and produce
ONE shareable PDF/Word file — one formal bug per failed test (Title / Environment /
Preconditions / Steps to Reproduce / Actual / Expected / Severity / Priority /
Attachments) with real steps and screenshots — customer-shareable.

## Changes
- **NEW** `.github/scripts/customer-bug-report.py` — generator. Evidence tiers:
  ExtentSpark detail HTML (real steps + inline screenshots) → testng-results.xml
  (retry-aware exception/description/params) → `*_FAIL_*.png` screenshots matched
  by method + time window. Re-run aware: with `--rerun-results`, only reproducible
  failures are filed; recovered tests go to a flaky appendix. Outputs PDF + DOCX +
  JSON.
- **parallel-suite.yml** — summary job generates + uploads `customer-bug-report`;
  rerun-failed job generates + uploads `customer-bug-report-after-rerun`.
- **parallel-suite-2.yml** — same, artifacts `customer-bug-report-suite2[-after-rerun]`.
- **full-suite.yml** — single-job variant, artifact `customer-bug-report`.
- Run step summaries list the new artifacts.

## Validation
- Real artifacts of run 31233370537: 103 bugs (102 failures + 1 classSetup),
  210-page PDF, 102/103 with embedded screenshots — pages visually verified.
- Synthetic re-run: 103 → 93 bugs, 10 recovered in appendix. All-green input →
  valid 0-bug PDF. Fixed during validation: false-High severity regex, Java
  lambda/array-ref noise in titles, duplicate data-driven titles.
