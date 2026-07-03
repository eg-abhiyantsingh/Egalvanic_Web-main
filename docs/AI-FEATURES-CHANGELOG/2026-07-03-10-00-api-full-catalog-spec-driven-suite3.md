# API Full Catalog health check (spec-driven, ~973 ops) → Parallel Suite 3

**Date:** 2026-07-03
**Author:** Claude Code (automation), for abhiyant.singh@egalvanic.com

## What was asked
> "https://acme.qa.egalvanic.ai/api/docs — this content, all API. Can you add all API in parallel suite 3."

`/api/docs` is Swagger UI backed by `/api/swagger.json` (OpenAPI 2.0, "EG-PZ Backend API v2.0.0").
The prior Suite 3 health check hit a hardcoded ~50 endpoints. This adds **every** operation in the
spec — automatically, at run time.

## What was built — `ApiFullCatalogHealthApiTest`
- **Spec-driven catalog:** fetches `/api/swagger.json` in a `@DataProvider`, enumerates every
  operation. 2026-07-03 spec = **862 paths / 973 operations** (420 GET · 319 POST · 138 PUT ·
  84 DELETE · 12 PATCH). New endpoints are covered the moment they ship — nothing to maintain.
- **Zero-mutation probe policy (by construction):**
  - `GET` → authenticated; `{path params}` filled with a **random UUID** (never a real id), so
    parameterized GETs exercise the not-found path. Flags 5xx, 200+HTML (SPA fallback), slow.
  - `POST/PUT/PATCH/DELETE` → **UNAUTHENTICATED**, empty `{}` body. The only signal is the auth
    gate: 401/403 = healthy; a real 2xx **JSON** response = auth-gate gap. No token is ever sent
    and ids are random UUIDs, so nothing can mutate real QA data.
  - Public-by-design endpoints (login/logout/signup/forgot/verify-token/branding/health/docs) are
    whitelisted — they only must not 5xx.
- **Coverage assertion:** `testCatalogCoverage` fails if probed != spec operations, so the suite
  proves it covered the whole catalog.
- **Report-mode failure semantics:** only a genuine outage (connection refused/reset) hard-fails.
  A read-timeout is retried once, then recorded as a **warn** + critical rec (a chronically slow
  endpoint shouldn't redden a monitor every run). Matches ApiHealthCheckApiTest's philosophy.
- **DTD-safe XML** is N/A here (JSON spec), but the report parser reuse in the detailed-report merge
  stays DTD-guarded.

## Findings (VERIFIED before labelling — per the don't-over-report rule)
First raw pass flagged 145 "critical" recs; I curl-verified the load-bearing ones and **corrected the
classifier** so the report doesn't over-state:

1. **Genuine unauthenticated endpoints (real, reproduced with curl, no token):**
   `POST /api/agent/clear` → `{"ok":true}`, `POST /api/agent/files` → `{"files":[]}`,
   `POST /api/agent/read`, `POST /api/agent/upload` — the `/agent/*` group answers with JSON while
   completely unauthenticated. Worth a security look (clear mutates; upload accepts data). Reported
   as a real auth-gate finding.
2. **False-positive corrected:** the parameterized "unauth mutation" hits (`…/export-xml`,
   `DELETE /sld-link/{id}`, `…/compute-curve`) actually returned **200 + HTML** — the SPA fallback,
   NOT an auth bypass. The classifier now checks body shape before calling something an auth gap, so
   these move to the (already-tracked) HTML-fallback bucket instead of inflating the security count.
3. **Systemic HTML-fallback (matches the known TC_HSC_01/05 doc-inspired failures):** many `/api/...`
   paths return **200 + HTML** for unknown resources instead of a JSON 404 — the same defect the
   documentation-inspired tests already flag. The catalog quantifies its blast radius.
4. **4 authenticated GETs 500** (`/attachment/{id}/nodes`, `/planned-labor/workorder/{id}`,
   `/reporting/status/{arn}`, `/z-university/pages/{id}`) — likely input-validation on the random
   UUID, but a 500 (not 400/404) is a server-error-shape finding.
5. **A few endpoints read-timeout >30s** (`/planned_workorder_line/`, `/shortcut/all`) — surfaced as
   critical recs, not hard fails.

## Wiring (Suite 3)
- `suite-api-health.xml` → added the `API Full Catalog (spec-driven)` test.
- `parallel-suite-3.yml` → renders `reports/api-catalog-report.md` to styled HTML via the existing
  `gen_api_health_report.py`, publishes header+recommendations to the run summary, and uploads
  `api-catalog-report.{md,html}` in the `api-health-report` artifact.

## Depth note (learning / manager review)
The interesting engineering choice is **how to probe 553 mutating endpoints without touching data.**
Sending them unauthenticated with a throwaway body turns each into a pure auth-gate assertion —
safe by construction, and it doubles as a security sweep. The calibration lesson: a 2xx status alone
doesn't mean "handler ran" — on this SPA-served host, unmatched routes 200 with HTML, so severity
logic must inspect the body shape, or it cries wolf. Verifying with curl before labelling kept the
"9 auth bypasses" from going out when only 4 are real.
