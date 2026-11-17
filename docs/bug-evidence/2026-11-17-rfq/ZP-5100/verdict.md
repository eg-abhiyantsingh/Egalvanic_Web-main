# ZP-5100: Hotfix, SKM-linked cables never arc-flash ready + SKM import leaves bus, transformer and core attributes empty

**Verdict: PASS → recommend READY TO RELEASE**, with one caveat for the lead: no real customer SKM study (e.g. Koch Building A) was reachable. I searched:
- Jira attachments on ZP-5100 and on every SKM/Koch ticket with attachments;
- Google Drive (Koch, SKM, PTW, study, .zip, .xml);
- Gmail (SKM/Koch/PTW with attachments);
- this repo and ~/Downloads, Desktop, Documents.

All XML found is EG's own exports or hand-made fixtures. So the two missing rows were run with an SKM-format study built from real SKM data:
- both transformers are SKM library exports (`POST /api/skm-transformer-library/transformer/{style}/export-xml`);
- the bus arc-flash fields use the exact SKM field ids and names the platform writes in its own SKM export.

Both rows pass. The rows that passed on 12 Nov were not changed by today's build.

- **Build:** `index-LGhrn-48.js`, 17 Nov 2026 14:38–14:50 IST. Same code as `index-CFMAmUdK.js` except the build stamp.
- **Newer build:** at about 14:50 IST QA moved to `index-CkkAeA9o.js`. The import itself is backend. The frontend chunks this test touched (Assets, UnifiedEntityEditor) differ only by renamed imports, so I did not re-run the import on it.
- **Seat:** Super Admin (EG-ACME).
- **New sites (own):**
  - "QA-DEMO ZP-5100 17 Nov SKM transformer + core attrs" (`2f17857d-c5fb-46de-8f18-3ba2192ac9e7`). Import 1, through the UI, recorded.
  - "QA-DEMO ZP-5100 17 Nov SKM AF fields (exact ids)" (`d4369e15-4e47-4b33-aafd-2dd5bfe5e17a`). Import 2, through the import API with the same preview/commit calls as the dialog.
- **Fixtures (in this folder):**
  - `QA-DEMO-ZP5100-17Nov-skm-format-study.xml` (import 1).
  - `QA-DEMO-ZP5100-17Nov-skm-format-study-AF-exact-ids.xml` (import 2).
  - The two raw SKM library exports: `skm-library-export-SQUARE-D-PADMOUNT-75kVA.xml` and `skm-library-export-SQUARE-D-DRY-225kVA.xml`.
- **Study content:**
  - Utility → switchboard (1200 A) → 2 breakers.
  - XF-PAD-LIB: SQUARE D PADMOUNT 75 kVA, library-bound, KeyInLibrary 1, Type "PADMOUNT", Pri Connection Delta / Sec Connection Wye-Ground.
  - XF-DRY-HAND: SQUARE D DRY 225 kVA block with KeyInLibrary set to 0, so hand-entered but naming the model ("DRY"); Delta / Wye.
  - Three buses carrying AF_ElectrodeConfig VCB / VCBB / HCB plus box sizes.

