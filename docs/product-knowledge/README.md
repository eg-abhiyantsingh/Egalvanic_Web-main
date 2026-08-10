# Product Knowledge Base

Durable, **verified** facts about the eGalvanic product — how it actually behaves, what its
APIs return, and the traps that make tests lie. Written for finding *more and better bugs*:
most deep bugs come from knowing a contract well enough to notice when it is quietly violated.

## Ground rules for this folder

1. **Only verified facts.** Every claim here was observed against the running app or a real
   API response. If something is inferred or unconfirmed, it is labelled **UNVERIFIED**.
   A confident-sounding guess in here will cost someone a day.
2. **Record the trap, not just the fact.** "X returns Y" is worth little; "X returns Y, and a
   test that asserts Z passes vacuously because…" is worth a lot.
3. **Date every entry** and name the build (e.g. QA V1.36). This app changes; a fact without a
   date cannot be trusted later.
4. **Link to evidence** — the run, the endpoint, the payload — so a future reader can re-check
   instead of re-deriving.

## Index

| File | What's in it |
|---|---|
| [upload-anything-onboarding-jobs.md](upload-anything-onboarding-jobs.md) | The Upload Anything / onboarding-job lifecycle, the empty-site invite, and why the Dashboard copy of it is unreachable |
| [forced-all-pages-and-site-scoping.md](forced-all-pages-and-site-scoping.md) | Which routes are company-wide vs site-scoped, how the stale `sldId` leak works, how to test it |
| [api-payload-shapes.md](api-payload-shapes.md) | Real response envelopes for the list APIs — the shapes that break naive parsing |
| [qa-env-test-data.md](qa-env-test-data.md) | QA fixtures: company id, empty sites for onboarding tests, site counts |
| [browser-testing-techniques.md](browser-testing-techniques.md) | Fault injection, request capture that survives SPA nav, site switching, and the control-first discipline |
| [scope-hydration-and-role-switching.md](scope-hydration-and-role-switching.md) | Deliberate vs **transitional** 'all', which surfaces are guarded (and why Tasks is an exception), role-switch hydration, and the masked-404 trap |

## Bug-hunting leads parked here

Things noticed but not yet chased. Each is a candidate for a real finding.

| Lead | Why it might matter | Status |
|---|---|---|
| `/jobs` renders an empty `<main>` and fires **zero** API calls for a Super Admin | Either a dead route, a flag-gated one, or a genuinely broken page. PR #1127 changed filtering logic here that nobody can observe. | Open — needs a different account/flag to confirm |
| Dashboard's "Let's get your assets in" invite is unreachable in normal navigation | Dashboard forces `sldId='all'` and the invite bails on `'all'`, so the gating added in PR #1127 has no observable effect there. Dead code, or a latent bug if the bail is ever removed. | Confirmed unreachable 2026-08-10 |
| Unknown `/api/` paths return **200 + text/html** (SPA shell) instead of 404 | Named by the devs in #1054 and still live. `response.ok` is true and `JSON.parse` fails confusingly, so the next missing endpoint will be just as hard to find. API tests must assert content-type, not just status. | Confirmed 2026-08-10 |
| Every page logs 7–9 console errors on load (401s on `action-items/counts`, `ops-attention`, `sales-attention`, `issues/open-by-site`) | Known ambient noise, but it means `verifyPageHealth` must keep ignoring them — and a *real* new error can hide in the crowd. | Known; see `project_arc_flash_module` memory |
