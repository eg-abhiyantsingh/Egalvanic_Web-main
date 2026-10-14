# ZP-4603 — Task #88: Distinct SLD symbols for more classes + converter placement in the class picker

**Verdict:** PASS (every QA Review row that the web can show). One **out-of-scope** bug found while doing the SKM round-trip row (re-import of an unchanged export changes asset classes).
**Build:** QA `index-C14cwR0U.js` (live 17:32 and 18:11 IST, 13 Oct).
**Tested:** 13 Oct 2026, 17:35–18:10 IST. Project Manager (main), Super Admin, Electrical Engineer, Client Portal.
**Test data (mine):** site "QA-DEMO ZP-4800 SLD canvas v2.4 (delete me)" (`363d5350-b42d-4e48-850f-b9f72d27ea08`) with one gallery asset per class (`QA G-*`).
**Ticket re-read live:** In QA, 0 comments.
**Flags:** no LaunchDarkly gate on this ticket. Symbols come from the database (`node_icons`), so they show in both editors. The canvas is the GoJS editor (`feature-gojs-sld` live ON; OFF was already simulated on ZP-4800).

## Checked

| Check | How | Result |
|---|---|---|
| Load not a motor; 3-Winding vs 2-Winding transformer; Series vs Shunt Reactor; Lightning Arrester | Canvas, gallery row (shot 1) + `/node_classes/user/{sub}` icon names | PASS. Icons `load`, `transformer3w`, `transformer`, `seriesReactor`, `shuntReactor`, `lightningArrester` are all different. |
| Lightning Arrester ends the line | Class terminals | PASS. Role endpoint, 1 in / 0 out, one terminal "In". **Question:** the ticket says "with a Protected output"; no terminal is named "Protected" on QA. Dev to confirm the label. |
| Instrument Transformer has a secondary output | Terminals + canvas | PASS. Terminals Top (LINE), Bottom (LOAD), **Sec (LOAD, right side)**; the right-side dot shows on the canvas (shot 2). |
| Capacitor Bank grounded, different from Series Capacitor; Relay SKM style; Meter | Canvas (shot 2) + SVG | PASS. `capacitorBank` has the 3-line ground; Capacitor uses `capacitor`; Relay uses `relaySkm` (contact + R). |
| PDU, Panelboard, Other; boxed Disconnect / MV / DC Disconnect | Canvas (shot 7) | PASS. `pdu`, `panelboard`, `other`, `switchEnclosure`, `switchEnclosureMv`, `switchEnclosureDcMark` all different. |
| DC mark on DC Circuit Breaker / DC Fuse / DC Disconnect; grounded DC Capacitor; 3 clean dashes | Canvas (shot 3) + SVG | PASS. Solid bar + dashed bar; the dash is `stroke-linecap="butt"` with `dasharray 0.95 0.57` over 4 units = exactly 3 dashes. DC Capacitor has the ground. |
| Renamed classes | Class list + picker | PASS. key `ac-pv-array` → "PV Array" (AC), key `pv-array` → "DC PV Array" (DC), key `battery` → "DC Battery" (DC). No class named "Battery" or "AC PV Array" is left. Keys unchanged. |
| No two of the ticket's classes share an icon | Grouped all 65 classes by icon | PASS for the ticket's classes. Still shared (not in this ticket): Motor + Motor Starter, Circuit Breaker + Other (OCP), Switchboard + MCC Bucket + DC Bus, Junction Box + DC Combiner Box, VFD + VFD Panel. |
| Rows on the old global icon moved; custom icon choices kept | Acme overrides | PASS for moved rows: every Acme override of the ticket's classes uses the new icon. **Custom icon kept: NOT WEB-TESTABLE.** Needs a company whose override had a custom icon before the migration. Dev to confirm on the migration dry-run. |
| + Asset picker, AC tab | Unlock › + Asset | PASS. "AC/DC CONVERSION › Rectifier" is its own heading; "Other Equipment" no longer holds Rectifier or Inverter (shot 4). |
| + Asset picker, DC tab | DC tab | PASS. "DC/AC CONVERSION › Inverter" (shot 5). |
| French | `userPreferredLanguage=fr` | PASS. "CONVERSION CC/CA" on the CC tab (shot 6). Class names stay English (they are data). |
| AC/DC rule still works (current_type unchanged) | `POST /edge/preview-rules` + UI | PASS. DC Bus → Inverter allowed (dc/dc). AC Panel → DC Capacitor refused `ac_dc_mismatch`. DC Breaker → AC Load refused. Panel → Rectifier and Rectifier → DC Disconnect allowed. In the UI, AC → DC shows "AC and DC equipment can't be connected directly — insert a rectifier (AC→DC) or inverter (DC→AC)." Inverter is still `current_type: ac`. |
| SKM export with the new names | Export › Export Engineering XML (UI, both acknowledgements) | PASS. DC Battery → `<s:Battery>`, PV Array → `<s:SynchGenerator GeneratorType="Solar PV">`, DC PV Array → `<s:DCGenerator>`, Capacitor Bank → `<s:Pi PiType="Series Capacitor">`. |
| SKM import maps to the new names | `POST /skm/import-xml/preview` with a crafted file (5 new assets, preview only) | PASS. Battery → **DC Battery**, SynchGenerator Solar PV → **PV Array**, DCGenerator → **DC PV Array**, Protection "Motor Overload" → Motor Overload. |
| Old names still import | — | NOT WEB-TESTABLE. Old names live only in the backend import rules. Dev to confirm with a unit test. |
| Round trip (export, then import the same file) | Assets › SKM › Import (Full) with the unchanged export of 4 assets, then commit | **Out-of-scope bug 1** (below): 3 of 4 assets change class. I set the classes back afterwards. |
| Roles | Super Admin, EE, CP | Super Admin sees the same symbols (ZP-4513 shot 2). EE uses the same company classes. Client Portal: `/slds` = Access Denied (ZP-4800). |
| Performance | SLD load, PM | 37 API calls, 8.8 s to a drawn canvas, graph 1.0 s, nothing over 2 s. Duplicates are app-wide (`translations ×4`, `plug-config ×2`), not this ticket. |

