# Engineering sign-off was a yes/no checkbox and SKM Data State never exported — four-state engineering_status end to end

**QA verdict — eg-pz-backend #1057 / eg-pz-frontend #1245: PASS (live on QA)**
**Tested:** 2026-09-02 · acme.qa.egalvanic.ai V1.36 · live browser + SKM XML inspection.
**Artifact:** https://claude.ai/code/artifact/ba905190-2d33-4cb8-82bb-ead626d3b13d

Ticket said "dev only, needs promotion" — WRONG, it is live on QA (PATCH .../engineering-status is
swagger-registered + functional; rows carry engineering_status). Tested QA directly per standing rule.

## Verified end to end (SLD Android Site e8743699, breaker "A1" 8b13b32e as the probe)
- Four values persist: Incomplete/Estimated/Complete/Verified each PATCHed → reloaded → read back identical.
- Approved mirror exact: Incomplete/Estimated→false, Complete/Verified→true.
- Dual columns consistent (engineering_status + eqp_engineering_approved), 100/100 rows.
- stats.by_status carries all four literals; stats.approved derives server-side.
- UI: checkbox GONE, Status column = 4-value selector; 25/31 disabled (no designation) w/ tooltip
  "Set a library designation first"; header breakdown Incomplete 327 / Estimated 0 / Complete 0 / Verified 1;
  "Engineering Approved" card = Complete+Verified.
- SKM export: Data State (field 65556) written on EVERY component type incl. Protection 327/327
  (breakers/fuses/relays — previously dropped silently), Bus 244, Xformer2 4, AutoSwitch 65, etc.
- End-to-end: breaker set Verified → exported <Field name="Data State" value="Verified">; dist 1 Verified/326 Incomplete.
- Negative: no eqp_lib still exports Incomplete (244 Buses, not omitted).
- Negative: status>Incomplete on undesignated asset → 400 error_needs_designation (names the asset), stays Incomplete.
- Backward compat: legacy eqp_engineering_approved:true via PUT /node/update → lifted to engineering_status Complete,
  both columns agree. (The bulk PATCH endpoint is enum-only and rejects a bare boolean — correct for that endpoint;
  the lift lives on the node write path.)

## Not covered
- French strings: couldn't force locale from browser (app reads language from user setting) — UNVERIFIED, not pass/fail.
- SKM re-import round trip (completion-state.xml, CBL-0050 Complete): confirmed export survives; did not re-import.
- Backfill (pre-deploy approved→Complete): QA all-Incomplete, no pre-approved asset to observe.
- Migration deploy single-head: backend-deploy observation, not a web check.

## Test data
One asset cycled through the four values, reset to Incomplete; SLD verified back to 328 Incomplete/0 approved. Exports read-only.
