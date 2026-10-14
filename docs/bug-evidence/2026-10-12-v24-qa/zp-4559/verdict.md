# ZP-4559: Work-order list: full-access roles (PM/EE) win over Account Manager scoping

**Verdict: NOT WEB-TESTABLE for the core case (Account Manager + PM/EE on one non-staff user). The single-role regression checks PASS.**
QA build `index-COeCBn0z.js` (13 Oct; checked live at 13:51 IST). The fix is backend only (eg-pz-backend #1420), and it has no LaunchDarkly flag.
The live ticket was re-read on 13 Oct: status In QA, no comments. The ticket itself says "qa / stag cherry-picks still pending".
QA exposes no build or version endpoint (`/api/health` returns only `status` and `uptime`). So I cannot prove that #1420 is deployed on the QA backend.

**Why the core case cannot be tested:** none of our QA seats is a customer user who holds Account Manager together with PM or EE.

| Seat (AppConstants) | Roles from `/auth/v2/me` | is_eg_admin | Can it test AM + PM/EE? |
|---|---|---|---|
| `VALID_EMAIL` = `ADMIN_EMAIL` (+admin@) | Super Admin, Admin, Portal Sales, Electrical Engineer, Project Manager, Account Manager | **true** | No. It holds Admin/Super Admin, so it returns early through ADMIN_ROLES. The new ordering is never reached. |
| `ADMINQA_EMAIL` | Admin | true | No |
| `PM_EMAIL` | Project Manager | false | No (single role) |
| `AM_EMAIL` | Account Manager | false | No (single role) |
| `EE_EMAIL` | Electrical Engineer | false | No (single role) |
| `FM_EMAIL` | Facility Manager | false | No |
| `CP_EMAIL` | Client Portal | false | No |
| `TECH_EMAIL` | Technician | false | No |
| `PORTAL_SALES_EMAIL` | Portal Sales | false | No |

A teammate's QA "all roles" seat is listed in memory. I did not use it: it is not in AppConstants, and the session's permission check refused using its credentials. It probably holds Admin too, which would also return early.

**Seat needed from the dev or the owner (no user was created and no roles were changed):** a non-staff Acme QA user (`is_eg_admin:false`, no Admin or Super Admin) with the roles **Account Manager + Project Manager** (a second variant with **Account Manager + Electrical Engineer** would also help). It must have **no account links**: not "mapped" on any account access list and not an account owner. Give it a few sites.
Expected with the fix: its `POST /company/<id>/workorders/v2` and `GET /company/<id>/sessions` return every work order on its sites, the same set as a PM-only user on the same sites. Expected without the fix: an empty list.

## Checked

| Check | How | Result |
|---|---|---|
| Live build | `curl` the index and `document.scripts` in every run | `index-COeCBn0z.js` |
| Roles of every QA seat | In-page `POST /api/auth/login` (one isolated context per seat), then `GET /auth/v2/me` | Table above. No customer seat holds AM + PM/EE |
| **PM-only = full access** | All pages of `workorders/v2` (page_size 100) plus `GET /sessions`, compared with Super Admin's list filtered to the PM's `accessible_sld_ids` (116 sites) | PASS: `/sessions` 1080 = **all** 1080 Super-Admin work orders on those sites (0 missing, 0 extra). v2 = 1195 (1080 work orders + 115 planned rows) |
| **EE-only = full access** | Same method, 4 sites | PASS: 143 = all 143 on its sites |
| **AM-only = account scope** | Same method on its 113 sites. Account links read with the Super Admin seat from `GET /account/{id}/access-list` (source `mapped`); the AM's own account list comes from `POST /account/by-company/{id}/v2` | PASS: 1072 work orders, **all** on the AM's 9 mapped accounts (746 + 165 + 55 + 44 + 32 + 19 + 6 + 3 + 2). Work orders with no account (6) and one from a soft-deleted or unlisted account (1) are excluded. 8 fewer than the PM sees on the same sites. One edge case is listed under Observations |
| Other roles = "assigned only" | Technician on 113 sites | PASS: `/sessions` = 1 of the 1080 on its sites |
| Facility Manager | 11 sites, account user of "Abhiyant Singh" | 6 = all 6 work orders on its sites, all on its own account. This is the account-user model, not "assigned only". It is the same count as 12 Oct, so the ticket did not change it |
| Client Portal / Portal Sales | API | 422 `permission_denied` (needs `company_data.view`). Same as before |
| Staff seats | Super Admin and Admin | Both 1557 v2 rows / 1378 work orders (whole company) |
| Web Work Orders page vs API | Real UI sign-in per seat, open `/sessions`, read the grid footer and the page's own `workorders/v2` call (default filter Open + Overdue, `include_planned:false`) | PM **96** = API total 96 (screen 1). Super Admin 200 = 200 (screen 3). Admin 200 = 200. FM 1 = 1 (screen 5). **AM-only and EE-only: Access Denied** (screens 2 and 4): the route needs `features.site_visits.view`, which AM and EE do not hold, and the nav does not offer the page either, so nav and route agree. CP: Access Denied. Technician and Portal Sales: "Web Access Restricted" |
| Both endpoints agree | `/sessions` id set vs the work-order rows of `workorders/v2`, per seat | PASS for every seat: identical sets. v2 adds only `__row_type:"planned"` rows |
| Tenancy | Each customer seat's work orders checked against its own `accessible_sld_ids` and the Acme staff list; `POST /company/<other company fdcadc86…>/workorders/v2` and `GET …/sessions`; random company id | PASS: no work order outside the seat's own Acme sites. Another company's id gives **422 "You do not have access to this company's data"** for every customer seat (`is_eg_admin:false`). The staff seat also gets 422 there, so there is no positive control for that path (refusal is uniform) |
| iOS-equivalent `/sld/v3/{sld}` (cheap probe, 3 sites) | `ir_sessions` length per seat | PM = Super Admin (37 / 1 / 2). **AM-only gets 0 on all three**, while its web/API list has 2 work orders on site c4098151 (see Observations). `/sld/v3` returns 7–21 MB per site |
| Flags | Backend change, no gate | n/a |
| Performance | Per-call timing | `workorders/v2` is mostly under 1.3 s per page, but there were spikes: **19.3 s** (Super Admin, one page) and 8.7 s (EE, one page). `GET /company/{id}/sessions` is unpaginated: 1.87 MB / 1378 rows / 2.3 s for staff, 1.45 MB for PM. The web page makes a single `workorders/v2` call per load (no duplicates) |
| Compared with 12 Oct (`index-D1IWZ6SI.js`) | Same sweep | Same shape, a few new work orders: Super Admin 1552→1557, PM 1192→1195, AM 1184→1187, EE 142→143, FM 6, Technician 116 |

## Bugs found
None that this ticket's change can cause (the single-role branches are unchanged, and the multi-role branch cannot be reached with our seats).

## Observations (for the dev to confirm; not filed)
1. **AM-only: web and iOS disagree.** The web list for the Account Manager seat shows 1072 work orders (2 on site c4098151). The same seat's `GET /sld/v3/c4098151…` (the iOS path) returns 0 `ir_sessions`, while PM and Super Admin get 2. AM-only is outside FULL_SESSION_ACCESS_ROLES, so iOS probably applies "can_see_all_sessions = false". That may be by design, but it means an AM-only user sees work orders on the web that the app does not show. Ask Avani which one is intended.
2. **One work order is hidden from its own account's AM.** "Work Order - Apr 2, 8:32 PM" (`9169f1f1…`) is stamped with the account "Abhiyant Singh". The AM seat is `mapped` on that account, and the site (`61e3bd88…`) is in the AM's site list. The work order is still missing from the AM list (and from the FM, who is an account user of that account), while the other 3 work orders of that account on the AM's sites are shown. I did not confirm the cause. A guess is that the scope reads the site's current account rather than the work order's stamped account (the ZP-3978 retention model). Low priority, and unrelated to the ordering change.

## Enhancement suggestions
1. Paginate `GET /company/{id}/sessions`: it returns every work order in one 1.9 MB response (known list-API finding, still true).
2. For Account Manager users, say on the work-order list why it is scoped ("Showing work orders for your 9 accounts"), so an account-scoped list is not mistaken for missing data, which is exactly how this bug surfaced.
3. Add a non-staff AM + PM seat to the QA seat set (and to the RBAC suite) so the union-of-roles class of bug is testable on QA. Single-role seats cannot see it (memory `feedback_multi_role_blind_spot`).

## Mobile part for Nency
- With a PM + AM (or AM + EE) user that has no account links, the iOS/Android work-order list (`/sld/v3` → `can_see_all_sessions`) should show all work orders on the user's sites. The ticket says iOS already did.
- AM-only user: compare the app's work orders on a site with the web list (Observation 1: on QA the AM seat's `/sld/v3` returns 0 work orders on site c4098151 while the web shows 2).

