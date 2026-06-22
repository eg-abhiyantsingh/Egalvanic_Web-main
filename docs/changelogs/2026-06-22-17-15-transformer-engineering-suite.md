# Transformer ENGINEERING test suite (asset library-match + connections)

- **Date:** 2026-06-22
- **Prompt:** Follow-up workflow spec — "Create a new transformer asset … Asset Class … Asset Subtype …
  secondary voltage … manufacturer … library matches (kVA / impedance %) … configure kVA / % impedance
  / primary connection (Delta) / secondary connection (Wye-Ground)."
- **Type:** New UI test class — 10 TestNG tests covering the Transformer Engineering flow.

---

## Flow (discovered live, then asserted against ground truth)

Create Asset → class=**Transformer** ⇒ ENGINEERING exposes **Primary Voltage** (derived/read-only),
**Secondary Voltage** (120V…69kV, 16 levels), **kVA Rating**, **% Impedance**, a **Type** toggle
(Dry Type / Oil-Filled — renders after manufacturer) and **Manufacturer**. Selecting **Generic**
reveals **"N possible matches"** library cards (`Generic · Oil Air · 3 kVA · R% 3.76 · X% 1.00`).
Clicking one applies a **LIBRARY MATCHED** card, populates kVA Rating, and reveals **Primary
Connection** + **Secondary Connection** dropdowns. Custom attributes include **BIL** + **Winding
Configuration**.

| # | Test | Asserts |
|---|------|---------|
| XFMR_01 | Section appears | Primary/Secondary Voltage, kVA Rating, % Impedance, Manufacturer render |
| XFMR_02 | Primary Voltage read-only | `disabled` / "Derived from upstream" |
| XFMR_03 | Secondary Voltage options | 480V / 240V / 600V present |
| XFMR_04 | kVA + % Impedance inputs | ≥2 numeric inputs with kVA / % adornments |
| XFMR_05 | Type toggle | Dry Type / Oil-Filled present + selectable (after manufacturer) |
| XFMR_06 | Manufacturer reveals matches | "possible matches" + kVA + R%/X% cards |
| XFMR_07 | Match populates config | LIBRARY MATCHED + kVA Rating gets a numeric value |
| XFMR_08 | Connection controls | Primary Connection + Secondary Connection appear post-match |
| XFMR_09 | Custom attributes | BIL + Winding Configuration present |
| XFMR_10 | Save + persist | matched transformer saves (drawer closes → /assets); best-effort read-back |

## Validation
Ran the full class **headed**, twice, against `acme.qa.egalvanic.ai`: **10/10 pass both runs** (stable,
no flakiness). DOM ground-truth captured via a disabled discovery diagnostic (`testXFMR_00_DumpFlow`).
Reuses the robust helpers proven in `AssetEngineeringTestNG` (retrying open + autocomplete, hardened
drawer cleanup) so cross-test contamination doesn't bite.

## Notes
- **XFMR_10 creates one transformer asset** (`XFMR-PERSIST-<ts>`) to prove write-persistence — this
  tenant otherwise has no Transformer assets (dashboard "Assets by Type" lists CB/Fuse/PDU/Switchboard
  etc., no transformer), so a read-back-only test would perpetually skip. The save-success is the
  reliable persistence signal; read-back is attempted best-effort.
- Wired into `suite-asset-transformer.xml` (dedicated), `suite-asset-4-5.xml`, and `parallel-suite.yml`
  (under the existing `ASSET_5` toggle).
