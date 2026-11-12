# ZP-4947 — Task #204: SLD views honour empty overrides, seed node manips, scope bus offsets and cascade stray cables

**Verdict:** PARTLY — QA, Web v2.6, build `index-C1ZRIunF.js`, 12 Nov 2026 16:47–17:07 IST, Super Admin seat. The empty-override and
add-to-view fixes work (5 Nov failures are fixed); **an inserted cable does not reach the other views that hold both its ends** (row 6 fails,
re-proved twice); the remove-from-view cascade works from the node menu but not from the toolbar Delete dialog.

**Test data (own):** site "QA-DEMO v2.6 SLD editing 12 Nov" (`c4438cd3-…`); views "QA-DEMO ZP-4947 view A 12 Nov" (`08fda987-…`),
"… view B 12 Nov" (`ec1b10f6-…`) (made with the same calls the View menu uses) and "… view C 12 Nov" (`0a2a2835-…`, made in the UI:
Add to… › Add to View › Create & Add). All Nodes: FUSE nosub → Load-1 hidden, CB-2 → PNL-2 one elbow.

## Checked
| Check | How | Result |
|---|---|---|
| Add-to-view copies a non-empty hide (5 Nov FAIL) | UI: select FUSE nosub + Load-1 › Add to View › Create & Add; and via API for view A | PASS — `nodes/add` → `edge_mappings_created` 2 (5 Nov: 0); view graph `edge_manips {"hidden":true}`; view A draws FUSE → Load-1 as pills |
| Clearing a hide in a view saves an empty override | view A › right-click the hidden FUSE → Load-1 › Show Connection | PASS — `PUT /api/sld-view/{view}/edges/{edge}/points` with `"edge_manips":{}` (5 Nov: null) |
| Cleared hide survives reload (no fallback to All Nodes) | reload, reopen view A | PASS — FUSE → Load-1 drawn as a normal line in view A |
| Negative: All Nodes unchanged | All Nodes after the view edits | PASS — FUSE → Load-1 still `{"hidden":true}`, CB-2 → PNL-2 elbow [[324,899]] |
| Per-view Simplify (supporting) | view B › Select Entire Run › Simplify Edge | writes `PUT /api/sld-view/{view}/node-manips` `{"conductor":"hidden"}` per view, not on the node |
| Remove a node from a view → stray cable cascades (node menu) | view A › right-click FUSE nosub › Remove from View › Remove | PASS — `nodes/remove {node_ids:[FUSE nosub, FDR-QA26 Load-1]}`: the cable left with one in-view neighbour went too |
| Same via toolbar Delete › "Delete or Remove?" › Remove from View | view B: PNL-1 (CBL-3 left with 1 neighbour); CB-1 (hidden CBL-2 left with 1 neighbour) | **FAIL (alternate path)** — `nodes/remove` sends only the selected node; CBL-3 and the hidden CBL-2 stay in view B (Bug 2) |
| Inserted cable reaches other views holding both ends | view A › right-click XFMR-1 → PNL-3 › Insert Cable; open view B (holds XFMR-1 + PNL-3) | **FAIL** — Insert Cable builds XFMR-1 → node bus → FDR-QA26 PNL-3 → PNL-3 and adds both new nodes to view A only; view B (15 min later) shows XFMR-1 and PNL-3 **unconnected**, its graph has neither new node (Bug 1) |
| Re-proof of the above | add FUSE nosub + Load-1 to view B, Insert Cable on FUSE → Load-1 in view A | same: view B shows FUSE nosub and Load-1 unconnected; chain FUSE → FDR-QA26 Load-1 → bus → Load-1 only in view A |
| Bus-off on an enclosure in a view; removing the last elbow in a view; label-drag seeding; fast view switching | — | not covered (time; see below) |
| Console | runs | no app errors |

## Bugs found
**Bug 1 — A cable inserted in one view does not reach other views that hold both of its ends; those views show the ends unconnected** — Medium
1. Site with two views that both contain XFMR-1 and PNL-3 (here view A and view B).
2. In view A, unlock, right-click the XFMR-1 → PNL-3 connection › Insert Cable.
3. Open view B (waited 15 min).
- Actual: Insert Cable (`POST /api/edge/resolve-impedance-insertion`, then `nodes/add` to view A) creates a chain XFMR-1 → node bus
  "QA26 XFMR-1-FDR-QA26 PNL-3-BUS" → cable "FDR-QA26 PNL-3" → PNL-3. View B gets neither node and draws XFMR-1 and PNL-3 with no
  connection — the exact symptom the ticket says is fixed. Re-proved on FUSE nosub → Load-1 (16:55 and 17:01 IST, same seat, same steps).
- Likely cause (for the developer): the pass-through rule places a Node Bus / default-impedance node into views holding **two** of its
  neighbours; with a cable + node bus chain each new node has only one neighbour in view B, so neither is placed.
- Expected: view B shows XFMR-1 → (bus) → cable → PNL-3, at the All Nodes offset.
- Evidence: `QA-12Nov-ZP-4947-4-view-a-cable-inserted-xfmr-pnl3.png`, `QA-12Nov-ZP-4947-5-view-b-xfmr-pnl3-unconnected.png`,
  `QA-12Nov-ZP-4947-7-view-a-cable-inserted-fuse-load.png`, `QA-12Nov-ZP-4947-8-view-b-after-second-insert.png`.
- Suggested assignee: Eric (ticket owner; backend `view_pass_through`) — in-scope of this ticket.
- JQL run: `project = ZP AND created >= -90d AND (text ~ "Insert Cable" OR text ~ "pass-through" OR text ~ "view_pass_through" OR text ~ "Remove from View")` → no ticket covers it.

**Bug 2 (minor, alternate path) — toolbar Delete › "Remove from View" does not cascade the stranded cable** — Low
1. In a view, select a node next to a cable (PNL-1 next to CBL-3; CB-1 next to the hidden CBL-2).
2. Toolbar Delete › "Delete or Remove?" › Remove from View.
- Actual: `POST /api/sld-view/{view}/nodes/remove {node_ids:[<node>]}` only; the cable stays with one neighbour (re-proved twice, 17:01 and 17:03).
- Expected: same as the node menu path, which sends `[node, cable]` (verified 17:06).
- Evidence: `QA-12Nov-ZP-4947-6-view-b-toolbar-remove-pnl1-cable-left.png`, `QA-12Nov-ZP-4947-9-view-b-remove-cb1-hidden-cable.png`,
  `QA-12Nov-ZP-4947-10-view-a-context-menu-remove-cascades-cable.png` (the working path).
- Suggested assignee: Krunal (frontend) — or Eric.

## Enhancement suggestions
- After Insert Cable, tell the user which other views were updated ("also added to 2 views"), so a miss is visible.
- The "Delete or Remove?" dialog and the node-menu "Remove from View" should share one code path.

## Mobile impact
src/sld changed (OTA): on iOS/Android open a view where a hide was cleared on web → it must stay shown; and check views after an
Insert Cable done on mobile (ZP-4945 iOS / ZP-4946 Android).

## Not covered
Bus-off on an enclosure in a view, removing the last elbow in a view (the fixed edge renders as two chain segments; my selection did
not reach "Convert to Calculated"), label-drag seeding of node manips, fast view switching (group bleed).

## Proposed Jira comment
See `ZP-4947.txt`.

## Recommended status
Stay **In QA** with Bug 1 in the comment (an in-scope row fails; the rest pass) — the owner may move it to To Do if Bug 1 is confirmed as in scope.
