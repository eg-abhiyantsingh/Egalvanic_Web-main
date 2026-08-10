# PR #1127 — full QA checklist executed on QA, + upload-invite regression net

**Date:** 2026-08-10 · **Build:** QA **V1.36** · **Ticket:** [Web] Suppress the Upload Anything
invite during a running job; stop site-scoping the FORCED_ALL pages

Closes the gap left by the earlier pass, which had verified only the site-scoping half.

## Checklist results

| # | QA item | Result |
|---|---|---|
| 1 | Invite must not appear on **Assets** while a job is running/pending | **PASS** — hidden for both `running` and `pending`, where the control shows it |
| 1b | …same on **Dashboard** | **NOT VERIFIABLE — see finding below** |
| 2 | Switch sites mid-check; previous site's answer must not leak | **PASS** — site A's slow "running" never leaked to site B |
| 3 | Break `getActiveJob`; invite must still appear (fail open) | **PASS** — both a rejected promise and an HTTP 500 leave the invite visible |
| 4 | All five FORCED_ALL routes span every accessible site | **PASS** on 4; `/jobs` not observable |
| 5 | Persistent window: create a quote from `/opportunities` | **PASS** — see below |
| 6 | Transient window: direct navigation, no race | **PASS** — no scoped request on direct nav to any of the four |
| 7 | `/jobs` shows whole job trees | **NOT VERIFIABLE** — page issues zero API calls |

## Method

The invite's three interesting states cannot be produced by clicking — you cannot ask the
backend for "running", and certainly not for "broken". So `window.fetch` was wrapped and
`GET /api/onboarding/jobs/active` answered locally, with the mode switched at runtime
(`off` / `running` / `pending` / `break` / `http500`, plus a **delayed** answer for the race
test). Pages were re-entered by clicking sidebar links, so the shim survives (a full reload
would drop it).

**Every negative assertion was paired with a positive control** on the same page and site.

## Evidence

**Invite gating** — empty site "Test without location", `/assets`:

| Condition | Invite |
|---|---|
| no job (control) | **shown** |
| `running` | hidden |
| `pending` | hidden |
| lookup rejects | **shown** (fails open) |
| lookup 500s | **shown** (fails open) |
| check in flight | **held** — not flashed |

**Site-switch race (item 2):** site A answered "running" after a deliberate 2.5 s delay; switched
to site B immediately. At 1200 ms (inside A's window) B's invite was correctly *held*; once B
resolved it appeared. A's stale answer never applied to B.

**Persistent window (item 5):** created a real quote from `/opportunities`
("QA-AUTO PR1127 persistent-window …", facility *Android Site 2*, opportunity
*29 july abhiyant v1- 1*). The app set the active site and navigated to `/plans/{id}`. Returning
to `/opportunities`: request body carried **no `sld_id`**, total moved **150 → 151**, and page 1
showed rows from **two different sites**. Not filtered.

**Scope, response level (item 4)** — rows carry `sld_id` + `sld_name`:

| Route | Rows | Distinct sites | Total |
|---|---|---|---|
| `/opportunities` | 50 | 19 | 150 |
| `/emps` | 50 | 17 | 147 |
| `/planned-work` | 9 | 2 | 9 |

## Two things that cannot be signed off

**Dashboard invite is unreachable.** `/dashboard` *does* call `getActiveJob`, but its invite
never renders in normal navigation: dashboard is itself a FORCED_ALL page, so `sldId` is `"all"`
and the invite bails on `"all"`. Verified by control: with an empty site and **no** job the
Dashboard invite is *also* absent. So "hidden during a job" is true there for the wrong reason —
the gating added to `Dashboard.jsx` has no observable effect. Either the bail makes that code
dead, or the invite is meant to appear and cannot. **A product decision, not a test failure.**

**`/jobs` issues zero API calls** for a Super Admin and renders an empty `<main>` — no 404, and
no feature flag mentioning jobs in `features/sync`. The removed client-side facility filter
("a stale site hid whole job trees") is therefore unobservable on QA today. Reported as
unverified rather than green.

## What was added

`UploadInviteGatingTestNG` (1 TC, 5 assertions) in `suite-asset-1-2.xml` (69 → 70 TCs).
It discovers an empty site at run time (emptiness drifts on this shared tenant), asserts the
control, then drives all four injected states.

**Its first run FAILED — on its own control**, because the site switch had silently not taken.
That is the guard working: without it the suppression checks would have gone green while
looking at the wrong site. Fixed by clearing the field before typing, matching the option on
exact text, and asserting the picker afterwards; presence checks now poll (the gate deliberately
*holds* the invite, so a single early read sees "hidden" on a page that is merely still
deciding). Re-run: **PASS**.

Product knowledge captured in `docs/product-knowledge/` (new folder) and in memory.
