# Re-verification: systemic API SQL-leak / input-validation defect — RESOLVED

**Date:** 2026-07-31 · **Prompt:** "check this is fixed or not; share screenshot and pdf to update the ticket."
(Ticket: Critical — Info Disclosure + Input Validation; psycopg2/SQL leak on malformed input, API-wide.)

## Verdict: FIXED — all vectors, no leaks
Every vector from the ticket was re-probed live against QA (`acme.qa.egalvanic.ai`, badge V1.36)
from an authenticated Super-Admin session, and each response body tested against the repo's
leak pattern (`psycopg2|sqlalchemy|Traceback|File "/|relation "|column "|SELECT…FROM|line N, in|
InvalidTextRepresentation|NotNullViolation|ProgrammingError|OperationalError`). **Zero leaks.**

| Vector | Before (ticket) | Now (2026-07-31) |
|---|---|---|
| `GET /account/by-company/abc` | 500 + psycopg2 | **400** `Invalid UUID format…` |
| `GET /company/-1/slds` | 500 + psycopg2 | **400** `Invalid UUID format…` |
| `POST /v2/issues/list page=-1` | 500 `OFFSET must not be negative` | **200** clamped |
| `POST /v2/issues/list page=abc` | 500 `unsupported operand … 'str' and 'int'` | **200** clamped |
| `POST /v2/issues/list page_size=-5` | 500 `LIMIT must not be negative` | **200** clamped |
| `POST /v2/issues/list page_size=lots` | 500 `invalid literal for int()` | **200** clamped |
| `POST /task/create {}` | 500 NotNullViolation | **400** `Missing required field: sld_id` |
| `POST /task/create sld_id="not-a-uuid"` | 500 InvalidTextRepresentation | **400** `Invalid UUID format…` |
| `POST /task/create [1,2,3]` | 500 `'list' object has no attribute 'get'` | **400** `Request body must be a JSON object` |
| `POST /issue/create sld_id="not-a-uuid"` | 400 **but leaked psycopg2** | **400** `badly formed hexadecimal UUID string` (no leak) |

All four of the ticket's Expected-Result requirements are met: input caught before the DB → 400;
no SQL/driver/traceback/schema text in any 4xx/5xx; genuine errors return a generic message +
`trace_id` (observed `073d3cd8…350ea`); abusive pagination clamps to 200. The `building/create`
control still returns its clean `400 Missing required field: name`, and the pattern is now applied
surface-wide.

## Minor, non-blocking observations (not the security defect)
- `issue/create` (bad UUID, alt path) returns "An internal error occurred." on an HTTP **400** —
  sanitized/safe but the wording reads like a 5xx; a "bad UUID" message would be clearer.
- Truncated JSON returns Flask's default HTML 400 page (no internals) rather than the app's JSON
  error envelope — shape inconsistency only.

## Deliverables (for the ticket)
`docs/bug-evidence/api-sql-leak-input-validation/`:
- `evidence.html` — source of the report
- `api-sql-leak-reverification-2026-07-31.png` — screenshot to attach
- `API-SQL-leak-reverification-2026-07-31.pdf` — PDF to attach

## Cross-check (repo STRICT gate) + one adjacent gap
`mvn test -Dtest=InputValidationApiTest,ErrorContractApiTest -DSTRICT_INPUT_VALIDATION=true`
(STRICT escalates every finding to a hard fail): **8 of 9 tests green.** The tests that cover
*this ticket* — `testMalformedBodyNeverCrashes`, `testNoServerErrorLeakage`,
`testAbusiveListParamsNeverCrash`, and the malformed-path GET contract — all PASS under STRICT.

The single STRICT red is a **separate, lower-severity concern**, not this ticket's
info-disclosure defect: `testRequiredFieldAndEnumEnforcement` — the create endpoints still
*accept* some invalid-but-well-formed payloads with **201** instead of 400:
- task with wrong field types (task_type=int, title=int) → 201
- task missing required `title` → 201
- issue with invalid status enum `BOGUS_STATUS` → 201
- issue missing required `issue_class` → 201

That's weak server-side validation *depth* (no leak, no 5xx) and deserves its own follow-up
ticket, distinct from the Critical SQL-leak defect (which is resolved). Sandbox issue residue
after the run = 0; `GET /tasks/{sld}` still 500s (the separate tasks-list defect), so the
title-sweep fallback can't run, but the id-based deletes in cleanup do. No probe in the live
re-verification created a record (all failed validation before insert).
