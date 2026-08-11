# [Tasks] `/api/tasks/all` (PR #872) returns HTTP 500 — and the All Facilities UI does not use it

**Env:** QA · https://acme.qa.egalvanic.ai · build V1.36 · **Tested:** 2026-08-11
**Ticket:** Tasks — All Facilities endpoint for cross-site task view
**PR:** Backend #872 — "real /tasks/all endpoint (All Facilities view)"
(pairs with frontend #1057: *"Tasks keeps 'all' — now a real endpoint via eg-pz-backend pair
PR (its All Facilities view was a masked 404)"*)

## TL;DR

The **feature works for users**, but **not through the endpoint this ticket delivers**.

- `POST /api/v2/tasks/list` with `sld_id` omitted powers the All Facilities view and works:
  1603 tasks across every accessible site, with sort / filter / search / pagination.
- `GET /api/tasks/all` — the endpoint PR #872 exists to provide — returns **HTTP 500 on every
  call** and is called by **nothing** in the Tasks UI.

## HARD FACTS (measured, reproducible)

### 1. `/api/tasks/all` returns 500 on every request

| Request | Result |
|---|---|
| `GET /api/tasks/all` | **500** `{"error":"internal_error","success":false,"trace_id":"8bd9a1cd…"}` |
| `…?company_id=…` | 500 |
| `…?page=1&page_size=10&sort_by=created_at&sort_dir=desc` | 500 |
| `…?sld_id=…` | 500 |
| `…?limit=5` | 500 |

It genuinely exists (real JSON error body + `trace_id`), not the masked-404 SPA shell — a
**control** request to `/api/tasks/definitely-not-real` returned `200 text/html` (the SPA shell),
confirming `/tasks/all` is a routed endpoint that is *erroring*, not merely absent. Each 500
carries a distinct `trace_id` (e.g. `8bd9a1cd0e034b43b2666fc6fd3975de`) for the backend team.

### 2. The All Facilities view uses a different endpoint entirely

Selecting **All Facilities** in the Tasks grid fired **`POST /api/v2/tasks/list`** (200), plus
`/api/tasks/stats`. `/api/tasks/all` was **never called**. Captured request bodies:

```
single facility : {"sld_id":"a3ed…3489","page":1,"page_size":25,"filters":{"status":"pending"},"search":"","sort_by":"due_date","sort_dir":"asc"}
All Facilities  : {                    "page":1,"page_size":25,"filters":{"status":"pending"},"search":"","sort_by":"due_date","sort_dir":"asc"}
```

The ONLY difference is that `sld_id` is omitted for All Facilities. Same endpoint, sld-scope
dropped — the exact "FORCED_ALL" pattern the other v1.36 scope PRs use.

### 3. The All Facilities capability WORKS (all 5 QA items pass, via v2/tasks/list)

| QA item | Result |
|---|---|
| All Facilities shows tasks from every site | **PASS** — total 1603; a single 100-row page spanned **13 distinct sites** |
| Single-facility still filters | **PASS** — `sld_id` present → scoped (0 pending on the empty test site, correct) |
| Sorting | **PASS** — `due_date` asc first row 2025-11-14; desc first row 3203-12-11 (garbage QA date, but sort honoured) |
| Filtering / search | **PASS** — `status:pending` = 1603; `search:"test"` narrows to 821 |
| Pagination | **PASS** — page 1 ≠ page 2, total stable; **page_size capped at 100** (requested 100000 → 100 returned) |
| Response time on many sites (tenant has 187) | **PASS** — 256–698 ms across all calls |

## THE QUESTION FOR THE AUTHOR (I cannot answer this — backend repo not readable)

Two readings, and I will not guess between them:

**(a) `/tasks/all` is superseded / orphaned.** The team built All Facilities by omitting
`sld_id` on `v2/tasks/list` instead, and `/tasks/all` is dead code that should 404/410 — not
500. Severity: Low (hygiene), but a 500 with a trace_id pollutes error monitoring and any
external caller of the documented endpoint breaks.

**(b) `/tasks/all` is the intended endpoint** and the frontend was simply never wired to it.
Then #872 shipped broken and the "All Facilities" win is riding on a different endpoint by
accident. Severity: Medium — the ticket's own deliverable is non-functional.

Either way there is a real defect: an endpoint named in a merged PR returns 500. Which severity
depends on the answer.

## What I did NOT verify

- **Cross-tenant tenancy (QA item 3).** I have one company and one admin login, so I confirmed
  All Facilities spans the *sites within this tenant* but cannot prove another tenant's tasks
  are excluded. Needs a second-tenant account.
- **Whether `/tasks/all` requires a param shape I didn't try.** I tried 5 combinations, all 500;
  a required body/param I didn't guess could change that, but a 500 (not 400/422) on a missing
  param would itself be a validation defect.

## Reproduce

```js
// 500 on the ticket's endpoint:
await fetch('/api/tasks/all', {credentials:'include'}).then(r=>r.status);   // 500
// the endpoint the UI actually uses for All Facilities (omit sld_id):
await fetch('/api/v2/tasks/list', {method:'POST', credentials:'include',
  headers:{'Content-Type':'application/json'},
  body:JSON.stringify({page:1,page_size:25,filters:{status:'pending'},sort_by:'due_date',sort_dir:'asc'})
}).then(r=>r.json()).then(j=>(j.data||j).total);   // 1603, across all sites
```
