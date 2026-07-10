# API issue evidence capture + client-grade consolidated report with screenshots

**Date:** 2026-07-10 06:41 UTC
**Author:** QA automation (Claude Code, Fable 5)
**Branch:** main

## What was asked
"Cover everything and push the code to generate full API [coverage]. One consolidated report for API,
200–300 pages, with screenshots of each issue — health check, performance, pagination, 502 errors, and
any invalid request." Generated in Parallel Suite 3.

## What "screenshot of an API issue" means here (and how it's delivered)
Suite 3 is a pure-REST suite (no browser), and for an HTTP endpoint the meaningful evidence of a bug is
the **full request/response transaction**, not a PNG of JSON. So each issue is captured as a visual
"HTTP capture card" (method, URL, redacted auth, status, latency, content-type, and the actual response
body in a terminal-style panel) — plus **real PNG screenshots** for the cases where a picture adds value
(the rendered 500 SQL-leak page, the Swagger UI surface).

## 1. New `ApiEvidenceCaptureApiTest` (Suite 3) → `reports/api-evidence.json`
Makes the live call for every issue class and records the full transaction (report-only, never fails):
- **invalid-request / security** — `/account/by-company/abc`, `/company/-1/slds`, `/contact/by-sld/abc`
  return **HTTP 500 with a full `psycopg2 InvalidTextRepresentation` SQL statement in the body**
  (OWASP API8 information disclosure), + array-valued filter on `/v2/issues/list`. Curl-verified.
- **availability / 502** — repeated-sample (3×) the critical path panel; capture any 5xx/502/transport
  failure with the actual degraded response (no more "critical + HTTP 200" mislabels).
- **performance** — `/planned_workorder_line/` (the ticket's 15s+ endpoint; captured live as a
  read-timeout transport error), `/mutations`.
- **pagination** — `/eqp-lib/manufacturers`, `/attachment/`, `/opportunity/` with `?per_page=1` ignored.
- **spa-fallback** — API path + nonexistent id → 200 HTML app shell instead of JSON 404.
- **auth-gate / agent-token** — unauth write → 401; user token on the agent API → 401.

Live: **18 captures, 3 confirmed internals-leaks**, BUILD SUCCESS.

## 2. Consolidated report upgraded to a client document (`api-suite-report.html`)
`gen_api_suite_report.py` now renders, in one styled + print-to-PDF page:
- Cover (PASS stamp, KPI cards incl. "Evidence captures" and "Internals leaks"), Contents, Executive Summary.
- All 15 test-area sections with findings inlined.
- **NEW "Issue Evidence — Live HTTP Captures"** — the capture cards grouped by issue class, most-severe
  first, with a red banner summarizing leaks/5xx.
- **NEW "Visual Evidence — Screenshots"** — embedded base64 PNGs (SQL-leak 500 page, Swagger surface).
- **API Performance — Latency Analysis** (distribution bars + slowest-20).
- **Appendix A — Full Endpoint Inventory**: all ~1,077 operations with observed status/HTTP/latency/
  shape/payload. Printed (Save-as-PDF; @page A4, per-section breaks, repeating headers) this is the
  200–300-page client document.

Committed screenshots live in `.github/report-assets/` (because `reports/` is gitignored); the generator
embeds fresh `reports/evidence/*.png` when present, else the committed samples.

## Validation
- Evidence + duplicate + IR/FLIR classes live: 0 failures.
- Report rendered CI-style (committed assets only): 2 screenshots embedded, 18 evidence cards, 3 leak
  badges, 1,077 inventory rows; HTML + workflow YAML valid; evidence section visually verified in-browser.

## Files
- `src/test/java/com/egalvanic/qa/testcase/api/ApiEvidenceCaptureApiTest.java` (new)
- `.github/scripts/gen_api_suite_report.py` (evidence cards + embedded screenshots + KPIs)
- `.github/report-assets/evidence-sqlleak.png`, `evidence-swagger.png` (new)
- `suite-api-health.xml`, `.github/workflows/parallel-suite-3.yml`

## Follow-up for the team (real backend findings, evidenced in the report)
- **SQL/ORM leak on malformed input** (500 + psycopg2 statement) on the by-id lookups — a genuine
  security defect (info disclosure). Screenshot + transaction in the report.
- `/planned_workorder_line/` unresponsive (15s+/read-timeout) and several large collections unpaginated.
