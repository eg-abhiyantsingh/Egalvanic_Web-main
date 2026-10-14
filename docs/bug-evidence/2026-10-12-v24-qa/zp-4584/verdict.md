# ZP-4584 — Track per-environment infra bootstrap (schedule-notices, close-stale) with Success-console run markers

**Verdict:** NOT WEB-TESTABLE (infra / CI scripts only) — build `index-D1IWZ6SI.js`. Ticket: "Dev only … Scripts/infra only — no in-product behavior to exercise."

## Checked
| Check | How | Result |
|---|---|---|
| `--enable` writes SSM marker `/eg-pz/bootstrap/be/<script>/<env>` (commit/time/principal/host) | AWS SSM Parameter Store, no API surface | NOT web-observable |
| `--disable` removes the marker | AWS SSM | NOT web-observable |
| Success-console check per armed env (notices: dev; close-stale: dev+qa) | internal CI/Success console | NOT web-observable |
| `infra/README.md` + task.py add-script docs | repo/doc review | Doc review only |
| Indirect: QA backend healthy (scheduled jobs still run) | `GET /api/health`, `/api/swagger.json` | 200 / 200 — nothing regressed. PASS (indirect) |

Moving `admin-scripts/` → tracked `infra/` with SSM run markers is pure DevOps plumbing. No product/web behaviour changes.

## Bugs found
None observable.

## Enhancement suggestions
- None web-facing. (If anything: surface the "armed per env" markers somewhere visible to the team, e.g. a status row, so the "kept missing it" problem the ticket describes is self-evident without opening the Success console.)

## Mobile part for Nency
None — infra only.

## Not covered (why)
SSM marker write/remove on `--enable`/`--disable` and the Success-console checks are AWS/CI-internal with no API surface. A DevOps/dev must show them.

## Proposed Jira comment
h3. QA — ZP-4584 NOT WEB-TESTABLE (infra/CI scripts only) — QA build index-D1IWZ6SI.js

Scripts moved to a tracked {{infra/}} folder with SSM run markers + Success-console checks. No in-product behaviour to exercise — nothing on the web app changes, and the QA backend stays healthy ({{/api/health}} 200). The SSM marker on {{--enable}}/{{--disable}} and the per-env Success-console checks (notices: dev; close-stale: dev+qa) are AWS/CI-internal; please confirm from DevOps.

!QA-12Oct-ZP-4584-1-infra-only-not-web-testable-proof.png|width=720!
!QA-12Oct-ZP-4584-2-build-and-scope-note.png|width=720!

## Recommended status
**Stay In QA** until DevOps confirms the SSM markers + Success-console checks (or the owner accepts it as dev-verified infra housekeeping). No QA PASS/FAIL possible from the web.
