# QaDriverDaemon — a signed-in browser driven by command files

**Date/Time:** 2026-10-05 14:20 IST
**Prompt summary:** "test all ready to qa ticket" for Jira version 14236 (Web v2.3). The browser extension's QA tab had been
signed out, and passwords are never typed into forms by hand. So the round needed a way to click through the real web app in a
signed-in session using the framework's own login.

## Code changes
| File | Change | Why |
|---|---|---|
| `src/test/java/com/egalvanic/qa/testcase/QaDriverDaemon.java` | New TestNG test (extends `BaseTest`) | One headed Chrome session that signs in once and then runs commands from a folder |
| `src/test/java/com/egalvanic/qa/testcase/BaseTest.java` | `-Dqa.unhandledPrompt=ignore` option in `startChrome()` | Keeps a native `confirm()` open so a test can read and accept it |

## How it works
1. `BaseTest.classSetup()` does what it does for every suite: it starts visible Chrome (never headless), accepts the QA host's
   internal certificate, signs in with `-DUSER_EMAIL` / `-DUSER_PASSWORD`, handles the 2FA enrolment prompt and selects the
   site. The seat's password is passed as a JVM property from a local file outside the repo, so it is never typed into a
   page and never committed.
2. `serve()` writes a `READY` file, then polls `<dir>/in` every 250 ms. A command is one file named `<sequence>.<type>`:
   - `js`: the body of an async function run in the page; its return value comes back as JSON. Most API checks run here,
     because `fetch` in the page uses the user's real session and headers.
   - `get`: open a path, then wait until the app has rendered and dismiss the 2FA prompt if it shows.
   - `shot`: save a screenshot of what the user sees.
   - `click` / `keys`: a real WebDriver click or real key presses. React form fields need real events, so values are never
     set through JavaScript.
   - `alert`: accept or dismiss an open `confirm()`/`alert()` and return its text.
   - `stop`: end the session.
3. Each result lands in `<dir>/out/<sequence>.txt` with the time taken and the page URL, and the command file moves to
   `done/`. That leaves a full audit trail of every action in the session.

The helper scripts (`q.sh`, `qj.sh`, `start-daemon.sh`, `prelude.js`) live in the session scratchpad, not the repo. `qj.sh`
prepends small helpers (`api()`, `byText()`, `dlg()`, `waitFor()`) to each JS command.

## Design decisions and trade-offs
- **Why extend `BaseTest` instead of writing a new login?** The login has many traps: the email-first form, the 2FA chooser,
  the update alert, the TLS interstitial, the site picker. `BaseTest` already handles them, and a second copy would drift.
- **Why files instead of a socket or HTTP server?** No port, no auth surface, nothing to clean up. Each command and result is
  a plain file you can read afterwards. The cost is 250 ms of polling latency, which doesn't matter for manual testing.
- **Why the `click` result skips reading the URL:** right after a click a native `confirm()` may be open, and *any* further
  WebDriver call (even `getCurrentUrl`) would dismiss it under Selenium's default behaviour. The ZP-4454 asset-class delete
  confirmation is exactly this case.
- **Why `qa.unhandledPrompt` is opt-in:** existing suites rely on Selenium dismissing stray dialogs. Changing the default would
  turn unexpected alerts into hard failures across the whole regression run.
- **Long work runs inside the page.** The tool layer limits a call to about 120 s. A long job (the 110-preview sweep, a
  156 s extraction) starts as an in-page async task stored on `window.__job` and is polled by later commands.
- **Two sessions side by side** (admin + a non-staff Project Manager) let one ticket be checked as the user who owns the data
  and as a customer who must be refused. The admin seat is Egalvanic staff, so its own tenancy results don't count.

## Learning notes (for the manager)
- The daemon doesn't decide anything. It runs exactly the commands it is given, one file each, and records them, so a
  reviewer can replay a ticket's steps from the `done/` and `out/` folders.
- The same approach can drive any role: start it with another seat's `-DUSER_EMAIL` to see the app as that role. That is how
  the ZP-4148 (Portal Sales) and other-company checks were done.
- Things that broke along the way and their fixes: React replaces custom element ids on re-render (use React's own ids or a
  `data-qa` attribute); a background job limit killed the first session (start it detached with `nohup`); restarting the
  session cleared its screenshot folder (the start script now keeps `shots/`).
