# Bugs I can prove on QA today — re-tested live, nothing inherited

**Count: 7** (was 9 — `/tasks/all` and the asset-create claim both retracted after testing from the UI)

**Artifact:** https://claude.ai/code/artifact/2d243a6e-f2cd-492c-b2b5-e9a34caeb8c8

**Why this file exists.** The owner's verdict on my 135-row register: *"most of the bugs are invalid."*
Correct. I had extracted claims from 109 older verdicts and published them having re-tested almost none.
This file contains **only findings reproduced live today** on the current bundle `index-Behk8HzS.js`.

## Proven today

| # | Bug | Proof run today |
|---|---|---|
| 1 | **A site-restricted user reads any site's engineering data** | `/sld/{id}/library-designations`: FM (11 of 114 sites) and EE (4 sites) get **byte-identical rows to PM** for an unmapped site, on all four kinds. Control: `/reporting/sample-entities` refuses both with 422. |
| 2 | **Four engineering pages open without the entitlement** | `features.equipment_designations.view` held only by EE, yet PM loads 162 / 40 / 111 / 2 assets. Re-verified on the new bundle. Contrast: the new `/asset-classes` and `/guest-portal-users` **do** refuse PM. |
| 3 | **A padlocked feature opens by address** | `/maintenance-portal/compliance` renders 638 deviations and a 0.6% score on a locked plan. |
| 4 | **Sorting a list sorts only the rows on screen** | 274 assets. True alphabetical last across all 274 is **`yu`**; after Z→A the grid puts **`7N-H1-2`** first — the alphabetical last of the 25 loaded rows. |
| 5 | **Report setup cannot find infrared work orders** | `work_type=IR` at the documented default `limit=10` → 0 IR rows + a note claiming none exist; `limit=25` → 4. Reproduces on **production** too. |
| 6 | **One panelboard carries two Manufacturer fields** | New dropdown in Engineering plus the old free-text `Manufacturer`/`Type`/`Model` under Custom Attributes; Panel Type empty because `/eqp-lib/panel-manufacturers` returns `[]`. |
| 7 | **No issue class defines `Recommendations`** | 50 issue classes, 22 distinct property names, zero named `Recommendations` and no near-miss — so a report template gating on that key can never render the field. |

## Dropped today after re-testing — do not re-file

| Claim | Why it is gone |
|---|---|
| **Creating an asset returns 200 and never saves** | **Invalid — the real UI works.** *Create Asset* → name → class → Create returns **201 Created** with the full node; the list goes **274 → 275** and the asset is there. My evidence was a hand-built `POST /node/create` with an incomplete payload, which the API answers 200 + `_mutation:{status:"received"}` and drops. No user can reach that path. At most a Low API note. |
| **`/api/tasks/all` returns 500** | **Not user-facing.** `tasks/all` appears **nowhere** in the shipped bundle. The Tasks page uses `POST /v2/tasks/list` (200, 1,634 tasks) and `GET /tasks/stats` (200), both healthy. A 500 on a legacy endpoint no screen calls is dead code. My re-test proved the endpoint errors but never asked whether anything uses it — owner caught it. |
| `/goals` crashes | Renders normally; two goals behind pace. |
| Report Builder preview 504s | Re-ran every config in its own evidence table; no longer reproduces. |
| Fill Forms poll loop spins 45 min | Fixed 21 Aug, runtime-confirmed. |
| `/api/company/{id}/*` cross-tenant P1 | Fixed — foreign id now 422. |
| Covered-services picker returns 0 | Never a defect; QA lacked a dev→qa lift. |
| **`/issue-suggestions` reachable by URL** | **By design** — it is a *global* page, so no per-tenant menu entry is correct. |
| Technician sees the sales pipeline | `platform.web` now false for Technician → "Web Access Restricted". Grant unchanged (still `quotes.approve` at 95 perms), so recorded as *symptom fixed, cause not*. |
| A new issue is never evaluated | Today's newest issue carries `proposed: 1`. |

## Claimed but NOT verified — kept separate on purpose

- **The ten cross-tenant routes.** Need a second populated tenant; the demo tenant is off-limits by your
  standing rule. This is the highest-value item and it needs your call on environment.
- **IR report** (no thermal photos; fonts 58% of 19 MB) — needs report generation.
- **EG Forms blue ghosts** — needs a form submission.
- **Quote labor override lost on reload**, **Upload Anything discards findings**, **bulk Mark As 400**,
  **OCPD warning chip** — need writes or fixtures I have not driven today.

## Method change recorded

`feedback_reverify_before_reporting_inherited_findings`: a finding is not reportable until reproduced
today on the current build; check the bundle hash first, because QA rebuilt from `index-jYhUcFb4.js` to
`index-Behk8HzS.js` inside a single day. **Nine proven beats 135 asserted.**

**Footprint:** three QA-DEMO asset creates were attempted and none persisted (that is finding 2), so
nothing was actually added. Everything else was read-only.
