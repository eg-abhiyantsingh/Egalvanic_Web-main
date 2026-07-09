# Full-catalog classifier: stop over-reporting parameterized SPA-catch-all as CRITICAL

**Date:** 2026-07-09 14:53 UTC
**Author:** QA automation (Claude Code, Fable 5)
**Branch:** main

## What was asked
Review a batch of "critical" findings from the catalog report — 6× `GET /account/{account_id}...`
"Returns 200 with HTML body (SPA fallback)", `POST /agent/clear` "auth gate missing", `GET /agent/health`
"5xx (503)" — "is this correct?"

## Live verification (each finding, curl)
- **`/account/{account_id}` + 4 sub-paths + `/api`** → with a **REAL** account id all return **200 JSON**
  (`/account/{id}`, `/details`, `/allowed-domains`, `/subscriptions`, `/internal-user-candidates` — all
  JSON). They return **200 HTML only when probed with a random/nonexistent id** — the backend's SPA
  catch-all serves the app shell instead of a JSON 404. So these are **FALSE CRITICALS**: the endpoints
  work; the catalog probe substitutes a random UUID into `{account_id}`, no record matches, SPA answers.
- **`POST /agent/clear` unauth** → live returns **415 Unsupported Media Type** (no data), not a 200-JSON
  bypass. **Not currently reproducing** as an auth-gate gap.
- **`GET /agent/health` 503** → live returns **200, all healthy** (`agent_service_reachable:true`, sdk
  0.2.114). The 503 was a **transient** blip while the downstream FastAPI agent service was momentarily
  unreachable — the health check working as intended, not a persistent defect.

## Root cause + fix (`ApiFullCatalogHealthApiTest`)
The classifier flagged **every** `200 + HTML` GET as `critical`, regardless of whether the path was
probed with a synthetic random id. A parameterized path hitting the SPA catch-all for a nonexistent id
is expected (real ids return JSON), so it must not be a per-endpoint critical. Fix distinguishes:
- **Parameterized** GET/mutation returning 200+HTML → `info` ("SPA shell for a nonexistent id; expected
  catch-all; a real id returns JSON — backend could return a JSON 404"). Status stays `pass`.
- **Fixed (unparameterized)** API path returning 200+HTML → still `critical` ("SPA fallback shadows a
  registered JSON route") — a genuine routing bug.

## Validation (live full-catalog run, 1078 ops, BUILD SUCCESS)
- `GET /account/{account_id}` etc. now classify **pass** (was warn/critical).
- **127** parameterized paths reclassified from false-critical → `info`.
- **2** critical-HTML findings remain, both legitimate FIXED paths: `GET /api` (root) and
  `GET /equipment-library/datstyle/resolve` — declared JSON ops that actually serve the SPA shell
  (real, low-severity routing findings worth a backend look).

## Net effect
The catalog report's critical count drops from a flood of ~130 SPA-fallback noise to the handful of
genuine issues, matching the standing "verify before labelling CRITICAL / don't over-report" rule.

## Files
- `src/test/java/com/egalvanic/qa/testcase/api/ApiFullCatalogHealthApiTest.java`
