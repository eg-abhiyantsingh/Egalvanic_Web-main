# Catalog-wide (spec-driven) pagination audit for Parallel Suite 3

**Date:** 2026-07-09 09:08 UTC
**Author:** QA automation (Claude Code, Fable 5)
**Branch:** main

## What was asked
> "pagination api testing is still not present in parallel suite 3"

## Finding
Pagination testing **was** present in Suite 3 (`ListApiContractApiTest` + `PaginationBehaviorApiTest`
both run and pass) — but it only covered a **curated 7 endpoints** (`node_classes`, `edge_classes`,
`issue_classes`, `company.opportunities`, `company.slds`, `sld.library-designations`, `lookup.nodes`).
Against the live catalog that is far too narrow: an empirical probe of the 187 param-free GET roots
found **104 are JSON collections**, and only 1 of them was audited. Large collections were completely
unaudited for the pagination contract:

| Endpoint | Records | Paginated? | Max-limit? | Default |
|---|---|---|---|---|
| `/action-items/` | 870 total | NO | yes | 20 |
| `/eqp-lib/manufacturers` | 448 | NO | NO | 448 UNBOUNDED |
| `/reporting/page-templates` | 366 | NO | NO | 366 UNBOUNDED |
| `/attachment/` | 217 | NO | NO | 217 UNBOUNDED |
| `/opportunity/` | 193 | NO | NO | 193 UNBOUNDED |
| `/skm-library/manufacturers` | 104 | NO | NO | 104 UNBOUNDED |

## What changed
New **`ListApiCatalogAuditTest`** — spec-driven, applies the SAME eGalvanic list contract as the
curated test to **every** collection the live `/api/swagger.json` exposes (enumerated at run time, so
new list endpoints are audited automatically):
- paginated (`page/per_page` or `limit` honored), bounded default (≤100, ideal ≤20), max-limit capped
  (a `per_page=1000` may not dump the table), filter/search doesn't 5xx, responds within a few seconds.
- **Discovery, not shape-enforcement:** a candidate that isn't a JSON collection (single object / HTML
  / error / empty) is SKIPPED, never failed. Only real collections are audited.
- **Signal over noise:** "NOT paginated" / "unbounded default" is only a VIOLATION when the collection
  is actually large (total > 100). Small fixed lists (enums, lookups, the 2-element `roles[]` on
  `/auth/me`) are advisories at most — the report flags real unbounded-growth risk, not every array.
- **Report-mode** (keeps the green-by-default Suite 3 monitor green); `-DSTRICT_LIST_API_CONTRACT=true`
  — the SAME gate the curated test uses — escalates violations to hard failures. GET-only, zero
  mutation risk. Findings → `reports/list-api-catalog-report.md`.
- Wired into `suite-api-health.xml` as "List-API Pagination Catalog (spec-driven)".

## Validation (live against `acme.qa.egalvanic.ai`)
`ListApiCatalogAuditTest`: **187 candidates → 104 audited, 83 skipped (non-collections), 0 failures**,
BUILD SUCCESS (report-mode). **30 genuine pagination violations** surfaced (the large unbounded
collections above + a few very slow endpoints), where before only 7 endpoints were checked at all.

## Files
- `src/test/java/com/egalvanic/qa/testcase/api/ListApiCatalogAuditTest.java` (new)
- `suite-api-health.xml`

## Follow-up for the team
The backend list endpoints above ignore `page`/`per_page`/`limit` and return the entire collection
with no cap — a real (currently low-severity, will worsen with data growth) pagination gap. The report
enumerates all of them; run Suite 3 with `-DSTRICT_LIST_API_CONTRACT=true` to gate on them once the
backend adds pagination.
