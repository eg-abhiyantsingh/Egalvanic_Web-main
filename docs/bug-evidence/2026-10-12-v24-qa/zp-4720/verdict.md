# ZP-4720: Let Cable and Busway be added to services and PM plans

**Verdict: PASS** on QA. Build `index-D1IWZ6SI.js` (19:15 IST); this ticket's code is the same as in `index-BdXGlAq5.js`.
Source check of the live build (source map):
- `AddPmPlanDialog.jsx`: `pickable = rows.filter(c => c.name && !c.is_deleted && !c.is_node_bus).sort(localeCompare)`
- `ServiceWorkOverview.jsx › AddClassDialog`: `.filter(c => c.key && !c.is_deleted && !c.is_node_bus).sort(localeCompare)`

The `is_pseudo_edge` exclusion is gone from both. No LaunchDarkly flag.
Class flags on QA (`GET /node_classes/user/{id}`): Cable, Busway and DC Cable are `is_pseudo_edge:true, is_node_bus:false`. Node Bus (twice) and DC Node Bus are `is_node_bus:true`.

Test data (mine, labelled): service **QA-DEMO hand-built (delete me)** and PM standard **QA-DEMO swap-test delete me**. I used service **QA-DEMO builder-dialog test - delete me** only to look at the picker; nothing was saved to it.

## Checked

| Check | How | Result |
|---|---|---|
| Service › Add class picker lists Cable and Busway | Builder › Services › QA-DEMO builder-dialog › Add class; typed "bus" and "cab" | PASS: Busway, Cable and DC Cable are listed (screens 1 and 2) |
| Alphabetical order | Full option list read from the DOM and compared with a sorted copy | PASS (see A3 log) |
| Node bus not pickable (service) | typed "node" | PASS: only "QANode" (a normal class); no Node Bus or DC Node Bus (screen 6) |
| Add Cable and Busway to a service | QA-DEMO hand-built: Add class › Cable, procedure "QA-DEMO ZP-4720 cable insulation test"; then Busway, "QA-DEMO ZP-4720 busway IR and torque" | PASS: versions 8 "Added Cable" and 9 "Added Busway"; `export-spec` now has procedures `cable` and `busway`, each with its method (screen 3) |
| Covered classes drop out of the picker | After adding, Cable and Busway no longer appear in that service's picker | PASS (by design: `covered` filter) |
| Add-to-PM-plan picker | PM Plans › QA-DEMO swap-test › Add asset class; typed "bus" | PASS: Busduct, Busway, DC Bus; the full list has Busway, Cable, DC Cable (62 options) (screen 4) |
| Node bus not pickable (PM) | typed "node" | PASS: only QANode |
| PM plan created for Cable and for Busway | Add asset class › Cable › Add; Busway › Add | PASS: standard went 232 → 237 plans; new Cable and Busway rows (screen 5) |
| Deleted classes not shown | Filter keeps `!is_deleted`; no deleted class appears in either list | PASS |
| Roles | Services and PM Plans are Admin/Super Admin pages (route gate). Tested as Super Admin; PM/AM/EE/FM have no Builder access | PASS for the role that can use it |
| API | `/procedures-v2/services/{id}/versions`, `export-spec`, `/procedures-v2/pm-plans/classes?pm_standard_id=` agree with the UI | PASS |
| Performance | Picker loads `/node_classes/user/{id}` (65 classes, ~1.5 s, unpaginated: known list-API finding) | OK |

## Bugs found
None in this ticket's change.

## Observation (existing behaviour, not caused by this ticket; recommend a separate Low ticket)
**Adding an asset class to a customised (forked) PM standard creates a second row with the same class name, and the dialog does not say "already covered".**
1. Open Builder › PM Plans › a standard customised from NFPA 70B (for example "QA-DEMO swap-test delete me": it already has Cable 6 plans and Busway 9 plans).
2. Click Add asset class › pick Cable (or ATS, which was always pickable).
3. Actual: C1, C2, C3 and Class-level are all ticked and free. After Add, the standard shows two "Cable" rows (6 plans and 4 plans) and two "Busway" rows (screen 5). For ATS the dialog also shows every slot free although the standard has 15 ATS plans (screen 7, not saved).
4. Expected: the dialog recognises the class the standard already covers and marks those slots "already covered", or the new plans merge into the existing row.
5. Cause seen in the API: the picker uses the company class ids (Cable `e68f22a7…`, ATS `453df10a…`), while the forked NFPA plans keep the global class ids (Cable `f9647080…`, ATS `a1bc7f83…`). ZP-4301 (Done) fixed plan matching for Cable/Busduct; this is the same id split showing up in the standard editor. Suggested assignee: Avani (backend ids) / Krunal (dialog). Severity Low. No existing open ticket found (JQL on PM standard / pm plan / class, last 60 days).

