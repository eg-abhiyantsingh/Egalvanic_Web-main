# Staff refusal messages (#1096 / ZP-3890) — QA PASS (partial) + artifact

Live on QA (ticket noted qa+stag+prod+dev). The generic "Not an authorized Egalvanic staff account"
is gone; four distinct refusal messages confirmed (no-token, malformed, bad-signature/issuer, both
entry points agree). alg:none rejected — no bypass; ordering fails closed. Four deeper branches
(config-unset, expired, wrong-type, allowlist-address) sit behind signature verification and need a
real Cognito-signed staff token QA can't mint — flagged as a backend/integration test, not omission.
Artifact published with the real ticket title as header.
