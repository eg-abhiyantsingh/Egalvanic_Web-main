# Build a service by hand — first asset class, forms, rules, tagged uploads — QA verdict

**Tested:** 2026-08-18 · **Build:** QA **V1.36** · **Tenant:** `acme.qa.egalvanic.ai`
**PRs:** eg-pz-engineering-ai-pipeline **#46** (deploy first) · eg-pz-backend **#997** · eg-pz-frontend **#1177** · eg-pz-frontend **#1179**
**Ticket's stated environment:** *"dev only — all three PRs are in the cicd/dev / release-side diff and are not yet in QA."*

---

## Verdict — the frontend is live on QA (note stale again); backend/pipeline deployment unconfirmed; behavioral items need a hands-on pass

Same pattern as most tickets this session: the "dev only / not yet in QA" note is **stale for the frontend** — #1177/#1179 are demonstrably deployed. What I could **not** confirm is that the **backend #997** and **pipeline #46** halves are also on QA, and the ticket itself warns about that exact ordering risk (*"the pipeline change must be deployed first — the backend sends flags a stale image would ignore"*). Nearly all 14 QA-review items are deep service-builder behaviors (form cloning, rule validation, tenancy 403, procedure rename) that need the real backend endpoints or a full click-through I did not complete.

## ✅ Confirmed live on QA (frontend #1177/#1179)

**Bundle strings** — every UI string these PRs introduced is in the shipped JS: `Create Without AI`, `Add asset class`, `Never runs`, `Attach existing form`, `New blank form`, `Build with AI`, `create_forms`, `set_pricing`, `rename`.

**Live in the UI** — the **Create Service** dialog (screenshot):
- offers **"Create Without AI"** (enabled once a name is entered) alongside "Create with AI" — item 1's entry point;
- splits the old single uploader into **paired Forms and Pricing sections, each with its own file uploader and its own switch** — item 7's headline. (Two `checkbox`+`file` pairs confirmed in the dialog DOM.)

Evidence: `docs/bug-evidence/service-builder/create-service-forms-pricing-sections.png`.

## ⚠️ Backend #997 / pipeline #46 — deployment unconfirmed

I could not reach the service-builder's backend routes (versions / build / attach-form / clone) — they're built dynamically in lazy chunks and my path guesses 404'd, so I have **no positive signal** that #997's validation, form-cloning, and tenancy gates are on QA. This matters because the frontend being live without the backend is the failure mode the ticket calls out. **A stale-backend risk is open until confirmed.**

## Behavioral QA items — NOT exercised (need the real endpoints or a full click-through)

None of these were verified; each needs either the backend routes or the multi-step builder flow:

| QA item | Why not done |
|---|---|
| Attach existing form → service gets its own **copy**; editing it never changes the library original | Needs the attach-form + clone flow end-to-end |
| **New blank form** opens the EG Form Builder | Needs the create-form flow |
| Rules editor flags **"Never runs"** after a catch-all; catch-all pinned last; COM-3 rule materializes | Needs a service with rules + the rules editor |
| Editor warns when a rule performs no procedures / a procedure no rule selects | (bundle strings "performs no"/"will never run" not seen in the 10 chunks scanned — may be lazy) |
| Build with both switches off → no invented forms / no site-walk pricing; revise defaults to last build | Needs the `/build` flow with `create_forms`/`set_pricing` (#46) |
| **Negative:** unknown condition key/operator → readable **400**, not 500 or silent accept | Needs `POST …/versions` with a bad spec |
| **Negative:** attach a global / other-company form → **rejected** | Needs the attach-form route + a foreign form id |
| **Negative:** post a version with a rejected spec → **400** not driver 500 | Needs the versions route |
| Status chip stops saying "draft" once work lands | Needs a created service with work |
| **#1179** procedure **rename** changes name only (key stable, rules still resolve) | Needs the method editor |
| **#1179** one-field edit **dims** the list (0.55 opacity) instead of a spinner; expansions/scroll survive | Needs a structural edit on a service |
| **#1179** Add-asset-class dialog does not **reopen** itself afterward | Needs the add-class flow |
| **#1179** navigating between services doesn't show the previous one's work | Needs two services |

## Recommendation

1. **Confirm the backend/pipeline halves are on QA first** (the ordering the ticket stresses). If #997/#46 are *not* deployed while #1177 is, the builder will call routes that behave old-way — worth checking before a manual pass.
2. Then a **dedicated service-builder click-through** covers the rest — it's a genuine end-to-end flow (create-without-AI → add asset class → labor → method editor → attach/clone form → rules editor → rename → save), plus three quick negative API calls (bad condition key → 400; foreign/global form attach → reject; rejected spec → 400). I can drive the whole thing in one focused pass if you want; it's the only way to exercise cloning, rule validation, and the tenancy gates.

## Method notes
- Frontend deployment confirmed two ways: bundle strings + the live Create Service dialog (Create Without AI + paired Forms/Pricing sections).
- No service was created (the Create dialog was cancelled); nothing changed.
- A meta-note across today's tickets: the merge monitor's environment field is unreliable in both directions — several "not in QA" tickets were live, one "not on QA" genuinely wasn't. Every deploy claim this session needed a live check.
