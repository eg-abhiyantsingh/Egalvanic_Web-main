# ZP-4831 · Safety module (Construction Manager role, install statuses, scenarios, lockout, ACTUAL layer) + Users roles UI (QA, 12 Nov 2026)

**Verdict: PARTLY** on QA, Web v2.6 build `index-C1ZRIunF.js` (checked 16:01, 16:42 and 16:59 IST, unchanged).

- **Users roles UI:** passes completely.
- **Construction Manager role:** exists on QA and can be assigned.
- **Safety role gating:** holds for customer seats on menu, 3 of 4 typed routes, and the API.

Gaps:
- **Typed `/safety/sld`** renders the SLD canvas with the Scenario control for seats without the role (minor UI leak; the API refuses).
- **Staff Admin / Super Admin** get no Safety menu, but every Safety page and the write API work for them (Admin bypass). The behaviour does not match the menu.

Blocked:
- A **signed-in Construction Manager seat**. I created a QA-DEMO CM user, but it has no deliverable email, so it cannot complete a first sign-in.

Seats used:
- SA (staff, is_admin, 6 roles, no CM).
- Staff Admin.
- PM, AM, EE, FM, CP and TECH (customer seats, is_eg_admin false, no CM).

## Checked
| Check | How | Result |
|---|---|---|
| Users: Roles column = first role + "+N" chip + tooltip | Admin › Platform Users | PASS: "Super Admin +2" with tooltip "Super Admin, Electrical Engineer, Admin" (screenshot 1) |
| Users: "no labor types" warning column removed | header row | PASS: columns are Name, Email, Job Title, Crew, 2FA, Roles, Status, Actions. No unnamed icon column (it was there on 5 Nov) |
| Create User: no Default role picker | Create User dialog | PASS: Basic Info, Roles, Site Access, Copy me, Send a temporary password. No Default / Active role field |
| Construction Manager + Portal Sales in the Roles picker | Roles list | PASS: Technician, Admin, Portal Sales, Account Manager, Electrical Engineer, Project Manager, Construction Manager (screenshot 2). `GET /api/users/roles/company/{acme}` lists Construction Manager (required_module null) |
| Assign Construction Manager | Created "QA-DEMO ZP-4831 CM 12 Nov" with only CM, site Android Site 2 | PASS: `POST /api/users/` 201. The list shows "Construction Manager" |
| Edit user: no Default role picker | Edit on the QA-DEMO CM user (Cancel) | PASS: Roles = Construction Manager; no Default role (screenshot 3) |
| Seat without CM sees no Safety (menu) | full rail walk for PM, AM, EE, FM, CP, Admin, SA | PASS: no Safety section for any of them |
| Typed `/safety/install-statuses`, `/safety/loto-registry`, `/safety/scenarios` (no CM) | PM, AM, EE | PASS: Access Denied (screenshot 5). FM / CP are redirected to the portal |
| **Typed `/safety/sld` (no CM)** | PM, AM, EE; re-proved with PM in the video run | **FAIL (minor)**: the SLD canvas renders with "Scenario / Export / Layout" instead of Access Denied. The Access Denied outlet is behind the canvas, the same pattern as `/maintenance-portal/sld`. Its safety calls are all refused: `/safety/sld/{id}/scenarios`, `/actual` and `/install-statuses/vocabulary` return 422 (screenshot 6) |
| Safety API refuses seats without CM | PM, AM, EE, FM, CP: `/safety/install-statuses/vocabulary`, `/safety/sld/{own}/install-statuses`, `/scenarios` | PASS: 422 `permission_denied` "Required: features.safety.*" |
| **Admin / Super Admin (no CM)** | SA and Admin: menu vs typed URL vs API | **Mismatch**: no Safety menu, but all 4 pages open by typed URL with data, the API answers 200, and writes work (screenshot 4). Admin bypass in the route guard and backend; the menu checks the plain permission list |
| Install statuses: set one row + history | SA (Admin bypass) on Test site A, QA asset "QA-DEMO ZP-4580 3-winding": Set status (1) › Tested, reason, then back to In Service | PASS: `POST /safety/sld/{id}/install-status` 200 `{nodes:1}`. The row shows "Tested · date · abhiyant admin". History API has both events with reasons. Restored to In Service |
| Unknown status enum | `status:"not_a_status"` | PASS: 400 with the allowed list |
| Status picker vocabulary | Set install status dialog | PASS: grouped Planning / Installed / Service, each marked "not energizable" or "may carry power" |
| Scenarios | SA: New scenario "QA-DEMO ZP-4831 scenario 12 Nov" on Test site A | PASS: 201, listed with Open on SLD / Edit / Delete |
| LOTO Registry | SA: Android Site 2 and Test site A | PASS: renders ("0 events on record · 0 locks hanging right now") |
| Power flow / ACTUAL | API (SA) | PASS: `/power-flow` 200 (Android Site 2: 214 KB, 1.0 s); `/power-flow?actual=1` 200; `/actual` 200; `/switching-events` 200 |
| Lockout options (compute) | `POST /safety/sld/{id}/lockout-options {node_ids:[QA asset]}` (the body the UI sends) | PASS: 200 "We don't have enough topology to reason about how these assets are fed." The QA asset has no feeder |
| Lockout options, malformed body | bare array `[id]` instead of `{node_ids:[…]}`; ran twice | **FAIL (Low)**: 500 `internal_error`, trace ids `bd68b8f4822e4789b8a73103f6a78e56` and `97a4962e3f95406c8d2e163a987bb4d5`. Should be 400 |
| Tenant scope | Safety install-statuses with another company's SLD `f86052b7…` | PASS: 422 "You do not have permission to access this SLD" even for staff; own SLD 200 (positive control); random id 422 |
| Flags | Safety has no LaunchDarkly gate in the bundle (permission `features.safety.*` only) | Nothing to flip. Whoever holds CM gets Safety in every company |
| Performance | `/safety/sld/{id}/install-statuses` | Android Site 2: 325 KB, 0.47 s. Install Statuses page: 620 assets + 242 connections in one call; no call over 2 s |

