# 2026-07-31 — Sentry 500-Cluster (22–23 Jul) Re-verification on QA

**Prompt:** Sentry recorded 80 × HTTP 500 across 6 endpoints on 22–23 Jul 2026 (66% of all 5xx).
Verify on QA whether the issue is still occurring before deploy.

## Verdict (full evidence: `docs/bug-evidence/sentry-500-cluster-jul22-23/EVIDENCE.md`)

| Endpoint (events) | Still 500 on QA? |
|---|---|
| `POST /mapping/node-session/bulk-create` (13) | **YES — deterministic.** Nonexistent node UUID → 500, trace_ids captured (`d908012d…`, `42eb8478…`, `3ad96f96…`). |
| `POST /skm/import-xml/preview` (7) | **YES — two triggers.** Malformed XML → 500 with raw expat message; spec-legal `<?xml version="1.0"?>` (no `encoding` attr) → 500. Control with `encoding="UTF-8"` → 200. |
| `GET /lookup/procedures` (26) | No — 200/JSON, junk param → clean 400. Risk factor: unbounded ~2 MB response remains. |
| `POST /reporting/quote_preview_html` (19) | No — appears fixed; render errors now caught per page (`render_error` in 200 body). Gaps: ghost/junk `quote_id` accepted; one 67.7 s validation response observed. |
| `GET /bulk-edit/rules-status/{id}` (10) | Cannot verify — **route absent on QA build** (SPA shell for any id; sibling `/bulk-edit/explain-warnings` answers JSON). |
| `POST /onboarding/jobs` (2) | No — clean 400 on every payload shape tried. |

## Changes

- **NEW** `src/test/java/com/egalvanic/qa/testcase/api/Sentry500ClusterReverifyApiTest.java` —
  8 API probes encoding the cluster; hard-fails on any 5xx-on-client-input (Suite-3 convention).
  Run of 2026-07-31: **3 failures = the 3 live repros, 5 passes** — the suite is the deploy gate:
  `mvn test -DsuiteXmlFile=suite-sentry500-reverify.xml` goes green when the backend is fixed.
- **NEW** `suite-sentry500-reverify.xml` — dedicated TestNG suite (API-only, no browser).
- **NEW** `docs/bug-evidence/sentry-500-cluster-jul22-23/` — evidence doc, raw 500 bodies,
  exact XML fixtures, copy-paste curl repros.

## Notes

- All probes were non-mutating: ghost UUIDs (`00000000-0000-4000-8000-…`) fail before any row
  is inserted; SKM/quote previews are read-only.
- Sentry MCP needs authorization (claude.ai connector settings) to correlate captured trace_ids
  with the original issue groups.
