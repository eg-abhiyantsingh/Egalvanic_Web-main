# ZP-4640 check, ZP-4798 blank page after signing back in, Dharmesh's production findings

- **Date:** 10 Oct 2026, 11:00–15:05 IST (loop ticks + owner requests)

## ZP-4640: Google sign-in direct from our domain
- Dharmesh moved it from Ready for QA to Done at 10:55 IST without QA. I checked what is possible without signing in, on QA and PROD at 11:05 IST. All passed:
  - the button calls `/api/auth/v4/google/authorize-url` and goes straight to accounts.google.com, with no Cognito Hosted UI;
  - Google shows "Sign in to continue to egalvanic.ai";
  - the callback with a dummy state returns 400 JSON `google_failed`.
- Jira comment 45069 with screenshots. Not checked: a real Google sign-in, 2FA skip, mobile, BCES-IQ.

## ZP-4798 (new bug, owner approved): blank page after signing back in
- Steps: sign in → sign out (lands on `/login`) → sign in again. The page stays on `/login` with an empty main area, even after a reload. Signing in from `/` works.
- Reproduced on QA at 11:10 IST and on PROD at 14:59 IST (PM seat, read-only).
- Ticket: High, assigned to Dharmesh, sprint Z-26-10-S1, To Do. Comments 45072 (QA) and 45073 (PROD), 6 screenshots.

## Dharmesh's "Z icon missing"
- QA and PROD (PM seat): the DevRev launcher shows after sign-in and after a reload. The app_id is identical in both bundles. So it isn't global; it's likely tied to his account or session.

## Messages (owner approved)
- Reply posted in eg-internal-dev and in Dharmesh's personal DM, with the ZP-4798 link.

## Evidence
- `docs/bug-evidence/2026-10-10-v23-prod/zp4640/`
- `docs/bug-evidence/2026-10-10-blank-after-sign-in-qa/`
- `docs/bug-evidence/2026-10-10-z-icon-qa/`
- `docs/bug-evidence/2026-10-10-v23-prod/zp4798-and-z-icon/`
