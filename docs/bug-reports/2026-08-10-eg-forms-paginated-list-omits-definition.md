# [API] `/api/eg-forms` paginated responses silently omit `definition`, `html_template`, `sample_data`

**Env:** QA V1.36 · **Found:** 2026-08-10 · **Severity:** Medium · **Priority:** Medium

## The defect

The same endpoint returns **two different row shapes** depending on whether pagination
parameters are supplied, with no indication that fields were dropped.

| Request | Keys per row | `definition` |
|---|---|---|
| `/api/eg-forms` (unpaginated) | **24** | present — up to **49 KB** |
| `/api/eg-forms?page=1&page_size=5` | **21** | **absent** |

Fields present unpaginated but missing when paginated: **`definition`, `html_template`,
`sample_data`** — i.e. the entire form body. The paginated row is a summary projection, but
nothing in the response says so: there is no `fields`/`view` parameter, no metadata flag, and
the omitted keys are simply absent rather than null.

## Why it matters

A caller that paginates (the documented, encouraged path — and what the web app itself does)
receives forms **with no definition** and cannot tell the difference between "this form has an
empty definition" and "this projection does not include definitions". Any consumer that
paginates for performance and then inspects `definition` sees every form as empty.

**This is not hypothetical — it produced a false QA finding during this session.** A scan run
over the paginated endpoint reported "344/344 forms have empty definitions", which contradicted
an earlier unpaginated scan showing definitions up to 14 KB. The discrepancy was the projection,
not the data. Re-run unpaginated: **328 of 344 forms have real definitions**, largest 49 KB.

## Steps to reproduce

```js
const paged = await (await fetch('/api/eg-forms?page=1&page_size=5', {credentials:'include'})).json();
const full  = await (await fetch('/api/eg-forms', {credentials:'include'})).json();
const row   = (paged.data||paged)[0];
const same  = (full.data||full).find(f => f.id === row.id);
'definition' in row;                              // false
JSON.stringify(same.definition).length;           // 4595
```

## Suggested fix

Either return the same shape from both (and let callers project explicitly via a `fields`
parameter), or make the projection **explicit and discoverable** — document it, and return
`definition: null` rather than omitting the key, so a consumer can distinguish "not included"
from "empty".

## Related

Same endpoint also ignores `page_size` unless `page` is present — see
`2026-08-10-forms-list-api-pagination-gaps.md`. Both are variants of the same problem: request
parameters silently changing or discarding the response with no signal to the caller.
