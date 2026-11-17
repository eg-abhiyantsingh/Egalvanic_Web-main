# ZP-5218 cross-platform (Android): Add Existing by Service
- Platform/build: Android 1.40 (54). Seat: Super Admin, EG-ACME, site Android Site 2. 17 Nov 2026 ~15:12 IST.
- Steps: WO "Job - Nov 6, 12:03 PM" > Assets > room R1 > + (Add Assets) > options; then Link Existing Asset.
- Result: Add Assets offers **New Asset, Link Existing Asset, Photo Walkthrough, Quick Count**. Link Existing Asset is a room-grouped picker with search, Scan Asset Code and Select All. There is **no "by Service" picker and no "Downstream of" filter**. (The WO-level + opens New Location.)
- Parity: **not on Android** (same as iOS per the backfill note). Nothing was added (Back without Add).
- Evidence: android-wo-assets-add-options.png (New Location sheet from the WO-level +), android-wo-room-add-assets-options.png, android-link-existing-asset-picker.png.

## iOS (17 Nov, iOS tester)
- Platform/build: iOS, Z Platform-QA 1.75 simulator build (binary 16 Nov 06:49 IST, from the owner), lane A (iPhone 17, iOS 26.2). Source read: eg-pz-mobile-iOS release/qa 34bd67e (#826).
- Seat: SA, EG-ACME, site (s) Wild Goose Brewery, WO "Work Order - Nov 17, 2:26 PM" › Assets › room "Bldg_465 › Floor 77 Updated_650 › Test" › + .
- Result: the + menu offers **New Asset, Link Existing Asset, Photo Walkthrough, Quick Count**. Link Existing Asset is a room-grouped picker with search, Scan, Select All and "Add (n)". On an asset: Add OCP › Create New Child / Create Multiple / Photo Walkthrough / Link Existing Node.
- There is **no "Add Existing by Service" picker and no "Downstream of" filter** on iOS. **Parity: not on iOS** (same as Android).
- Evidence: ios-1-wo-room-add-menu-no-add-by-service.png. The same menus are in the ZP-5104 video.
