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
| [node-mutations-and-arc-flash-inputs.md](node-mutations-and-arc-flash-inputs.md) | The **async** node-mutation pipeline (a 200 ≠ a write), where class attributes really live, `aic_rating` as a node column, SKM's two-stage export, and the unlinked Equipment Designations route |
| [work-order-types-and-eg-forms.md](work-order-types-and-eg-forms.md) | Work-order types (`work_type_id` → services), the EG Forms instance API, the MuiDrawer-not-dialog trap, and how to fake a LaunchDarkly flag-off state |

## Bug-hunting leads parked here

Things noticed but not yet chased. Each is a candidate for a real finding.

| Lead | Why it might matter | Status |
|---|---|---|
| `/jobs` renders an empty `<main>` and fires **zero** API calls for a Super Admin | Either a dead route, a flag-gated one, or a genuinely broken page. PR #1127 changed filtering logic here that nobody can observe. | Open — needs a different account/flag to confirm |
| Dashboard's "Let's get your assets in" invite is unreachable in normal navigation | Dashboard forces `sldId='all'` and the invite bails on `'all'`, so the gating added in PR #1127 has no observable effect there. Dead code, or a latent bug if the bail is ever removed. | Confirmed unreachable 2026-08-10 |
| Unknown `/api/` paths return **200 + text/html** (SPA shell) instead of 404 | Named by the devs in #1054 and still live. `response.ok` is true and `JSON.parse` fails confusingly, so the next missing endpoint will be just as hard to find. API tests must assert content-type, not just status. | Confirmed 2026-08-10 |
| Every page logs 7–9 console errors on load (401s on `action-items/counts`, `ops-attention`, `sales-attention`, `issues/open-by-site`) | Known ambient noise, but it means `verifyPageHealth` must keep ignoring them — and a *real* new error can hide in the crowd. | Known; see `project_arc_flash_module` memory |
| `POST /api/node/create` returns **200 + the echoed value** then produces no asset when a value the storage layer can't accept is sent (out-of-range ints, non-numeric strings) | Web is insulated by client validation, but iOS and bulk import are not — they get a success response for an asset that does not exist. Note a float is *coerced* and a dangling class UUID is *accepted*, so "type-invalid" is the wrong rule. | **Confirmed 2026-08-11**, filed as TEGG-1 |
| A **non-existent `node_class` UUID** is accepted on create and yields a "No Class" asset | Referential integrity gap found while running the control for TEGG-1. Not chased further. | Open — worth its own look |
| Bulk Export → Bulk Import proposes **18 connection updates** for an untouched Connections sheet | Either the preview misreports, or the round-trip is not idempotent. Would destroy trust in bulk import either way. | Open — import deliberately not processed |
| Deleting an EG form instance leaves the Forms tab count stale until reload | Add refreshes the badge, delete does not. Same family as #1077's Equipment Designations staleness — a mutation that refreshes its own view but not the parent counter. | **Confirmed 2026-08-11**, filed as EGF-1 |
| The QA build badge says **V1.36** while the release panel says **"Fixes in Web v1.39.1"** | "Which build am I on?" is unanswerable from the UI, so ticket claims like "not yet in QA" can't be checked without testing behaviour. Cost real time on #1107. | Open — worth resolving |
