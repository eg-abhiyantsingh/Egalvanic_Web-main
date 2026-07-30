# Frontend evidence — GET /planned_workorder_line/ timeout ticket

Captured 2026-07-30 ~13:15 IST on QA (`acme.qa.egalvanic.ai`, app badge **V1.36**),
authenticated browser session (Super Admin, `abhiyant.singh+admin@egalvanic.com`).

## 1. Where the frontend calls this endpoint family

The UI surface is the **Workorder Details modal** (`WorkorderDetailView.jsx`), mounted on
two pages:

| Page | Route | How it opens |
|---|---|---|
| Jobs | `/jobs` | Expand a job row → click the **work order name** in the tree grid (`handleWorkorderClick`, Jobs.jsx:1196) |
| Scheduling | `/scheduling` | `WorkorderDetailView` is mounted (Scheduling.jsx:1939); the info-click handler exists at Scheduling.jsx:749 |

Frontend calls (from `eg-pz-frontend-reference`, snapshot of `developer` @ d168200, May 30 2026):

| Call | Method | Where |
|---|---|---|
| `/planned_workorder_line/by-workorder/{workorderId}` | **GET** | `WorkorderDetailView.jsx:160` (`loadWorkorderData`) — fired on modal open |
| `/planned_workorder_line/` | POST (create line) | `WorkorderDetailView.jsx:504,620` |
| `/planned_workorder_line/{lineId}` | PUT (edit line) | `WorkorderDetailView.jsx:582` |
| `/planned_workorder_line/analyze-removal`, `/bulk-remove` | POST | `WorkorderDetailView.jsx:650,680` |
| `/planned_workorder_line/bulk-remove`, `/bulk-update` | POST | `quoteLineService.js:97,115` |

**Key finding: no frontend page issues the bare collection `GET /planned_workorder_line/`.**
The UI only ever fetches the *scoped* `by-workorder/{id}` variant. The unpaginated
list-all is hit by API clients/health probes only — so adding pagination + a fast 404
existence check (the ticket's suggested fix) carries **zero UI regression risk**, and the
UI's scoped variant proves a bounded query on the same table returns fast (892 ms below).

## 2. Live network capture (screenshots)

Repro used for the screenshots: `/jobs` → expand job **"dcdsd (2)"** → click work order
**"dcdsd - SV1"** → Workorder Details modal opens and fires:

```
GET https://acme.qa.egalvanic.ai/api/planned_workorder_line/by-workorder/d139b40b-db69-4b9e-937b-cffcd49b832d
→ 200 OK in 892 ms   (performance.getEntriesByType('resource') timing)
```

- `01-jobs-wo-detail-summary.png` — Jobs page with **Workorder Details: dcdsd - SV1** modal (Summary tab) that fires the call on open.
- `02-jobs-wo-detail-scope-items.png` — **Scope tab**: "Scope Items (2)" table rendering the rows returned by the endpoint.

## 3. Bare-collection endpoint measured from the same session (the ticket's defect)

Timed in-page (`fetch('/api/planned_workorder_line/', {credentials:'include'})`, cookies
included, same authenticated session):

```
GET /api/planned_workorder_line/
→ HTTP 504 (CloudFront "Error from cloudfront", x-amz-cf-pop: BOM78-P11)
   after 181,247 ms (~3 min 1 s) — CloudFront origin-read timeout expired;
   backend never responded. Response: 936-byte CloudFront HTML error page.
```

This is **worse than the Prod probe's 15,058 ms FAIL**: on QA today the endpoint does not
return at all — the gateway kills it at ~180 s. The endpoint is effectively down, while
the scoped `by-workorder` query on the same table answers in under a second, which
supports the ticket's query-plan/pagination diagnosis.
