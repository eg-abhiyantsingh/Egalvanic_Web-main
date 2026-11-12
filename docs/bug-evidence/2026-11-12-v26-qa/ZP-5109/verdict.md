# ZP-5109 — Engineering schedule asset drawer: Approve / Approve & Next + follow a live library link

**Verdict:** PASS on QA, build `index-C1ZRIunF.js` (checked 16:02 and 17:17 IST, 12 Nov 2026). Super Admin seat.

**Recommended status:** READY TO RELEASE.

Test data (created today): site "QA-DEMO ZP-5108 ZP-5109 12 Nov" (e1aeadfa-6b58-484a-acf1-1ef09496b8b9) with 3 new breakers
"QA-DEMO ZP-5109 CB-1/2/3". Page: Engineering › Equipment Designations (https://acme.qa.egalvanic.ai/equipment-designations) with that
site selected.

## Checked
| Check | How | Result |
|---|---|---|
| Drawer has Approve / Approve & Next | Pencil on CB-1 | PASS: footer Cancel · Save Changes · Approve · Approve & Next (on 2 and 5 Nov only Cancel / Save Changes) |
| No library link → Approve blocked | Same, CB-1 not linked | PASS: both disabled, tooltip "Link this asset to a library entry (and save) to approve it" (shot 1) |
| Approve follows a library link made in the drawer | Use library › 3P › ABB › "Emax 2, E1.2, Ekip DIP — LI, 250-1200A" (LIBRARY MATCHED, trip configuration loaded), nothing saved yet | PASS: Approve and Approve & Next become enabled straight away (shot 2) |
| Approve & Next approves and advances | Clicked Approve & Next on CB-1 | PASS: `PUT /node/update/<CB-1> {eqp_lib…skm_oid 318402}` (the link is saved first), then `{engineering_status:"Complete"}`. Toast "QA-DEMO ZP-5109 CB-1 approved". The drawer moves to CB-2 (its open-asset link points to CB-2), where Approve is disabled again (shot 3) |
| Approve approves the asset and closes | CB-2: linked in the drawer, Approve | PASS: same two writes, toast "CB-2 approved", drawer closed. List shows CB-1 / CB-2 "Complete" with the designation. Header "Engineering Approved" 0 of 3 → 2 of 3 (67%) (shot 4) |
| Last row | Pencil on CB-3 (no next asset) | PASS: only Approve is offered, no Approve & Next (shot 5) |
| Cross-check with ZP-5108 | The WO on the same site | The work order's Engineering Approved ring shows the same 2 of 3 |
| Other schedules | Bundle: the same drawer and approval wiring is used for all, sccr, feeder, ocpd, transformer (Short-Circuit Ratings, Feeder Schedule, OCPD Settings, Transformer Schedule) | Run on Equipment Designations ("all") only |
| Roles | `approval` is passed only when the user holds `nodes.manage` | ADMIN, PM, AM, EE and FM hold `nodes.manage` (from `/auth/v2/me`); CP does not, so it gets no Approve buttons. Not walked in the UI per role |
| Flags | No ticket flag (designations pages run under the existing engineering flags, all ON) | n/a |

## Bugs found
None.

## Enhancement suggestions
- Show the asset's name in the drawer header ("Edit Asset — QA-DEMO ZP-5109 CB-2"). After Approve & Next, the only clue to which asset
  is open is the toast about the previous one; the drawer itself shows only engineering fields.
- After the last asset is approved through Approve & Next, a short "All assets on this page reviewed" note would help.

## Questions / observations (not filed)
- The Account Manager seat holds `nodes.manage` (so it would see Approve), although its role description says "view-only SLD access".
  This is RBAC matrix drift seen before (memory: project_rbac_*). Raising it as a question, not a bug of this ticket.

## Mobile impact
None directly (web engineering schedules). Approved status (`engineering_status: Complete`) should show on the iOS asset if the app
displays it.

## Not covered
The other four schedule pages (same drawer code) and a per-role UI walk.

## Evidence
- `QA-12Nov-ZP-5109-video-steps.mp4` (83 s): steps 1–6 with captions (contact sheet checked; the last card was trimmed because its
  read-back line used the wrong endpoint).
- `QA-12Nov-ZP-5109-1-approve-disabled-until-library-link.png`, `-2-library-linked-in-drawer-approve-enabled.png`,
  `-3-approve-and-next-advanced-to-cb2.png`, `-4-both-approved-header-updated.png`, `-5-last-asset-no-approve-and-next.png` (all PASS).
