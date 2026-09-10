# `/sld/{id}/library-designations` ignores accessible_sld_ids — site-scope leak

**Date:** 2026-09-10
**Prompt:** *"whenever you are testing any testing make sure to test api too. like not other company data is showing etc"*
**Env:** `acme.qa.egalvanic.ai` · V1.36 · tenant acme · read-only (GET only)

## What the new rule turned up immediately

Applying the owner's directive to the endpoint behind ZP-3904's designation schedules found a real
in-tenant horizontal scope leak on the first test.

`GET /api/sld/{sld_id}/library-designations?kind=…` filters by the id in the path and **never checks
whether that diagram is in the caller's `accessible_sld_ids`**:

| seat | mapped sites | target mapped? | HTTP | rows |
|---|---|---|---|---|
| Project Manager | 114 | yes | 200 | 10 |
| **Facility Manager** | **11** | **NO** | **200** | **10** |
| **Electrical Engineer** | **4** | **NO** | **200** | **10** |

FM and EE row ids are **byte-identical to the PM's**. Applies to every scope: sccr 10, feeder 4, ocpd 4,
transformer 2, all 10.

**Why it is a defect:** the platform enforces this exact scope elsewhere on the same seats in the same
session — `/reporting/sample-entities` refuses FM and EE with 422 `permission_denied`, and
`/v2/issues/list` with a foreign `company_id` refuses with 422 *"You do not have access to this
company's data."*

**Pattern:** same family as the recorded `/ir_session/{id}/full` finding — list endpoints derive scope
from the token, by-id endpoints trust the id in the path. Recommend auditing the whole by-id family.

## Other API scope results from the same sweep (all good)

- `POST /v2/issues/list` + foreign `company_id` → **422 refused** ✅
- `GET /reporting/sample-entities` — `company_id` / `company` / `subdomain` / `sld_id` /
  `mapping_user_sld` all ignored; id set identical to baseline ✅
- `GET /reporting/sample-entities` as FM / EE → **422** `required_permission: reports.view` ✅
- `/staff/*` (ZP-3874) → **401** `eg_staff_denied` for every caller ✅

## Lesser observations on the leaking endpoint

- An unknown SLD id returns **200 with an empty list**, not a 404 — "no such diagram" is
  indistinguishable from "an empty diagram". A malformed id falls through to the SPA's HTML.
- An unknown `kind` is **not rejected** — `?kind=banana` silently returns the `all` scope.

## Deliberately not tested

The sibling PATCH endpoints (`…/engineering-status`, `…/available-fault-current`) take the same path
parameter and likely share the missing check. I did **not** write to an unmapped site — that would
modify real engineering data on a site the seat should not touch. Flagged as the first thing the fix
should verify.

## Deliverables

- Verdict — `docs/bug-reports/2026-09-10-QA-library-designations-site-scope-leak.md`
- Artifact — https://claude.ai/code/artifact/06ad5f78-f47f-4875-bb22-588cc34ce68f
- Evidence — `docs/bug-evidence/library-designations-site-scope/scope-leak-responses.json`

## Footprint

Read-only. Only acme QA was contacted; no second tenant, so **cross-tenant behaviour is untested and
unclaimed**.
