# Bug audit C: skeptical developer review of 9 bugs filed 13 Oct 2026

- **Build:** QA `index-C14cwR0U.js` (checked with curl at the start and in every browser run). Runs were 20:06–20:21 IST, 13 Oct 2026, in headed Chrome through `pwslot.sh`.
- **Scripts:** `.playwright-mcp/v24/audit-c/r1.js … r9.js`. Outputs are in `.playwright-mcp/v24/audit-c/out/`.
- **Fresh evidence:** `docs/bug-evidence/2026-10-13-bug-audit/` (files that start with `4867-` … `4882-`).
- **Seats:** customer seats PM, FM and CP (`is_eg_admin=false`, printed in each run). Super Admin (staff) was used only for the builder (ZP-4870) and for the original ZP-4880 asset, which customer seats cannot open.
- **Jira:** read only. Every ticket was read live (description, status, comments, attachments). None of the 9 had a comment yet. No Jira writes, no Chat, no git.
- **Data changes and clean-up:**
  - ZP-4877 probes created 3 nodes and 1 self-edge on the QA-DEMO ZP-4800 SLD. All of them were deleted afterwards: `bulk-delete` → 200, the edge was set to `is_deleted` → 200, and a graph re-check shows 0 leftovers and 0 self-loops.
  - ZP-4868 and ZP-4870 opened forms but did not save them (no eg-form-instance writes).
  - One exception on ZP-4870: opening the builder sent its own `PUT /api/eg-forms/dc24bb98…`. That is our own QA-DEMO form.
  - ZP-4880 Save calls returned 400, so nothing was stored.
  - ZP-4876 used the SKM preview only. Nothing was committed.

## Summary

| Bug | Verdict | One line |
|---|---|---|
| ZP-4867 | **DOUBTFUL** | Reproduces (CP gets 246 names), but the CP role holds `forms.view` and `form_instances.view`, and portal asset and issue pages mount the form renderer that uses this list. So "no screen uses it" is not proven, and this may be by design. |
| ZP-4868 | **VALID-CLEAR** | Reproduces: "abh" gives 25 matches, and after one tick the box is empty and the list shows all 246. ZP-4753's own QA step says "Tick several filtered options". |
| ZP-4869 | **VALID-CLEAR** | Reproduces in a fresh ARCH-D export: the JBOX left-terminal tag reads "-DEMO 4591 SRC CB (delete me)" because its start is under the black symbol. |
| ZP-4870 | **VALID-NEEDS-EDIT** | Reproduces, but the builder preview does load datasets: Test Equipment shows 10 items. Only the **Users** source is missing from the builder's dataset list. The ticket's control and dev note are wrong. |
| ZP-4871 | **VALID-NEEDS-EDIT** (minor) | Reproduces: 3 rows from 2 forms are shown under one name. Keyless forms are real (33 of 658 forms on acme QA are not QA-DEMO). Replace the unproven "created through the API" note. |
| ZP-4876 | **VALID-NEEDS-EDIT** | The bug is real and bigger than filed. But the steps say "unchanged full export" while the screenshots and numbers come from a hand-trimmed 4-asset file. The real full export has 94 components and changes **12** asset classes. |
| ZP-4877 | **INVALID** (as written) | The AC/DC half is **documented by-design** in ZP-4593: "create_edge never rejects (offline-sync precedent ZP-2577), the SLD issues engine flags connection.ac_dc_mismatch instead". The self-loop half is API-only and the same "never reject" policy probably covers it, so it is DOUBTFUL at best. |
| ZP-4880 | **VALID-NEEDS-EDIT** | Reproduces with the exact raw text, and the date box has no `max`. But the ticket names no role or site, and its asset gives a Project Manager "This asset link doesn't look right". It needs reachable steps. |
| ZP-4882 | **VALID-NEEDS-EDIT** | The warning text is wrong ("Transformer", "winding"). But the claim that the user "can't fix" it is false: Edit Asset on an Inverter has a **Secondary Voltage** field. This is a wording-only issue. |

