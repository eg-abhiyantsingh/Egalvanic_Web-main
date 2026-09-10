# QA open-defect register — every major defect live on `acme.qa.egalvanic.ai`

**Artifact:** https://claude.ai/code/artifact/9b83e732-0072-40c8-8771-035f7d76617e
**Compiled:** 2026-09-10 · **Source:** all 109 verdict files in `docs/bug-reports/` (24 MB)
**Method:** 8 parallel extractors read every file; the 24 most severe claims then went through an
adversarial verifier instructed to refute each one. 2.26 M subagent tokens, 32 agents, 362 tool calls.
**Raw data:** `docs/bug-evidence/2026-09-10-open-defect-register-source.json`

## Numbers

| | |
|---|---|
| findings extracted | 163 |
| asserted open | 141 |
| **major (P1 / Critical / High)** | **49** |
| medium + low | 90 |
| severity-verified | 24 |
| **refuted and dropped** | **4** |

## 1 — Cross-tenant IDOR is only partially fixed (the headline)

The tenant guard was applied to `/api/company/{id}/*` and nowhere else. **Ten routes still return
another customer's data by id alone**, each reproduced by a non-staff customer admin
(`is_eg_admin:false`) with own-tenant and random-id controls:

| Route | What leaks | Sev |
|---|---|---|
| `POST /ir_session/scope-preview` | 84 foreign assets + building/floor/room PII; **both directions**, from the caller's own host | P1 |
| `GET /eg-form-instance/by-session/{id}` | 8 EG form instances with submitted field data; session list is self-discovering | Critical |
| `GET /api/sld/{id}` | the entire foreign SLD document | P1 |
| `GET /api/contact/by-sld/{id}` | contact PII — email, full name, job title, contact id | P1 |
| `GET /api/issues/open-by-site?company_id=` | foreign site names + open-issue counts (query-string selector) | P1 |
| `GET /api/mapping/node-session/by-session/{id}` | foreign session→node mapping, supplying more ids to pivot on | P1 |
| `GET /eg-form-instance/previous/{form}/{node}` | a foreign tenant's `form_submission` | High |
| `POST /eg-form-instance/{id}/pin` | accepts and **persists** a cross-company pin where 404 is required | High |
| offline queued-mutation path | cross-tenant **CREATE** still allowed (UPDATE/DELETE fixed) | High |
| `GET /api/ir_session?limit=50` | 1,734 sessions including other tenants' work orders | High |
| `/api/company/{id}/*` | foreign id → **422** | **FIXED** |

**Recommendation: fix the shape, not the routes.** The pattern is consistent — *list endpoints derive
scope from the token; by-id and by-scope endpoints trust the id.* Ten patches will leave the eleventh
hole; this wants a framework-level default.

**The same shape exists inside one tenant.** Found today: `GET /sld/{id}/library-designations` never
checks `accessible_sld_ids`, so an FM mapped to 11 of 114 sites reads byte-identical rows to a PM for an
unmapped site — `2026-09-10-QA-library-designations-site-scope-leak.md`.

## 2 — Entitlements declared but not enforced

- **21 routes have no route-level guard** — incl. `/assets`, `/issues`, `/opportunities`, `/arc-flash`,
  `/slds`, `/agent`.
- **The four Designations schedules serve full data without the entitlement** —
  `features.equipment_designations.view` is held by only the Electrical Engineer, yet all four load on PM.
- **Maintenance Portal licence lock is menu-only** — padlocked pages open by URL; Compliance renders 638
  deviations on a Free licence.
- **Nav and route disagree on `/maintenance/*`** — EE sees a link to Access Denied; FM sees no link but
  the page renders; Client Portal reaches everything through a portal bypass.
- **`company_data.manage` is held by no role**, hiding the Builder rail from everyone, while
  `/issue-suggestions` opens by URL fully editable.
- **Two permission namespaces** — 20 `features.*` alongside the plain ones; `X.view` does not imply
  `features.X.view`.
- **Technician over-privileged** — 93 permissions vs PM's 90, including the full sales pipeline.

## 3 — Silent data loss (a 200 that saved nothing)

| Where | What is lost |
|---|---|
| `POST /api/node/create` | 200 with the payload echoed back, then the asset is discarded. 6 attempts → 3 assets |
| Fill Forms from Photos | multiselect written as a scalar; on the 9 Sep retest **nothing** lands and the dialog reports success |
| Upload Anything | a run with issues + photos but zero assets is killed by validation and the extras discarded |
| Quote labor | the Billed-hours override is discarded on a plain page reload |
| Pricing spec builder | reports `applied, error null` while silently dropping a reserved formula name |
| WO check-offs | bulk Mark As posts synthetic row ids → 400 invalid id, no toast, console only |

## 4 — The IR report a customer receives

- **Fonts are 58% of the file** — a font subset per page, 5,397 programs for 10 typefaces, 11.08 MB of
  19.23 MB, over the 10 MB mail cap. Images are the small part.
- **The Infrared Thermography report contains no thermal images** — with the option checked and 23
  healthy IR photo pairs on the work order.
- **Proposed Resolution never renders** — gated behind `{% if "Recommendations" in details_by_key %}`,
  a key no issue class on the tenant defines. One-line template fix.

## 5 — EG Forms blue ghosts

The previous-submission lookup ships `signature` and `image_capture` field objects, so a prior
technician's photos and signature can materialise into a new submitted record — and via the un-scoped
`previous` route above, the ghost can come from **another company**.

## 6 — Found this week

- **ZP-3863** — `?work_type=IR` at the default `limit=10` returns zero IR work orders plus a note
  claiming none exist; `limit=25` returns four. Reproduces on **production**. Also 4 of 7 real queries
  silently ignore `query_name`.
- **ZP-3902** — the migration has not run on QA; two Manufacturer fields coexist and the panel library is
  empty.
- **ZP-3912** — crash fixed and placeholder works, but priority is still a chip; half the header trim.
- A quote keeps its **stale price** after the accepted resolution is edited.
- **`/goals` crashes** to the application error boundary with a TypeError.

## 7 — Four claims dropped after verification

| Claim | Why not open |
|---|---|
| `/api/company/{id}/*` cross-tenant P1 | **Fixed** — superseded by the 14 Aug re-verify; the one part of the family that was fixed |
| Report Builder preview 504s at 60 s | **No longer reproduces** — verifier re-ran every config in the file's own evidence table today |
| Fill Forms poll spins 45 min | **Fixed** 21 Aug — content-type guard, runtime-confirmed in the live bundle |
| Covered-services picker returns 0 | **Not a defect** — fix exists in backend; QA lacked the dev→qa lift |

## How much to trust this

Status means **what the verdict asserts**, not re-tested today. Only the 24 most severe claims were
adversarially verified and **4 of 24 were refuted** — a 1-in-6 staleness rate. Applied to the 117
unverified findings, expect roughly **15–20 already fixed**. This is a prioritised worklist, not a
guaranteed-live bug list.

The P1 cross-tenant family is the exception: re-verified 17 Aug ("nothing has been fixed"), a sibling
route found ungated again **today**, and its own verdict says *"keep the P1 open"*.

**To make it authoritative:** a scripted re-probe of the ten cross-tenant routes — each is one request
with a known control, minutes of work. Cross-tenant checks need the second tenant, so they need explicit
environment approval.
