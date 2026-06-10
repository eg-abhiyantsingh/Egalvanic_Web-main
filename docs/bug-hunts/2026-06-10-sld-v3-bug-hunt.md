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

## Coverage of the release-gate matrix
- **Web functional v3 (render/nodes/edges/icons/labels):** ✅ tested — render breaks on overlap
  (S2/S7/SLD-BUG-01), edges don't render when orphaned (S1/SLD-BUG-09), labels collide.
- **CRUD + three-layer integrity (UI / browser-local / backend):** ✅ tested — backend↔UI compared
  via GoJS model vs `/api/lookup/nodes` + `/api/sld`; deleted-record leak + endpoint-filtering
  inconsistency (S6/SLD-BUG-11) found; duplicate-fetch waste (SLD-BUG-03).
- **Cross-browser (Chrome/Safari/Edge/Firefox):** Chrome covered here (Playwright Chromium). Safari/
  Edge/Firefox v3 GoJS-canvas rendering = **not yet run** (next session — same scenarios, esp. the
  490-node SLD + overlap cases; verify GoJS canvas + export render parity).

## Still to do (next session, divided)
- Part A: cross-browser render parity (Safari/Edge/Firefox) of the gyu + Wild Goose SLDs + export.
- Part B: Export output correctness (does Export honor the broken layout? PDF/PNG/JSON fidelity).
- Part C: AI-assistant SLD feature (if present) + reload/deep-link persistence of a manual node move.
- Part D: file the top systemic bugs (S1/S2/S4/S6) into Jira after sign-off (per never-modify-Jira-
  without-permission rule — list prepared, await go-ahead).
