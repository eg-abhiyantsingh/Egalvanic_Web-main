# SKM export: Xformer2/3 from first-class node columns (backend #ZP-…) · QA verdict

**Tested:** 2026-08-18 · **Build:** QA **V1.36** · **Tenant:** `acme.qa.egalvanic.ai`
**PR:** eg-pz-backend — `app/services/skm_xml_output_simplified.py` (+45/−0, `_first_class_transformer_fields` for `skm_type in (Xformer2, Xformer3)`) · pairs with the cable fix ZP-3654
**Ticket note:** *"Merged to cicd/dev only. Not yet in QA at time of filing."*

---

## Verdict — PARTIALLY VERIFIED; full verification blocked by a fixture I can't build via API

**What I could confirm is genuinely good:** the SKM exporter is live on QA and emits populated `<s:Xformer2>` datablocks carrying the transformer nameplate fields. **What I could not confirm** is that *this PR's specific nameplate code path* is what produces them (vs. the pre-existing library/core-attributes path), nor the three fields that are new to this PR (**Pri RatedVoltage, SystemNominalVoltage, Type**), because a clean **nameplate-only** transformer (no equipment-library pick) has to be an **SLD-graph node**, and I could not create one through the API.

I'm flagging this honestly rather than declaring PASS off transformers whose modeling I couldn't control.

## What IS confirmed (live on QA)

- **Export path works:** `POST /api/sld/{sld_id}/export-skm` → `{presigned_url}` → XML on S3. XML uses `<s:Xformer2 …>` components with `<s:Field s:name="…" s:value="…"/>` rows.
- **Real transformers export with nameplate fields.** Exporting the *Z — Hospital* SLD (`d5d99deb…`, 154 components) produced **10 `<s:Xformer2>` datablocks**, each carrying:
  - `Nominal kVA`, `FullLoad kVA` (e.g. UTILITY XFMR = **1500.0**)
  - `Z%` (e.g. **5.75**)
  - `Sec RatedVoltage` + `SystemNominalVoltageSecondary` (e.g. **480.0**)

  Sample (verbatim):
  ```xml
  <s:Xformer2 s:action="create" s:id="5" s:name="UTILITY XFMR" …>
    <s:Field s:name="Nominal kVA" s:value="1500.0"/>
    <s:Field s:name="FullLoad kVA" s:value="1500.0"/>
    <s:Field s:name="Z%" s:value="5.75"/>
    <s:Field s:name="Sec RatedVoltage" s:value="480.0"/>
    <s:Field s:name="SystemNominalVoltageSecondary" s:value="480.0"/>
    <s:Field s:name="ConnectedComponent1" s:value="UTILITY FUSE-UTILITY XFMR:1"/>
    <s:Field s:name="ConnectedComponent2" s:value="02050-MAIN SWG BREAKER:1"/>
  </s:Xformer2>
  ```

## What I could NOT verify, and exactly why

| Ticket verification item | Status |
|---|---|
| Xformer2 from nameplate fields (no library pick): kVA, FullLoad kVA, Z%, Pri/Sec RatedVoltage, SystemNominalVoltage(Secondary), Type | ⚠️ **Blocked** — see below |
| Repeat for Xformer3 (3-winding) | ⚠️ **Blocked** |
| Regression: library-linked transformer unchanged (library wins) | ⚠️ **Blocked** |
| Blank nameplate → valid topology stub, no empty-string fields | ⚠️ **Blocked** |
| Round-trip into SKM/PTW | ❌ out of reach (no PTW) |

**The blocker.** The change only affects a transformer with **no equipment-library pick**, priced from its first-class node columns (`kva_rating`, `percent_impedance`, `voltage`, `secondary_voltage`, `trip_type_id`). To exercise it I must have such a transformer **as a node on an SLD** and export that SLD. On QA:

- `POST /api/node/create` accepts the payload and returns `200` + echoed columns + `_mutation:{status:"received"}`, **but the node never lands in the SLD.** I confirmed this twice — a full nameplate transformer *and* a minimal control transformer both returned 200 yet were **absent from `/api/lookup/v2/nodes/{sld}`** afterward, and the export's component count did not change (184 → 184; 0 `<s:Xformer2>` produced).
- So a bare API `node/create` does **not** wire a node into the SKM-exportable topology — transformers are placed through the **SLD editor (GoJS canvas)**, which isn't reasonably scriptable.

I did not find a nameplate-only transformer among existing SLDs either (the node **list** endpoint doesn't expose the nameplate columns, and node **detail** isn't reachable at `/api/node/{id}`).

## One observation for the dev (NOT a defect on current evidence)

The Z-Hospital Xformer2 blocks carry the **secondary**-side fields but **no `Pri RatedVoltage`, no `SystemNominalVoltage` (primary), and no `Type`** — the three fields this PR adds. Three innocent explanations, and I can't distinguish them without the fixture:
1. those transformers are **library-linked** (so the new nameplate path never runs), or
2. their `node.voltage` (primary) and `trip_type_id` (cooling type) simply **aren't set**, so `setdefault` emits nothing (working as designed — "every value emits at face value"), or
3. the PR **isn't deployed to QA yet** (matching the ticket's own note).

Worth the dev confirming which, since the primary-voltage + Type emission is the heart of the change and I saw it on **zero** exported transformers.

## How to complete this QA (needs one of)

1. **Dev supplies the test SLD** used in local verification (a nameplate-only Xformer2 and Xformer3), and I re-export + diff — fastest.
2. **Place a transformer via the SLD editor** with kVA/%Z/primary+secondary voltage/cooling type and no library link, then export — I can drive this in the browser if you want, but GoJS-canvas automation is slow/fragile.
3. Confirm **deployment**: is this backend PR actually on QA yet? If not (per the ticket), this is correctly "not testable until deployed."

## Method notes
- Export endpoint + XML schema captured from live UI traffic and re-run via API.
- Evidence: `/tmp/skm_hosp.xml` (Z-Hospital export, 10 Xformer2 blocks). Two orphaned test nodes (`e925ef34`, `ac78c6db`) were created but never persisted to the SLD, so there's nothing to clean up.
