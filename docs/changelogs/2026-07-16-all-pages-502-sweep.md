# 2026-07-16 — All-Pages 502 Sweep in Parallel Suite 3 (+ live findings)

## Prompt
"Check everything, fix anything found, cover everything in API testing too. Find 502
errors in all the pages — add that in Parallel Suite 3."

## What was built (commit 1d80b95)

**`AllPagesStabilityApiTest`** — a per-page 502/5xx/timeout sweep, added to Suite 3
(`suite-api-health.xml` → "API All-Pages 502 Sweep").

**Method — ground truth first:** loaded 13 app routes logged-in via Playwright and
captured each page's REAL `/api` calls from the network panel (not guessed from swagger).
The page→endpoint map:

| Page | Backing endpoints |
|---|---|
| shell (every page) | alliance-config, auth/v2/me, legal/acceptance/check, action-items/counts, company timezone, users/{id}/roles, users/{id}/slds |
| Dashboard | lookup/site-overview/{site}, sites/{site}/status |
| Assets | lookup/nodes/{site}, lookup/v2/nodes/{site} (paginated) |
| Issues | POST /v2/issues/list |
| Connections | connections/v2/sld/{site} |
| Locations | location/sld/{site} |
| Tasks | tasks/{site} |
| SLDs | sld/{site}/views, /view-mappings, /graph |
| SLD view | sld/v3/{id}, sld/v2/{id} (heaviest) |
| Arc Flash | lookup/nodes/{site}?include_bus_nodes=true |
| Work Orders | company/{co}/sessions, /workorders (paginated) |
| Opportunities | POST company/{co}/opportunities/v2 |
| Accounts | POST account/by-company/{co}/v2 |
| EMPs | POST company/{co}/committed-quotes/v2 |
| PM Readiness | lookup/nodes/{site}, POST lookup/class-availability |

The sweep runs the full panel 3 rounds (~29 endpoints, 87 calls) and attributes any
502/5xx/timeout to the PAGE a real user would see fail. Report-mode (single blip
tolerated as QA jitter); `-DSTRICT_ALL_PAGES=true` hard-gates. Report:
`reports/all-pages-502-report.md`, surfaced as critical findings on the consolidated
dashboard via `gen_api_suite_report.py`, uploaded as a CI artifact.

**Bug found & fixed in own test during validation:** SLD rows carry no `site_id` field —
site↔SLD is 1:1 and the row's `id` IS the siteId (proven site_id→id fallback pattern);
first run silently skipped all 11 site-scoped endpoints.

## Live findings (2 validation runs)

- **Zero 502 / zero 5xx across every page's endpoints.** No gateway errors at run time.
- **REPEATABLE round-3 latency degradation on heavy endpoints** (all HTTP 200, all on the
  3rd consecutive sweep): `/lookup/nodes/{site}` (Assets) **68.0s**, POST
  `/account/by-company/{co}/v2` (Accounts) **31.3s**, POST `/opportunities/v2` **8.8s**.
  Pattern: backend degrades under repeated sequential load — plausibly the same mechanism
  behind this week's `alliance-config/branding` flapping (502/timeout cycles). **Backend
  escalation recommended**; the sweep now documents it every Suite-3 run.

## Suite-3 coverage state after this session
Health check, full catalog, pagination (curated/catalog/behavior + ZP-3041/3043 rows),
error/transport contracts + 502 detector (now incl. SLD v3/v2 + planned-workorder-lines),
security headers, filter/search, CRUD, input validation, mutation semantics, SLD v3
payload benchmark (+ ZP-3120 v3-vs-v2 regression guard), agents contract, IR/FLIR,
duplicate audit, evidence capture, **and the new all-pages 502 sweep**.

## Addendum (2026-07-17, commit ef138ed): screenshot evidence added

**`Page502ScreenshotSweepTestNG`** — the browser half of 502 detection. A real logged-in
Chrome walks all 13 sidebar pages ×2 rounds via client-side routing (keeps the injected
fetch/XHR recorder alive across navigations, so every backing API call is captured). Any
5xx/502 → recorded per page + **the page screenshotted as user-visible evidence**
(`reports/evidence/evidence-502-{page}.png`, auto-embedded in the consolidated report's
Visual Evidence section, CRITICAL + evidence badge on the findings dashboard).

Suite-3 changes: new last test block in `suite-api-health.xml`; **Install Chrome step
added to parallel-suite-3.yml** (the suite was API-only before — why screenshots weren't
possible); report-generator AREAS + findings collector; artifacts include
`reports/evidence/`. `-DSTRICT_PAGE_502=true` gates the build.

Live validation: 26 page visits in 117s, 0×502/0×5xx (clean env at run time).

**Suite 3's 502 detection is now three-layered:** intermittent-5xx stability probe
(critical panel incl. SLD v3/v2 + planned-workorder-lines) → all-pages API sweep
(per-page attribution) → browser screenshot sweep (user-visible evidence).
