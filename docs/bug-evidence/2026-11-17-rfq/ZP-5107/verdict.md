# ZP-5107: Task #205, offline-first SLD layout groups + assembly placement (web)

**Verdict: PASS for the web scope → recommend READY TO RELEASE.**
- **Web rows:** layout groups persist on reopen, and online placement recreates the group. The real flag OFF (Demo) / ON (EG-ACME) for placement is now done.
- **Offline rows:** these run on the mobile apps, not the web page. They are covered by:
  - **iOS:** ZP-5102, READY TO RELEASE 16 Nov (Nency, TC3 "place an assembly offline").
  - **Server-side order:** read from the Mutation Audit Log (below).
  - **Android:** ZP-5112, being tested on Android 1.40 now.
- **New iOS defect found** while reading the iOS placement back on web: the hidden connection and the elbow are lost. Filed separately: `BUG-ios-assembly-placement-loses-hidden-and-elbow.txt`. It does not block this web ticket, but the lead/owner may choose to hold it.

- **Build:**
  - EG-ACME `index-LGhrn-48.js`, 17 Nov 2026 14:14–14:27 IST.
  - Demo OFF run: `index-CFMAmUdK.js` 14:01 IST (same code, only the build stamp differs).
- **Seats:** Super Admin (EG-ACME). Demo staff login, read only.
- **LaunchDarkly** `feature-sld-assemblies`: false for Demo Company, true for EG-ACME (live evalx at sign-in).

- **Re-check on the newest build:** `index-CkkAeA9o.js` was deployed at about 14:50 IST, during this round. At 14:56 IST, Demo Company (flag OFF) still shows no Assembly button and /assemblies says "Feature Not Available". EG-ACME (flag ON) still shows the Assembly button, and the next suggested name is now **L3** (after L2). The SLD, AiTemplateEdit and AuditLog chunks of that build differ from `index-LGhrn-48.js` only by renamed imports: the files are the same size and the code is the same.

## Checked today
| QA Review row | How | Result |
|---|---|---|
| Create layout groups; they persist on reopen | created "QA-DEMO ZP-4943 badge grp 17 Nov" and "QA-DEMO ZP-4943 marker grp 17 Nov" (`POST /api/node-group` 201), then left the SLD and reopened it | PASS: `GET /api/sld/{id}/node-groups` and the canvas list all 6 groups, each still locked |
| Assembly placement (online web) recreates its group | + Assembly › QA4942-FDR-\{name\} › L2 › Place | PASS: `POST /api/assemblies/{id}/place` 201; group "QA-DEMO ZP-4942 grp 12 Nov QA4942-FDR-L2" holds CB-2 + PNL-2 |
| Flag OFF: no placement path | Demo › SLD › unlock | PASS: no + Assembly button, and Add to… has no Assembly… |
| Flag ON | EG-ACME | PASS: + Assembly places (above) |
| Placement syncs ordered after its node/edge creates (server side) | Admin › Audit Log data (`GET /api/mutations`) for the iOS placement of the same assembly, 16 Nov (user Kush Jadia, device B874DA96…, site Android Site 2) | PASS (order): node create ×4 at 13:05:21–22Z → edge create ×2 at 13:05:23Z → node_group update at 13:05:24Z, all "applied". The node_group payload names the 2 new node ids |
| node_group/update replays in tier order | same log | PASS for this run, but its client_timestamp equals its received time, so this particular run was sent online. The offline replay itself was tested on iOS (ZP-5102 TC2/TC3) |
| Web read-back of the mobile placement | Android Site 2 SLD on web | Assets (4) and group arrive. **FAIL (iOS):** FUSE nosub → Load-1 is not hidden and CB-2 → PNL-2 has no elbow (edge_manips null). The web placement keeps both. See the BUG file |

Supporting source (iOS release/qa 34bd67e):
- `AssemblyPlacementService.swift` says: "online, the node / edge creates go out one by one (parents first) followed by the groups; offline — or from whatever step fails — the rest is queued in that same order". That matches the log.
- `APIClient.createEdge` omits `edge_manips` (the cause of the iOS bug).

## Bugs found
- **NEW:** `BUG-ios-assembly-placement-loses-hidden-and-elbow.txt`, iOS. Related to ZP-5048 but a different trigger. A JQL search found no existing ticket.
- **Web:** none. 12 Nov observation (still true, not a defect): a desktop browser that is offline cannot create a group and shows no message. The web page is not offline-first by design; the offline path is the mobile viewer bridge.

## Not covered
- A truly offline placement replay, seen in the log with client_timestamp earlier than received. This needs a device in airplane mode; see MOBILE-CHECKS.
- Android: not checked by me.

## Cross-platform
- **Twins:**
  - ZP-5102 [iOS] Task #205: READY TO RELEASE (16 Nov, Nency; Kush's test cases TC1–TC4).
  - ZP-5112 [Android] Task #205: In QA (Android 1.40).
- **iOS:** layout groups and offline placement exist in the iOS source (SLDNodeGroup, AssemblyPlacementService, AssemblyCacheService), gated by `feature-sld-assemblies`. Placement order: PASS. Placement fidelity: FAIL (new bug).
- **Android:** no source access; under test in ZP-5112.

### MOBILE-CHECKS
1. **iOS 1.75+, EG-ACME QA: offline placement replay order.**
   - Steps: open SLD "QA-DEMO v2.6 SLD editing 12 Nov" online once, then turn on Airplane mode › unlock › Assembly › QA4942-FDR-\{name\} › name IOS17OFF › Place › tap the canvas › wait 1 min › turn Airplane mode off › wait for sync.
   - Expected: on web Admin › Audit Log (Last 24 Hours), the device's mutations show node create ×4 → edge create ×2 → node_group update, with client times earlier than received times, and no failed rows. The SLD on web shows the 4 assets and group "QA-DEMO ZP-4942 grp 12 Nov QA4942-FDR-IOS17OFF".
2. **iOS: placement fidelity** (the BUG file).
   - Steps: after check 1, look at the copy on web.
   - Expected: FUSE nosub → Load-1 is hidden (badges, no line) and CB-2 → PNL-2 keeps its elbow.
   - Then refresh the SLD on the device and check the same there.
3. **iOS, company code `demo` (flag OFF).**
   - Expected: no Assembly button. Group creation still works.
4. **Android 1.40 (ZP-5112):** checks 1–3. If check 2 fails on Android too, file a separate Android ticket.
