# SLD v3 — deep bug hunt (release-gate sign-off)

- **Date:** 2026-06-10
- **Tester:** Claude Code (Opus), driven by abhiyant.singh@egalvanic.com
- **Scope:** `https://acme.qa.egalvanic.ai/slds` — SLD v3: list/CRUD, GoJS diagram render (nodes/
  edges/icons/orientations/labels), edit, export, three-layer integrity (UI / browser-local /
  backend), console/network/a11y. Live-driven with Playwright (non-headless), evidence captured.
- **Env facts:** v1.21 web; GoJS-based diagram; 107 SLDs accessible to admin; site **gyu** SLD
  `d5f229cc-61d8-4ecb-95e3-d852cdd57d96` = 6 nodes / 3 edges / complexity_level 2.

> Severity key: **HIGH** = blocks core flow / data loss / crash · **MED** = wrong/garbled output
> or significant UX · **LOW** = cosmetic / third-party / minor.

---

## Confirmed findings

### SLD-BUG-01 (MED) — Node/bus labels overlap & become unreadable in the diagram render
- **What:** On the gyu SLD, the bus node label **"dosconnect switch test"** (large text) renders
  ON TOP OF / overlapping the adjacent **"pole 3"** MCB node label — the two strings collide
  ("…nect switch test" bleeds across "pole 3 / MCB 3P4W"), making both unreadable.
- **Evidence:** `docs/bug-hunts/sld-label-overlap-gyu.png`.
- **Why it matters:** v3 label rendering must keep labels legible & non-overlapping; this is a
  core "render diagrams / labels" defect on a small (6-node) SLD — worse on dense ones.
- **Note:** the on-canvas health badge still reads **"No issues"** → the issue-check does NOT
  detect label collisions (coverage gap, see SLD-BUG-02).

### SLD-BUG-02 (LOW-MED) — "No issues" badge despite overlapping/garbled labels
- **What:** The diagram's health indicator shows **"No issues"** while labels visibly overlap
  (SLD-BUG-01). The validation doesn't flag layout/label-collision problems.
- **Why it matters:** false "all clear" on a diagram that is visibly broken — misleads sign-off.

### SLD-BUG-03 (MED-HIGH) — Severe duplicate/redundant API calls on a single SLD load (perf regression)
- **What:** Loading ONE SLD fires the same backend requests many times (network-log counts):
  **`GET /api/sld/{id}` ×4**, **`GET /api/enum-node-voltages` ×6**, **`node_classes/user/{id}` ×6**,
  `edge_classes/user/{id}` ×4, **`users/{id}/slds` ×3**, `lookup/nodes/{id}` ×3 (×2 with
  include_bus_nodes + ×1 without), `sld/{id}/views` ×2, `view-mappings` ×2, `library-designations` ×3.
- **Evidence:** Playwright network capture (Part-1). The whole node list + "AF not ready" logs also
  print twice (double render pass).
- **Why it matters:** the SLD page is the heaviest in the app; firing the same GETs 3–6× each
  multiplies backend load + slows first render. Clear regression / missing request de-dup+memo.

### SLD-BUG-04 (MED) — Arc-Flash readiness flags a node with ZERO missing fields as "not ready"
- **What:** Console readiness check logs `🔴 [AF not ready] test fuse {eqp_lib_type: fuse,
  device_role: protective_device, is_node_bus: false, has_eqp_lib: true, missingFields: Array(0)}`
  — a node with **`has_eqp_lib: true` and `missingFields: []`** is still marked **not ready**.
- **Why it matters:** the readiness logic is internally inconsistent (0 missing fields ⇒ should be
  ready). Drives the Arc Flash Readiness module / SLD readiness badges incorrectly.

