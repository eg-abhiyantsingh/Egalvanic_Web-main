# 2026-09-03 — ZP-3836 SKM round-trip QA verdict (tested on QA V1.36)

**Prompt:** Test ZP-3836 "[Web] SKM round trip duplicated every asset on re-import — Alternate UID
sync, meter ports, library-less properties, and create-vs-update review" (backend #1049 + frontend
#1234) and share an Artifact with plenty of screenshots.

**Artifact:** https://claude.ai/code/artifact/1f236413-4c58-4635-8961-71332f88b62c

## What was done
- Live-tested the whole QA-review checklist through the real frontend (Playwright MCP) on
  `acme.qa.egalvanic.ai` V1.36 — no request tampering; API payloads captured from the browser
  session only as corroborating evidence.
- **Real-study leg (Android Site, read-only):** exported 1,238 components → re-import preview =
  0 New / 1238 Updated, "Update 1238 assets" button, pinned-footer modal measured (no overlap).
  All 1,238 altIds 36-char; meter exports ConnectedComponent2 (port 2).
- **Controlled fixture leg ("Addtioanl Site" `fd1e25c4`, empty):** hand-crafted a 10-component SKM
  study in the exact vocabulary (learned from the platform's own skm-cable/transformer-library
  `export-xml` endpoints) → create import (10 nodes/9 edges) → EG export → re-import commit →
  same 10 UUIDs, same x/y, 0 duplicates. Variants: removed edge ×2, rewire, partial (3-comp),
  foreign-SLD UIDs, renamed meter, mixed (+1 new), kVA 500→750 overwrite.
- **Library resolution proven in persisted rows:** KeyInLibrary=3 → IEEE 141 Red Book eqp_lib;
  KeyInLibrary=0 → typed values on node columns with NO phantom card; zero kVA/Z% → null not 0.
- **Required markers:** Length*/Cable Size* red asterisks in the matched-lib card, save NOT
  blocked (PUT /node/update 200 with empty Length).

## Verdict
10 PASS · **1 FAIL** · 1 PARTIAL (cross-company UID negative not testable on QA; SLD-scope proven
live — foreign-SLD UUIDs all treated as New).

**Defect D1 (Medium-High, silent data-integrity):** connection removal never happens.
Three probes: deleting an edge from the file (two different edges/declaration styles) →
`removed_edges: 0`, edge survives; rewire probe → new edge created AND stale edge kept →
TX-ZERO double-fed (9→10 edges). Contradicts the ticket's "connections between two synced assets
absent from the file are removed" and its QA item "exactly that one edge is removed".

## Key technique notes (for future SKM testing)
- SKM import UI: Assets → SKM → **Import (Full)**; export: SLD page → Export → **Export
  Engineering XML**. Endpoints: `POST /skm/import-xml/preview|commit` (FormData `xml_file`+`sld_id`).
- Fixture vocabulary source: `POST /api/skm-cable-library/cable/{oid}/export-xml?size_id=` and
  `/api/skm-transformer-library/transformer/{oid}/export-xml?kva_entry_id=` emit real SKM blocks
  (KeyInLibrary, CableModel, "Nominal kVA", "Z%", ConnectedComponent ids per type).
- Empty-site "Let's get your assets in" onboarding dialog intercepts clicks on /assets — dismiss
  "Not now" first. MCP-created sites (no creating user) do NOT appear in the user's site list.

## Files
- Screenshots: `zp3836-01…14-*.png` (repo root, 14 real pixel captures)
- Fixture XMLs preserved in session scratchpad (crafted study + 6 mutation variants)
