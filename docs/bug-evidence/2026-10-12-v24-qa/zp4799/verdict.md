# ZP-4799 — UI SLD: placeholder dot per orientation terminal on unwired nodes

**Verdict:** PASS on what QA data can show, with one caveat (below).
**Build:** QA `index-C-Cc1qD1.js` (live 16:40 and 17:06 IST, 13 Oct). The placeholder code (`mock:!0` from `node_orientation.orientation_terminals`) is in `GoJSGraphEditor-D7_Hh4Ae.js`.
**Tested:** 13 Oct 2026, 16:45–17:05 IST, Project Manager seat, my site "QA-DEMO ZP-4800 SLD canvas v2.4 (delete me)" (`363d5350-…`).
**Test data (mine):** new never-connected assets "QA-DEMO 4799 JBOX (delete me)" (Junction Box, 8 terminals), "… TIE" (Tie Breaker, 4), "… ATS" (3), "… XFMR3W" (3), "… RECLASS" (Circuit Breaker re-classified to Switchboard, 7).
**Ticket re-read live:** In QA, 0 comments.
**Flags:** none of its own. GoJS editor only (`feature-gojs-sld` live ON).

**Caveat:** on QA the backend now saves **every** orientation terminal when an asset is created (8/8 rows for the new Junction Box). It does the same when the class changes (CB→Switchboard got 7 rows). A read-only scan of the older 107-asset site "Abhiyant test 12" found **0** assets with missing rows. So the "unsaved placeholder" path (`mock:true`) never runs on real QA data. The real-data checks below prove the visible result (one dot per orientation terminal, twins hidden). The placeholder path itself was only exercised **SIMULATED**, by stripping rows from the graph response.

## Checked

| Check | How | Result |
|---|---|---|
| New, never-connected assets show one dot per orientation terminal | Create 4 assets (API, same call the UI uses), open the SLD | PASS. Junction Box: 4 dots (top/bottom/left/right; the 4 coincident LINE/LOAD twins hidden). Tie Breaker: 4. ATS: 3 (two coloured inputs). XFMR 3-winding: 3 (shot 1). |
| Wired terminal hides the unwired ones at the same spot | Wired Switchboard SWBD1; JBOX after connecting into "Left In" | PASS after load: no two visible dots at one spot (shot 2). Right after connecting (same session) JBOX briefly has `left-source-0` and `left-target-0` both visible at the same point; they overlap exactly, so you see 1 dot. After reload only the wired one shows (shot 3). |
| Coincident twins / empty extra slots hidden | Junction Box (8 terminals, 4 coincident pairs) | PASS. Only 4 dots. |
| Connect onto an unwired terminal | SRC CB → JBOX "Left In"; Meter → TIE "Left In" | PASS. Snaps to the terminal; the pick dialog lists the free terminals (shot 11). |
| Placeholders never saved | Saved `node_terminals` before/after opening + connecting; reload | PASS. Opening the SLD sends no writes. Row lists are unchanged; after reload the dots are the same. |
| Simplified nodes and groups unchanged | Group "QA PARENT-OK" | PASS. No placeholder terminals on groups (`mock=0`). |
| **SIMULATED** missing rows | Graph response rewritten so TIE has only 1 saved terminal | Placeholders render: 3 terminals flagged `mock`, all 4 dots visible, no writes on load (shot 8). **But connecting onto it did not use a placeholder:** the Connect dialog skipped terminal picking and the edge was created to `top-target-0` (a handle the Tie Breaker does not have) on the node **body**, with `target_node_terminal_id:null` (shot 9). Simulation only; see observation. |
| Roles | Client Portal `PUT /node/update` | 422 permission_denied (correct). |
| Performance | Network | No extra calls. Placeholders are computed client-side; no writes on load. |

## Bugs found
None proven on real data.

## Observations (claimed under simulation, not filed)
- When an asset genuinely lacks saved terminal rows (older/imported data on other environments), the Connect dialog may not offer the placeholder terminals, and the connection lands on the node body with a non-existent handle. The ticket says "on connect, ensureTerminalPort mints a real terminal and replaces the placeholder". Ask Eric to confirm with a real row-less node on dev. The backend also accepts that edge (201) instead of rejecting an unknown handle (shot 10: how it renders on real data).

## Enhancement suggestions
1. Show the terminal label (e.g. "Left In") as a tooltip on placeholder dots, so users know where a connection will land before entering connect mode.

## Mobile part for Nency
SLD viewer change: needs `scripts/publish-sld-viewer.sh <env>` (OTA) first. On iOS, open the QA-DEMO site above and check that JBOX shows 4 dots, TIE 4, ATS 3, XFMR3W 3. Connect onto a free dot and check the line snaps to it.

## Not covered
- A real asset without saved terminal rows (none exist on QA; simulated only).
- iOS viewer (mobile).

## Proposed Jira comment
```
h3. QA 13 Oct, build index-C-Cc1qD1.js (QA): PASS (web)

# New, never-connected assets show one dot per terminal: Junction Box 4, Tie Breaker 4, ATS 3, 3-winding transformer 3. Overlapping in/out twins stay hidden.
# A wired terminal hides the unwired dot at the same spot (no double dots after load).
# Connecting onto a free terminal snaps to it. Opening the diagram saves nothing, and the saved terminals do not change.
# Groups look the same as before.

!QA-13Oct-ZP-4799-1-PASS-new-unwired-nodes-placeholder-dots.png|width=720!
!QA-13Oct-ZP-4799-2-PASS-wired-node-no-duplicate-dots.png|width=720!

*Note for Eric:* on QA the backend already saves every terminal when an asset is created or its class changes, so the "unsaved placeholder" path never runs on QA data. In a simulated test (terminals removed from the graph response) the dots appeared, but connecting onto one created the line on the node body ("top-target-0", no terminal id) instead of on the placeholder. Please confirm with a real asset that has no saved terminals.
!QA-13Oct-ZP-4799-9-SIMULATED-connect-onto-placeholder.png|width=720!

*Status:* READY TO RELEASE (web). The placeholder-connect note needs a dev check, but it is not reproducible on real QA data.
```

## Recommended status
**READY TO RELEASE** for the web part. Everything reachable on real data passes. The simulated note is for Eric; it is not a proven defect. iOS (OTA) goes to Nency.
