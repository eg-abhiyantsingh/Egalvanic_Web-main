# [API] Forms list endpoints — `/api/forms` unpaginated; `/api/eg-forms` silently ignores `page_size` without `page`

**Env:** QA V1.36 · **Found:** 2026-08-10 · **Severity:** Medium · **Priority:** Medium
Measured as internal Admin. Checked against this repo's own `api-contract-review` rules:
*"Every list endpoint must be paginated, cap at a max page size, default to a small page size (10–20)."*

## Finding 1 — `/api/forms` has no working pagination at all

| Request | Rows | Payload |
|---|---|---|
| `/api/forms` | 169 | **5.60 MB** |
| `/api/forms?page=1&page_size=5` | 169 | **5.60 MB** |
| `/api/forms?page=1&per_page=5` | 169 | **5.60 MB** |

Every parameter combination returns the full 5.6 MB. No `page`, `page_size`, `per_page` or
`limit` form has any effect. There is no cap and no small default — the contract's three core
rules are all unmet on this endpoint.

## Finding 2 — `/api/eg-forms` paginates only when `page` is present

| Request | Rows | Payload |
|---|---|---|
| `/api/eg-forms` *(no params — the default)* | **344** | **3.69 MB** |
| `/api/eg-forms?page_size=5` *(alone)* | **344** | **3.69 MB** ← silently ignored |
| `/api/eg-forms?page=1&page_size=5` | 5 | 0.01 MB |
| `/api/eg-forms?page=1&page_size=25` | 25 | 0.06 MB |
| `/api/eg-forms?page=1&page_size=10000` | **200** | 0.47 MB ← max page size correctly capped |

Two issues:
- **The default is unbounded** — a caller who passes nothing gets all 344 rows / 3.69 MB.
  The contract asks for a small default (10–20).
- **`page_size` is silently ignored unless `page` is also supplied.** A caller asking for 5 rows
  receives 3.69 MB and **no error and no warning** — the parameter is accepted and discarded.
  Silent parameter-dropping is the worst failure mode here: the client believes it is paginating.

Working correctly: the max page size **is** capped at 200, so `page_size=10000` cannot be used
to pull everything in one request.

## Not a user-facing bug today

The web app itself paginates correctly — the EG Forms page requests
`/api/eg-forms?page=1&page_size=25&exclude_types=3,4`. So this is an **API-contract** problem
affecting integrators, scripts, the mobile client and any future caller, not a live UI slowdown.
I verified this rather than assuming: my first measurement (no params) suggested the app was
pulling 3.69 MB per page load, which the captured network traffic disproved.

## Suggested fix

1. `/api/forms`: implement pagination, or document it as intentionally unbounded and cap it.
2. `/api/eg-forms`: honour `page_size` without requiring `page` (default `page=1`), and give the
   collection a small default page size instead of returning everything.
3. Reject unknown/ineffective pagination params with a 400 rather than dropping them silently.
