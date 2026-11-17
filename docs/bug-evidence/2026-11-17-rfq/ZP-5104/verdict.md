# ZP-5104 [iOS] Task #227: work type follows every join; connection cables join the WO: verdict

**Verdict: PASS → READY TO RELEASE.** Every in-scope row passed on the simulator, and the web read-back matches. No new defects in scope.

- **Build:** Z Platform-QA 1.75 simulator build (binary 16 Nov 06:49 IST, from the owner). Lane A: iPhone 17, iOS 26.2. API https://eg-pz.qa.egalvanic.ai/api.
- **Web build for the read-back:** index-CkkAeA9o.js.
- **Seat:** SA (EG-ACME). Site (s) Wild Goose Brewery, room "Bldg_465 › Floor 77 Updated_650 › Test".
- **Work order:** "Work Order - Nov 17, 2:26 PM", created on iOS for this test, id 2dcb3c4f-5eca-4fe1-a32a-bde472a64e89.
  - Automation typed the name into the default-name field but it did not stick, so the WO kept its default name. The "QA-DEMO ZP-5104 17Nov …" names are on the assets.
- **Date:** 17 Nov 2026, 14:56–16:05 IST.

## Rows (Kush's comment 46180)
| TC | Step | Result |
|---|---|---|
| TC4.1 | Work Type = General → room › + › New Asset "QA-DEMO ZP-5104 17Nov G1" (Panelboard) → long-press › Manage Services | "Registered on this asset: No services registered." PASS |
| TC4.2 | Details › Capture Settings › Work Type → Arc Flash Data Collection (AFDC), then New Asset "A1 new" | A1 has AFDC registered. The WO gains the SLD tab. PASS |
| TC1 New Asset | room › + › New Asset | AFDC registered (A1, and O1 offline). PASS |
| TC1 Link Existing Asset | room › + › Link Existing Asset › "QA-DEMO 4 Nov ZP-5171 Main Distribution Panel" › Add (1) | The panel and the 7 breakers that come along with it all have AFDC. PASS |
| TC1 Quick Count | room › + › Quick Count › Circuit Breaker ×1 › Create 1 Assets | "Circuit Breaker 1" has AFDC. PASS |
| TC1 Photo Walkthrough | room › + › Photo Walkthrough, photo from the Gallery (the simulator has no camera; camera permission denied) › Create All (1) | The AI-named "Circuit Breaker 1" has AFDC. PASS |
| TC1 Create Child | A1 › Add OCP › Create New Child "C1 child" (Circuit Breaker) › Save Changes | AFDC registered. PASS |
| TC1 Create Multiple OCP | A1 › Add OCP › Create Multiple, Circuit Breaker ×2 › Save Changes | "QZ1AN-Circuit Breaker 1/2" have AFDC. PASS |
| TC2.1–2 | A1 asset page › Connections › New Loadside Connection → target G1, method Cable, source terminal Bottom › Create | Cable "FDR-QA-DEMO ZP-5104 17Nov G1" is listed in the WO Assets tab with AFDC ("AF 15 remaining"). PASS |
| TC2.3 | WO › SLD tab › search the cable | The cable is drawn in full colour straight away. The neighbouring asset that is not in the WO (TX-B 208V) is grey. PASS |
| TC3.1 | Home › network button › **Go Offline** (the app's own offline mode; the Mac network was not touched) → New Asset "O1 offline" | Manage Services shows AFDC at once, under the "Offline — changes apply now and sync later" banner. 3 changes are queued: Asset CREATE, Service Registration CREATE, Asset-Session Link CREATE. PASS |
| TC3.2 | Go Online → sync | The queue reached Pending 0 with no errors (History 13). On the server, O1 has AFDC. PASS (see the note below) |
| Web read-back | /sessions/2dcb3c4f… (web) and GET /api/ir_session/{id}/service-registry | See below. PASS |

## Web read-back (same WO)
- **Registry:** service-registry by_node has 16 nodes, all with service d625cfa0 (Arc Flash Data Collection, the WO's work_type_id).
  - They include the cable, O1 offline, the linked panel and its 7 breakers, the Quick Count breaker, the walkthrough breaker, C1, QZ1AN 1/2 and A1.
  - G1 (added while the type was General) has no service, as expected.
- **Web WO page:** Assets 16, "0 of 16 arc-flash ready".
  - The Assets tab lists the cable and O1 with an Arc Flash cell. G1 has no Arc Flash cell.
  - The SLD tab opens.
- **Web parity (web twin ZP-5108, RTR):** on the same WO, web › Actions › Add New Asset "QA-DEMO ZP-5104 17Nov W1 web" (Panelboard) → service-registry shows W1 with AFDC (17 nodes). Same behaviour as iOS.

## Notes, not defects
- **G1 keeps no service after the type change.** It was added while the WO was General, and changing the type later does not add AFDC to it. Kush's TC4 only says the next asset gets the new type, so this is expected as written. Web shows the same (no Arc Flash cell for G1).
- **Sync after Go Online did not start for 2+ minutes.** The 3 queued changes stayed pending until I tapped Settings › Sync Queue Analyzer › Flush Queue Now. Then all 3 synced with no errors.
  - This was the in-app offline toggle on a simulator, not a real network drop (there is no airplane mode on the simulator).
  - I did not wait longer than about 2½ minutes, so I am not filing it.
  - Worth a check on a device with real airplane mode.
- **App hang (not reproduced).** At 15:32–15:37 the app's main thread hung in SwiftUI layout while automation long-pressed and scrolled the "Assets in Room" list with a panel expanded. WDA timed out, and iOS later killed the app in the background (0x8BADF00D watchdog). The crash report is Z Platform-QA-2026-11-17-153747.ips in ~/Library/Logs/DiagnosticReports. It may be caused by automation; I am only noting it.

## Not covered
- Real camera capture in Photo Walkthrough (the simulator has no camera; I used a Gallery photo).
- Real airplane mode (I used the app's own Go Offline).
- A busway connection (I created a cable).

## Cross-platform
- **Web:** PASS.
  - The iOS-made assets, the cable and the offline asset show in the WO on web with the service.
  - A web-added asset on the same WO gets the service too (ZP-5108, web twin, RTR).
- **Android:** ZP-5114 (Android twin) is **Done** (Android v1.39). Not re-run by me.

## MOBILE-CHECKS (Android 1.40, if the lead wants a re-check of the Done twin)
1. On a WO with Work Type = General, add an asset → Manage Services shows no service.
2. Change the Work Type to Arc Flash Data Collection, then add an asset by each path: New Asset, Link Existing, Quick Count, Photo Walkthrough, Create Child, Create Multiple OCP. Each gets the service.
3. On an asset in the WO › New Connection › Cable → the cable is listed in the WO Assets with the service, and the WO SLD shows it in full colour.
4. Offline: add an asset → the service shows at once; online → web shows it in the WO with the service.

## Evidence (this folder)
- `QA-17Nov-ZP-5104-every-add-path-cable-offline-general.mp4` (81 s, step captions).
- `QA-17Nov-ZP-5104-1-general-type-new-asset-no-service.png` (PASS, expected).
- `QA-17Nov-ZP-5104-2-afdc-type-new-asset-service-registered.png` (PASS, expected).
- `QA-17Nov-ZP-5104-3-new-cable-in-wo-service-registered.png` (PASS, expected).
- `QA-17Nov-ZP-5104-4-wo-sld-new-cable-full-colour.png` (PASS, expected).
- `QA-17Nov-ZP-5104-5-offline-new-asset-service-at-once.png` (PASS, expected).
- `QA-17Nov-ZP-5104-6-web-wo-assets-cable-and-offline-asset.png`: web read-back. Green = has the service; amber = G1, added under General, no service (expected).
- `QA-17Nov-ZP-5104-web-wo-sld.png`, `QA-17Nov-ZP-5104-web-add-new-asset-after.png`: web parity.

## Test data left (QA-DEMO, EG-ACME, (s) Wild Goose Brewery, room Test)
- The WO "Work Order - Nov 17, 2:26 PM", active.
- Assets: QA-DEMO ZP-5104 17Nov G1, A1 new, C1 child, O1 offline, W1 web; QZ1AN-Circuit Breaker 1/2; two "Circuit Breaker 1" (Quick Count and walkthrough); cable FDR-QA-DEMO ZP-5104 17Nov G1.
- The QA-DEMO 4 Nov ZP-5171 panel and its breakers were linked into the WO.
- Nothing was deleted.
