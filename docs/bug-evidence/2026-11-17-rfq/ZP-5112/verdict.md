# ZP-5112 [Android] Task #205: SLD viewer layout groups + offline-first assembly placement

**Verdict: PASS** (all three QA-review rows, with feature-sld-assemblies ON in EG-ACME and OFF in Demo Company). One related parity gap was found and filed separately: the assembly copy-name suggestion (BUG-assembly-name-suggestion, ZP-5264 class).

- Build: Android Z Platform (QA) **1.40 (54)**, **SLD Viewer Version 90** (1.39 had 89). Emulator 1080x2400 density 420. The APK now contains node-group, assemblies and feature-sld-assemblies code (absent in 1.39).
- Seats: Super Admin (EG-ACME), and the Technician seat as a guest in Demo Company for the flag-OFF check. 17 Nov 2026, 13:55-15:35 IST.
- Flags (lead, live LD evalx 17 Nov): feature-sld-assemblies TRUE for EG-ACME, FALSE for Demo. GET /api/assemblies → 200 (one assembly: "QA4942-FDR-{name}", 4 nodes, 2 edges).
- Fix: eg-pz-mobile-android PR #566.

## What was run
| # | QA-review row | Result |
|---|---------------|--------|
| 1 | Android shows layout groups made on web | Site "QA-DEMO ZP-4800 SLD canvas v2.4 (delete me)": the frames and labels "QA4940 GRP full" (blue) and "QA4940 GRP partial" (red) are drawn, matching web. On 12 Nov (1.39) there were none. PASS |
| 2 | Create layout groups in the Android viewer; they persist on reopen | Unlocked > long-press a node: **Add to Group** / **Remove from Group** (Add to Group is disabled with "Already in a group — remove first…" when the node is already grouped). The Layout Group dialog has Create new group / Add to existing group, with name and color. Created "QA-DEMO ZP-5112 17Nov GRP" (offline). After the sync, I left the SLD and reopened it: the group is still drawn. Server: node-groups for the SLD = 5 (was 3), incl. "QA-DEMO ZP-5112 17Nov GRP" (d560c752-b0e1-45df-853c-6876b6b54dd7). PASS |
| 3 | Place an assembly offline; reconnect; it syncs ordered after its node/edge creates | Airplane mode ON > the app shows offline > Assembly > "QA4942-FDR-{name}", name "QA-DEMO ZP-5112 17Nov" > Will create Fuse / Circuit Breaker / Load / Panelboard > Place > tap the canvas. It was placed with "saved offline" and 9 pending, then 11 after the group change. Airplane OFF > Go Online > Sync 11 records > the badge cleared. Server: 4 nodes (FUSE nosub, CB-2 at 08:36:27Z; PNL-2, Load-1 at 08:36:30Z), and both edges after them (FUSE→Load-1 at 08:36:30.579, CB-2→PNL-2 at 08:36:30.789), plus the assembly's own layout group "QA-DEMO ZP-4942 grp 12 Nov QA4942-FDR-QA-DEMO ZP-5112 17Nov". PASS |
| 4 | Flag OFF (Demo Company): no assembly controls | Technician seat as guest in Demo, SLD unlocked: Asset, Bus, Note, Multi, Area, and **no Assembly**. The same seat in EG-ACME: Assembly is shown. PASS. (In Demo the node menu still offers "Add to Group": layout groups are not behind this flag, only assemblies.) |
| 5 | Placement fidelity (from the backfill, ZP-4942/5107): placed as "AND17" on "QA-DEMO v2.6 SLD editing 12 Nov" | Server: "FUSE nosub → Load-1" edge_manips {"hidden":true} (still hidden) and "CB-2 → PNL-2" edge_manips {"elbows":[[904,1781]]} (elbow kept), the same as the web-placed L1/L2. Edges were created after the nodes. PASS |

## Related defect (filed separately, Android SLD viewer)
- **BUG-assembly-name-suggestion.txt** (in ZP-5264/xplat): on Android the second placement's Name box is pre-filled "L1 CB-3" on the SLD where the server's place-preview returns the next free name "L3". On ZP-4800 it is pre-filled "QA-DEMO ZP-5112 17Nov PNL-3" (previous copy name + a device name). This is the web bug ZP-5264 (READY TO RELEASE on web), still present in Android viewer 90.

## Not covered
- A real device or tablet.
- Group edit/rename/delete on Android. Moving a grouped node.
- The Demo flag-OFF check was done with the Technician guest seat (no Demo Super Admin seat).

## Cross-platform
- **Web** (index-CkkAeA9o.js): the groups and the assembly made on Android read back on web/API (Groups (5); nodes, edges, hidden + elbow kept). PASS. Web twin ZP-5107 is In QA.
- **iOS** (ZP-5102, READY TO RELEASE, iOS v1.76): the backfill note says iOS seems to lose the hidden connection and the elbow on placement; the lead has an iOS repro requested. Not checked by me.
- MOBILE-CHECKS (iOS): on "QA-DEMO v2.6 SLD editing 12 Nov" place QA4942-FDR (name IOS17); on web/API check FUSE nosub → Load-1 edge_manips.hidden = true and CB-2 → PNL-2 has elbows. Place it a second time: the suggested name should be the next free one (L3/L4), not "L1 CB-3". Demo (flag OFF): no Assembly control.

## Test data (QA test sites, left in place)
- "QA-DEMO ZP-4800 SLD canvas v2.4 (delete me)" (SLD 363d5350-b42d-4e48-850f-b9f72d27ea08): nodes QA4942-FDR-QA-DEMO ZP-5112 17Nov FUSE nosub / CB-2 / Load-1 / PNL-2 (e9f9c59a…, 5dc46eea…, 3e51f45e…, 995e3f86…); groups "QA-DEMO ZP-5112 17Nov GRP" and "QA-DEMO ZP-4942 grp 12 Nov QA4942-FDR-QA-DEMO ZP-5112 17Nov".
- "QA-DEMO v2.6 SLD editing 12 Nov" (SLD c4438cd3-59b3-4639-9e60-2752dc5aa478): QA4942-FDR-AND17 FUSE nosub / CB-2 / Load-1 / PNL-2.
- Airplane mode was turned off again. The app is back on Android Site 2.

## Evidence
- QA-17Nov-ZP-5112-layout-groups-offline-assembly.mp4 (69 s)
- QA-17Nov-ZP-5112-0-about-1.40-54-sld-viewer-90.png, -1-android-draws-web-layout-groups.png, -2-unlocked-assembly-tool.png, -3-place-an-assembly-picker.png, -4-offline-assembly-will-create-4.png, -5-offline-assembly-placed-9-pending.png, -6-offline-new-group-created-11-pending.png, -7-after-sync-reopen-groups-persist.png, -8-web-readback-sld.png, -9-demo-flag-off-no-assembly-tool.png
- AND17 fidelity: ../ZP-4942/xplat/
