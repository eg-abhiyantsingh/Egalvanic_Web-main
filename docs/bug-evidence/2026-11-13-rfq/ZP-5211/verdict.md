# ZP-5211 · Power Scheme overlay on the SLD + backfeed walk fix — QA 13 Nov 2026

**Verdict: PASS** on QA, build **index-C6DvLko9.js** (13 Nov 2026). The build changed from `index-CDvWXKDj.js` to `index-C6DvLko9.js` at about 13:25 IST. I diffed the two builds with chunk hashes ignored: `GoJSGraphEditor`, `apiService`, `App` and `completionUtils` are byte-identical. The rebuild only changed the work-order wizard text. All final runs, video and screenshots below are on **C6DvLko9**.

Colours are judged against **ZP-5265's** values, because ZP-5265 later replaced this ticket's purple/orange/blue. The values are UPS `#ea580c`, Generator `#dc2626`, Utility `#000000`, and no source `#9ca3af` (grey).

**Test site:** my own QA-DEMO site, "QA-DEMO ZP-5211 13 Nov power scheme bypass" (`c037d47c-1daf-4684-94bd-3d678e5b2aa9`). It has 23 assets:
- Utility → TX-1 → SWBD-MAIN.
- Generator → CB-GEN → ATS-1 (E). CB-ATS-N → ATS-1 (N).
- ATS-1 → PNL-EMERG → CB-UPS-IN → UPS-1 → CB-UPS-OUT → PNL-CRIT.
- A **closed maintenance bypass**, PNL-EMERG → CB-BYP → PNL-CRIT.
- PNL-CRIT → cable → PNL-CRIT-SUB.
- A tie: UPS-2 → SWBD-B ← CB-TIE ← SWBD-MAIN.
- An orphan panel, plus 2 buses the server added automatically.

## Checked
| Check | How | Result |
|---|---|---|
| Menu item present | SLD › Status toggle ("Show Status Badges") | The menu reads Arc Flash Readiness, Custom Attributes Only, Engineering Status, **Shutdown Rules, Condition Assessment**, Photos, **Power Scheme**, Off. Power Scheme sits right after Photos, which follows Condition Assessment. ✅ (screenshot 1) |
| Border + body wash by source | GoJS BORDER stroke/fill on every node; pixels from the screen | stroke = role colour; fill = colour + `33` (20 % wash); icon background + `33`; data pill + `4d` ✅ (2, 3) |
| Grey = no source | Orphan PNL-ORPHAN: API role `null` | `#9ca3af` stroke, `#9ca3af33` wash ✅ (5) |
| UPS no longer climbs through the bypass | `GET /api/sld/<site>/power-scheme` per role | **UPS 7 · Generator 7 · Utility 8 · none 1**; sources UPS 2 / Gen 1 / Utility 1. UPS appears only on UPS-1 › CB-UPS-OUT › PNL-CRIT › CBL-CRIT › PNL-CRIT-SUB and on UPS-2 › SWBD-B. The **closed bypass CB-BYP = Generator**; PNL-EMERG and ATS-1 = Generator; SWBD-MAIN, TX-1 and CB-ATS-N = Utility. The old walk would have entered CB-BYP and ATS-1 from the load side and painted all 22 fed assets UPS. ✅ (3, card in video) |
| Tie | CB-TIE line on SWBD-MAIN, load on SWBD-B (fed by UPS-2) | CB-TIE = Utility, SWBD-MAIN = Utility, SWBD-B = UPS. UPS did not climb through the tie ✅ (4) |
| Strict-device rule | Breakers, ATS and transformer entered only from the line side | CB-BYP and CB-TIE (load side faces UPS) keep the upstream role; ATS-1 is not UPS ✅ |
| Links: role only when both ends agree | GoJS link strokes | Matching ends are coloured. Mismatched ends are grey: CB-ATS-N→ATS-1, CB-UPS-IN→UPS-1, CB-BYP→PNL-CRIT, CB-TIE→SWBD-B ✅ |
| A cable drawn as an edge takes its own role | Cable CBL-CRIT collapsed into an edge (`node_manips.conductor=hidden`, the same call as the editor's "Hide") | The link PNL-CRIT→PNL-CRIT-SUB carries `pseudoEdgeNodeId = CBL-CRIT` and is drawn `#ea580c` (the cable's role) ✅ (8) |
| Legend | DOM | Power Scheme · UPS · Generator · Utility · Not fed by any source ✅ |
| Legend loading state | power-scheme response held 6 s (SIMULATED) | "Loading..." ✅ |
| Legend unavailable state | power-scheme answers 500 (SIMULATED) | "Power scheme could not be loaded" ✅ (7) |
| Roles re-fetched on every toggle | Network | 1 `GET …/power-scheme` each time the mode is turned on; no duplicate calls ✅ |
| API contract | 306 acme sites scanned read-only | All 200 `{success, data:{scheme:{id:{role,source_ids}\|null}, sources:{UPS,Generator,Utility}, sld_id}}` ✅ |
| Tenant / site scope | PM, AM, CP, EE and FM (`is_eg_admin:false`) against my site (not assigned to them), with a positive control | 422 `permission_denied` for all 5 seats. The positive control is the same id: SA gets 200, and after I assigned the site, PM gets 200 ✅. Another company's site `f86052b7…` gives 422 and a random UUID gives 422. Staff also get 422 on that foreign id from the acme host, so there is no positive control for the cross-company case. |
| Roles (UI) | PM (site assigned) / CP | PM sees the same menu and the overlay ✅. CP has no SLD page (redirected to Maintenance Portal › Site Health), which is by design; the power-scheme API answers 200 for CP on its assigned site |
| Flags | Bundle + LaunchDarkly evalx (Acme) | **No dedicated flag.** The Status menu exists only when `feature-eng-lib` (live ON) is on **or** the user has `photos.view`. The Power Scheme item only needs the GoJS renderer (`feature-gojs-sld`, live ON). See the flag rows below |
| eng-lib OFF (SIMULATED) | evalx rewrite + clientstream blocked | Menu = Arc Flash Readiness, Photos, **Power Scheme**, Off. Shutdown and Condition are hidden, but Power Scheme stays (question below) |
| gojs-sld OFF (SIMULATED) | evalx rewrite | Falls back to React Flow. The shared toolbar still lists Power Scheme, but React Flow has no power-scheme colouring: 0 handlers in the App chunk and no request on click. Inconclusive in the UI because the view did not load; low risk while gojs-sld is ON |
| LaunchDarkly unreachable (SIMULATED) | All `*launchdarkly.com` blocked | React Flow with a plain toggle and no Power Scheme. Fails closed ✅ |
| Performance | Scan of 306 sites | power-scheme p50 280 ms, max 1,545 ms. The largest site ("Performance sld", 8,486 entries) is a 1.0 MB response in 1.5 s; the payload includes `null` entries for unfed nodes (5,420) |
| Real QA data sanity | Sites with a UPS | UPS stays a minority: Performance sld UPS 132 of 3,066 fed; Chicago 106 of 1,715 fed. No site is all-UPS except "K P electronics" (16 UPS sources out of 20 assets, by its own data) |

