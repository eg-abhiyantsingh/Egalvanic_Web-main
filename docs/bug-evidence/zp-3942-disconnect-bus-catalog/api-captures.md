# ZP-3942 — disconnect switches link to the bus-model catalog — captures (QA V1.36, 2026-09-08)

## Where
Admin → OPERATIONS → **Materials** (`/materials`) → Material Library grid, columns Name · Type · Price · Unit · Default Margin · **Equipment Link**; toolbar Material Library / Material Presets / Material Types / Material Units · Create Material · **Unpriced (1)** · Bulk Ops · AI Setup. Clicking a row opens **Edit Material** with an **Equipment Linkage** section: **Equipment type** · **Manufacturer (filter)** · **Catalog entry** · Poles · Amps · Fuse class · **Clear link**, under the helper "Links this part to the equipment catalog — AI-suggested materials dedupe against it, and pricing this entry prices every resolution that references the same device. A part the catalog doesn't carry stays unlinked; its name is its identity."

## Disconnect switches now search the bus-model catalog
Existing entry "1 sep abs" (Equipment, $1.00) shows Equipment Link **"Disconnect Switch · bus 452651"**; its stored `catalog_ref` is `{equipment_type:"disconnect_switch", skm_bus_oid:452651, ampere_rating:1, fuse_class:"1", pole_count:31}` — a **bus** oid, not a device oid.
Opening it fires `GET /api/equipment-catalog/resolve?skm_bus_oid=452651` → 200 `{label:"SafeGear HD (AR)", sublabel:"ABB · MV Switchgear Arc-Resistant", options:[]}` — **the saved link resolves and renders a real catalog entry**, not a blank or placeholder.
Typing in Catalog entry calls `GET /api/equipment-catalog/quick-search?type=disconnect_switch&q=…` (empty `q` browses). Results by product family:
| query | rows | sample (label · manufacturer · category · ref) |
|---|---|---|
| `BPS` | 2 | **BPS · GE · LV Switchboard · `{skm_bus_oid:425107}`**; Pow-R-Line C · EATON/CUTLER-HAMMER · LV Switchboard · `{skm_bus_oid:424674}` |
| `HVL` | 2 | **HVL FUSED SWITCH** · SCHNEIDER/SQUARE D · MV Switchgear · `{skm_bus_oid:425376}`; **HVL UNFUSED SWITCH** · `{skm_bus_oid:425389}` |
| `MINIBREAK` | 2 | **MINIBREAK SWITCH** · SCHNEIDER/SQUARE D · MV Switchgear · `{skm_bus_oid:444021}` and `{skm_bus_oid:444019}` |
| `bolted` | **0** | — |
| `PRESSURE` | **0** | — |
Every row carries `source: "skm"` and a `ref` of `{skm_bus_oid}`. The UI Autocomplete showed the same two rows for "BPS".

## Manufacturer filter
`GET /api/equipment-catalog/manufacturers?type=disconnect_switch` → 200, **15 entries**: ABB · ALLEN-BRADLEY · **ANSI C37.06-1979** · **ANSI C37.06-1987** · **ANSI C37.06-1997** · **ANSI C37.16-1997** · EATON/CUTLER-HAMMER · GE · LITTELFUSE · SCHNEIDER/SQUARE D · SIEMENS · **UL 347** · **UL 67** · **UL 845** · **UL 891**.
Narrowing works: `manufacturer_id` for GE → **17 rows** (8000-Line · GE · LV MCC, AD/AE · GE · LV Panelboard, AKD-10 WavePro · GE · LV Switchgear…); ABB → **1 row** (SafeGear HD (AR) · ABB · MV Switchgear Arc-Resistant).

## Negatives
- A nonsense query (`q=zzzznotathing`) → 200 `{"rows":[]}` — a clean empty result, no error, no unfiltered list. ✅
- Filtering by **ANSI C37.06-1979** (a standards document, not a manufacturer of disconnect switches) → 200 with **6 rows** ("Indoor Oil Circuit Breakers · ANSI C37.06-1979 · MV Switchgear", …). The manufacturer list itself contains four ANSI documents and four UL standards, so the ticket's negative ("search a manufacturer that makes no disconnect switches → empty") cannot be posed as intended: those entries do return rows.

## Regression — other equipment types still search their own catalogs
| type | rows | ref key in results |
|---|---|---|
| transformer | 30 | `skm_transformer_oid` (NONE-Forced Air · Generic) |
| cable | 30 | `skm_cable_oid` (AluminumMagneticRHHXLPE600StabiloyALCAN… · ALCAN) |
| fuse | 30 | `skm_device_oid` (DO-III Dual Element · ABB · Fuses - High Voltage) |
| circuit_breaker | 30 | `skm_device_oid` (ADVAC · ABB; Emax 2 · ABB · Low Voltage Breakers - Static Trip) |
| panelboard | 20 | `skm_bus_oid` (Pow-R-Command · EATON/CUTLER-HAMMER · LV Panelboard) |
Each type resolves into its own id space, unaffected by the disconnect reroute.

## Not covered
Saving a *new* disconnect link end to end (the existing entry already carried one, and it was not overwritten to avoid disturbing shared QA data); "bolted-pressure" lineups by any other spelling.
