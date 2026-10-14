# ZP-4760 — SKM export: diagram layout attributes, node-bus study flags, and unsuffixed arc-flash labels

**Verdict:** PASS (backend; verified in XML) — build `index-D1IWZ6SI.js`.

## Checked
| Check | How | Result |
|---|---|---|
| eg_x / eg_y on every component, eg_width on groups | Richmond + own site exports | every component tag carries eg_x/eg_y; group types also carry eg_width. PASS |
| Absolute coords relative to parent | compared XML to `/sld/{id}/graph` x/y | top-level node eg = node x/y; **child eg = node x/y + parent x/y** (absolute); 55/55 Richmond children matched. PASS |
| Node bus "Do Not Size" on | all node buses | Size/Do Not Size = "Do Not Size" on every node bus. PASS |
| Unsuffixed AF labels | `?af_label_from_location=true` | labels are the verbatim location with **no -1/-2 suffix** (3 buses sharing a room export the identical label). PASS |
| Negative: node with no stored position | own site, set x=null/y=null | **no eg_x/eg_y emitted** (falls back, no bogus 0,0). PASS |
| Regression with ZP-4762 | same export | the four Exclude flags are now True (ZP-4762), consistent — only expected delta. PASS |
| Tenant/site scope | PM vs staff | export-skm leaks cross-tenant/cross-site — cross-cutting finding, not this ticket. |

## Bugs found
**B1 (Medium) — SLD-page "Export Engineering XML" is not behind `feature-skm-export`** (same defect reported on ZP-4482; both this and 4482 ride the SLD export menu). One ticket, not two.

## Enhancement suggestions
- None; the diagram-fidelity change is internal.

## Mobile part for Nency
Backend-only. Nothing for Nency beyond the shared SLD-viewer OTA note.

## Not covered
Did not open the file in PTW to confirm the rendered arrangement visually; verified eg_x/eg_y/eg_width arithmetic against the graph instead.

## Proposed Jira comment
h3. QA — ZP-4760 PASS (QA build index-D1IWZ6SI.js)

Exports now carry the layout: every component tag has eg_x/eg_y (group types also eg_width). Top-level nodes export their own x/y; children export parent-absolute coordinates (verified 55/55 on Richmond). Node buses carry "Do Not Size"; AF labels from a bus location are written verbatim with no -1/-2 suffix. A node with no stored position emits no eg_x/eg_y rather than a bogus 0,0.

!QA-12Oct-ZP-4760-2-PASS-xml-layout-attributes.png|width=720!

## Recommended status
**READY TO RELEASE.** File the shared B1 (SLD-page export item not gated on feature-skm-export) once.
