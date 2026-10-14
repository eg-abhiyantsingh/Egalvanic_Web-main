# ZP-4565 — Maintenance Portal nav gated to Portal Sales only; Site Data > Maintenance rides the LD company flag

**Verdict:** PASS — build `index-BdXGlAq5.js` → rebuilt `index-D1IWZ6SI.js` mid-run (same code; only the build-id string differs, main bundle byte-identical after hash-normalisation, ReportingConfigEditor chunk the only app diff — unrelated to this ticket). Live flag `feature-maintenance-portal` = **ON (v25)** for Acme QA.

## Checked
| Check | How | Result |
|---|---|---|
| Nav rail: Maintenance Portal section | 9-seat sweep, flag ON (real) | Shows for every T2/portal seat **and** every T1 staff-role seat that holds `maintenance_portal.manage` (Super Admin, Admin, PM, AM, FM, CP). **Hidden for EE** (no `maintenance_portal.manage`, 84 perms) and for Technician/Portal-Sales (no web). PASS |
| Negative: Super Admin without Portal Sales | Super Admin seat holds the perm, not the role | Section still shows because the v2.4 gate is `requiresTier2: !canSeeMaintenancePortal` where `canSeeMaintenancePortal = T2 OR maintenance_portal.manage` — matches the ticket's "who-you-are only" design. PASS |
| Site Data > Maintenance (Program/Compliance/Reports) | flag ON vs simulated OFF, every seat | ON → enabled for seats with `features.assets.view` (PM/AM/EE/FM); pure Admin does not list them (no assets.view) ✓. OFF → the 3 rows show **disabled with tooltip "Maintenance isn't enabled for your company. Contact your admin to enable."** ✓ |
| Typed route `/maintenance/program|compliance|reports` | flag ON/OFF | ON → FeatureGate admits (staff/assets.view); admin without assets.view → **Access Denied** (route needs assets.view) ✓. OFF → **"Feature Not Available"** page ✓. Menu and route agree. |
| Typed route `/maintenance-portal/*` | flag ON/OFF, every seat | ON → opens for portal-capable seats; **EE → Access Denied** (no perm, matches hidden nav) ✓. OFF → **"Feature Not Available"** ✓ |
| Flag OFF (SIMULATED via evalx rewrite) | `feature-maintenance-portal:false` | Portal section disappears from the rail for ALL seats; Site Data > Maintenance disabled with tooltip; typed URLs → "Feature Not Available". Menu == route in both states. |
| API | `/api/features/access`, `/api/features/sync`, `/api/entitlements` | All 200; the frontend no longer reads a DB entitlement row — `hasMaintenancePortal` is LD-only, matching the ticket. |
| Tech / Portal-Sales seats | typed URLs | **Web Access Restricted** (no `platform.web`) — expected. |
| Performance | per-load request counts | No dup storms; `features/access` + `features/sync` + `entitlements` once each per seat. |

Nav vs route agreement holds in both flag states and for all 9 seats. Menu-hidden == route-denied everywhere I checked.

## Bugs found
None for this ticket. The one small subtlety — **EE (Electrical Engineer) does NOT see Maintenance Portal** — is correct for THIS ticket's design (gate = T2 OR `maintenance_portal.manage`, and the QA EE seat has neither). Note this is the *older* Eric-9/8 design; if the product rule from [[project_portal_sales_entitlement]] ("Maintenance Portal for every role except Technician", ZP-4703) is still the intended behaviour, EE being hidden would be a **product-rule regression**, but that is a question for the owner, not a defect in ZP-4565's stated spec. Flagged for the owner's call.

## Enhancement suggestions
- The disabled "Maintenance" tooltip ("Maintenance isn't enabled…") and the typed-URL "Feature Not Available" use two different copy strings for the same flag-off state — unify the message so a user sees one consistent reason.

## Mobile part for Nency
iOS/Android reads the same `feature-maintenance-portal` + `maintenance_portal.manage`. She should confirm the mobile Maintenance Portal tab hides when the company flag is OFF and when the seat lacks the perm, and that the EE seat behaves the same as web (no portal).

## Not covered
Real flag FLIP to OFF (I only simulated — the live Acme flag is ON and I must not flip it). RTR must not rest on a simulated OFF per [[feedback_qa_miss_checklist]] #1.

## Flag flip needed
**flag `feature-maintenance-portal`, env QA, company Acme (d59d449b…), set ON→OFF then back ON** — to confirm the real OFF state (rail hides, Site Data > Maintenance disabled, routes "Feature Not Available") on a controlled switch before READY TO RELEASE. Ask Shubham in eg-internal-dev.

## Proposed Jira comment
h3. QA — ZP-4565 PASS (QA build index-BdXGlAq5.js, flag feature-maintenance-portal ON)

Swept all 9 seats with the LaunchDarkly flag ON (real) and OFF (*simulated* via evalx rewrite).

*Maintenance Portal rail section* — now who-you-are only (T2 seat OR {{maintenance_portal.manage}}): shows for Super Admin, Admin, PM, AM, FM, CP; hidden for EE (lacks the perm) and Tech/Portal-Sales (no web). Menu and {{/maintenance-portal/*}} route agree for every seat.

*Site Data > Maintenance* (Program / Compliance / Reports) — lists for seats with {{features.assets.view}} (PM/AM/EE/FM), not a pure Admin. Flag ON → enabled; flag OFF → disabled with tooltip, and the routes show "Feature Not Available". Menu == route in both states.

!QA-12Oct-ZP-4565-32-pm-flagOFF-site-data-menu.png|width=720!
!QA-12Oct-ZP-4565-76-cp-flagOFF-typed-maintenance-portal-overview.png|width=720!

*Still needed before RTR:* a controlled real OFF→ON flip of {{feature-maintenance-portal}} on QA/Acme (only simulated so far). Open question for the owner: EE is hidden from the portal here — is that intended, or should the ZP-4703 "all roles except Technician" rule apply?

## Recommended status
**Stay In QA** — PASS on everything observable, but RTR needs a real flag flip (not a simulated OFF) and an owner decision on the EE visibility rule.

## UPDATE 13 Oct 2026: real flag switches, PASS → READY TO RELEASE
- Build `index-COeCBn0z.js`. Shubham switched `feature-maintenance-portal` (QA, Acme): OFF 12:57 IST, ON 13:06 IST. All 9 seats were swept in each of ON-before, OFF and ON-after; evidence is in `real-flip/`.
- ON (both times): portal shows for SA, Admin, PM, AM, FM, CP; EE hidden, and typed portal URLs return Access Denied. Site Data › Maintenance opens for SA, PM, AM, EE; not listed for Admin (no assets.view) or for FM/CP (T2, `staffOnly`).
- OFF: portal hidden for every seat; Maintenance items locked with the tooltip; every typed URL shows "Feature Not Available".
- The ticket text ("Portal Sales only", "no requiresFlag") is superseded:
  - ZP-4572/4703: gate is `requiresTier2: !W("maintenance_portal.manage")`, rule "every role except Technician".
  - ZP-4732, commits 1f388dd3 and 4eb191a6 (8 Oct): `requiresFlag: be` plus URL gate. Confirmed in the bundle and in git history.
- QA data gap: the EE role lacks `maintenance_portal.manage`. Stage got the grant on 7 Oct (ZP-4703). Noted on the ticket.
- Jira: comment 45108 (3 screenshots) from abhiyant.singh@, then transition → READY TO RELEASE.
