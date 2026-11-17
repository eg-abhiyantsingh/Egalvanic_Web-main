# ZP-5100 cross-platform (Android): SKM-imported data on the device
- Platform/build: Android 1.40 (54). Seat: Super Admin, EG-ACME. 17 Nov 2026 ~16:55 IST. Server values from GET /api/nodes/sld/<site>.

| Asset (site) | Server | Android (asset › Engineering) | Parity |
|---|---|---|---|
| QA5100C-XF-PAD-LIB ("QA-DEMO ZP-5100 17 Nov SKM AF fields (exact ids)") | type (trip_type_rel) "Pad Mount"; library SCHNEIDER/SQUARE D 75 kVA; windings Delta / Wye-Ground | Library Matched card "SCHNEIDER/SQUARE D · PAD · PDMT · R% 1.31 · X% 3.24"; **Primary Connection Delta, Secondary Connection Wye-Ground**. There is no separate "Type: Pad Mount" field (only the library abbreviation PAD / PDMT). | windings same; type **differs in display** (abbreviation only) |
| QA5100C-PNL208 (same site) | core attribute Electrode Configuration = VCBB | **Electrode Configuration VCBB** | same |
| QA5100-CBL-NEC ("QA-DEMO ZP-5100 12 Nov SKM import") | insulation_type_label "N/A", insulation_class_label "RHW" | Cable Engineering shows **no insulation type/class field** (only the nameplate "Insulation Thickness", empty) | **differs** (insulation type not shown on Android) |

- Note, not filed: whether the Android cable/transformer Engineering is meant to show these fields is a dev question. Web shows them (per the ZP-5100 web test). If Android should, this is an Android gap.
- Evidence: android-1-xf-pad-lib.png (windings), android-1b-xf-pad-lib-type.png (library card), android-2-pnl208-electrode.png, android-3-cbl-nec-insulation.png (Insulation Thickness only)

## iOS (17 Nov, iOS tester)
- Platform/build: iOS, Z Platform-QA 1.75 simulator build (binary 16 Nov 06:49 IST, from the owner), lane A. Seat: Super Admin, EG-ACME. Asset page › Engineering. 17:11–17:16 IST.

| Asset (site) | Server | iOS (asset › Engineering) | Parity |
|---|---|---|---|
| QA5100C-XF-PAD-LIB ("QA-DEMO ZP-5100 17 Nov SKM AF fields (exact ids)") | type "Pad Mount"; library SCHNEIDER/SQUARE D 75 kVA; windings Delta / Wye-Ground | Library Matched card "SCHNEIDER/SQUARE D · PADMOUNT · 75 kVA · R% 1.31 · X% 3.24". **Primary Connection Delta, Secondary Connection Wye-Ground** (also Primary/Secondary Winding Delta / Wye-Ground). The "Type" field is **empty ("Enter type")**. "Pad Mount" shows only as the library abbreviation PADMOUNT. | windings same; type **differs in display** (same as Android) |
| QA5100C-PNL208 (same site) | Electrode Configuration = VCBB | **Electrode Configuration VCBB** | same |
| QA5100-CBL-NEC ("QA-DEMO ZP-5100 12 Nov SKM import") | insulation_type_label "N/A", insulation_class_label "RHW" | Library Matched card "Aluminum · Magnetic · Conduit (Steel) · RHW" (the class RHW shows in the card). There is **no insulation type field** ("N/A" not shown). Only "Insulation Thickness" (empty). | **differs** (insulation type not shown; same gap as Android) |

- Note, not filed: whether mobile Engineering should show Type "Pad Mount" and the cable insulation type is a dev question. Web shows them. iOS and Android behave the same.
- "Equipment library not downloaded" banner on the device (Settings › Load Latest Equipment Library was not run). The matched-library cards still showed.
- Evidence: ios-1-xf-pad-lib-library-and-windings.png, ios-1b-xf-pad-lib-type-field-empty.png, ios-2-pnl208-electrode-vcbb.png, ios-3-cbl-nec-library-card-rhw-no-insulation-field.png
