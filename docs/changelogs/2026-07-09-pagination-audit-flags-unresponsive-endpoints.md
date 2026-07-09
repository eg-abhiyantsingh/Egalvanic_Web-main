# Pagination audit now flags unresponsive endpoints (e.g. /planned_workorder_line/)

**Date:** 2026-07-09 12:27 UTC
**Author:** QA automation (Claude Code, Fable 5)
**Branch:** main

## Context
Question raised about `/planned_workorder_line/` (very slow, ~15–30s) and whether our tests cover it /
whether it should be paginated.

## Scope note
The `?page/per_page` **implementation** is a backend change in the API service (`eg-pz-frontend`), which
is read-only upstream from this QA-automation repo — it cannot be implemented here. This change is the
QA side: making sure our suite **detects and reports** the problem.

## Finding
- `ApiFullCatalogHealthApiTest` already flagged `GET /planned_workorder_line/` as **~30s → unresponsive**
  (warn + "critical: read timed out after 30s"). So Suite 3 did surface the slowness.
- But `ListApiCatalogAuditTest` (the pagination audit) **silently skipped it**: its discovery step does a
  default GET and, on a timeout (`status 0`), threw `SkipException("not an accessible collection")`. So
  the one endpoint that most needs a "must paginate" finding was dropped *because* it was too slow to
  classify. That's the gap this fixes.

## Change (`ListApiCatalogAuditTest`)
- `Probe` now detects socket/read timeouts (`timedOut`).
- On a base-probe timeout the audit **retries once**, and if it still times out it records a **distinct
  `UNRESPONSIVE` finding** ("GET timed out after ~Nms; too slow to classify; likely an unbounded
  collection — add ?page/per_page") instead of skipping.
- `UNRESPONSIVE` is deliberately a **separate category from confirmed pagination `VIOLATION`s** and is
  **not STRICT-gated**: under the suite's 6-thread parallel load a slow-but-fine endpoint can also time
  out, so this is a "couldn't audit — verify manually" advisory, not a hard bug. Calibration: the
  single-probe CI catalog had ~18 total timeouts; the multi-probe audit sees more purely from load, so
  conflating them with real violations would cry wolf.
- Report summary now reads e.g. "112 audited — 32 pagination violation(s), 15 unresponsive/too-slow (advisory)".

## Validation (live, 3-class run)
BUILD SUCCESS, 287 tests, 0 failures. `/planned_workorder_line/` now appears in
`reports/list-api-catalog-report.md` as `UNRESPONSIVE | TIMEOUT 30070ms` with the pagination
recommendation. Summary: 32 confirmed pagination violations + 15 unresponsive advisories, cleanly split.

## Answer to "did we cover this too?"
Yes — `/planned_workorder_line/` is covered in two places now: the full-catalog health probe (latency
warn) and the pagination catalog audit (UNRESPONSIVE + explicit "needs ?page/per_page" recommendation).
The backend fix to actually add the params is a separate `eg-pz-frontend` PR.

## Files
- `src/test/java/com/egalvanic/qa/testcase/api/ListApiCatalogAuditTest.java`