## Bugs found
1. **Typed `/safety/sld` opens for users without Construction Manager.** Minor, frontend: suggest Krunal, or Eric in this ticket.
   - Steps:
     1. Sign in as Project Manager.
     2. Type `/safety/sld`.
   - Actual: the SLD canvas with the Scenario lens control renders (API calls 422).
   - Expected: Access Denied, like the other three Safety pages.
   - Evidence: screenshot 6 and the video (part 2). Seen for PM, AM and EE.
2. **Admin / Super Admin: no Safety menu, but every Safety page and its write API work by typed URL.** Low / product question for Eric: should Admins see Safety in the menu, or be refused? Screenshot 4.
3. **`POST /safety/sld/{id}/lockout-options` with a malformed body returns 500** instead of 400. Low, backend: suggest Avani. Re-proved; trace ids above.

JQL run (none found):
`project = ZP AND (summary ~ "Collaborators" OR summary ~ "company link" OR summary ~ "Withdraw" OR summary ~ "Safety" OR summary ~ "Construction Manager" OR summary ~ "BCES" OR summary ~ "guest access") ORDER BY created DESC`

## Enhancement suggestions
- Users list: add a Roles filter (for example "Construction Manager") so admins can find who holds Safety access.
- Install Statuses: a search box and a status filter. Android Site 2 lists 620 assets on one page set.
- The guest-portal Create / Edit Account User dialogs still ask for an "Active Role" / default role. Remove it there too for consistency with Platform Users.

## Mobile impact
Install statuses and switching events live on nodes and edges. The iOS and Android testers should check that asset sync still works after a status change. The QA asset "QA-DEMO ZP-4580 3-winding" on Test site A was changed and restored today. No mobile UI is in this ticket.

## Not covered (and why)
- **The Safety menu for a Construction Manager, and the multi-role union** (for example PM + CM). The CM user I created ("QA-DEMO ZP-4831 CM 12 Nov") uses a non-deliverable test address, so its first sign-in cannot be completed. Needed: a CM test seat with a deliverable email (for example a new `+cm@` QA seat), or a developer adding Construction Manager to a dedicated QA test seat.
- **Bulk Edit with combined install + engineering status.** Not reached; it needs a multi-select bulk edit on shared assets.
- **Safety SLD lens switching, power-flow overlay drawing, the Lockout options dialog and the impact diff in the canvas.** Only the API side was checked; driving a GoJS context menu reliably needs a CM seat run.
- **The LOTO Registry with real switching events.** There are none on QA, and creating one writes switching or lock records.

## Test data created
- User "QA-DEMO ZP-4831 CM 12 Nov" (Construction Manager, site Android Site 2, non-deliverable email).
- Scenario "QA-DEMO ZP-4831 scenario 12 Nov" on Test site A.
- Two install-status history rows on "QA-DEMO ZP-4580 3-winding". The status was restored to In Service.

## Proposed Jira comment
See `ZP-4831.txt`.

## Recommended status
**Stay In QA.** Needed:
- A Construction Manager seat that can sign in.
- A decision on Admin access (bug 2).

Fix bug 1 (minor) together with that.
