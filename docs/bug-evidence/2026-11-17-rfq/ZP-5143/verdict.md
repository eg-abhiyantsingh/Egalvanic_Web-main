# ZP-5143 [iOS] Task #161: hide site & work-order creation for guest Technicians and sub logins: verdict

**Verdict: PASS for the guest Technician (TC1) and the normal user (TC3).**
- TC2 (sub login with no own company) is **not covered**: no such seat exists on QA.
- Status: stays **In QA** until TC2 is run, or until the lead decides TC1 + TC3 are enough for RTR.
- No new defects.

- **Build:** Z Platform-QA 1.75 simulator build (binary 16 Nov 06:49 IST, from the owner). Lane A: iPhone 17, iOS 26.2. API https://eg-pz.qa.egalvanic.ai/api.
- **Web build at the time:** index-CkkAeA9o.js. QA web changed twice today: oFAZQ3rK → CFMAmUdK → CkkAeA9o.
- **Seats:** SA (normal user, EG-ACME). TECH (Technician; home EG-ACME; guest Technician in Demo Company, company code `demo`).
- **Date:** 17 Nov 2026, 14:00–14:55 IST.

## Server side first (/api/auth/v2/me, the same call the app makes)
| Seat / acting in | field_limits | is_guest | roles | accessible sites |
|---|---|---|---|---|
| TECH in Demo (X-Subdomain demo) | can_create_sites **false**, can_create_work_orders **false** | true (home EG-ACME) | Technician (72 perms) | 1 |
| TECH at home EG-ACME | true / true | false | Technician (73 perms: Demo minus platform.web) | 113 |
| SA in EG-ACME | true / true | false (is_eg_admin true) | Super Admin + 5 | n/a |

## What was run on iOS, and what happened
| # | Step | Result |
|---|---|---|
| TC3.1 | SA › Site home › Sites | **Create New Site** shows. Tapping it opens the Create New Site form (cancelled, nothing saved). PASS |
| TC3.2 | SA › Work Orders | **Start New Work Order** shows. It works: I used it to create the ZP-5104 test WO ("Work Order - Nov 17, 2:26 PM"). PASS |
| home | TECH signs in at EG-ACME (field_limits true) | Select Site still shows **Create New Site**. PASS. The ticket's "Technician at home keeps what field_limits says" holds. |
| TC1.1 | TECH › Sites › company header › Switch Company › "Demo Company, Guest · Technician" › Continue | Switched in about 16 s, with no 2FA prompt. The Demo site list shows only site "Demo" and **no Create New Site**. PASS |
| TC1.2 | Open site "Demo" › Work Orders | The list is empty ("No Work Orders Yet"), with **no Start New Work Order and no "Start your first work order" prompt / Create First Work Order button**. PASS |
| control | Same seat back in EG-ACME, empty Work Orders list on (s) Wild Goose Brewery | It shows "Start your first work order" + **Create First Work Order**. This proves the Demo screen hides it because of field_limits, and not because the list is empty. |
| back | TECH switches Demo → EG-ACME | Create New Site is back in the site list. PASS |

## Not covered
- **TC2, sub login (subcontractor with no company of its own):** no such login exists on QA.
  - The PM seat is a subcontractor-link guest, but it has its own company, so it is not the same case.
  - Ask: a sub-login seat, or the steps to create one.
- **TC1.2, "assigned work orders still open and work":** the TECH has no Demo WO, so this was covered with the **PM seat**. The PM is a subcontractor-link Technician guest on Demo, with field_limits false / false as well.
  - The PM's Demo list shows exactly its 2 assigned WOs (QA-DEMO ZP-4830 17 Nov WO 10 and WO 11), still with no Start New Work Order.
  - WO 11 opens and works: Details, Assets (Building A › Electrical Room 104). PASS.
- **Backend that omits field_limits (old qa/prod behaviour):** not testable. QA now always sends field_limits.

## New defect found on the way (separate ticket)
- A guest cannot log out while working in the host company. Settings › Logout gives "Logout failed (status 403)" (backend: POST /auth/v2/logout → 403 guest_route_not_allowed), and the user stays signed in. Draft: ../BUG-ios-guest-logout-403/.

## Guest menus (owner add-on)
Full table: ../GUEST-MENUS/ios.md. In short:
- In Demo, the guest Technician's menus match the same seat at home, except that Create New Site and the empty-list Create First Work Order are hidden.
- The app shows no Quotes anywhere: the Quotes card is commented out in SiteTabView.swift.
- No admin or company-settings items show.

## Cross-platform
- **Web:** the TECH seat cannot act in Demo on web.
  - https://demo.qa.egalvanic.ai/login refuses it with 403 COMPANY_MISMATCH: "This account does not belong to this organization."
  - On acme I found no company switcher in the Technician's web UI.
  - Web guest access is ZP-4830, which the web guest tester covers with other seats.
  - So web is "not checked with this seat (no web entry for a guest Technician)".
- **Android:** ZP-5144 (the twin), tested on 1.40 by the Android tester with the same seat. The lead holds its result. The Android tester's guest answers match iOS:
  - no 2FA on the switch;
  - Schedule shows;
  - Issues are site-wide.

## MOBILE-CHECKS (for Android, if not already done in ZP-5144)
1. TECH › Switch Company › Demo › site list: no Create New Site.
2. Demo › Work Orders, empty list: no Start New Work Order, no "Start your first work order" / Create First Work Order.
3. Control: the same seat at home in EG-ACME, empty list: the Create First Work Order button shows.
4. Switch back to EG-ACME: Create New Site shows again.

## Evidence (this folder)
- `QA-17Nov-ZP-5143-guest-tech-hidden-vs-home-and-SA.mp4` (54 s, step captions).
- `QA-17Nov-ZP-5143-1-SA-site-list-create-new-site-shows.png` (PASS, expected).
- `QA-17Nov-ZP-5143-2-SA-wo-list-start-new-work-order-shows.png` (PASS, expected).
- `QA-17Nov-ZP-5143-3-guest-tech-demo-site-list-no-create-new-site.png` (PASS, expected).
- `QA-17Nov-ZP-5143-4-guest-tech-demo-empty-wo-list-no-start-prompt.png` (PASS, expected).
- `QA-17Nov-ZP-5143-5-control-home-company-empty-list-shows-create-first.png` (control).

## Test data
- Nothing created for this ticket. The SA work order it was used to create belongs to ZP-5104.
