# planned_workorder_line — frontend caller evidence + live 504 measurement

**Date:** 2026-07-30 · **Time:** ~13:20 IST
**Prompt:** screenshot of the frontend page that calls `/planned_workorder_line/` for the
read-timeout ticket (Priority-2, pairs with /ir_session as heaviest scaling endpoints).

## Changes
- New evidence pack `docs/bug-evidence/planned-workorder-line-timeout/`:
  `EVIDENCE.md` + 2 screenshots of the Jobs → Workorder Details modal (Summary + Scope tabs).
- New changelog entries (this file + `docs/changelogs/` twin). No test-code changes.

## How the evidence was captured (depth explanation)
1. **Static trace first, then live proof.** Grepping the frontend reference clone found every
   `planned_workorder_line` call site. The only GET is `WorkorderDetailView.jsx:160` —
   `GET /planned_workorder_line/by-workorder/{workorderId}`, fired by `loadWorkorderData()`
   when the modal opens. The modal opens from Jobs (click a WO name in the tree grid,
   `handleWorkorderClick` → `setWorkorderDetailOpen(true)`) and is also mounted on Scheduling.
   Lesson: the Scheduling "Open Work Order" button instead deep-links to `/sessions/{id}` —
   tracing the state setter (`workorderDetailOpen`) found the real trigger, not the
   likeliest-looking button.
2. **Cookie-auth quirk.** A curl replay with the localStorage `access_token` got 401 —
   `apiClient.js` moved auth to **HTTP-only cookies** (`credentials: 'include'`), so the
   bare-endpoint timing had to run as an in-page `fetch` (browser attaches cookies). The
   fetch was started fire-and-forget onto `window.__pwlTest` and polled, so no tool/client
   timeout could truncate the measurement.
3. **Result:** scoped `by-workorder` → 200 in 892 ms; bare collection → **504 CloudFront
   origin-timeout at 181,247 ms**. Same table, bounded vs unbounded query — the delta is the
   ticket's whole story (missing pagination + query plan), and since no UI page calls the
   bare route, the fix is UI-risk-free.
