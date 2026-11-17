# ZP-5197 [Android] Create Asset screen has no section links (add the links the asset page has)

**Verdict: PASS**

- Build: Android Z Platform (QA) **1.40 (54)**, emulator quacky_test 1080x2400, density 420, portrait. Web QA build index-oFAZQ3rK.js.
- Seat: Super Admin QA seat, site Android Site 2. 17 Nov 2026, ~13:06-13:15 IST.
- Fix: eg-pz-mobile-android PR #603 (merged to dev 11 Nov, per Dharmesh's comment).

## What was run
Assets tab > + (Add Assets) > New Asset.

| # | Row | Result |
|---|-----|--------|
| 1 | Quick mode, no class: section links shown | "Asset sections" row: Basic, Photos, Engineering, Infrared; Basic highlighted. PASS |
| 2 | Quick: tap Photos / Infrared / Basic | Each jumps to its section and highlights. PASS |
| 3 | Detailed + class Circuit Breaker: links | Basic, Photos, Engineering, Commercial, Connections, Infrared, Notes (">" arrow slides the row). PASS |
| 4 | Detailed: tap each of the 7 links | Basic > Asset Details, Photos > Asset Photos, Engineering > Engineering, Commercial > Commercial, Connections > Connections, Infrared and Notes > end of the form (sections visible; they are the last ones, so the form cannot scroll them higher). PASS |
| 5 | Current section highlighted while scrolling | Quick: Basic > Photos > Infrared. Detailed: Basic > Photos > Engineering > Commercial; the link row slides to keep the highlighted link in view. PASS |
| 6 | Same links as the asset page | Existing asset "29 july abhiyant asset" (Circuit Breaker): Basic, Photos, Engineering, Issues, Maintenance, Connections, Commercial, Notes, in the same chip style. New Asset has the same chips minus Issues and Maintenance, which need a saved record. PASS |

## Observations (minor, not defects; for the dev to judge)
1. Quick mode has no Engineering header, but it still shows an "Engineering" link. Tapping it lands on the "Trust the Photos" card near the end of the form. In Detailed, the Engineering link also lands with "Trust the Photos" just above the Engineering header, so the card seems to be counted as part of Engineering.
2. Connections and Infrared (Detailed) are never highlighted while scrolling by hand. When the form reaches the bottom, the highlight jumps from Commercial to Notes. Tapping those links highlights them correctly.
3. The ">" (More sections) arrow covers the right edge of the last visible link. A tap there hits the arrow and slides the row instead of opening that section. This happened once in the recorded run with "Notes"; tapping "Notes" fully in view works (checked in a separate run).

## Not covered
- Tablet layout: the ticket says field tablets show a side list. Only phone width (density 420, portrait) was checked here.
- Landscape. Other asset classes beyond Circuit Breaker / none.
- iOS twin ZP-5196 (READY TO RELEASE) not re-checked.

## Test data
Nothing saved; the New Asset form was discarded each time. Asset page compared: "29 july abhiyant asset" (site Android Site 2).

## New defects
None.

## Evidence
- QA-17Nov-ZP-5197-create-asset-section-links.mp4 (61 s, captioned)
- QA-17Nov-ZP-5197-1-new-asset-quick-section-links.png
- QA-17Nov-ZP-5197-2-detailed-engineering-link.png
- QA-17Nov-ZP-5197-3-detailed-connections-link.png
- QA-17Nov-ZP-5197-4-scroll-highlight-follows.png

## Cross-platform
- **Web** (index-CkkAeA9o.js): Assets > Create Asset opens "Add Asset" with a left section list: All, Basic Info, Engineering, Condition, Notes. So web already has section links. The section sets differ by platform (web: Basic Info / Engineering / Condition / Notes; Android: Basic / Photos / Engineering / (Commercial / Connections / OCP) / Infrared / Notes). Web also puts "Trust the Photos" inside Engineering, which matches Android's grouping (so observation 1 above is consistent with web). Parity: same idea, different layout (web list vs. Android chip row).
- **iOS** (ZP-5196, READY TO RELEASE, iOS v1.76): not checked by me.
- MOBILE-CHECKS (iOS): New Asset (Quick and Detailed, class Circuit Breaker): a section link row/list is shown; each link jumps to its section; the highlight follows scrolling; compare the section set with Android (Basic, Photos, Engineering, Commercial, Connections, Infrared, Notes).
- Extra evidence: QA-17Nov-ZP-5197-5-web-create-asset-form.png
