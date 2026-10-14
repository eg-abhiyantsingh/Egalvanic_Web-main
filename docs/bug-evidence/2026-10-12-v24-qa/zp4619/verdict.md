# ZP-4619 — SLD layout ops skip nodes whose parent is off-canvas (orphan children treated as free nodes)

**Verdict:** PASS.
**Build:** QA `index-C14cwR0U.js` (live 17:32 and 18:11 IST, 13 Oct).
**Tested:** 13 Oct 2026, 18:00–18:05 IST, Project Manager. The same result was seen on 12 Oct (`index-D1IWZ6SI.js`).
**Test data (mine):** site "QA-DEMO ZP-4800 SLD canvas v2.4 (delete me)" (`363d5350-…`).
- Orphan: QA ORPH-RELAY (`1c075a57-…`). Its `parent_id` still points at QA ENCL-PARENT (`0e60e6bc-…`), which I deleted on 12 Oct. Downstream: QA ORPH-CB, then QA ORPH-LOAD.
- Control: QA CHILD-OK inside QA PARENT-OK (parent on the canvas), downstream QA CHILD-OK-CB.
**Ticket re-read live:** In QA, 0 comments. Frontend-only (eg-pz-frontend #1645).
**Flags:** GoJS editor (`feature-gojs-sld` live ON). There is no separate flag.

## Checked

| Check | How | Result |
|---|---|---|
| The orphan is drawn as a free asset | Canvas | PASS. QA ORPH-RELAY is not inside a group; it sits at its stored (20,20) (shot 1). |
| Orphan: Straighten Downstream now works | Unlock › right-click QA ORPH-RELAY › Straighten Downstream (shot 2) | PASS. QA ORPH-CB moves from x −1300 to x 20, under the relay (shot 3). One save `PUT /node/update/{id}` `{"x":20,"y":600,"parent_id":null}` 200. It is still there after a reload (DB 20,600). Before the fix nothing moved. |
| Control: a real child still lays out as a child | Same on QA CHILD-OK | PASS. QA CHILD-OK-CB moves to x −1980 (the child's real position). QA CHILD-OK stays inside QA PARENT-OK (shot 4). |
| Negative: a node with no parent at all | Same on QA ORPH-CB | PASS. QA ORPH-LOAD moves from x −1000 to x 20 (shot 5). |
| The menu offers the layout ops on the orphan | Right-click | PASS. Straighten Downstream / Clean / Upstream / Magic Layout are all there. |
| Roles | — | Same editor and permission as any SLD layout op (`nodes.manage`). Client Portal has no SLD. Nothing role-specific in this ticket. |
| Performance | Network | One `PUT` per moved asset (1 here). No extra reads. |

## Bugs found
None.

## Notes (not bugs, already known)
- The orphan still has `parent_id` = the deleted parent. Its right-click menu therefore still offers "Remove from Enclosure", and it is drawn at its old parent-relative (20,20), on top of QA U1. The ticket says the group-delete path that leaves live children behind is a separate follow-up.

## Enhancement suggestions
1. When an enclosure/parent is deleted, clear `parent_id` on its live children (or move them out with their absolute position). They would then not jump to the top-left and overlap other assets. This is the follow-up the ticket mentions; worth its own ticket if none exists.
2. On the canvas, show a small badge on assets whose parent no longer exists, so users can find and fix them (dev counted about 38 on dev).

## Mobile part for Nency
This is a web-editor change only (buildMaps), so there is no mobile code. Still check that iOS draws QA ORPH-RELAY on "QA-DEMO ZP-4800 SLD canvas v2.4 (delete me)" as a free asset, and that an iOS "straighten" on it moves QA ORPH-CB. The same orphan rule should apply there.

## Not covered
The ~38 real orphans on dev (QA has my fixture only).

## Proposed Jira comment
```
h3. QA 13 Oct, build index-C14cwR0U.js (QA): PASS

Setup on my QA-DEMO site: QA ORPH-RELAY still points at a parent I deleted, so it is drawn as a free asset.
# Right-click QA ORPH-RELAY > Straighten Downstream: QA ORPH-CB lines up under it (x -1300 to x 20). It is saved and still there after a reload. Before the fix nothing moved.
# Control: QA CHILD-OK, whose parent is on the canvas, still lays out as a child and stays inside its parent.
# A node with no parent at all (QA ORPH-CB) still straightens its own downstream.

!QA-13Oct-ZP-4619-2-PASS-orphan-right-click-straighten-downstream.png|width=720!
!QA-13Oct-ZP-4619-3-PASS-orphan-straighten-downstream-lines-up.png|width=720!
!QA-13Oct-ZP-4619-4-PASS-control-child-with-parent-still-lays-out-as-child.png|width=720!

*Note:* the orphan still offers "Remove from Enclosure" and keeps its old parent-relative position. That is the group-delete follow-up named in the ticket.

*Status:* moved to READY TO RELEASE
```

## Recommended status
**READY TO RELEASE.**
