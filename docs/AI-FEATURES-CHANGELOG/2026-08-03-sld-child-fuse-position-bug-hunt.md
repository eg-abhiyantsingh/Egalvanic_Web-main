# SLD child-fuse position bug — customer repro hunt ("View: Substation 2")

**Date/time:** 2026-08-03, ~13:00–14:00 UTC
**Prompt:** Customer reports all child fuses for bus switches move away from parent assets every time the SLD is opened; manual fixes don't persist. View "Substation 2" — check in QA.

## What was done
1. **Searched for the view:** swept all 179 QA sites' `/api/sld/{id}/views` — no "Substation 2" (it's a prod-tenant view). Pivoted to reproducing the *mechanism* on QA.
2. **Discovered the redesigned /slds page** (V1.36): view-centric loading (facility + Select View), and — critically — **two renderers** (GoJS canvas *or* React Flow) mounting nondeterministically for the same view.
3. **Built the repro fixture:** joined view-mappings with node classes across all 46 view-bearing sites → 51 fuse-bearing views → picked Richmond, CA "View 2": Switchboard 1434 with 3 `parent_id` children (2 fuses + MCC bucket) — the customer's exact "child fuses on a bus switch" shape.
4. **Established the coordinate contract:** children store parent-relative x/y (both facility table and view mapping); a real drag saves the parent-relative delta via `PUT /sld-view/{viewId}/positions` (drag to 27519,211 → body `x:69,y:111` → 200, persisted with fresh `modified_at`).
5. **Proved the bug:** across two clean reloads, the loader rendered the children at **facility coords** (parent+340,149 etc.), ignoring the just-saved view mapping. Parents/top-level nodes honor mappings; only children don't. Saved-but-never-honored = the customer's "I have to move them every time".
6. **Secondary defect:** the React Flow render path drops parented children from the DOM entirely (and renders All Nodes as a blank canvas), consistent with children being placed at relative-coords-as-absolute (~27,000px from parent) on customer-sized diagrams → visibly "moved away".
7. **Hygiene:** restored the moved fuse's mapping to its original (110,100); no other tenant data touched.

## Deliverable
`docs/bug-evidence/sld-view-child-position-not-honored/EVIDENCE.md` + save-payload artifact + renderer screenshot. Fix pointer: loader must source child positions mapping-first and compose `parent.abs + child.rel` in both renderers; verify with view `c11ab25d…` / child `17df3522…` → expect (27560,200) on every reload.

## Depth notes (learning)
- **Why the user's fix "never sticks":** the write path and read path disagree on the *source of truth*. The write goes to `mapping_node_sld_view` (per-view layout), the read derives child placement from the facility-level `node.x/y`. Both are internally consistent — they just aren't the same table. Classic dual-source-of-truth defect; the tell was the mapping's fresh `modified_at` sitting in the DB while the render ignored it.
- **Why fuses specifically:** only *children* (`parent_id` set — fuses/buckets attached to a bus/switchboard) take the facility-coord code path; top-level assets honor mappings. Hence "all child fuses for bus switches" in the complaint, not "all nodes".
- **Renderer migration risk:** the GoJS→React Flow migration introduced a second disagreement (relative-vs-absolute composition). Two bugs, one family: coordinate-space contract violations around `parent_id`.