---

## ZP-4867: Client Portal can download the forms "Users" list. Verdict: DOUBTFUL

**What I reproduced** (r1, evidence `4867-1-cp-users-list-vs-other-user-endpoints.png`):

| Seat | is_eg_admin | `GET /api/eg-form-sources/users` | `GET /api/test-equipment` |
|---|---|---|---|
| Client Portal | false | 200, 246 rows, 57 names are emails, fields `id,is_current,name` | 422 permission_denied |
| Facility Manager | false | 200, 246 | 200, 10 |
| Project Manager | false | 200, 246 | 200, 10 |

- `/eg-forms` and `/sessions` show "Access Denied" for CP. That matches the ticket.

**Why a developer will push back:**
1. **The CP role is granted form permissions.** Its permission list includes `forms.view`, `form_instances.view` and `sessions.view`, so viewing filled forms is part of the CP role.
2. **The renderer that uses this list is reachable by CP.**
   - `EGFormRendererV2` is imported by `AssetDetails`, `IssueDetail`, `Issues`, `ConditionAssessment` and others.
   - The Maintenance Portal has `/maintenance-portal/assets/:assetId`, `/issues/:issueId`, `/work-orders` and `/reports`.
   - The renderer calls this endpoint to turn a Users-field answer (user id) into a name.
   - So the line "the Client Portal has no screen that uses this list" is not proven. I did not find a CP issue or asset whose form has a Users field, so I could not prove the opposite either.
3. **The comparison with test-equipment is weak.** That endpoint is gated by `procedures.view`, which is a different permission family.
4. **No email field is exposed.** The 57 "emails" are users who have no display name. That is a data-quality point, not a field leak.

**Duplicate:** none found (JQL on `eg-form-sources`, "user list").

**Recommendation:** do not push as a bug as written. Ask the product owner one question: "Should a Client Portal contact be able to list all 246 company users (including Egalvanic staff logins shown as emails)?"
- If **no**: reword it as a scoped privacy request. Return only the users referenced by forms the CP can view, and never list `is_eg_admin` staff. Keep it Low.
- If **yes**: withdraw it.

---

## ZP-4868: Form multi-pick clears the search after each tick. Verdict: VALID-CLEAR

**What I reproduced** (r2, PM, WO `22a22b9c…`, asset row 13N-H1-1, field M1):
- Typing "abh" narrows the list to **25** options.
- After ticking "abhiyant admin", the input value is `""` and the list shows **246** options.
- Evidence: `4868-1-typed-abh.png`, `4868-2-after-tick-search-cleared.png`.
- No save was made (0 eg-form-instance writes).

**By-design check:**
- MUI Autocomplete clears the input on select by default, so a developer may call this "standard behaviour".
- But ZP-4753's own QA Review says "type a partial string … **Tick several filtered options**". That is the intended flow, and today it needs a re-type for every pick.

**Reachability:** UI, any form-filling role. Low and usability-only, which the ticket already says.

**Duplicate:** none.

**Clarity:** good. Optional improvement: in "What should happen", quote ZP-4753's step "Tick several filtered options" instead of the "clear the search text" check, which is a weaker argument.

---

## ZP-4869: SLD PDF "TO" tag on a left terminal is drawn under the symbol. Verdict: VALID-CLEAR

**What I reproduced** (r9, PM):
- I triggered the same export the Export PDF button sends: `/sld/export/pdf/start` with `{arch_size:"arch-d", omit_text:false, engineering_mode:true}`, which is the payload the original UI run captured.
- The graph confirms the hidden edge: SRC CB `bottom-source-0` → JBOX `left-target-0`, `edge_manips={"hidden":true}`.
- In the downloaded PDF the JBOX tag reads "-DEMO 4591 SRC CB (delete me)". Its start runs under the filled "JB" symbol.
- The SRC CB end shows "TO QA-DEMO 4799 JBOX (delete me)" in full.
- Evidence: `4869-1-pdf-jbox-left-terminal-tag-under-symbol.png`, `4869-2-pdf-wide-view-control-src-cb-tag-readable.png`, `4869-sld-export-audit-13oct.pdf`.

