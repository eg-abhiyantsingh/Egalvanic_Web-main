# ZP-4803 — Tasks #162-164: SLD walks ignore hidden edges, Reset Terminal Positions, combine connections, rotated-node drag + PDF parity

**Verdict:** PASS on the canvas (walks, collapse, collapsed-parent drag, Reset, Combine/Split, rotated-node drop, drop onto a combined terminal). **PDF parity is not proven.** The 180-degree dual case cannot be tested on QA data (no dual asset). The combined-terminal PDF was identical before and after Combine, and on my cluttered test layout I cannot tell whether that is right.
**Build:** QA `index-C-Cc1qD1.js` (live 16:40 and 17:06 IST, 13 Oct).
**Tested:** 13 Oct 2026, 16:45–17:15 IST, Project Manager (main), Super Admin (menus), Client Portal (API).
**Test data (mine):** site "QA-DEMO ZP-4800 SLD canvas v2.4 (delete me)" (`363d5350-…`). Assets "QA-DEMO 4591 SRC CB", "QA-DEMO 4799 JBOX / XFMR3W (delete me)", fixture "QA SWBD1/CB1/CB2". Hidden connection SRC CB → JBOX (`30fcae76`).
**Ticket re-read live:** In QA, 0 comments.
**Flags:** GoJS editor only (`feature-gojs-sld` live ON; SIMULATED OFF → older editor, see ZP-4800 shot 8).

## Checked

| Check | How | Result |
|---|---|---|
| Select Downstream / Upstream / Lineage skip hidden edges | Control before hiding: F on SRC CB → {SRC CB, JBOX}; ⇧F on JBOX → {JBOX, SRC CB}. Then hide the link and repeat | PASS. After hiding: F → {SRC CB}; ⇧F → {JBOX}; ⇧⌥F → {SRC CB}. After Show Connection, F includes JBOX again. |
| Collapse / expand | C on SRC CB (only link to JBOX hidden) | PASS. JBOX stays visible (shot 1). |
| Drag a collapsed parent | Collapse SRC CB, drag it +120/+90 | PASS. JBOX (linked only by the hidden connection) did not move. Assets linked by visible connections moved with it, as expected (`batch-positions`) (shot 7). |
| Straighten / Magic Layout | S and M on SRC CB | PASS. JBOX not moved; no position writes. |
| Electrical tracing unaffected | Trace Lineage toolbar from SRC CB | PASS. JBOX still highlighted through the hidden link (shot 2). |
| Hand-drag a terminal | Select JBOX; drag its connected "Left In" handle to the top edge at 70% | PASS. Lands 8 px from the drop point (port sits inside the border; same on every drop). `PUT /node/update` saves `node_manips.terminals["left-target-0"].slots[0]={side:"top",off:0.71}` (shot 3). |
| Drop on a rotated node | Rotate JBOX right (90°), drag the same terminal to the visual right edge, then to the bottom edge | PASS. Lands 8 px from the drop point both times. The saved side is un-rotated (visual right → stored "top", visual bottom → stored "right") (shot 4). The 3-winding transformer has no Rotate item (not rotatable). |
| 180-degree dual (e.g. "REF1 ATS"), no Reverse item | Node menus of ATS, JBOX, SWBD1, XFMR3W, Super Admin seat too | No "Reverse" item anywhere (PASS for that part). No dual asset exists on QA, so "lands where dropped on a 180° dual" and "rotate skips duals" are **not tested**. |
| Reset Terminal Positions | Node menu on JBOX and XFMR3W after dragging | PASS. Item appears only when manips exist. `node_manips.terminals:null`; the dot returns to its default (left) spot (shot 5). Rotation reset separately (Rotate Left). |
| Reset keeps the combined flag | SWBD1: Combine "Bottom", drag Top, Reset | PASS. After Reset: `{terminals:{"bottom-source-0":{merged:true}}}` (slots gone, merged kept). |
| Combine / Split | SWBD1 "Combine connections on 'Bottom'" (2 connections) | PASS. One dot (shot 6 / 8); Split restores 2 (`bottom-source-0`, `bottom-source-0#1`). Only offered on terminals with 2+ connections. |
| Drop onto a combined terminal | Drag SWBD1's Top handle onto the combined Bottom dot | PASS. No error in the console; saved `top-target-0` slot on "bottom". |
| PDF parity (merged = one dot) | Export PDF with SWBD1 split, then combined (merged flag verified saved via API first) | **Inconclusive.** Both PDFs are pixel-identical. Around SWBD1, other fixture assets overlap (CB1/CB2/ORPH-CB at the same spot, from 12 Oct tests), so I can't tell whether the split PDF already drew one dot. Same with SRC CB (3 connections): one exit in both. |
| Roles | Client Portal `PUT /node/update` | 422 permission_denied (correct). Super Admin has Reset/Combine/Rotate in the node menu. |
| Site scope (pre-existing) | Project Manager `PUT /node/update` on an asset of an unassigned site (no-op body) | 200. See the ZP-4800 out-of-scope bug (same family as `edge/update`). |
| Performance | Network | Each action = one `PUT /node/update` (200–600 ms). Collapse = one `batch-collapse-states`. No slow calls. |

## Bugs found
None proven in scope.

## Out-of-scope
- Writes on unassigned sites are accepted by `PUT /node/update` / `PUT /edge/update` (full steps in the ZP-4800 verdict).

## Enhancement suggestions
1. Show the saved terminal side as a small label while dragging (e.g. "Top 70%"), so the drop target is clear on rotated nodes.
2. "Reset Terminal Positions" could also be offered from multi-select, for many hand-dragged assets at once.

## Mobile part for Nency
SLD viewer (OTA): on iOS, open my QA-DEMO site. After a web Combine on SWBD1 "Bottom", the viewer must show one dot. A hidden connection must not be followed by any iOS "select downstream" action. A rotated JBOX with a moved terminal must show it on the same side as on web.

## Not covered
- 180-degree dual nodes (no dual on QA).
- PDF parity, decisive version: needs a clean SLD with a combined terminal and no overlapping assets. Dev should share a known-good example, or QA should build one on a fresh QA-DEMO site.

## Proposed Jira comment
```
h3. QA 13 Oct, build index-C-Cc1qD1.js (QA): PASS on canvas, PDF/dual part not proven

# Select Downstream / Upstream / Lineage, Collapse, and dragging a collapsed parent no longer follow a hidden connection. Trace Lineage still does.
# Dragging a terminal works, also on a rotated asset: it lands where dropped.
# Reset Terminal Positions puts the terminals back and keeps "combined".
# Combine connections makes one dot; Split brings the separate dots back. Dropping a terminal onto a combined dot gives no error.
# No "Reverse" item in any node menu.

!QA-13Oct-ZP-4803-1-PASS-collapse-ignores-hidden-edge.png|width=720!
!QA-13Oct-ZP-4803-4-PASS-terminal-drop-on-rotated-node.png|width=720!
!QA-13Oct-ZP-4803-6-PASS-combine-connections-one-dot.png|width=720!

*Not proven:* the 180-degree dual case (no dual asset on QA), and the PDF "merged terminal = one dot". The PDF looked the same before and after Combine, but my test layout has overlapping assets, so I can't call it. Eric, can you point to a site with a dual (e.g. REF1 ATS) on QA?

*Status:* stay In QA until the dual + PDF rows are checked.
```

## Recommended status
**Stay In QA.** The canvas behaviour all passes. Two QA Review rows (180° dual drop, PDF parity for duals/merged terminals) still need suitable data. Not To Do: nothing failed.
