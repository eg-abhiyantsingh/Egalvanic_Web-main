# ZP-4831 · Safety module (Construction Manager role, install statuses, scenarios, lockout, ACTUAL layer) + Users role UI (QA, 17 Nov 2026)

**Verdict: PASS (main purpose) → recommend READY TO RELEASE, with 2 new bugs filed separately and 1 open product question.**
The 12 Nov blocker is gone. A real Construction Manager login sees exactly the Safety module, and every function in the QA Review worked for it on its own QA-DEMO sites.

The ticket's own small gap from 12 Nov is still there: typed `/safety/sld` renders the canvas for users without the role. The owner rule is that "main part works → gaps become new bugs". So it is filed as `BUG-safety-sld-typed-url-renders-canvas.txt`, together with `BUG-lockout-options-malformed-body-500.txt`.

Builds:
- The full run was on `index-LGhrn-48.js` (live from about 14:05 IST). The first CM sign-in was at 14:01 IST on `index-CFMAmUdK.js`: menu, pages and API, with the same result.
- Smoke on `index-CkkAeA9o.js` (live from about 14:45 IST) at 14:49 IST:
  - PM: no Safety menu; typed install-statuses gives Access Denied; typed `/safety/sld` still renders; the vocabulary API returns 422.
  - SA: still no Safety menu.

## The Construction Manager seat: what I changed and how I restored it
Seat: the spare **Portal Sales** test seat (`PORTAL_SALES_EMAIL` in AppConstants). It is not TECH, SA or ADMIN; it is a customer seat (`is_eg_admin` false). Every change was made in Admin › Platform Users › Edit User with the Super Admin seat.

| When (IST) | Roles | Sites | Accounts | Other |
|---|---|---|---|---|
| **Before** (13:59, screenshot 1) | Portal Sales (active role Portal Sales) | none (0 / 320) | none | Labor types none, Active, MFA not required. Sign-in result: "Web Access Restricted" (1 permission: `maintenance_portal.manage`) |
| 14:00 | **Construction Manager only** (`role_ids` = [91ef4a0d…]) | + "QA-DEMO ZP-4561 site 16 Oct" | assigning that site auto-linked "QA-DEMO ZP-4561 customer 16 Oct" | `PUT /users/712b05f0…` 200, `POST …/sld-assignments` 201 (`accounts_linked: 1`) |
| 14:08 | Construction Manager only | + "QA-DEMO ZP-5211 13 Nov power scheme bypass" (has a utility and back-feeds, so lockout options are ranked) | (same) | |
| 14:38 | Construction Manager + Portal Sales (union check) | (same) | (same) | |
| **After / restored** (14:40, verified 14:41, screenshot 13) | **Portal Sales** (active role Portal Sales) | **none** (0 / 321) | **none** | `DELETE …/sld-assignments/3578784e…` and `…/c037d47c…` 200; `DELETE …/internal-account-assignments/3cb8a94f…` 200; labor types []. Sign-in again: "Web Access Restricted"; `/auth/v2/me` roles [Portal Sales], 1 permission, 0 SLDs, `is_eg_admin` false. **Identical to before** |

