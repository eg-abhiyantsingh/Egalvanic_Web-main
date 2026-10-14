# ZP-4574 — Merge Alembic heads tqlock_a1 + swjrnl_a8 to unblock cicd/dev migrations

**Verdict:** NOT WEB-TESTABLE (backend / migration-DAG only) — build `index-D1IWZ6SI.js`. Ticket env: "Dev only (cicd/dev; not yet in QA)."

## Checked
| Check | How | Result |
|---|---|---|
| Single Alembic head on cicd/dev | needs `alembic heads` / `ScriptDirectory.get_heads()` from a dev shell | NOT web-observable |
| Merge revision is a no-op (no schema change) | needs the migration DAG | NOT web-observable |
| Indirect: QA backend boots / serves (migrations apply cleanly) | `GET /api/health`, `GET /api/swagger.json`, `GET /api/mutation/health` | 200 / 200 (1,349 paths) / 200 — the backend is up and serving, so whatever migrations exist on QA applied without a "Multiple head revisions" failure. PASS (indirect) |

This is a no-op merge revision (`mrg_tqlock_swjrnl`, down_revision → both heads) reconciling the migration DAG on cicd/dev. There is no product behaviour and no API surface to exercise.

## Bugs found
None observable.

## Enhancement suggestions
- CI gap the ticket itself hints at: two PRs revised the same revision 21 s apart with no guard. A pre-merge `alembic heads == 1` check in CI would prevent the whole class of "Multiple head revisions" deploy failures. (Process/CI, not a web ticket.)

## Mobile part for Nency
None — backend migration only.

## Not covered (why)
Everything substantive — single-head verification and no-op confirmation require a dev shell (`alembic heads`, `get_heads()`). The only web signal is that the backend boots and serves, which it does.

## Proposed Jira comment
h3. QA — ZP-4574 NOT WEB-TESTABLE (migration DAG only) — QA build index-D1IWZ6SI.js

A no-op Alembic merge revision reconciling two heads on cicd/dev. Nothing to exercise from the web. The only indirect signal available to QA is that the backend boots and serves — {{GET /api/health}} → 200, {{GET /api/swagger.json}} → 200 (1,349 paths) — so migrations applied without a "Multiple head revisions" failure.

!QA-12Oct-ZP-4574-1-backend-serves-not-web-testable-proof.png|width=720!

Please confirm from a dev shell: {{alembic heads}} (or {{ScriptDirectory.get_heads()}}) returns exactly one head, and both the {{tqlock}} and {{swjrnl}} migrations remain applied.

!QA-12Oct-ZP-4574-2-build-and-env-note.png|width=720!

## Recommended status
**Stay In QA** until a dev confirms a single head on cicd/dev (or the owner accepts it as a dev-verified housekeeping merge). Not a candidate for a QA PASS/FAIL on its own.
