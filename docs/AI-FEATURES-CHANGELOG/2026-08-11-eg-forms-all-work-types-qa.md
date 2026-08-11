# EG Forms on every work order type (#1107) — full QA pass

**Date:** 2026-08-11 · **Build:** QA V1.36 · **PR:** eg-pz-frontend #1107
**Report:** [`docs/bug-reports/2026-08-11-eg-forms-all-work-types-QA.md`](../bug-reports/2026-08-11-eg-forms-all-work-types-QA.md)
**Evidence:** `docs/bug-evidence/eg-forms-all-work-types/` (4 screenshots)

## Outcome

**All 6 QA items PASS.** One new low-severity defect (EGF-1: Forms tab count stale after delete).

Also corrected a ticket error: the *Scope at filing* note says **"dev only, not yet in QA"**, but
the change **is live on QA** and was tested there.

## What I did first, and why it mattered

The ticket said the work was dev-only. If true, every item below would have been testing the *old*
build and I would have filed six false failures. So the first action was proving deployment, not
testing behaviour: the Forms tab is enabled on all seven work types, and on an IR work order it
issues `GET /api/eg-form-instance/by-session/{id}` — `SessionEGFormsTab`, the exact component this
PR made universal. Only then did I start on the checklist.

## The core test

On an IR work order (the ticket's stated dead end): attach → open → fill → save → **verify
server-side**. `POST /api/eg-form-instance/create-for-asset`, then the viewer, then
`PUT /api/eg-form-instance/{id}`, and finally a re-read confirming all four typed values had
persisted. A 200 is not persistence — that lesson came from the TEGG ticket earlier the same day.

## Two places I nearly filed a false bug

**1. "Empty context menu on AF and PM Forms."** My first sweep found no menu items on those two
types, which reads exactly like the gating fix not covering them. The tell was in the tab labels:
every type that worked showed a count badge ("Assets 45"), while those two showed a bare "Assets".
They had **zero assets** — no row to right-click. Re-run against work orders that have assets, both
behave correctly.

**2. "Clicking the Forms cell does nothing."** The cell click fired the right request but
`[role="dialog"]` count was 0. The viewer is a **MuiDrawer**, not a dialog. Checking the DOM instead
of trusting the selector showed a fully rendered, fillable form.

Both would have been embarrassing in front of the PM, and both were caught by asking "what else
explains this?" before writing anything down.

**3. "No way to detach a form."** The Forms-tab row menu offers only *Copy Data To…*, and the assets
context menu has no Remove. I was one step from reporting a missing capability — the delete lives
**inside the form viewer** as an icon button (`aria-label="Delete this form"`).

## Testing the flag gate without a second tenant

Item 6 needs a company **without** `feature-eg-forms`. EG-ACME has all 35 LaunchDarkly flags set to
true, so the state cannot be produced by clicking. Rather than log into the second tenant (which
would clobber the acme session — the two subdomains share cookies), I intercepted the LaunchDarkly
`evalx` response with Playwright routing, forced `feature-eg-forms = false`, and aborted the
streaming channel so the real value could not be pushed back. Tab rendered **disabled**; removing
the interception restored it. Injection plus control, so the flag is demonstrably the cause.

## EGF-1 — the one defect

Add refreshes the tab count; delete does not. After deleting an instance the grid empties but the
tab still reads "Forms 1", and stays stale after 8 s — only a reload fixes it. Server `/count`
reports `total: 0` throughout. Reproduced twice.

The asymmetry is visible in the network trace: add fires `…/count` and `…/node-status` after
mutating, delete fires only its `PUT`. Same family as PR #1077's Equipment Designations staleness —
a mutation path that refreshes its own view but not the parent's counter.

## Verification

Before writing the findings up, I ran a 21-audit adversarial workflow over the seven headline claims
from **both** of today's tickets (3 independent lenses each: correctness, evidence-sufficiency,
trap-exposure), specifically to catch overclaims before they reach the PM.

## Depth notes

- Work orders are `/sessions`; the list is `POST /api/company/{cid}/workorders/v2` with envelope
  `data.items`. Work type is `work_type_id` → `/api/procedures-v2/services` (`type` field);
  `null` = General. QA has PM Forms(10) · AF(3) · Checklist(1) · COM(1) · IR(1) · Schedule(1).
- EG Forms API: `by-session/{id}`, `by-session/{id}/count`, `by-session/{id}/node-status`,
  `by-session/{sid}/by-node/{nid}`, `available-for-node/{nid}`, `create-for-asset`,
  `PUT {id}` (save), `PUT {id}/delete` (delete — note the verb).
- The QA build badge says **V1.36** while the release panel says **"Fixes in Web v1.39.1"**. Worth
  resolving; "which build am I on?" should not be ambiguous.
