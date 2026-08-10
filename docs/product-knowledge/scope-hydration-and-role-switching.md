# Scope hydration, the transitional 'all', and role switching

**Verified:** 2026-08-10 against QA **V1.36**. Origin: eg-pz-frontend PRs #1053, #1054, #1055, #1057.

## Two different meanings of "all" — do not conflate them

This trips people up, because the same value is correct in one place and a bug in another.

| | Meaning | Correct behaviour |
|---|---|---|
| **Deliberate 'all'** | The user is on a FORCED_ALL page (Quotes, EMPs, Planned Work, Scheduling) with no site picker | Fetch **company-wide** — see [forced-all-pages-and-site-scoping.md](forced-all-pages-and-site-scoping.md) |
| **Transitional 'all'** | A site-scoped page is mounting and the store has not yet resolved the real site | **Do not fetch at all** until it resolves |

A site-scoped page that fetches during the transitional window shows an "all-sites flash" — or
worse, hits a path that does not exist server-side.

## The guarded surfaces

| PR | Surfaces guarded |
|---|---|
| #1054 | Assets (root cause of the constant "Error loading assets") |
| #1055 | Issues |
| #1057 | Locations, Attachments, Panel Schedules, Connections, Condition Assessment |

**Tasks is a deliberate EXCEPTION.** #1057: *"Tasks keeps 'all' — now a real endpoint via
eg-pz-backend pair PR (its All Facilities view was a masked 404)."* So a blanket QA instruction
of the form "confirm nothing fetches with 'all'" would flag Tasks as a defect when it is
intentional. Verified 2026-08-10: all seven surfaces (including Tasks) issued **zero**
`all`-scoped calls when entered from a FORCED_ALL page with a 6 s settle.

## Role-switch hydration (#1053) — what the bug actually was

The real symptom was **not** "the previous role's data renders". It was: the page fetched
*before the auth context rehydrated*, the fetch failed, and it **never retried** — leaving
"Company information not available" (Sales Overview) or "Error loading assets" (Assets) stuck
on screen. The fix subscribes and refetches on hydration, and Sales Overview only shows an
error after a **3 s genuine-failure window** (mirroring Dashboard).

Our suite already tracks this symptom: `BugHuntDashboardTestNG.testBUG012_CompanyInfoNotAvailable`.

Verified 2026-08-10: after switching roles on Sales Overview, and after rapid role churn on
Assets, **no error appeared at any point** across ~8 s of sampling and both pages rendered.

## Testing gotchas

- **Role switching REDIRECTS.** Switching role navigated to `/reporting/builder` and, during
  rapid switching, to `/site-walks` — a full navigation that destroys the JS execution context.
  A script that switches roles and then samples in the same evaluate will simply die. Switch,
  navigate back, then sample.
- **Sample through the window, don't take one reading.** The 3 s failure window means a single
  early or late read can miss the state you are testing. Sample repeatedly and report both
  "ever seen" and "at settle".
- **A short settle hides the defect.** With a 1.2 s wait every surface looked clean; the real
  test needs ~6 s, because the guarded fetch fires *after* the scope resolves.

## Unknown /api/ paths are masked as 200 + HTML (still true)

The infrastructure trap named in #1054 is still live on QA:

| Path | Status | Content-Type |
|---|---|---|
| `/api/lookup/nodes/all` | **200** | **text/html** (SPA shell) |
| `/api/this-endpoint-does-not-exist-qa` | **200** | **text/html** (SPA shell) |
| `/api/lookup/nodes/<bogus-uuid>` | 200 | application/json (proper response) |
| `/api/auth/v2/me` | 200 | application/json |

So an **unknown** API path returns the SPA's index.html with a 200 rather than a 404. Any client
checking `response.ok` sees success; `JSON.parse` then throws a confusing syntax error far from
the real cause. This is infrastructure (CloudFront fallback), not a regression of these PRs —
the frontend guards route around it — but it is why the original bug was so hard to diagnose,
and it will mask the next missing endpoint just as effectively.

**Testing implication:** an API test asserting only on status code cannot detect a missing
endpoint here. Assert the **content type** (or that the body parses as JSON) too.
