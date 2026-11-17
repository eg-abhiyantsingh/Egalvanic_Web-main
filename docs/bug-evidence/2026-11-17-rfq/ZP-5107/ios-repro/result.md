# ZP-5107 / ZP-4942: iOS assembly placement repro, 17 Nov 2026

**Result: reproduced. iOS loses BOTH the hidden flag and the elbow.**

| Check | iOS copy IOS17 | Web copy L2 (control) | Android copy AND17 |
|---|---|---|---|
| FUSE nosub → Load-1 hidden kept? | **N** (edge_manips null) | Y (`{"hidden":true}`) | Y (`{"hidden":true}`) |
| CB-2 → PNL-2 elbow kept? | **N** (edge_manips null) | Y (`{"elbows":[[1035,1322]]}`) | Y (`{"elbows":[[904,1781]]}`) |

- Build: Z Platform-QA 1.75 simulator build (binary 16 Nov 06:49 IST, from the owner), lane A (iPhone 17, iOS 26.2).
- Seat: SA (EG-ACME). Site "QA-DEMO v2.6 SLD editing 12 Nov", SLD c4438cd3-59b3-4639-9e60-2752dc5aa478.
- Placed at 17 Nov 16:16 IST (10:46:13 UTC): SLD › unlock › Assembly › "QA4942-FDR-{name}" › name IOS17 › Place › tap an empty spot.

## API values (GET /api/sld/c4438cd3-59b3-4639-9e60-2752dc5aa478, read a minute after placing)
- "QA4942-FDR-IOS17 FUSE nosub" → "QA4942-FDR-IOS17 Load-1": edge 1e121b41-82da-4fc3-859a-40cd01df1e30, `edge_manips: null`.
- "QA4942-FDR-IOS17 CB-2" → "QA4942-FDR-IOS17 PNL-2": edge c0a96fbd-6076-4351-9811-87181b4a076a, `edge_manips: null`.
- All 4 assets were created (CB-2, FUSE nosub, Load-1, PNL-2), and both connections exist.
- Full dump: `api-sld-c4438cd3-copies.json` (IOS17, L2, AND17).

## Device before and after a refresh
1. Right after placing, the device draws FUSE nosub → Load-1 as a hidden connection: you see the "QA4942-FDR-IOS17 Load-1 →" badge, no line. This is the local copy.
2. After the SLD Refresh button (the device reloads the server copy), the same connection is drawn as a normal vertical line.
   So the user sees the drawing change on their own device, and every other device and web get the server copy.

## Name pre-fill (for ZP-5264 / ZP-5318)
- On the same SLD, iOS pre-filled the Name as **AND18**, the next number after the Android tester's AND17.
- It did not pre-fill "L1 CB-3" (Android ZP-5318) or "L3" (the server's place-preview).
- AND18 is a free name, so it is not a defect. It does differ from the server preview.
- When the field is cleared, the placeholder shows "L1".

## Evidence (this folder)
- `QA-17Nov-ZP-5107-ios-repro-IOS17-hidden-and-elbow-lost.mp4`: 50 s with step captions. It shows the placement, the badge right after placing, and the normal line after the refresh.
- `QA-17Nov-ZP-5107-ios-1-right-after-placing-hidden-badge.png`: outline boxes on the hidden-connection badge and the FUSE node.
- `QA-17Nov-ZP-5107-ios-2-after-sld-refresh-normal-line.png`: outline box on the line that replaced the badge.
- `api-sld-c4438cd3-copies.json`: the API values for IOS17, L2 and AND17.
- No web screenshot of IOS17 was taken. The API values above are what web draws from.

## Test data left
- Assembly copy "QA4942-FDR-IOS17" (4 assets, 2 connections) on the site above. Nothing was deleted.
