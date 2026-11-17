# ZP-5280 cross-platform (iOS): quote amounts
- Platform/build: iOS, Z Platform-QA 1.75 simulator build (binary 16 Nov 06:49 IST, from the owner), lane A (iPhone 17, iOS 26.2). Source read: eg-pz-mobile-iOS release/qa 34bd67e (#826).
- Seat: SA, EG-ACME. 17 Nov 2026.
- **iOS has no reachable Quotes screen.** Views/Quotes/QuotesView.swift exists, but its only entry point, the "Quotes" FeatureCard on the site home, is commented out (Views/Site/SiteTabView.swift lines 279–286). `showingQuotes` is never set to true anywhere else, so QuotesView cannot be opened. On the device, the site home quick actions are Work Orders, Issues, Arc Flash, Refresh, Sites and Schedule, with no Quotes (SA seat, EG-ACME, (s) Wild Goose Brewery). The guest Technician tour (GUEST-MENUS/ios.md) found no Quotes anywhere either.
- The sync code for quotes (APIClient+SnapshotPayload snapshotQuoteCreateBody/UpdateBody, SyncExecutionService createQuote/updateQuote) stays, but a user cannot create or edit a quote on iOS in this build.
- So the amount and status of the QA-DEMO ZP-5280 quotes can't be compared on iOS. **Parity: not on iOS** (no quote list or total on the device). Nothing to compare: no $0 risk on iOS, because there is no amount screen.
- Evidence: ios-1-site-home-quick-actions-no-quotes.png.
