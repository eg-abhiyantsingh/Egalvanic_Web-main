# ZP-4645 — [Task #102] SLD issue severity rework + SKM export acknowledgement gate

**Verdict:** PASS. Three severity mappings (missing phase config, incompatible phase config, eqplib voltage mismatch) were checked in the bundle only; no QA data produces them (see Not covered).
**Build:** QA `index-D1IWZ6SI.js` (SLD and SKM chunks identical to `index-BdXGlAq5.js` with hashes ignored).
**Tested:** 12 Oct 2026, 19:15–19:45 IST, acme.qa.egalvanic.ai, Project Manager seat.
**Test data:** my site `QA-DEMO ZP-4594 SLD notes groups realtime v2.4 (delete me)` (`bb593be6-…`), built up in stages:
1. A clean chain.
2. Plus `QA-4645 Breaker 1`.
3. Plus `QA-4481 MCC Bucket 1` (uncategorized class).
4. Plus `QA-4645 Switchboard 2` wired directly to Panelboard 1 (bus-to-bus), later deleted.
5. Plus `QA-4645 Utility 3 (208V)` feeding the 480 V Panelboard (voltage conflict).

Read-only checks ran on acme sites "Abhiyant test 12", "Test 123456", "Web Copy Asset (location)" and "Migration testing 31 to 32".
**Flags:** the SKM export entry points use `feature-skm-export`, which is live ON for Acme. The gate itself has no flag.

## Checked

| Check | How | Result |
|---|---|---|
| Warnings (voltage) | Voltage conflict built on my site; "Source without voltage" on my site and Abhiyant test 12 | PASS. **Voltage conflict detected** and **Source without voltage …** show as warnings (orange) (shot 14). |
| Errors (topology) | Panelboard→Switchboard wired directly; Load↔Load same role; populated site | PASS. **Buses connected without impedance**, **Invalid same-role connection**, **Missing node bus**, **Source connected directly to endpoint**, **AC and DC equipment connected** and **Illegal children** all show as errors (red) (shots 10, 12, 13). |
| Uncategorized / SKM-unsupported class is an error | MCC Bucket on my site; 14 assets on Abhiyant test 12 | PASS. "Custom asset class(es) not supported by Egalvanic" shows as an error. No acme class has the `unsupported` role, so that row's severity (error) is checked in the bundle only. |
| The permanent "settings revert" / "ground fault dropped" rows are gone | Every issue row on 4 sites, plus a bundle search | PASS. No such rows anywhere; `skm.settings_revert` and `skm.ground_fault_dropped` no longer exist in the bundle. Breaker and Red Book detection now drive the export notice. |
| Clean model: no gate | SLD → Export → **Export Engineering XML** with no errors and no breakers | PASS. Only the existing "Choose what to include" options (arc-flash label numbers) appear. There are no acknowledgement boxes and Export is enabled (shot 2). |
| Breakers present: Settings Verifier acknowledgement | Same, after adding a Circuit Breaker | PASS. One box: "I understand that after importing to SKM, I will need to run the Settings Verifier (**download EG Utils**) …". Export stays disabled until it is ticked. **Learn more** expands its three sections (shots 3–4). The EG Utils link is `…/eg-utils/EG-Utils.msi` and returns 200. |
| Error-level issues: topology acknowledgement plus error list | Same, with the MCC Bucket | PASS. "1 error in this model", the error card, and **both** boxes. Export is disabled with zero or one box ticked and enabled only when both are ticked (shots 5–6). |
| Assets → SKM → Export | Pick the scope, then **Export** | PASS. The same gate appears after the scope step, lists the error and keeps Export disabled (shots 7, 9). |
| The export itself | Tick both boxes, then Export | PASS. One `POST /sld/{id}/export-skm` (1.5 s) downloads `skm_export_bb593be6_….zip` containing the XML, **READ THIS BEFORE IMPORTING.pdf** (company, scope, "1 errors, 1 warnings", each issue) and `uncategorized-assets.csv` (shot 8). |
| Pre-export check runs once | Network from clicking Export to the download | PASS. `GET /sld/{id}/graph` runs **once**, reused for the bundle PDF, and `export-skm` runs once. |
| LOTO gating (shared with ZP-4481) | Right-click menu | PASS. Blocked while errors or warnings stand; enabled at "No issues". |
| Performance | Network | The preflight also re-fetches `node_classes`, `edge_classes`, `enum-node-voltages` and `enum-node-phase-configurations` (4 calls, 0.2–1.0 s each) although the editor already holds them. Total time from click to dialog is about 2 s. |
| Roles | | Export is available to the PM (and to roles with SLD access). Client Portal has no `/slds` access. |

## Bugs found
None.

