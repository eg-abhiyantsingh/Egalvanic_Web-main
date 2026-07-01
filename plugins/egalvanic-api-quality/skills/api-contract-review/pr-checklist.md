# API Contract Review — PR checklist

> Copy into the PR description. Tick each item for every list/CRUD endpoint the PR adds or changes.
> ✅ pass · ⚠️ warn (note it) · ❌ fail (block until fixed) · n/a not applicable.

**Endpoint(s) reviewed:** `<METHOD> /path` …
**Reviewed via:** ☐ static (code) ☐ live probe (`probe_endpoint.py` on dev)

### Records & shape
- [ ] Returns the expected records (200, JSON, right shape)
- [ ] Consistent envelope (`{success, data, total, page, per_page}`) — bare array is ⚠️

### Pagination (list endpoints)
- [ ] Accepts `page` + `per_page` **and honors them** (`per_page=1` → 1 item)
- [ ] Default page size is bounded and small (**10–20** ideal) — unbounded is ❌
- [ ] Max page size clamped/rejected (**≤ 50–100**; `per_page=100000` must not return everything) — ❌ if not
- [ ] `total` / page count exposed for client paging

### Filters & search
- [ ] Documented filter/search params work
- [ ] Garbage/unknown filter value → 200 (narrowed/empty), **never 500**
- [ ] Filtering done in the query, not in memory

### Performance
- [ ] Responds in < 2 s (⚠️ 2–8 s, ❌ > 8 s)
- [ ] Payload bounded (⚠️ > 500 KB)

### Security
- [ ] No token → **401/403** (never 200 with data) — ❌ if open
- [ ] No IDOR — can't read/edit another tenant's/user's record by id — ❌ if possible
- [ ] Bad input → **400** with a clear message (never 500)
- [ ] Filter/search values parameterised (no injection)
- [ ] No secrets/PII/stack traces in the response
- [ ] Token sent in `Authorization: Bearer` header, not the URL
- [ ] (writes) RBAC-gated, ownership-validated, idempotent/transactional

**Result:** ☐ PASS ☐ PASS with ⚠️ (noted below) ☐ has ❌ (not ready)

**Notes / follow-ups:**
-
