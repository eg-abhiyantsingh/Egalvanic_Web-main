# ZP-4614: Joining a work order no longer auto-registers unchosen services (Task #96)

**Verdict: PASS** on QA. Build `index-D1IWZ6SI.js`, re-checked at 19:15 IST. It replaced `index-BdXGlAq5.js` during the session; only two reporting files changed between them (ConfigBuilderModal, ReportConfigPreviewPane), so this ticket's code is the same.
The fix is backend only (eg-pz-backend #1457) and has no LaunchDarkly flag. I tested all of it against the live QA API with the Project Manager seat, then checked the UI on the same work order.

Test data, all mine and labelled: work order **QA-DEMO ZP-4614 link adds no services (delete me)**
https://acme.qa.egalvanic.ai/sessions/3f682025-6e77-4176-8461-68d8c27d36fc (site "Abhiyant test 12").
It was created with the service Clean, Tighten, Torque (CTT) on one panelboard. I then explicitly added Infrared Thermography (IR) to that panelboard, so the work order performs CTT + IR.
Every ATS used below matches CTT's rules (`/implementation-methods/evaluate-for-node` returns "Standard Clean, Tighten, Torque" for each). The old `register_added_assets` hook would therefore have registered CTT on them. This is the positive control that shows the "no services" result is meaningful.

## Checked

| Check | How | Result |
|---|---|---|
| Creation spec is unchanged | `POST /ir_session/create` with `work_type_id`=CTT and scope = panelboard A | PASS: A gets CTT (materializer still runs) |
| Web asset picker / create drawer path | `POST /mapping/node-session/create` (the call LinkExistingChildDialog, the create drawer and QuickCreateChildDialog make) for ATS 00789 | PASS: member, **no services, no method lines** |
| Bulk link (Quick Count / copy path) | `POST /mapping/node-session/bulk-create` for ATS 6 | PASS: member, no services |
| Mobile-style queued link (no `X-Direct-Write`) | `POST /mapping/node-session/create` for Motor Starter 15, waited 12 s | PASS: member, no services |
| `/add-assets` without `service_id` | ATS 1: response `method_lines:0, nodes_linked:1, unmatched_node_ids:[ATS 1]` | PASS: membership only, **no fallback to the work type (CTT)** |
| Positive path: `/add-assets` with service | ATS 1 (second one) + CTT | PASS: `method_lines:1`, CTT registered |
| Positive path: `/method-lines` | ATS 82 + CTT methods | PASS: registered, 3 form instances minted |
| Unlink removes method lines | `POST /mapping/node-session/bulk-remove` for A and ATS 00789 | PASS: A's CTT and IR lines are gone; IR drops off the work order's service list |
| Relink adds no services | `node-session/create` again for A (new mapping) and ATS 00789 (same mapping id 800abf31 reactivated = relink path) | PASS: both members, no services came back |
| Forms only where chosen | `GET /eg-form-instance/by-session/{id}/count` → 7, Forms column shows forms only on ATS 82 | PASS |
| UI | WO Assets tab: linked assets show no forms; right-click → Manage Services (screens 2 and 3) | PASS |
| Roles | Write flow run as Project Manager (customer seat, `workorders.manage`). The other roles do not add assets to work orders | PASS for PM |
| Tenant | Cross-company ids: on 12 Oct even the staff seat gets HTML (no JSON) for the old foreign WO `aed4636d…` and the foreign site is 422, so there is no valid positive control | NOT PROVEN (see Not covered) |
| Performance | add-assets 0.3–1.2 s, node-session create < 1 s, service-registry/method-lines < 0.6 s | OK |

Full API log: 8 states captured after each step (members, registered services and method lines per asset).

## Bugs found
None.

## Enhancement suggestions
1. Add a "Services" column (or service chips) to the work-order Assets grid. Today the only way to see that a just-linked asset carries no services is right-click → Manage Services, or an empty Forms cell.
2. After linking an existing asset (or relinking one), show a one-line hint such as "This asset was added with no services. Add a service?", with a link to Add Service. Users who were used to services arriving automatically will otherwise think the link is broken.
3. The comment in SessionDetail.jsx (line ~972, "The asset is in the work order and carries its services already") is now stale after this change. The Set Up Forms sheet should say services must be chosen (frontend wording, Krunal).

## Mobile part for Nency
iOS #682 is the mobile half. Please check:
- the post-create prompt: unticked services must stay unregistered;
- map-in / relink of an asset into a work order adds no services;
- unlink on the device removes the asset's method lines and its unsubmitted forms.

The QA work order above can be used; it is on site "Abhiyant test 12".

## Not covered
- Cross-company `add-assets` / `method-lines` refusal: there is no other-company work order on QA that the staff seat can read as JSON today, so a refusal cannot be told apart from "not found".
- The UI create-drawer save itself: I used the same endpoint the drawer calls, not the drawer.

## Proposed Jira comment
```
h3. QA result: PASS (QA, build index-COeCBn0z.js, 13 Oct; first passed on index-D1IWZ6SI.js, 12 Oct)

Tested on QA work order *QA-DEMO ZP-4614 link adds no services (delete me)* (site Abhiyant test 12), which performs Clean, Tighten, Torque and Infrared Thermography. Every asset below matches those services' rules, so the old hook would have registered them.

||Step||Result||
|Link an asset (node-session/create, the asset picker / create drawer call)|member, no services, no method lines|
|Bulk link (node-session/bulk-create)|member, no services|
|Queued (mobile-style) link|member, no services|
|/add-assets with no service_id|membership only, no fallback to the work type|
|/add-assets with a service, and /method-lines|service registered, forms minted|
|Unlink|method lines removed|
|Relink after unlink|no services come back|

Re-checked 13 Oct on index-COeCBn0z.js with one fresh link: ATS *Hello hello* (Clean, Tighten, Torque's rules match it) was linked to the same work order. After 10 s it is a member with *no services, no method lines and no forms*. ATS 82 (CTT added on purpose) still has CTT and its 3 forms.

# Open https://acme.qa.egalvanic.ai/sessions/3f682025-6e77-4176-8461-68d8c27d36fc
# Assets tab: Hello hello is listed (1-9 of 9) with an empty Forms cell.
# Right-click Hello hello: View Full Asset, Add Service, Add Procedure, Add Issue. *No Manage Services*, because that item only appears when the asset has registered services.
# Right-click ATS 82 (control): the menu also shows Manage Services and Open Forms (3).

!QA-13Oct-ZP-4614-1-fresh-link-no-services-no-forms-PASS.png|width=720!
!QA-13Oct-ZP-4614-2-fresh-link-menu-no-manage-services-PASS.png|width=720!

Not covered: cross-company refusal (no other-company work order on QA can be read even by staff today). iOS #682 is with Nency.
```

## Recommended status
**READY TO RELEASE**

## Re-check 13 Oct on index-COeCBn0z.js
**Result: still PASS. Nothing changed in behaviour.** Checked at 13:45 IST as the **Project Manager** seat (`is_eg_admin:false`), signed in through the real login form. The work order is the same QA-DEMO one: **QA-DEMO ZP-4614 link adds no services (delete me)**
https://acme.qa.egalvanic.ai/sessions/3f682025-6e77-4176-8461-68d8c27d36fc (site "Abhiyant test 12").

| Check | How | Result |
|---|---|---|
| Positive control still valid | `GET /implementation-methods/evaluate-for-node/{ATS 82}?service_id=CTT` | "Standard Clean, Tighten, Torque": the rules still match ATS-class assets |
| Fresh link of an existing asset | `POST /mapping/node-session/create` (the call the asset picker / create drawer makes) for **ATS "Hello hello"** (`3e07488c…`, which CTT's rules match), then waited 10 s (writes are async) | PASS: 201. The asset is a member, with **no registered services, no method lines**. The work order's services stay ["Clean, Tighten, Torque"], method lines stay 2, form instances stay 7 |
| Explicit service still kept | ATS 82 after the link | Still CTT + "Standard Clean, Tighten, Torque" |
| UI grid | Assets tab | "Hello hello · ATS" listed, 1–9 of 9, empty Forms cell (screen 1) |
| UI context menu | Right-click Hello hello vs ATS 82 | Hello hello: View Full Asset, Add Service, Add Procedure, Add Issue (**no Manage Services**, screen 2). ATS 82: …, **Manage Services**, …, Open Forms (3), Copy Form Data To…. In the code, "Manage Services" appears only when the asset has registered services, so its absence is the UI proof |
| Extra link (by mistake, harmless) | My first pick, "ATS 4 g", is really a **DC Battery** (CTT does not apply to it) | Linked, member, no services. It stays on my QA-DEMO work order (9 assets now: 7 from 12 Oct + Hello hello + ATS 4 g) |

Note: the 12 Oct Jira comment draft referenced screenshots `-2-linked-asset-manage-services-empty` and `-3-explicit-asset-manage-services-ctt`, but those files were never in this folder (only `-1` exists). The comment above now uses the two 13 Oct screenshots instead.
Also, "right-click › Manage Services: no services" in the old steps cannot happen: for an asset with no services the menu does not offer Manage Services at all. The steps are corrected above.

New screenshots: `QA-13Oct-ZP-4614-1-fresh-link-no-services-no-forms-PASS.png`, `QA-13Oct-ZP-4614-2-fresh-link-menu-no-manage-services-PASS.png`.
Recommended status unchanged: **READY TO RELEASE**.
