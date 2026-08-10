# FORCED_ALL pages vs site-scoped pages

**Verified:** 2026-08-10 against QA **V1.36**. Origin: eg-pz-frontend PR #1127.

## The rule

The app has a topbar **site picker** whose selection lives in the store as `sldId`. Most pages
are *site data* and filter by it. A handful of routes are **company-wide** — they sit in
`Layout`'s `FORCED_ALL_PAGES`, render **no site picker**, and must show data for every site the
user can access.

| Route | Sidebar label | Site picker? | Data endpoint |
|---|---|---|---|
| `/opportunities` | Quotes | no | `POST /api/company/{cid}/quotes/v2` |
| `/emps` | EMPs | no | `POST /api/company/{cid}/committed-quotes/v2` |
| `/planned-work` | Planned Work | no | `GET /api/planned-workorders?page&page_size&sort_by&sort_dir` |
| `/scheduling` | Scheduling | no | `GET …/workorders-with-jobs`, `…/sessions`, `…/slds` |
| `/jobs` | *(no sidebar link)* | no | **none observed** |
| `/dashboard` | Site Overview | no | also FORCED_ALL — see the invite note below |

Deliberately **not** in this set: `/planning` genuinely branches on `sldId === "all"` and
converges either way; `/customers` never reads `sldId`.

## Why scope leaked (the mechanism worth understanding)

`sldId` is a **stale leftover** from the last site-scoped page. `Layout` resets it to `"all"`
**asynchronously**, and **only on a pathname change**. That produced two defect windows:

- **Persistent** — creating a quote from `/opportunities` calls `setActiveSldId(...)`. The
  pathname never changes, so nothing resets it and the list stays filtered to that site until
  you navigate away and back.
- **Transient** — on a direct navigation the page's own fetch races the force-to-`"all"`, so
  the first result set is scoped and then silently corrects.

**Why this is nasty to catch:** the page renders perfectly. There is no error, no spinner, no
banner — the grid is simply *short*, and the control that would explain it (the site picker) is
not on screen. Nothing in a screenshot, a status code, or a console log reveals it.

## Current verified behaviour (post-#1127)

Neither the URL nor the POST body carries a site id on any of these routes — including when
arriving from a site-scoped page with a real `sldId` in the store (the "persistent" path), and
including on a direct navigation (the "transient" path).

Response-level confirmation that lists genuinely span sites (rows carry `sld_id` + `sld_name`):

| Route | Rows sampled | Distinct sites | Total |
|---|---|---|---|
| `/opportunities` | 50 | **19** | 150 |
| `/emps` | 50 | **17** | 147 |
| `/planned-work` | 9 | 2 | 9 |

## How to test it (and how to test it *wrongly*)

**Assert on the request, not the row count.** Counts depend on test data — if every quote
happened to belong to the selected site, a count assertion passes while the page is still
scoped. The request either carries a site id or it does not.

**Seed a real `sldId` first.** A test that lands directly on `/opportunities` with a clean
store exercises none of the defect and would stay green through a full regression. Park on a
site-scoped page (e.g. `/assets`) first so the store holds a real site.

**Check the POST body, not just the URL.** `/opportunities` and `/emps` carry scope in the body;
URL-only inspection (e.g. `performance.getEntriesByType('resource')`) cannot see it.

Encoded as `ForcedAllPagesScopeTestNG` (4 TCs, in `suite-opportunities.xml`), which does all
three and additionally fails loudly if the route's data call never appears — otherwise an empty
capture would pass vacuously.

## Open question

`/jobs` is in the set but renders an empty `<main>` and fires **zero** API calls for a Super
Admin. PR #1127 removed a client-side facility filter there that "walked parent rows to filter
children, so a stale site hid whole job trees" — none of which is observable on QA today.
**UNVERIFIED**; needs a different account, feature flag, or seeded job data.
