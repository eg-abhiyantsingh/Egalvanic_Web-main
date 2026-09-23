# ZP-4353 filed — sign-in screen leads with device sign-in (2026-09-23, evening)

**Prompt:** "create a bug and assign a bug avani. sign in with device when we are cliking on email log in"
(with a screen recording `2026-09-23 18-37-31.mov`).

**I could not open the .mov** — only the filename reached me — so I reproduced the report myself on the live QA
login screen and filed from my own evidence rather than from a description I could not verify.

Ticket: <https://egalvanic.atlassian.net/browse/ZP-4353> — Bug · **High** · **avani.patel** · Web v2.2 ·
sprint Z-26-09-S2 · Backlog → **To Do** · 3 screenshots attached.
Evidence: `docs/bug-evidence/2026-09-23-login-device-signin/`.

## What the owner saw, confirmed

QA rebuilt a **fourth** time today — `index-vDJBGpU_.js` — and acme's advertised sign-in methods changed again:

| Time | `GET /api/auth/v4/methods?subdomain=acme` |
|---|---|
| 12:38 | `["password"]` |
| 12:55 | `["google","email_code","password"]` |
| 18:4x | **`["passkey","google","password"]`** |

`email_code` was dropped and `passkey` added. So the **"Email me a code"** button is gone, and the email field's
primary action is now **"Sign in with device sign-in"** — which is exactly what the recording showed.

## Reproduction (logged out, fresh browser, no passkey)

1. `/login` → type an address → the main button reads **Sign in with device sign-in**.
2. Press it → **"Waiting for your device… / Follow the prompt on your device to finish signing in."**
3. Nothing can answer it → back to the sign-in screen with **"We couldn't use that sign-in method. Choose another
   way to continue."** and the **Email Address box emptied** (`input.value === ""`).
4. `POST /api/auth/v4/passkey/start` → **401** `passwordless_start_failed`.

The screen's own help text is the catch: *"New to this? Passkeys are set up in Security settings after you sign
in"* — you must sign in to create the passkey, and signing in is what the button is trying to do. Only **Use my
password** completes on this tenant; Continue with Google still answers `google_not_configured`.

## Note on the first attempt

`createJiraIssue` did not apply `additional_fields` — the block landed in the description as literal text and the
issue was created unassigned with no priority/fixVersion/sprint. Fixed with a follow-up `editJiraIssue`, which set
all four. Also: this Jira renders **Markdown**, not wiki markup — `h3.` showed literally and `#` became an H1, so
the description was rewritten in Markdown.