**By-design:** no. The canvas draws the tag pointing left.

**Reachability:** UI (Export › Export PDF).

**Duplicate:** none. ZP-3839 is about label size, which is a different problem.

**Clarity:**
- The steps and screenshots match.
- The JBOX position on the sheet moved since the original run because other testers changed the diagram, but the defect is identical.

---

## ZP-4870: Builder Web View shows dataset fields empty. Verdict: VALID-NEEDS-EDIT

**What I reproduced** (r3, Super Admin, form "QA-DEMO ZP-4751 ZP-4753 dataset type-to-search (delete me)", Edit Form › Web View):

| Field | Options in preview |
|---|---|
| S1 Users select | 1 ("Select...") |
| M1 Users multiselect | 0 |
| **S2 Test equipment select (dataset)** | **11** ("Select...", "test (3445)", "DLRO (56654)", …) |

- Evidence: `4870-1-web-view-users-select-empty.png`, `4870-2-control-web-view-test-equipment-loads.png`.
- **Code check:**
  - The builder chunk `EGFormBuilderV2-kgrwB9vv.js` has its own dataset hook (`So()`) that fetches dataset options for the preview.
  - Its list of sources contains only `test_equipment`.
  - The renderer's list (`EGFormRendererV2`) has both `test_equipment` and `users` (`/eg-form-sources/users`).

**Why the current text gets rejected:**
- The ticket says the builder preview "does not fetch the dataset options (eg-form-sources)" and uses a static select as its control. Both are misleading: the preview **does** load datasets, and only the Users source is missing.
- A developer who reads the current note may look in the wrong place.
- The builder's "Options source" picker also offers only Test Equipment and Linked Asset. Users-sourced fields cannot be made in the web builder, so the form was built through the API. That is worth saying plainly.

**Reachability:** UI, form authors (forms.manage), Low.

**Duplicate:** none.

**Replacement description (Jira wiki markup):**
```
h3. What happens
In the form builder, *Web View* preview, a field that takes its options from the company *Users* list shows no options ("Select..." only; the multi-pick shows 0). A *Test Equipment* dataset field in the same preview lists its 10 items, so the preview does load datasets - only the Users source is missing.

h3. Steps
# Sign in as a Super Admin. Go to *Builder > Forms* (https://acme.qa.egalvanic.ai/eg-forms), search "QA-DEMO ZP-4751" and open "QA-DEMO ZP-4751 ZP-4753 dataset type-to-search (delete me)".
# Click the pencil *Edit Form*, then *Web View*.
# Open *S1 Users select (dataset, 246 rows)*: only "Select..." is listed. Open *M1 Users multiselect (dataset)*: 0 options.
# Control, same preview: open *S2 Test equipment select (dataset)*: it lists the company's 10 test-equipment items.
# Control, real form: the same S1 field inside work order "QA-DEMO ZP-4801 Tuned column (delete me)" > More > Forms > 13N-H1-1 lists all 246 users.

h3. What should happen
Users-sourced fields show the same options in Web View as in a real work order, like Test Equipment fields already do.

h3. Notes
* QA build index-C14cwR0U.js, 13 Oct 2026. Low - preview only; real forms are fine.
* For the developer: the builder's preview dataset list (EGFormBuilderV2, hook that loads options_source) only knows test_equipment; the renderer (EGFormRendererV2) also has users -> /eg-form-sources/users. The builder's Options-source picker also does not offer "Users", so such fields can only be created outside the web builder today.
```

---

## ZP-4871: Keyless forms are grouped under another form's name. Verdict: VALID-NEEDS-EDIT (minor)

**What I reproduced** (r2, PM, WO `22a22b9c…` › More › Forms):
- "Form Types" shows one entry, "QA-DEMO ZP-4717 visible normalization (delete me)", with **3** rows.
- `by-session` returns 3 instances from **2** forms: `e293697b` ×1 and `dc24bb98` ×2. All have `eg_form_key = null`.
- The first 13N-H1-1 row opens "QA-DEMO ZP-4751 ZP-4753 dataset type-to-search (delete me)".
- Evidence: `4871-1-wo-forms-tab-form-types.png`, `4871-2-row-opens-other-form.png`.

