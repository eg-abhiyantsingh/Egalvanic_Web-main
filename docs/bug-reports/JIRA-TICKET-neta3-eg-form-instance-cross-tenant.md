# [Web] NETA-3 Forms: a user of one company can read another company's EG form data (cross-tenant leak)

**Severity:** Critical · **Priority:** High
**Environment:** QA (Project Z), V1.36 · found live on `acme.qa.egalvanic.ai` + `demo.qa.egalvanic.ai`
**Backend PR:** eg-pz-backend #1011 (neta-3 — the new `eg-form-instance/by-session` endpoints)

---

## What's wrong (one line)

The NETA-3 `eg-form-instance/by-session` endpoints authenticate the token but **don't check the token's company** — so a logged-in user of Company B can read (and, via a tampered request, tag and export) Company A's EG form instances, including the filled-in answers.

## Impact

A real, ordinary logged-in user of one tenant can pull another tenant's inspection data — technician names, emails, dates, verdicts — for any work order. No admin rights, no special tooling; it happens over normal authenticated HTTP.

## Steps to reproduce (read leak — the serious one)

1. Log in as a **Company B** user (used: `demo.qa.egalvanic.ai`, `shubham.goswami@egalvanic.com`).
2. Call, **on Company B's own host, with Company B's own token**, a Company A session id:
   `GET https://demo.qa.egalvanic.ai/api/eg-form-instance/by-session/fcc37c67-01fc-4880-87f3-8028fc86e97a`
   (`fcc37c67-…` is an **acme** work order.)
3. **Actual:** HTTP 200 + JSON — all 8 of acme's form instances, each with its real `form_submission`, e.g.
   `{"technician_name":"...","technician_email":"...","inspection_date":"2026-08-04", ...}`.
   **Expected:** rejected (4xx / permission_denied), because the caller is not in that company.

**It's self-discovering, not "you had to know the id":** as the Company B user,
`GET /api/ir_session?limit=50` returns **1734 sessions including Company A's** — the list hands you the ids to feed into step 2.

## The write side-effects (lower urgency — request-tamper only)

With the Company B token pointed at **Company A's host** (`acme.qa.egalvanic.ai`), these also succeed and have real effects:
- `PUT …/eg-form-instance/by-session/{acmeSession}/bulk-tags {add_tags:[...]}` → tag persists on all 8 acme instances (confirmed with acme's own token).
- `POST …/eg-form-instance/by-session/{acmeSession}/export {output_format:"pdf"}` → a real reporting job starts for acme's forms (`execution_arn` returned).

Note these two **only** work when the request is aimed at the *other tenant's host* — on the caller's own host they correctly return the masked-404. A normal browser only talks to its own host, so the writes are **request-tamper**, not something a normal user hits by accident. The **read leak is the priority** because it reproduces on the user's own host.

## Root cause (for the dev)

Same per-route (not global) tenant-guard gap we saw in the Aug-14 cross-tenant P1: the `/company/{id}/*` routes got fixed, but the `/api/<resource>/by-<scope>/{id}` family was left ungated. The ticket's own QA section even calls tenant gating "the important negative test" and says company must be resolved from `X-Subdomain` and fail closed, the session's SLD must belong to the caller's company, and every instance id must resolve to that session — **on the read path none of that is enforced.** Add the same company check the `/company/{id}/*` handlers use to `eg-form-instance/by-session/*` (GET, export, bulk-tags, bulk-patch — audit the whole family).

## Evidence
Independently reproduced twice (adversarial API panel + hand re-verification), demo→acme, with an own-tenant positive control. Masked-404 (200 + SPA HTML) responses were treated as rejections, never leaks — every FAIL above is a 200 + JSON body carrying acme data or a verified persisted side-effect.
