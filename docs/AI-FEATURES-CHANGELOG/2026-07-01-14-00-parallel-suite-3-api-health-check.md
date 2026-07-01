# Parallel Suite 3 — API Health Check (ported from the Utility Toolkit)

- **Title:** New `ApiHealthCheckApiTest` + `parallel-suite-3.yml` — a read-only API health monitor over the
  ~50-endpoint Z-Platform registry, classifying pass/warn/fail with the same recommendation buckets as the
  Egalvanic Utility Toolkit v3.0. Live: **49 endpoints, 48 pass, 1 warn, 0 fail, avg 1117ms, 2 critical /
  21 warning / 7 info recommendations.**
- **Date:** 2026-07-01
- **Time:** 14:00
- **Prompt:** "create parallel suite 3 to run this. For health check" (the Utility Toolkit health check).

---

## What changed

**`ApiHealthCheckApiTest`** (extends `BaseAPITest`) ports the toolkit's health engine into TestNG:
- **Registry:** the ~50 endpoints across 15 categories (Core Health, Auth, Accounts/Users/Contacts,
  Nodes & Classes, SLD, Sessions & Work Mgmt, Quotes & Opportunities, Procedures, Materials, Labor, Test
  Equipment, Lookups, Forms, Bulk, Reporting, Branding, Mutations, Z University).
- **Classification:** pass = 2xx · warn = 4xx (reachable, needs params/permission) · fail = 5xx / timeout /
  connection error.
- **Recommendation thresholds (verbatim from the toolkit):** slow ≥1500ms / very-slow ≥4000ms; large
  ≥500KB / ≥5000KB; consider-pagination >50 items, no-pagination >500 (critical >10000); scaling >200 items
  + slow; plain-array shape; empty (0 items).
- **Read-only:** every probe is a GET (plus read-only `/auth/verify-token` and `/health`). Context ids
  (`company_id` / `sld_id` / `quote_id` / `subdomain`) resolved live from `/auth/v2/me` (+ `/company/{id}/slds`
  fallback for sld_id, which admin's `accessible_sld_ids` leaves empty).
- **Build gate:** a case hard-fails only on a genuine outage (5xx/timeout after one retry); a 4xx is a
  healthy server → WARN; latency/payload/pagination findings are advisory. Report →
  `reports/api-health-report.md`.

**`parallel-suite-3.yml`** — "Parallel Suite 3 — API Health Check": runs `suite-api-health.xml` on a daily
cron + `workflow_dispatch`, no browser (pure REST → fast), environment-selectable (QA default; Prod is a
choice that reads `EG_PROD_EMAIL`/`EG_PROD_PASSWORD` secrets — never committed — and is still read-only). The
health report is echoed to the run summary and uploaded as an artifact.

## Live findings (QA, 2026-07-01) — for the backend team
- **2 critical (very slow):** Mutations List 5981ms, Z University Pages 5582ms.
- **No-pagination / scaling:** Node Classes (554 items, 4141KB, 1840ms), User Schedule (781, 887KB),
  Lookup Procedures (1195, 2520KB, 3732ms), Users (171), Mutations (100), Edge Classes (51).
- **1 warn (400 — needs params):** Lookup Procs by Class/Subtype.
- Everything else healthy (2xx). 0 outages.

## Depth explanation (for learning + manager review)

**Why port into TestNG rather than run the Flask toolkit in CI.** The toolkit (`~/Downloads/egalvanic-
utility-toolkit`) is a local Flask *dashboard* — `run.sh` starts a server at :8500, not a one-shot pass/fail
job a CI runner can gate on. So Parallel Suite 3 reuses the toolkit's *logic* (registry + classifier +
ruleset, transcribed verbatim) inside this repo's existing REST-Assured/TestNG harness, giving a headless,
schedulable, artifact-producing health check consistent with Suites 1 & 2.

**Health-gate philosophy: down ≠ slow.** A health check must go red on a real outage but not on a backend's
pagination decisions or a permission 4xx. So the build gate is narrow (5xx/timeout, with one retry to shed
ambient 502 flakiness); everything else is classified and reported. This matches the repo's `STRICT_*`
posture — informative by default, actionable without being noisy.

**On the token you pasted:** it's a DevRev session JWT (issued by `auth-token.devrev.ai`, for the
Beamer/DevRev widget), not the app's API token — so it isn't used. The health check authenticates the same
way the app does: `/api/auth/login` (email+password → Cognito `access_token`). No token is committed.

## Validation
`mvn test -DsuiteXmlFile=suite-api-health.xml` against QA: 49 endpoints probed, 0 failures (green), report
written. `parallel-suite-3.yml` YAML validated.
