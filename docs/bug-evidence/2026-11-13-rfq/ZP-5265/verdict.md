# ZP-5265 · Power Scheme colours: Utility black, UPS orange, Generator red — QA 13 Nov 2026

**Verdict: PASS** on QA, build **index-C6DvLko9.js**. The constants are identical in `index-CDvWXKDj.js`, the earlier build of the day: the `GoJSGraphEditor` chunk is byte-identical once chunk hashes are ignored.

**Test site:** my own QA-DEMO SLD from ZP-5211, "QA-DEMO ZP-5211 13 Nov power scheme bypass". It has Utility-only, Generator, UPS, UPS+Generator (through the closed bypass), a tie and an orphan.

## Checked
| Check | How | Result |
|---|---|---|
| Constants in the shipped bundle | `GoJSGraphEditor-*.js` | `{UPS:"#ea580c", Generator:"#dc2626", Utility:"#000000"}`; no-source `#9ca3af`; order `["UPS","Generator","Utility"]` ✅ |
| Legend swatches | `getComputedStyle` on the legend | UPS `rgb(234,88,12)`, Generator `rgb(220,38,38)`, Utility `rgb(0,0,0)`, Not fed by any source `rgb(156,163,175)` ✅ (screenshot 1) |
| Node strokes (GoJS) | BORDER.stroke on every node | Utility nodes `#000000`; Generator nodes `#dc2626`; UPS nodes `#ea580c`; orphan `#9ca3af` ✅ |
| Node strokes (screen pixels) | Screenshot PNG decoded at the border, zoomed in | UTIL-1, TX-1, SWBD-MAIN, CB-ATS-N **#000000**; GEN-1, CB-GEN, ATS-1, PNL-EMERG, CB-BYP **#dc2626**; UPS-1, CB-UPS-OUT, PNL-CRIT **#ea580c**; PNL-ORPHAN **#9ca3af**. Exact matches ✅ (2, 3, 4) |
| Body tint | GoJS fill + pixels | Fill = colour + `33` (20 %). On screen: Utility #c6c7c9, Generator #f2cfd1, UPS #f5d9cb, none #e5e8ec ✅ |
| Link strokes | GoJS link shapes | `#000000`, `#dc2626` or `#ea580c` when both ends share a role, `#9ca3af` when the ends differ. The cable-as-edge link is `#ea580c` (its own role) ✅ (5) |
| A node fed by UPS **and** Generator stays orange | PNL-CRIT is fed by UPS-1 and by the closed bypass from the Generator panel | API role `UPS` (source_ids = UPS-1 only); stroke `#ea580c` ✅ (3). Priority is unchanged: ATS-1 (Utility N + Generator E) is `Generator` / `#dc2626` |
| Grey is unchanged | Orphan | `#9ca3af` ✅ (4) |
| Flags | Same gates as ZP-5211: Status menu needs `feature-eng-lib` or `photos.view`; GoJS needs `feature-gojs-sld` (both live ON for Acme) | No colour-specific flag. OFF states are SIMULATED in ZP-5211's evidence |
| Roles | PM seat (site assigned) | Same colours ✅ |
| Performance | Constants only | No extra requests |

## Bugs found
None.
- JQL run: `project = ZP AND (text ~ "power scheme" OR summary ~ "power scheme")`. Nothing open about the colours.

## Enhancement suggestions
- **Utility vs "not fed".** The Utility wash (black at 20 % = #c6c7c9 grey) and the "not fed" wash (#e5e8ec) are both grey at normal zoom; only the border (black vs light grey) separates them. A lighter Utility wash, or hatching for "not fed", would help technicians reading against Koch's drawings.
- **Mixed-role links.** Mixed-role links use the same grey as "not fed". Use a dashed grey so "two sources meet here" does not read as "dead".

## Mobile impact
None. The overlay is web-only: the iOS 1.72/1.73 embedded SLD bundle has no `powerScheme` mode. A mobile tester only needs to confirm the iOS/Android SLD has no Power Scheme item.

## Not covered
- **Koch's own reference drawings.** Not available on QA. The values are checked against the ticket's hex codes.

## Evidence
- `QA-13Nov-ZP-5265-video-steps.mp4` (80 s; contact sheet checked).
- Screenshots 1–5.
- Pixel and GoJS measurements: `.playwright-mcp/rfq13/sld-skm-upload/r12-out.json`.

## Recommended status
**READY TO RELEASE**
