# `/sld/{sld_id}/library-designations` ignores `accessible_sld_ids` — site-restricted roles read every site's engineering data

**Severity:** Medium–High (in-tenant horizontal scope leak; read-only, no cross-tenant exposure proven)
**Found:** 2026-09-10 while API-testing the ZP-3904 designation schedules
**Env:** `acme.qa.egalvanic.ai` · V1.36 · tenant acme · **read-only** (GET only, nothing modified)
**Artifact:** https://claude.ai/code/artifact/06ad5f78-f47f-4875-bb22-588cc34ce68f
**Endpoint:** `GET /api/sld/{sld_id}/library-designations?kind=…`
**Surfaces:** the four Engineering › Designations schedules (`/short-circuit-ratings`,
`/feeder-schedule`, `/ocpd-settings`, `/transformer-schedule`) and `/equipment-designations`
**Evidence:** `docs/bug-evidence/library-designations-site-scope/scope-leak-responses.json`

## What happens

The endpoint filters rows by the `sld_id` in the path and **never checks whether that SLD is in the
caller's `accessible_sld_ids`**. A user mapped to a handful of sites can read the full engineering
designation record for any site in the tenant by supplying its id.

Target SLD `02b0a2a9-b85c-4da4-b1fb-c5e6ca9e7418`, `?kind=sccr&limit=10`:

| seat | mapped sites | target mapped to seat? | HTTP | rows | first two assets |
|---|---|---|---|---|---|
| Project Manager | 114 | **yes** | 200 | 10 | `B 14`, `B 15` |
| **Facility Manager** | **11** | **NO** | **200** | **10** | `B 14`, `B 15` |
| **Electrical Engineer** | **4** | **NO** | **200** | **10** | `B 14`, `B 15` |
| Client Portal | 234 | yes | 200 | 10 | `B 14`, `B 15` |
| Account Manager | 113 | yes | 200 | 10 | `B 14`, `B 15` |
| Technician | 113 | yes | 200 | 10 | `B 14`, `B 15` |

**The FM and EE row id lists are byte-identical to the PM's.** Not a truncated or redacted view — the
same records.

It applies to every scope, not just one:

| `kind` | rows returned to FM on the unmapped site |
|---|---|
| `sccr` | 10 |
| `feeder` | 4 |
| `ocpd` | 4 |
| `transformer` | 2 |
| `all` | 10 |

## Why this is a defect and not by design

**The platform enforces this scope elsewhere, on the same seats, in the same session.**
`GET /api/reporting/sample-entities` refuses both FM and EE with a clean
**422 `permission_denied` / `required_permission: reports.view`**, and `POST /api/v2/issues/list` with a
foreign `company_id` refuses with **422 `permission_denied` — "You do not have access to this company's
data."** So scope checks exist and work; this endpoint is missing one.

A Facility Manager is deliberately restricted to 11 of 114 sites. The data returned is not trivial —
asset labels, classes, ampere ratings, kVA, %Z, frame settings, `bus_summary`, manufacturer and
engineering status for equipment at sites that seat is not entitled to see.

## Repro (two requests)

```
# 1. sign in as the Facility Manager seat and read its scope
GET /api/auth/me                    -> accessible_sld_ids: 11 ids
                                       (02b0a2a9-b85c-4da4-b1fb-c5e6ca9e7418 is NOT among them)

# 2. ask for that unmapped site's designations anyway
GET /api/sld/02b0a2a9-b85c-4da4-b1fb-c5e6ca9e7418/library-designations?kind=sccr&limit=10
                                    -> 200, 10 rows, identical ids to the Project Manager's response
```

**Expected:** 403/422, the way `/reporting/sample-entities` refuses the same seat.
**Actual:** 200 with the full record set.

## Related, and why it matters more than it looks

This is the **same family** as the already-recorded open finding that `/ir_session/{id}/full`, `/team`
and `/summary/v2` return complete payloads for a work order whose site is outside the caller's
`accessible_sld_ids`, while the corresponding *list* endpoints scope correctly. The pattern is
consistent: **list endpoints derive scope from the token; by-id endpoints trust the id in the path.**
Worth an audit of the whole by-id family rather than a one-line fix here.

## Two lesser observations on the same endpoint

- **An unknown SLD id returns `200` with an empty result set**, not a 404 — so "no such diagram" and
  "a diagram with nothing on it" are indistinguishable. A malformed id falls through to the SPA's HTML.
- **An unknown `kind` is not rejected**: `?kind=banana` silently returns the `all` scope instead of a
  400, so a typo in a deep link looks like a working page.

## Not tested, deliberately

The sibling **write** endpoints — `PATCH /sld/{id}/library-designations/engineering-status` and
`…/available-fault-current` — take the same path parameter and may well share the missing check. I did
**not** attempt a write against an unmapped site, because that would modify real engineering data on a
site the seat should not touch. **This should be the first thing the fix verifies**: if the read path
does not check membership, the write path probably does not either, which would turn a read leak into an
unauthorised-mutation bug.

## Footprint

Read-only. Every call was a `GET`; no asset, designation, engineering status or fault-current value was
created, modified or deleted. Only the acme QA tenant was contacted — no second tenant was used, so
**cross-tenant behaviour is untested and unclaimed** (per the QA-only environment rule, that needs a
second populated tenant or explicit instruction).