## Not covered
- **The core multi-role case**: no seat (see above). This is the one test that proves the fix.
- The Hi-Tech "rparsons @ Arvco Cadillac" regression: that is production data. QA only, by rule.
- Whether #1420 is deployed on the QA backend: there is no version endpoint, and single-role behaviour is identical with or without the fix.

## Proposed Jira comment
```
h3. QA result: NOT WEB-TESTABLE on QA for the core case; single-role regression PASS (QA, build index-COeCBn0z.js, 13 Oct)

*Why:* none of our QA seats is a customer user with Account Manager + Project Manager (or + Electrical Engineer). The only seat holding AM + PM + EE is an Egalvanic staff Super Admin, which returns early via ADMIN_ROLES, so the new ordering is never reached.
*Seat needed:* a non-staff Acme QA user (is_eg_admin false, no Admin/Super Admin) with roles *Account Manager + Project Manager*, *no account links* (not mapped on any account, not an owner), on a few sites. Expected: workorders/v2 and /sessions return every work order on its sites (same as a PM-only user), not an empty list.

||Seat (single role)||API work orders||Expected scope||Web Work Orders page||
|Project Manager|1080 = all 1080 on its 116 sites|full access|96 open = API 96|
|Electrical Engineer|143 = all 143 on its 4 sites|full access|Access Denied (no features.site_visits.view)|
|Account Manager|1072, all on its 9 mapped accounts|account scope|Access Denied (no features.site_visits.view)|
|Technician|1 of 1080 on its sites|assigned only|Web Access Restricted|
|Facility Manager|6 (its account's sites)|unchanged from 12 Oct|1 open = API 1|

# Every seat: /sessions and workorders/v2 return the same work orders (v2 only adds planned rows).
# Tenancy: no work order outside the seat's own sites; another company's id gives 422 for every customer seat.

!QA-13Oct-ZP-4559-1-pm-only-work-orders-96-open-equals-api-PASS.png|width=720!
!QA-13Oct-ZP-4559-2-am-only-web-work-orders-access-denied-expected-PASS.png|width=720!

Notes for the dev: (1) AM-only gets 0 work orders from /sld/v3 (iOS path) on a site where the web list shows 2. Is that intended? (2) One work order stamped with an account the AM is mapped to (9169f1f1, site 61e3bd88) is hidden from that AM. Not caused by this change.
```

## Recommended status
**Stay In QA**: blocked on a non-staff AM + PM test seat (and on confirming that #1420 is cherry-picked to the QA backend). Nothing failed.
