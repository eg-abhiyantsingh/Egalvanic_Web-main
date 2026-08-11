# Node mutations, class attributes, and arc-flash inputs

**Verified:** 2026-08-11 against QA **V1.36**, tenant `acme.qa.egalvanic.ai`.
Origin: TEGG/SKM Arc-Flash Inputs ticket (FE #1075/#1076/#1077, BE #895–#906).

## The async mutation pipeline — a 200 does NOT mean it was written

`POST /api/node/create` returns **HTTP 200/201** with the submitted payload echoed back and:

```json
"_mutation": { "mutation_id": "7fda48e9-…", "status": "received" }
```

`received` is not `applied`. The write happens asynchronously afterwards, and **if it fails the
asset is silently never created** — no error status, no error body, and no mutation-status
endpoint could be found to poll (`/api/mutations/{id}` and four variants are all masked 404s).

**Testing implication — this will make you report a false bug.** Reading the node straight after
the POST returns `null` for *every* field, including on writes that succeed. You must wait
(~9 s was reliable) before reading, and the authoritative existence check is the SLD listing
(`GET /api/lookup/v2/nodes/{sldId}?page_size=200`), not the enriched GET.

Verified behaviour, identical payloads differing only in one value:

| Field value | HTTP | Asset exists after settle |
|---|---|---|
| `aic_rating: 65` / `0` | 200 | yes (`0` persists as `0`) |
| `aic_rating: 65.5` | 200 | yes — stored as `65` (int truncation) |
| `aic_rating: -5` / `2147483648` / `"abc"` | 200 | **NO — silently dropped** |
| `com: "xyz"` / `width: "big"` | 200 | **NO — silently dropped** |
| `node_class:` all-zeros UUID | 200 | **yes** — asset created with **"No Class"** |

So: any **type-invalid** field discards the whole asset, but a **non-existent class UUID** is
accepted. The drop is a property of the pipeline, not of any one field — always run a control with
a different bad field before blaming the field you are testing.

## Where class attributes actually live

**`GET /api/node_classes/user/{userId}`** → 43 classes for this tenant. Attributes are **inside
each class's `definition[]` array**, not at a separate endpoint:

```json
{ "key": "ampereRating", "name": "Ampere Rating", "type": "select",
  "options": ["15A", "20A", …], "af_required": true,
  "description": "…", "exclude_ai": false, "id": "f0c95425-…" }
```

- `af_required` — this attribute is required for the arc-flash calculation.
- `/api/node_classes/{id}` (single class) is a **masked 404**. Only the list endpoint works.
- The **Classes → Core Attributes** tab is fed by this same list endpoint — no extra call.
- The Classes page's **Asset Classes** grid shows "No rows" for this tenant; company-level
  overrides only. Not a bug on its own.

Useful class fields: `requires_phase_config`, `device_role_id` / `device_role_code`, `is_node_bus`,
`skm_config` (drives SKM `EquipmentCategory`), `default_datablock_config`, `has_panel_schedule`.

### The arc-flash attribute set

`electrodeConfig`, `enclosureHeight`, `enclosureWidth`, `enclosureDepth` — present on seven classes:
**Busduct, Junction Box, MCC, Panelboard, PDU, Switchboard, VFD Panel**. `sections` is on **MCC and
Switchboard** only.

**Don't shorten this to "the bus classes".** The implication runs one way only: every class with the
AF set is bus-role (`device_role_id = 3`), but there are **10** bus-role classes and 3 lack the set —
**Disconnect Switch, Other, Node Bus**. The AF set is a *strict subset* of bus-role. (Disconnect
Switch and Other are supposed to lack it; Node Bus has no attributes at all.) Asserting
co-extension here was an error caught in review on 2026-08-11.

`requires_phase_config` is a **strict boolean on all 43 classes** — never null/undefined. That
matters because FE gates differ (`!== false` for rendering vs `=== true` for readiness); with no
null in the data the two can never disagree, so that class of bug is latent only.

## `aic_rating` is a node column, not a class attribute

Do not look for it in `definition[]` — it is not there, and its absence is **not** a bug. It is a
first-class column on the node, reachable at:

**`GET /api/graph/nodes/{nodeId}/enriched`** → 116 keys including `aic_rating`,
`phase_configuration_id`, `busway_ampere_rating`.

The app calls this with a **doubled prefix** (`/api/api/graph/nodes/…`); both the doubled and
single forms return identical JSON, so routing tolerates it.

The **list** projection `/api/lookup/v2/nodes/{sldId}` **omits `aic_rating`** — a scan over the
list will conclude "no node has an AIC" regardless of the truth. Same class of trap as the
paginated EG-Forms endpoint dropping `definition`.

## SKM export is two-stage

`POST /api/sld/{sldId}/export-skm` returns **JSON**, not XML:

```json
{ "component_count": 2, "connection_count": 0, "presigned_url": "https://…s3…/skm_export_….xml",
  "unconfigured_classes": [], "warnings": [], "success": true }
```

Fetch the XML from the presigned S3 URL (expires in 3600 s). **CSP blocks that fetch from the
page** — use curl.

Bus elements carry the AIC as `ShortCircuitRating`, formatted `%.3f`:

```xml
<s:Bus s:action="create" s:name="…">
  <s:Field s:name="EquipmentCategory"  s:value="LV Switchboard"/>   <!-- from class skm_config -->
  <s:Field s:name="NodeBus"            s:value="False"/>
  <s:Field s:name="ShortCircuitRating" s:value="65.000"/>           <!-- from node.aic_rating -->
</s:Bus>
```

`aic_rating = 0` exports as **`0.000`** — an explicit "no bracing" value rather than missing data,
and the field is simply **omitted** when the AIC is null.

## Write endpoints worth remembering

| Action | Endpoint |
|---|---|
| Create asset | `POST /api/node/create` |
| Delete asset | `DELETE /api/node/delete/{nodeId}` |
| Node detail | `GET /api/graph/nodes/{nodeId}/enriched` |
| Assets on a site | `GET /api/lookup/v2/nodes/{sldId}?page=1&page_size=N` |
| SKM export | `POST /api/sld/{sldId}/export-skm` |
| Bulk edit template / export | `GET /api/bulk-edit/template` · `GET /api/bulk-edit/export?sld_id=` |
| Signature donors | `GET /api/extraction/signatures/donors/{sldId}` |

`PATCH`/`PUT` on `/api/graph/nodes/{id}` returns **405** — the path exists but takes neither.
The node-update endpoint was not found; asset detail pages are read-only, and edits happen via
the SLD editor or Bulk Import.

## Equipment Designations lives at an unlinked route

**`/equipment-designations`** (and a WO-embedded variant) — both render
`NFPA70EDashboard mode="equipment-designations"`. **Not in the sidebar**, and not under
`/arc-flash` (whose tabs are Overview / Asset Details / Source-Target Connections /
Connection Details). Hosts the **Signatures** and **AI Extraction** dialogs from #1077.

Selecting rows swaps the toolbar to **"Run Extraction (N)"** — that starts a **real, billable AI
job** and mutates assets. Don't click it casually.

Extraction eligibility needs **nameplate photos**. On QA's test SLDs no asset has any, so the
Signatures dialog reports "All eligible devices have a signature" with "Extract all (0)" and no
per-row run buttons. The apply → close refresh path of #1077 is therefore not testable on current
QA data.

## Bulk edit round-trip

`node.aic_rating` **is** a bulk-import column (sheet 1, header `node.aic_rating`), so bulk import
is a third write path alongside web and iOS. The import preview shows proposed diffs per row and
**does no range validation** — `-5` and `2147483648` appear as ordinary changes.

**Unresolved, needs dev input:** exporting a site and re-importing with only AIC cells changed
produced a preview proposing **18 connection updates** (e.g.
`source_asset_name: ATS-EM-L-Cable90-BUS → ATS-EM-L`). The exported Connections sheet already
holds the clean name and **no node named `…-BUS` exists** on that SLD, so the "before" value is
composed somewhere. Either the preview is misreporting, or export→import is not idempotent. I did
not process the import to find out. The connections API paths I tried are all masked 404s.
