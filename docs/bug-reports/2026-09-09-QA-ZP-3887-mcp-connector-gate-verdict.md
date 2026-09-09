# ZP-3887 — MCP connector per-company entitlement · QA verdict: **NOT VERIFIABLE FROM THE WEB SURFACE**

**Ticket:** [ZP-3887](https://egalvanic.atlassian.net/browse/ZP-3887) — "[Web] MCP connector was
all-or-nothing: any tenant with Cognito coordinates could connect, with no way to enable or revoke it
per customer" (Ready for QA) · fix: eg-pz-backend **#1090** (+ #1091 stag, #1092 qa, #1093 dev)
**Artifact:** https://claude.ai/code/artifact/9dcbfac9-b2ea-44f3-8ef9-fe710c20e5fe
**Attempted:** 2026-09-09 · **Env:** `acme.qa.egalvanic.ai` / `demo.qa.egalvanic.ai` · V1.36

---

## Verdict

The route this ticket gates — `GET /auth/oauth-config/<company_code>` — **is not reachable from the
customer-facing web host**, so seven of the eight QA-review items cannot be exercised from where QA
sits. Nothing here contradicts the fix; there is simply nothing to test on this surface.

## What was tried

Unauthenticated GETs (correct for a pre-sign-in route) against every plausible path, on two tenant
hosts:

```
/api/auth/oauth-config/<code>      → 200 text/html   (SPA catch-all)
/auth/oauth-config/<code>          → 200 text/html
/api/oauth-config/<code>           → 200 text/html
/api/auth/oauth-config?company_code=…→ 200 text/html
/api/mcp/auth/oauth-config/<code>  → 200 text/html
/api/auth/cognito-config/<code>    → 200 text/html
```

Company codes tried: `acme`, `demo`, `eee`, `kochinc`, `bces-iq`, plus a nonexistent one. All returned
the frontend's catch-all HTML rather than JSON. The control case proves the probe method is sound:
`GET /api/auth/me` on the same host returns proper JSON (`401 {"error":"No authorization provided"}`),
so `/api/auth/*` does reach the backend — this particular route is not exposed here.

**One weak positive worth stating:** from the public web host, no request I could construct returned
Cognito pool coordinates for any tenant. That is consistent with the gate, but it does not verify it —
the route is absent from this surface either way.

## Per-item status

| # | QA-review item | Status |
|---|---|---|
| 1 | Gate marker present on dev/qa/stag/prod; no migrations | ❌ needs backend repo access (the GitHub MCP failed to connect this session) |
| 2 | Default-deny: untargeted company → `404 MCP_NOT_ENABLED` | ❌ route unreachable from the web host |
| 3 | Grant cycle: target on LD flag `feature-mcp` → `200` with pool ids | ❌ blocked twice — route unreachable **and** no LaunchDarkly access (the LD connector in this session exposes only `authenticate`, so flag targeting cannot be changed) |
| 4 | Revoke cycle → back to `404 MCP_NOT_ENABLED` | ❌ same |
| 5 | Live-session expiry within one access-token lifetime | ❌ needs a connected MCP session plus the grant/revoke cycle |
| 6 | Unknown subdomain → `404 COMPANY_NOT_FOUND`, distinguishable | ❌ route unreachable |
| 7 | No `client_secret` in the 200 body | ❌ requires a 200 from a granted company |
| 8 | `tests/test_company_scope.py` sweep not regressed, view in `_EXEMPT_VIEWS` with a reason | ❌ needs backend repo access |

## What would unblock this

1. **The base URL the MCP server actually calls** for `/auth/oauth-config/<company_code>` (the backend
   host or API gateway, not the tenant web host). With that, items 2, 6 and 7 are a few minutes of
   probing.
2. **LaunchDarkly access** to target and untarget `feature-mcp` on the QA environment — items 3, 4
   and 5. QA should not be flipping a customer entitlement flag without that being an explicit,
   sanctioned action anyway; worth confirming who owns that toggle for QA runs.
3. **Read access to eg-pz-backend** for items 1 and 8 (the `MCP_NOT_ENABLED` marker on four branches
   and the `_EXEMPT_VIEWS` entry).

Given 1 and 2, this ticket is testable in well under an hour; the gate's placement (one call before
every sign-in and refresh) makes the revoke behaviour easy to demonstrate.

**Footprint:** read-only. A handful of unauthenticated GETs; nothing created, changed or granted.