### SLD-BUG-05 (LOW, third-party) — CSP blocks Beamer fonts + PLuG auth init error on /slds
- **What:** 6× `Loading the font 'https://app.getbeamer.com/fonts2/Lato-*.woff2' violates …
  Content-Security-Policy directive "font-src 'self' https://fonts.gstatic.com data:"` + `[PLuG]
  Error initializing with authentication: {}` + Beamer `/track` 400.
- **Why it matters:** third-party (Beamer/DevRev) but pollutes the console with SEVERE errors on
  every SLD load (noise that masks real errors); either widen CSP for Beamer or stop loading it.

### SLD-BUG-06 (HIGH) — Unplaced nodes pile at default coords (0,0)/(100,100) → overlapping diagram
- **What:** Nodes without saved layout coordinates default to (100,100) or (0,0) and the layout does
  NOT spread them, so they stack. **Backend-confirmed:** gyu SLD has **3 nodes at (100,100)**
  (Disconnect Switch + Panelboard + Fuse) + a Fuse at (0,0). **Wild Goose Brewery SLD (490 active
  nodes): 57 nodes stacked at (0,0) + 10 at (100,100) = 67 overlapping at two points.** Renders as an
  unreadable pile (root cause of SLD-BUG-01).
- **Evidence:** GoJS model dump + `/api/lookup/nodes/{id}` coords; screenshots
  `sld-label-overlap-gyu.png`, `sld-wildgoose-490nodes.png`.
- **Why it matters:** core v3 layout/persistence defect — new/edited nodes aren't assigned distinct
  positions (or the layout isn't applied+saved), so diagrams collapse into overlapping stacks.

### SLD-BUG-09 (HIGH) — Orphan/dangling edges: deleting a node leaves its edges behind
- **What:** On Wild Goose Brewery the backend has **5 active edges, but ALL 5 are orphans** — their
  `source`/`target` reference **8 node ids that no longer exist at all** (hard-deleted). The GoJS
  canvas therefore renders **0 links** for a 490-node SLD. Deleting a node does NOT cascade-delete /
  clean up its connected edges → referential-integrity violation; the diagram shows no connections.
- **Evidence:** `/api/sld/9138fd14…` — 5 non-deleted edges, 0 with both endpoints present (8 endpoint
  refs missing from the node set). UI GoJS `diagram.links` count = 0.
- **Why it matters:** silent data corruption — a "single-line diagram" with no rendered connections;
  orphan edges accumulate and can break routing/export/analysis.

### SLD-BUG-10 (MED, security) — Stored XSS payload persisted as a node name
- **What:** Wild Goose Brewery contains a node literally named
  `<script>alert('XSS')</script>_1780930307763` (a stored payload from prior testing). It did NOT
  auto-execute on the GoJS canvas (canvas text), but the same value flows into DOM surfaces (search
  results, node-detail panel, exports) that must be verified to escape it.
- **Status:** stored ✓; execution-on-render check pending (Part 5). Even if escaped today, the field
  accepts and stores raw `<script>` — input not sanitized.

### SLD-BUG-11 (MED-HIGH) — `/api/sld/{id}` leaks soft-deleted nodes & edges (bloat + inconsistent)
- **What:** `GET /api/sld/{id}` returns **ALL** records incl. soft-deleted: Wild Goose = **1237 nodes
  (only 490 active → 747 deleted returned)** + **258 edges (only 5 active → 253 deleted returned)**.
  The sibling `GET /api/lookup/nodes/{id}` correctly returns only the 490 active. Inconsistent
  server-side filtering + heavy payload bloat on the app's heaviest page.
- **Why it matters:** 2–3× payload bloat (slows the SLD load that's already firing duplicate calls,
  SLD-BUG-03) and an integrity inconsistency between two endpoints for the same SLD.

### SLD-BUG-12 (LOW-MED) — Edges with null `edge_class` (unclassified)
- **What:** Active edges carry `edge_class: null` (gyu edge[0]; 2 of 5 active edges on Wild Goose).
  Connections page corroborates ("2 unclassified"). Unclassified edges have undefined type/behavior.

### SLD-BUG-13 (INFO/positive + gap) — Issue-detector flags electrical data bugs but NOT layout
- **What:** Wild Goose shows a **9-issue** panel — real electrical findings: *"Invalid phase
  configuration on Trim600639 — 208V doesn't permit 3P4W-HLD"*, ×2 *"Transformer without secondary
  voltage"*. Good product behavior. BUT the same detector reports **"No issues"** on gyu despite the
  overlapping labels (SLD-BUG-01/02) → it validates electrical data, not layout/render integrity.

---

## SYSTEMIC findings — backend integrity scan across ALL 107 SLDs (parallel, 10 agents)

Scanned every SLD's `/api/sld/{id}` + `/api/lookup/nodes/{id}` (full result:
`docs/bug-hunts/sld-107-scan-result.json`). **87 of 107 SLDs had ≥1 integrity issue.** These
collapse into 8 systemic, server/data-layer bugs — the SLD v3 release gate.

