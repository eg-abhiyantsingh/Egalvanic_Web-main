# ZP-5274 cross-platform (Android): Technician permissions, EG-ACME vs guest in Demo
- Platform/build: Android 1.40 (54). Seat: AppConstants TECH (home EG-ACME; guest Technician in Demo Company). 17 Nov 2026 14:50-15:05 IST.
- Server (GET /api/auth/v2/me): EG-ACME **73** permissions incl. platform.web, field_limits true/true. Demo **72** (EG-ACME's minus platform.web), is_guest true, field_limits false/false. (The web ticket's counts are 89/88; /auth/v2/me returns 73/72 for this seat today. The difference is only platform.web in both cases.) Lists: ../../GUEST-MENUS/tech-seat-auth-v2-me-acme-vs-demo.json
- Android screens, same seat: identical menus in both companies (home quick actions Work Orders / Issues / Arc Flash / Sites / Refresh / Schedule; bottom nav Site / Assets / Connections / SLD / Settings; add/create on assets, issues, connections). The only differences are field_limits (Create New Site, Start New Work Order hidden in Demo) and the flag-gated Assembly tool (hidden in Demo). Full table: ../../GUEST-MENUS/android.md (+ video).
- Removed-permission screens to watch (opportunities, planning, scheduling, site_overview, tasks, data.export): Android has no opportunities, planning, site-overview, tasks or export screens for this seat. **Schedule** (calendar + View Sites) shows in both companies although the set has no scheduling permission → question for the lead (same on iOS?).
- Parity: Android follows the server permission set except the Schedule question. Web/iOS comparison is the lead's.

## iOS (17 Nov, iOS tester)
- Platform/build: iOS, Z Platform-QA 1.75 simulator build (binary 16 Nov 06:49 IST, from the owner), lane A. Seat: Technician (home EG-ACME; guest Technician in Demo). 14:40–16:52 IST.
- Server (/auth/v2/me): EG-ACME 73 permissions, Demo 72. The only difference is platform.web. field_limits true/true at home, false/false in Demo.
- iOS screens, same seat, recorded by an identical tour script in both companies: the menus are **identical**.
  - Tab bar: Site / Assets / Connections / SLD / Settings.
  - Home quick actions: Work Orders / Issues / Arc Flash / Refresh / Sites / Schedule.
  - Add/create on Assets, Issues and Connections. SLD tools.
  - The only differences: Create New Site and the empty-list Create First Work Order are hidden in Demo (field_limits), and Assembly is hidden in Demo (flag OFF).
  - Full table: ../../GUEST-MENUS/ios.md, with video.
- Removed-permission screens (opportunities, planning, scheduling, site_overview, tasks, data.export):
  - iOS has no opportunities, planning, tasks or export screen for this seat. The Tasks and Sessions cards are commented out in SiteTabView.swift.
  - The "site home" is the app's own home, not site_overview.
  - **Schedule** shows in both companies (an empty calendar) although the set has no scheduling permission. This is the same open question as on Android.
- Parity: iOS = Android, and both follow the server set except the Schedule question.
- **New iOS/backend defect found with the guest seats:** a guest can't log out while in the host company (403 guest_route_not_allowed). See ../../BUG-ios-guest-logout-403/.
