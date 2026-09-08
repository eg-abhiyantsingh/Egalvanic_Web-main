# Materials-library equipment linkage (ZP-3935 → ZP-3936 → ZP-3937) — captures (QA V1.36, 2026-09-08)

One session, one surface: Admin → OPERATIONS → **Materials** (`/materials`). These three tickets are successive layers on the same editor, so the evidence is shared.

## The grid (ZP-3935)
Columns **Name · Type · Price · Unit · Default Margin · Equipment Link**; toolbar tabs Material Library / Material Presets / Material Types / Material Units, plus **Create Material · Unpriced (1) · Bulk Ops · AI Setup**.
The **Equipment Link** column leads with the equipment type, as ZP-3936 requires:
| entry | type | price | Equipment Link cell |
|---|---|---|---|
| 1 sep abs | Equipment | $1.00 | **Disconnect Switch · bus 452651** |
| 19 aug abs | Equipment | $12.00 | **Fuse · SKM 550331** |
| Bussmann LPJ Class J current-limiting fuse | Material | $45.00 | **Fuse · family 144** |
| 19 aug abs 2, ABC_Ra, ABCD, Manage Material Information, Material name, New 12, New Material Test, New materials, Roanna Lamb, test, test 890, test abc, testbv678, Testing Material | — | various | — (unlinked) |
17 entries total. **Unpriced (1)** filters the library to the single entry with no price ("New materials", Indirect Cost, price "-"), i.e. a working pricing queue.

## `catalog_ref` shapes actually stored (ZP-3936 / ZP-3937)
- `1 sep abs` → `{equipment_type:"disconnect_switch", skm_bus_oid:452651, ampere_rating:1, fuse_class:"1", pole_count:31}` — typed, bus id space, **no free-text manufacturer/model/family**.
- `19 aug abs` → `{equipment_type:"fuse", skm_device_oid:550331}`.
- `Bussmann LPJ …` → `{equipment_type:"fuse", family:"LPJ (Class J)", fuse_class:"J", ampere_rating:null, pnl_device_id:144}` — a **legacy shape still carrying free-text `family`**; it loads and renders without error (ZP-3937's backwards-compatibility negative).

## Type-first gate and per-type scoping (ZP-3936)
**Create Material** → the Equipment Linkage section opens with **Equipment type** empty and, until a type is chosen, only Manufacturer (filter) / Catalog entry / Poles / Amps / Fuse class as inert fields — no catalog search is possible first. The type list is exactly: **— · Panelboard · Switchboard · Disconnect Switch · Circuit Breaker · Fuse · Relay · Transformer · Cable · Busway** (nine types plus the empty option).
Choosing **Circuit Breaker** immediately fires `GET /api/equipment-catalog/manufacturers?type=circuit_breaker` → 200 (ABB, ABB/BBC, ABL SURSUM, ALLEN-BRADLEY, …) and relabels the fields to that class: Manufacturer (filter) placeholder "**All manufacturers**", Catalog entry placeholder "**Browse or type to narrow**", Poles, Amps, plus **Clear link**. The Fuse class field disappears for this type.

## Browse-before-typing and the frame axis (ZP-3937)
Clicking **Catalog entry** with an empty query calls `GET /api/equipment-catalog/quick-search?type=circuit_breaker&q=` → 200 and the list is **already populated before any typing**: ADVAC · __ADV__ · ABB · HV/MV Breakers - without Integral Trip-Unit; Emax 2, E1.2, Ekip DIP · 1SDC200039D0204 · ABB · Low Voltage Breakers - Static Trip; Emax 2, E1.2, Ekip Hi-Touch · … Each row's `ref` is `{skm_device_oid}`.
Picking "ADVAC · __ADV__" calls `GET /api/equipment-catalog/resolve?skm_device_oid=213512` → 200:
```
{ "label": "ADVAC · __ADV__",
  "option_label": "Frame",
  "options": [ {"label":"1200A · 4760V · 41kA", "patch":{"ampere_rating":1200,"skm_frame_sid":1}},
               {"label":"1200A · 4760V · 29kA", "patch":{…}}, … ] }
```
and a **Frame** select appears in the dialog (it was not there before the pick). So the secondary axis is driven by the catalog entry, and each frame option carries an `ampere_rating` patch — the mechanism by which picking a frame fills Amps. The saved identity is the catalog entry plus subsettings; the Catalog entry field holds "ADVAC · __ADV__" with Manufacturer left empty, confirming manufacturer is a browse filter and not part of the identity.

## Per-type catalog routing (all three tickets, and ZP-3942's regression)
`quick-search` fans out to the table that can identify each type:
| type | rows | ref key |
|---|---|---|
| disconnect_switch | BPS / HVL FUSED / HVL UNFUSED / MINIBREAK ×2 | `skm_bus_oid` |
| panelboard | 20 | `skm_bus_oid` |
| transformer | 30 | `skm_transformer_oid` |
| cable | 30 | `skm_cable_oid` |
| fuse | 30 | `skm_device_oid` |
| circuit_breaker | 30 | `skm_device_oid` |
A nonsense query returns 200 `{"rows":[]}` — a clean empty, never an unfiltered list.

## Helper copy the editor shows
"Links this part to the equipment catalog — AI-suggested materials dedupe against it, and pricing this entry prices every resolution that references the same device. **A part the catalog doesn't carry stays unlinked; its name is its identity.**" — this is the product stating ZP-3937's deliberate tradeoff.

## Not exercised
Saving a brand-new link and reopening it (no write was made to shared QA library data); clearing a link to null; the validation negatives (a non-existent SKM device/frame, and setting `catalog_ref` on a global row); pricing an entry out of the Unpriced queue; Relay / Busway / Switchboard searches individually; and every agent-minting step (no agent run could be triggered on QA — `reevaluate` returns `jobs: 0`).