| # | Systemic bug | Affected | Sev | Worst evidence |
|---|---|---|---|---|
| S1 | **Connectivity loss / orphan nodes** — non-bus nodes referenced by NO edge; topology missing | **82/107** | HIGH | Wild Goose 488/489 orphan (5 edges/490 nodes); Android Site 1095 orphan (122 edges/1228 nodes); test site 1530/1540; "Test 12/4" 214/217 (3 edges) |
| S2 | **Default-coordinate pile-up** — nodes persisted at unplaced defaults (0,0)/(100,100), layout lost | **79/107** | HIGH | Android Site 754 nodes piled at (0,0); test site 187@(100,100)+102@(0,0); "Quick Count on Web 2" all 46 unplaced |
| S3 | **Negative / out-of-range coords** — x<0/y<0, some wildly off-canvas (bad layout/import scaling) | **61/107** | MED | Outliers (103105,-1888), (49193,-3320), (-17347,-10264), (-16469,67771); Migration ios 222/292 negative |
| S4 | **Unclassified edges** — active edges with null/empty `edge_class` (connection type not persisted) | **54/107** | HIGH | Whole diagrams 100% unclassified: QA-Site-1202 15/15, user A 5/5, Test_25_may_Web 3/3; Test site 88/95 |
| S5 | **Duplicate node labels** — same label repeated within an SLD (copy/import not de-duping) | **49/107** | MED | test site 'Fuse 1' ×60, 'UpdatedModel' ×46; Android Site 191 dup labels; 'App update check' ×23 |
| S6 | **Soft-deleted edges leak through `/api/sld`** — `is_deleted=true` returned (not filtered server-side) | **35/107** | HIGH | Wild Goose 253 deleted edges leaked (258 raw vs 5 active); test site 181; Yxbzjzxj 41; London,UK 25 (confirms SLD-BUG-11) |
| S7 | **Node coordinate overlap** — distinct nodes at identical coords → render on top of each other | **40/107** | MED | Performance sld 216 nodes at (20,50); two distinct nodes both labeled 'Fuse 2' at (125,75) |
| S8 | **Isolated SLDs — nodes but ZERO edges** (edge data never saved / fully dropped) | **8/107** | MED | Migration ios 292 nodes / 0 edges; "Bhhhh" 76/0; "Quick Count on Web 2" 46/0 |

**Root-cause hypothesis (strong):** the worst connectivity-loss + pile-up SLDs are named
**"Migration ios", "*offline*", "*sync*", "Android Site"** → edges and layout coordinates are being
**dropped on offline-sync / migration / bulk-import**. This is the highest-value lead for dev:
the create/import/sync path is not persisting edge endpoints or node coordinates.

