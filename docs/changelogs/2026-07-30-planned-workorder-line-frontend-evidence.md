# Frontend screenshot evidence for the /planned_workorder_line/ timeout ticket

**Date:** 2026-07-30 · **Prompt:** "need screenshot of this in front end where this page is calling"
(for the GET /planned_workorder_line/ read-timeout ticket, Prod probe FAIL @ 15,058 ms)

## What was done
1. Grepped `eg-pz-frontend-reference` for `planned_workorder_line` — all callers live in
   `WorkorderDetailView.jsx` (+ 2 bulk POSTs in `quoteLineService.js`). The modal is
   mounted on `/jobs` and `/scheduling`.
2. Drove the live QA app (Playwright, authenticated session): `/jobs` → expanded job
   "dcdsd (2)" → clicked WO "dcdsd - SV1" → Workorder Details modal fired
   `GET /api/planned_workorder_line/by-workorder/{id}` → **200 in 892 ms**. Screenshotted
   the Summary and Scope tabs.
3. Timed the ticket's bare endpoint from the same session (in-page fetch, cookie auth):
   `GET /api/planned_workorder_line/` → **504 from CloudFront after 181,247 ms (~3 min)**.
   Backend never responded; gateway killed it.

## Key findings
- **No frontend page calls the bare `GET /planned_workorder_line/`** — the UI only uses the
  scoped `by-workorder/{id}` variant. Pagination/404-fast-fail on the collection route has
  zero UI regression risk.
- The scoped query on the same table returns in <1 s while the unscoped list 504s at 3 min —
  strong support for the ticket's "no pagination + query-plan/index" diagnosis.
- QA today is worse than the 1-May Prod probe: not slow, **down** (504).

## Artifacts
- `docs/bug-evidence/planned-workorder-line-timeout/EVIDENCE.md` (ticket-ready writeup)
- `01-jobs-wo-detail-summary.png`, `02-jobs-wo-detail-scope-items.png` (same folder)
