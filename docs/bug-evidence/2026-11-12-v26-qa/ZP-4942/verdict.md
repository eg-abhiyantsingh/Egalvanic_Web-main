# ZP-4942 — SLD assemblies (#197), simplified terminals (#198), Select Entire Run (#199), elbows follow moves (#200), Simplify Edge (#201)

**Verdict:** PARTLY — every main flow works on QA (Web v2.6, build `index-C1ZRIunF.js`, 12 Nov 2026, 16:20–16:50 IST, Super Admin + role seats);
one QA-Review row fails (the "next suggestion" name after a placement) and the assemblies flag OFF state is only SIMULATED.

**Test data (own):** site "QA-DEMO v2.6 SLD editing 12 Nov" (SLD `c4438cd3-59b3-4639-9e60-2752dc5aa478`): run CB-1 → CBL-2 → Node Bus → CBL-3 → PNL-1;
CB-2 → PNL-2 (fixed edge, one elbow) in layout group "QA-DEMO ZP-4942 grp 12 Nov"; FUSE nosub → Load-1 (hidden); enclosure SWBD-2 with child
breakers CB-21/CB-22. Assembly "QA4942-FDR-{name}" (`cd981de3-5916-4481-8495-2afb906f0c62`), placed once as QA4942-FDR-L1.
LaunchDarkly `feature-sld-assemblies` = **true** for Acme (live evalx, 16:07 IST).

## Checked
| Check | How | Result |
|---|---|---|
| #199 Select Entire Run, SLD locked | right-click CB-1 → CBL-2 link | PASS — menu shows only "Select Entire Run"; selects the 4 links + CBL-2, Node Bus, CBL-3 |
| #199 unlocked | same | PASS — Select Entire Run · Edit · Hide Connection · Insert Cable · Delete |
| #201 Simplify Edge | run selected › toolbar "Simplify Edge" | PASS — 3× `PUT /api/node/update` `{"node_manips":{"conductor":"hidden"}}` → one calculated edge CB-1 → PNL-1 |
| #201 Convert to Fixed greyed | select the calculated edge, hover | PASS — disabled, tooltip "Unsimplify the edge first — an edge with hidden components can't be fixed yet" |
| #201 Unsimplify Edge | toolbar | PASS — cables/bus revealed, `batch-positions` puts each back on the line, conductor "node" |
| #200 elbows follow | Convert to Fixed (elbow saved), select both ends, drag | PASS — elbow [[269,880]] → [[44,993]] while CB-2/PNL-2 moved (−230,+110); elbow moved (−225,+113) (≤5 px drift, nodes snap to grid) |
| #198 simplified terminals | right-click SWBD-2 › Simplify; right-click › "Split connections on 'Bottom'" | PASS — combined Line/Load ports by default; split → `PUT /api/node/update` `{"node_manips":{"terminals":{"simplified:bottom-source-0":{"merged":false}}}}`, ports bottom-source-0 + #1 |
| #197 Save as Assembly | select CB-2, PNL-2, FUSE nosub, Load-1 › Add to… › Assembly… | PASS — "4 assets · 2 connections · 1 groups", {name} label templates, Edit with AI box, "2 connection(s) to equipment outside the selection will not be included"; `POST /api/assemblies` 201, toast "Saved assembly…" |
| #197 Replace existing | same dialog › Replace existing | PASS — `POST /api/assemblies` 200, toast "Replaced assembly…" |
| #197 Library + thumbnail | Admin › Config › Assemblies, detail page | PASS — grid row (Name, Description, Nodes 4, Edges 2, Classes, Created by, Updated); detail page with Preview; `GET /api/assemblies/{id}/thumbnail` 200 image/svg+xml 19 KB |
| #197 Place with {name} templating | + Assembly › pick › type L1 › Place › click canvas | PASS — "Will create" list (place-preview 200), banner "Click to place QA4942-FDR-L1", 4 nodes "QA4942-FDR-L1 …", edges wired by handle code (bottom-source-0 → top-target-0), elbow carried (anchor-relative), hidden flag kept, group recreated as "QA-DEMO ZP-4942 grp 12 Nov QA4942-FDR-L1" |
| #197 next suggestion | + Assembly again after placing L1 | **FAIL** — suggests "L1 CB-3" (Bug 1) |
| assemblies.manage gating (API) | ADMIN, PM, AM, EE, FM, CP seats | PASS — Admin + PM hold assemblies.manage; AM/EE/FM/CP get 422 `permission_denied … Required: assemblies.manage` on POST /assemblies, ai-templates and DELETE; GET /assemblies 200 for all (reads = slds.view) |
| place gating (nodes.manage) + site scope | place-preview on my site from each seat | PASS — CP (no nodes.manage) 422; PM/AM/EE/FM (no access to my site) 404 "SLD not found"; Admin 200 (positive control) |
| Library page by role | /assemblies typed | Admin list shown; PM, AM → "Access Denied" (see Bug 2 for PM) |
| Fullscreen pickers | Full screen › + Asset | PASS — Add Asset picker drawn above the canvas (z-index 1260) |
| Flag OFF | **SIMULATED** (evalx rewritten, LD stream blocked) | PASS (simulated) — no "+ Assembly", Add to… without "Assembly…", /assemblies → "Feature Not Available", no Assemblies in Admin nav |
| Console | runs | no app errors |

