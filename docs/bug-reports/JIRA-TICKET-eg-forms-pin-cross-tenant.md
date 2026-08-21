# [Web] EG Forms pinning: a user of one company can pin another company's form instances

**Severity:** High · **Priority:** High
**Environment:** QA (Project Z), V1.36 · `acme.qa.egalvanic.ai` + `demo.qa.egalvanic.ai`
**Backend PR:** eg-pz-backend #989 (the new `/eg-form-instance/{id}/pin` route)

---

## What's wrong (one line)
The new pin route doesn't check the caller's company, so a logged-in Company B user can pin (and unpin) Company A's EG form instances — the exact negative case the ticket says must return a 404.

## Steps to reproduce
1. Log in as a **Company B** user (used: `demo.qa.egalvanic.ai`, `shubham.goswami@egalvanic.com`, company `93611164`).
2. Take two of **Company A's** form-instance ids (both acme, company `d59d449b`) — a primary and a same-template pinnee.
3. With Company B's token, call **on Company A's host**:
   `POST https://acme.qa.egalvanic.ai/api/eg-form-instance/{acmePrimary}/pin` body `{"pinned_instance_id": "{acmePinnee}"}`
4. **Actual:** HTTP **200**, and the pin persists — reading the primary back with Company A's own token shows the cross-tenant pin, with Company A's real form data.
   **Expected:** rejected (the ticket says **404**, not a successful pin). The PR states scoping should resolve `session_id → Session.sld_id → SLD.company_id` and block cross-company pins.

## Severity note (so it's not over-stated)
This only succeeds when the request is aimed at the **other tenant's host**. On the caller's own host (`demo.qa.egalvanic.ai`) the same call returns the masked-404, so a normal Company B user's browser — which only talks to its own host — doesn't hit this by clicking. It's a **request-tamper** cross-tenant write, not a normal-user path. Filed High (not Critical) for that reason, but it's a real violation of the ticket's own security requirement and it writes durable cross-tenant state.

## Root cause (for the dev)
Same per-route gap as the NETA-3 `eg-form-instance/by-session` finding: the `eg-form-instance` endpoints authenticate the token but don't bind the caller's company on the acme host. Add the session→sld→company check against the **caller's** token to `/pin` (and audit `/unpin`, `by-session`, export, bulk-tags, bulk-patch together), and return 404 on mismatch as intended.

## Evidence
Reproduced live demo→acme with distinct companies confirmed via `/auth/me` (acme `d59d449b`, demo `93611164`); persistence verified with acme's own token; masked-404 responses treated as rejections, never as the leak. Probe pin cleaned up (unpinned) afterward.