## Enhancement suggestions
1. In the PM standard editor, group plans by class *name* (or show the class key), so the same class never appears twice.
2. Add a search box above "Asset Classes (23)" in the PM standard editor. The list is long and only grows now that Cable and Busway are addable.
3. In Add asset class (service), show the class family in the picker ("Cable · connection", "Busway · connection"), so users know these are drawn as connections on the SLD.

## Mobile part for Nency
The fix is web-only. Please check that a work order whose service now covers Cable or Busway shows those connection-type assets on iOS/Android (forms and checklists open for a Cable/Busway asset), and that PM plan cadences for Cable/Busway show on the device's asset maintenance view.

## Not covered
- Real cable/busway assets inside a work order using the new Cable/Busway procedures (the QA-DEMO service has no forms or methods with field data). This is a mobile and field-data check.

## Proposed Jira comment
```
h3. QA result: PASS (QA, build index-COeCBn0z.js, 13 Oct; first passed on index-D1IWZ6SI.js, 12 Oct)

||Check||Result||
|Service > Add class lists Cable and Busway (alphabetical)|PASS|
|Added Cable and Busway to QA-DEMO hand-built (delete me): versions 8 and 9, procedures cable and busway created|PASS|
|PM Plans > Add asset class lists Cable and Busway; plans created for both|PASS|
|Node Bus / DC Node Bus not pickable in either dialog|PASS|
|Deleted classes not shown|PASS|

# Builder > Services > a company service > Add class > type "bus" and "cab": Busway, Cable and DC Cable are listed; type "node": no Node Bus.
# Pick Cable, name the first procedure, Add: the Cable class appears with its procedure.
# Builder > PM Plans > a custom standard > Add asset class > pick Cable > Add: Cable plans are created.

Re-checked on 13 Oct (index-COeCBn0z.js): the service picker lists 56 classes and the PM picker lists 62, both alphabetical and both including Cable, Busway and DC Cable. "node" finds only QANode. Nothing was saved in the re-check.

!QA-13Oct-ZP-4720-1-service-add-class-cable-listed-PASS.png|width=720!
!QA-13Oct-ZP-4720-2-pm-plan-picker-busway-listed-PASS.png|width=720!

Side note (existing behaviour, separate ticket suggested): in a standard customised from NFPA 70B, adding a class it already has (Cable, Busway, even ATS) creates a second row with the same name, because the picker uses company class ids and the forked plans keep the global ids.
!QA-12Oct-ZP-4720-5-pm-standard-plans-created-duplicate-rows-OBSERVATION.png|width=720!
```

## Recommended status
**READY TO RELEASE**

## Re-check 13 Oct on index-COeCBn0z.js
**Result: still PASS. Nothing changed.** Checked at 13:39–13:50 IST as Super Admin (Services and PM Plans are Admin/Super Admin pages). Read-only: both dialogs were opened and cancelled, and nothing was saved.

| Check | Result |
|---|---|
| Code | `ServiceDetailV2-ol5LxPRG.js` and `PmStandardDetail-Bm_-iPAa.js` are **identical** to 12 Oct's chunks once hashes are ignored. Both filters are still `!is_deleted && !is_node_bus`, with no `is_pseudo_edge` |
| Service › Add class (QA-DEMO builder-dialog test - delete me) | 56 classes, alphabetical. **Cable, Busway, DC Cable listed**. "cab" → Cable, DC Cable (screen 1). "bus" → Busduct, Busway, DC Bus. "node" → QANode only (no Node Bus / DC Node Bus) |
| PM Plans › QA-DEMO swap-test delete me › Add asset class (opened from the PM Standards list) | 62 classes, alphabetical. **Cable, Busway, DC Cable listed**. "bus" → Busduct, Busway, DC Bus (screen 2). "cab" → Cable, DC Cable. "node" → QANode only |
| Saved work from 12 Oct still there | `QA-DEMO hand-built (delete me)` versions still show "Added Cable" and "Added Busway". The standard still shows its Cable and Busway rows (237 plans) |
| Console / network | No 4xx/5xx on the PM standard page. Only the known Beamer/DevRev warnings |

API side note (minor, not filed, not this ticket): `GET /procedures-v2/pm-plans/classes?pm_standard_id=undefined` (a malformed id, from my own script error) returns **500** instead of a 4xx validation error. The UI never sends that.
New screenshots: `QA-13Oct-ZP-4720-1-service-add-class-cable-listed-PASS.png`, `QA-13Oct-ZP-4720-2-pm-plan-picker-busway-listed-PASS.png`.
Recommended status unchanged: **READY TO RELEASE**. The duplicate-row observation (forked-standard class ids) is unchanged and is still a separate Low ticket suggestion.
