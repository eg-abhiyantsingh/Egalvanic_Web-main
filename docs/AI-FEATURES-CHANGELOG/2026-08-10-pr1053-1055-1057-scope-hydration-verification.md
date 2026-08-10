# PRs #1053 / #1055 / #1057 — QA verification, and corrections to the ticket

**Date:** 2026-08-10 · **Build:** QA V1.36 · Ticket carried a heads-up that its Problem and QA
sections were **extrapolated from the change summary rather than a recorded defect report**.
That turned out to matter — three of the ticket's statements do not match the PRs.

## Corrections for the author

**1. The Problem statement describes the wrong symptom.**
Ticket: *"role-switch hydration races … could render against the previous role's data."*
PR #1053: *"Intermittent 'Company information not available' / 'Error loading assets' after
switching roles: pages fetched before the auth context rehydrated **and never retried**. Both now
subscribe and refetch on hydration; Sales Overview errors only after a **3s genuine-failure
window**."*
The defect was a **stuck error state**, not stale data rendering. QA item 1 ("confirm no data
from the previous role renders") therefore tests something never claimed. The meaningful check
is: does the page **recover** instead of sticking on an error?

**2. "Data section" hides the actual surfaces.** #1057 guards **Locations, Attachments, Panel
Schedules, Connections, Condition Assessment**.

**3. Tasks is a deliberate exception — QA item 2 as written would produce a false bug.**
#1057: *"Tasks keeps 'all' — now a real endpoint via eg-pz-backend pair PR (its All Facilities
view was a masked 404)."* An instruction to "confirm nothing fetches with 'all'" would flag
Tasks as defective when it is intentional.

**4. The ticket omits #1054**, which #1055 explicitly builds on and which carries the Assets
'all'-scope guard — the root cause of the constant "Error loading assets"
(`/lookup/nodes/all` 404 masked as index.html by CloudFront).

## Results (all four QA items pass, against the corrected reading)

| # | Item | Result |
|---|---|---|
| 1 | Role switch on Sales Overview / Assets | **PASS** — no error at any sample across ~8 s; content rendered |
| 2 | No fetch on the transitional 'all' | **PASS** — 0 all-scoped app calls on Issues, Locations, Connections, Attachments, Panel Schedules, Condition Assessment **and** Tasks (6 s settle) |
| 3 | Both surfaces settle after direct navigation | **PASS** — settled on a real site, no error |
| 4 | Rapid role switching | **PASS** — after deliberate role churn Assets settled clean (role Super Admin, site "Test android"), no error |

## Method note — a near-miss worth recording

A first pass appeared to show Issues fetching `lookup/nodes/all`. It was **my own probe**: the
recorder captures every `/api/` fetch, including the ones the test itself makes. Re-run with
self-traffic tagged and excluded, it was clean. Reporting that would have been a fabricated bug
against a PR that works. Recorders must distinguish app traffic from test traffic.

Also: a 1.2 s settle made every surface look clean; the guarded fetch only fires **after** the
scope resolves, so ~6 s is needed. A too-short wait produces a vacuous pass here.

## Independent finding (not a regression of these PRs)

Unknown `/api/` paths return **HTTP 200 with `text/html`** (the SPA shell) instead of a 404:

| Path | Status | Content-Type |
|---|---|---|
| `/api/lookup/nodes/all` | 200 | **text/html** |
| `/api/this-endpoint-does-not-exist-qa` | 200 | **text/html** |
| `/api/lookup/nodes/<bogus-uuid>` | 200 | application/json |
| `/api/auth/v2/me` | 200 | application/json |

The devs already named this in #1054; it is still live. `response.ok` is true and `JSON.parse`
fails far from the real cause, so the next missing endpoint will be equally hard to find.
**Testing implication:** API tests must assert content-type (or that the body parses as JSON),
not status alone.

Knowledge captured in `docs/product-knowledge/scope-hydration-and-role-switching.md`.
