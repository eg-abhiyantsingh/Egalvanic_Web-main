# Engineering Mains-Configuration cascade (Phase Config → Mains Type → Main Breaker)

- **Date:** 2026-06-23
- **Prompt:** "how many test cases are we covering for the engineer section" + a Panelboard create workflow
  spec (Phase configuration · Mains type · Main breaker subtype · Pole count).
- **Type:** New UI depth suite — 14 tests for the distribution-equipment mains-config cascade.

---

## Coverage count answered

Before this change the Engineering section had **61** cases: `AssetEngineeringTestNG` (12, Circuit
Breaker depth) + `TransformerEngineeringTestNG` (10) + `AssetEngineeringMatrixTestNG` (39, per-class
breadth). The matrix covered the phase+mains classes only at the **label** level — so the deeper
**mains-configuration cascade** was the gap this suite fills. New total: **75** Engineering cases.

## Flow (discovered live with Playwright)

Select a phase+mains class (Panelboard / MCC / Switchboard / PDU / Motor Starter / Other / VFD / VFD
Panel) ⇒ ENGINEERING exposes:
- **Phase Configuration** — `3P4W · 3P4W-HLD · 3P3W · 1P3W · 1P2W`
- **Mains Type** — `MCB · MLO · FDS · NFDS`

Choosing **MCB** (Main Circuit Breaker) opens a **"Create a Main Breaker?"** dialog:
- **Name*** (prefilled "MCB"), **Subtype** (11 breaker subtypes — Low-Voltage Insulated Case / Molded
  Case ≤225A / >225A / Power, Medium-Voltage Air/Gas/Oil/Vacuum, Motor Circuit Protector, Recloser),
  **Pole Count** (1P/2P/3P), **Create Main**.

Choosing **MLO** (Main Lug Only) opens **no** breaker dialog.

## Tests (`MainsConfigEngineeringTestNG`, 14)

- **Deep (Panelboard):** MAINS_01 Phase+Mains present · MAINS_02 Phase Config 5 options · MAINS_03
  Mains Type 4 options · MAINS_04 MCB opens the breaker dialog (Subtype + Pole Count + Create Main) ·
  MAINS_05 Main Breaker Subtype offers CB subtypes · MAINS_06 Pole Count 1P/2P/3P selectable in the
  dialog · MAINS_07 MLO opens no breaker dialog.
- **Breadth (data-driven):** MAINS_08 × 7 — Phase Configuration + Mains Type (MCB/MLO/FDS/NFDS)
  populated for MCC, Switchboard, PDU, Motor Starter, Other, VFD, VFD Panel.

## Validation
Ran **headed** against the live app: **14/14 pass**. (One iteration fixed MAINS_05, which had matched
the drawer's "Asset Subtype" instead of the dialog's "Select subtype (optional)" — now scoped to the
dialog.) Non-destructive — every test cancels the create form / breaker dialog (no asset is created).

## Wiring
`suite-asset-mains-config.xml` (dedicated, 14 TCs) + added to `suite-asset-4-5.xml` + a
`parallel-suite.yml` matrix row under `ASSET_5`.
