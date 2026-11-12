# ZP-5108 — Tasks #227 + #229: WO Details engineering & Topology Errors readiness, arc-flash readiness, work-type auto-add

**Verdict:** PASS on QA, build `index-C1ZRIunF.js` (checked 16:02 and 17:17 IST, 12 Nov 2026). Super Admin seat.

**Recommended status:** READY TO RELEASE. The `feature-skm-export` OFF state was only SIMULATED; a real flip is listed under flags.

Test data (all created today, QA-DEMO): site "QA-DEMO ZP-5108 ZP-5109 12 Nov" (e1aeadfa-6b58-484a-acf1-1ef09496b8b9) with 3 breakers
(ZP-5109), switchboard "QA-DEMO ZP-5108 SWBD-1", panelboard "QA-DEMO ZP-5108 PNL-1" and breaker "SWBD-1 CB-A". Arc Flash work order
"QA-DEMO ZP-5108 12 Nov AF WO", made with the 5-step wizard: https://acme.qa.egalvanic.ai/sessions/c3791ea4-f421-465e-9c16-61b37eecdd6d

## Checked
| Check (QA Review row) | How | Result |
|---|---|---|
| Engineering readiness ring + Topology Errors ring render; Generate Report card works | WO › Details (new first tab) | PASS: rings "Field Data Collected", "Engineering Approved 67% · 2 of 3", "Topology Errors · 0 / No errors" (shot 1). Compact copies of the rings also sit in the WO header (0% · 50% · red "1"). Study File card: "Available once engineering approval reaches 100% (67% now)", Export disabled, Override checkbox. Report card › Generate Report opens the "Generate Report" dialog (config + PDF) (shot 2) |
| Arc-flash data readiness beside the check-off count | Ticked the Arc Flash check-off on CB-3, whose AF data is incomplete | PASS: ring "17% · 1 of 6", under it "0 of 6 arc-flash ready" and "⚠ 1 checked off with incomplete data". The Study File card adds "1 checked-off asset(s) no longer have complete arc flash data." API `service-completion` → `arc_flash {ready:1, data_ready:0, checked_incomplete:1, total:6}` (shot 5) |
| SLD edit → readiness background-refresh, ring subtext centred | Edited the site's SLD (node + edge through the same API calls the editor makes), opened the SLD tab, back to Details | PASS: Topology Errors went "No errors" → "1 error" with no page reload. Subtext ("1 of 6", "2 of 4", "error") is centred under each number (shots 3, 5) |
| Bus containing an OCP with no impedance is flagged | SWBD-1 bus → its breaker CB-A → PNL-1 bus, no cable or busway | PASS: SLD issue panel "Buses connected without impedance — QA-DEMO ZP-5108 SWBD-1 and QA-DEMO ZP-5108 PNL-1 are both buses wired directly together — insert an impedance (cable / busway) between them." (shot 4). Control: before CB-A was attached to the SWBD-1 bus the same direct wire gave "No issues" |
| `exclude_kinds` hides SCCR-less library designations | `GET /api/sld/<site>/library-designations?session_id=<WO>` with and without `&exclude_kinds=sccr` | PASS: total 5 → 3 (the SCCR-kind switchboard and panelboard drop out), approved 2. The Engineering ring uses the excluded count (2 of 3). On 5 Nov the parameter changed nothing (89 = 89) |
| Work type auto-adds when an asset joins | Assets › Actions › Add New Asset › "QA-DEMO ZP-5108 CB-joined 12 Nov" (Circuit Breaker) | PASS: the asset is in the WO and the service registry gives it "Arc Flash Data Collection" with no service picked (registry 5 → 6 assets). Rings re-based to 6 / 4 (shot 6) |
| Connection cables join the WO | — | Not covered: my test site has no cable nodes (see below) |
| AFDC tour | WO ⋮ › "Take the Guided Tour" | PASS: 9-step "Arc flash data collection" walk-through (Details → Assets "tick its Arc Flash check box" → Engineering → SLD "assets outside this work order are greyed out" → progress rings → study file → relaunch from ⋮). The "New to arc flash data collection?" pop-up only shows on an empty AF WO, which matches the code; not seen because the WO had assets |
| Flag `feature-skm-export` (gates the Topology ring + Study File) | Live LD for Acme = ON. SIMULATED OFF (evalx false, stream blocked) and LD unreachable | ON: ring + card shown. SIMULATED OFF: Topology ring and Study File card hidden, Engineering + Field Data rings still shown. LD blocked: same as OFF, so it fails closed (x-SIMULATED shots) |
| Roles / tenancy | `/auth/v2/me` permissions; WO API from customer seats | The Topology ring needs `sld_diagrams.view` (ADMIN/PM/AM/EE/FM have it, CP does not). My new site is not assigned to customer seats, so their `service-completion` call on this WO → 404 (no data leak). The UI was not walked per role |
| Performance | Network over 5 WO loads | 270 calls; every load repeats the app bootstrap once (`auth/v2/me`, `features/access` … ×5 = once per load), no duplicate within a load, no app call over 2 s. Details tab ready in about 8 s on a fresh navigation |

## Bugs found
None.

## Enhancement suggestions
- Make the Topology Errors ring clickable. Open the SLD tab with the issue panel expanded; today clicking it does nothing, and the
  errors are one tab and one chevron away.
- The Assets tab badge counts assets still to collect (6 → 5 after one check-off) while the ring next to it says "1 of 6". Add a tooltip
  ("5 still to collect") so the two numbers don't look like a mismatch.
- Show the compact header rings' labels on hover (Field Data / Engineering / Topology). They appear on every tab as bare "0% · 50% · 1".

## Mobile impact
iOS work-type follow-up is ZP-5104. iOS/Android testers: an asset added to an Arc Flash WO on the device should get the AF service
the same way. The web ring "1 checked off with incomplete data" depends on the device's Arc Flash check-off syncing.

## Flags / real flips needed
- `feature-skm-export`, env QA, company Acme: ON today. A controlled OFF → re-check → ON flip is needed before relying on the OFF
  state (it was simulated here).

## Not covered
- "Connection cables join the work order": needs cable nodes between assets on the site (none on my test site; I did not edit shared
  sites).
- An SLD edit made inside the SLD canvas itself (I used the editor's API calls, then the tab switch triggers the refresh).
- The per-role UI walk.

## Evidence
- `QA-12Nov-ZP-5108-video-steps.mp4` (151 s, played at 1.4×): Details rings → Study File / Report → API data → first SLD edit
  (breaker not yet attached to the bus: still "No errors") → bus–OCP–bus edit → "1 issue" → "1 error" → Add New Asset joins with AF →
  check-off with incomplete data → issue text → Guided Tour.
- Main shots 1–6 (all PASS). Extra: `x-SIMULATED-skm-export-OFF.png`, `x-SIMULATED-launchdarkly-blocked.png`, `x-flag-control-real-ON.png`,
  `x-afdc-tour-in-menu.png`, `x-afdc-tour-step-1.png`, `x-first-edit-ocp-outside-bus-no-error.png`, `x-add-new-asset-form.png`,
  `x-assets-actions-menu.png`.