## Checked (Construction Manager seat unless noted)
| QA Review row | How | Result |
|---|---|---|
| CM sees the Safety nav section | Sign in with Construction Manager only | **PASS**. It lands on `/safety/install-statuses`. The rail shows only **Safety**: DATA › Install Statuses, LOTO Registry; DIAGRAM › SLD, Scenarios (screenshot 2, video). `/auth/v2/me`: 19 permissions, the whole `features.safety.*` / `safety.*` set plus node, edge and SLD view/manage and `platform.web` |
| CM sees nothing else | Typed `/sessions`, `/users`, `/maintenance-portal` | **PASS**: Access Denied. (Observation: typed `/assets` and `/sld` open, because CM holds `nodes.view/manage` and `sld_diagrams.view`, which the Safety SLD needs. `/assets` has no route guard for any role; this is not new and not in this ticket) |
| Users without CM see none of it | PM (customer seat): menu and typed URLs; SA: menu | Menu **PASS** (no Safety for PM or SA). Typed `/safety/install-statuses` and `/safety/scenarios`: Access Denied **PASS**. **Typed `/safety/sld`: the SLD canvas with the Scenario control still renders** behind which the Access Denied outlet sits; the safety calls return 422. **FAIL (minor)**, as on 12 Nov; filed as a bug (screenshot 11, video part 2) |
| Safety API scope | CM: own site vs an unassigned site (Test site A) vs a random id | **PASS**. Own site: install-statuses (0.8 s), scenarios, actual and power-flow return 200. Unassigned and random ids: 422 `permission_denied` "You do not have permission to access this SLD" |
| Install status on a node (individually) + history | Safety SLD › right-click CB-F1 › Install status › Planned, then back to In Service | **PASS**. The node shows `planned` and history records In Service → Planned by the CM user. Restored to In Service |
| Install status on a connection | Install Statuses › Connections › ATS-1 → PNL-EMERG › Set status (1) › Wire Pulled + reason › Apply, then back to In Service | **PASS**. The row shows "Wire Pulled · time · Abhiyant Singh", then In Service again |
| Bulk Edit: combined install + engineering status | Safety SLD › select CB-GEN + CB-TIE › Edit › Bulk Edit (2) › Change Install Status = Tested + Change Engineering Status = Estimated › Apply to 2 | **PASS**. `POST …/install-status` 200 (both are Tested). The engineering part is reported and not written: "2 asset(s) have no library designation — Estimated needs one", as the dialog explains (screenshot 8). Restored with Install Statuses › Set status (2) › In Service + reason |
| History recorded | Install Statuses › CB-GEN › history icon | **PASS**. Drawer: In Service → Tested (bulk edit) and Tested → In Service with its reason (screenshot 9). Small: Bulk Edit and the right-click menu record no "why", although the dialog says "Every change is recorded with who, when and why" |
| Scenarios | Scenarios › New scenario "QA-DEMO ZP-4831 CM 17 Nov" | **PASS**: 201, listed with Open on SLD / Edit / Delete |
| Scenario and Actual lenses | Safety SLD › Scenario dropdown | **PASS**. STATE None / Actual; SCENARIOS lists mine; "New scenario". The Power flow toggle appears only once a lens is on (absent at "None") |
| Power flow gated on install status | Scenario lens + Power flow; CB-F1 → Planned | **PASS**. With CB-F1 Planned, `/power-flow` marks CB-F1 not energizable and **PNL-NORMAL goes de-energized**. Back In Service: PNL-NORMAL is energized again (screenshots 4, 7) |
| Lockout options (ranked, implied breakers) | Right-click CB-BYP › Lockout options… | **PASS**, 0.34 s. Options 1–3 ranked by what else goes dark (8 / 10 / 18 assets). Isolate ATS-1 "Open, lock, tag". Secure back-feeds: ATS-1 → Off, UPS-1 → Bypass. Collateral 8 (screenshot 6) |
| Impact diff | Under the scenario: "Impact if closed…" on CB-BYP (CM) and "Impact if open…" on CB-F1 (SA) | **PASS**. CM: "Nothing changes" (correct, it is already closed). SA: CB-F1 → Open, **goes dark 1 asset · 1 connection (PNL-NORMAL)** (screenshot 5) |
| LOTO Registry against the ACTUAL layer | Actual lens › CB-BYP › Apply lock / tag (tag ref, hole 1, the required lock photo, remarks) › Apply Lock; then View locks › Remove this lock › Remove Lock; then Document Switching Operation Open → Closed | **PASS**. `POST …/switching-events` 201 ×3. Applying the lock first recorded "Closed → Open, placed in safe position". The registry shows 4 events and "0 locks hanging". `/actual`: no locks, CB-BYP closed (screenshot 10) |
| Multi-role union | Construction Manager + Portal Sales on the same seat | **PASS**. 20 permissions. The rail shows Safety and Maintenance Portal (screenshot 12). The portal overview says "Failed to fetch issues", which is expected: neither role holds issue permissions, and Portal Sales is a marker role "to hold beside a working role" |
| Users role UI (passed 12 Nov) | Quick re-look | Edit User: no Default role. The Roles picker offers Technician, Admin, Portal Sales, Account Manager, Electrical Engineer, Project Manager, Construction Manager. The list row shows the held role |
| Admin / Super Admin question (12 Nov) | SA typed `/safety/install-statuses` | Same as 12 Nov: no Safety menu, but the page opens with data (Admin bypass). There is no reply in Jira yet, so this stays an open question, not a bug |
| Bad input | `POST /safety/sld/{id}/lockout-options` with a bare array `[id]` | **FAIL (low)**: 500 `internal_error`, trace `9eac5d606d4f4033af60a0ec014e68cb` (also on 12 Nov). A string `node_ids` returns 200 "not installed" (handled). Filed as a bug |
| Flags | — | No LaunchDarkly gate; permission only. Nothing to flip |

