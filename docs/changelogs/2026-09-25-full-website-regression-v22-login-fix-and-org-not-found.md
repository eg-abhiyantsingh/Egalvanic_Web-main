# Full-website regression on Web v2.2: login fix, framework hardening, one new defect (2026-09-25)

**Prompts:** "test full website too", then "for faster testing use multiple browser".

## What was done
1. **The Selenium suites could not sign in to v2.2.** The login page now asks for the email first and shows the
   password box only after "Use my password". `LoginPage.revealPasswordFieldIfNeeded()` presses it (waits up to 45 s,
   because the sign-in-methods lookup took 8.7 s under load). Eleven test classes that still waited on `id="email"`
   now use the shared `LoginPage.EMAIL_INPUT` / `PASSWORD_INPUT` / `isLoginScreen()`.
2. **Ran the full regression**: all three CI suites (Core, Suite 2 with every optional group, API Health) plus local
   headed runs, several browsers at once (`parallel="tests"`, 2–3 browsers — the repo's measured limit on this Mac).
3. **Triaged every failure** by screenshot, bundle code and live probes. Stale tests fixed:
   - two-factor chooser ("Set up two-factor authentication") opens after the shell mounts and hid pages → wait for it,
     dismiss before every test (`dismissMfaPromptIfShowing()`), recognise the new heading;
   - silent signed-out continuation → `waitUntilSignedIn()` + retry in `BaseTest.loginAndSelectSite()`;
   - Customers menu gate is `accounts.view OR features.accounts.view` (gating test knew one);
   - FM / AM land on `/pm-readiness` (no Site Overview permission) — by design;
   - CriticalPath clear-search read "10134" from the Arc Flash Readiness card, not the grid;
   - Authentication tests rewritten for the two-step page; site search matches site **or account** name (ZP-3978).
4. **Framework: a lost browser no longer cascades.** One Chrome died in a 3-browser run and 20 tests failed in 0 s.
   `BaseTest.replaceBrowserIfLost()` swaps a new Chrome into the same `SelfHealingDriver`
   (`replaceDelegate()`), signs in again, and carries on. Proven by `BrowserLossRecoverySelfTest` (in no suite).
5. **New defect (Medium):** a failed or busy company-settings request (`/api/company/alliance-config/acme.egalvanic`)
   makes the real ACME address say "Organization Not Found — check the URL", with no retry. Reproduced with a normal
   control and an unknown-company control; it also happened on its own on QA at 10:06 UTC. Covered by
   `OrgLookupResilienceTest` (fails until fixed). Added to the QA Defect Register (v26). Not ticketed — owner's call.

## Results (validated)
| Run | Before | After |
|---|---|---|
| smoke-auth (6 roles + wrong password) | 0/6 | 6/6 |
| Auth + Site (local, 2 browsers) | 45/59 on CI | 56/59 → remaining 2 stale fixed (4/4 re-run); 1 = ZP-2025 rate limit (real, known) |
| RBAC UI gating (CI) | 5/6 | 6/6 |
| RBAC login E2E (CI) | 2/8 | 6/8 → landing rule fixed |
| API Health (CI) | 1955 run, 2 fail | both known: report history 500, planned WO line list never answers |

## Depth: why these fixes are the right ones
- **Why press "Use my password" instead of typing into a hidden field?** The password input does not exist in the DOM
  until the user chooses that method. Scripting a value into it would test nothing a user can do.
- **Why a 3 s "stay clear" window for the two-factor chooser?** The chooser is opened by a later effect after the shell
  mounts. Returning the moment `<main>` exists raced it; requiring the shell to stay prompt-free for 3 s removes the race
  without slowing a normal login much.
- **Why swap the delegate instead of creating a new driver?** Test classes cache the driver in page objects, a
  `JavascriptExecutor`, and helpers. Replacing the object inside `SelfHealingDriver` keeps every reference valid.
- **Why is "Organization Not Found" a product bug and not load noise?** The code maps *any* failure to the not-found
  code. The server does send `COMPANY_NOT_FOUND` for a real unknown company, so the client can tell the two apart and
  should show a retryable "can't reach the server" message for everything else.
- **Trap met:** VS Code's Java extension rebuilt `target/` with a broken class ("Unresolved compilation problems") and
  Maven reported BUILD SUCCESS without compiling. Fix: delete the class files and let javac rebuild; scan `target/` for
  "Unresolved compilation" before trusting a run.