> Reconciliation with the live findings: S2≈SLD-BUG-06, S1/S8 explain SLD-BUG-09 (orphan edges →
> 0 rendered links), S6=SLD-BUG-11, S4=SLD-BUG-12, S7=SLD-BUG-01. The hypothesized "null node name"
> bug was NOT found (labels are duplicated, never null); the `<script>`-named node (SLD-BUG-10) is
> stored raw but **escaped on render** (no execution via search) — downgrade to "input not
> sanitized at write; output safe."

---

## Part 2 — deep INTERACTIVE / render-architecture findings (2026-06-10, session 2)

Driven live with Playwright + GoJS-model introspection on gyu (6 nodes) and Wild Goose (490 nodes).
Focus: actual edit/move/delete/export interactions + how the diagram is mounted & rendered.

### SLD-BUG-14 (HIGH, perf/architecture) — The SLD canvas is **double-mounted**; every diagram renders TWICE
- **What:** The `/slds` page mounts **TWO** GoJS `Diagram` instances for the same SLD — one inside a
  **`display:none`** container (`MuiBox css-7ariaq`, measured **0×0 px**, GoJS canvas falls back to its
  16×16 default at scale ~0.001) and one in the visible container. **Both fully load, parse and lay out
  every node.** Verified live: selecting Wild Goose loaded **490 nodes into BOTH** the hidden (0×0) and
  the visible diagram (`G.Diagram.fromDiv` enumeration returns 2 diagrams, both `nodes.count=490`).
