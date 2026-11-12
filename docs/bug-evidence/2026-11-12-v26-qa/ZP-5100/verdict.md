# ZP-5100 — Hotfix: SKM-linked cables never arc-flash ready + SKM import leaves bus, transformer and core attributes empty

**Verdict:** PARTLY — QA, Web v2.6, build `index-C1ZRIunF.js`, 12 Nov 2026 16:50–17:00 IST, Super Admin seat. Cable N/A + arc-flash-ready,
bus ampere rating + phase on import, and the blank re-export all PASS; transformer Type and reserved core attributes on import could not
be shown with my fixture (not verified, not failed).

**What changed since 2/5 Nov:** I then looked at the library ENTRIES (`/skm-cable-library/cable/…`), which still show a blank insulation
type / installation — and still do today, by design ("export back as blank (SKM's own value)"). The N/A lives on the **cables bound to**
those entries. Today's checks look there.

**Test data (own):** new site "QA-DEMO ZP-5100 12 Nov SKM import" (SLD `7dffbd91-676a-49cf-a3c9-b9d11ba4496d`), imported from
`QA-DEMO-ZP5100-12Nov-import-fixture.xml` (in this folder): utility, LV switchboard bus (ContinuousRating 800), breaker, LV panel bus (225),
NEC cable block taken from the platform's own library "Download XML" (cable 456624), hand-entered 75 kVA transformer, 208 V panel bus (100).

## Checked
| Check | How | Result |
|---|---|---|
| Library entries keep SKM's blank value | `GET /api/skm-cable-library/cable/456624` and `/429447`; library pages | as designed — NEC: insulation_type null, installation "Conduit (Steel)"; Typical: both null; pages show no N/A |
| Library XML writes N/A as blank | cable page › Download XML (456624) | PASS — `InsulationType=""`, `Installation="Conduit (Steel)"`, no literal "N/A" |
| Existing bound cables carry N/A (data script) | read-only scan of 120 SLDs, 23 library-bound cables | PASS — every cable bound to a Generic/Custom entry with a blank axis has **insulation N/A / installation N/A** (8 cables, modified 2026-10-28 20:53 UTC = script run) |
| Negative: real values untouched | same scan | PASS — Red Book PVC/Conduit (9), XLP3/Direct Buried (2), XLP3/Free Air, XLP1/Duct Bank, NEC PVC/Air, ALCAN XLPE/Conduit keep their values |
| Imported NEC cable gets N/A | Assets › SKM › Import (Full) › Analyze › Import 7 assets; API read-back | PASS — QA5100-CBL-NEC bound to NEC 456624 (resolved_eqp_lib 1), insulation_type_label **N/A**, installation "Conduit (Steel)" |
| Cable can reach arc-flash ready | Arc Flash Readiness on the imported site | PASS — Cable 100% (1/1 complete) |
| Import sets bus ampere rating + phase | API read-back | PASS — SWBD 800 A / 3P4W, PNL 225 A / 3P4W, PNL208 100 A / 3P4W (mains MLO) |
| Import sets transformer Type | hand-entered Xformer2 (kVA 75, Z% 5.5, 208 V) | not shown — kVA, %Z and secondary voltage imported; no subtype/Type set. My block had no model name to infer from, so this row is **not verified** |
| Import writes reserved core attributes | API read-back | not shown — core_attributes empty on all 7 assets; my fixture carried no electrode/winding fields, so **not verified** |
| Re-export re-emits values | SLD › Export › Export Engineering XML (imported site) | PASS for ContinuousRating (800/225/100) and blank InsulationType; no electrode/winding/Type fields emitted (nothing set); 0 literal "N/A" |
| Import UI | review screen | 7 assets · 6 connections · 1 library matched · 3 unmatched; "Import successful — Created 7 assets and 6 connections" |
| SIEMENS entries | — | none bound on QA (not covered) |
| Console | run | no app errors |

## Bugs found
None proven.

## Enhancement suggestions
- Library entry page: show "N/A (blank in SKM)" for a blank axis instead of hiding the row, so users see why a bound cable shows N/A.
- Import review: show which fields will be set per asset (amps, phase, Type, core attributes) so engineers can spot gaps before committing.

## Mobile impact
None directly (backend + data). Mobile asset screens should show N/A for insulation/installation on library-bound cables.

## Not covered
- Transformer Type inference and reserved core attributes (electrode configuration, winding connections) — needs a real SKM study
  (e.g. Koch Building A) or the exact SKM field names the importer reads. Ask Eric for a sample file.
- The Koch Building A before/after readiness numbers (prod data).

## Proposed Jira comment
See `ZP-5100.txt`.

## Recommended status
Stay **In QA** until the transformer-Type and core-attribute rows are run with a real SKM study (all other rows pass).
