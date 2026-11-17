# ZP-5264 cross-platform (Android)
- Platform/build: Android 1.40 (54), SLD Viewer 90, emulator. Seat: Super Admin, EG-ACME (flag ON). 17 Nov 2026 ~15:20-15:35 IST. Web build index-CkkAeA9o.js.
- Steps: v2.6 SLD (L1 and L2 exist) > unlock > Assembly > QA4942-FDR-{name}. Also on ZP-4800 after a custom-named copy.
- Result: Android pre-fills **"L1 CB-3"** (v2.6 SLD), while the server's place-preview for the same SLD returns **"L3"**. On ZP-4800 it pre-fills "QA-DEMO ZP-5112 17Nov PNL-3".
- Parity: **differs**. Web is fixed (ZP-5264 RTR; the server suggests L3). The Android viewer still shows the bug. → BUG-assembly-name-suggestion.txt (+ video, 2 PNGs).
- iOS: not checked (iOS tester item 3).

## iOS (17 Nov, iOS tester)
- Platform/build: iOS, Z Platform-QA 1.75 simulator build (binary 16 Nov 06:49 IST, from the owner), lane A. Seat: Super Admin, EG-ACME (flag ON). Site "QA-DEMO v2.6 SLD editing 12 Nov" (L1, L2, AND17 already placed). 16:14 IST.
- Steps: SLD › unlock › Assembly › QA4942-FDR-\{name\}.
- Result: iOS pre-fills the Name as **AND18**, the next number after the newest placement AND17. "Will create" lists QA4942-FDR-AND18 FUSE nosub / CB-2 / ….
  - It does **not** show the Android bug "L1 CB-3".
  - It also does not match the server's place-preview "L3". Both AND18 and L3 are free names, so I am not filing this.
  - When the field is cleared, the placeholder shows "L1".
- Parity: iOS **differs** from Android (no "L1 CB-3" bug) and from the web/server suggestion (AND18 vs L3). Handles and elbows: the iOS placement loses the hidden flag and the elbow on the server (see ../../ZP-4942/xplat iOS section and ../../ZP-5107/ios-repro/).
- Evidence: ios-1-second-placement-prefilled-AND18.png. Video: ../../ZP-5107/ios-repro/QA-17Nov-ZP-5107-ios-repro-IOS17-hidden-and-elbow-lost.mp4 (step 2).
