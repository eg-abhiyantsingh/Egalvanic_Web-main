# ZP-3874 — audited staff elevation (`/staff/*`) · QA verdict: **default-deny PASSES; 10 of 14 steps need dev**

**Ticket:** [ZP-3874](https://egalvanic.atlassian.net/browse/ZP-3874) — *"[Web] Staff reached customer
tenants through 225 separate product logins - no audited, scoped path existed"* · status **Ready for QA**
**Artifact:** https://claude.ai/code/artifact/6fcadb40-931a-438e-b5db-ae2936658639
**Tested:** 2026-09-10 · **Env:** `acme.qa.egalvanic.ai` · V1.36 · tenant acme · **read-only**
**Backend:** #1064, #1065, #1066, promoted by #1080 (prod) / #1081 (qa) / #1082 (stag)

The ticket's own QA Review splits the work by environment: *"Run the allowlist-unset refusal case on qa,
stag and prod; run the positive and tenant-scoping cases on dev, where the allowlist is configured."*
**QA can only ever exercise the refusal half**, and that half passes.

## Verdict per review step

| # | Step | Verdict |
|---|---|---|
| 1 | no regression — customer auth untouched, own company only | ✅ **PASS** |
| 2 | allowlist unset ⇒ refuse everyone (on QA) | ✅ **PASS** |
| 2b | same on stag and prod | ⛔ not run — needs explicit instruction |
| 3 | positive: `/staff/companies` ⇒ tenant directory | ⛔ needs dev + internal-tools Cognito |
| 4 | `/staff/reporting/configs` + act-as ⇒ that tenant only | ⛔ needs dev |
| 5 | act-as header removed ⇒ refused | ⚠️ **inconclusive** — see below |
| 6 | tenant-B config id while acting as A ⇒ 403 `wrong_tenant` | ⛔ needs dev |
| 7 | access token vs ID token; wrong app client; non-allowlisted / unverified email | ✅ **PARTIAL PASS** |
| 8 | audit record written, even on a failing handler | ⛔ no log access |
| 9 | `/staff/eg-forms` ⇒ tenant forms + globals + lock reason | ⛔ needs dev |
| 10 | `/staff/dataprep` in the three allowed modes | ⛔ needs dev |
| 11 | **act-as A + body `company_id` B ⇒ scoped to A** | ⛔ blocked at the token gate |
| 12 | non-allowlisted mode ⇒ refused, never proxied | ⛔ blocked at the token gate |
| 13 | page-templates / `eg-forms/{id}` + wrong-tenant id | ⛔ needs dev |
| 14 | `sample-entities` parity, staff vs customer caller | ⚠️ needs both callers on ONE env |

## Step 1 — no regression (the ticket calls this the main risk surface) · PASS

```
POST /api/auth/login  (product seat)        -> 200
GET  /api/auth/me                           -> 200, company_id d59d449b…, 114 sites, 95 perms
POST /api/v2/issues/list  own company_id    -> 200, total 926
POST /api/v2/issues/list  foreign company_id-> 422 permission_denied
                                               "You do not have access to this company's data."
```

Customer-facing auth works, still returns only its own company's data, and refuses another company's.
Nothing about the staff blueprint bled into it.

## Step 2 — unset config refuses everyone · PASS

Every `/staff/*` route is registered on QA and refuses all callers:

| call | result |
|---|---|
| `GET /staff/companies`, no token | **401** — *"Staff authentication required. Send the Cognito ID token as a bearer token."* |
| `GET /staff/companies`, customer product token | **401** — *"Your token was not issued by the Egalvanic staff directory. Sign in through the staff console."* |
| `GET /staff/companies`, malformed bearer | **401** — *"That does not look like a sign-in token."* |
| `/staff/reporting/configs`, `/staff/eg-forms`, `/staff/reporting/page-templates`, `/staff/reporting/sample-entities`, `POST /staff/dataprep` | all **401** `eg_staff_denied` |
| a bogus `/staff/…` path | **200 SPA HTML** — which is how a registered route is distinguished from a fall-through |

Three *distinct* messages for malformed / wrong-directory / missing is good failure design — support can
tell them apart.

## Step 7 — token-shape negatives · PARTIAL PASS

**The ticket's stated premise is empirically correct.** Decoding both tokens the customer pool issues:

```
access_token : token_use=access  email claim ABSENT   client_id=3ksgppes5skvdrqhn8brocctdp
id_token     : token_use=id      email claim PRESENT  aud=3ksgppes5skvdrqhn8brocctdp
               email_verified=true   iss=…/us-east-2_ibkN2rpaj  (customer pool)
```

So "Cognito access tokens carry no email claim and email is what membership is matched on" holds.

**Covered:** a **customer-pool ID token** — a genuine Cognito ID token, with an email claim and
`email_verified: true`, but minted for a **different app client** — is **refused**. That is the ticket's
*"a token minted for a different app client must be refused"* case, and it passes. The access token is
refused too.

**Not covered:** an internal-tools-pool token whose email is **not on the allowlist**, and one with an
**unverified** email. Both need a token from that pool, which I do not have.

## Step 5 — why "act-as removed" is inconclusive rather than a pass

With the header removed, the request returns **401 `eg_staff_denied`** — but so does the request *with*
the header. The refusal comes from the **token check, which runs first**, so the act-as requirement is
never reached. **The observed 401 is not evidence that the act-as check works.** Same reasoning voids
steps 11 and 12 on QA: the dataprep body-override and mode-allowlist tests both die at the token gate,
and step 11 is the one the ticket itself calls *"the important one"*.

## Step 14 — the parity check needs both callers on one environment

I compared the staff `sample-entities` output against a customer-facing call and initially read the
difference as per-caller scope. **That was wrong and is retracted** — the two calls hit different
environments. A real parity test needs the staff caller and the customer caller on the *same*
environment. See `2026-09-09-QA-ZP-3863-sample-entities-verdict.md`.

## ⚠️ Discrepancy the ticket author should confirm: the allowlist appears to be SET on prod

The ticket states the feature *"stays inert outside dev: `EG_STAFF_ALLOWLIST` is unset there…* so every
`/staff/*` route registers and refuses all callers until an address is deliberately added", and asks QA
to run the **refusal** case on prod.

**Observed otherwise.** The "Egalvanic - Internal" staff MCP server connected to this session — which
reaches customer tenants exclusively through these `/staff/*` routes — **successfully returns production
data**: `list_companies` yields **36 real customer tenants** (Eaton, Miller Electric, Koch Wichita
Campus, eee/453 sites), and `get_company_profile` for `acme` returns company id
`0a61e613-7887-4c10-99bf-59cedc4460f2` with branding in the **`eg-pz-prod-s3-branding-ohio`** bucket.
QA's `acme` is `d59d449b-09d8-45d6-8f0a-ef70024b1293`.

Staff routes that serve data on prod mean the allowlist and client-id list are **configured there**, not
unset. That is either a deployment fact the ticket's environment note is out of date about, or an
unintended enablement — **either way the ticket's prod "refusal" step will not reproduce as written.**
All calls I made through it were read-only (`list_companies`, `get_company_profile`,
`list_report_queries`, `list_sample_entities`); I did not test any negative case against prod, because
production testing needs explicit instruction.

**Worth noting for the ticket's own security story:** this is the elevation working as designed —
audited and tenant-named — but it also means the audit log on prod already has real entries, so step 8
could be verified there by whoever owns that log.

## What would close this ticket

1. **A dev seat + an internal-tools Cognito ID token** for an allowlisted address — unblocks steps 3, 4,
   6, 9, 10, 11, 12, 13 and turns 5 from inconclusive to a real result.
2. **Confirmation of the prod allowlist state**, and whether the prod refusal step still applies.
3. **Audit-log read access** for step 8.
4. **Two tenants on one environment** for step 14's parity check and the wrong-tenant id checks.

## Footprint

Read-only on QA: every call a GET or a list POST. No user, company, config, form or template was
created, modified or deleted. The production reads through the staff MCP are enumerated above and are
flagged rather than buried.
