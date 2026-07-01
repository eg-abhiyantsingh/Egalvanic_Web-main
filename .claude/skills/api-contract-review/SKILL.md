---
name: api-contract-review
description: >-
  Review an API endpoint against eGalvanic's non-functional contract BEFORE opening a PR. Every list
  endpoint must be paginated (page/per_page), support filters/search, cap at a max page size (≤50/100),
  default to a small page size (10–20), return the expected records, and respond within a few seconds —
  plus baseline security (auth required, input validation, no data leakage, no IDOR) and performance
  (latency, payload size). Use this when adding, editing, or reviewing any API (list / filter / search /
  create / update / delete) and you want to self-check before generating a pull request. Trigger on
  phrases like "check my API", "API review before PR", "is this endpoint paginated", "API contract",
  "pagination/limit/filter check", "API security/performance check".
---

# API Contract Review (pre-PR)

Self-check an API endpoint against the eGalvanic non-functional contract **before you open a PR**, so the
"is it paginated / filtered / bounded / fast / secure" questions are answered by the developer, not caught
later by QA or in production.

Use it whenever you add or change an endpoint. It works two ways — **static** (review the code in your
branch; no server needed) and **live** (probe a running dev server). Do the static review always; add the
live probe when you have a dev server + token.

## The contract (what "good" looks like)

**Every LIST / collection endpoint must:**
1. **Return the expected records** — correct 2xx, JSON, the right shape (prefer a `{success, data}` /
   `{data, total, page, per_page}` envelope over a bare array).
2. **Be paginated** — accept `page` + `per_page` (or `limit`/`offset`) and actually honor them.
3. **Have a default page size** — bounded and small (**ideal 10–20**); never return the whole collection by default.
4. **Enforce a max page size** — clamp/reject requests above the cap (**≤ 50–100**); `per_page=100000` must not return everything.
5. **Support filters / search** — the documented query params work and don't 5xx on unknown/garbage values.
6. **Respond within a few seconds** — target < 2s; investigate > 3s; a health/list call should never take > 8s.

**Every endpoint (list + create/update/delete) must:**
7. **Require auth** — no token ⇒ 401/403, never 200 with data.
8. **Validate input** — bad params ⇒ 400 with a helpful message, never a 500.
9. **Not leak data / allow IDOR** — you can't read/edit another tenant's or user's record by guessing an id; no secrets/PII in responses.
10. **Bound payload + write safely** — no unbounded payloads; writes are transactional/idempotent where expected.

Full thresholds, rationale, and per-check detail are in **reference.md**. A copy-paste PR checklist is in
**pr-checklist.md**.

## How to run

### 1) Scope the endpoints (always)
Find what changed on this branch:
```
git diff --name-only origin/main...HEAD
git diff origin/main...HEAD
```
Identify the route/controller/serializer/repository code for each **added or changed** endpoint. If the
diff isn't obvious, ask the developer which endpoint(s) to review.

### 2) Static review (always — no server needed)
For each endpoint, read its handler and check every applicable contract item above. For **list** endpoints
specifically, confirm in the code:
- pagination params are read AND applied to the query (not accepted-but-ignored),
- `per_page` is clamped to a max (e.g. `min(per_page, 100)`),
- a default page size is set when the param is absent,
- filter/search params map to query conditions,
- auth middleware/guard is present,
- input is validated before use.

Report each as **PASS / WARN / FAIL** with the exact file:line and a concrete fix.

### 3) Live probe (optional — when a dev server + token are available)
Zero-dependency (stdlib only). Probes pagination, max-limit, default size, filter, response time, and
baseline auth:
```
python3 .claude/skills/api-contract-review/probe_endpoint.py \
  --base https://acme.dev.egalvanic.ai/api \
  --path /node_classes \
  --token "$DEV_TOKEN" \
  --insecure          # only for self-signed dev/QA certs; omit vs a properly-issued cert
```
It prints a per-check pass/warn/fail table and **exits non-zero if any check FAILs** — so it also works as
a pre-commit / CI gate. Feed its findings into the checklist. (Get `$DEV_TOKEN` from your own login;
never hardcode or commit it.)

### 4) Output — a PR-ready checklist
Fill in **pr-checklist.md** and paste it into the PR description. Classify:
- **FAIL (block the PR):** unbounded list (no pagination / no max limit), missing auth, 500 on bad input,
  IDOR / cross-tenant access, secrets in the response.
- **WARN (fix soon / note in PR):** default page size > 20, response 2–8s, large payload, bare-array shape,
  missing a documented filter.
- **PASS:** meets the contract.

## Guardrails
- **Read-only by default.** For live probes, only issue GETs (and safe reads); never run create/update/delete
  probes against a shared environment without explicit confirmation, and never against production.
- **Never commit tokens.** Pass the token via `--token` / an env var; it must never be written to a file
  that gets committed.
- Static review needs no credentials — prefer it, and it's the part that works in every repo/CI.

See `reference.md` for the full spec and `pr-checklist.md` for the template.
