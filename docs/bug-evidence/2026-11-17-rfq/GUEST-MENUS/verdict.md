# Guest users: are the web menus correct? (QA, 17 Nov 2026, web)

**Answer for the owner:** the guest menus on web are correct in every guest state.
- A guest gets exactly the host company's role menus, minus Admin › Customers.
- Every Admin, Collaborators, Users, Teams, Subcontractors, Customers/Sites, Work Orders and Planning page refuses a guest typed URL.
- The Technician guest gets "Web Access Restricted", as expected: Technician has no web access in Demo.

Two real problems, neither of them a menu entry:
1. security finding ZP-5315 (details in Jira only; public repo rule).
2. Low: a guest whose role holds quotes permissions (Electrical Engineer) can open **/opportunities** by typed URL.
   - The Quotes page and the "Create Quote" dialog both open.
   - Demo's data stays hidden only because Demo has no Sales subscription (422), not because of a guest rule.

**Environment**
- QA web build `index-CkkAeA9o.js`. Host: Demo Company. Home: EG-ACME.
- LaunchDarkly `feature-guest-access`: Demo true. EG-ACME false until 15:09, true from 15:26 (it doesn't change what a guest sees in Demo).
- Every guest is a non-staff EG-ACME seat (is_eg_admin false). I didn't use TECH: it's reserved for the device testers, and the Android tester covered it in `android.md`.

## Guest states checked
| State | Seat | How they got access | Role in Demo | What web shows |
|---|---|---|---|---|
| A. Sub-link guest | PM | Member of a linked EG-ACME Team, ticked onto open Demo WOs (ZP-4830 Link A) | Technician (72 permissions, no `platform.web`) | **Web Access Restricted**, on every route (/sites, /sessions, /assets, /slds, /admin/setup, /collaborators, /customers, /users) |
| B. Old Guests invite | EE | Existing guest membership (Collaborators › People invite), site "Demo" | Electrical Engineer (84) | Full app: rail Site Data + Engineering + Help |
| C. Customer-link guest | AM | EG-ACME picked AM on the customer link; Demo shared SLD "test" | Electrical Engineer (84) | Same rail as B; site picker = "test" only |
| D. Old-invite Technician | TECH | (existing) | Technician | Not run on web (seat reserved). Same role as A, so the same restricted screen. Android: android.md |

Same users at home in EG-ACME, for comparison:
- PM: Project Manager.
- EE: Electrical Engineer. Rail = Site Data + Engineering + **Admin (Customers)**.
- AM: Account Manager.

## Menu table (web)
Legend: ✓ shown, — not shown.

| Menu item | Permission behind it | A (Tech guest) | B (EE guest) | C (cust-link guest) | Same user at home | Expected for a guest | Verdict |
|---|---|---|---|---|---|---|---|
| Company switcher (logo) | guest membership | — (restricted screen; its only button "Logout" returns to EG-ACME, still signed in) | ✓ EG-ACME + "Demo Company · Guest · Electrical Engineer" + Home | ✓ | ✓ | Way back home | PASS. A: works, but labelled "Logout" (minor UX) |
| Site Data › Site Overview | features.site_overview.view | n/a | ✓ | ✓ | ✓ | role decides | PASS |
| Condition Assessment | features.condition_assessment.view | n/a | ✓ | ✓ | ✓ | role | PASS |
| Assets (+ Create Asset, Upload Anything, Bulk Edit, SKM, Bulk Ops) | features.assets.view, nodes.manage | n/a | ✓ | ✓ | ✓ | role (EE has nodes.manage) | PASS |
| Panel Schedules / Torque Rules / Connections / Locations / Issues / Tasks | features.*.view | n/a | ✓ | ✓ | ✓ | role | PASS |
| Maintenance Program / Compliance / Attachments | features.assets.view, features.attachments.view | n/a | ✓ | ✓ | ✓ | role | PASS |
| Engineering › Arc Flash Readiness, SLD, Short-Circuit Ratings, Feeder Schedule, OCPD Settings, Transformer Schedule | features.arc_flash.view, features.slds.view, features.equipment_designations.view | n/a | ✓ | ✓ | ✓ | role | PASS |
| Operations (Work Orders, Planning) | workorders/site-visit perms | n/a | — (typed /sessions, /planning → Access Denied) | — | — | role | PASS |
| **Start / Create WO** | field_limits.can_create_work_orders | false | true, but no entry point (/sessions denied) | true, no entry point | true | false for a Technician guest | PASS (A). B/C true: spec covers Technician only |
| **Create Site** | field_limits.can_create_sites | false | true, but Sites lives under Customers (denied) | true, no entry point | true (Admin › Customers › Add Site) | false for a Technician guest | PASS |
| **Admin › Customers** | features.accounts.view (EE has it) | — | **—** | **—** | **✓** | Never for a guest (Task #77) | PASS. Typed /customers and /sites → Access Denied |
| Admin › Users / Teams / Subcontractors / **Collaborators** | users.manage (EE holds it!), company_data.manage | — | — (typed → Access Denied) | — | — | Never | PASS. APIs /vendors, /crews, /subcontracting, /guest-access/guests → 403 `guest_route_not_allowed` |
| Admin › Setup / Subscription / company settings | admin | — | — (typed → shows the guest's landing page) | — | — | Never | PASS |
| **Sales / Quotes** | features.salesdb.view; quotes.* / opportunities.* (EE has them) | — | — in the menu; **typed /opportunities opens the Quotes page + "New Quote" → Create Quote dialog**; `POST /company/{demo}/quotes/v2` → 422 "not included in your organization's subscription" | same as B | Same typed URL at home shows real quotes (EE) | Guests mustn't have Sales/Quotes | **FINDING (Low)**: the page and dialog are reachable; data is blocked by subscription, not by a guest rule |
| Builder | reports.manage / company_data.manage | — | — | — | — | role | PASS |
| Help, avatar | — | — | ✓ | ✓ | ✓ | ✓ | PASS |
| Sites in the site picker | accessible_sld_ids | the WO's site only ("Demo") | "Demo" (invited site) | **"test" only** (the shared SLD) | all EG-ACME sites | Only assigned / shared | PASS. Other sites' SLD calls → 422; `/sld/{id}` → 403 for guests |
| Work orders (list) | assignment | only the 2 ticked WOs (API) | (no WO menu) | 1 closed WO on "test" (API) | — | Only assigned | PASS |
| (one guest-scope row moved to ZP-5315, Jira only) | | | | | | | FAIL → ZP-5315 |
| 2FA enrolment prompt on switching into Demo | Demo's MFA setting | — | shown ("Set up later" works) | shown | — | — | Note: every switch into Demo asks guests to enrol 2FA |
| "Let's get your assets in" (Upload Anything onboarding) on the empty shared site | onboarding | — | — | shown on "test" (0 assets) | — | Questionable for a guest | Note |

API checks behind the table (guest in Demo):
- `/api/features/access` → 200.
  - A: granted issue-suggestions, assets, attachments, issues, locations, arc-flash, condition-assessment, connections, slds, planned-work, work-orders, accounts.
  - B and C: panel-schedules, assets, attachments, issues, locations, site-overview, tasks, arc-flash, condition-assessment, connections, equipment-designations, slds, customers.
  - "customers" / "accounts" are granted at the feature level, but the frontend `NotForGuests` hides Customers and the account APIs refuse guests.
- `/api/auth/v2/me` → `is_guest: true`, `guest.home_company_name: "EG-ACME"`.
  - A: Technician, `field_limits` false/false.
  - B/C: Electrical Engineer, true/true.
- `/api/company/{demo}/sessions`: A = only assigned WOs. `/api/company/guided-tours` → 403 for every guest (console noise only).

Evidence (this folder):
- Video: `QA-17Nov-GUEST-MENUS-web-technician-guest-restricted-logout.mp4` (60 s): A switches into Demo, sees the restricted screen with Logout only, and Logout returns to EG-ACME.
- Screenshots:
  - `QA-17Nov-GUEST-1-sublink-guest-in-demo-web-access-restricted.png` (A)
  - `-4-technician-guest-restricted-logout-only-PASS.png` (A, Logout boxed)
  - `-2-ee-guest-in-demo-landing.png` (B)
  - `-3-ee-guest-switcher-back-home-PASS.png` (B)
  - `-8-ee-guest-in-demo-no-admin-rail-PASS.png` (B rail)
  - `-7-ee-at-home-admin-customers-control.png` (B at home, control)
  - `-5-customer-link-guest-in-demo.png` and `-9-customer-link-guest-menus-PASS.png` (C)
  - `-6-ee-guest-typed-opportunities-quotes-page.png` (finding)
- Raw: `raw/r08-ee.json`, `r10-restricted.json`, `r15-ee-lists.json`; ZP-4830/raw/r12, r13, r17.
- Android: `android.md` + android-*.png + `QA-17Nov-GUEST-MENUS-android-demo-guest-technician.mp4`, written by the Android tester.

## MOBILE-CHECKS (for the iOS and Android device testers)
Live state left on QA:
- PM (abhiyant.singh+project@, company code `acme`) is a **sub-link Technician guest** in Demo, on "QA-DEMO ZP-4830 17 Nov WO 10" and "WO 11".
- AM (abhiyant.singh+accountm@) is a **customer-link guest** (Electrical Engineer) on site "test" only.
- EE (abhiyant.singh+electric@) is an **old-invite EE guest** on site "Demo".

1. **Sub-link guest on the device (PM).**
   - Sign in at `acme` › Sites header › Switch Company: "Demo Company · Guest · Technician" is listed.
   - Switch. Expected:
     - site list = "Demo" only;
     - Work Orders = exactly WO 10 and WO 11;
     - no Create Site, no Start New Work Order (field_limits false);
     - no Quotes, no admin screens.
   - iOS = ZP-5143 rows; Android = ZP-5144 rows.
2. **Access follows assignment, on the device (PM).**
   - Ask the lead to untick PM on WO 11 and remove PM from WO 10 (Demo admin).
   - After a pull-to-refresh or relaunch: Demo leaves Switch Company. If the app was inside Demo, it should fall back to EG-ACME cleanly, without an error loop.
   - Then tick PM on a **new** WO, not the same one (BUG-retick-500). Demo comes back.
3. **ZP-5315 check (PM in Demo):** steps are in the Jira ticket (security, kept out of the public repo).
4. **Customer-link guest (AM).**
   - Switch Company › "Demo Company · Guest · Electrical Engineer". Expected: site list = "test" only; "Demo" and "Riverside Plant" absent; the SLD for "test" opens (empty, 0 nodes, same as for the Demo admin).
   - Create Site / Start WO: `field_limits` is true for an EE guest, so they may show. Report what each app does.
5. **Old-invite EE guest (EE).** Switch Company › Demo › site list = "Demo" only. Same expectations as 4.
6. **2FA prompt.** On web, every switch into Demo shows "Set up two-factor authentication" (Set up later works). Does the app ask on switch?
7. **Company-link requests on mobile.** Collaborators / Requests (subcontractor and customer links) exist only on web Admin.
   - Confirm neither app shows "Wants you as a subcontractor/customer" anywhere, or note where it shows.
   - Guest invites (Task #77) do show in the site picker (ZP-4588/4589).
8. **Android tester's open questions** (android.md): Schedule quick action for Technician guests; site-wide Issues for a guest.
   - Compare with iOS.
   - On web, the Technician guest can't reach any page, so there's no web parity point.

## Not covered
- Old-invite Technician guest on web (TECH reserved; same role as A).
- The sub admin's own Collaborators page as a guest (admins only).
- Pages inside an assigned WO for a web guest (Technician has no web).
