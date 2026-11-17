# ZP-4636 (and ZP-5279/5280/5281) cross-platform: does Android have a site Quotes tab?
- Platform/build: Android 1.40 (54). Seat: Super Admin, EG-ACME. 17 Nov 2026 ~15:15 IST.
- Steps: Field Technician experience (home quick actions, bottom nav Site / Assets / Connections / SLD / Settings, Site screen); Settings > Switch Experience > Account Manager (Site Walks list, a site walk detail); back to Field Technician.
- Result: **no Quotes tab or quote screen** in either experience. The APK caches quotes locally (Room table `quotes`, quote–task mapping, a draft-quote count) and has one "Quotes" label, but no screen reaches them in the UI.
- Parity: **differs from iOS** (iOS has Views/Quotes/QuotesView.swift per the lead). Android: not on this platform, so the iOS items 1-2 (create/edit a quote, created_by/modified_by, amounts) can't run on Android.
- Evidence: android-account-manager-home.png; APK string check in ../../ZP-5292/xplat/apk-string-check.txt.

## iOS (17 Nov, iOS tester)
- Platform/build: iOS, Z Platform-QA 1.75 simulator build (binary 16 Nov 06:49 IST, from the owner), lane A (iPhone 17, iOS 26.2). Source read: eg-pz-mobile-iOS release/qa 34bd67e (#826).
- **iOS has no reachable Quotes screen.** Views/Quotes/QuotesView.swift exists, but its only entry point, the "Quotes" FeatureCard on the site home, is commented out (Views/Site/SiteTabView.swift lines 279–286). `showingQuotes` is never set to true anywhere else, so QuotesView cannot be opened. On the device, the site home quick actions are Work Orders, Issues, Arc Flash, Refresh, Sites and Schedule, with no Quotes (SA seat, EG-ACME, (s) Wild Goose Brewery). The guest Technician tour (GUEST-MENUS/ios.md) found no Quotes anywhere either.
- The sync code for quotes (APIClient+SnapshotPayload snapshotQuoteCreateBody/UpdateBody, SyncExecutionService createQuote/updateQuote) stays, but a user cannot create or edit a quote on iOS in this build.
- So created_by / modified_by on an iOS-made quote cannot be tested through the UI. **Parity: not on iOS** (no quote UI). If the Quotes card is ever turned back on, re-run: create + edit a QA-DEMO quote on iOS, then GET /api/quote/<id> → created_by / modified_by = the iOS user.
- Evidence: ios-1-site-home-quick-actions-no-quotes.png (amber box: quick actions without Quotes).