## Enhancement suggestions
1. **Reuse the editor's loaded classes and enums for the preflight.** That saves 4 calls (about 1–2 s) before the dialog appears.
2. **Add "Show on SLD" to each error card in the dialog,** so the user can fix it instead of exporting a broken model.
3. **List warnings in the dialog as non-blocking.** They are already in the PDF; seeing "1 warning" before export avoids surprises.
4. **Pre-tick nothing, but remember "Generate arc-flash label numbers from location".** It is already remembered in `localStorage`; also show that it was remembered.

## Mobile part for Nency
- `src/sld` changed, so after the SLD-viewer OTA republish, open site `bb593be6-…` on iOS. The same rows must show with the same colours (errors red, warnings orange), with no "settings revert" or "ground fault dropped" info rows.
- If iOS offers SKM export (it probably does not), it needs the same gate.

## Not covered
- Missing phase config, incompatible phase config and eqplib voltage mismatch as warnings, plus overcrowded terminal and SKM-unsupported class as errors. Checked in the bundle's severity table only; I did not build data for them.
- 3 AWG Red Book cables triggering the Settings Verifier box (the breaker path was tested).
- `feature-skm-export` OFF. The gate is not flag-gated; the flag only hides the SKM menu.

## Re-check 13 Oct on index-C-Cc1qD1.js
Live QA build `index-C-Cc1qD1.js` (deployed 13 Oct 15:21 IST, newer than `index-COeCBn0z.js`). Re-run 13 Oct 15:58–16:01 IST, PM seat. Live ticket re-read: still In QA, no comments. LD live for Acme: `feature-skm-export` ON. Script: `.playwright-mcp/v24/sld-13oct/b.body.js`.

| Check | Result 13 Oct |
|---|---|
| Severities on my site | PASS. "Custom asset class not supported" = error (red); "Voltage conflict detected" (480 V vs 208 V source) = warning (orange). |
| Severities on a populated site ("Abhiyant test 12") | PASS. Errors: custom classes, AC and DC equipment connected, Source connected directly to endpoint, Missing node bus, Illegal children. Warnings: Source without voltage (8 rows). |
| Old permanent info rows gone | PASS. No "settings revert" / "ground fault dropped" row on any site; the 120 loaded JS chunks contain neither `skm.settings_revert` nor `skm.ground_fault_dropped`. |
| Export gate, model with an error + a breaker | PASS. SLD → Export → Export Engineering XML: "1 error in this model", the error card, and two boxes (topology + Settings Verifier with the EG Utils link). Export disabled with 0 and with 1 box ticked; enabled only with both (shots 1–2). |
| Export result | PASS. One `POST /sld/{id}/export-skm` (0.5 s) → zip with the XML, `READ THIS BEFORE IMPORTING.pdf` and `uncategorized-assets.csv`. |
| Positive control: clean model | PASS. "Migration testing 31 to 32" ("No issues", no breakers): only the label options, no acknowledgement boxes, Export enabled (shot 3). Cancelled, nothing exported. |
| Performance | Unchanged: the preflight still re-fetches node_classes, edge_classes, voltages and phase configs (0.6–0.9 s each) plus one `/graph`. Enhancement only. |

What still holds: every 12 Oct claim. What changed: nothing.
Out-of-scope bugs: none.

## Proposed Jira comment
```
h3. QA re-check (13 Oct, QA build index-C-Cc1qD1.js) — PASS

Tested as Project Manager on my site *QA-DEMO ZP-4594 SLD notes groups realtime v2.4 (delete me)* (has a custom-class asset, a breaker and a 208 V vs 480 V voltage conflict), plus read-only checks on acme sites.

||Check||Result||
|Custom class / AC-DC / source-to-endpoint / missing node bus / illegal children|PASS — errors (red)|
|Voltage conflict / source without voltage|PASS — warnings (orange)|
|Old "settings revert" / "ground fault dropped" rows|PASS — gone|
|Export Engineering XML on a model with an error and a breaker|PASS — error list + 2 boxes; Export only enabled when both are ticked|
|Export result|PASS — zip with the XML, "READ THIS BEFORE IMPORTING.pdf" and uncategorized-assets.csv|
|Control: clean model with no breakers|PASS — no boxes, only the label options, Export enabled|

Not built on QA: missing/incompatible phase config, eqplib mismatch, overcrowded terminal, 3 AWG Red Book (checked in code only).

!QA-13Oct-ZP-4645-1-PASS-export-gate-error-list-and-two-acknowledgements.png|width=720!
!QA-13Oct-ZP-4645-2-PASS-both-acknowledged-export-enabled.png|width=720!
!QA-13Oct-ZP-4645-3-PASS-control-site-export-dialog.png|width=720!
```

## Recommended status
READY TO RELEASE
