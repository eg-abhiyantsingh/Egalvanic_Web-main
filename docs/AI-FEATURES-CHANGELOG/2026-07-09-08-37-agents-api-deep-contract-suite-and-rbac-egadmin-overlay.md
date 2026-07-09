# Agents-API deep contract suite (agent-token boundary) + RBAC EG-Admin overlay tolerance

**Date:** 2026-07-09
**Time:** 08:37 UTC
**Author:** Claude Code (automation, Fable 5), for abhiyant.singh@egalvanic.com

## What was asked
> "hi have you check everything" → then, pointing at `https://acme.qa.egalvanic.ai/api/docs`,
> "this all are the endpoint did we cover all this" → then "yes" to:
> *fix the RBAC test to survive the EG-Admin contamination, and stand up a deep contract suite for `agents`.*

## Coverage answer that motivated the work
The live spec behind `/api/docs` (`/api/swagger.json`, Swagger 2.0) is **944 paths / 1,077 operations /
138 resource families** — grown from the 862/973 the suite was written against. Coverage split:
- **Breadth (health probe): ~100%** — `ApiFullCatalogHealthApiTest` fetches the spec at run time and
  touches every op (but report-mode, never gates).
- **Depth (asserting contracts): ~28%** — only 38 of 138 families had real assertions. The **largest
  untested-in-depth family was `agents` (105 ops)**. That is the gap this change closes.

## The agent-token privilege boundary (the AI-feature-specific finding)
The `agents` family is the AI Quote/Plan agent's API. Probing it live revealed a dedicated auth model that
was previously untested: the entire `/agents/quoteagent/*` + `/agents/planagent/*` sub-API is gated by an
**agent token**, NOT the ordinary user JWT.
- GET with a valid **user** bearer → `401 {"error":"Invalid or expired agent token"}`
- Unauthenticated mutation → `401 {"error":"Agent token required"}`

This is a real privilege boundary between user-land and agent-land: if a refactor ever let the user JWT
satisfy the agent guard, ~91 write-capable agent endpoints would be exposed to every logged-in user. Nothing
was asserting it stays closed — now `AgentsContractApiTest` does, endpoint by endpoint.

## What was built

**`AgentsContractApiTest`** (spec-driven, hard-failing):
1. `testAgentServiceHealthContract` — `GET /agent/health` returns 200 JSON with `success` +
   `sdk_installed` + `agent_service_reachable` all true and a semver `sdk_version` (proves the downstream
   FastAPI agent service at `agent.egpz.qa:8899` and the Claude Agent SDK are both live).
2. `testQuoteAgentReadRequiresAgentToken` (42 ops) — each `/agents/*` GET rejects a user token 401/403.
3. `testQuoteAgentMutationRequiresAgentToken` (49 ops) — each `/agents/*` mutation rejects an
   unauthenticated call 401/403; a 2xx-with-JSON would be an unauthenticated write (hard fail).
4. `testAgentsCatalogCoverage` — all 105 agents-family ops are contract-asserted or in the explicitly
   excluded `/agent/*` runtime bucket (chat/create/clear/upload/simple/… — LLM/sandbox side-effecting, so
   deliberately not probed; logged, never silently dropped).

## Depth explanation (for learning + manager review)

**Why skip the RBAC contaminated roles instead of "fixing" the accounts?** The five QA role accounts were
granted the internal `EG Admin` super-admin role on 2026-07-09 ~07:22 UTC, so `/auth/me` returns
`is_admin=true` + all 115 permissions — every "denied" cell falsely trips. Removing a role from shared QA
accounts is a state-changing action on data I don't own, and the accounts might be that way intentionally, so
the test-side fix is correct: detect the overlay and SKIP loudly. Crucially the guard is **surgical** — it
fires only when an *explicitly-assigned* `EG Admin` role appears in `roles[]`. A genuine backend escalation
(perms inflated with no assigned overlay) still hard-fails, so the security guarantee is intact. This is why
I extended `LiveAuth` to read the whole `roles[]` array: the overlay sits at `roles[1]` while the intended
role is at `roles[0]`, so reading only `roles[0]` (as before) made the contamination invisible.

**Why the three action-enforcement RBAC classes needed no change.** They derive their expectation from the
live `/auth/me` set ("does enforcement match what the token declares?"), not from the static prod matrix, so
they remain self-consistent and green under contamination. Only the two *matrix-oracle* tests break on it.
This is a clean principled split: matrix-oracle → skip on overlay; live-oracle → unaffected.

**A subtle correctness bug I hit and fixed in the agents suite.** Substituting a UUID into the *integer*-typed
`{*_oid}` SKM path params made the route converter reject the path → the request fell through to the SPA
catch-all → `200 HTML`, which my first cut mis-read as an auth bypass. Fix: numeric probes for `{*_oid}`, and
the classifier now treats **any HTML body as "route not exercised" (skip), never a breach** — because a real
bypass returns agent JSON, never the SPA shell. Lesson: when probing typed path routes, the probe value must
match the converter or you test the frontend, not the API.

## Validation (live)
- `AgentsContractApiTest`: **93 run, 0 fail, 0 skip** (1 health + 42 GET + 49 mutation + 1 coverage).
- RBAC full push-suite (`suite-api-health` RBAC set): **606 run, 0 fail, 570 skipped** (was 196 fail).

## Files
- `src/test/java/com/egalvanic/qa/testcase/api/AgentsContractApiTest.java` (new)
- `src/test/java/com/egalvanic/qa/testcase/api/RbacFixtures.java`
- `src/test/java/com/egalvanic/qa/testcase/api/RoleBasedPermissionContractTest.java`
- `src/test/java/com/egalvanic/qa/testcase/api/RolePermissionMatrixCellTest.java`
- `suite-api-health.xml`
