# Web v2.2 deep pass — every ticket walked in the browser (2026-09-21)

**Prompt:** "web 2.2v test all the ticket in depth create a artifact for me. dont follow lazy approach steps screenshot"

Artifact: <https://claude.ai/artifact/S97U6jJcahXeEVx3bg4sXu>
Evidence: `docs/bug-evidence/2026-09-21-v22-deep/` — 46 fresh captures + `NOTES.md` (per-ticket walkthrough log)
Companion summary board (unchanged URL): <https://claude.ai/artifact/FD2esA3TgCg2cenoDEHP26>

## What changed versus the morning board

The morning board leaned on bundle-grep "present in the build" verdicts for 18 tickets. The owner called that
lazy. This pass replaced every one of them with a browser walkthrough — numbered clicks, what the screen showed,
a capture at the proving moment — on the shipping build. Bundle greps were used only to decide where to look.

## Release = 42 tickets (Jira re-read at publish)

| State | Count |
|---|---|
| Verified in the UI, spec matched, no defect | 19 |
| Present, partly exercised (no data / no seat for one path) | 4 |
| Open defects (reproduced today or still open) | 10 |
| Cannot be settled on QA (no data, prod-only, not deployed, not a web surface) | 9 |

Verified: 4038 4039 4041 4043 4044 4062 4068 4084 4109 4110 4112 4113 4131 4149 4152 4167 4170 4171 4174.
Partial: 4086 4127 4138 4153. Open: 4042 4292 4159 3782 3783 3834 4189 4212 4315 4322.
Unsettleable: 4040 4045 4060 4061 4066 4067 4082 4111 4128.

## Headline findings

1. **ZP-4042 blocker survives the rebuild.** `/api/reporting/history` → 500 on BOTH builds today
   (`index-BV-phiFE.js`, then `index-BOaMecwk.js` after a mid-afternoon rebuild); traces becd79c4…, 7dc46732…,
   d3bde59f…. `/reporting/configs` → 200 each time.
2. **ZP-4292 reproduced** (this morning): all 17 services unticked → saved asset carries service columns; the
   edit form has no Services section to undo it.
3. **New regression candidate on `index-BOaMecwk.js`: Admin → Asset Classes renders "No rows".** Four full loads;
   `/api/lookup/node-classes` returns 47 rows, all company-owned (none global) in the same session; mounting the
   page fires no API request at all (fetch/XHR hooked). Not filed — owner's call.
4. **ZP-4111 is not on QA** — `sources=skm|bogus|omitted` return identical rows; the picker itself works.

## Clean UI verifications worth naming

* ZP-4167/4174 with positive AND negative controls: Junction Box (System Voltage only), Disconnect Switch
  (Mains Type kept, manufacturer block gone), Busduct (System Voltage only), Panelboard (everything present).
* ZP-4041: one card spins + "Generating…" chip, six cards dim with no spinner, after a real Generate.
* ZP-4113: Issue Report card → its 3 options; issue export lists only Issue Report; assessment export excludes it.
* ZP-4084: single Condition filter = 1 / 2 / 3 / Non-serviceable / Not assessed; no "Not rated".
* ZP-4062: Mark As… popover lists only the 3 NETA services registered on the two selected assets.
* ZP-4149: IR-typed WO report modal lists "Infrared Thermography Report" in scope.
* ZP-4131: Settings Verifier in BOTH menus (SKM after Export; SLD after Export Engineering XML).
* ZP-4152: Rename in the service menu; stars only on company-owned PM standards, none starred.
* ZP-4068: Count/Journal radio cards; JOURNAL chips; Journal Review page with rail/timeline/Interpreted Model.

## Deliberately not executed (writes on shared tenant state)

Bulk Mark As confirm; star-a-default; bulk method apply (mints a service version); defer submit; class remove
(dialog opened by mistake — cancelled, class intact). One report generation WAS run (it is what the page is for).

## Caveats

* QA rebuilt mid-run; each finding names its bundle. Dashboard/Reports/Condition Assessment/Compliance were on
  BV-phiFE; Assets-class forms onward on BOaMecwk; ZP-4042 hit on both.
* Portal Sales positive case still needs a human sign-in as the new seat (no password entry by me).
* ZP-4066: SLD shows 21 issues; the issue list did not open on click, so the phase-config false positive was
  neither confirmed nor ruled out.
