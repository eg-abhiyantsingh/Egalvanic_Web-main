# [Web] Disconnect switches could not be linked to a real catalog entry - the search looked in the wrong SKM catalog

**QA verdict — PASS on the fix. Disconnect-switch searches now return bus-model records: BPS, HVL FUSED SWITCH, HVL UNFUSED SWITCH and MINIBREAK SWITCH all come back by name, every row carrying a `skm_bus_oid` ref, and a saved link resolves to a real catalog entry ("SafeGear HD (AR) · ABB · MV Switchgear Arc-Resistant") rather than a blank. The manufacturer filter is populated and narrows correctly, and every other equipment type still searches its own catalog. Two gaps: "bolted-pressure" lineups return nothing under that name, and the manufacturer list is padded with ANSI and UL standards documents that do return rows — so the ticket's negative case cannot be posed as intended.**

**Tested:** 2026-09-08 · `acme.qa.egalvanic.ai` build **V1.36**, bundle **`index-CSsDpG3c.js`** · tenant acme · Super Admin seat · live UI plus the catalog endpoints the UI calls.
**Ticket said "dev only, not yet promoted to QA" — wrong here:** the typed linkage editor and the bus-model routing are both live on QA, and an existing library entry already carries a `skm_bus_oid` disconnect link.

**Ticket:** [ZP-3942](https://egalvanic.atlassian.net/browse/ZP-3942)
**Artifact:** https://claude.ai/code/artifact/cfc44623-3592-4e7b-acf7-af6f12d60bf5

---

## QA-review checklist — results

| # | Ticket step | Result |
|---|---|---|
| 1 | In the materials library, add an equipment link on a `disconnect_switch` entry and confirm the catalog search returns bus-model results — BPS, HVL, MINIBREAK and bolted-pressure lineups all findable by name | ⚠️ **PASS for three of the four families** — `quick-search?type=disconnect_switch` returns **BPS · GE · LV Switchboard** (`skm_bus_oid 425107`), **HVL FUSED SWITCH** and **HVL UNFUSED SWITCH** · SCHNEIDER/SQUARE D · MV Switchgear (`425376`, `425389`), and two **MINIBREAK SWITCH** rows (`444021`, `444019`). Every row is `source: "skm"` with a `{skm_bus_oid}` ref, i.e. the bus-model catalog. **"bolted" and "PRESSURE" both return 0 rows**, so bolted-pressure lineups are not findable by those names. |
| 2 | Confirm the manufacturer filter is populated from the bus catalog and that selecting a manufacturer narrows the results correctly | ✅ **PASS** — `manufacturers?type=disconnect_switch` → 15 entries (ABB, ALLEN-BRADLEY, EATON/CUTLER-HAMMER, GE, LITTELFUSE, SCHNEIDER/SQUARE D, SIEMENS, plus ANSI/UL standards). Narrowing works: GE → 17 rows, ABB → exactly 1 (SafeGear HD (AR) · MV Switchgear Arc-Resistant). |
| 3 | Save a disconnect-switch link, reopen the entry, and confirm the stored link resolves and renders the chosen catalog entry rather than a blank or placeholder | ✅ **PASS on the reopen half** — the library grid shows "**Disconnect Switch · bus 452651**" for entry "1 sep abs", and opening it calls `resolve?skm_bus_oid=452651` → 200 `{label:"SafeGear HD (AR)", sublabel:"ABB · MV Switchgear Arc-Resistant"}`, rendered in the Equipment Linkage section with its Poles / Amps / Fuse class subsettings. A fresh save was deliberately not performed over shared QA data, so "save" is evidenced by the already-saved link resolving. |
| 4 | Negative — search a manufacturer that makes no disconnect switches and confirm an empty result, not an error or an unfiltered list | ⚠️ **CANNOT BE POSED AS WRITTEN / clean empty on a nonsense query** — a nonsense query returns 200 `{"rows":[]}`, a clean empty result with no error and no unfiltered fallback. But the manufacturer dropdown itself lists four ANSI documents and four UL standards, and filtering by **ANSI C37.06-1979** returns **6 rows** ("Indoor Oil Circuit Breakers · ANSI C37.06-1979 · MV Switchgear"). There is no selectable "manufacturer that makes no disconnect switches" — see FINDING 1. |
| 5 | Regression — confirm the other equipment types still search their own catalogs and are unaffected by the reroute | ✅ **PASS** — transformer → `skm_transformer_oid` (30 rows), cable → `skm_cable_oid` (30), fuse → `skm_device_oid` (30), circuit_breaker → `skm_device_oid` (30), panelboard → `skm_bus_oid` (20). Each type resolves in its own id space. |

---

## Findings

### FINDING 1 (Low) — the disconnect-switch manufacturer list contains standards documents, and they return rows
Eight of the fifteen entries offered in the Manufacturer filter are not manufacturers: **ANSI C37.06-1979, ANSI C37.06-1987, ANSI C37.06-1997, ANSI C37.16-1997, UL 347, UL 67, UL 845, UL 891**. Selecting ANSI C37.06-1979 returns six real rows, so these are how some bus models are attributed in the source catalog rather than dead options. It works, but a user picking a "manufacturer" is offered standards bodies alongside ABB and GE, and the ticket's negative case has nothing left to select.

### FINDING 2 (Low) — bolted-pressure lineups are not findable by that name
The ticket names bolted-pressure lineups as one of the four families that should be findable. Neither "bolted" nor "PRESSURE" returns anything for `type=disconnect_switch`. They may be catalogued under a manufacturer's own product name, but a user searching the words in the ticket finds nothing.

---

## Test data — direct links (QA)
- Materials library: https://acme.qa.egalvanic.ai/materials — entry "1 sep abs" carries the disconnect link (`skm_bus_oid 452651`)
- The endpoints the editor uses: `https://acme.qa.egalvanic.ai/api/equipment-catalog/quick-search?type=disconnect_switch&q=HVL` · `…/manufacturers?type=disconnect_switch` · `…/resolve?skm_bus_oid=452651`

Evidence: `docs/bug-evidence/zp-3942-disconnect-bus-catalog/`.

## Not covered / honest gaps
- **Saving a new disconnect link end to end** — the existing entry already carried one and was not overwritten, to avoid disturbing shared QA data; the resolve-and-render half is what was verified.
- **Bolted-pressure under an alternative product name** — not searched exhaustively.
- **Roles other than Super Admin** — mandatory MFA on QA blocks the other seats.
