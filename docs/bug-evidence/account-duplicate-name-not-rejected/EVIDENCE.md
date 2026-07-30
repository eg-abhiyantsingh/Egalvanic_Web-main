# Bug evidence — Account creation does not reject duplicate name/domain (ZP-3156 AC-6)

**Module:** Accounts (Customers) · **Env:** QA `acme.qa.egalvanic.ai` (badge V1.36)
**Verified:** 2026-07-30, authenticated Super Admin session (UI + direct API).
**Severity:** Medium — data-integrity / spec violation, not a crash.

## Spec being violated
ZP-3156 "Updated Create Account Flow" — Acceptance Criterion 6:

> Given the derived subdomain already exists, when the user submits, then submission is
> blocked with a duplicate-domain error and no partial records … are created.

and NFR: "domain/subdomain uniqueness check < 1 s (server-side)".

## Actual behavior
Creating two accounts with the **same name** (and same derived subdomain) both succeed —
no rejection, two rows created.

### API-level proof (single authenticated session)
Two identical `POST /api/account/v2` calls, same `name` + `subdomain`, differing only in
contact email local-part:

```
POST /api/account/v2  {name:"QA_AVX_dupapi_174443", subdomain:"egalvanic",
                       company_id:…, owner:…, contact:{…, email:"dup.a@egalvanic.com"}}
  → HTTP 201   {"account":{…}}

POST /api/account/v2  {…same name…, contact:{…, email:"dup.b@egalvanic.com"}}
  → HTTP 201   {"account":{…}}

GET list, search "QA_AVX_dupapi_174443"  →  total: 2   (two distinct account ids)
```

Expected: the second POST should return a 4xx duplicate error and create nothing.

### Corroboration
- The same double-create reproduces through the **UI** (New Account → same name twice;
  both land in the grid).
- The tenant already contains multiple pre-existing accounts sharing subdomain
  `"egalvanic"` (also `"intel"`, `"aws"`) — consistent with uniqueness never being
  enforced, not just a test artifact.

## Why this matters
Subdomain is the tenant/portal key (auto-derived from the contact email domain, hidden
from the user per ZP-3156). Allowing collisions undermines the "valid unique domain per
account" guarantee the whole create-flow redesign is built on, and can misroute portal
access.

## Regression coverage added
`AccountV135ExtendedTestNG.testDuplicateAccountNameRejected` (group `known-product-bug`,
quarantined from the functional gate). It creates the baseline account, attempts a
same-name create, and asserts a rejection + single row — currently RED with the message
"rejected=false, copies=2 … POST /api/account/v2 returned 201 twice". It self-cleans its
rows and flips GREEN automatically once the backend enforces uniqueness.

## Test-data hygiene
All probe accounts (`QA_AVX_dupapi_*`, `QA_AVX_dup*`) were deleted after capture;
post-cleanup list search for `QA_AVX` returns total 0.
