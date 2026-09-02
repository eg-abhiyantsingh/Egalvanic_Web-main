# PM remove-service (#1089/#1267) — QA PASS + artifact

Live-browser test on QA (ticket claimed dev-only; verified live per standing rule). Both removal
affordances (rail-wide Remove, matrix row trash) behave to spec with server-driven dry-run counts;
cancel writes nothing; PM Plans moved under Builder. Negatives: malformed→400, tenant-isolation→
removed:0, global UI hides controls. ONE discrepancy: global-standard endpoint returns 200+null not
the 404 the ticket specifies (no user harm, UI never exposes it; author confirm). Artifact published
per the new every-ticket-gets-an-artifact rule.
