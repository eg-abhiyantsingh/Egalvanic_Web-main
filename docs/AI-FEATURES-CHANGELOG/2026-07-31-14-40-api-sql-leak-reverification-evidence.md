# Re-verify systemic API SQL-leak defect + produce screenshot/PDF evidence

**Date:** 2026-07-31 · **Time:** ~14:40 IST
**Prompt:** check whether the Critical psycopg2/SQL-leak + input-validation ticket is fixed;
deliver a screenshot and PDF to attach to the ticket.

## Outcome
FIXED. All 10 enumerated ticket vectors now return clean 400s (or clamped 200 for pagination)
with zero SQL/psycopg2/traceback/schema text. Delivered a screenshot + PDF evidence pack.

## What I produced
- `docs/bug-evidence/api-sql-leak-input-validation/evidence.html` (source)
- `…/api-sql-leak-reverification-2026-07-31.png` (screenshot to attach — 1180×1820 @2x)
- `…/API-SQL-leak-reverification-2026-07-31.pdf` (4-page PDF to attach)
- `docs/changelogs/2026-07-31-api-sql-leak-input-validation-reverification.md`

## Depth explanation (how the verdict was reached, and why it's trustworthy)
1. **Two independent signals, not one.** (a) Live authenticated probes of every vector from the
   browser session, each response body run through the repo's own leak regex; (b) the repo's
   `InputValidationApiTest` + `ErrorContractApiTest` under `-DSTRICT_INPUT_VALIDATION=true`
   (STRICT turns every soft finding into a hard fail). Both agree the leak/5xx defect is gone.
2. **Scoped the one STRICT red honestly.** The STRICT run is 8/9, and it would have been easy to
   report "still failing." But the single red is `testRequiredFieldAndEnumEnforcement` — a
   *different* concern (create endpoints accept invalid-but-well-formed payloads with 201), which
   produces no leak and no 5xx. I separated it into its own "adjacent gap" section rather than
   letting it muddy the fixed verdict. The tests that actually cover this ticket all pass.
3. **Auth reality.** The app uses HTTP-only-cookie auth, so live probes ran as in-page fetches on
   the app origin (a localhost-served evidence page can't call `/api`); the Java suite authenticates
   separately via `POST /auth/login` Bearer. The session expired mid-task and was re-logged-in.
4. **The trace_id path is the intended design.** One genuine-error case returns
   `{"error":"An internal error occurred.","trace_id":"073d3cd8…"}` — exactly the ticket's
   Expected-Result "bare error + correlation id, full trace server-side only."
5. **No side effects.** Every probe was crafted to fail before insert; verified sandbox issue
   residue = 0. PDF/PNG generated with headless Chrome (`--print-to-pdf` / `--screenshot`).
