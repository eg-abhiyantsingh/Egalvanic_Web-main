# engineering_status four-state (#1057/#1245) — QA PASS + artifact

Live on QA (ticket claimed dev-only; verified live). Four-state enum persists end to end, checkbox
replaced by 4-value selector (25/31 disabled + tooltip on undesignated), per-status breakdown + approved
card correct, dual-column mirror consistent. SKM export stamps Data State on every component type incl.
Protection 327/327 (the class that dropped it); a breaker set Verified exports as Data State="Verified".
Negatives pass (no-eqp_lib→Incomplete, status-without-designation→400, legacy boolean lifted to Complete).
Not covered: French (locale unforceable from browser), SKM re-import round trip, backfill, migration step.
Artifact header = real ticket title.