## Bugs found
None in this ticket's scope.
- JQL run: `project = ZP AND (text ~ "power scheme" OR summary ~ "power scheme")`.
- It found **ZP-5212**, "Power Scheme overlay on the SLD, and the backfeed walk fix that makes its colours correct" (Resolved (migrated)). It looks like a duplicate of this ticket. Worth telling Eric.

## Questions for Eric
1. With `eng-lib` OFF, Shutdown Rules and Condition Assessment are hidden, but Power Scheme stays for anyone with `photos.view`. Is that intended?
2. Power Scheme sits after Photos rather than directly after Condition Assessment. Is that order OK?

## Enhancement suggestions
- **Counts in the legend** (for example "UPS 7 · Generator 7 · Utility 8 · Not fed 1"). Users can then check a site against Koch's drawing without counting boxes.
- **Click a legend row to highlight or filter that role.** The filter already supports Power Scheme, so the legend could reuse it.
- **Make a "mixed" link look different from a "not fed" one.** Both are `#9ca3af` today; a link between a Generator section and a UPS section looks unfed. A dashed grey line would separate them.
- **Utility wash vs no-source wash.** The 20 % black (#c6c7c9) wash reads as grey at a glance, close to the "not fed" grey wash. A lighter Utility wash, or hatching for "not fed", would make them easier to tell apart.
- **Hover tooltip "Fed by UPS-1 (UPS)".** The data is already in `source_ids`.

## Mobile impact
- The iOS 1.72/1.73 embedded SLD bundle (`Flow/assets/index-BK2ZkmBo.js`) has **no** `powerScheme` mode. Web-only, as the ticket says.
- An iOS/Android tester should check that the mobile SLD shows no Power Scheme item or legend.
- The backend walk fix changes the power-scheme roles. Any mobile screen that reads them (power-scheme filters, work-order "Power Scheme" facet) will now show fewer UPS assets on sites with a bypass or tie. That is expected.

## Not covered
- **Koch ICT Campus numbers.** That is prod data; QA only.
- **A before/after on the same data.** The fix is already on QA, so "old walk" behaviour is reasoned from the ticket, not replayed.
- **Real LaunchDarkly flips.** No dedicated flag exists. The OFF states above are SIMULATED.
- **The editor's right-click "Hide" path.** The right-click menu did not open under automation, so I hid the cable with the same API the editor calls.

## Test data and settings changed
- Created my own site "QA-DEMO ZP-5211 13 Nov power scheme bypass" with 21 nodes and 20 edges.
- Assigned this site and "QA-DEMO ZP-5243 13 Nov UA job A looks good" to the **PM, AM and CP test seats** (`POST /api/users/<id>/sld-assignments`) for the role checks.
- No company-level settings changed.

## Evidence
- `QA-13Nov-ZP-5211-video-steps.mp4` (85 s; contact sheet checked).
- Screenshots 1–8.
- `flag-*-SIMULATED.png`.
- Scripts: `.playwright-mcp/rfq13/sld-skm-upload/r01–r04, r08, r10–r12`.

## Recommended status
**READY TO RELEASE.** Eric can answer the questions above separately.
