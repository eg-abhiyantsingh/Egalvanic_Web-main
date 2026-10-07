# Web v2.2.4 (14277): ZP-4699 hotfix re-test on stage `index-DY5kMkfp.js`

- **Date:** 2026-10-07, 15:30–15:50 UTC · **Prompt:** "Test now hot fix"
- **Stage build:** `index-DY5kMkfp.js` (15:24 UTC, PR #1681 "ZP-4699: Skip the SLD graph fetch and refuse the shutdown
  overlay without sld_diagrams.view"). Still live at 15:46 UTC.

## ZP-4699: step 5 partly fixed, ticket back to To Do
Client Portal seat abhiyant.singh+cpstage@, own site "Atest" (`db9b8488-adec-4f85-b85b-38c02c8e9415`):

| Call | First round (C9Xyd0R1) | Hotfix (DY5kMkfp) | Admin control |
|---|---|---|---|
| `GET /api/sld/{id}/graph` | 200, whole diagram | **422** permission_denied (fixed) | 200 |
| `GET /api/sld/{id}/views` | 422 | 422 | 200 |
| `GET /api/sld/{id}` | 200 | **200**: 9 nodes + 3 edges | 200 |
| `GET /api/sld/{id}/edges` | 200 | **200**: 3 rows with drawing points | 200 |

- No Client Portal screen calls the record or edges routes. Network was recorded on 12 pages; the only `/api/sld/…` calls are
  `emp-compliance-schema` (Compliance) and `work-order-reports` (Reports). Refusing the two routes breaks nothing.
- Screens (steps 1–4, 6): pass, unchanged. Regression (step 7): PM, EE, FM and Admin keep the Engineering tab, Edit
  Connection, and the SLD page (graph, views and view-mappings all 200).
- Step 12 (Interactive license through `PUT /api/account/{id}`): still accepted. Avani's comment 44960 says it waits on a
  product decision (A or B) and ships as its own change.
- Side note: CP Work Orders and Reports make 422 calls (`workorders/v2`, `procedures-v2/services`) with no toast. QA's
  older build does the same, so the hotfix didn't cause it.

**Jira:** comment **44961** posted as Abhiyant Singh. ZP-4699 moved **In QA → To Do** (transition 2). The To-Do checks:
the failure reproduces on the live build, the admin control works, the screenshot exists, the ticket text requires it
("the route, and the API behind it, must refuse the request", step 5), and no later comment overrides it.
**Report page:** https://claude.ai/artifact/9qMh3c9Sp3eh7MhLden37i v4 (new "Hotfix re-test" section on top).
**Evidence:** `docs/bug-evidence/2026-10-07-zp4699-stage-hotfix2/` (step 5 screenshot, CP portal, Connections Graph,
the Engineering tab and SLD page for each internal role).

## READY TO RELEASE tickets on the hotfix build: no regression
9 seats, Maintenance Portal rail + `/maintenance-portal/overview` + `/sessions`:

| Seat | Portal in rail / route | Work Orders | Create Work Order |
|---|---|---|---|
| Admin, Super Admin, dharmesh (reporter) | yes / opens | 1–25 of 28 | shown |
| Project Manager | yes / opens | 1–1 of 1 | shown |
| Facility Manager | yes / opens | 1–25 of 27 | hidden (tier T2, by design) |
| Account Manager, Electrical Engineer | yes / opens | Access Denied | — (the gap already known on QA and stage) |
| Client Portal | yes / opens | Access Denied | — |
| Technician | Web Access Restricted | — | — |

ZP-4703 / ZP-4572 (portal for every role except Technician) and ZP-4701 (Create Work Order) hold on the hotfix build.
Evidence: `docs/bug-evidence/2026-10-07-v224-stage-recheck/*-portal.png`, `pm-work-orders.png`. No Jira change made.
