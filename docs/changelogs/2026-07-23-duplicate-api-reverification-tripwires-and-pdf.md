# 2026-07-23 — Duplicate-API re-verification: FIX-CHECK tripwires (Suite 3) + developer evidence PDF

**Prompt:** "update this in parallel suite 3 and share me full pdf with proof in screenshot so that
developer can see the issue i need proper screenshot becuase developer told he already fix"
(follow-up to "cehcek this is fixed or not" on the 17-Jul duplicate-API findings).

## What was verified live (23 Jul 2026, authenticated QA session)

| Finding (17 Jul) | Status 23 Jul | Evidence |
|---|---|---|
| CRITICAL dash/underscore twin `/planned-workorder-line` vs `/planned_workorder_line` | **NOT fixed** | Both spellings still in live swagger: 6 dash + 12 underscore paths, writes on both sides |
| Underscore list ignores pagination / times out | **NOT fixed — identical signature** | `GET /planned_workorder_line/?page=1&per_page=5` aborted at 40–45s with no response; bounded `by-workorder/{id}` answers (~6.5s) |
| Paginated dash list `/planned-workorder-lines/` (200 in 1.4s on 17 Jul) | **REMOVED from spec** — regression, broken read is now the only list | swagger diff |
| Dashboard `GET /sites/{id}/status` fetched 2× (with + without `?narrative=false`) | **NOT fixed** | reproduced on a fresh dashboard load (resource-timing log) |
| `GET /users/{id}/roles` 2× exact-URL dup | did **not** reproduce on this sample (1×) | page-dependent; Suite 2 aggregate covers the app-wide picture |
| 13 v1/v2 equipment/SKM-library overlaps | **NOT fixed** | all pairs still in spec |

Also observed (context, not the defect): a QA-wide ambient degradation episode mid-verification —
even control endpoints returned 502/504 (e.g. `/action-items/counts` → 502 after 41s, branding
endpoint curl-timeout for minutes). Probes were re-run after recovery so the evidence isolates the
specific endpoint defect from the outage.

## Changes

### 1. `DuplicateApiAuditTest` (Parallel Suite 3) — two FIX-CHECK tripwires (hard-fail)
- `testFixCheckUnderscoreListPaginates` — RED until `GET /planned_workorder_line/?page=1&per_page=5`
  answers HTTP 200 in <10s. 35s socket budget to measure the hang; failure message carries the full
  history (flagged 09 Jul, dev-reported fixed, re-verified broken 23 Jul) and the fix contract.
- `testFixCheckSingleSpelling` — RED while BOTH spellings are registered in the live spec; failure
  message lists every path+methods on each side so drift is visible in CI.
- Both guard with `requireHealthyBackend()`: SKIP (never false-fail) when the control endpoint
  (`/action-items/counts`) is 5xx or >10s — the ambient-502 episodes would otherwise make the
  tripwire noisy and easy to dismiss.
- Report-mode audit tests unchanged (still green-by-default with `-DSTRICT_SPEC_HYGIENE` gate);
  the tripwires are deliberately exempt from report-mode because the dev claim "already fixed"
  needs a machine answer, not a warning line.
- `writeReport()` preamble now records the 23-Jul re-verification status in
  `reports/api-duplicate-endpoints-report.md` (ships in the api-health-report CI artifact).

### 2. `suite-api-health.xml` — comment on the audit test block documents the tripwires.

### 3. Developer evidence bundle `docs/bug-repro/duplicate-api-endpoints/`
- `Duplicate-API-NotFixed-Reverification.pdf` — self-contained findings PDF: verdict, 4 live
  screenshots (swagger both-spellings, probe race pending, probe race timeout, dashboard
  status-duplicate), 1-minute curl reproduction, fix recommendation, CI tripwire pointer.
- `build_report.py` — HTML assembler (base64-embedded screenshots) → Chrome `--print-to-pdf`,
  same pipeline as the Shea report.
- `shot1..shot4 .png` — the live captures.

## Depth notes (why this shape)

- **Tripwire over report-line:** Suite 3 is a green-by-default monitor; a WARN line is exactly what
  allowed "already fixed" to go unchallenged. A named FIX-CHECK test that stays red converts the
  disagreement into CI state, and flips green the day the contract genuinely holds — no re-audit
  needed.
- **Control-endpoint guard:** verified necessary the same afternoon it was written — the first probe
  run landed inside a QA-wide 502/504 episode and would have produced an unfair "evidence" frame
  (every endpoint failing). Rigor = the tripwire only speaks when the backend is otherwise healthy.
- **Screenshot method:** no Swagger UI on this host (`/api/docs` itself times out), so evidence
  panels are injected into the authenticated SPA page and fetch live — every number on screen is a
  real same-origin request from the real session, and the panel shows origin + ISO timestamp.
