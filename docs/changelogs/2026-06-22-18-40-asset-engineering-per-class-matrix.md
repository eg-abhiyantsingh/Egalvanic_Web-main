# Asset ENGINEERING per-class matrix — every asset class's Engineering section

- **Date:** 2026-06-22
- **Prompt:** "check everything in engineer section… create test case and cover everything according to
  asset it will show" — i.e. the Engineering section content is class-dependent; cover what every class shows.
- **Type:** New data-driven UI suite — 1 test per asset class (39 classes).

---

## What it does

The Add-Asset **Engineering** block renders different fields per asset class. I drove the Create form
live for all **39** classes (from `testcase/node_classes_gold.json`), dumped each one's Engineering
section, and built `AssetEngineeringMatrixTestNG` — a `@DataProvider` test that, per class, asserts the
**distinctive Engineering fields** the user will see plus the correct **voltage mode**.

### Voltage modes
- **DERIVED** — read-only "System Voltage" ("Derived from upstream"): 31 classes.
- **EDITABLE** — selectable "Voltage" ("Select voltage"): Battery, Generator, Utility.
- **BOTH** — derived Primary + selectable Secondary/Tertiary: Transformer, Transformer (3-Winding).
- **NONE** — no voltage field (conductor classes): Busway, Cable.

### Per-class field groups (verified live)
- **Library match (Manufacturer + search):** Circuit Breaker (Subtype + Pole Count + Mfr), Fuse
  (Subtype + **Fuse Count** + Mfr), Relay (Subtype + Mfr), Switch (Pole Count + Mfr).
- **Transformer:** Transformer (Subtype + kVA Rating + % Impedance + Type + Mfr); Transformer
  (3-Winding) (**Tertiary Voltage** + kVA Rating + % Impedance).
- **Phase Configuration + Mains Type:** MCC, Motor Starter, Other, PDU, Panelboard, Switchboard, VFD,
  VFD Panel (several also with Asset Subtype).
- **Mains Type:** Busduct, Disconnect Switch (+Subtype), Junction Box.
- **Subtype + System Voltage:** ATS, Capacitor, Load, Motor, UPS.
- **System Voltage only:** Capacitor Bank, Default, Lighting Controls, Loadcenter, MCC Bucket, Meter,
  Motor Controller, Rectifier, Series Reactor, Shunt Reactor, Tie Breaker.
- **Pole Count, no Mfr:** Other (OCP).
- **Conductor (Length / Conductor Material / Size / Insulation, no voltage):** Busway, Cable.
- **Editable Voltage:** Battery (+Subtype), Generator, Utility (+Configuration).

## Validation
`AssetEngineeringMatrixTestNG` ran **headed**, **39/39 pass** — each class's section matches the live
app. The discovery diagnostic (`testMATRIX_00_Discover`, disabled) dumps all sections to
`/tmp/eng-matrix.txt` for future re-mapping.

Complements the deep flows: `AssetEngineeringTestNG` (Circuit Breaker trip config, 12) and
`TransformerEngineeringTestNG` (10). This matrix is the **breadth** layer — no asset class's Engineering
section is now uncovered.

## Wiring
`suite-asset-engineering-matrix.xml` (dedicated, 39 TCs) + added to `suite-asset-4-5.xml` + a
`parallel-suite.yml` matrix row under the existing `ASSET_5` toggle.
