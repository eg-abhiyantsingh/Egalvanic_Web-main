# API Contract — full reference

Thresholds and checks the `api-contract-review` skill applies. Ported from the eGalvanic QA Parallel
Suite 3 health check (`ApiHealthCheckApiTest`) + list-API contract audit (`ListApiContractApiTest`), so a
developer's pre-PR self-check uses the *same* rules CI enforces.

## 1. Records & response shape
- **200** for a successful read; the body is JSON, not HTML/an error page.
- Prefer a **consistent envelope**: `{ "success": true, "data": [...], "total": N, "page": P, "per_page": S }`.
  A bare top-level array works but forces every client to branch on shape → flag as **INFO/WARN**.
- Returns the records you expect for the probe account (empty is fine if the account legitimately has none).

## 2. Pagination  (the core list requirement)
- Accept **`page`** (1-based) and **`per_page`** — this is the eGalvanic convention (not `limit`/`offset`,
  though accepting those as aliases is fine).
- The params must be **honored**: `?page=1&per_page=1` returns 1 item when the collection has ≥2. A param
  that's accepted but ignored (still returns everything) is a **FAIL**.
- Response should expose `total` (or `total_pages`) so clients can page through.

| Rule | Threshold | Severity if violated |
|---|---|---|
| Default page size (no `per_page` given) | ideal **10–20**, must be bounded | WARN if 20–100; **FAIL if unbounded (returns all)** |
| Max page size (clamp/reject large `per_page`) | **≤ 50–100** | **FAIL** if `per_page=100000` returns > 100 |
| `per_page` honored | count ≤ requested | **FAIL** if ignored |

Reference implementation in this codebase: `GET /sld/{siteId}/library-designations?page=&per_page=` (honors
`per_page`). Known violators to learn from: `/node_classes` (554 items, ignores `per_page`),
`/company/{id}/opportunities` (151), `/users/` (returns all).

## 3. Filters & search
- Documented filter/search query params (`search`, `q`, `status`, date ranges, etc.) must **work** and must
  **not 5xx** on unknown or garbage values (`?search=zzq` → 200 with a narrowed/empty set, never 500).
- Filtering should happen in the **query/DB**, not by loading everything and filtering in memory.

## 4. Performance
| Band | Response time | Action |
|---|---|---|
| Fast | < 500 ms | good |
| Acceptable | 500–800 ms | fine |
| Watchlist | 800 ms – 1.5 s | monitor as data grows |
| Slow | 1.5 – 3 s | **WARN** — query-plan review |
| Critical | > 3 s | **FAIL/investigate** — index + pagination |
- **Payload:** > 500 KB → WARN, > 5 MB → critical. Usually a symptom of missing pagination.
- **Scaling:** > 200 items AND slow, or any unbounded collection, is a compound risk — fix with pagination + an index.

## 5. Security (baseline — not a full pentest)
- **Auth required:** the endpoint without a valid token ⇒ **401/403**, never 200 with data. (**FAIL** if open.)
- **Authorization / IDOR:** a user cannot read or mutate another tenant's / user's record by changing an id
  in the path/body. Test with an id you shouldn't have access to ⇒ 403/404, not the record. (**FAIL**.)
- **Input validation:** malformed params ⇒ **400** with a clear message, never a **500** (a 500 on bad input
  is an unhandled-exception / possible injection surface). (**FAIL** on 500.)
- **Injection safety:** filter/search values are parameterised, not string-concatenated into SQL/NoSQL.
- **No data leakage:** responses don't include secrets, tokens, password hashes, internal stack traces, or
  other tenants' data.
- **Transport & headers:** HTTPS only; sensible security headers; auth token in `Authorization: Bearer`,
  not in the URL/query (tokens in URLs get logged).
- **Writes:** create/update/delete require the right permission (RBAC), validate ownership, and are
  idempotent/transactional where expected. Never probe writes against shared/prod envs.

## 6. Severity → PR gate
- **FAIL → block the PR:** unbounded list (no pagination or no max cap), missing auth, IDOR/cross-tenant
  read or write, 500 on bad input, secrets in response.
- **WARN → fix soon / call out in the PR:** default page size > 20, 2–8 s response, > 500 KB payload,
  bare-array shape, missing a documented filter, no `total` in the envelope.
- **PASS → meets the contract.**

## 7. Notes for reviewers
- The check applies per **module**: run it on each list/CRUD endpoint the PR adds or changes.
- Static review (reading the handler) catches most of this without a server and works in CI.
- The live probe (`probe_endpoint.py`) confirms real behavior — pagination-honored, max-limit-clamped,
  timing, and the no-token → 401 auth check — against a **dev** server.
