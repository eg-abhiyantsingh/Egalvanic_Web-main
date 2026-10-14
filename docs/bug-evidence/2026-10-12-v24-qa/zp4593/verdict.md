# ZP-4593: DC/solar equipment classes, AC/DC connection rule, and company-typed source voltages

**Verdict:** PARTIAL. The AC/DC rule, the DC/solar classes, the picker, typed-voltage find-or-create and the override lock all pass.
**One QA Review row fails as far as the web can see:** a node write accepts any `voltage_id`, even one that does not exist. Two rows are not covered: SKM round-trip and the AI-pipeline companion.
Build `index-C14cwR0U.js` (QA, checked 17:33 and 19:12 IST, 13 Oct 2026). Frontend PR #1634 is in the bundle (`connectionRules` `currentTypeMismatch`, `ac_dc_mismatch`, `find-or-create`, AC/DC picker).

Test data:
- Read only: the SLD tester's QA-DEMO fixture site "QA-DEMO ZP-480…" (SLD `363d5350-b42d-4e48-850f-b9f72d27ea08`), nodes "QA U2", "QA DC-BUS2", "QA DC-PV1", "QA INV1", "QA REC1", "QA G-Inverter", "QA G-Rectifier". No edge was created by me: the UI refused the AC→DC link, and the DC→DC dialog was cancelled.
- Writes, only on my own node "QA-DEMO ZP-4710 switchboard (delete me)" `cccad67b…` (voltage set, then reset to empty).