## Checked today
| Row | How | Result |
|---|---|---|
| Import UI | Assets › SKM › Import (Full) › upload › Analyze | PASS: review shows 8 assets · 7 connections · 1 library matched (XF-PAD-LIB → "SCHNEIDER/SQUARE D PADMOUNT") · 4 unmatched; "Import successful: Created 8 assets and 7 connections" |
| **Transformer Type from the bound library model's symbol** | XF-PAD-LIB read back (API + Engineering tab) | **PASS:** Type **"Pad Mount"** (`trip_type_rel` xfmr_pad, from symbol PDMT); `eqp_lib` transformers-skm kva_entry 296, 75 kVA, %Z 3.49; the library card shows "Code PDMT, Windings Primary Delta, Secondary Wye-Ground" |
| **Transformer Type inferred from the named model (hand-entered)** | XF-DRY-HAND (KeyInLibrary 0, Manufacturer "SQUARE D", Type "DRY") | **PASS:** Type **"Dry Type"** (xfmr_dry); no library binding (`eqp_lib` null), as expected |
| **Reserved core attributes: winding connections** | both transformers | **PASS:** `primaryWinding`=Delta, `secondaryWinding`=Wye-Ground (PAD) and Delta / Wye (DRY). Shown on the Engineering tab, ticked |
| **Reserved core attributes: electrode configuration** | import 2 (exact SKM field ids) | **PASS:** SWBD `electrodeConfig`=VCB, PNL208 VCBB, PNL120 HCB. Engineering tab: "Electrode Configuration VCBB" |
| Electrode configuration, import 1 (right names, invented field ids 65750–65753, no AF_UnLinked… flags) | import 1 | not written. Import 2 added two things: the real ids (21364827…) and the `AF_UnLinkedElectrodeConfig`/`AF_UnLinkedBoxDimensions` = "UnLinked" flags that EG's own export writes. With both, the value is written. So the importer needs the real SKM id and/or the UnLinked flag; I did not separate the two. Not a defect: a real SKM study carries both |
| Bus ampere rating + phase (12 Nov row, re-check) | both imports | PASS: 1200 / 225 / 100 A, 3P4W |
| Re-export re-emits the values | `POST /api/sld/{id}/export-skm` on both new sites | PASS: Type "Pad Mount" / "Dry Type"; Pri / Sec Connection; AF_ElectrodeConfig VCB / VCBB / HCB; ContinuousRating 1200 / 225 / 100; X/R + Rpos/Xpos; 0 literal "N/A" |
| Transformer arc-flash readiness | Engineering tab | Properties 4/4 complete (incl. Type). The ring shows 4 of 6 only because the company's custom attributes BIL and Class are required, and SKM does not carry them |
| Console | runs | Noise only. Once on the Assets page: `PAGEERROR Uncaught TypeError: p.target.className.indexOf is not a function` (no visible effect; it looks like the Beamer widget; not filed) |

12 Nov rows not re-run, all passed then:
- library-bound cables carry N/A insulation / installation;
- the imported NEC cable is arc-flash ready;
- real values are left unchanged (negative case);
- library XML writes N/A as blank.

## Observation (question for Eric, not filed)
- The enclosure box size is also a reserved core attribute (Enclosure Height / Width / Depth, exported as AF_BoxHeight / AF_BoxWidth / AF_BoxDepth with ids 21364829 / 21364828 / 21364830).
- It is **not** written on import, even with the exact ids. A re-export of an imported site therefore drops the box size.
- The ticket names only electrode configuration and winding connections. Are box dimensions in scope?

## Not covered
- A real customer study (Koch Building A) and its before/after readiness counts (prod data).
- SIEMENS cables: none on QA.

## Cross-platform
- **Twins:** no iOS/Android twin found (JQL on "SKM import", "insulation" in the last 60 days). SKM import/export is a web-only feature.
- **iOS source** (release/qa 34bd67e) shows the imported data:
  - transformer Type (`trip_type_id` is labelled "Type", AssetsTab.swift);
  - insulation type from the synced enum table (NodeEngineeringSection.swift), which includes the N/A row;
  - the reserved core attributes, as generic core attributes.
  - iOS also has its own client-side SKM matcher (`SkmMatcher.swift`), so the "N/A keeps the library entry" behaviour needs a device check.
- **Android:** no source access.

### MOBILE-CHECKS
1. **iOS 1.75+, EG-ACME QA, site "QA-DEMO ZP-5100 17 Nov SKM AF fields (exact ids)": transformer.**
   - Steps: Assets › QA5100C-XF-PAD-LIB › Engineering.
   - Expected: Type "Pad Mount", Primary Winding Delta, Secondary Winding Wye-Ground, kVA 75, %Z 3.49.
2. **iOS: panel.**
   - Steps: Assets › QA5100C-PNL208 › Engineering.
   - Expected: Electrode Configuration VCBB, amperage 225 A, 3P4W.
3. **iOS, site "QA-DEMO ZP-5100 12 Nov SKM import": cable.**
   - Steps: Assets › QA5100-CBL-NEC › Engineering.
   - Expected: insulation type "N/A", installation "Conduit (Steel)". Open the library match: the bound NEC entry (456624) is still offered with N/A, not dropped.
4. **Android 1.40:** same three checks.
