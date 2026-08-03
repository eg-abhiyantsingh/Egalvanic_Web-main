# SLD View — child-node positions saved but NEVER honored on reload (customer: "child fuses move away from bus switches")

**Verified live on QA** `acme.qa.egalvanic.ai` (badge V1.36), 2026-08-03, session `abhiyant.singh+admin@egalvanic.com` (role Admin, `x-active-role-id: e9ad3158…`).
Customer report: *"every time they open their SLD, all child fuses for their bus switches move away from their parent assets … every time I close and reopen the SLD I have to move them instead of it preserving my formatting. View: Substation 2."*
"Substation 2" is a prod view name (not on QA — all 179 QA sites / 46 view-bearing sites swept). The mechanism reproduces deterministically on QA with:

**Repro fixture:** Site **Richmond, CA** (`sld_id 768ff683-4244-4fa0-aced-36ed471f0c33`) → view **"View 2"** (`c11ab25d-07eb-44d9-8403-824fa389d3d3`, 7 mapped nodes).
Parent **Switchboard 1434** (`d782fcfd…`, group) has 3 children via `parent_id`: **Fuse 111s** (`17df3522…`), **Fuse 2** (`31ce6208…`), **MCC Bucket545** (`be195ae1…`) — the exact "child fuses on a bus switch" shape.

## Coordinate contract (established from data + save path)

- Top-level nodes: absolute canvas coords in `mapping_node_sld_view` (e.g. switchboard 27450,100).
- **Child nodes (parent_id set): PARENT-RELATIVE coords** — in both the facility table (`/api/lookup/nodes`: 40/340/640, 149) and the view mapping (25,0 / 110,100 / 195,0).
- Save path obeys this: dragging Fuse 111s to absolute (27519, 211) fired
  `PUT /api/sld-view/{viewId}/positions` with body `{"positions":[{"node_id":"17df3522-…","x":69,"y":111}]}`
  → **69/111 = drag position minus parent (27450,100)** → 200 `{"success":true,"updated_count":1}` → mapping row updated, `modified_at 2026-08-03T13:54:49Z`. (`drag-save-PUT-body.json`)

## BUG A (the customer's bug): loader sources CHILD positions from facility coords, not the view mapping

Reload → reopen View 2 (GoJS canvas render):

| Node | View mapping says (parent+rel) | Actually rendered | Source |
|---|---|---|---|
| Switchboard 1434 (parent) | 27450,100 | 27450,100 | mapping ✓ |
| Fuse 111s | 27450+69, 100+111 = **27519,211** | **27790,249** | facility (340,149) ✗ |
| Fuse 2 | 27645,100 | **28090,249** | facility (640,149) ✗ |
| MCC Bucket545 | 27475,100 | **27490,249** | facility (40,149) ✗ |

- The freshly-saved mapping (69,111) is **in the DB** (re-fetched `/view-mappings`, modified stamp present) yet the render ignores it.
- Confirmed on a **second** clean reload after restoring the mapping to its original (110,100): children still render from FACILITY coords (27790/28090/27490, y=249). Top-level nodes (Motor AST-1, Fuse-23, AST-Dull) honor their mappings on every load.
- ⇒ **Write path is correct; the view loader takes child positions from facility `node.x/y` instead of the per-view mapping.** The user's arrangement is saved, acknowledged (200), persisted — and discarded on every reopen. They must re-move the children every time. This is precisely the reported behavior.
- Note: the FIRST view load of the session rendered children from mappings (27560,200 etc.) — the source-selection is inconsistent across loads (cache/race), but after any interaction it lands on facility coords consistently.

## BUG B (same family): dual renderer, nondeterministic — React Flow path drops parented children

The /slds page mounts **either** a GoJS canvas **or** a React Flow renderer for the same view, varying between loads of the same session (React Flow `.react-flow__node` DOM one load; `go.Diagram` canvas the next):
- **React Flow load of View 2:** only the 4 top-level nodes render; the 3 parented children are absent from the DOM entirely (their small relative coords treated as absolute puts them ~27,000px from the parent, outside the fitted viewport). On a small diagram they'd render **near the origin, visibly detached from the parent** — the customer's literal symptom.
- **All Nodes** mode on this load path rendered a completely blank canvas (0 nodes of 579) with only the 72-issue panel.
- The app even ships a validation for the degenerate form: **"Illegal children on …"** ("X can't contain child devices, but has N (Fuse-1…) — re-parent to a panel/bus") — the same parent/child containment domain.

## Where to point the fix
`GET /api/sld-view/{viewId}/graph` returns children with `parent_id` + relative x/y; the client (or the graph endpoint's coordinate merge) must apply **mapping → fallback facility**, and compose `parent.abs + child.rel` in BOTH renderers. Verify against: view `c11ab25d…`, child `17df3522…` — set mapping (110,100), reload twice, expect render at (27560,200) every time.

## Data hygiene
Fuse 111s mapping was restored to its original (110,100) after the experiment (`modified 2026-08-03T13:57:50Z`). No other tenant data touched.

## Artifacts
- `drag-save-PUT-body.json` — the parent-relative save payload + 200 response.
- `richmond-view1-reactflow.png` — React Flow renderer active on the same page (View 1).
- Session scratchpad (this run): `view1-compare.json`, `view2-graph.json`, `fuse-views.json` (all 51 fuse-bearing views on QA, sweep).
