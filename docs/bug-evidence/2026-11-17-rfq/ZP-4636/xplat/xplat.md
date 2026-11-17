# ZP-4636 (and ZP-5279/5280/5281) cross-platform: does Android have a site Quotes tab?
- Platform/build: Android 1.40 (54). Seat: Super Admin, EG-ACME. 17 Nov 2026 ~15:15 IST.
- Steps: Field Technician experience (home quick actions, bottom nav Site / Assets / Connections / SLD / Settings, Site screen); Settings > Switch Experience > Account Manager (Site Walks list, a site walk detail); back to Field Technician.
- Result: **no Quotes tab or quote screen** in either experience. The APK caches quotes locally (Room table `quotes`, quote–task mapping, a draft-quote count) and has one "Quotes" label, but no screen reaches them in the UI.
- Parity: **differs from iOS** (iOS has Views/Quotes/QuotesView.swift per the lead). Android: not on this platform, so the iOS items 1-2 (create/edit a quote, created_by/modified_by, amounts) can't run on Android.
- Evidence: android-account-manager-home.png; APK string check in ../../ZP-5292/xplat/apk-string-check.txt.