**Is the test data an artefact?**
- Partly, because the two QA-DEMO forms were created by QA.
- But `GET /api/eg-forms` (PM) returns 658 forms: **40 have no key, and 33 of those are not QA-DEMO**. Examples: "Untitled Form" ×4 (Apr–May 2026), "70B-DA", "Test eg form 1", "TESYASHASKJA-final", "new 2".
- A user can add any available form to an asset in a WO. The web uses `available-for-node` and then `create-for-asset` with `eg_form_ids`, which does not need a key.
- So a real WO can hold two keyless forms.

**Duplicate:** none. ZP-3880 covers a different place, as the ticket says.

**Edit (replace the second Notes bullet):**
```
* Not only QA data: on acme QA, 40 of 658 forms have no key and 33 of them are not QA-DEMO (e.g. "Untitled Form" x4, "70B-DA", "Test eg form 1"). Any of them can be added to an asset on a work order (Add form picker uses the form id, not the key), so a work order can end up with two keyless forms.
```

---

## ZP-4876: SKM re-import of an unchanged export changes asset classes. Verdict: VALID-NEEDS-EDIT

**What I reproduced** (r5, PM, site QA-DEMO ZP-4800, SLD `363d5350…`):
- I took the **full** unchanged export (`POST /sld/{id}/export-skm`, 94 components) and ran it through `POST /skm/import-xml/preview`. Nothing was committed.
- Then I did the same file in the UI: Assets › SKM › Import (Full) › Analyze. The review says **"94 Updated … Update 94 assets"**. Evidence: `4876-1-full-export-reimport-review.png`.
- Comparing the preview's class with each asset's current class gives **12** class changes. 82 assets are unchanged.

| Asset | Current class | Class after import | SKM type |
|---|---|---|---|
| QA G-PDU | PDU | Panelboard | Bus |
| QA G-Other | Other | Panelboard | Bus |
| QA G-MVSwitch | MV Switch | Panelboard | Bus |
| QA-DEMO 4799 JBOX (delete me) | Junction Box | Panelboard | Bus |
| QA G-SeriesCap | Capacitor | Capacitor Bank | Pi |
| QA G-MotorStarter | Motor Starter | Motor Controller | MotorController |
| QA G-Arrester | Lightning Arrester | Load | Load |
| QA G-ShuntReactor | Shunt Reactor | Load | Load |
| QA G-InstrXfmr | Instrument Transformer | Load | Load |
| QA MC1 | Motor Controller | Load | Load |
| QA G-MotorController | Motor Controller | Load | Load |
| QA-DEMO ZP-4514 MC (delete me) | Motor Controller | Load | Load |

- The ticket's author proved on the same build that committing really changes the class. Run `.playwright-mcp/v24/sld4603/out/e2.txt` committed 4 assets and the classes changed. They were then reverted.

**Why the current text gets rejected:**
- Step 2 says "Export Engineering XML … do not edit it", and step 3 says to import "the same file". But Screenshot 3 shows SKM project "QA-DEMO ZP-4603 roundtrip (delete me)" with only **4** components and "Update 4 assets".
- That was a hand-made 4-asset file (`roundtrip-4.xml`). Anyone who follows the steps sees "Update 94 assets" and different chips, so the numbers in the ticket don't match the steps.
- The ticket also lists only 3 changes. The real impact is 12, including protective and other gear becoming **Load**.

**By-design check:**
- Export-side loss is partly expected: the export dialog lists "unconfigured classes", and the SKM guidance says unmapped classes import "as generic loads".
- But nothing documents that a **re-import onto matched EG assets** overwrites their class. The review banner says only that "Engineering values … overwrite", and class is not an engineering value.

