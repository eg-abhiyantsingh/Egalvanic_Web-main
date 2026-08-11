# QA verdict — Markup mode: ship page renders + manifest (pipeline #44)

**Assessed:** 2026-08-11 · **Build:** QA **V1.36** · **Tenant:** `acme.qa.egalvanic.ai`
**PR:** eg-pz-engineering-ai-pipeline **#44** (`agent-runner/runner/modes.py`, merged to `cicd/dev` 2026-08-09)
**Related:** ZP-3660 (markup-first extraction, mode B)

---

## Verdict: NOT TESTABLE on QA today — blocked, not failed

Unlike the other two tickets reviewed today, I found **no evidence contradicting** this one's
"dev-only, not yet in QA" note, and four independent blockers stop the checklist from being run at
all. Recording it as blocked rather than green or red.

### What blocks it

**1. No read access to the repository.** `Egalvanic/eg-pz-engineering-ai-pipeline` is not visible
to my GitHub account — `gh repo list Egalvanic` returns only `eg-pz-frontend` and
`eg-pz-mobile-iOS`. For the other tickets I could read the diff and derive the exact contract to
test (payload keys, bounds, naming rules); here I cannot see `modes.py`, so I do not know the
manifest's real field names, the stem-naming rule, or how `px = pt × scale` is expressed. Testing
against a contract paraphrased from the ticket description would produce assertions that pass or
fail for the wrong reasons.

**2. No in-app markup review viewer on QA.** The whole point of the change is to give that viewer
something to draw on. It is not reachable: `/markup`, `/review`, `/onboarding` and `/jobs` all
render an **empty `<main>`**. (`/jobs` rendering empty is a pre-existing condition already recorded
in `docs/product-knowledge/` from PR #1127 testing — it fires zero API calls for a Super Admin.)

**3. The artifact is a shipped workbook, not an API surface.** `output/renders/<stem>.jpg` plus
`manifest.json` live inside a shipped workbook. There is no endpoint I found that exposes a
workbook's file tree, and guessing paths is worthless here — unknown `/api/` paths on this host
return **200 + `text/html`**, so a masked 404 cannot be distinguished from a real absence.

**4. Mode B has to be invocable to produce anything.** `/api/onboarding/jobs/active` exists but
requires `sld_id`, and nothing in the onboarding surface exposes a markup-first ("mode B") job
option. Without a way to *run* a markup job on QA there is no workbook to inspect.

### What the ticket's own evidence rests on

The description says it was *"verified by the author against the E0521 sheet PDF locally: stem and
manifest shape byte-match what the staged localdev jobs already consume."* That is a local
verification against staged localdev jobs — reasonable for the author, but it means the only
existing proof lives outside QA, which matches the dev-only placement.

---

## What I need to test it

Any **one** of these unblocks the checklist:

1. **Read access to `eg-pz-engineering-ai-pipeline`** — enough to derive the manifest contract, at
   which point I can validate a workbook's `manifest.json` against it field by field
   (2× scale, `px = pt × scale`, stems pairing to findings files).
2. **A QA-reachable markup-first job** — a way to run mode B on QA and a pointer to where the
   shipped workbook lands, so I can confirm `output/renders/*.jpg` and `manifest.json` are present
   and that a render failure degrades gracefully rather than failing the workbook (the ticket's
   "best-effort" claim, which is the part most worth testing).
3. **The in-app review viewer enabled on QA**, so the end-to-end assertion — findings boxes drawn
   in the right place over the right raster — can be checked visually, which is the only test that
   really matters to a user.

## Testable once unblocked

For whoever picks this up, the checklist I would run:

- Every source PDF page produces a render; page count in `manifest.json` equals the PDF's.
- Geometry: `px == pt × scale` per page, with `scale == 2`, in both directions.
- Stems match the findings-file naming so the viewer pairs render → findings; specifically a
  multi-page PDF where stem ordering could drift.
- **Best-effort:** corrupt or lock one page's render and confirm the workbook still ships with the
  remaining renders, and that the failure is recorded rather than silent.
- Boxes land in the right place: a finding whose PDF-point geometry is known, checked against its
  pixel position in the raster at 2×.

---

## Note on the environment field

This ticket's "dev-only" note appears accurate. That is worth stating explicitly, because the same
note was **stale on both other tickets reviewed today** (Manual shutdown schedules, and EG Forms
#1107 — both already live on QA). The field is not reliable in either direction and should be
re-checked per ticket rather than trusted; the ticket monitor appears to stamp it at creation and
never revisit.
