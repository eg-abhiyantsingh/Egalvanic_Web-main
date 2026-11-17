# ZP-4942 / ZP-5107 cross-platform (Android)
- Platform/build: Android 1.40 (54), SLD Viewer 90. Seat: Super Admin (EG-ACME, flag ON). Flag OFF check: Technician seat as a guest in Demo Company. 17 Nov 2026. Web/API build index-CkkAeA9o.js.
1. **Placement fidelity**: site "QA-DEMO v2.6 SLD editing 12 Nov" > SLD > unlock > Assembly > QA4942-FDR-{name}, name **AND17** (the box was pre-filled "L1 CB-3", see ZP-5264 bug) > Place > tap the canvas.
   - Server graph (GET /api/sld/c4438cd3-…/graph): "QA4942-FDR-AND17 FUSE nosub → Load-1" edge_manips = {"hidden":true} (**still hidden**). "CB-2 → PNL-2" edge_manips = {"elbows":[[904,1781]]} (**elbow kept**). Nodes 09:57:41-44Z, edges 09:57:44-45Z (after the nodes). CB-2 and PNL-2 share layout group 6a8fc872-….
   - Control, web-placed copies on the same SLD: L1 (12 Nov) and L2 (17 Nov) have the same hidden/elbow manips. (L1's FUSE/Load-1 are also in a second group 7e371ebd; AND17 and L2 have no group on FUSE/Load-1. AND17 matches today's L2.)
   - Parity: **same** (PASS).
2. **Company code demo (flag really OFF)**: the Technician seat as a guest in Demo Company, SLD unlocked: Asset, Bus, Note, Multi, Area, and **no Assembly**. The same seat in EG-ACME has Assembly. Parity: **same as expected** (PASS). This was done with the Technician seat; Super Admin has no Demo membership.
- Evidence: android-assembly-AND17-placement.mp4 (35 s), android-1-name-prefilled.png, android-2-will-create-AND17.png, android-3-placed-AND17.png. Flag OFF: ../../GUEST-MENUS/android-12-demo-guest-sld-unlocked-no-assembly.png, ../../ZP-5112/QA-17Nov-ZP-5112-9-demo-flag-off-no-assembly-tool.png
- Test data: SLD c4438cd3-59b3-4639-9e60-2752dc5aa478, nodes QA4942-FDR-AND17 FUSE nosub / CB-2 / Load-1 / PNL-2 (left in place).
