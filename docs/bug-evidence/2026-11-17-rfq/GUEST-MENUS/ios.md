# Guest menus on iOS: Technician seat as a guest in Demo Company (owner: "for guest user the menu that are visible is incorrect or not")

- **Build:** Z Platform-QA 1.75 simulator build (binary 16 Nov 06:49 IST, from the owner). Lane A: iPhone 17, iOS 26.2. QA.
- **Date:** 17 Nov 2026, 14:40–16:52 IST.
- **Seats:**
  - TECH: home EG-ACME, Technician guest in Demo.
  - PM: subcontractor-link Technician guest on Demo WO 10 and WO 11.
  - AM: customer-link Electrical Engineer guest on site "test".
  - EE: old-invite Electrical Engineer guest on site "Demo".
- **Spec (ZP-4830 / Task #161):**
  - A guest gets the host company's role permissions and works only on assigned jobs.
  - Create Site and Start WO are hidden when field_limits is false.
- **Permissions:** /auth/v2/me while acting in Demo (X-Subdomain demo, the same call the app makes).
  - TECH and PM in Demo: Technician, 72 permissions (the home set of 73 minus platform.web). field_limits create_sites = false, create_work_orders = false. 1 accessible site.
  - AM and EE in Demo: Electrical Engineer, 84 permissions. field_limits true / true. 1 site each.
- **Method:** I recorded every screen for TECH in Demo, then the same seat at home in EG-ACME as a control (an identical tour script), plus the PM, AM and EE seats.

## TECH seat in Demo (guest Technician) vs the same seat at home
| Menu item | Guest sees? | Permission behind it | Expected per spec | Verdict |
|---|---|---|---|---|
| Tab bar: Site, Assets, Connections, SLD, Settings | Yes, same 5 tabs as at home | features.assets/connections/slds.view | Technician tabs | OK |
| Home tiles: Assets (10), Connections (4) | Yes, Demo site counts | features.assets.view, features.connections.view | Yes | OK |
| Quick action Work Orders | Yes, no count (0 assigned) | workorders.view / sessions.view | Yes, assigned WOs only | OK |
| Quick action Issues (5) | Yes. All 5 issues of site Demo, not only ones from assigned jobs | issues.view, issues.manage | Spec says "works only on assigned jobs"; issues are site-wide | **Question** (same on Android) |
| Quick action Arc Flash | Yes (readiness for site Demo) | features.arc_flash.view | Yes | OK |
| Quick action Refresh, Sites | Yes | n/a | Yes | OK |
| Quick action Schedule | Yes. Opens an empty calendar ("No scheduled work today") | **None.** The Technician role has no scheduling permission | Hidden, or harmless? | **Question** (same on Android) |
| Top-left network button (Go Offline) | Yes | n/a (device) | Yes | OK |
| Scan QR button | Yes | n/a | Yes | OK |
| Site list (Sites) | **Only "Demo"**, the 1 accessible site | accessible_sld_ids | Only the guest's site(s) | OK |
| Create New Site | **Hidden** (shown at home) | field_limits.can_create_sites = false. iOS also hides it for every guest (SLDSelectorView: "A guest works in the host's sites; they never create them") | Hidden | OK |
| WO list | Empty: "No Work Orders Yet" (none assigned) | sessions.view (server returns assigned only) | Assigned only | OK |
| Start New Work Order / "Start your first work order" / Create First Work Order | **Hidden** (the empty-list Create First Work Order shows at home) | field_limits.can_create_work_orders = false | Hidden | OK |
| Assets tab: list + Add (+) + Scan | Yes, all 10 assets of site Demo | nodes.manage | Allowed by Technician perms | OK (site-wide data, like issues) |
| Connections tab + Create Connection | Yes | edges.manage | Allowed | OK |
| SLD tab (Views, Refresh, filter, search, minimap, fit, more, lock) | Yes | slds.view, slds.manage | Allowed | OK |
| Quotes | **Not shown anywhere** (not at home either) | quotes.approve only (no quotes.view) | n/a | OK. The iOS Quotes card is commented out in SiteTabView.swift lines 280–286, so QuotesView cannot be reached |
| Settings: company (Demo Company), Account, Language, Security (Passkeys, 2FA), Session Analytics, Sync & Network (Sync Queue Analyzer, Network Mode), Equipment Library, Diagnostics (debug), Storage, About, Logout | Yes, same as at home | n/a | Yes | OK |
| Admin / company settings / users | Not shown (not shown at home either) | n/a | Hidden | OK |
| Switch Experience | Not shown (the Technician has only Field Technician) | n/a | n/a | OK |
| Logout while in Demo | **Fails**: "Logout Error – Logout failed (status 403)". The user stays signed in. | POST /auth/v2/logout → 403 guest_route_not_allowed | Logout must work | **DEFECT**, see ../BUG-ios-guest-logout-403/ |

## Other guest seats
| Seat / link | Switch sheet shows | Sites in Demo | Create New Site | WOs | Start New Work Order | Verdict |
|---|---|---|---|---|---|---|
| PM, subcontractor-link Technician guest (WO 10, 11) | "Demo Company, Guest · Technician" | "Demo" only | Hidden | **Exactly WO 11 and WO 10** (Available and Show All). Opening WO 11 works (Details, Assets tabs) | Hidden | OK, matches the expected result |
| AM, customer-link EE guest on "test" | "Demo Company, Guest · Electrical Engineer" | "test" only | Hidden (iOS hides it for every guest, although field_limits.can_create_sites = true) | 1 (Job - Oct 14, completed) | **Shown** (field_limits true) | As expected for an EE guest. Note: the client is stricter than field_limits for Create Site |
| EE, old-invite EE guest on "Demo" | "Demo Company, Guest · Electrical Engineer" | "Demo" only | Hidden (same rule) | All Demo-site WOs (15) | Shown | Reported. Whether an old-invite EE guest should list every WO on its site is part of the ZP-5315 question. The evidence is only in the job tmp dir |

## Answers to the open questions
1. **2FA when switching into Demo?** **No.** No 2FA prompt on any switch (TECH, PM, AM, EE). Same as Android.
   - The only 2FA prompts I saw were "Set Up Two-Factor Authentication / Set up later" when the automation re-opened the app, not on a switch.
2. **Schedule for a Technician guest?** Shown, and it opens an empty calendar. The Technician role has no scheduling permission. Same as Android.
3. **Issues for a guest:** site-wide (all 5 issues of site Demo, with no WO assigned), not limited to assigned jobs. Same as Android.
4. **Security check (ZP-5315) as PM:** results are only in the job tmp dir (sec2/ios-result.md), not in the repo.
   - In short: the PM's iOS work-order list and search do not show the unassigned host WO.

## Findings
- **New defect:** a guest cannot log out while in the host company (403). Draft: `../BUG-ios-guest-logout-403/BUG-ios-guest-logout-403.txt`, with video and screenshots.
- **Questions for the spec owner (not filed):**
  - Schedule shows for guest Technicians.
  - Issues and assets are site-wide for a guest with no assigned WO.
  - iOS hides Create Site for every guest even when field_limits says true (EE guests).

## Evidence (this folder, iOS files)
- `QA-17Nov-GUEST-MENUS-ios-demo-guest-technician.mp4` (62 s, step captions): TECH guest tour + PM's assigned-only WO list.
- `ios-1-tech-guest-demo-home.png` … `ios-6-tech-guest-demo-settings-bottom.png`: TECH in Demo. Green = matches the spec; amber = question.
- `ios-7-pm-sublink-guest-demo-only-assigned-wo10-wo11.png`, `ios-8-am-customer-link-ee-guest-demo-site-test-only.png`, `ios-9-am-ee-guest-demo-start-new-wo-shows.png`, `ios-10-ee-oldinvite-guest-demo-site-demo-only.png`.
- Raw dumps of every screen: `.playwright-mcp/ios170/rfq17nov/g01-demo.json` and `g01-acme.json` (control), and the g02-* screenshots for PM, AM and EE.
