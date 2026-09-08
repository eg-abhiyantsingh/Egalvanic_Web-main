# ZP-3938 — every company Issues list returned 500 — captures (QA V1.36, bundle `index-CSsDpG3c.js`, 2026-09-08)

## The endpoint answers 200
`POST /api/v2/issues/list {"company_id":"d59d449b-09d8-45d6-8f0a-ef70024b1293","page":1,"page_size":5,"filters":{},"search":""}` → **200**, `total: 1059`, 5 items returned. Called many times across this session (it is the list call behind both pages) — **no 500 at any point**.

## Both pages render
- `/issues` (company Issues register) rendered its grid with rows on every visit — Asset · Title · Issue Class · Priority · Status.
- `/pull-through-work` rendered stat cards (NO RESOLUTION SET 965 · READY TO QUOTE · QUOTED 27 · RESOLVED 65) and 25 rows of 925–1059, spanning several sites.
Both were loaded repeatedly while other work happened; neither produced an error page or a 500 in the network log.

## `resolution_processing_at` is NOT the field name on QA
Every row's resolution-related keys are: **`proposed_resolution`, `resolution_counts`, `resolution_processing`, `resolution_state`** — `'resolution_processing_at' in item` is **false** for all rows, and reading it yields `undefined`. So the serializer field on this build is `resolution_processing` (no `_at` suffix). The bug the ticket fixes (the missing column killing the request) is gone; the exact field name in the ticket's step 3 does not match what QA serves, so "confirm `resolution_processing_at` present on each row, null for issues never evaluated" could not be confirmed as written.

## Empty result is clean
Same call with `search: "zzzz-no-such-issue-zzzz"` → **200** `{items: [], page: 1, page_size: 5, total: 0, stat_counts: {awaiting_review: 13, quoted: 27, ready: 2, resolved: 65, unset: 965}}` — an empty list plus intact stat counts, not an error.

## Not covered
A company with genuinely zero issues (the acme tenant has 1059; the empty case was produced with an unmatchable search instead) and an issue caught mid-background-evaluation with a non-null processing timestamp — `resolution_processing` read as absent/undefined on every row sampled, and no evaluation was in flight at sample time.
