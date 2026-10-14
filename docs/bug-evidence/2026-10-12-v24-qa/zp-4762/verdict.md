# ZP-4762 — SKM export: exclude node buses from every PTW study (Exclude flags checked)

**Verdict:** PASS (backend; verified in XML) — build `index-D1IWZ6SI.js`.

## Checked
| Check | How | Result |
|---|---|---|
| Node bus study flags | Richmond, CA export — all 33 exported node buses | each has NodeBus(37552142)=True, Size/Do Not Size(65540)="Do Not Size", **DE_Excluded(65554)=True, AF_Exclude_Bus(37552144)=True, AF_Exclude_LineSide(21364796)=True, CoorEval_Excluded(65570)=True**. PASS |
| Negative: ordinary bus unaffected | 93 non-node buses on Richmond | none carry any Exclude or Do-Not-Size flag. PASS |
| A stray source attribute can't override | imported a node bus with DE_Excluded=False, AF_Exclude_Bus=False in the file | export still forces all four Exclude flags + Do Not Size = True. PASS |
| Regression vs ZP-4760 | same export | eg_x/eg_y/eg_width and Do Not Size still present; only the Exclude flag values changed (False→True). PASS |
| DC node bus | own site DC node bus | DC node bus exported (own fixtures) — DC node bus study-flag table coverage limited (see Not covered). |

## Bugs found
None.

## Enhancement suggestions
- None specific; the change is a one-line correctness fix.

## Mobile part for Nency
Backend-only export generation; no mobile surface. Nothing for Nency.

## Not covered
DC-node-bus Exclude-flag emission depends on the DC field tables existing for that style; my DC node bus exported but I could not independently confirm the DC-specific exclude ids against PTW. Did not open in PTW.

## Proposed Jira comment
h3. QA — ZP-4762 PASS (QA build index-D1IWZ6SI.js)

Exported Richmond, CA (33 node buses). Every node bus now reads Do Not Size = on and all four Exclude flags = True — DE_Excluded, AF_Exclude_Bus, AF_Exclude_LineSide, CoorEval_Excluded. Ordinary (non-node) buses are untouched. Importing a node bus whose file said DE_Excluded=False still forces it True, so a stray source attribute can't override.

!QA-12Oct-ZP-4762-2-PASS-xml-node-bus-exclude-flags.png|width=720!

Regression from ZP-4760 holds: eg_x/eg_y/eg_width and the Do-Not-Size pin still export; only the Exclude flag values changed.

## Recommended status
**READY TO RELEASE.**