**Duplicate:**
- None found (JQL "SKM").
- ZP-4851, ZP-4852, ZP-4853 and ZP-4848 are different SKM issues.
- ZP-4854 is the Bulk Edit round trip.
- ZP-2689 ("SKM EXPORT Class Mapping Defaults", Backlog) is about export defaults with an empty description. Link it as related.

**Replacement description:**
```
h3. What happens
Re-importing an SKM file that was just exported from the same site, without editing it, silently changes the class of existing assets. On the test site 12 of 94 assets change class (e.g. Capacitor -> Capacitor Bank, Motor Starter -> Motor Controller, PDU / Other / MV Switch / Junction Box -> Panelboard, and Lightning Arrester / Shunt Reactor / Instrument Transformer / Motor Controller -> Load). The review screen does not warn that the class will change.

h3. Steps
# Sign in to https://acme.qa.egalvanic.ai as a Project Manager. Pick site "QA-DEMO ZP-4800 SLD canvas v2.4 (delete me)". Open *Site Data > Assets* and note the classes: QA G-SeriesCap = *Capacitor*, QA G-MotorStarter = *Motor Starter*, QA G-PDU = *PDU*, QA G-Arrester = *Lightning Arrester*.
# Open *Site Data > SLD*, click *Export > Export Engineering XML*, confirm, and save the file. Do not open or edit it.
# Go to *Site Data > Assets > SKM > Import (Full)*, choose that file, click *Analyze*. The review says "94 Updated" and the button reads *Update 94 assets*. In the table, QA G-SeriesCap shows Class "Capacitor Bank", QA G-MotorStarter "Motor Controller", QA G-PDU "Panelboard" - but there is no "class will change" warning.
# (Optional, changes data - restore afterwards) Click *Update 94 assets* and open the assets again: the 12 assets in the table below now have the new class.

||Asset||Class before||Class after re-import||
|QA G-PDU / QA G-Other / QA G-MVSwitch / QA-DEMO 4799 JBOX|PDU / Other / MV Switch / Junction Box|Panelboard|
|QA G-SeriesCap|Capacitor|Capacitor Bank|
|QA G-MotorStarter|Motor Starter|Motor Controller|
|QA G-Arrester / QA G-ShuntReactor / QA G-InstrXfmr|Lightning Arrester / Shunt Reactor / Instrument Transformer|Load|
|QA MC1 / QA G-MotorController / QA-DEMO ZP-4514 MC|Motor Controller|Load|

h3. What should happen
An unchanged round trip changes nothing. When an imported row matches an existing EG asset (by Alternate UID), the asset keeps its class - or the review lists each "class will change from X to Y" and lets the user keep the current class.

h3. Notes
* QA build index-C14cwR0U.js, 13 Oct 2026. Preview checked on the full 94-component export; commit proven on 4 of these assets (then reverted).
* Why it matters: export -> fix a few values in SKM -> import back is the documented workflow; it would turn breakers' neighbours, arresters and starters into Loads/Panelboards and change their symbols, forms and maintenance plans.
* For the developer: several EG classes share one SKM type (Pi, Bus, Load, MotorController) and on update the importer writes its default class for that type over the existing class. Related: ZP-2689 (export class mapping defaults), ZP-4854 (Bulk Edit round trip).
```

---

## ZP-4877: Server accepts self-loop and AC/DC cables. Verdict: INVALID (as written)

**What I reproduced** (r6, PM, `is_eg_admin=false`, QA-DEMO ZP-4800 SLD). Evidence: `4877-1-api-accepts-self-loop-and-ac-dc-cable.png`.

| Call | Result |
|---|---|
| `preview-rules` QA PNL2 (AC) → QA DC-CAP1 (DC) | 200, `reason: ac_dc_mismatch`, `current_type_mismatch: true` |
| `preview-rules` PNL-A → PNL-A | 200, `reason: bus_to_bus_no_blocker`. **Not** a "same asset" refusal. |
| `insert-pseudo-edge` (Cable) PNL-A → PNL-A | 201 |
| `insert-pseudo-edge` (Cable) PNL2 → DC-CAP1 | 201 (creates a cable node and a node bus) |
| `edge/create` (direct) PNL-A → PNL-A | 201 |

