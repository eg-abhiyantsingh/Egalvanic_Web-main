# Sentry 500-Cluster Re-verification (22–23 Jul window) — 2026-07-31, 17:25 IST

## What was asked
Sentry flagged 80 × HTTP 500 across 6 endpoints on 22–23 Jul 2026 (66% of all 5xx in the
period). Action item: *"Verify fixes in QA before deploying — check this issue is coming or
not."*

## What was done
1. Authenticated directly against the QA API (`POST /api/auth/login` → bearer token) and
   probed all six endpoints with curl — no browser, no UI flows.
2. Resolved live context ids the probes needed (company → session → quote → SLD) the same way
   the existing `ApiHealthCheckApiTest` does.
3. Designed every write-endpoint probe to be **non-mutating**: malformed bodies and
   valid-format-but-nonexistent "ghost" UUIDs fail server-side before any row can be inserted.
4. Encoded the findings as `Sentry500ClusterReverifyApiTest` (8 probes) + 
   `suite-sentry500-reverify.xml`, ran it, and confirmed it fails on exactly the 3 live
   repros and passes the 5 clean contracts.
5. Evidence package: `docs/bug-evidence/sentry-500-cluster-jul22-23/EVIDENCE.md` with raw
   response bodies, XML fixtures, trace_ids, and copy-paste repros.

## Result
- **Still broken (2 endpoints, 3 distinct triggers):**
  - `POST /mapping/node-session/bulk-create` — nonexistent node UUID → deterministic 500
    (`{"error":"An internal error occurred.","trace_id":…}`); 3/3 runs, 3 trace_ids captured.
  - `POST /skm/import-xml/preview` — (a) malformed XML → 500 leaking the raw expat message;
    (b) spec-legal `<?xml version="1.0"?>` without the optional `encoding` attribute → 500,
    while the same document with `encoding="UTF-8"` (or no declaration) → 200.
- **Clean today:** `lookup/procedures` (but still an unbounded 2 MB list — the likely reason
  the original 500s were load-window-dependent), `quote_preview_html` (render errors now
  caught per page → returned inside a 200 body; looks deliberately fixed),
  `onboarding/jobs` (clean 400s).
- **Unverifiable on QA:** `bulk-edit/rules-status/{id}` — the route isn't deployed on this QA
  build (SPA-shell fallback), so its 10 events must be re-verified on prod or after it ships.

## Depth explanation (for learning / manager walkthrough)

**Why probe with "ghost" UUIDs instead of real ones?** The Sentry events are *server
exceptions*, i.e., unhandled error paths. The error paths are reachable with inputs that
cannot possibly commit a write: a UUID that exists nowhere fails FK resolution, an unclosed
XML tag fails parsing. If the endpoint is fixed, those inputs get 400/404; if not, the same
unhandled exception Sentry saw fires again — but QA data stays untouched. This is how you
re-verify a production error cluster without a reproduction script from the original users.

**How the "route absent" conclusion was reached (differential diagnosis):** QA's gateway
answers *any* unrouted `/api/*` GET with the SPA's `index.html` (200, 2,089 bytes). A naive
reading of `GET /bulk-edit/rules-status/{id}` → 200 would be "healthy". The tell: the sibling
route `POST /bulk-edit/explain-warnings` answers real JSON, so the `/bulk-edit` prefix *is*
routed and only `rules-status` is missing from this build. Always distinguish "endpoint
answered" from "backend answered" — the repo's API tests do this with an `isHtmlShell()`
check, and this is also why `per_page`-style probes must inspect bodies, not just codes.

**Why the SKM `encoding` finding matters:** per the XML 1.0 spec the `encoding` pseudo-attribute
is optional, and SKM PowerTools exports can legitimately omit it. So trigger (b) isn't just an
error-handling gap like (a) — it rejects *valid customer files*. The bug report separates the
two because the fixes differ: (a) is "catch the parse exception → 400", (b) is "fix or replace
whatever pre-parses the declaration".

**Why a failing test is the deliverable:** the action item is "verify fixes in QA before
deploying". A one-off curl session answers "is it fixed *today*?"; the encoded suite answers
it *after every deploy* — red while the bug lives, green when fixed, with the assertion message
carrying a fresh trace_id for Sentry correlation. This follows the repo's Suite-3 convention:
findings are reproduced with curl first, then encoded as contract probes (see
`InputValidationApiTest`'s javadoc for the same pattern).

**Incidental zsh lesson from the session:** in zsh, lowercase `path` is linked to `PATH`;
`local path=$3` inside a function wiped command lookup and made `curl` "disappear" mid-script.
Renamed to `urlpath`. Worth knowing for anyone writing shared QA shell scripts on macOS.