## Checked
| Check | How | Result |
|---|---|---|
| DC/solar classes exist | `GET /lookup/node-classes`, SLD +Asset picker | DC tab (14): DC Battery, DC Generator, DC PV Array, DC Circuit Breaker, DC Fuse, DC Bus, DC Combiner Box, DC Disconnect Switch, DC Node Bus, DC Cable, DC Capacitor, DC Load, DC Motor, Inverter. AC tab (51) has PV Array, Rectifier. All carry `current_type`. PASS (shots -1, -2) |
| Picker | Search "pv", group headers, symbols | Searchable, split AC/DC, grouped by role (SOURCE / PROTECTION / BUS / IMPEDANCE / LOAD / DC/AC CONVERSION), each with its symbol. PASS |
| Converter terminals | Class orientation | Rectifier "AC→DC": top AC (`ac`), bottom DC (`dc`). Inverter "DC→AC": top DC, bottom AC. PASS |
| AC→DC link in the SLD | Connection mode QA U2 (utility, AC) → QA DC-BUS2 | Refused with the toast "AC and DC equipment can't be connected directly — insert a rectifier (AC→DC) or inverter (DC→AC)." PASS (shot -3) |
| DC→DC link | QA DC-PV1 → QA DC-CB1 | The Connect dialog offers only "Insert DC Cable" / "Connect directly" (only cables of the right current type). PASS (shot -4) |
| `/edge/preview-rules` | AC utility → DC bus; inverter AC side → DC capacitor; DC→DC; rectifier DC side → DC; AC→AC | `current_type_mismatch:true, reason:"ac_dc_mismatch"` for the two mixed pairs; `false` for the others. PASS |
| create_edge never rejects + issues engine | Existing API-made U2→DC-BUS2 edge (created 12 Oct by the SLD tester, 201) | The edge is on the canvas, and the Issues panel shows the error "AC and DC equipment connected — QA U2 (AC) and QA DC-BUS2 (DC) are wired together…". PASS (shot -5) |
| Typed source voltage, find-or-create | `POST /enum-node-voltages/find-or-create` | 1000 → existing company 1000V (id 27, `created:false`); 480 → global id 8 (global first); 1500 → company id 28; 0.5 / 1,000,001 / −5 → 400 "Voltage must be between 1 V and 1,000,000 V"; "abc" → 400. PASS |
| Company-only list | `GET /enum-node-voltages` | Only globals + Acme's 4 custom values (1V, 1000V, 1500V, 1000kV). No other company's values appear. PASS (as far as one company can see) |
| **Node write with a voltage id that isn't the company's** | `PUT /node/update/<my node>` with `voltage_id` 99999 (does not exist), 2, 3, 7, 18, 19, 31, 40 (not in Acme's list) | **All 200, and the id is stored on the node (voltage and label then show empty).** A real id (27, 8) stores 1000 V / 480 V. FAIL (see B1) |
| Override class: current_type locked | Settings › Classes › DC Bus (override) | Current Type AC/DC buttons disabled, DC selected. PASS (shot -6) |
| DC breaker trip types; "Add Custom" gone | Create Asset › DC Circuit Breaker | Types "DC Breaker (Molded Case) / (w/ Trip Unit) / (w/o Trip Unit)" (an AC breaker shows Static Trip, Thermal Magnetic…). No "Add Custom" / "Custom (manual entry)" anywhere. PASS (shot -7) |
| Roles | PM / FM / EE / CP | PM, FM and EE can use find-or-create and preview-rules. CP gets 422 "Required: nodes.manage". PASS (but see lead L1) |
| Flags | Bundle + live evalx | SLD is behind `feature-gojs-sld` (ON). No new flag for this ticket. Nothing to flip |

## Bugs found
**B1 (Medium, backend), in scope ("a node write rejects another company's voltage id"): node writes accept any voltage id.**
1. As an admin, send `PUT /api/node/update/cccad67b-134c-4c34-9ceb-437c762b2a28` (my QA-DEMO switchboard) with `{"voltage_id": 99999}` (also tried 2, 3, 7, 18, 19, 31, 40: none are in Acme's voltage list).
2. Read the asset back (`GET /api/graph/nodes/<id>/enriched`).
- What happens: 200, and `voltage_id` is stored as sent, with no voltage or label behind it.
- What should happen: refused (400/422) when the id is unknown or belongs to another company. The ticket promises this check.
- Not proven: whether any of ids 2–40 belong to another company. QA can't list other companies' voltages. But if a non-existent id is stored, there is clearly no lookup on this path.
- Suggested assignee: Avani.

## Out-of-scope bugs (related, separate tickets)
**O1 (Low, frontend): the SLD Issues panel calls an Inverter and a Rectifier a "transformer".**
1. Open the fixture SLD (site "QA-DEMO ZP-480…") › Issues panel (shot -10).
2. The warnings "Transformer without secondary voltage on QA G-Inverter" and "… on QA G-Rectifier" say "This transformer has no secondary winding voltage set…".
- QA G-Inverter is of class Inverter (shot -8). Its Engineering tab has DC-input / AC-output fields, not windings (shot -9).
- Expected: wording for converters, e.g. "Inverter without AC output voltage".
- Cause: both classes have `primary_secondary_voltage=true`, so the transformer rule fires.
- Evidence: shots -8, -9, -10.

**Lead L1 (Low, needs a 2nd look before filing): `/edge/preview-rules` answers for an SLD the user is not assigned to.**
- Facility Manager (11 sites) and Electrical Engineer (4 sites) got 200 JSON (current types and roles of two nodes) for the fixture SLD they don't have.
- Project Manager has that site, so PM is the positive control.
- The route is older than this ticket. It is listed here because the ticket added `current_type` to its answer.

## Enhancement suggestions
- Name check: the ticket lists "PV Array (DC)" and "AC PV Array". On Acme the classes are "DC PV Array" (DC) and "PV Array" (AC). Confirm this naming is intended. Users may expect "PV Array" to be the DC solar array.
- Show the AC/DC badge on each node in the canvas, so a mixed connection is visible before you draw it.

## Mobile part for Nency
iOS twin ZP-4598 + the SLD viewer OTA. The iOS embedded viewer must:
- refuse AC↔DC links with the same toast;
- show the DC classes and "Convert to Node Bus / Panel";
- raise the AC/DC issue.
The viewer bundle has to be republished per env after the FE merge.

## Not covered / NOT WEB-TESTABLE
- SKM XML import/export round-trip of the DC/solar types (incl. PGEN GeneratorType=Solar PV ↔ AC PV Array). The SKM tester covers SKM; not run here.
- AI pipeline companion (#141): DC trip subtypes in the asset agent and company-scoped voltage enums for the agent. That is AI-pipeline only; dev to confirm.
- Migration counts (Rectifier/Inverter/PV remaps): dev to confirm with the dry-run numbers on QA.

## Proposed Jira comment
h3. QA: ZP-4593 mostly passes, one row fails (QA build index-C14cwR0U.js, 13 Oct)

What works:
# +Asset is split into AC (51) and DC (14) tabs, searchable and grouped. All DC/solar classes are there (DC Bus, DC Node Bus, DC Combiner Box, DC Cable, DC breaker/fuse/disconnect, DC motor/load/capacitor/generator, DC PV Array, Inverter, Rectifier).
# Drawing a utility (AC) to a DC bus is refused with "AC and DC equipment can't be connected directly — insert a rectifier or inverter". DC to DC only offers a DC cable.
# {{/edge/preview-rules}} returns {{current_type_mismatch: true}} for AC↔DC. An AC/DC edge made through the API is kept, and the Issues panel shows "AC and DC equipment connected".
# Typed voltages (API): 1000 V finds the company value, 480 V finds the global one, 0.5 V and 1,000,001 V are refused.
# On an override class (DC Bus) the AC/DC switch is locked. DC breakers show DC trip types. "Add Custom" is gone.

What fails:
# Send {{PUT /api/node/update/<asset id>}} with {{"voltage_id": 99999}} (an id that does not exist) on my QA-DEMO asset.
# *Actual:* 200, and 99999 is saved on the asset. Ids 2, 3, 7, 18, 19, 31, 40 (not in Acme's list) are saved too.
# *Expected:* the write is refused when the voltage id is unknown or belongs to another company (QA Review row 3).

!QA-13Oct-ZP-4593-2-class-picker-dc-tab-dc-solar-classes-PASS.png|width=720!
!QA-13Oct-ZP-4593-3-ac-to-dc-link-refused-with-toast-PASS.png|width=720!
!QA-13Oct-ZP-4593-5-issues-panel-ac-dc-equipment-connected-PASS.png|width=720!

Not covered yet: SKM XML round-trip of the DC types, and the AI-pipeline part (#141). Side note, to file separately: the Issues panel calls Inverter and Rectifier nodes "transformer without secondary voltage".

*Status:* stays In QA

## Recommended status
Stay In QA. B1 is very likely a real gap, but "another company's id" can't be proven from one tenant, and SKM + AI-pipeline rows are still open. Move to To Do if the dev confirms the node write has no company check.
