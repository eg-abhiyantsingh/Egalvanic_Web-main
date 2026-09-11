# All-roles sweep: three High leaks that single-role testing could not see

**Date:** 2026-09-11 (evening) · **Env:** acme.qa.egalvanic.ai V1.36 · **Artifact:** Version 19
**Regression tests:** `src/test/java/com/egalvanic/qa/testcase/TenantAndSiteScopeContractTest.java`
**Run with:** `mvn test -DsuiteXmlFile=suite-tenant-site-scope.xml`

## Why this run happened

Owner: *"check for all role in case if we miss any high bugs… we have tested 50 ticket so can be possible
we might miss something so check in full depth no lazy approach."*

Correct instinct. Every earlier test drove **one role against its own data**, so a rule that is missing
for *everyone* is indistinguishable from normal behaviour. 73 routes × 8 seats (6 customer + 2 staff
controls), cross-role diffed, each candidate challenged by 3 adversarial lenses.

**Sweep cost:** 169 agents, 48 completed, 121 died on the session limit (verify passes for candidates
#13–#53 and the completeness critic never ran). 8 survivors were returned; **I then re-verified all of
them by hand.** 4 confirmed, 2 did not reproduce for me, 1 is Medium, 1 unverifiable.

## Confirmed — and now under automated test

### 1. The user directory of every company is readable by any customer · High

Project Manager, `is_eg_admin: false`, 95 permissions, **does not hold** `features.settings.users.view`.
The `/users` page correctly renders **Access Denied**. `GET /api/users/` from the same session returns
**200 application/json, 265 records across 8 companies** — 43 belonging to 7 *other* tenants, with email,
active flag and MFA state. Foreign emails observed: `eric.ehlert16@gmail.com`, `marali.polasani@gmail.com`.

**Positive control (decisive):** `/materials-library` (18), `/labor-rates` (20) and `/test-equipment` (10)
are comparable company catalogs on the same tenant and return **zero** foreign rows. The filter exists; it
was not applied here.

Evidence: `docs/bug-evidence/2026-09-11-register-rebuild/xtenant-03-users-directory-pm-denied-page-but-api-serves.png`

### 2. Every company's class taxonomies are readable by any customer · High

| Endpoint | Total | Own | Global | **Foreign** | Foreign companies |
|---|---|---|---|---|---|
| `/api/node_classes` | 637 | 66 | 44 | **527** | 11 |
| `/api/edge_classes` | 54 | 12 | 3 | **39** | 11 |
| `/api/issue_classes` | 50 | 20 | 9 | **21** | 11 |

The Classes screens are gated client-side on role **name**; the data behind them is not gated at all.

### 3. The asset-lookup family ignores site assignment · High · ALL SIX customer roles

FM assigned 11 of 241 sites; PM 114 of 241. For a site **outside** that list:

| Call, same seat, same unmapped site | Result |
|---|---|
| `GET /api/sld/{id}` (diagram) | **422** refused |
| `GET /api/connections/v2/sld/{id}` | **422** refused |
| `GET /api/nodes/sld/{id}` | **200 JSON — 282 named assets** |
| `GET /api/lookup/v2/nodes/{id}` | 200 JSON — 50 rows |
| `GET /api/lookup/node-class-counts/{id}` | 200 JSON — 29 classes |
| Positive control: own site | 200 JSON — 618 assets |
| Negative control: random UUID | 0 rows |
| **Staff ground truth for the same site** | **282 — identical to what FM sees** |

Client Portal is assigned 234 of 241 sites; on the 7 deliberately withheld it still reads 14 assets,
2 issue descriptions (NEC/NFPA-70B) and 7 designations. Its own role description is *"Read-only access to
assigned SLDs"*.

## Did NOT reproduce for me — deliberately excluded

- **CP reads opportunities/quote totals** (agent said 2/3): `/api/opportunities` → 200 **text/html**
  (masked shell), `/api/plans` → **422**. Not a leak on the endpoints I could reach.
- **`/account/{id}/access-list` hands CP the 219-user directory** (2/2): `/api/account/` returned **0
  accounts** for the CP seat, so I could not obtain an account id. Unverified, not published.
- **Agent infra URL** (`/api/agent/health` exposes `http://agent.egpz.qa:8899`): Medium at most, not filed.

## Relationship to this morning's withdrawal — important nuance

This morning I withdrew the cross-tenant P1 because the **by-id** routes are properly scoped (foreign id →
422 for all six customer roles) and the original proof used a staff account. **That withdrawal stands.**
What is new is a different mechanism: the **list** endpoints, which take no id at all, apply no company
filter. Not a leak by pasting an id. A leak by asking for the list.

## The regression tests

`TenantAndSiteScopeContractTest` — three cases × every role:

1. `tenantScopedListsMustNotLeakOtherCompanies` — **fails on all 6 customer roles today**
2. `correctlyScopedCatalogsStayScoped` — the positive control, **passes on all roles** (proves the
   assertion mechanism works and guards the catalogs against regressing)
3. `siteScopedNodeLookupsMustRefuseUnmappedSite` — **fails on all 6 customer roles today**

Live result: **Tests run 21, Failures 12, Skipped 4.** Staff seats skip by design, with the reason printed.
Every assertion carries its own positive control and checks content-type, not just status.
