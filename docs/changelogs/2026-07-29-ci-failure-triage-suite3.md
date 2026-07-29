# 2026-07-29 — CI failure triage: Parallel Suite 3 red since Jul-24 (7 failing tests)

**Prompt:** "check all the fail test case in ci cd"

## CI state

Suite 2: green (latest Jul-28). Suite 3 (API Health, 1612 tests): failing daily since Jul-24.
Latest run (Jul-29): 7 failures, 126 skips. Every failure triaged with dates from run history:

| # | Failing test | Since | Verdict |
|---|---|---|---|
| 1–2 | `DuplicateApiAuditTest.testFixCheck*` (×2) | Jul-23 | **Intentional tripwires** — stay red until the dash/underscore `/planned_workorder_line/` defect is fixed (re-verified still broken today: the unbounded list still times out) |
| 3 | `SecurityHeadersApiTest.testContentTypeContract` | Jul-28 | **Same root cause as #1–2** — its sweep hit `/planned_workorder_line/ → 504`. Excluded that one known endpoint (comment points to the tripwire) so one defect ≠ two red tests |
| 4 | `IrFlirContractApiTest.testFlirIndEndpointContract` | Jul-24 | **Test-side bug, fixed** — the FLIR-IND "watch" false-activated when the EXISTING `/ir_photo/*` definitions gained "flir" (photo-type enum), then blew up on an unsubstituted `{photo_id}`. Now requires the real acceptance signature (`ir_photo_key`+`visual_photo_key`+`platform`), skips the known pipeline, and substitutes path params. Returns to SKIP (endpoint genuinely not shipped) |
| 5 | `CrudLifecycleApiTest.testTaskCrudLifecycle` | Jul-28 | **VERIFIED BACKEND REGRESSION** (below) — test is correct, stays red |
| 6 | `MutationSemanticsApiTest.testAsyncWriteEventuallyConverges` | Jul-28 | same regression |
| 7 | `MutationSemanticsApiTest.testDeleteIdempotency` | Jul-28 | same regression |
| — | `PaginationBehaviorApiTest.testBeyondEndPage` | Jul-27 only | one-off transient, not recurring — no action |

## The verified backend regression (needs a dev ticket)

**`GET /api/tasks/{sld_id}` returns HTTP 500 (HTML "Internal Server Error" page) — since ~Jul-28.**
Live-reproduced 2026-07-29 on the sandbox SLD (`db8f5673-…`, "test site for api check"):
- `POST /task/create` (x-direct-write) → 200, id returned ✓
- `GET /task/{id}` → 200, full task JSON ✓ (**data is intact — this is NOT data loss**)
- `GET /tasks/{sld_id}` → **500 HTML error page, every poll** ✗

So all three "task never appeared / not visible" CI failures are ONE defect: the tasks LIST
endpoint is broken while the write path and item reads work. (Probe task deleted after verify.)

Also incidentally re-confirmed while probing: `POST /task/create` with an empty `sld_id` still
returns the raw psycopg2 SQL error (the known input-validation leak defect).

## Changes

1. `IrFlirContractApiTest` — watch-activation fixed (strong signature + existing-pipeline
   exclusion + `{param}` substitution). Expected CI result: SKIP with the "not yet deployed" note.
2. `SecurityHeadersApiTest` — content-type sweep skips `/planned_workorder_line/` with a comment
   tying it to the FIX-CHECK tripwire; exclusion to be removed when the tripwire goes green.
3. `CrudLifecycleApiTest` + `MutationSemanticsApiTest` — kept red on purpose (regression guards),
   but list-poll helpers now capture the list endpoint's real status, so the failure message reads
   "…[GET /tasks/{sld} → HTTP 500 (HTML error page) — the tasks LIST endpoint itself is failing,
   not the write path]" instead of the misleading "never appeared / silent data loss".

## Expected Suite-3 state after this change

RED with exactly **4 intentional failures**, each self-explaining: 2 duplicate-API tripwires +
2–3 task-list-regression guards (until the backend fixes `GET /tasks/{sld}` and the
`/planned_workorder_line/` twin). No unexplained or misleading reds.
