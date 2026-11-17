# ZP-5100 cross-platform (Android): SKM-imported data on the device
- Platform/build: Android 1.40 (54). Seat: Super Admin, EG-ACME. 17 Nov 2026 ~16:55 IST. Server values from GET /api/nodes/sld/<site>.

| Asset (site) | Server | Android (asset › Engineering) | Parity |
|---|---|---|---|
| QA5100C-XF-PAD-LIB ("QA-DEMO ZP-5100 17 Nov SKM AF fields (exact ids)") | type (trip_type_rel) "Pad Mount"; library SCHNEIDER/SQUARE D 75 kVA; windings Delta / Wye-Ground | Library Matched card "SCHNEIDER/SQUARE D · PAD · PDMT · R% 1.31 · X% 3.24"; **Primary Connection Delta, Secondary Connection Wye-Ground**. There is no separate "Type: Pad Mount" field (only the library abbreviation PAD / PDMT). | windings same; type **differs in display** (abbreviation only) |
| QA5100C-PNL208 (same site) | core attribute Electrode Configuration = VCBB | **Electrode Configuration VCBB** | same |
| QA5100-CBL-NEC ("QA-DEMO ZP-5100 12 Nov SKM import") | insulation_type_label "N/A", insulation_class_label "RHW" | Cable Engineering shows **no insulation type/class field** (only the nameplate "Insulation Thickness", empty) | **differs** (insulation type not shown on Android) |

- Note, not filed: whether the Android cable/transformer Engineering is meant to show these fields is a dev question. Web shows them (per the ZP-5100 web test). If Android should, this is an Android gap.
- Evidence: android-1-xf-pad-lib.png (windings), android-1b-xf-pad-lib-type.png (library card), android-2-pnl208-electrode.png, android-3-cbl-nec-insulation.png (Insulation Thickness only)
