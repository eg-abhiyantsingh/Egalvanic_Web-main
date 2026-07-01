# List-API contract audit — pagination / filters / max-limit / default page size / response time

- **Title:** New `ListApiContractApiTest` — a non-functional contract audit of every list/collection API for
  pagination, filters, a max page-size cap (≤100), a bounded default (ideal ≤20), and response time. Live
  run: **7 list APIs audited, 6 violations, 1 compliant, 0 test failures (report mode).**
- **Date:** 2026-07-01
- **Time:** 12:45
- **Prompt:** QA-team request (Dharmesh Avaiya / Mukul Panchal): "check all the list API's — they should
  always be paginated, have filters, max limit 50/100, default 10/20, and respond within a few seconds."

---

## What changed

Added `com.egalvanic.qa.testcase.api.ListApiContractApiTest` (extends `BaseAPITest`). For each catalogued
collection endpoint it probes the live API and audits:
- **Health (hard-fail):** 200 + JSON list + response ≤ 8s + a `search` param does not 5xx.
- **Pagination policy (reported; enforced under `-DSTRICT_LIST_API_CONTRACT=true`):** is `per_page`/`limit`
  honored (probe `per_page=1`), is `per_page=1000` capped at ≤100, is the default page size bounded
  (≤100, ideal ≤20), does a filter/search param work.

Ids are resolved dynamically (companyId from `/auth/v2/me`, siteId from the first `/company/{id}/slds`
item) — nothing hardcoded to a mutable tenant. Writes a shareable compliance matrix to
`reports/list-api-contract-report.md`. Wired into `suite-api.xml`, a standalone `suite-list-api-contract.xml`,
and the `load_api` toggle of Parallel Suite 2 (its own parallel group).

## Live audit result (site acme.qa, 2026-07-01)

| Endpoint | Records | Paginated | Max-limit ≤100 | Filter | Response | Verdict |
|---|---|---|---|---|---|---|
| `/node_classes` | 554 | NO | NO (554) | ignored | 5.1s SLOW | ❌ |
| `/company/{id}/opportunities` | 151 | NO | NO (151) | ignored | 1.5s | ❌ |
| `/company/{id}/slds` | 138 | NO | NO (138) | ignored | 0.4s | ❌ |
| `/edge_classes` | 51 | NO | n/a | ignored | 2.5s | ❌ |
| `/issue_classes` | 29 | NO | n/a | ignored | 0.5s | ❌ |
| `/lookup/nodes/{site}` | 5 | NO | n/a | ignored | 2.0s | ❌ |
| `/sld/{site}/library-designations` | 3 | yes (`per_page`) | n/a | yes | 0.8s | ✅ |

**6 of 7 list APIs return the full collection unbounded, ignore `per_page`/`limit`, and don't filter.**
`/node_classes` is the worst — 554 records, no cap, and 5.1s (over the "few seconds" target). Only
`library-designations` complies (honors `per_page`, filter works). These are the endpoints the backend
team should prioritise for the pagination/limit/default policy.

## Depth explanation (for learning + manager review)

**1. Discover the real contract before asserting one.** The pagination params aren't in this repo (they're
in the SPA's compiled JS), so I captured them from the live app's Resource-Timing network log
(`?page=&per_page=`), then `curl`-probed each endpoint to learn its response shape (bare array vs
`{data|items|opportunities|slds:[…], count}`) and confirm whether `per_page` is actually honored. Only
then did I encode the audit — so the test measures reality, not an assumed convention.

**2. Report vs enforce — the `STRICT_*` pattern.** Most list APIs violate the policy today, and that's a
backend decision outside a QA test's authority to fail the pipeline over. So the audit hard-fails only on
objective health (non-200 / non-JSON / >8s / a filter that 5xx-es) and *reports* the pagination-policy
violations, mirroring the repo's existing `STRICT_HEALTH_GATES`. The team flips
`-DSTRICT_LIST_API_CONTRACT=true` to turn the audit into a release gate once a module's APIs comply — which
matches their stated plan to adopt this "based on module, in parallel."

**3. The verdict distinguishes determinable from not.** Pagination is only asserted where a collection has
≥2 records; max-limit only where the collection exceeds 100. Small collections are marked `n/a` rather than
falsely "compliant" — so the report doesn't over- or under-claim.

## Validation
`mvn test -DsuiteXmlFile=suite-list-api-contract.xml` (headless-safe pure REST): ran green (0 failures,
report mode), emitting the compliance matrix above to `reports/list-api-contract-report.md`.
