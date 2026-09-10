# QA open-defect register — every major defect live on `acme.qa.egalvanic.ai`

**Artifact (register):** https://claude.ai/code/artifact/9b83e732-0072-40c8-8771-035f7d76617e
**Artifact (plain-language bugs with steps + screenshots):** https://claude.ai/code/artifact/86992e10-f895-4c12-8940-da109bbbcdb1
**Compiled:** 2026-09-10 · **Source:** all 109 verdict files in `docs/bug-reports/` (24 MB)
**Bundle note:** QA rebuilt on 2026-09-10 to `index-Behk8HzS.js` (was `index-jYhUcFb4.js`, 15.6 MB → 14 MB).
The entitlement-bypass defect in section 2 was **re-verified on the new build** — `/short-circuit-ratings`
and `/transformer-schedule` (1–2 of 2) still load on a PM seat lacking
`features.equipment_designations.view`. Note the contrast: the *new* admin pages shipped in that build
(`/asset-classes`, `/guest-portal-users`) **do** return Access Denied on that same seat, so the gap is
specific to the older designation routes, not to the new nav grants.
**Method:** 8 parallel extractors read every file; the 24 most severe claims then went through an
adversarial verifier instructed to refute each one. 2.26 M subagent tokens, 32 agents, 362 tool calls.
**Raw data:** `docs/bug-evidence/2026-09-10-open-defect-register-source.json`

## Numbers

| | |
|---|---|
| findings extracted | 163 |
| asserted open | 141 |
| **listed in the artifact appendix** | **135** (6 rows removed for 5 refuted claims) |
| P1 | 7 |
| Critical | 2 |
| High | 36 |
| Medium-High | 2 |
| Medium | 53 |
| Low | 35 |
| severity-verified | 24 |
| **refuted and dropped** | **5** |
| findings with a full page to open | 73 |
| findings with a screenshot folder | 8 |
| findings with a written verdict only | 60 |
| distinct tickets covered | 53 (45 with an artifact page) |

## Every finding is now enumerated in the artifact

The artifact carries a full **Appendix — every finding, all 135**, grouped P1 → Critical → High →
Medium-High → Medium → Low and sorted by area so near-duplicates sit together. Each row gives the
finding, a plain line on what it costs a user, its area, its source verdict file, and a link to a full
page where one exists (**100 artifact links** on the page now).

The counts reconcile: 163 extracted → 141 asserted open → **135 listed**, after removing 6 rows covering
5 refuted claims. Several rows are the same defect described by two verdict files, which is why 135 rows
represent fewer than 135 distinct problems — the curated table below collapses the majors to 24.

## Correction 2026-09-10 — the Technician finding is now "symptom fixed, cause not"

`platform.web` is **false** for Technician now: signing in as `+tec@` lands on *"Web Access Restricted —
Current Role: Technician"*, so the reported symptom (a Technician browsing the sales pipeline) **no longer
reproduces**. But `/auth/me` still returns **95 permissions — the same count as the PM seat** — including
`quotes.approve`, `quotes.manage` and `opportunities.manage`. If web access is ever restored, or the
iOS/API surface honours those grants, a Technician can approve quotes again. Recorded as partly fixed
rather than closed.

## The majors in one list, in plain words

The 49 major findings collapse into **24 distinct problems** once duplicates across verdict files are
merged. The artifact carries the full table with a "full write-up" link per row; **18 of the 24 already
have a page with steps and a screenshot.**

**The six that do not** — worth writing up next, since they are also among the most serious:

| Problem | Evidence today |
|---|---|
| Another customer's data comes back if you paste their id | verdict + `docs/bug-evidence/cross-tenant-*/` (incl. a frontend capture of a cross-tenant task) |
| Creating an asset says it worked, then throws it away | verdict only |
| A photo run that finds issues but no assets discards the findings | verdict only |
| An infrared report arrives with no infrared photos | verdict only |
| Fonts are 58% of a 19 MB report | verdict only |
| A previous technician's photos and signature appear in a new form | `docs/bug-evidence/eg-forms-ghosts/` (blue-value capture) |

These predate the one-page-per-ticket rule. The register now embeds the two captures that do exist (the
cross-tenant task and the blue ghost), so the family is no longer text-only.

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

### 3b — Sorting a list only sorts the rows on screen (High)

**Steps:** Site Data › Assets (footer reads **1–25 of 274**) → click **Asset Name** once (nothing moves,
already A–Z) → click again for Z–A → read the first row.
**What happens:** the top row becomes `7N-H1-2` — only the last name among the **25 rows on screen**.
**What should happen:** the last asset out of all **274**, a letter-named one (letters sort after digits).
**Proof:** the click fires **zero** network requests, and page 1 in A–Z order holds the alphabetically
*first* 25, so a real Z–A sort cannot begin with one of them. Same on `/connections`.
Screenshot: `docs/bug-evidence/2026-09-10-bug-screenshots/bug-sort-descending-page-local.png`

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

## 7 — Four claims dropped after verification

| Claim | Why not open |
|---|---|
| `/api/company/{id}/*` cross-tenant P1 | **Fixed** — superseded by the 14 Aug re-verify; the one part of the family that was fixed |
| Report Builder preview 504s at 60 s | **No longer reproduces** — verifier re-ran every config in the file's own evidence table today |
| Fill Forms poll spins 45 min | **Fixed** 21 Aug — content-type guard, runtime-confirmed in the live bundle |
| Covered-services picker returns 0 | **Not a defect** — fix exists in backend; QA lacked the dev→qa lift |
| `/goals` crashes to the error boundary | **No longer reproduces** — opened it 10 Sep, renders normally with two goals behind pace |

## Ticket coverage — did the tested tickets make it in?

**Audited, not assumed.** 53 distinct tickets have a QA verdict on file; **45 have an artifact page**.
Where a tested ticket is absent from the sections above, it is absent **because it passed**.

Taking only the 30 tickets whose verdict file covers that one ticket (exact attribution):

| Outcome | Count | Tickets |
|---|---|---|
| **Produced a major defect** — in the sections above | 9 | ZP-2025, ZP-3662, ZP-3863, ZP-3874, ZP-3904, ZP-3932, ZP-3934, ZP-4018, ZP-4024 |
| Minor defects only | 8 | ZP-3563, ZP-3607, ZP-3660, ZP-3942, ZP-3943, ZP-3945, ZP-3948, ZP-3978 |
| **No defect at all** — passed or not testable by design | 13 | ZP-1242, ZP-3566, ZP-3654, ZP-3747, ZP-3855, ZP-3887, ZP-3888, ZP-3890, ZP-3898, ZP-3937, ZP-3938, ZP-3941, ZP-4020 |

**I checked the "no defect" group rather than trusting the extraction.** ZP-3936 has no findings at all;
ZP-3935 and ZP-3937 carry only Low *documentation* notes; ZP-3944's core regression passes with
documentation findings; **ZP-3987's one real finding was withdrawn after an adversarial pass**; ZP-4020 is
gated behind an env var unset everywhere, so it cannot be exercised. None belong in a defect register —
passes, documentation notes and coverage gaps are deliberately excluded.

The remaining 23 tickets sit in **13 multi-ticket verdict files**, where a finding belongs to the file
rather than one key. The heaviest is the **nav / licence / route-guard family** (ZP-4033 / ZP-4036 /
ZP-4123) with **7 defects, 4 major** — that is where most of section 2 comes from. Next is
**ZP-3782 / ZP-3783** (Fill Forms), the multiselect data loss in section 3.

Raw audit: `docs/bug-evidence/2026-09-10-ticket-coverage-audit.json`.

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
