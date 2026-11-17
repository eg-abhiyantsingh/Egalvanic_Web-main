# ZP-3021 Android: Equipment Library selection while creating OCP assets

**Verdict: PASS (main purpose) + 1 gap filed as a new bug (BUG-pole-count)**

- Build: Android Z Platform (QA) **1.40 (54)**, emulator 1080x2400, density 420, portrait. Web QA build at check time: index-oFAZQ3rK.js at start, **index-CFMAmUdK.js** from ~13:30 IST (server read-backs below were done on CFMAmUdK).
- Seat: Super Admin QA seat, EG-ACME (engineering library on), site Android Site 2. 17 Nov 2026, 13:15-14:05 IST.
- Spec used: Abhiyant's comments 46126 and 46198 (fields, Pole Count, Create Multiple rules), Rajat's questions 46120/46197.

## What was run
| # | Spec row | Result |
|---|----------|--------|
| 1 | "Create a Main Breaker?" (New Asset > Panelboard > Detailed > Engineering > Mains Type = MCB) has the picker | Engineering card with **Use library** switch, off by default. On: Manufacturer, then "N possible matches" + "Search matches…" list. After a pick: green "Library Matched" card with **Unlink**. PASS |
| 2 | Create New Child (from the OCP section of Create Asset AND from a saved asset's page OCP > Add Child Node) | Same Use library card + **Pole Count** (1P/2P/3P). PASS |
| 3 | Create Multiple: one pick for the batch + "Applies to N" line + Pole/Fuse Count | "Applies to 1 breaker" / "Applies to 3 breakers" / "Applies to 2 fuses" updates with Quantity; one library pick + one Pole Count for the batch; Fuse class shows **Fuse Count** (1/2/3). PASS |
| 4 | Optional: switch off = created without a link | "QA-DEMO ZP-3021 17 Nov Child CB3 no library" created with the switch off: server `eqp_lib` empty, pole_count 1. PASS |
| 5 | Unlink | Unlink on the card returns to the match list; re-pick works. PASS |
| 6 | Link saved the same as the Engineering section (asset's Engineering opens with the device linked) | Opened MCB / CB1 / Multi 1-3 / CB2 / Fuse 1-2 on Android: each Engineering section shows "Library Matched" with the picked device and the picked Pole/Fuse Count. Server read-back (`GET /api/nodes/sld/aadcee4c-…`) shows `eqp_lib` + `manufacturer` + `pole_count` on every one (table below). PASS |
| 7 | Mixed batch (breakers + fuses) | Not reachable: Create Multiple takes ONE asset class per batch, so a batch cannot mix breakers and fuses. The "Applies to" line always equals the quantity. Not a defect; noted. |
| 8 | **Pole Count narrows the matching devices** (46198 point 2) | **Does not narrow.** Square D: 550 matches with 1P, 2P or 3P; QO search: 19 matches incl. 1-, 2- and 3-Pole whichever pole is picked. Same in Create a Main Breaker?, Create New Child, Create Multiple **and in the existing asset Engineering section (control)**, so it is not a regression from this ticket but the spec line is not met. **New bug: BUG-pole-count.txt** |
| 9 | Offline create | During the Panel C run the app dropped to offline ("Network connection is unstable… saved offline", emulator Wi-Fi flap). After Go Online > Sync 13 records, Panel C, its MCB and both C-Multi breakers reached the server with their `eqp_lib`. PASS (bonus) |

Server read-back (17 Nov ~13:55 IST):

| Asset | pole_count | eqp_lib |
|---|---|---|
| QA-DEMO ZP-3021 17 Nov Panel B MCB (e46e80d4-df27-4df5-87df-5540163cea82) | 3 | SCHNEIDER/SQUARE D / J-Frame, Powerpact / 150-250A, UL (item 203134) |
| …Child CB1 (371a5471-a0c3-4019-81db-6b9694fb2e88) | 1 | SCHNEIDER/SQUARE D / QO, 1-Pole / 15-70A |
| …Child CB2 asset page (9294456b-47f6-4369-8a6f-485c1c138357) | 2 | SCHNEIDER/SQUARE D / QO, 2-Pole / 15-125A |
| …Child CB3 no library (30248abc-0157-415b-868b-58a2fd6be56c) | 1 | none |
| …Multi 1/2/3 (0bfb27ec…, d80ac34c…, 7a43d860…) | 3 | SCHNEIDER/SQUARE D / QO, 3-Pole / 15-100A |
| …Fuse 1/2 (1acece01…, cb61d435…) | 3 | BUSSMANN / LPJ, 600V Class J / 1-600A |
| …Panel C MCB (e2a2f74f-5009-4884-bc40-47c28efc7f8d) | 3 | SCHNEIDER/SQUARE D / J-Frame, Powerpact / 150-250A, UL |
| …C-Multi 1/2 (8b41ac6e…, 7d92e310…) | 3 | SCHNEIDER/SQUARE D / QO, 3-Pole / 15-100A |

## Not covered
- A company with the engineering library turned OFF (picker must be hidden). EG-ACME has it on; turning it off is a company-wide setting, not changed.
- "Skip" and "Cancel" in the Create a Main Breaker? dialog. Tablet / landscape.
- Web/iOS: web ZP-3019 and iOS ZP-3020 are To Do (no counterpart to compare).

## New defect
- BUG-pole-count.txt: Pole Count (Fuse Count) does not narrow the matching library devices, in all three new pickers and in the existing Engineering section. Video BUG-pole-count-does-not-narrow-library-matches.mp4 + 4 stills.

## Observations
- Create Multiple's default Name Prefix is the parent's initials ("QZ1NPB-Circuit Breaker"); typed over with QA-DEMO names.
- In the asset Engineering section the Manufacturer picker also offers "Not in the list? Use what you typed" (custom manufacturer, ZP-5127). The three new pickers only list library manufacturers (correct for library matching).

## Test data (QA, left in place)
Room QA-DEMO ZP-5120 17 Nov Room. Panels "QA-DEMO ZP-3021 17 Nov Panel A" (no children), "Panel B" (MCB, CB1, CB2 asset page, CB3 no library, Multi 1-3, Fuse 1-2), "Panel C" (MCB, C-Multi 1-2). Nothing unsaved was kept (the pole-check child was discarded).

## Evidence
- QA-17Nov-ZP-3021-library-picker-create-main-and-multiple.mp4 (84 s)
- QA-17Nov-ZP-3021-1-create-main-use-library-off.png, -2-create-main-device-linked-unlink.png, -3-create-multiple-applies-to-3-breakers-linked.png, -4-saved-main-engineering-linked.png
- BUG-pole-count-does-not-narrow-library-matches.mp4 (48 s) + BUG-pole-count-1..4 PNGs

## Cross-platform
- **Web** (index-CkkAeA9o.js): the links made on Android read back on web. Panel C MCB Engineering shows MANUFACTURER SCHNEIDER/SQUARE D, TYPE Thermal Magnetic, POLES 3P (QA-17Nov-ZP-3021-5-web-panel-c-mcb-library-linked.png). The web picker on OCP create (ZP-3019) is To Do, so it is not on web yet.
- **iOS** (ZP-3020, To Do, iOS v1.76): not built yet, so not on iOS.
- MOBILE-CHECKS (iOS, when ZP-3020 is built): Create a Main Breaker? / Create New Child / Create Multiple: Use library switch (off by default) → Manufacturer → search list → linked card with Unlink; Pole/Fuse Count on Child/Multiple; "Applies to N breakers/fuses"; the saved asset's Engineering opens linked (web/API eqp_lib set). Also check whether Pole Count narrows the list there (on Android it doesn't, see BUG-pole-count).
