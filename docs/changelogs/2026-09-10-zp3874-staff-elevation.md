# ZP-3874 — audited staff elevation (`/staff/*`)

**Date:** 2026-09-10
**Prompt:** ZP-3874 ticket text + "done testing this ticket"
**Env:** `acme.qa.egalvanic.ai` · V1.36 · read-only

## Verdict: the refusal half passes; 10 of 14 steps need a dev seat

The ticket's own review splits by environment — refusal cases on qa/stag/prod, positive and
tenant-scoping cases on dev. **QA can only exercise the refusal half.**

| Step | Result |
|---|---|
| 1 · no regression to customer auth | **PASS** — login 200, own company only (total 926), foreign `company_id` → 422 |
| 2 · unset config refuses everyone (QA) | **PASS** — every `/staff/*` route 401 `eg_staff_denied` |
| 7 · token-shape negatives | **PARTIAL PASS** — a customer-pool ID token (verified email, wrong app client) is refused |
| 5 · act-as removed | **INCONCLUSIVE** — 401 comes from the token check, which runs first |
| 11, 12 · dataprep body-override + mode allowlist | **BLOCKED** at the token gate — 11 is the ticket's own "important one" |
| 3, 4, 6, 9, 10, 13 | **NEED DEV** + an internal-tools Cognito ID token |
| 8 · audit record | **NO LOG ACCESS** |
| 14 · sample-entities parity | needs staff + customer caller on **one** environment |
| 2b · refusal on stag/prod | **NOT RUN** — needs explicit instruction |

## Confirmed the ticket's premise empirically

Decoded both customer-pool tokens: `access_token` has **no** email claim (`token_use=access`);
`id_token` has `email` + `email_verified=true`. So "access tokens carry no email claim and email is what
membership is matched on" holds. Three distinct refusal messages exist for malformed / wrong-directory /
missing token — good failure design.

## Discrepancy: the allowlist appears to be SET on production

The ticket says the feature "stays inert outside dev" with `EG_STAFF_ALLOWLIST` unset, and asks for the
**refusal** case on prod. Observed otherwise: the connected staff MCP server — which reaches tenants only
through these `/staff/*` routes — returns **production** data (`list_companies` → 36 real customer
tenants; `acme` there is `0a61e613…` with branding in `eg-pz-prod-s3-branding-ohio`, vs QA's
`d59d449b…`). So staff routes are serving data on prod and **the prod refusal step will not reproduce as
written**. All calls through it were read-only; no negative case was run against prod.

Silver lining: if the routes are live on prod, its audit log already holds real entries, so step 8 can be
verified there.

## Deliverables

- Verdict — `docs/bug-reports/2026-09-10-QA-ZP-3874-staff-elevation-verdict.md`
- Artifact — https://claude.ai/code/artifact/6fcadb40-931a-438e-b5db-ae2936658639

## Footprint

Read-only. No user, company, config, form or template created, modified or deleted.
