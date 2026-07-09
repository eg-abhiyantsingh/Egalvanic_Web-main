# Consolidated API test report for the Suite 3 artifact

**Date:** 2026-07-09 11:09 UTC
**Author:** QA automation (Claude Code, Fable 5)
**Branch:** main

## What was asked
> "i cant see pagination and other test case for api testing in artifact only api performance testing
> report is there … api-catalog-report.html this is the correct file but it dont have pagination and
> other api testing result"

## Finding
The Suite 3 artifact already contained every area's raw `.md` report and per-class ExtentReports HTML,
**but the only polished, styled HTML pages were `api-health-report.html` (the ~50-endpoint health check)
and `api-catalog-report.html` (the ~1077-op probe)** — each covers ONE area. So opening the artifact
looked "performance-only"; pagination, agent-token, and the other contract results were buried in raw
markdown / per-class detail files. Also, my new `list-api-catalog-report.md` (spec-driven pagination
catalog) was never added to the artifact upload list.

## What changed
1. **New `.github/scripts/gen_api_suite_report.py`** — builds a single styled landing page,
   `reports/api-suite-report.html`, covering **every** Suite 3 area:
   - Parses `target/surefire-reports/testng-results.xml` for authoritative per-area pass/fail/skip.
   - Summary table (area → tests/pass/fail/skip/verdict) + a section per area that **inlines that
     area's markdown findings** (pagination curated + catalog-wide, error/transport, security, CRUD,
     input-validation, filter/search, mutation semantics, health, full catalog).
   - The agent-token contract (no markdown file) is included from its test counts with a pointer to
     the ExtentReports detail HTML.
   - Theme-aware CSS, self-contained, degrades gracefully if an input is missing.
   - Uses `defusedxml` when present (XXE-safe), falls back to stdlib for the trusted build-generated XML.
2. **`.github/workflows/parallel-suite-3.yml`**:
   - Renders `api-suite-report.html` after the run.
   - Lists it **first** in the `api-health-report` artifact, and **adds the missing
     `list-api-catalog-report.md`**.
   - Run summary now leads with a pointer to the consolidated report + inlines the pagination-catalog
     findings.

## Validation (local, real multi-class results)
Ran a 3-class subset (`ListApiCatalogAuditTest` + `ListApiContractApiTest` + `AgentsContractApiTest`)
→ `testng-results.xml`, then generated the report. Per-area counts exact:
`List Pagination — Curated 7/7`, `List Pagination — Catalog-wide 187 (99 pass / 88 skip)`,
`Agents Contract 93/93`; all 12 areas render, pagination findings shown as a full table with per-endpoint
VIOLATION verdicts. HTML parses clean; workflow YAML valid.

## How to use
In the `api-health-report` artifact, open **`api-suite-report.html`** — it is now the single landing
page for all API test areas. The individual `api-health-report.html` / `api-catalog-report.html` remain
for the deep per-area views.

## Files
- `.github/scripts/gen_api_suite_report.py` (new)
- `.github/workflows/parallel-suite-3.yml`
