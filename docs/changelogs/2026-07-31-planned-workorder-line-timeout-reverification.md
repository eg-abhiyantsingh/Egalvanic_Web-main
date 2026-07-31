# Re-verification: GET /planned_workorder_line/ timeout — STILL NOT FIXED

**Date:** 2026-07-31 · **Prompt:** re-check the `/planned_workorder_line/` timeout ticket (Medium-High,
reliability/scaling) and produce screenshot + PDF to update it.

## Verdict: NOT FIXED
Re-probed live on QA (`acme.qa.egalvanic.ai`, V1.36) from an authenticated session:

| Request | Result |
|---|---|
| `GET /planned_workorder_line/` (bare) | **client read timeout > 30 s** (504 at ~181 s earlier today) |
| `GET /planned_workorder_line/?page=1&per_page=5` (suggested fix) | **client read timeout > 30 s** — pagination doesn't help |
| `GET /planned_workorder_line/0000…0000` (unknown id) | **200 + HTML** SPA shell in ~1.3 s — not a 404 |
| `GET /planned_workorder_line/not-a-uuid` (malformed id) | **500** + trace_id in ~1.1 s — sanitized, not a 404 |
| `GET /planned_workorder_line/by-workorder/<id>` (bounded baseline) | **200**, count 2, ~1.1 s — fast |

All three Expected-Result requirements remain unmet: no SLA return; `page`/`per_page` still times out
(so the pagination + query-plan/index fix is not effective/deployed on this route); unknown/malformed
ids do not fail fast with 404. The bounded `by-workorder` path answering in ~1 s on the same table
confirms the root cause is the **unbounded list read**, not raw table size.

## Corroboration
- Earlier this session: bare endpoint → CloudFront **504 after 181,247 ms** (`x-cache: Error from cloudfront`).
- CI Parallel Suite 3 (2026-07-30): `?page=1&per_page=5` → socket timeout at 35,043 ms ("NOT FIXED — unbounded list read that never returns").

## Deliverables (attach to ticket)
`docs/bug-evidence/planned-workorder-line-timeout/`:
- `reverification-2026-07-31.html` (source)
- `planned-workorder-line-timeout-reverification-2026-07-31.png` (screenshot)
- `planned-workorder-line-timeout-reverification-2026-07-31.pdf`

Frontend caller + scope from the earlier evidence: same folder's `EVIDENCE.md`.

## Method note
Timed via concurrent in-page fetches on page globals; the two hanging calls used a 30 s
`AbortController` (the ticket's "client-side read timeout"). Fast-path timings are from the unblocked
run — a re-run inflates them because the hanging calls head-of-line-block the connection.
