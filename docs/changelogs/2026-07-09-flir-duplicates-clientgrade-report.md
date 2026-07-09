# FLIR/IR pipeline coverage + duplicate-endpoint audit + client-grade Suite 3 report

**Date:** 2026-07-09 14:27 UTC
**Author:** QA automation (Claude Code, Fable 5)
**Branch:** main

## What was asked
1. Are we covering the new FLIR Camera visual-generation API (ir_photo_key / visual_photo_key /
   photo_type=FLIR-IND / platform)?
2. Are we checking for duplicate APIs across the swagger surface?
3. One comprehensive client-facing report in Parallel Suite 3 covering everything (health, testing,
   duplicates, pagination, performance…), print-to-PDF ready.

## 1. FLIR — KEY FINDING: the feature is ALREADY DEPLOYED on QA (ZP-3112)
`POST /ir_photo/create_and_extract` is live and its swagger description literally says
"FLIR camera mobile app (ZP-3112)"; `POST /ir_photo/{photo_id}/extract_visual` references
`visual_photo_key`; there's also `/ir_photo/extract_visual/batch`. Verified live (curl):
- unauth POST → **401 "No authorization provided"** (auth-gated ✓)
- authed, bad/missing type → **400 "type must be 'IR' or 'VISUAL'"** (clean validation, no 5xx, no leak ✓)

Contract nuance for the ticket owner: the deployed endpoint validates **`type ∈ {IR, VISUAL}`**, not the
ticket's `photo_type=FLIR-IND` — either the ticket's exact param shape is still coming or the mobile
contract differs from the deployed one. Worth reconciling with backend.

**New `IrFlirContractApiTest`** (Suite 3):
- Auth-gate assertions on EVERY `ir_photo`/`ir_session` mutation (spec-driven data provider).
- `GET /ir_session` answers JSON (not the SPA shell); `GET /ir_photos/{unknown}` never 5xx / no SQL leak.
- **FLIR contract WATCH** — scans the spec each run for flir/ir_photo_key/visual_photo_key ops. It
  auto-activated (found the deployed endpoints) and asserted: unauth 401, invalid type → 400-class,
  missing params → 4xx. If the FLIR-IND-shaped endpoint lands later, it is covered with zero code change.
- Live: all green (16 tests with the duplicate audit, 0 failures).

## 2. Duplicate APIs — new spec-level audit (+ the existing runtime one)
Two different "duplicate" problems, now both covered:
- **Runtime duplicate CALLS** (same endpoint refetched on one page load) — already covered by the
  browser-driven `ApiDuplicateCallTestNG` in Suite 2's `api` toggle (latest: 21 redundant logical
  endpoints at 3–4×/load + 68 exact-URL duplicates). Needs Chrome, so it stays in Suite 2; its report
  is included in the consolidated report when present.
- **Spec-level duplicate DEFINITIONS** — new `DuplicateApiAuditTest` in Suite 3 (pure spec analysis):
  dash/underscore twins, trailing-slash twins, singular/plural root families, v1/v2 overlaps.
  **Live findings (945 paths): 26 total** — 1 CRITICAL dash/underscore twin
  (`/planned-workorder-line/{line_id}` AND `/planned_workorder_line/{line_id}` both live — two
  registrations that drift independently), 12 singular/plural families (account/accounts,
  agent/agents, user/users, ir_photo/ir_photos…), 13 v1/v2 overlaps. Report:
  `reports/api-duplicate-endpoints-report.md`; `-DSTRICT_SPEC_HYGIENE=true` gates exact twins.

## 3. Client-grade consolidated report (`api-suite-report.html`)
Rebuilt `gen_api_suite_report.py` into a print-to-PDF-ready client document:
- **Cover page** (title, PASS/FAIL stamp, KPI cards, environment, coverage line) + **Contents** +
  **Executive Summary** (area × verdict table).
- Per-area sections for all **15 areas** (now incl. Duplicate-Endpoint Audit, IR/FLIR Pipeline,
  Runtime Duplicate Calls) with each area's findings inlined.
- **API Performance — Latency Analysis**: distribution bars across all probed ops + slowest-20 table.
- **Appendix A — Full Endpoint Inventory**: all ~1,077 operations with observed status/HTTP/latency/
  shape/payload — the per-endpoint evidence table (the API-testing equivalent of a screenshot per
  endpoint). Printed, this appendix alone is 200+ pages.
- **@media print CSS**: A4 pages, cover break, per-section page breaks, repeating table headers —
  browser "Save as PDF" produces the full paginated client document.
- On screenshots: Suite 3 is deliberately browser-less (pure REST), so the "visual evidence" for API
  testing is the captured per-request data (status/latency/shape/payload per endpoint) plus the
  ExtentReports per-test logs in `detail-report/`. Browser screenshots exist in Suite 2's UI flows.

## Validation
- `DuplicateApiAuditTest` + `IrFlirContractApiTest` live: 16 tests, 0 failures, BUILD SUCCESS.
- Report rendered locally from the real CI catalog data: 14–15 areas, 1,077 inventory rows, HTML valid,
  visually verified (cover page screenshot) via Playwright.
- Both new classes wired into `suite-api-health.xml`; new report md added to the workflow artifact.

## Files
- `src/test/java/com/egalvanic/qa/testcase/api/DuplicateApiAuditTest.java` (new)
- `src/test/java/com/egalvanic/qa/testcase/api/IrFlirContractApiTest.java` (new)
- `.github/scripts/gen_api_suite_report.py` (client-grade rewrite)
- `suite-api-health.xml`, `.github/workflows/parallel-suite-3.yml`
