# ZP-5120 [Android] Let Create close the New Location sheet with only an existing building picked

**Verdict: PASS**

- Build: Android Z Platform (QA) **1.40 (54)** (Firebase App Tester, installed 17 Nov 12:46 IST over 1.39 (53)), emulator quacky_test 1080x2400 density 420, portrait.
- Web build at time of check: index-oFAZQ3rK.js (QA, EG-ACME).
- Seat: Super Admin QA seat, Field Technician experience, site **Android Site 2**.
- Date: 17 Nov 2026, ~12:53-13:05 IST.

## What was run
Path: Assets tab > + (Add Assets) > New Asset > Location > Select location > + (Add Location) > New Location sheet.

| # | Row | Result |
|---|-----|--------|
| 1 | Negative: nothing picked, nothing typed | Create is grey; its button node is `enabled="false"`; tapping it does nothing (sheet stays). PASS |
| 2 | Negative: Building Name = three spaces | Create stays disabled. PASS |
| 3 | **Main row: pick ONLY the existing building BD1** (no floor, no room) | Create turns orange (enabled). Tap Create: sheet closes, back on Select Location, no error. Server: BD1 still exists once, floor_count 0 (nothing duplicated or added). PASS |
| 4 | New building only ("QA-DEMO ZP-5120 17 Nov Bldg", floor/room blank; hint "Leave blank to create only the building.") | Created, listed in the tree and on web Locations. PASS |
| 5 | Existing building (QA-DEMO) + new floor name, room blank | Floor "QA-DEMO ZP-5120 17 Nov Floor" created under it. PASS |
| 6 | Existing building + existing floor, room blank (hint "Name a room to add it to this floor.") | Create enabled; sheet closes, nothing duplicated (server: building has 1 floor). PASS |
| 7 | Existing building + existing floor + new room "QA-DEMO ZP-5120 17 Nov Room" | Room created; the picker returns to New Asset with Location = "QA-DEMO ZP-5120 17 Nov Bldg > QA-DEMO ZP-5120 17 Nov Floor > QA-DEMO ZP-5120 17 Nov Room" (auto-selected). PASS |

API check (web, SA seat): `GET /api/location/sld/aadcee4c-7dd0-45b3-81b9-309c5c166084` -> 200. "BD1" appears once (id 0891e6b9-121c-479b-b476-25c11666b0d2, floor_count 0, room_count 0). "QA-DEMO ZP-5120 17 Nov Bldg" (id 1728512b-0944-4aba-9656-8e95ca256dd8) floor_count 1, room_count 1.

## Not covered
- Offline create (no network) and sync afterwards.
- Other roles (Technician / PM): only the Super Admin seat.
- Tablet size / landscape layout of the sheet.
- iOS twin ZP-4968 is Done; iOS not re-checked in this round.

## Test data (left in place, QA)
- Building "QA-DEMO ZP-5120 17 Nov Bldg" > Floor "QA-DEMO ZP-5120 17 Nov Floor" > Room "QA-DEMO ZP-5120 17 Nov Room", site Android Site 2. No asset was saved (the New Asset form was discarded).

## New defects
None.

## Evidence
- QA-17Nov-ZP-5120-new-location-existing-building.mp4 (41 s, captioned)
- QA-17Nov-ZP-5120-1-empty-create-disabled.png
- QA-17Nov-ZP-5120-2-existing-building-only-create-enabled.png
- QA-17Nov-ZP-5120-3-sheet-closed-back-on-select-location.png

## Cross-platform
- **Web** (index-CkkAeA9o.js): the building, floor and room made on Android read back on web Locations (Android Site 2: "QA-DEMO ZP-5120 17 Nov Bldg" > Floor > Room). BD1 is still single. PASS. Web has no single "New Location" sheet: it adds per level ("Add Floor" on a building). So "Create with only an existing building" has no direct web equivalent (not on web in this form).
- **iOS** (ZP-4968, Done, iOS v1.72, iOS #763): the same sheet fix shipped on iOS. Not re-checked today.
- MOBILE-CHECKS (iOS): New Asset > Location > + > New Location: (1) nothing picked → Create disabled; (2) only the existing building "BD1" → Create closes the sheet, no error, BD1 not duplicated (web Locations shows BD1 once); (3) an existing building + a new floor only → the floor is created.
- Extra evidence: QA-17Nov-ZP-5120-5-web-locations-readback.png
