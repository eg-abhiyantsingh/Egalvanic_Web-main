# ZP-5144 [Android] Task #161: Hide site & work order creation for guest Technicians and sub logins

**Verdict: PASS** for the guest-Technician case (the case QA can run). Sub-login case: not covered (no subcontractor sub-login exists on QA).

- Build: Android Z Platform (QA) **1.40 (54)**. The APK contains field_limits / can_create_sites / can_create_work_orders handling (absent in 1.39 (53), see the 12 Nov comment).
- Seats: Technician seat (AppConstants TECH), home company EG-ACME and guest Technician in Demo Company (company code demo); Super Admin seat as the normal-user control. 17 Nov 2026, 14:50-15:05 IST.
- Fix: eg-pz-mobile-android PR #567.

## Server values (GET /api/auth/v2/me, same Technician seat)
| Context | is_guest | field_limits | permissions |
|---|---|---|---|
| EG-ACME (home) | false | can_create_sites true, can_create_work_orders true | 73 (incl. platform.web) |
| Demo Company (guest, after POST /api/guest-access/switch + handoff redeem) | true (home EG-ACME) | **can_create_sites false, can_create_work_orders false** | 72 (same as home minus platform.web) |
| Super Admin, EG-ACME | false | true / true | — |
Full lists: ../GUEST-MENUS/tech-seat-auth-v2-me-acme-vs-demo.json

## What was run
| # | QA-review row | Result |
|---|---------------|--------|
| 1 | Guest Technician: Create Site hidden | Demo Company (Guest · Technician) > Select Site: **no Create New Site**; only the assigned site "Demo". PASS |
| 2 | Guest Technician: Create Work Order hidden | Demo > Work Orders: "No Work Orders Yet" with **no Start New Work Order** and **no "Start your first work order"** prompt. PASS |
| 3 | Same seat at home (field_limits true) still sees both | EG-ACME > Select Site shows Create New Site. Work Orders shows Start New Work Order and the empty-state "Start your first work order". PASS |
| 4 | Normal full-permission user still sees both | Super Admin (EG-ACME): Select Site shows Create New Site and Work Orders shows Start New Work Order (seen during today's site switches and WO navigation). PASS |
| 5 | Backend without field_limits still shows both | Not testable now: QA sends field_limits for every seat since 12 Nov. On 5 Nov (QA without field_limits) both showed, but on 1.39, which had no gating code. |
| 6 | Subcontractor sub-login (no own company) | **Not covered**: no such seat. Related case PASS: the PM seat is a **subcontractor-link Technician guest** in Demo (it has its own home company). On Android in Demo it sees site Demo only, exactly its assigned WOs (QA-DEMO ZP-4830 17 Nov WO 10 and WO 11), and neither Create New Site nor Start New Work Order (17:25 IST; ../GUEST-MENUS/android.md, extra checks). |

The full guest-menu inventory (every menu, permission and verdict) is in ../GUEST-MENUS/android.md.

## Not covered
- Sub-login (row 6), backward compatibility on 1.40 (row 5).
- Switching workspace while offline.

## New defects
None.

## Cross-platform
- **Web:** the main web bundle has no field_limits handling. Web gates on permissions. The Technician at home on web sees Add Site, New Customer and Create Work Order (field_limits true there). In Demo the guest's permission set has no platform.web; I could not open the web UI as the guest (the handoff needs the SPA's own route, and the cookie-only redeem lands on the sign-in page). So web for the guest is not checked; per the API it has no web access in Demo.
- **iOS** (ZP-5143, In QA, iOS v1.76): not checked by me. The iOS tester is building the same guest-menu table.
- MOBILE-CHECKS (iOS): Technician seat > switch company to Demo Company (Guest · Technician): Select Site has no Create New Site. Work Orders has no Start New Work Order and no "Start your first work order". Switch back to EG-ACME: both shown.

## Restored
- The Android app was signed out of the Technician seat and signed back in as Super Admin (Field Technician experience, Android Site 2). The Technician seat was left on its home company EG-ACME (switched back from Demo before logout).

## Evidence
- QA-17Nov-ZP-5144-guest-technician-create-hidden.mp4 (36 s, outline boxes)
- QA-17Nov-ZP-5144-1-acme-tech-select-site-create-shown.png, -2-acme-tech-start-new-wo-shown.png, -3-demo-guest-select-site-no-create.png, -4-demo-guest-work-orders-no-start.png
