# PR #1127 (FORCED_ALL page scoping + upload invite) — live verification and a regression net

**Date:** 2026-08-10
**Prompt:** eg-pz-frontend PR #1127 shared as "new code" — *Suppress Upload Anything invite during a running job; stop site-scoping the FORCED_ALL pages*

---

## What the PR changed (dev side)

1. **Upload invite** — `Assets.jsx` / `Dashboard.jsx` now hold the "Let's get your assets in"
   invite until `getActiveJob` resolves for the current site, and skip it entirely while a job
   is running/pending. A `jobCheckedForRef` stops a previous site's answer leaking across a
   site switch; a failed lookup falls back to `false`.
2. **FORCED_ALL pages stopped site-scoping** — `/opportunities`, `/planned-work`, `/emps`,
   `/jobs`, `/scheduling` render no site picker, so their data must span every accessible
   site. They were filtering by the store's stale `sldId`.

## Our exposure (what I checked before touching anything)

| Change | Tests referencing it | Verdict |
|---|---|---|
| Upload invite copy | **none** — no test asserts on "Let's get your assets in" / "Upload Anything" | No breakage. The new async gate does add a delay before the invite appears, so any *future* test must wait for it rather than assert immediately. |
| FORCED_ALL scoping | `OpportunitiesTestNG`, `NewModulesSmokeTestNG`, `Phase4QualityGates`, `Phase5ModuleInteraction`, `UiApiDataConsistency`, `AIPageAnalyzer`, others | No `sld_id`/`sldId` assertion anywhere; no hardcoded row counts. `TC_OPP_29` mentions "returns 80" only in a comment and compares lower-vs-upper, so a changed result-set size cannot break it. |

**Net: zero test changes required.** The suite was not asserting the old behaviour.

## Live verification (QA V1.36, Playwright, 2026-08-10)

The local `eg-pz-frontend-reference` clone is stale, so every claim below is from the running
app, not from reading code.

| Route | Request observed | `sld_id`? |
|---|---|---|
| `/opportunities` | `POST …/quotes/v2` body `{"page":1,"page_size":10,"search":"","sort_by":"created_at","sort_dir":"desc"}` | **no** |
| `/emps` | `POST …/committed-quotes/v2` body `{"page":1,"page_size":25,…,"filters":{}}` | **no** |
| `/planned-work` | `GET /api/planned-workorders?page=1&page_size=25&sort_by=window&sort_dir=asc` | **no** |
| `/scheduling` | `GET …/workorders-with-jobs`, `…/sessions`, `…/slds`; page renders 283 KB of work orders | **no** |
| `/jobs` | **zero API calls**, empty `<main>`, no 404 | not observable |

I also reproduced the **persistent** path the PR describes: parked on a site-scoped page so
the store held a real site id, installed a fetch/XHR recorder, then reached each route by
clicking its sidebar link (a real SPA navigation, no document reload). Still zero scoped
requests — the fix holds on the stale-store path, not just on a clean direct load.

`/jobs` is reported as **unverified, not fixed and not broken**: it issues no request for this
account, so its "removed client-side facility filter" change cannot be observed from outside.
Calling it green would be a fabricated verdict.

## What I added: `ForcedAllPagesScopeTestNG` (4 TCs)

This class of bug regresses silently — the grid just looks short, with no visible control to
explain it, and nothing in a screenshot or a status code gives it away. The only durable guard
is asserting on the wire.

- Parks on a **site-scoped** page first so the store holds a real site id. (Arriving with a
  clean store would pass even if the bug were fully reintroduced — the test would be vacuous.)
- Installs a fetch/XHR recorder capturing **URL *and* POST body**, because these endpoints
  carry scope in either form.
- Navigates by **clicking the sidebar link**, so the recorder survives and the traversal is a
  real user path.
- Asserts the route's own data call was actually observed *before* judging scope, so an empty
  capture fails loudly instead of passing vacuously.
- Wired into `suite-opportunities.xml` → runs in parallel-suite.yml's SALES group (41 → 45 TCs).

**Validation — 4/4 green against live QA**, and, because a green test proves nothing on its
own, a temporary negative probe replayed the **pre-fix wire format** (`quotes/v2` with
`sld_id` in the body) and confirmed the detection flags it. The probe was deleted after use.

## Depth explanation (for learning / manager review)

- **Why not just read the diff?** Project memory records that the local frontend clone is stale
  (May 30), so a code-only audit gives false verdicts. Everything here is measured against the
  running QA app.
- **Why the test parks on a site-scoped page first.** This is the whole subtlety of the bug.
  `sldId` is a *leftover*; `Layout` resets it to "all" asynchronously and only on a pathname
  change. A test that lands on `/opportunities` with a clean store exercises none of that and
  would stay green through a full regression.
- **Why assert on the request rather than the row count.** Row counts depend on test data — if
  every quote happened to belong to one site, a count assertion would pass while scoped. The
  request either carries a site id or it does not.
- **Why `/jobs` is left unasserted.** A test that cannot observe the behaviour it names is worse
  than no test: it reports green for a page it never checked.
