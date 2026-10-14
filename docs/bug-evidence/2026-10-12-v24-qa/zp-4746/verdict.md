# ZP-4746 — SKM export: generate arc-flash label numbers from bus location

**Verdict:** PARTIAL — build `index-D1IWZ6SI.js`. Core behaviour works; one real UI bug (stale tooltip) and one unverifiable AC ("existing label preserved"). The suffix AC is intentionally superseded by ZP-4760.

## Checked
| Check | How | Result |
|---|---|---|
| Option ON stamps AF_LabelNumber from Building/Floor/Room | Richmond + own site, `?af_label_from_location=true` | field 21364797 on Bus = `Building/Floor/Room`; 47/127 Richmond buses labelled (the 80 with no location stay blank); 0 mismatches vs the app. PASS |
| DC bus | own site DC bus with a room | field 38338583 stamped = `F2/R2`. PASS |
| "/" in a name → "-" ; whitespace collapsed | room "Rm A/B", floor "Fl  1/2" (two spaces), bus "Txt" (loc 122344/This/Bznnz) | exports `Rm A-B`, `Fl 1-2`, `122344/This/Bznnz` → each "/" inside a name becomes "-", separators stay "/"; XML well-formed (ElementTree parse OK). PASS |
| Free-text location fallback | "Parent Asset 1" (no room, location "Unassigned") | label = `Unassigned`. PASS |
| Negative: option OFF (default) | default export | no AF_LabelNumber added; output byte-identical to before. PASS |
| Suffix -1/-2 for buses sharing a room | 3 buses in one room | **no suffix** — all three export the identical label, a lone bus gets no -1. This is ZP-4760's "unsuffixed AF labels" (also In QA) overriding this ticket's AC. Intended end-state, but the UI hint wasn't updated — see B1. |
| "A label already on the bus is preserved" | imported a study whose bus carried AF_LabelNumber=QA-PRESET-LABEL-A, then exported with option ON | label **replaced** with the location value, not kept — see B2. |
| Roles / flags / tenant | role+flag sweep; PM vs staff | same as the other export tickets: export gated by `feature-skm-export` + `sld_diagrams.view`; **cross-tenant/site export IDOR** (cross-cutting). |

## Bugs found
**B1 (Low, frontend) — the label-option tooltip is out of date: it promises a "-1, -2, …" suffix the export no longer adds (ZP-4760 removed it), and "Buses that already have a label number keep it" (not observed — see B2).**
- Steps: Assets > SKM > Export, hover the info icon by "Generate arc-flash label numbers from location".
- Actual: "Each bus gets Building/Floor/Room plus -1, -2, … as its SKM arc-flash label number. Buses that already have a label number keep it; …". Expected: no suffix (per ZP-4760) and no "keep it" promise unless B2 is fixed.
- Severity: Low (misleading help text). Evidence: `QA-12Oct-ZP-4746-2-FAIL-hint-promises-suffix-and-kept-labels.png`. Assignee: Krunal.

**B2 (Low–Medium, needs Eric's call) — an AF label imported with a study is not preserved on re-export.**
- Steps: import QA-DEMO-PROJ-A.xml whose bus carries AF_LabelNumber="QA-PRESET-LABEL-A"; give the bus a room; export with "from location" ON.
- Actual: label = the computed location (`QA Bldg-North/Fl 1-2/Rm A-B`); the imported label is gone. Expected (per the ticket): the existing label is kept.
- Caveat for honesty: EG has **no node column that stores an AF label** (the import appears not to persist field 21364797), so the "preserve" branch may have nothing to act on by design. I can't tell from the web whether this is a real miss or an untestable AC — hence stay In QA for Eric to confirm. Evidence: `QA-12Oct-ZP-4746-4-FAIL-imported-label-not-kept.png`. Assignee: Eric / Avani.

## Enhancement suggestions
- Show a live preview of the resulting label for one bus in the dialog (also fixes the confusion B1 causes).

## Mobile part for Nency
Backend + a frontend toggle; no mobile export surface. Nothing for Nency.

## Not covered
Did not open in PTW. Did not confirm the project-folder namespace vs filename parsing with a real PTW header (my synthetic header fell back to filename).

## Proposed Jira comment
h3. QA — ZP-4746 PARTIAL (QA build index-D1IWZ6SI.js)

Core works: with the option ON, each bus gets AF_LabelNumber = Building/Floor/Room (DC buses use the DC field); a "/" inside a name renders as "-", whitespace collapses, the XML stays valid; a bus with only free-text location falls back to that text; option OFF adds nothing (default path unchanged). Verified on Richmond (47/127 buses, 0 mismatches) and a controlled site.

!QA-12Oct-ZP-4746-3-PASS-xml-labels-slash-spaces-dc-bus.png|width=720!

Two small things:
# The "-1, -2" suffix in this ticket's AC is *not* produced — ZP-4760 (also in this release) made the labels unsuffixed. That's the intended end-state, but the dialog tooltip still promises "plus -1, -2, …" and "buses that already have a label number keep it".
# A label that arrived with an SKM import is *not* preserved on re-export (it is overwritten by the computed location). EG has no node field that stores an AF label, so this may be untestable by design — please confirm.

!QA-12Oct-ZP-4746-2-FAIL-hint-promises-suffix-and-kept-labels.png|width=720!

## Recommended status
**Stay In QA** — feature is sound, but confirm the "preserve existing label" AC (B2) and refresh the tooltip (B1) before release.