## New bugs (draft files, not filed in Jira)
1. `BUG-safety-sld-typed-url-renders-canvas.txt`: Minor, frontend.
2. `BUG-lockout-options-malformed-body-500.txt`: Low, backend (API only).

## Open question for Eric (no bug)
Admin / Super Admin without Construction Manager get no Safety menu, but every Safety page and its write API work by typed URL. Should Admins see Safety in the menu, or be refused?

## Not covered
- The install-status **mask** as a status-badge mode under a lens: the badge "more" menu was not reached in automation. Power-flow gating on install status was checked instead.
- "Save as scenario" from Lockout options, and "Apply to scenario" from the Impact dialog.
- A union with a working role such as PM + CM: I used CM + Portal Sales to keep to the spare seat's own role.

## Test data (QA-DEMO, kept)
- Scenario "QA-DEMO ZP-4831 CM 17 Nov" on "QA-DEMO ZP-5211 13 Nov power scheme bypass". Site: https://acme.qa.egalvanic.ai/safety/scenarios (pick that site).
- On that site: install-status history rows (all restored to In Service), and 4 LOTO events on CB-BYP (lock applied and removed; CB-BYP is back to Closed). The lock photo is a QA screenshot.
- Safety pages: https://acme.qa.egalvanic.ai/safety/install-statuses, https://acme.qa.egalvanic.ai/safety/loto-registry, https://acme.qa.egalvanic.ai/safety/sld

## Cross-platform
- **Twins:** none. A Jira search for "task #167", Safety, Construction Manager, install status and LOTO finds only ZP-4831 and the old backlog idea ZP-1948 "LOTO". No [iOS] or [Android] ticket exists.
- **Web:** PASS (above).
- **iOS:** **not on this platform.** The source (`eg-pz-mobile-iOS`, `release/qa` 34bd67e, 16 Nov, 1.75) has no `install_status`, LOTO, lockout or Construction Manager code. A CM-only user has no `platform.mobile` permission, so iOS `PlatformAccessGate` should show "Mobile Access Restricted".
  - Risk to check: install statuses live on nodes and edges, which iOS edits and syncs. iOS's node model has no `install_status`.
- **Android:** not checked. The source is not readable here; same risk as iOS.

### MOBILE-CHECKS
1. **iOS and Android, SA seat, site "QA-DEMO ZP-5211 13 Nov power scheme bypass".**
   - The web tester sets CB-TIE to **Tested** first (Safety › Install Statuses › tick CB-TIE › Set status).
   - On the device: open asset "QA-DEMO ZP-5211 CB-TIE", change one harmless field (for example a note or the room), save and sync.
   - Expected on web: CB-TIE is **still Tested**, and its history has no extra event from the device. The web tester then sets it back to In Service.
2. **iOS and Android, same site:** open the site's SLD / asset list.
   - Expected: it loads normally after today's Safety switching events (CB-BYP Closed → Open → Closed). No crash, no missing nodes.
3. **iOS and Android:** expected: no Safety, install-status or LOTO screens anywhere (not a mobile feature).
4. Optional, needs CM on a seat again (I restored the Portal Sales seat): sign in with a Construction-Manager-only seat on iOS and Android.
   - Expected: "Mobile Access Restricted" (CM has no `platform.mobile`), not an empty app.
