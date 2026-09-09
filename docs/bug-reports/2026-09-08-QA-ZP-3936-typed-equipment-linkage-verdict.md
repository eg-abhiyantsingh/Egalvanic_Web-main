# [Web] Materials-library equipment links were untyped, so transformers, cables and busways could not be identified at all

**QA verdict — PASS. The editor leads with the equipment type: until one is chosen there is no catalog search to run, and the nine types the ticket names are exactly the nine offered — Panelboard, Switchboard, Disconnect Switch, Circuit Breaker, Fuse, Relay, Transformer, Cable, Busway. Each type scopes everything downstream: it loads its own manufacturer list, searches only the catalog tables that can identify it, and tailors the detail fields. Every type sampled returns results in its own id space — transformers by `skm_transformer_oid`, cables by `skm_cable_oid`, breakers and fuses by `skm_device_oid`, panelboards and disconnects by `skm_bus_oid`. The grid chip leads with the type, and a link saved before this change still loads.**

**Tested:** 2026-09-08 · `acme.qa.egalvanic.ai` build **V1.36**, bundle **`index-CSsDpG3c.js`** · tenant acme · Super Admin seat · Admin → Materials, live UI with the catalog endpoints captured.
**Ticket said "dev only, not yet promoted to cicd/qa" — wrong:** typed linkage is live on QA and existing library rows already carry typed refs.

**Artifact:** https://claude.ai/code/artifact/9cf81bd5-c985-4174-ad3c-e7059c057296

---

## QA-review checklist — results

| # | Ticket step | Result |
|---|---|---|
| 1 | Open Settings > Materials Library, edit an entry, and confirm the Equipment Link editor asks for an equipment type before it offers a catalog search | ✅ **PASS** — the **Equipment Linkage** section opens with **Equipment type** empty; Manufacturer (filter), Catalog entry, Poles, Amps and Fuse class sit inert beneath it and no search can be issued. Choosing a type is what activates the rest — the type pick is genuinely the required first step. |
| 2 | For each of Panelboard, Switchboard, Disconnect Switch, Circuit Breaker, Fuse, Relay, Transformer, Cable and Busway, run a search and confirm results come back and the detail fields shown match the class | ⚠️ **PASS for six of nine, and the type list is exactly right** — the dropdown offers precisely those nine types (plus an empty option). Searches returning rows in their own id space: **disconnect_switch** (BPS, HVL FUSED/UNFUSED, MINIBREAK — `skm_bus_oid`), **panelboard** (20 rows, `skm_bus_oid`), **transformer** (30, `skm_transformer_oid`), **cable** (30, `skm_cable_oid`), **fuse** (30, `skm_device_oid`), **circuit_breaker** (30, `skm_device_oid`). **Switchboard, Relay and Busway were not searched individually.** Field tailoring is real: on Circuit Breaker the Fuse class field disappears and a Frame select appears once an entry is picked; on Disconnect Switch the Fuse class field is present. |
| 3 | Circuit Breaker should offer a frame list, Cable a size list, Transformer a kVA list. Pick one, save, reopen the entry and confirm the choice was stored with the link | ⚠️ **PARTIAL — the breaker frame list was confirmed live** — picking "ADVAC · __ADV__" returns `option_label: "Frame"` with options "1200A · 4760V · 41kA", "1200A · 4760V · 29kA" and more, each with a `patch` of `{ampere_rating, skm_frame_sid}`, and a **Frame** select duly appears. The cable-size and transformer-kVA equivalents were not opened, and no fresh save/reopen cycle was performed (shared QA data). The stored-with-the-link half is evidenced by the existing disconnect entry, whose ref carries its subsettings (`ampere_rating`, `fuse_class`, `pole_count`) beside `skm_bus_oid`. |
| 4 | Confirm the materials-library grid chip for a linked entry leads with the equipment type | ✅ **PASS** — the Equipment Link column reads "**Disconnect Switch** · bus 452651", "**Fuse** · SKM 550331", "**Fuse** · family 144"; unlinked rows show an em dash. Type first, every time. |
| 5 | Negative: pick an equipment type whose catalog table is not seeded on dev and confirm the save is refused with an error rather than storing an unresolvable link | ⚠️ **NOT EXERCISED** — every type sampled on QA returned rows, so no unseeded table was found to provoke the fail-closed path. |
| 6 | Negative: open a link created before this change (under ZP-3935) and confirm it still loads without error | ✅ **PASS** — "Bussmann LPJ Class J current-limiting fuse" carries the older shape `{equipment_type:"fuse", family:"LPJ (Class J)", fuse_class:"J", ampere_rating:null, pnl_device_id:144}` — free-text `family`, non-SKM `pnl_device_id` — and both its grid chip and its edit dialog render without error. |
| 7 | Run the resolution agent so it mints a new material, then confirm the created materials-library row has a type and a unit set (Material / Unit if nothing more specific resolves) rather than blank | ❌ **NOT EXERCISED** — no agent run is triggerable on QA (`reevaluate` → `{issues:1, jobs:0}`, nothing minted). Adjacent evidence: the one material created by hand on a resolution during this session came back `cost_source: "manual"`, `ml_id: null`, `catalog_ref: null` — i.e. the unlinked-by-name path, not an agent mint. |

---

## Test data — direct links (QA)
- Materials library and its Equipment Link column: https://acme.qa.egalvanic.ai/materials
- Per-type searches: `…/api/equipment-catalog/quick-search?type=transformer&q=a` · `…?type=cable&q=a` · `…?type=fuse&q=a` · `…?type=circuit_breaker&q=a` · `…?type=panelboard&q=a` · `…?type=disconnect_switch&q=HVL`
- Legacy ref that still loads: entry "Bussmann LPJ Class J current-limiting fuse" (`ml_id` in `/api/materials-library`)

Evidence: `docs/bug-evidence/zp-materials-library-equipment-linkage/`.

## Not covered / honest gaps
- **Switchboard, Relay and Busway searches** individually, and the **cable-size / transformer-kVA** secondary pickers.
- **The unseeded-table negative** — no unseeded type was found on QA.
- **A fresh save-and-reopen cycle** — avoided to leave shared library data untouched.
- **Agent-minted rows carrying type and unit** — no agent run is triggerable on QA.
- **Roles other than Super Admin** — mandatory MFA on QA blocks the other seats.
