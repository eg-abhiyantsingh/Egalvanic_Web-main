# Engineering exhaustive option coverage — 308 data-driven test cases

- **Date:** 2026-06-23
- **Prompt:** "at least create 300 test cases for the engineer section and share me in chat."
- **Type:** New data-driven UI suite — 308 cases verifying every Engineering **option value**.

---

## Why this exists

The matrix suite verified each class shows the right Engineering *fields*; this suite verifies the
actual *option values* the controls offer. Engineering coverage goes from **75 → 383** test cases.

## The 308 cases (data-driven, one invocation = one case)

| Dimension | Cases | Source |
|-----------|------:|--------|
| ENGINEERING section present (per class) | 39 | all classes |
| Voltage mode correct (DERIVED/EDITABLE/BOTH/NONE) | 39 | all classes |
| Asset Subtype offered (class × subtype) | 65 | `node_classes_gold.json` |
| Phase Configuration offered (class × config) | 40 | 8 phase+mains classes × 5 configs |
| Mains Type offered (class × type) | 32 | 8 classes × {MCB,MLO,FDS,NFDS} |
| Voltage level offered (class × level) | 64 | 4 editable-voltage classes × 16 levels |
| Conductor fields (Busway/Cable) | 13 | live |
| Transformer fields (Transformer / 3-Winding) | 7 | live |
| Manufacturer / library-match present | 5 | CB/Fuse/Relay/Switch/Transformer |
| Pole/Fuse Count present | 4 | CB/Switch/Other (OCP)/Fuse |
| **Total** | **308** | |

## How it stays fast + reliable

Each class's create form is opened **once**; its dropdown option lists (subtype / phase / mains /
voltage) + voltage-mode flags are **cached**. Every per-value test is then a cache lookup — so 308
cases cost ~39 form-opens, not 308. Non-destructive — every open is cancelled.

## Option data (discovered live, then asserted)

- **Subtypes:** the 65 from gold (e.g. Circuit Breaker 11, Disconnect Switch 10, Other 7, Switchboard 6).
- **Phase Configuration:** `3P4W · 3P4W-HLD · 3P3W · 1P3W · 1P2W`.
- **Mains Type:** `MCB · MLO · FDS · NFDS`.
- **Voltage levels (×16):** `120V · 208V · 240V · 277V · 347V · 480V · 600V · 2.4kV · 4.16kV · 12.47kV
  · 13.2kV · 13.8kV · 14.4kV · 23kV · 34.5kV · 69kV` — identical across Battery / Generator / Utility /
  Transformer-secondary (verified live).

## Validation
Ran **headed** against the live app. Wired into `suite-asset-engineering-exhaustive.xml` (dedicated) +
`suite-asset-4-5.xml` + a `parallel-suite.yml` matrix row under `ASSET_5`.

## Engineering coverage total: **383 cases** across 5 suites
AssetEngineeringTestNG (12) · TransformerEngineeringTestNG (10) · AssetEngineeringMatrixTestNG (39) ·
MainsConfigEngineeringTestNG (14) · **AssetEngineeringExhaustiveTestNG (308)**.
