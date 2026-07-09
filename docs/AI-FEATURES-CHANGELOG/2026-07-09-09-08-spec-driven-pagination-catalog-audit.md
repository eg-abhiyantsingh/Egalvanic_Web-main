# Spec-driven catalog-wide pagination audit (Suite 3)

**Date:** 2026-07-09
**Time:** 09:08 UTC
**Author:** Claude Code (automation, Fable 5), for abhiyant.singh@egalvanic.com

## What was asked
> "pagination api testing is still not present in parallel suite 3"

## Method + finding
First verified the claim against the live CI run: pagination tests DO run in Suite 3
(`ListApiContractApiTest`, `PaginationBehaviorApiTest`, both green). The real gap was **coverage
breadth** — the curated contract test asserts on only 7 hardcoded endpoints. An empirical live probe
of all 187 param-free GET roots (concurrent, 8s timeout each) showed **104 are JSON collections**, only
1 of which was audited; large collections (`/eqp-lib/manufacturers`=448, `/reporting/page-templates`=366,
`/attachment/`=217, `/opportunity/`=193, `/action-items/`=870 total, `/skm-library/manufacturers`=104)
had no pagination coverage at all.

## What was built
`ListApiCatalogAuditTest` — enumerates the swagger catalog at run time and applies the eGalvanic list
contract (paginated / bounded default / max-limit / filter-no-5xx / latency) to **every** collection
endpoint, report-mode with the existing `-DSTRICT_LIST_API_CONTRACT=true` gate. Same spec-driven,
auto-covering pattern as `ApiFullCatalogHealthApiTest` and the `AgentsContractApiTest` built earlier
today. Wired into `suite-api-health.xml`.

## Depth explanation (for learning + manager review)

**Why a new spec-driven class instead of adding endpoints to the curated one?** The curated
`ListApiContractApiTest` HARD-asserts `status==200 + jsonList + latency` on its 7 known-good endpoints.
If I fed it 187 arbitrary catalog endpoints, the many that are single objects / slow / non-200 would
turn those hard assertions into false failures and redden CI. The codebase already separates
"curated + hard-asserting" from "catalog-wide + report-mode" (the full-catalog health test), so the new
class follows that split: it DISCOVERS collections (skips anything that isn't one) and reports, keeping
the trusted curated test intact and Suite 3 green-by-default.

**Signal-over-noise design.** A first run flagged 92 "violations" — but most were tiny fixed lists (2–8
records: enums, lookups, the 2-element `roles[]` array on `/auth/me`) where pagination is not actually
required. Pagination is a real requirement only when a collection can exceed the max page size. So the
verdict logic now raises a VIOLATION only when `total > 100` (genuinely large, returns everything =
unbounded-growth risk); mid-size lists (20–100) are advisories; small lists are OK. That dropped the
count to **30 real violations** — the ones a backend engineer would actually act on.

**Why the probe is empirical, not schema-based.** The swagger spec is flask-restx auto-generated and
does NOT populate response schemas, so "is this a list endpoint?" can't be answered statically — the
audit must call each endpoint and inspect the live body shape (bare `[...]` array or a `{key: [...]}`
envelope). This is also why it detects collections the spec never declared.

## Validation (live)
187 candidates → **104 audited, 83 skipped, 0 failures**, 30 genuine violations reported. Green.

## Files
- `src/test/java/com/egalvanic/qa/testcase/api/ListApiCatalogAuditTest.java` (new)
- `suite-api-health.xml`
