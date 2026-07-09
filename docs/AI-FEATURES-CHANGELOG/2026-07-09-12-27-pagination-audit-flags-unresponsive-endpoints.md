# Pagination audit surfaces unresponsive endpoints instead of dropping them

**Date:** 2026-07-09
**Time:** 12:27 UTC
**Author:** Claude Code (automation, Fable 5), for abhiyant.singh@egalvanic.com

## What was asked
Implement `?page/per_page` on the slow `/planned_workorder_line/` endpoint (~15s), and: "did we cover
this too?"

## Two-part answer
1. **Implementation** — that's a backend (`eg-pz-frontend`) change; this QA-automation repo can't (and
   per policy must not) modify the read-only upstream API. So no backend code changed here.
2. **Coverage** — partially yes, and I closed the gap: the full-catalog health probe already flagged the
   endpoint as ~30s/unresponsive, but the pagination catalog audit was *silently skipping* it because a
   timeout made its discovery GET look like "not a collection".

## Change
`ListApiCatalogAuditTest` now detects read timeouts, retries once, and records a distinct
`UNRESPONSIVE` finding (with "likely unbounded — add ?page/per_page") rather than skipping. It is kept
**separate from confirmed pagination violations and is not STRICT-gated**.

## Depth explanation (for learning + manager review)

**Why the slowest endpoint was invisible to the pagination audit.** The audit's discovery filter is
GET-then-classify: `status != 200 → skip`. A socket timeout returns `status 0`, which matched the skip.
So the endpoints most likely to need pagination (too slow because they return everything) were exactly
the ones excluded. Fixing detection required distinguishing a *timeout* (a signal) from a genuine
non-collection (correctly skipped).

**Why UNRESPONSIVE is a separate, non-gating category — the calibration step.** My first cut labeled
timeouts as `VIOLATION` and produced 38 of them. I calibrated against the single-probe CI full-catalog
report, which had only ~18 timeouts total. The audit fires up to 5 probes per endpoint, so under the
suite's 6-thread parallel load it *induces* extra timeouts on slow-but-otherwise-fine endpoints. Turning
all of those into "confirmed pagination bugs" would cry wolf. So: require BOTH the initial probe and the
retry to time out, label it `UNRESPONSIVE` (not `VIOLATION`), count it separately, and don't hard-fail on
it even under STRICT (it's environmental/latency, not a deterministic contract breach). Confirmed
violations (a collection that returns >100 rows ignoring per_page) stay STRICT-gated; unresponsive stays
advisory. This keeps the signal trustworthy: 32 real violations + 15 advisories, not 47 "bugs".

**The honest engineering boundary.** The most useful thing I could add here was *detection + a precise,
actionable recommendation* ("this endpoint likely needs ?page/per_page"), not the fix itself — the fix
lives in a repo I don't own. Reporting it well is the QA deliverable.

## Validation (live)
BUILD SUCCESS, 287 tests, 0 fail. `/planned_workorder_line/` → `UNRESPONSIVE | TIMEOUT 30070ms` in the
report with the pagination recommendation; summary splits 32 violations vs 15 unresponsive advisories.

## Files
- `src/test/java/com/egalvanic/qa/testcase/api/ListApiCatalogAuditTest.java`
