# Staff API refused nine different ways with one message, so a misconfigured server looked like an unauthorized account

**QA verdict — eg-pz-backend #1096 / ZP-3890: PASS (partial coverage)**
**Tested:** 2026-09-02 · acme.qa.egalvanic.ai V1.36 · live browser (admin) + JWT-shaped API probes.
**Artifact:** https://claude.ai/code/artifact/4f4ea67c-834a-4893-9e43-b210ee93ad58

## Confirmed on QA — the old single message is GONE; refusals are now distinct (all 401 eg_staff_denied)
| Cause | Message returned |
|---|---|
| No / empty Authorization | "Staff authentication required. Send the Cognito ID token as a bearer token." |
| Malformed bearer (not 3-seg JWT) | "That does not look like a sign-in token." |
| Bad signature / alg:none / wrong issuer | "Your token was not issued by the Egalvanic staff directory. Sign in through the staff console." |
| Both entry points | require_eg_staff route === /staff/companies — identical wording (shared authenticate_staff) |

Security in passing: alg:none unsigned token REJECTED (no bypass); a signature-failing token never advances
to the deeper branches (fails closed, correct ordering).

## NOT reachable from QA (structural, not omission) — need a genuine Cognito-signed staff token
- EG_STAFF_CLIENT_IDS-unset message, expired-sign-in, wrong-token-type, not-on-allowlist(names address):
  all sit BEHIND signature verification. App auth = httpOnly cookie (invisible to JS); no route echoes a raw
  token (/auth/verify-token, /auth/session both return SPA shell). A self-signed token always stops at the
  signature check. Verify these against a DEV backend with env vars toggled + a real staff token minted.
- Positive control (allowlisted account + valid ID token succeeds on every /staff/* route, logs unchanged):
  needs a real allowlisted staff seat; QA's staff surface is inert (allowlist unset).
- Internal MCP client "Refused: <reason>" passthrough: separate client, not in this web repo.

## Test data
Zero mutations — every probe was a refused read.
