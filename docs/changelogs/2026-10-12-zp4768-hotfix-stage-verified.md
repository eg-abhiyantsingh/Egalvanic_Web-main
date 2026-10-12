# ZP-4768 (Web v2.3.1 hotfix) — verified FIXED on stage

- **Date:** 12 Oct 2026, 14:00–14:35 IST. **Env:** acme.stage.egalvanic.ai, build `index-Dnmq_0Vh.js` (hotfix on stage 13:55 IST; 276/302 chunks changed vs the prior stage build).
- **Flow:** Ready for QA → In QA (Start Testing) → **READY TO RELEASE** (QA-Complete). Comment **45078** as abhiyant, 5 screenshots.

## Result: PASS (both defects fixed)
- **Defect 1 (two emails / empty password):** creating an Admin user sends exactly ONE "Welcome to Z Platform - Your Login Credentials" email with a populated PASSWORD. Confirmed by IMAP (1 message to the new +alias) and the app's "User Created" dialog ("A welcome email … has been sent", singular). Both `Copy me` and `Send a temporary password` default OFF.
- **Defect 2 (blank page after sign-in):** new Admin lands on `/dashboard` → mandatory two-factor setup (usable page), not a blank `/login`.
- **Redirect regression (old ZP-4798):** sign out (→`/login`) → sign in again → populated `/dashboard`. Fixed.
- **Role spot-check (AC5):** Technician → one email w/ password, sign-in shows a clear "Web Access Restricted" message (field/mobile role), not a blank shell.
- **AC4** one user record, correct role (1 of 1). **AC8** console clean except pre-existing Beamer CSP font warnings. **AC11** existing admin signs in normally.
- **Not covered:** AC10 prod/BCES-IQ (prod still pre-fix; re-check after release); AC7 forced failure; AC5 AM/EE/PM (shared code path, Admin+Technician pass). A separate DevRev session-token `400` is in the console → ZP-4776 / ZP-4767, not this bug.

## Test data (stage, QA-DEMO, delete-me)
- Admin user: `abhiyant.singh+zp4768smv50gvuh@egalvanic.com` (role Admin), created 08:50 UTC.
- Technician user: `abhiyant.singh+zp4768tmv50nd9p@egalvanic.com` (role Technician), created 08:55 UTC.
- Both in Platform Users on stage. Passwords were throwaway; not retained.

## Evidence
`docs/bug-evidence/2026-10-12-zp4768-stage/` (ZP4768-1..5 + raw b/c/d/e frames).

## Other v2.3.1 tickets (status at 14:35 IST)
- ZP-4767 (Z icon), ZP-4776 (session token), ZP-4673 (double me/devrev calls): all still **To Do** (dev not finished). On stage the Z launcher IS present, but a DevRev `/session-token` 400 still fires — consistent with 4776/4767 not yet fixed. Nothing to verdict until dev moves them to Ready for QA.