- **Evidence:** ancestor-chain dump (`gojs-diagram`→`css-a9lb7p`→`css-7ariaq[display:none]` all 0×0 vs
  sibling `css-1hxddig` 1494×1047); the per-node processing, the `🔴 [AF not ready]` lines AND the whole
  `/api/sld`+`lookup/nodes`+`node_classes`+`enum-*` fetch chain all run **twice** (this is the mechanism
  behind SLD-BUG-03's duplicate fetches and the ~485-line ×2 console flood per heavy SLD).
- **Why it matters:** doubles layout/render CPU + memory + backend fetches on the app's heaviest page;
  for a 490-node SLD that is a second full GoJS layout into a canvas the user never sees. Unmount the
  hidden duplicate (or render it lazily) and the duplicate-fetch waste (SLD-BUG-03) largely disappears.
- **Caveat for testers:** because there are two diagrams, `G.Diagram.fromDiv` returns the **hidden** one
  first — always pick the diagram whose div has width>10 & `display!=='none'` when introspecting, or you
  will mis-read a 0×0 "blank" diagram as a render bug.

### SLD-BUG-15 (HIGH) — **Export** produces no output and no feedback (silent no-op)
- **What:** Clicking the **Export** button yields **nothing**: no file download (Playwright `download`
  event never fires), no format-selection menu, no dialog/popover, **no network request, and no console
  log or error.** Reproduced on **2 independent attempts** (visible-ref click + role-name click).
- **Why it matters:** "export diagrams (PDF/PNG/JSON)" is an explicit v3 release-gate capability. A
  user clicking Export gets zero feedback and no artifact. **Release-gate blocker** until confirmed.
- **Caveat:** the double-mount (SLD-BUG-14) means there are 2 Export buttons; recommend a manual
  confirmation clicking the *visible* button, but both attempts here produced no observable effect.

### SLD-BUG-16 (MED-HIGH) — S1/S2 reproduce on a **small, web-created** SLD, not just migration data
- **What:** gyu (6 nodes) itself exhibits the systemic bugs live: **4 of 6 nodes sit at exactly
  (100,100)** — `test fuse`, `dosconnect switch test Fuse`, `dosconnect switch test`, `pole 3` — all
  stacked at the same screen point and fully overlapping (S2); and **2 nodes are orphans with 0 links**
  (`test fuse`, `pole 3 MCB (copy)`) (S1). Also visible: a duplicate-paste artifact node
  **`pole 3 MCB (copy)`** and a persisted typo **`dosconnect switch test`**.
- **Why it matters:** disproves "S1/S2 are only legacy offline/migration SLDs" — they occur in tiny,
  everyday SLDs too, so the create/place/connect path on *current* web/app data is still producing
  unplaced + unconnected nodes. Raises S1/S2 priority for the release gate.

### SLD-BUG-17 (MED) — Delete dialog says **"cannot be undone"** while an **Undo** button is present
- **What:** Unlock graph + select a node → contextual toolbar exposes **Undo / Redo / Refresh / Copy /
  Add to… / + Asset / Box / Edit / Delete**. The Delete button opens a confirmation:
  *"Delete Node — Are you sure you want to delete 'pole 3'? **This action cannot be undone.** [Cancel]
  [Delete]"* — directly contradicting the toolbar's **Undo** control.
- **Why it matters:** mixed signals → data-loss risk. It implies node deletion is a **hard backend
  delete** that Undo does NOT cover; a user who trusts Undo can permanently lose a node (and, per
  SLD-BUG-09, orphan its edges). (Test cancelled to protect shared QA data; cascade behaviour for
  delete is already documented systemically as S1/S6.)

### SLD-BUG-18 (MED) — No keyboard / right-click delete; Delete works only via the toolbar button
- **What:** With graph unlocked + a node selected (`allowDelete:true`), pressing **Delete** does
  nothing and **right-click shows no context menu**. GoJS itself *can* delete — a programmatic
  `commandHandler.deleteSelection()` correctly cascaded **6→5 nodes / 3→1 link** (both edges of the
  deleted node removed, no client-side orphan) — so the Delete key is intercepted/unbound, not absent.
- **Why it matters:** non-standard editor affordances; users expect Delete/Backspace + right-click in a
  diagram editor. Minor, but a discoverability/efficiency gap on the v3 editor.

### SLD-BUG-19 (MED) — Dragging one node silently **moves a connected node** by the same offset
- **What:** Dragging `pole 3 MCB` +260px down persisted correctly (real drag → **`PUT /api/node/update`
  ×2 + `PUT /api/edge/update` ×2, all 200**) — BUT after reload its connected node `pole 3` had ALSO
  moved by the identical **+260** (100,100 → 100,360), despite only `pole 3 MCB` being dragged.
- **Why it matters:** either an unintended coupling or an undocumented "move whole branch" behavior;
  either way a user can silently relocate nodes they didn't intend to move.
- **Positive sub-finding (green):** a single node **move DOES persist three-layer** (UI drag → backend
  PUT 200 → survives reload at the new coords). So S2's default-coord pile-up is **not** caused by moves
  failing to save — it comes from node *creation/import* defaulting to (100,100) and never being placed.

### SLD-BUG-20 (LOW, a11y/UX) — aria-hidden traps focus on dialog; bus group swallows child-node clicks
- **What:** (a) Opening the delete dialog logs `Blocked aria-hidden on an element because its descendant
  retained focus … Ancestor with aria-hidden: <div#root>` — a focused `<button>` is hidden from AT
  (WCAG/ARIA violation; use `inert`). (b) Clicking on the `pole 3 MCB` child node selects the enclosing
  **bus group** instead — the group captures the hit, so a child node can't be selected by clicking it.
  (c) The red **Lock Graph** FAB still has **no accessible name** (ties into BUG-B icon-button-name).
- **Why it matters:** keyboard/AT users can't operate the dialog cleanly; the group-hit issue makes
  per-node editing (select → Edit/Delete) hard for nodes inside a bus.

### Green / working (verified, not bugs — recorded for the sign-off)
- **Node move three-layer persistence** works (SLD-BUG-19 sub-finding).
- **Toolbar v3 view features** — MiniMap, Trace Lineage (upstream/downstream), Edge Labels, Status
  Badges, Highlight Selection — all toggle on **without crashing** and render (minimap + green trace
  path verified on gyu: `sld-toolbar-features.png`).
- **Delete confirmation dialog** is well-formed (clear node name, Cancel/Delete) — only its
  "cannot be undone" copy conflicts with Undo (SLD-BUG-17).
- The web SLD editor **does** support edit operations via its own toolbar (+Asset/Box/Edit/Delete/Copy/
  Undo/Redo) — GoJS-native insert/link are off (`allowInsert/allowLink=false`) but the app wraps its own
  insert flow; so "web can only view SLDs" would be an **inaccurate** claim.

### Evidence (session 2)
`sld-fresh-reload-state.png` (gyu healthy render), `sld-switch-stale-canvas.png` (Wild Goose 490/0-edge
render), `sld-toolbar-features.png` (minimap/trace/badges + overlap), `sld-node-dblclick.png` (edit
toolbar: +Asset/Box/Edit/Delete + Undo/Redo).

---

## Coverage of the release-gate matrix
- **Web functional v3 (render/nodes/edges/icons/labels):** ✅ tested — render breaks on overlap
  (S2/S7/SLD-BUG-01), edges don't render when orphaned (S1/SLD-BUG-09), labels collide.
- **CRUD + three-layer integrity (UI / browser-local / backend):** ✅ tested — backend↔UI compared
  via GoJS model vs `/api/lookup/nodes` + `/api/sld`; deleted-record leak + endpoint-filtering
  inconsistency (S6/SLD-BUG-11) found; duplicate-fetch waste (SLD-BUG-03). **Update/Move:** verified
  end-to-end (drag → `PUT /node/update` 200 → survives reload, SLD-BUG-19). **Delete:** affordance +
  confirmation flow verified; "cannot be undone" vs Undo conflict (SLD-BUG-17); not executed on shared
  data. **Create:** GoJS-native insert off; app-level +Asset/Box exist (not exercised this session).
- **Export diagrams:** ✅ tested — **non-functional / silent no-op (SLD-BUG-15)** — release-gate blocker.
- **Render architecture:** ✅ double-mount defect (SLD-BUG-14); live S1/S2 on a small SLD (SLD-BUG-16);
  Wild Goose 490 nodes / 0 rendered edges (SLD-BUG-09 confirmed visually live).
- **Cross-browser (Chrome/Safari/Edge/Firefox):** Chrome covered here (Playwright Chromium). Safari/
  Edge/Firefox v3 GoJS-canvas rendering = **not yet run** (next session — same scenarios, esp. the
  490-node SLD + overlap cases; verify GoJS canvas + export render parity).

## Still to do (next session, divided)
- Part A: cross-browser render parity (Safari/Edge/Firefox) of the gyu + Wild Goose SLDs + export.
- Part B: **Confirm SLD-BUG-15 (Export no-op) manually** clicking the *visible* Export button; if it
  ever does produce a file, check whether the export honors the broken layout / leaks deleted nodes.
- Part C: AI-assistant SLD feature (if present); exercise the app-level **+ Asset / Box** create flow
  (does a newly-created node get a real coordinate, or default to (100,100) → S2 at creation time?).
- Part D: file the top systemic bugs (S1/S2/S4/S6) + SLD-BUG-14/15 into Jira after sign-off (per
  never-modify-Jira-without-permission rule — list prepared, await go-ahead).

## Session-2 verdict (deep interactive pass)
Net-new HIGH-priority: **SLD-BUG-14** (double-mounted diagram → 2× render + the duplicate-fetch root
cause) and **SLD-BUG-15** (Export silent no-op — release-gate blocker). Net-new MED: SLD-BUG-16
(S1/S2 on small web SLDs), 17 (delete "cannot be undone" vs Undo), 18 (no key/right-click delete), 19
(drag moves connected node), 20 (a11y/focus + group-hit). Move-persistence is the one clearly-green
core path. SLD v3 is **not release-ready**: a non-functional Export + the S1/S2/S6 data-integrity
cluster + a 2× render cost are all open.
