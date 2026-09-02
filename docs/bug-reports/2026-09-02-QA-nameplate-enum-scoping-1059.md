# Nameplate extractor offered enum options the asset's class does not allow

**QA pre-verification — eg-pz-backend #1059 / pipeline #69 (follows ZP-3856)**
**Measured:** 2026-09-02 · acme.qa.egalvanic.ai (QA only) · fix is dev-only, NOT on QA.
**Artifact:** https://claude.ai/code/artifact/dfb336cd-8484-4555-a4cc-f49f53f2deb6

## The oracle — every ticket number independently confirmed on QA
| Enum / class | Unscoped (pre-fix) | Correctly scoped | Ticket claims |
|---|---|---|---|
| Mains types — Disconnect Switch | 6 | **3** (MCB, Fuse, Non) | 3 ✓ |
| Mains types — Panelboard | 6 | **4** (MCB, MLO, FDS, NFDS) | 4 default_visible ✓ |
| Insulation classes — busway | 32 | **3** (Class A/B, N/A) | 3 ✓ |
| Insulation classes — cable | 32 | **29** | "32 to 29" ✓ |
| Conductor descriptions — busway | 29 | 17 | — |
| Insulation types — busway | 14 | 3 | — |
| Installations — busway | 29 | 6 | — |
| Duct materials — busway | 6 | 2 | — |

Mains-type figures derived by applying the ticket's own ZP-2550 rule to /api/enum-node-mains-types
(rows carry allowed_classes / mapped_classes / default_visible): Disconnect Switch has explicit rows for
MCB/Fuse/Non → 3; Panelboard has none → default_visible 4.

## TRAP to test hardest (found in advance)
The acme (company) Disconnect Switch class carries the explicit mapping → 3 options. The GLOBAL Disconnect
Switch class carries none → falls back to 4 (adds MLO/FDS/NFDS). So resolving the global side instead of the
company's for_entity override silently offers 3 wrong options. **3-vs-4 is the tell** — assert 3.

## Robustness gap (adjacent, not a blocker)
/api/skm-cable-library/insulation-classes: ?is_busway=true → 3, =false → 29 (correct), but **=1 → 32 and
=0 → 32** (full unscoped list, no error). Only literal true/false parse. Same "silently wrong list" shape as
the ticket; the fix itself passes :is_busway as a SQL bind param so likely unaffected, but any HTTP client of
these endpoints inherits it. One-line hardening.

## Not verified
- The fix's behaviour (not on QA; _enum_options is server-side, no web fingerprint either way).
- All six checklist runs — need the fix on QA + a legible nameplate photo fixture (same blocker as ZP-3856).
- Single-asset vs bulk Lambda agreement (Step Function).
- The form's own Mains Type picker photographed — mains type is an engineering node column, not a core
  attribute; not reachable from the surfaces I could open. I measured the API the picker is built from.

## Test data
All reads. Extract dialog opened for the screenshot and cancelled — no extraction run, nothing written.
