# Duplicate-API-call detection + UI/API data-consistency checks

**Date:** 2026-07-03
**Author:** Claude Code (automation), for abhiyant.singh@egalvanic.com

## What was asked
Pre-CI hardening pass: the automation should also cover **duplicate API calls** and **data
mismatches**. Two new browser+API tests, both **report-mode** (they add coverage and write
findings but cannot redden the pipeline unless a STRICT flag is set), wired under the existing
`api` toggle.

## 1. `ApiDuplicateCallTestNG` — redundant/duplicate API-call detector
Drives each module and, via the Resource Timing API, records **every** `/api` request the SPA
fires on that one page load (unlike `NetworkApiContractTestNG`, which collects a deduped Set).
Then normalises each URL to a logical endpoint (strip query; collapse UUID/numeric/long-hex path
segments to `{id}`) and counts repeats — both per logical endpoint and per exact URL.

Thresholds: logical endpoint ≥3× on one load = redundant (warning), ≥5× = critical; same exact URL
(query included) ≥2× = wasteful duplicate. Gate: `-DSTRICT_DUPLICATE_API=true`. Report:
`reports/api-duplicate-calls-report.md`.

**Live result (verified real, not noise):** 253 `/api` calls across 9 modules → **21 redundant
logical endpoints, 68 exact-URL duplicates**. Concrete confirmed cases (identical URL, one load):
`/api/users/{id}/roles` ×3 on dashboard, `/api/node_classes/user/{id}` ×3, `/api/enum-node-voltages`
×2, and the SLDs page firing 45 calls with 7 redundant endpoints. These are genuine
refetch-on-load inefficiencies (React double-render / duplicated effects). Telemetry/3rd-party/poll
endpoints (sentry/beamer/devrev/…/health) are filtered out.

## 2. `UiApiDataConsistencyTestNG` — data-mismatch checks
Per grid module:
- **Refresh determinism (asserted, tolerant):** the grid's "1–N of M" total must be identical
  before and after a same-session hard reload. A total that changes on refresh is an unambiguous
  data-consistency defect (cache race / non-deterministic pagination). Retried to absorb async
  settling; only a stable, reproducible difference is flagged. Gate: `-DSTRICT_DATA_CONSISTENCY=true`.
- **API-vs-UI count (advisory, never asserted):** replays the SPA's own captured list API and
  compares its record count to the grid total. Because the grid may be client-filtered/scoped, this
  is informational only — surfaced in the report, not failed on (avoids false "mismatch" noise).

Report: `reports/data-consistency-report.md`.

## Why report-mode (so the CI run stays clean)
Both mirror the established `STRICT_LIST_API_CONTRACT` / `STRICT_HEALTH_GATES` pattern: findings are
logged to the Extent report + written to markdown, and the build only fails when the corresponding
`-DSTRICT_*` flag is passed. A normal run surfaces the findings without turning red — so this adds
coverage without destabilising the pipeline.

## Wiring
- `suite-network-api.xml` → added both classes alongside `NetworkApiContractTestNG`.
- Parallel Suite 2 `api` toggle catalog: `api-network` group renamed + count 1→3.
- Per-group artifact upload now includes `reports/*.md`, so the duplicate + consistency (and API
  catalog) markdown reports ship in the `reports-s2-*` artifacts.

## Depth note
The key technique for duplicate detection is **URL normalisation before counting** — without
collapsing `{id}` segments, every parameterised call looks unique and real refetch patterns hide.
Counting both the logical endpoint (catches "same resource type refetched") and the exact URL
(catches "literally the same request twice") separates a legitimately-paginated list from a
genuine double-fetch. And keeping the API-vs-UI compare advisory-only is deliberate: grid scope
legitimately differs from raw API count, so asserting on it would cry wolf — the deterministic
signal is the *refresh* invariant, which has no legitimate reason to vary.
