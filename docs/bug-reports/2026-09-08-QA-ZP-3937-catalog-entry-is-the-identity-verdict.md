# [Web] Free-text manufacturer/model in equipment links meant the same real part produced different identities

**QA verdict — PASS on every mechanism that could be reached from the product. Manufacturer is now a per-type browse filter, not part of the identity: choosing an equipment type loads its manufacturer list, and the catalog list is already populated before a single character is typed. Picking a circuit-breaker entry makes a **Frame** select appear whose options each carry an `ampere_rating` patch, which is how Amps gets filled from the catalog. Stored `catalog_ref` values are catalog entry plus subsettings with no free-text manufacturer, model or family, and a legacy ref that still carries free text loads without error. The product states the tradeoff itself: a part the catalog does not carry stays unlinked and its name is its identity.**

**Tested:** 2026-09-08 · `acme.qa.egalvanic.ai` build **V1.36**, bundle **`index-CSsDpG3c.js`** · tenant acme · Super Admin seat · Admin → Materials, live UI with the catalog endpoints captured.
**Ticket said "dev only, not yet promoted to cicd/qa" — wrong:** the browse-by-manufacturer editor and the identity-only `catalog_ref` are both live on QA.

**Artifact:** https://claude.ai/code/artifact/920bf269-332a-4ce4-b0ee-54cdfe2d3081

---

## QA-review checklist — results

| # | Ticket step | Result |
|---|---|---|
| 1 | Open the Equipment Link editor, pick an equipment type, and confirm the catalog list is already populated before you type anything | ✅ **PASS** — with **Circuit Breaker** chosen, clicking Catalog entry (placeholder "**Browse or type to narrow**") fires `quick-search?type=circuit_breaker&q=` with an **empty query** and the dropdown lists real entries immediately: ADVAC · __ADV__ · ABB · HV/MV Breakers - without Integral Trip-Unit; Emax 2, E1.2, Ekip DIP · 1SDC200039D0204 · ABB · Low Voltage Breakers - Static Trip; and more. |
| 2 | Confirm Manufacturer is a dropdown scoped to the chosen type, that selecting one narrows the catalog list, and that the manufacturer is not part of the saved link | ✅ **PASS** — choosing a type immediately calls `manufacturers?type=<type>`; for circuit_breaker that returns ABB, ABB/BBC, ABL SURSUM, ALLEN-BRADLEY and more, and the field reads "**All manufacturers**" until one is picked. Narrowing is real: on `disconnect_switch`, GE → 17 rows, ABB → exactly 1. And the manufacturer is **absent from the stored ref** — the saved disconnect link is `{equipment_type, skm_bus_oid, ampere_rating, fuse_class, pole_count}` with no manufacturer key, while the Catalog entry field held the entry and Manufacturer stayed empty. |
| 3 | Pick a Circuit Breaker frame and confirm Amps is filled from the catalog rather than left blank | ⚠️ **PASS on the mechanism; the fill was not watched** — picking "ADVAC · __ADV__" calls `resolve?skm_device_oid=213512` → 200 with `option_label: "Frame"` and `options: [{label:"1200A · 4760V · 41kA", patch:{ampere_rating:1200, skm_frame_sid:1}}, {label:"1200A · 4760V · 29kA", patch:{…}}, …]`, and a **Frame** select appears in the dialog that was not there before the pick. Each option's patch carries the amps, so selecting a frame is what writes Amps. Selecting one and reading the Amps box back was not done — the dialog was on an unsaved **Create Material** and was abandoned rather than written to shared QA data. |
| 4 | Save a link, reopen the entry, and confirm the identity shown is the catalog entry plus its subsettings with no free-text manufacturer/model text | ✅ **PASS on the reopen half** — entry "1 sep abs" reopens with Equipment type **Disconnect Switch**, and `resolve?skm_bus_oid=452651` → `{label:"SafeGear HD (AR)", sublabel:"ABB · MV Switchgear Arc-Resistant"}` rendered alongside its Poles / Amps / Fuse class. The identity is the catalog entry; ABB appears only as the resolved entry's own sublabel, not as stored text. A fresh save was not performed (shared data). |
| 5 | Negative: for a part not in the catalog, confirm it can be recorded by name and stays unlinked — it must not save as a linked entry | ✅ **PASS by construction, stated by the product** — 14 of the 17 library entries are unlinked, carrying a name and price with an em dash in the Equipment Link column, and the editor's own helper says "**A part the catalog doesn't carry stays unlinked; its name is its identity.**" Creating one fresh was not needed to see the state exists. |
| 6 | Negative: reopen a link saved earlier under #1121 (free-text manufacturer/model still in `catalog_ref`) and confirm it loads without error | ✅ **PASS** — "Bussmann LPJ Class J current-limiting fuse" stores the legacy shape `{equipment_type:"fuse", family:"LPJ (Class J)", fuse_class:"J", ampere_rating:null, pnl_device_id:144}` — free-text `family` and a non-SKM `pnl_device_id`. Its grid chip renders "Fuse · family 144" and the row opens without error. |
| 7 | Run the resolution agent twice against the same issue and confirm it does not mint two materials-library rows for one catalog part | ❌ **NOT EXERCISED** — no agent run can be triggered on QA in this session: `POST /issue-resolution/reevaluate` answers `{issues: 1, jobs: 0}` and produces nothing. What *was* verified adjacently: two issues were re-evaluated with the library counted before and after, and it stayed at 17 with no write call — so evaluation does not mint at all, which makes a double-mint from two runs impossible on this build. |

---

## Findings

### FINDING 1 (Low) — manufacturer lists include standards documents
On `disconnect_switch` the manufacturer filter offers four ANSI documents and four UL standards alongside ABB, GE and SCHNEIDER/SQUARE D, and selecting ANSI C37.06-1979 returns six real rows. Harmless for identity (manufacturer is only a filter now) but it makes the dropdown read oddly, and it removes any way to pose "a manufacturer with no parts of this type".

---

## Test data — direct links (QA)
- Materials library: https://acme.qa.egalvanic.ai/materials — linked entries "1 sep abs" (Disconnect Switch · bus 452651), "19 aug abs" (Fuse · SKM 550331), "Bussmann LPJ …" (legacy free-text ref)
- Browse mode: `https://acme.qa.egalvanic.ai/api/equipment-catalog/quick-search?type=circuit_breaker&q=`
- Frame axis: `https://acme.qa.egalvanic.ai/api/equipment-catalog/resolve?skm_device_oid=213512`
- Manufacturer filter: `https://acme.qa.egalvanic.ai/api/equipment-catalog/manufacturers?type=circuit_breaker`

Evidence: `docs/bug-evidence/zp-materials-library-equipment-linkage/`.

## Not covered / honest gaps
- **Selecting a frame and reading Amps back**, and **saving then reopening a fresh link** — both would write to shared QA library data; the mechanisms were verified from the payloads that drive them.
- **The agent double-mint check** — no agent run is triggerable on QA.
- **Roles other than Super Admin** — mandatory MFA on QA blocks the other seats.
