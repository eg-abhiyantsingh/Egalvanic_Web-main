# API payload shapes (real responses)

**Verified:** 2026-08-10 against QA **V1.36**, company `d59d449b-09d8-45d6-8f0a-ef70024b1293`.

Recorded because the envelopes are **inconsistent between endpoints**, which is a recurring
source of tests that silently parse nothing and then assert nothing.

## The envelopes are not uniform

```jsonc
// POST /api/company/{cid}/quotes/v2          (Quotes)
// POST /api/company/{cid}/committed-quotes/v2 (EMPs)
{ "success": true,
  "data": { "items": [ … ], "page": 1, "page_size": 50,
            "total": 150, "total_pages": 3, "stats": { … } } }

// GET /api/planned-workorders?page=1&page_size=50
{ …top-level list envelope — NOT wrapped in `data`… }

// GET /api/onboarding/jobs/active?sld_id=…
{ "success": true, "job": { … } }     // key ABSENT when there is no job

// GET /api/company/{cid}/slds
[ … ] or { slds: [ … ] }              // 187 sites on QA acme
```

**Traps:**

- Rows live at **`data.items`**, not `data`, not `quotes`, not `results`. A helper that tries
  `j.quotes || j.data || j.results` returns the *object* `data` for these endpoints, and
  `for (const r of rows)` then throws `is not iterable` — I hit exactly this.
- The count field is **`total`**, not `count`.
- Pagination is `page` / `page_size` (snake), while some other endpoints use different names —
  don't assume one convention across the API.

## Row shape — Quotes / EMPs

```
created_at, id, is_v2, opportunity_id, opportunity_name,
quote_type, sld_id, sld_name, status, title, total_value
```

`sld_id` + `sld_name` on every row is what makes "does this list span sites?" directly
checkable — count the distinct values rather than trusting the request alone.

## Other endpoints seen

| Purpose | Endpoint |
|---|---|
| Assets list (also "is this site empty?") | `GET /api/lookup/v2/nodes/{sld_id}?page=1&page_size=25` |
| Asset filter options | `GET /api/lookup/v2/nodes/{sld_id}/filter-options` |
| Sites for company | `GET /api/company/{cid}/slds` |
| Sites for user | `GET /api/users/{uid}/slds` |
| Roles for user | `GET /api/users/{uid}/roles` |
| Identity + permissions | `GET /api/auth/v2/me` |
| Scheduling data | `GET /api/company/{cid}/workorders-with-jobs`, `…/sessions`, `…/slds` |
| Branding (blocks login render — see memory) | `GET /api/company/alliance-config/acme.egalvanic` |
| Feature flags | `POST /api/features/sync` |

## Ambient 401 noise on every page

`action-items/counts`, `company/{cid}/ops-attention`, `company/{cid}/sales-attention`,
`issues/open-by-site` return **401** on essentially every page load, producing 7–9 console
errors. Health gates must keep ignoring these — but note the cost: a genuinely new severe error
is easy to miss in that crowd.
