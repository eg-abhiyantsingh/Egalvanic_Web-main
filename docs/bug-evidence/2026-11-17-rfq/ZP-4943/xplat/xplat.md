# ZP-4943 cross-platform (Android SLD viewer)
- Platform/build: Android 1.40 (54), SLD Viewer 90. Seat: Super Admin, EG-ACME. Site "QA-DEMO v2.6 SLD editing 12 Nov" (SLD c4438cd3-…), group "QA-DEMO ZP-4943 badge grp 17 Nov" (7e371ebd-…, members QA4942-FDR-L1 FUSE nosub + Load-1). 17 Nov 2026 ~17:10 IST.
- Steps and results:
  1. SLD locked > search "QA4942-FDR-L1 FUSE" > tap the hidden-edge badge "QA4942-FDR-L1 Load-1 →" under the fuse (inside the group): the canvas **jumps to QA4942-FDR-L1 Load-1 and selects it** (blue frame, the hidden run is highlighted). PASS
  2. Locked SLD > long-press a connection: the menu offers **only "Select Entire Run"**. PASS
- Parity: **same as web** (members, badges and edges inside a locked group stay clickable).
- Not covered: dragging a locked group (edit mode) and "Edit with AI" for name templates (web-only assembly editor; Android has no Save as Assembly screen).
- Evidence: android-locked-group-badge-and-run.mp4, android-1-badge-before-tap.png, android-2-after-badge-tap.png, android-3-locked-longpress-connection.png
