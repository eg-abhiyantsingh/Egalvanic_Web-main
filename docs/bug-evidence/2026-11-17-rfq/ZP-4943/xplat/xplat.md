# ZP-4943 cross-platform (Android SLD viewer)
- Platform/build: Android 1.40 (54), SLD Viewer 90. Seat: Super Admin, EG-ACME. Site "QA-DEMO v2.6 SLD editing 12 Nov" (SLD c4438cd3-…), group "QA-DEMO ZP-4943 badge grp 17 Nov" (7e371ebd-…, members QA4942-FDR-L1 FUSE nosub + Load-1). 17 Nov 2026 ~17:10 IST.
- Steps and results:
  1. SLD locked > search "QA4942-FDR-L1 FUSE" > tap the hidden-edge badge "QA4942-FDR-L1 Load-1 →" under the fuse (inside the group): the canvas **jumps to QA4942-FDR-L1 Load-1 and selects it** (blue frame, the hidden run is highlighted). PASS
  2. Locked SLD > long-press a connection: the menu offers **only "Select Entire Run"**. PASS
- Parity: **same as web** (members, badges and edges inside a locked group stay clickable).
- Not covered: dragging a locked group (edit mode) and "Edit with AI" for name templates (web-only assembly editor; Android has no Save as Assembly screen).
- Evidence: android-locked-group-badge-and-run.mp4, android-1-badge-before-tap.png, android-2-after-badge-tap.png, android-3-locked-longpress-connection.png

## iOS (17 Nov, iOS tester)
- Platform/build: iOS, Z Platform-QA 1.75 simulator build (binary 16 Nov 06:49 IST, from the owner), SLD Viewer 90, lane A. Seat: Super Admin, EG-ACME. Site "QA-DEMO v2.6 SLD editing 12 Nov" (SLD c4438cd3-…), group "QA-DEMO ZP-4943 badge grp 17 Nov". 17:09 IST.
- Steps and results:
  1. SLD locked › search "QA4942-FDR-L1 FUSE" › tap the hidden-edge badge "QA4942-FDR-L1 Load-1 →" under the fuse (inside the group): the canvas **jumps to QA4942-FDR-L1 Load-1 and selects it** (blue frame; the hidden run is highlighted in yellow). PASS
  2. Locked SLD › long-press a connection: the menu offers **only "Select Entire Run"**. PASS
- Parity: **same** as web and Android.
- Side note (not filed): on this SLD, "QA26 Load-22" is drawn on top of "QA4942-FDR-L1 FUSE nosub". This is test layout data, not the viewer.
- Evidence: ios-1-badge-before-tap.png, ios-2-after-badge-tap-load1-selected.png, ios-3-locked-longpress-only-select-entire-run.png