## Bugs found (in scope)
None.

## Out-of-scope bug (found on the round-trip row; not caused by this ticket's migrations)
### 1 (Medium): Re-importing an unchanged SKM export changes the class of existing assets
Steps (Project Manager, QA, build `index-C14cwR0U.js`, 13 Oct):
1. Open site "QA-DEMO ZP-4800 SLD canvas v2.4 (delete me)" › SLD. The assets QA G-SeriesCap (class **Capacitor**), QA G-MotorStarter (**Motor Starter**) and QA G-PDU (**PDU**) show their own symbols (Screenshots 1 and 2: `QA-13Oct-ZP-4603-2-…`, `QA-13Oct-ZP-4603-7-…`).
2. Export › Export Engineering XML. Keep these 4 assets in the file and change nothing (QA G-SeriesCap, QA G-MotorStarter, QA G-PDU, QA G-CapBank).
3. Site Data › Assets › SKM › Import (Full) › choose the file › Analyze. The review screen already lists "Capacitor Bank: 2, Motor Controller: 1, Panelboard: 1" (Screenshot 3: `QA-13Oct-ZP-4603-x1-FAIL-skm-reimport-review-unchanged-export.png`).
4. Click "Update 4 assets", then open the SLD again.
- **What happens:** QA G-SeriesCap is now a **Capacitor Bank**, QA G-MotorStarter a **Motor Controller**, QA G-PDU a **Panelboard**. The symbols change too (Screenshot 4: `QA-13Oct-ZP-4603-x2-FAIL-after-reimport-classes-changed.png`).
- **What should happen:** the importer matched these assets by Alternate UID. It should keep their class, or at least warn before it changes it. A round trip with no edits should change nothing.
- **For the developer:** several classes share one SKM type, so the import picks one class per type: `Pi/Series Capacitor` → Capacitor Bank (both classes export with that PiType), `MotorController` → Motor Controller, `Bus` → Panelboard (PDU, Other and MV Switch export as Bus too). On `update` the importer writes `eg_class` over the existing class.
- **Severity:** Medium (silent data change). **Assignee:** Avani (batch rule 13 Oct). **Jira check:** related but different: ZP-4854 (Bulk Import of an unchanged Bulk Export), ZP-4851 (SKM review button text). No ticket for a class change on SKM re-import.

