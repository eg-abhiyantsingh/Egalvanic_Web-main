# ZP-4805 — SKM export: group eg_y at the bus-bar midline, not the top-left (task #165)

**Verdict:** PASS (backend; verified in XML) — build `index-D1IWZ6SI.js`.

## Checked
| Check (all on my own QA-DEMO site, group shapes I set) | How | Result |
|---|---|---|
| Normal group eg_y = top + renderHeight/2 | 920x240 group at y=1000 | eg_y = 1120 (1000 + 240/2). PASS |
| Height missing → floor to 90 | group height=null at y=1000 | eg_y = 1045 (1000 + 90/2). PASS |
| Legacy-small (both axes tiny) → clamp to 90 | 100x40 at y=1000 | eg_y = 1045 (clamped to 90). PASS |
| Wide-but-short honoured as stored | 600x40 at y=1000 | eg_y = 1020 (1000 + 40/2, not clamped up). PASS |
| Simplified group keeps top-left | is_simplified=true, 600x240 at y=1000 | eg_y = 1000 (top-left, no bar). PASS |
| Children resolve against group top-left | child CBs at 40,140 / 340,149 | eg = parent top-left + child offset (QDB-CB1 eg_y=1140, QDC-CB1 eg_y=1149). PASS |
| Whole-SLD and view-scoped both | export + `?view_id=` | both emit the midline eg_y. PASS |
| View mapping width/height null → fall back to node dims | set view position width=null/height=null, re-export | eg_y=1120, eg_width=920 (node's own dims used). PASS |
| Node with no stored x/y | QDC-UTIL x=null | no eg_x/eg_y emitted. PASS |
| Tenant/site scope | PM vs staff | export-skm leaks cross-tenant/cross-site — cross-cutting finding, not this ticket. |

## Bugs found
None.

## Enhancement suggestions
- The caveat in the ticket (export duplicates ENCLOSURE_MIN_WIDTH/HEIGHT=170/90 and the bar-at-half-height rule from the SLD editor) is a maintenance risk; a shared constants source or a contract test would stop the two drifting. (Engineering note, not a user-facing bug.)

## Mobile part for Nency
Backend-only. Nothing for Nency.

## Not covered
Did not open in PTW to confirm children visually hang off the bus bar; verified the eg_y arithmetic against every documented case instead.

## Proposed Jira comment
h3. QA — ZP-4805 PASS (QA build index-D1IWZ6SI.js)

On a controlled site I set a group's height/width to each documented case and exported (whole-SLD and view-scoped). A non-simplified group's eg_y is now top + render-height/2 (bus-bar midline): 920x240 at y1000 → 1120; height missing → floors to 90 (1045); legacy-small 100x40 → clamps to 90 (1045); wide-but-short 600x40 → honoured as stored (1020); a simplified group stays at its top-left (1000). Children resolve against the group top-left. A view whose mapping has null width/height falls back to the node's own dimensions.

!QA-12Oct-ZP-4805-3-PASS-xml-group-midline-cases.png|width=720!

## Recommended status
**READY TO RELEASE.**
