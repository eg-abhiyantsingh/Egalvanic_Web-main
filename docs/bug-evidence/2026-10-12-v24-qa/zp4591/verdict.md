# ZP-4591 — UI Task #81: Show new connections on unwired terminals immediately; re-block bus-to-bus direct connections

**Verdict:** PASS (web).
**Build:** QA `index-C-Cc1qD1.js` (checked live 16:40 and 17:06 IST, 13 Oct).
**Tested:** 13 Oct 2026, 16:45–17:05 IST, acme.qa.egalvanic.ai, Project Manager seat; Super Admin, Electrical Engineer, Client Portal for menus and API.
**Test data (mine):** site "QA-DEMO ZP-4800 SLD canvas v2.4 (delete me)" (`363d5350-…`). New assets: "QA-DEMO 4591 SRC CB (delete me)", "QA-DEMO 4799 JBOX / TIE / ATS / XFMR3W / RECLASS (delete me)", plus fixture assets "QA PNL2/PNL3/SWBD1/PARENT-OK/CHILD-OK/G-Meter" made by QA on 12 Oct.
**Ticket re-read live:** In QA, 0 comments. Also live on prod per the ticket (#1629 hotfix); prod not touched.
**Flags:** none of its own. It lives in the GoJS editor (`feature-gojs-sld` live ON). SIMULATED OFF → older editor (shot 4800-8).

## Checked

| Check | How | Result |
|---|---|---|
| Connect onto an unwired terminal, no refresh | Unlock → double-click SRC CB → double-click JBOX → pick "Bottom" → "Left In" → "Connect directly" | PASS. The link is attached to `bottom-source-0` → `left-target-0` ports straight away (not the node body). The dot shows (shot 1). `POST /edge/create` 201 carries both terminal ids. |
| Same with other classes | Meter → Tie Breaker "Left In"; SRC CB → Switchboard "TIE-IN-L" (my reclassified asset) | PASS. Snapped to `left-target-0` each time. |
| Parent ↔ child edge | PARENT-OK (group) → its child CHILD-OK, "Connect directly" | PASS with a note. The child's top dot shows at once. Before reload the parent end attaches to the group's hidden bottom port (drawn as a small hook). After reload it uses the `child:<id>` bus port (straight line). Shots 2 and 4. |
| Bus-to-bus "Connect directly" blocked | PNL2 → PNL3; SWBD1 → my new CB (feeding a bus) | PASS. "Connect directly" is disabled, with the tooltip "A direct connection here would leave a bus-to-bus chain with no impedance — exported models can't ship that way." `POST /edge/preview-rules` → `has_bus_to_bus_no_blocker:true`, `reason:"bus_to_bus_no_blocker"` (shot 3). Insert Cable/Busway stay enabled. |
| Control: allowed direct connect | SRC CB → JBOX, Meter → TIE | PASS. Preview `allow_direct:true`. Blocker-to-blocker (ATS → XFMR) inserts a bus node automatically, as before. |
| Negative: no duplicate/orphan terminals | Compare saved `node_terminals` before/after connecting, then reload | PASS. Rows unchanged (8 / 4 / 3 / 3 / 2). After reload every link uses the same ports. |
| Already-wired terminals | Connections from SWBD1 (2 wired on Bottom) | PASS. Load and connect as before. |
| Roles | Client Portal `PUT /edge/update` | 422 permission_denied (correct). Client Portal `/slds` = Access Denied. Super Admin has the editor. |
| Performance | Network | Preview call 200–400 ms, create 201 < 1 s, one preview per connect attempt. SLD load: 34 API calls, `/sld/{id}/graph` 925 ms, duplicates only `translations ×4`, `plug-config ×2`. |

## Bugs found
None in scope.

## Out-of-scope bugs (see ZP-4800 verdict for full steps)
- An Electrical Engineer **not assigned** to the site can still change its connections through `PUT /api/edge/update/{id}` (200, saved), although reading the site is refused (422). Same for `PUT /api/node/update/{id}` from a Project Manager on an unassigned site (200).

## Observations
- The backend now creates **every** orientation terminal when an asset is created or re-classified. So on QA a truly "unwired, row-less" terminal no longer exists, and the old "edge on node body" case can only be reproduced with old data. In a **SIMULATED** run (graph data stripped to 1 saved terminal) the Connect dialog did not offer the missing terminals, and the edge went to `top-target-0` on the node body (shot 4799-9). See the ZP-4799 verdict.
- The backend still accepts an edge to a handle the class does not have (`top-target-0` on a Tie Breaker → 201). It is drawn on the body (shot 4799-10).

## Enhancement suggestions
1. In the disabled "Connect directly" tooltip, name the fix ("Insert Cable or Busway between the two buses").
2. Backend: reject `/edge/create` with a handle that is not in the class orientation (400), so bad clients can't create body-attached edges.

## Mobile part for Nency
iOS SLD viewer (needs the OTA viewer publish per env): connect onto a terminal that has no connections → dot and edge snap at once, no refresh. Try "Connect directly" between two panelboards → must be blocked. Check that `window.createEdge` / `createEdgeWithTerminals` paths behave the same.

## Not covered
- The prod hotfix (QA only).
- iOS bridge paths (mobile).

## Proposed Jira comment
```
h3. QA 13 Oct, build index-C-Cc1qD1.js (QA): PASS (web)

# New connection onto an unwired terminal (e.g. "Left In" of a junction box): the dot and the line appear right away on the terminal, no refresh needed.
# Parent to child connection also draws right away.
# "Connect directly" between two buses (panelboard to panelboard) is disabled, with the tooltip about the bus-to-bus chain without impedance. Insert Cable/Busway still work.
# No duplicate or orphan terminals: the saved terminals are the same before and after, and connections load the same after a reload.
# Client Portal cannot change connections (422).

!QA-13Oct-ZP-4591-1-PASS-new-edge-snaps-to-port-immediately.png|width=720!
!QA-13Oct-ZP-4591-3-PASS-bus-to-bus-connect-directly-disabled.png|width=720!

*Note:* iOS part not tested (needs OTA viewer publish, sent to Nency).

*Status:* READY TO RELEASE (web).
```

## Recommended status
**READY TO RELEASE** (web part). The iOS part goes to Nency.