## Enhancement suggestions
1. Give Motor Starter its own symbol. It still draws the same M-in-a-circle as Motor (the confusion ZP-4513 fixed for Motor Controller).
2. Also split the other shared icons: Circuit Breaker vs Other (OCP), Junction Box vs DC Combiner Box, VFD vs VFD Panel.
3. In the + Asset picker search, match the old names ("Battery", "AC PV Array") to the renamed classes, so users who type the old word still find them.
4. On the SKM review screen, show "Class changes from X to Y" when an update would change an asset's class.

## Mobile part for Nency
The SLD viewer draws `icon.svg` from the database, so iOS and Android must show the new symbols with no app change. Open site "QA-DEMO ZP-4800 SLD canvas v2.4 (delete me)" on iOS and Android and compare the three `QA G-*` rows with shots 1, 2, 3, 7 and 8. Check four things. Lightning Arrester has only the top terminal. Instrument Transformer has a right-side "Sec" terminal. The DC marks show 3 separate dashes, not a solid line. The class pickers say "DC Battery", "DC PV Array" and "PV Array".

## Not covered
- Custom icon kept for a company override (no such row on QA): NOT WEB-TESTABLE.
- Old SKM names still accepted on import (backend rules only): NOT WEB-TESTABLE.
- The migration downgrade and re-run (backend).

## Proposed Jira comment
```
h3. QA 13 Oct, build index-C14cwR0U.js (QA): PASS

# Each class now draws its own symbol: Load, 3-Winding Transformer, Series and Shunt Reactor, Lightning Arrester, Instrument Transformer (with a Sec output on the right), Capacitor Bank (grounded), Relay (SKM style), Meter, PDU, Panelboard, Other.
# Disconnect, MV and DC Disconnect are boxed. DC Breaker, DC Fuse and DC Disconnect carry the DC mark, with 3 clean dashes. DC Capacitor is grounded.
# Class names now read PV Array (AC), DC PV Array and DC Battery. The keys did not change.
# + Asset: Rectifier is under "AC/DC Conversion" on the AC tab. Inverter is under "DC/AC Conversion" on the DC tab. French shows "CONVERSION CC/CA". The AC/DC connection rule still blocks AC to DC.
# SKM export uses Battery / SynchGenerator Solar PV / DCGenerator. SKM import maps them back to DC Battery / PV Array / DC PV Array.

!QA-13Oct-ZP-4603-1-PASS-load-3winding-xfmr-reactors-arrester.png|width=720!
!QA-13Oct-ZP-4603-3-PASS-dc-mark-on-dc-breaker-fuse-disconnect-grounded-dc-cap.png|width=720!
!QA-13Oct-ZP-4603-4-PASS-picker-AC-tab-rectifier-under-AC-DC-conversion.png|width=720!
!QA-13Oct-ZP-4603-5-PASS-picker-DC-tab-inverter-under-DC-AC-conversion.png|width=720!

*Question:* the ticket says Lightning Arrester ends the line "with a Protected output". On QA it has one terminal, named "In". Is that the intended label?
*Not checked from the web:* custom icons kept after the migration, and old names still importing (backend only).
*Separate issue (new ticket):* re-importing an unchanged SKM export changes Capacitor to Capacitor Bank, Motor Starter to Motor Controller and PDU to Panelboard.

*Status:* moved to READY TO RELEASE
```

## Recommended status
**READY TO RELEASE.** All web-visible rows pass. The SKM class change on re-import is a separate, older importer issue and gets its own ticket. Mobile check goes to Nency.
