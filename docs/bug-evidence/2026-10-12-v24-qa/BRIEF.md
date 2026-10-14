# Web v2.4 (Jira version 14280): deep QA brief for every tester

Owner instruction, 12 Oct 2026: "do sanity testing in depth, take longer time, check api, button, ui, what enhancement we can do,
tenant, all roles, flag enable/disable (some new flags are added), api checking is important, performance, and create tickets for
any new bug or suggestion (enhancement, filter etc.)". Read these two memory files first and apply them on every ticket:
- `~/.claude/projects/-Users-abhiyantsingh-Downloads-Egalvanic-Web-main/memory/feedback_release_deep_sanity_plan.md`
- `~/.claude/projects/-Users-abhiyantsingh-Downloads-Egalvanic-Web-main/memory/feedback_qa_miss_checklist.md`

## Environment (QA ONLY)
- App: https://acme.qa.egalvanic.ai. Build with the v2.4 code: `index-BdXGlAq5.js` (deployed 12 Oct 17:35 IST). Check the live build
  (`curl -sk https://acme.qa.egalvanic.ai/ | grep -o 'index-[^"]*\.js'`) at the start and before each verdict; name it in the verdict.
- Never use production, stage, dev or the demo tenant. Never Jira writes, never Google Chat, never git commit/push. Reading Jira with the
  Atlassian Rovo getJiraIssue tool is fine. The main session posts all Jira comments/status changes from the owner's account.
- Ticket text (description; no comments existed at 18:30 IST): `docs/bug-evidence/2026-10-12-v24-qa/tickets/<KEY>.md`.
  Re-read the live ticket with getJiraIssue (fields description, comment) before testing: comments can land any time.

## Seats (read at runtime from `src/main/java/com/egalvanic/qa/constants/AppConstants.java`; never print passwords)
`pick(name)` regex: `new RegExp('\\b'+name+'\\s*=\\s*getEnv\\("[^"]*",\\s*"([^"]*)"\\)')` (first match = the QA value).
- Staff seats (is_eg_admin, see every company; useless for tenancy tests): `VALID_EMAIL`/`VALID_PASSWORD` (Super Admin),
  `ADMINQA_EMAIL`/`ADMINQA_PASSWORD` (Admin).
- Customer seats: `PM_EMAIL`/`PM_PASSWORD` (Project Manager), `AM_EMAIL` (Account Manager), `EE_EMAIL` (Electrical Engineer),
  `FM_EMAIL` (Facility Manager), `CP_EMAIL` (Client Portal), `TECH_EMAIL` (Technician, no web access: expect "Web Access Restricted"),
  `PORTAL_SALES_EMAIL` (Portal Sales). Password constants follow the same name with `_PASSWORD`.
- Sign-in flow: email → "Use my password" → password → "Sign In" → click "Set up later" if the MFA prompt shows.
- QA has OTHER companies' data too (see memory `project_other_companies_on_qa_tenancy.md`): use it for cross-tenant checks from a
  customer seat, with a positive control (staff seat gets the same id as JSON through the same call).

## Browser runner (headed Chrome; never headless)
- `/Users/abhiyantsingh/Downloads/Egalvanic_Web-main/.playwright-mcp/runner/pwslot.sh <script.js> [timeoutSec]`
  runs `async (page) => { ... }` (Node `require` works) in its own headed Chrome and prints `RESULT <json>` as the last line.
  Write scripts under your own folder `.playwright-mcp/v24/<group>/`. Examples: `runner/boot.body.js`, `runner/prod231.js`, `runner/common.js`.
- Only 3 browser slots exist for ALL testers (`pwslot.sh` waits for a free one). Keep each run under ~8 minutes, batch a whole flow
  into one run, and do API probes inside the same run with `p.evaluate(fetch(...))` (cookie auth; no extra headers needed).
- Screenshots: `await p.bringToFront(); await p.screenshot({path, timeout:90000, animations:'disabled'})`.
- Gotchas: MUI selects open on mousedown; Escape closes MUI drawers (memory `project_mui_drawer_escape.md`); never click "first N
  checkboxes" or bare icon buttons blindly; test populated records, not empty states; wait for spinners/skeletons to clear.

## LaunchDarkly flags (ON and OFF on every gated feature; NEW flags in this release too)
- Find gates in the bundle: `Po("<name>")` / `useFeatureFlag` → LD key usually `feature-<name>` (also look for `sld-realtime`, etc.).
- Live values (no sign-in): `GET https://clientsdk.launchdarkly.com/sdk/evalx/6a34e78f93e3e00a6eef8fda/contexts/<base64url of
  {"kind":"organization","key":"d59d449b-09d8-45d6-8f0a-ef70024b1293","name":"Acme"}>` (QA client id + Acme QA company id).
- Simulate the other state by rewriting the evalx response in Playwright (`ctx.route('**/sdk/evalx/**', ...)`); label results
  "SIMULATED". Do NOT contact anyone. List every real flip needed (flag key, env QA, company Acme, target value, why) in the verdict;
  the main session asks Shubham in eg-internal-dev.

## Data rules
- Create test data only when needed and label it `QA-DEMO <KEY> (delete me)`. Never edit or delete records you did not create. No
  bulk destructive actions. API probes that only read are fine; write probes only on your own QA-DEMO records.

## Deliverables per ticket (folder `docs/bug-evidence/2026-10-12-v24-qa/<key-lowercase>/`)
1. Screenshots `QA-12Oct-<KEY>-<n>-<what>.png`: real app pixels; at least 2 per ticket; each one named/captioned PASS or FAIL.
2. `verdict.md` with:
   - **Verdict:** PASS / FAIL / PARTIAL / BLOCKED / NOT WEB-TESTABLE (backend-only checked via API) and the build.
   - **Checked** table (check · how · result) covering: UI + every button/menu/filter the ticket touches, API contract, tenant/foreign-id
     scope, roles (all relevant seats + nav vs route agreement), flags ON/OFF (real or simulated), performance (requests per load,
     duplicates, slow calls > 2 s, payload size), the description's own "How to test" rows.
   - **Bugs found:** each with a plain title, numbered steps, actual vs expected, severity, evidence filename, suggested assignee
     (Eric Ehlert owns these tickets; Avani = backend, Krunal = frontend), and a check that no existing Jira ticket covers it.
   - **Enhancement suggestions:** concrete, user-facing (missing filter/search/sort/bulk action/export/empty state/label), one line each + why.
   - **Mobile part for Nency:** what the iOS/Android side of this ticket needs her to check.
   - **Not covered** and why.
   - **Proposed Jira comment** in Jira wiki markup (`h3.`, `*bold*`, `#` lists, `||h||`/`|c|` tables, `{code}`), plain words, short,
     with `!QA-12Oct-<KEY>-<n>-<what>.png|width=720!` image lines.
   - **Recommended status:** READY TO RELEASE (pass) / To Do (100% sure fail) / stay In QA (partial, blocked, needs a flag flip).
3. Final message back to the main session: max ~40 lines: per ticket verdict + bugs + suggestions + flags needed + Nency notes.