- Everything was cleaned up and the graph re-check showed 0 leftovers.
- The SLD Issues panel on the same site already lists "AC and DC equipment connected QA U2 (AC) and QA DC-BUS2 (DC)…".

**Why it is invalid:**
- **The AC/DC half is documented design.** ZP-4593 (READY TO RELEASE) says: "/edge/preview-rules reports current_type_mismatch; **create_edge never rejects (offline-sync precedent ZP-2577), the SLD issues engine flags connection.ac_dc_mismatch instead**."
- Its QA Review also says: "confirm … the edge is still created (**not rejected**)".
- The ticket asks for the exact opposite of the designed behaviour.

**The self-loop half:**
- It is API-only.
- The app's rule check does not treat it as "same asset" (`bus_to_bus_no_blocker`).
- The same "the server never rejects, for offline sync" policy very likely covers it.
- The claim that iOS or the offline queue sends `insert-pseudo-edge` was not verified.

**Recommendation:**
- Withdraw the AC/DC part.
- If the owner wants the self-loop kept, refile it as a Low "Issues panel should flag a connection from an asset to itself", which fits the design (flag, don't reject). Do not ask for a 400 from the server.
- Self-loop check in the Issues panel: not verified.

**Duplicate:** none.

---

## ZP-4880: Add Service History with a future date shows the raw API error. Verdict: VALID-NEEDS-EDIT

**What I reproduced** (r4 and r6):
- **Super Admin**, asset FDR-Circuit Breaker-3 (`713ff57d…`):
  - Maintenance › + Add record › Service "Cleaning" › Last serviced 2026-10-14 › Save.
  - Result: `PUT /api/asset-maintenance/node/713ff57d…/history` → 400.
  - The dialog shows exactly: `API call failed: 400 - { "error": "last_serviced_on cannot be in the future — it records work that has already happened", "success": false }`.
  - Evidence: `4880-1-future-date-accepted-by-picker.png`, `4880-2-raw-api-error-after-save.png`.
- **Project Manager** (customer, `is_eg_admin=false`), site QA-DEMO ZP-4800, asset QA PNL1 (`4b84bf4a…`): same steps give the same raw error. Evidence: `4880-3-pm-customer-seat-same-raw-error.png`.
- **The date picker does not block future dates.** It is a plain `<input type="date">` with `max=null` (live DOM). The `AssetDetails` chunk confirms the field has no max.

**Why the current text gets rejected:**
- No role and no site are given.
- The asset in step 1 opens "This asset link doesn't look right" for a Project Manager. Only the staff Super Admin can open it.
- "Pick 14/10/2026" is tied to the date it was written.
- Also, "FDR-Circuit Breaker-3" is a **Cable**-class asset, so its name is misleading for a developer.

**By-design:** the server refusal is correct and the ticket says so. The raw text is the bug, the same family as ZP-4730, ZP-4714 and ZP-4412.

**Duplicate:** none found (`last_serviced_on`, "Service History", "API call failed").

**Replacement Steps section:**
```
h3. Steps
# Sign in to https://acme.qa.egalvanic.ai as a Project Manager (e.g. abhiyant.singh+project@egalvanic.com). In the site picker choose "QA-DEMO ZP-4800 SLD canvas v2.4 (delete me)".
# Go to *Site Data > Assets*, search "QA PNL1" and open it (https://acme.qa.egalvanic.ai/assets/4b84bf4a-ad24-4c31-944d-5d65ba29a846).
# Click the *Maintenance* tab. On the right, under *Service history*, click *+ Add record*. (Screenshot 1)
# In *Add Service History* pick Service = *Cleaning*. (Screenshot 2)
# In *Last serviced* pick tomorrow's date. The date box accepts it - future days are not greyed out.
# Click *Save*. (Screenshot 3, close-up Screenshot 4)
```
- Add to Notes: "Also reproduced as Super Admin on asset FDR-Circuit Breaker-3 (https://acme.qa.egalvanic.ai/assets/713ff57d-288d-47b3-b7b4-ef12958f0b9c), which a Project Manager cannot open. QA build index-C14cwR0U.js. The Last serviced box is a plain date input with no max date."
- Keep the rest of the ticket as it is.

---

## ZP-4882: SLD Issues panel calls an Inverter and a Rectifier "Transformer". Verdict: VALID-NEEDS-EDIT

**What I reproduced** (r5, r8, PM, `is_eg_admin=false`, QA-DEMO ZP-4800 SLD):
- In the Issues panel (18 issues) these lines appear:
  - "Transformer without secondary voltage on QA G-Inverter"
  - "… on QA G-Rectifier"
  - "… on QA INV1"
  - "… on QA REC1"
- The detail text is "This transformer has no secondary winding voltage set…". Evidence: `4882-1-sld-issues-inverter-rectifier-called-transformer.png`.
- Classes: Inverter and Rectifier both have `primary_secondary_voltage=true`, the same as Transformer. The frontend rule `goJSDataService` fires on `primary_secondary_voltage || tertiary_voltage` together with `secondary_voltage_id == null`.
- **Key finding:** on QA G-Inverter, the ⋮ menu › **Edit Asset** › ENGINEERING shows **Primary Voltage, Secondary Voltage**, kVA Rating and % Impedance. Evidence: `4882-2-inverter-edit-has-secondary-voltage.png`.
- So the field the warning checks exists, and setting it clears the warning.

**Why the current text gets rejected:**
- The ticket says the asset pages "have no transformer windings to fill in, so the user can't understand or fix the warning". Step 4 says the Engineering tab has "no secondary winding field".
- That only looks at the read-only Engineering tab. A developer will open Edit Asset, see Secondary Voltage, and reject the bug as "not a bug".
- The real defect is only the wording: "Transformer" and "winding" for a converter.

**By-design:** modelling converters as primary/secondary is design (ZP-4593: Rectifier AC LINE / DC LOAD, Inverter DC LINE / AC LOAD). The transformer wording is not.

**Duplicate:** none.

**Replacement description:**
```
h3. What happens
On the SLD the *Issues* panel shows "Transformer without secondary voltage on QA G-Inverter" (and on QA G-Rectifier, QA INV1, QA REC1), with the text "This transformer has no secondary winding voltage set". These assets are an Inverter and a Rectifier, not transformers, so the wording is confusing. The fix the warning wants (set the Secondary Voltage) is in *Edit Asset*, but the message never says so.

h3. Steps
# Sign in to https://acme.qa.egalvanic.ai as a Project Manager. Pick site "QA-DEMO ZP-4800 SLD canvas v2.4 (delete me)" and open *Site Data > SLD*.
# Click the issues box at the top right ("3 / 15 / 18 issues") to open the list. Find "Transformer without secondary voltage on QA G-Inverter" and "... on QA G-Rectifier". (Screenshot 1)
# Open asset QA G-Inverter: Asset Class = *Inverter*. (Screenshot 2)
# Click the ⋮ menu (top right) > *Edit Asset*. Under ENGINEERING the form has *Primary Voltage* and *Secondary Voltage* - this is the value the warning checks. (Screenshot 3)

h3. What should happen
The warning names the real class and the field, e.g. "Inverter without output voltage on QA G-Inverter - set Secondary Voltage in Edit Asset" (Rectifier: "Rectifier without output voltage ..."). Transformers keep today's text.

h3. Notes
* QA build index-C14cwR0U.js, 13 Oct 2026. Wording only - the check itself is right. Low.
* For the developer: Transformer, Inverter and Rectifier all have primary_secondary_voltage = true, and the issue builder uses one transformer title/detail for all of them (code voltage.secondary_missing).
```
- Replace the screenshots too: keep 1 and 2, and use `4882-2-inverter-edit-has-secondary-voltage.png` instead of the Engineering-tab one.