## Bugs found
**Bug 1 — Assembly placement suggests "L1 CB-3" as the next name instead of the next instance (e.g. "L2")** — Low/Medium
1. SLD (unlocked) › + Assembly › "QA4942-FDR-{name}" › name L1 › Place (creates "QA4942-FDR-L1 CB-2", "… PNL-2", "… FUSE nosub", "… Load-1").
2. Open + Assembly again and pick the same assembly.
- Actual: Name box pre-filled "L1 CB-3"; Will create "QA4942-FDR-L1 CB-3 CB-2", "QA4942-FDR-L1 CB-3 PNL-2"… `POST /api/assemblies/{id}/place-preview {"sld_id":…}` returns `"value":"L1 CB-3"` (re-proved twice, 16:35 and 16:40 IST, same seat/input).
- Expected: the next free instance name (e.g. "L2"); member labels must not be parsed as instance names.
- Evidence: `QA-12Nov-ZP-4942-13-next-name-suggestion-L1-CB-3-BUG.png`, `QA-12Nov-ZP-4942-8c-next-free-suggestion.png`.
- Suggested assignee: Avani (backend `place-preview` value) — ticket owner Eric.
- JQL run: `project = ZP AND (summary ~ "assembly" OR summary ~ "assemblies" OR text ~ "next free name" OR text ~ "place-preview") AND created >= -60d` → no ticket covers it.

**Bug 2 — Project Manager holds assemblies.manage but cannot open the Assemblies library** — Low
1. Sign in as PM (has assemblies.manage), open any SLD they can edit (here QA-DEMO ZP-4800…), unlock, click + Assembly.
2. The picker offers "Manage assemblies →"; click it.
- Actual: /assemblies shows "Access Denied" (re-proved twice: typed URL at 16:33 and via the picker link at 16:47 IST). The route is Admin/Super Admin only.
- Expected: PM can open the library (or the link is hidden for PM).
- PM typed `/assemblies` → "Access Denied" (route is Admin/Super Admin only), so a PM can save/replace from the SLD but cannot rename, duplicate or delete an assembly in the UI (API would allow it).
- Evidence: `QA-12Nov-ZP-4942-15-PM-picker-manage-link.png`, `QA-12Nov-ZP-4942-16-PM-manage-assemblies-access-denied-BUG.png`.
- Suggested assignee: Krunal (frontend route guard) — or confirm with Eric that PM should only save/replace.
- JQL run: `project = ZP AND created >= -60d AND (text ~ "layout group" OR text ~ "locked group" OR summary ~ "Assemblies page" OR text ~ "Manage assemblies")` → no bug covers it.

## Enhancement suggestions
- The Save as Assembly default name is "QA{name}" (from the selection's common prefix); suggest the selection's top asset name instead — "QA{name}" is easy to save by mistake.
- Show the thumbnail in the + Assembly picker list (it shows a generic icon; the thumbnail exists).
- After "Replace existing", show which placed copies are now out of date (they don't change by design).

## Mobile impact
iOS/Android get the src/sld viewer changes via OTA: check Select Entire Run (locked), simplified edges with markers, split/combined
terminals and that assembly controls are hidden in the flow-only viewer (ZP-5102 iOS / ZP-5112 Android for placement).

## Not covered
- Real LaunchDarkly OFF (only simulated) — needs a flip.
- Dragging a combined simplified terminal; per-view persistence of #198 (tested in All Nodes only).
- Markers along a simplified edge were seen in the screenshot only, not measured.

## Flags needed
`feature-sld-assemblies`, env QA, company Acme (`d59d449b-09d8-45d6-8f0a-ef70024b1293`): OFF → re-check (no + Assembly / no Admin › Assemblies / API?) → ON.

## Proposed Jira comment
See `ZP-4942.txt`.

## Recommended status
Stay **In QA** until the real `feature-sld-assemblies` OFF/ON check is done; then READY TO RELEASE with Bug 1 and Bug 2 filed separately.
