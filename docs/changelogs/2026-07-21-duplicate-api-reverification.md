# 2026-07-21 — Re-verification of the 2026-07-17 duplicate-API reports (fixed or not?)

**Prompt:** owner attached the two Jul-17 handoff reports (runtime duplicate calls + spec-level
duplicate-endpoint audit) and asked (a) is it fixed, (b) is the audit in Parallel Suite 3.

## Verdict A — the CRITICAL spec finding is NOT fixed (re-verified live today)

Dash/underscore twin `/planned-workorder-line/` vs `/planned_workorder_line/`:

| Check (live QA, authenticated, 2026-07-21) | Result |
|---|---|
| swagger.json registrations | **Both spellings still present** (spec now 961 paths vs 957 on Jul 17). Dash family: 6 paths. Underscore family: **12+ paths and GROWING** — new `activation`, `activation-details`, `activation-bulk`, `analyze-removal`, `bulk-remove` endpoints were added to the **underscore** spelling since the report. |
| Method drift on `{line_id}` | Unchanged: dash = GET only; underscore = GET + PUT + DELETE. |
| `GET /api/planned-workorder-lines/?page=1&per_page=5` | HTTP 200 in **1.44 s**, ~2 KB (paginated) — same as Jul 17. |
| `GET /api/planned_workorder_line/?page=1&per_page=5` | **Still hangs — client abort at 25 s**, no response (Jul 17: 36 s+ timeout). Unbounded read unchanged. |

Not only is it not fixed — the drift the report warned about is actively worsening: all the new
v1.35 planning/activation write-endpoints landed on the broken-read underscore spelling.

Runtime (frontend) duplicates, fresh `/dashboard` loads ×2 today:
- `GET /sites/{id}/status` bare **and** `?narrative=false` → **still both fired on every load** (not fixed).
- `GET /users/{id}/roles` 2× exact dupe → **not reproduced** in either sample today (1× both loads);
  may be fixed or timing-dependent — the 21/68 aggregate needs a Suite-2 `api-network` re-run to re-measure.

## Verdict B — yes, both audits are wired into the parallel suites

- **`DuplicateApiAuditTest`** (spec audit, source of the duplicate-endpoints report) →
  `suite-api-health.xml:95` → run by **Parallel Suite 3** (`.github/workflows/parallel-suite-3.yml`,
  `api-health-check` job: `mvn -DsuiteXmlFile=suite-api-health.xml`). Four checks: dash/underscore
  twins (critical), trailing-slash twins (critical), singular/plural roots (info), v1/v2 overlaps
  (info). Writes `reports/api-duplicate-endpoints-report.md` every run.
- **`ApiDuplicateCallTestNG`** (browser-driven runtime duplicates) → `suite-network-api.xml:15` →
  run by **Parallel Suite 2** (`api-network` group, API toggle default-on). Deliberately kept out
  of Suite 3 (browser-driven; the suite-api-health.xml comment documents this). Writes
  `reports/api-duplicate-calls-report.md`.
- Both run in **report mode** in CI: the escalation flags `-DSTRICT_SPEC_HYGIENE` /
  `-DSTRICT_DUPLICATE_API` exist in code but no workflow sets them, so findings are logged and
  reported, never red-failing the build. Flipping STRICT_SPEC_HYGIENE on Suite 3 would make the
  dash/underscore twin a hard failure every run until backend fixes it — owner's call.

## Recommendation
Re-escalate the dash/underscore twin to backend with today's evidence (esp. that new activation
endpoints are still being added to the timing-out underscore spelling — the consolidation cost
grows every sprint it slips).
