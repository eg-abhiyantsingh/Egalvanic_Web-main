# CORRECTION (2026-07-29) — the "CRITICAL dash/underscore twin" claim was an over-claim

The report/PDF in this folder (`Duplicate-API-NotFixed-Reverification.*`, dated 2026-07-23) framed
`/planned-workorder-line/*` vs `/planned_workorder_line/*` as a **CRITICAL dash/underscore twin** —
"one resource split across two spellings that must be collapsed onto a single canonical route."

A deep re-verification on 2026-07-29 (adversarially checking whether the failing tests were valid)
proved that framing **wrong**. From the live `/api/swagger.json`:

| Path family | operationId prefix | summaries | Resource |
|---|---|---|---|
| `/planned-workorder-line/*` (dash) | `quote_api.*_quote_line` | "Create a new **quote line**", "Get a single **quote line**" | **Quote lines** (Sales) |
| `/planned_workorder_line/*` (underscore) | `planned_workorder_line_api.*` | "Create **Planned workorder line**" | **Planned WO lines** (Ops planning) |

They are **two genuinely different resources** with confusingly-similar path spellings — **not** one
resource registered twice. So "collapse them onto one route" would have been the wrong fix (it would
merge two distinct resources). The naming is a real *confusion smell* worth cleaning up, but it is an
**INFO-level hygiene issue, not a CRITICAL split-registration defect.**

## What IS a real, still-live defect (re-verified 2026-07-29)

`GET /api/planned_workorder_line/` (the planned-WO-line **list** read) performs an **unbounded read
and times out** — 45s with no response, identically with `?page=1&per_page=5`, with `?limit=1`, and
with no params. That is a genuine **availability defect** (a list endpoint that never returns),
independent of the naming question. This is what the reframed tripwire
`DuplicateApiAuditTest.testFixCheckUnderscoreListResponds` now guards (RED until it answers <10s).

## Test changes made as a result
- `testFixCheckSingleSpelling` — **retracted/removed** (asserted the false "must collapse" contract).
- `testFixCheckUnderscoreListPaginates` → **`testFixCheckUnderscoreListResponds`** — asserts only the
  real defect (list must respond, not time out); pagination/twin language removed.
- `testDashUnderscoreTwins` — downgraded from `critical` to `info`, reworded to "verify these are
  intentionally distinct resources vs an accidental split."

The screenshots in this folder are still accurate as *evidence of the list timeout*; only the
"twin / must-collapse" interpretation was wrong.
