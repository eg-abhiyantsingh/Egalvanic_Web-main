# Sentry 500-Cluster (22–23 Jul 2026) — QA Re-verification, 2026-07-31

**Question asked:** 80 × HTTP 500 across 6 endpoints were recorded in Sentry on 22–23 Jul 2026
(66% of all 5xx in the period). Is this still happening on QA before the next deploy?

**Answer: partially. 2 of the 6 endpoints still 500 deterministically on QA today; 3 are clean;
1 route doesn't exist on the QA build so its 10 events can't be re-verified here.**

- Environment: `https://acme.qa.egalvanic.ai` (acme tenant), verified 2026-07-31 ~17:00 IST.
- Method: direct authenticated API probes (curl + REST Assured), token via `POST /api/auth/login`
  with the `AppConstants` admin account. All write-endpoint probes used malformed or
  valid-shaped-but-nonexistent UUIDs (`00000000-0000-4000-8000-0000000000xx`) so **nothing was
  created or mutated** on QA.
- Rerunnable check: `mvn test -DsuiteXmlFile=suite-sentry500-reverify.xml`
  (`Sentry500ClusterReverifyApiTest`, 8 probes). Run of 2026-07-31: **3 failures = the 3 live
  500 repros below; 5 passes.** When the backend is fixed, this suite goes green — that is the
  "verify fix in QA before deploying" gate.

## Verdict per endpoint

| Sentry events | Endpoint | Verdict 2026-07-31 |
|---|---|---|
| 26 | `GET /api/lookup/procedures` | **Not reproducible** — 200/JSON on 6 consecutive hits + param variants; junk `node_class_id` → clean 400. Residual risk: response is **unbounded (~2.08 MB, 1,196 rows)** — a load-dependent timeout/500 candidate, consistent with errors appearing only during the 22–23 Jul window. |
| 19 | `POST /api/reporting/quote_preview_html` | **Not reproducible — appears fixed.** Missing `quote_id` → 400. Real/ghost/junk `quote_id` → 200 whose pages carry `render_error: "Template '' is not HTML; preview unavailable"` — render exceptions are now caught per page and surfaced in-body instead of 500ing. Two follow-ups: (a) ghost/junk `quote_id` is accepted (no existence/format validation, returns a job); (b) one validation call took **67.7 s** to answer 400 (once; 0.3 s on repeat). |
| 13 | `POST /api/mapping/node-session/bulk-create` | **STILL 500 — deterministic (3/3 runs).** Valid `session_id` + one nonexistent node UUID → `500 {"error": "An internal error occurred.", "trace_id": "…"}`. trace_ids: `d908012d34e546549144d8b57da4364c`, `42eb84781e3a47cbba096a105d35de28`, `3ad96f964c4a4b86af0cc7f86411c483`. Malformed bodies (`{}`, junk types) DO get clean 400s — only the unknown-FK path is unhandled. |
| 10 | `GET /api/bulk-edit/rules-status/{id}` | **Cannot re-verify — route absent on the QA build.** Any id (well-formed UUID or junk) returns the SPA HTML shell (200, 2,089 B), while sibling `POST /api/bulk-edit/explain-warnings` answers JSON — so the `/bulk-edit` prefix IS routed and this specific route just isn't deployed. The Sentry events must come from a build that has it (prod). |
| 7 | `POST /api/skm/import-xml/preview` | **STILL 500 — two distinct triggers, deterministic.** (a) Genuinely malformed XML → `500 {"error": "Failed to preview SKM XML: mismatched tag: line 1, column 59"}` — client input answered with 500 + raw expat parser message (should be 400). (b) **Spec-legal XML rejected:** a declaration without the optional `encoding` attribute (`<?xml version="1.0"?>`) → `500 "XML declaration not well-formed: line 1, column 12"`, while the identical document with `encoding="UTF-8"` (or with no declaration at all) → 200 preview summary. |
| 2 | `POST /api/onboarding/jobs` | **Not reproducible** — every payload shape tried (empty, ghost `sld_id` as JSON top-level/nested/query-param) answers a clean `400 "sld_id is required"`. Curiosity: a multipart POST to it falls through to the SPA shell (content-type-dependent routing). |

