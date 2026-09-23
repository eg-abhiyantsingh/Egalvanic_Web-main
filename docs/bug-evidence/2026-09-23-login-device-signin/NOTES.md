# ZP-4353 — sign-in screen leads with device sign-in (2026-09-23, evening)

Reported by the owner from a screen recording (`2026-09-23 18-37-31.mov`) I could not open — reproduced
independently on the live QA login screen instead.

Bundle: **`index-vDJBGpU_.js`** (FOURTH rebuild of 23 Sep: CPjC9Hwo → CH4p1H5S → 6NT43E1C → vDJBGpU_).
Logged-out, fresh isolated browser context, no passkey ever registered on this machine.

## What changed under us
`GET /api/auth/v4/methods?subdomain=acme`
- 12:38 → `["password"]`
- 12:55 → `["google","email_code","password"]`
- 18:4x → **`["passkey","google","password"]`** — `email_code` dropped, `passkey` added.

So the "Email me a code" button that existed this afternoon is gone, and the email field's primary action
is now **"Sign in with device sign-in"**.

## Reproduction
1. `/login`, type an address → primary button reads **Sign in with device sign-in** (01).
2. Press it → **"Waiting for your device… / Follow the prompt on your device to finish signing in."** (02).
3. Nothing can answer (no passkey on this machine) → back to the sign-in screen with
   **"We couldn't use that sign-in method. Choose another way to continue."** and the
   **Email Address box emptied** (`input.value === ""`) (03).
4. `POST /api/auth/v4/passkey/start` → **401** `passwordless_start_failed` —
   "We couldn't sign you in with this device. Try another way."

Catch-22 in the screen's own copy: "New to this? Passkeys are set up in Security settings after you sign in."

Only `Use my password` actually completes on this tenant; `Continue with Google` still answers
`google_not_configured`. Filed as **ZP-4353** (Bug, High, avani.patel, Web v2.2, sprint Z-26-09-S2, To Do).
