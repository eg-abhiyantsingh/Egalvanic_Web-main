# Guest menus on Android: Technician seat as a guest in Demo Company

- Build: Android Z Platform (QA) **1.40 (54)**, SLD Viewer 90, emulator 1080x2400. 17 Nov 2026, 14:50-15:05 IST.
- Seat: AppConstants TECH. Home company EG-ACME (Technician); guest membership in Demo Company (role Technician, membership 762f3546-…). Switched with Sites header › **Switch Company** › Demo Company "Guest · Technician".
- Server permission sets (GET /api/auth/v2/me, same seat): EG-ACME 73 permissions, field_limits true/true. **Demo 72 permissions** (EG-ACME's minus `platform.web`), is_guest true, **field_limits can_create_sites false, can_create_work_orders false**. Full lists: tech-seat-auth-v2-me-acme-vs-demo.json.
- Spec (ZP-4830 / Task #161): a guest gets the host company's Technician permissions and works only on assigned jobs. Create Site and Start WO are hidden (field_limits false).
- Video: QA-17Nov-GUEST-MENUS-android-demo-guest-technician.mp4 (61 s, outline boxes on create/add controls). Screens: android-0 … android-14 PNG.

| Menu item | Guest sees? (Demo) | Same seat at home (EG-ACME) | Permission behind it | Expected per spec | Verdict |
|---|---|---|---|---|---|
| Switch Company sheet (Sites header) | Yes: EG-ACME "Your company", Demo Company "Guest · Technician" | Yes | guest membership | Yes | OK |
| Select Site list | Only "Demo" (1 site) | All EG-ACME sites | site assignment / accessible SLDs | Only assigned sites | OK (only one Demo site is shown; I couldn't check the total Demo site count without a Demo admin seat) |
| **Create New Site** | **No** | Yes | field_limits.can_create_sites | Hidden for guest | **PASS** |
| Home cards: Assets 10 / Connections 4 | Yes | Yes (647 / 261) | features.assets.view, features.connections.view | Yes (Technician) | OK |
| Quick action Work Orders | Yes (no badge) | Yes (no badge: none assigned) | workorders.view / sessions.view | Yes | OK |
| Work Orders list | "No Work Orders Yet" (none assigned) | "No Work Orders Yet" (none assigned to this seat) | sessions.view; assignment | Only assigned WOs | OK (nothing assigned in either company; not proven with an assigned/unassigned pair) |
| **Start New Work Order** + empty-state "Start your first work order" | **No / No** | Yes / Yes | field_limits.can_create_work_orders | Hidden for guest | **PASS** |
| Quick action Issues | Yes (badge 5) | Yes (97) | features.issues.view | Yes | OK |
| Issues list | All issues on the Demo site (5+), incl. "test abhiyant on Panelboard-1" | All site issues | issues.view | Technician perms: yes | OK (site-wide, not job-scoped; compare with iOS/web) |
| Add Issue (+ on Issues, Add Issue on asset) | Yes | Yes | issues.manage | Technician has it | OK |
| Quick action Arc Flash (Arc Flash Analysis) | Yes | Yes | features.arc_flash.view | Yes | OK |
| Quick action Sites | Yes (opens Select Site, no create) | Yes | — | Yes | OK |
| Quick action Refresh | Yes | Yes | — | Yes | OK |
| Quick action **Schedule** (calendar + "View Sites") | Yes | Yes | no scheduling/planning permission in the set (only tasks.*) | Unclear: ZP-5274's list says Technician lost "scheduling" | **Question** (shows for both; compare with iOS/web) |
| Support (Z button), Scan Asset Code | Yes | Yes | — | Yes | OK |
| Bottom nav Site / Assets / Connections / SLD / Settings | Yes, all five | Yes, all five | features.*.view | Yes | OK |
| Assets tab + (Add Assets: New Asset, Photo Walkthrough, Quick Count) | Yes | Yes | nodes.manage | Technician has it | OK |
| Asset page sections + Add Record / Add Child Node (OCP, Connections) | Yes | Yes | nodes.manage, edges.manage | Yes | OK |
| Asset ⋮ menu: View on SLD, Copy From, Copy To, Duplicate | Yes | (not compared) | nodes.manage | Yes | OK |
| Connections tab + (Create Connection) | Yes | Yes | edges.manage | Yes | OK |
| SLD (view; unlock edit tools Asset, Bus, Note, Multi, Area) | Yes | Yes | slds.view, slds.manage | Yes | OK |
| SLD **Assembly** tool | **No** | Yes | LD feature-sld-assemblies (Demo OFF, EG-ACME ON) | Hidden in Demo | **PASS** |
| SLD node menu Add to Group / layout groups | Yes | Yes | slds.manage | Not flag-gated | OK (note: groups show with the assemblies flag OFF) |
| Forms (EG forms in a WO) | Not reachable (no WO assigned) | — | form_instances.* | Only in assigned WOs | Not checked |
| Quotes / Sales entry | None anywhere | None | quotes.approve is in the set | Android has no Quotes screen in any experience | OK (not on Android) |
| Settings: Account (email, user ID), Security (Passkeys, 2FA, Screen Lock), Language, Sync & Network, Diagnostics, Session Analytics, Device Library, Storage, About, Logout | Yes | Yes (same list) | — | Yes | OK |
| Settings › Switch Experience | No | No (Super Admin has it) | multiple experiences | — | OK (role-based, not guest-specific) |

## Findings
- No extra or missing item against the spec. Both field_limits controls are hidden for the guest, and the flag-gated Assembly tool is hidden in Demo.
- Questions for the lead's 3-way compare (not filed as bugs):
  1. **Schedule** shows for the guest (and for the Technician at home), although the Technician permission set has no scheduling/planning permission (ZP-5274's removed list mentions "scheduling").
  2. **Issues** are site-wide for the guest (all Demo-site issues), not limited to assigned jobs. That matches `issues.view`, but the spec phrase "works only on assigned jobs" may mean more.
  3. **Layout groups** (Add to Group) are offered with feature-sld-assemblies OFF; only Assembly is flag-gated.
- Not verified: the assigned vs. unassigned WO split (no WO is assigned to this seat in either company), and Demo's full site list (no Demo admin seat).

## Extra guest checks (added 17:20-17:45 IST, other seats; Android 1.40 (54))
Each seat signed in on Android (Field Technician experience), then Sites header › Switch Company › Demo Company, and back to EG-ACME afterwards. Nobody was ticked or unticked on any Demo WO.

| Seat (guest kind) | Switch sheet label | Demo sites shown | Create New Site | Start New Work Order | WOs listed | 2FA on switch? |
|---|---|---|---|---|---|---|
| PM (subcontractor-link Technician guest) | Demo Company · Guest · Technician | **Demo** only | **hidden** | **hidden** | **exactly WO 10 and WO 11** ("QA-DEMO ZP-4830 17 Nov WO 10 (drawer add + remove)", "… WO 11 (PM stays ticked)"): Available (2) / All (2); the home WO pill lists the same two | No |
| AM (customer-link guest) | Demo Company · Guest · **Electrical Engineer** | **test** only (home: 0 assets / 0 connections) | **hidden** | **shown** | Available (0) / All (1) ("No available work orders … Show all") | No |
| EE (old-invite guest) | Demo Company · Guest · **Electrical Engineer** | **Demo** only | **hidden** | **shown** | **Available (12) / All (15)**, incl. QA-DEMO ZP-4830 17 Nov WO 7, 8, 9, 10, 11 | No |

Evidence: android-pm-0-switch-sheet.png, android-pm-1-demo-select-site.png, android-pm-2-demo-work-orders.png, android-pm-3-demo-work-orders-all.png, android-am-*.png, android-ee-*.png. Raw label dumps were kept locally.

Notes for the 3-way compare:
- PM (Technician guest) matches the spec exactly: the assigned/unassigned split holds (only WO 10/11), and both creates are hidden.
- EE guests (AM and EE): **Start New Work Order is shown** (consistent with field_limits true for EE guests, per the lead), but **Create New Site is hidden** for both. If field_limits.can_create_sites is also true for these EE guests, Android hides site creation for EE guests anyway. Please compare with the /auth/v2/me values the web tester recorded. I did not read /auth/v2/me for AM/EE myself.
- The EE old-invite guest sees 12 available / 15 total Demo WOs (not only assigned ones). That may be expected for an old-invite EE guest; flagging it for the compare.
- 2FA: Android did not ask for 2FA when switching into Demo for PM, AM or EE (nor for the Technician earlier). The 2FA enrolment prompt ("Set up later") appears only at sign-in.
- Security check (unassigned host WO by id, ZP-5315 class): done as PM in Demo. The result and evidence are kept outside the repo (jobs tmp/sec2) per the public-repo rule. In short: not reachable through Android lists or search.
