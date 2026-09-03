# 2026-09-03 — QA verdict: SKM export sign-off downgrade + Equipment Designations Bulk Ops/filters (backend #1095, frontend #1268)

**Prompt:** test the "[Web] SKM export sent Complete sign-offs on components PTW silently
mangles, and Equipment Designations could not filter or set status in bulk" ticket end to end,
with step screenshots highlighting each click, and publish an artifact.

**Artifact (per-ticket deliverable):** https://claude.ai/code/artifact/5ff718d8-65c4-4648-b938-a0b646b35001

## Environment finding first

The ticket says "dev only — neither PR is on cicd/qa". **Stale.** Both PRs are live on QA
V1.36 (`skm_export_fixups` in the API, `Allow: PATCH` on the bulk endpoint, overlay/Bulk
Ops/Status-rename in the shipped bundle). Tested everything on QA per standing practice.

## Verdict: 11 PASS / 6 bugs / 2 not testable on QA

All three verdicts (`ground_fault_dropped`, `segment_revert`, `cable_3awg_redbook`) downgrade
the exported Data State regardless of sign-off; negative control holds (unaffected Complete
exports Complete — proven via a 509-component histogram); filter tray (5 filters, server-side,
options never shrink), Select-all-N + one-request bulk PATCH, all-or-nothing refusals with
named assets, Status overlay (4 states + neutral + legend), gauges/panel-names/Node-Bus fixes
all verified.

### Bugs (full repro in the artifact)

1. **HIGH, FE:** the export's "N assets exported as Incomplete" warning (warnings[0]) is never
   shown in the web UI — only the unconfigured-classes panel renders. The ticket's headline
   deliverable is invisible to the exporting user.
2. **MEDIUM, BE:** `segment_revert` never appears in `/library-designations`
   `skm_export_fixups` (export warning + SLD issue both fire for the same node state) — the
   grid asterisk is blind to trip-settings reverts.
3. **MEDIUM, FE:** `/equipment-designations` has no nav entry in any of the 8 categories for a
   Super Admin holding the permission; all four sibling routes with identical gating show.
4. **LOW, FE:** bulk Edit dialog's "have no library designation" count is page-local ("25" when
   the 121-selection truly holds 117). Server-side check is correct and names each asset.
5. **LOW, FE:** post-export warning panel covers the reopened Export menu and swallows clicks
   on "Export Engineering XML"; its class list is clipped.
6. **LOW:** "Library Designated" header counts SKM-linked rows (2 of 5) while the grid's
   Designated column shows 1, on the same screen.

### Not testable on QA

- Foreign-tenant PATCH → 404 (single-tenant policy; unknown-id + >2000-cap negatives covered
  in-tenant). Note: the QA edge rewrites backend API 404s to 200+index.html — masks the 404
  contract entirely; worth an infra ticket.
- Status mode hidden for non-eng-lib companies / iOS viewer (only tenant is eng-lib; iOS out
  of web scope). Gating verified in the shipped bundle (menu item behind the eng-lib flag).

## How the tricky verdicts were crafted

- **3 AWG Red Book:** flipped CBL-REDBOOK-3836 (ZP-3836 fixture) to 3 AWG in the asset editor →
  fixup computed immediately → Complete → export → Data State Incomplete + warning naming 1
  asset. Fixture deliberately left in this state as a living repro.
- **segment_revert:** on Richmond CB 451 (ABB Emax 2 Ekip DIP), deselected both `init_on`
  segments (ids from `/api/skm-library/devices/89413/detail`) in the trip-settings picker.
  Learned the trigger by reading the deployed issuesCalculator: `skm_import_gap.kind`.
  Dial-only selection exports `Settings=";"` with NO verdict anywhere (edge case worth a look);
  settings-segments-without-init_on fires the export warning + SLD issue but not the grid fixup
  (bug 2). CB 451 fully restored afterwards.

## Data hygiene

Richmond, CA restored exactly (132 Incomplete / 1 Complete / approved 1; CB 451 segments
byte-verified). Addtioanl Site fixture intentionally keeps the 3 AWG + Complete cable.