## Live repros (copy-paste)

```bash
TOKEN=$(curl -sk -X POST https://acme.qa.egalvanic.ai/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"<AppConstants.VALID_EMAIL>","password":"<AppConstants.VALID_PASSWORD>","subdomain":"acme"}' \
  | python3 -c 'import json,sys;print(json.load(sys.stdin)["access_token"])')

# BUG 1 — bulk-create: nonexistent node UUID → 500 (expect 400/404)
curl -sk -X POST https://acme.qa.egalvanic.ai/api/mapping/node-session/bulk-create \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"session_id":"b158deb5-044f-44ca-89ce-60fecc4f7169","node_ids":["00000000-0000-4000-8000-000000000001"]}'
# → 500 {"error": "An internal error occurred.", "trace_id": "…"}

# BUG 2a — SKM preview: malformed XML → 500 + raw parser message (expect 400)
printf '<?xml version="1.0" encoding="UTF-8"?><DAPPER><unclosed></DAPPER>' > /tmp/broken.xml
curl -sk -X POST https://acme.qa.egalvanic.ai/api/skm/import-xml/preview \
  -H "Authorization: Bearer $TOKEN" \
  -F 'xml_file=@/tmp/broken.xml;type=text/xml' -F 'sld_id=aadcee4c-7dd0-45b3-81b9-309c5c166084'
# → 500 {"error": "Failed to preview SKM XML: mismatched tag: line 1, column 59"}

# BUG 2b — SKM preview: LEGAL declaration without encoding attr → 500 (expect 200)
printf '<?xml version="1.0"?>\n<DAPPER><PROJECT NAME="qa-probe"/></DAPPER>\n' > /tmp/noenc.xml
curl -sk -X POST https://acme.qa.egalvanic.ai/api/skm/import-xml/preview \
  -H "Authorization: Bearer $TOKEN" \
  -F 'xml_file=@/tmp/noenc.xml;type=text/xml' -F 'sld_id=aadcee4c-7dd0-45b3-81b9-309c5c166084'
# → 500 {"error": "Failed to preview SKM XML: XML declaration not well-formed: line 1, column 12"}
# Control: same body with encoding="UTF-8", or with no declaration at all → 200.
```

## Artifacts in this directory

| File | What it is |
|---|---|
| `bc2.body` | bulk-create 500 response (2nd determinism run, trace_id `42eb8478…`) |
| `skm8.body` | SKM 500 on mismatched tag (raw parser message leak) |
| `skm9.body` | SKM 500 on legal no-encoding declaration |
| `broken2.xml` / `noenc.xml` / `valid.xml` | the exact XML fixtures (bad body / legal-no-encoding / passing control) |
| `qp_real_quote.body` | quote_preview 200 with per-page `render_error` (the "fixed" shape) |
| `rs_ghost_uuid.body` | rules-status SPA HTML shell (route absent on QA) |

## Recommended actions

1. **bulk-create (13 ev):** wrap the node-id resolution/insert in existence validation → 404/400
   with the offending UUID; the generic-500-with-trace_id handler already hides internals, the
   status code is the remaining defect. Reproduce with BUG 1 above.
2. **SKM preview (7 ev):** catch `ExpatError`/parse exceptions → 400 with a user-safe message
   (2a), and fix the declaration handling that rejects `<?xml version="1.0"?>` (2b) — SKM
   PowerTools exports may legitimately omit `encoding`.
3. **lookup/procedures (26 ev):** nothing to fix today on the error path, but paginate/cap the
   unbounded 2 MB list (existing list-contract audit finding) — likeliest cause of the original
   window being load-dependent.
4. **quote_preview_html (19 ev):** treat as fixed pending Sentry confirming zero new events;
   add `quote_id` existence/format validation (currently accepts `"not-a-uuid"`).
5. **rules-status (10 ev):** re-verify on the environment where the route exists (prod) or after
   it ships to QA — the suite warns until then.
6. Sentry MCP is connected but unauthenticated in this session — authorize it (claude.ai
   connector settings) to correlate the captured trace_ids with the original issue group and
   confirm the 22–23 Jul events map to the same stack traces.
