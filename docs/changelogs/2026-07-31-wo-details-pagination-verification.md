# Verification: Work Order Details — paginate all content sections → IMPLEMENTED (1 gap)

**Date:** 2026-07-31 · **Prompt:** check whether the WO-Details pagination improvement (Assets, Issues,
Tasks, Photos, Attachments) is fixed; if not, share screens in a PDF with description.

## Verdict: IMPLEMENTED for all five named sections; **Forms** still unpaginated
Verified live on QA (`/sessions/{id}`, badge V1.36) from an authenticated Super-Admin session by
activating each tab and isolating its API calls (resource-timing diff per click).

| Section | Endpoint called | Page size | Verdict |
|---|---|---|---|
| Assets | `assets/v2?limit=20&offset=0` | 20 | paginated |
| Tasks | `tasks/v2?limit=20&offset=0` | 20 | paginated |
| Issues | `issues/v2?limit=20&offset=0` | 20 | paginated |
| IR Photos | `photos/v2?limit=25&offset=0` | 25 | paginated |
| Attachments | `attachments/v2?limit=25&offset=0` | 25 | paginated |
| **Forms** | `eg-form-instance/by-session/{id}` — **no params** | — | **not server-paginated** |

Sections fetch **lazily on tab activation**, so opening a work order no longer downloads every
section. A `summary/v2` endpoint supplies per-section totals (`counts:{assets,issues,tasks,photos,
attachments,asset_nodes}`) without fetching rows.

### Proof it is genuinely server-side (not client-side slicing)
1. Changing "Rows per page" to 10 issued a **new** request `photos/v2?limit=10&offset=0` (a
   client-side grid would re-slice in memory with no network call).
2. `offset=0` vs `offset=3` (limit 3) returned **different** records with echoed metadata
   `pagination {limit, offset, total: 8, has_more}`.

### Acceptance criteria
- Page size 20–25 + controls — **MET**
- Backend limit/offset per section — **MET** (Forms excepted)
- Total count per section — **MET** (tab badges + heading "IR Photos (8)")
- 50+ item load-time improvement — **NOT VOLUME-TESTED**: QA tenant has no 50+ item work order
  (largest available: 8 photos). Mechanism verified; the numeric target needs seeded data.

## Recommendation
Close the main scope; keep/split two items: (a) paginate the **Forms** sub-tab for parity;
(b) measure the 50+ item load time once a large work order exists on QA.

## Deliverables (attach to ticket)
`docs/bug-evidence/wo-details-pagination/`:
- `WO-Details-pagination-verification-2026-07-31.pdf` (4 pages, screenshots + description)
- `WO-Details-pagination-verification-2026-07-31.png`
- `report.html` (source) + 3 app screenshots (IR Photos, Assets, Forms)

## Important note on the stale reference clone
`eg-pz-frontend-reference` (snapshot of `developer` @ d168200, 30 May 2026) still shows the
**pre-fix** pattern — one `/ir_session/{id}/full` payload carrying tasks+issues+photos, unbounded
`/assets`, `getAttachments()` with no params, and every grid `paginationMode="client"`. It contains
no `/v2` endpoints at all. A code-only audit of that clone concludes "no pagination anywhere," which
is wrong for the live build. **Live behavior is authoritative; refresh the clone before code audits.**
