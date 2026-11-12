# Web v2.6 (Jira version 14299): deep QA brief for every tester (12 Nov 2026)

Owner, 12 Nov 2026: "we can start testing now ready to qa and in qa ticket in deepth sanity testing".
Before anything else, read these memory files (`~/.claude/projects/-Users-abhiyantsingh-Downloads-Egalvanic-Web-main/memory/`):
- `feedback_release_deep_sanity_plan.md` (what depth means: UI + every button, API, tenant, ALL roles, flags ON/OFF, performance,
  enhancements)
- `feedback_qa_miss_checklist.md` (real past misses; run it before every verdict)
- `feedback_test_the_intended_path_first.md`, `feedback_dont_overreport_sld_bugs.md`, `feedback_positive_control_before_outage_claim.md`
- `feedback_video_first_short_tickets.md` + `feedback_web_tickets_add_video_with_steps.md` (every ticket gets a short step video)
plus the module memories named in your task.

## Environment: QA ONLY
- App https://acme.qa.egalvanic.ai. The v2.6 code landed on QA today: build **`index-C1ZRIunF.js`** (14:06 IST 12 Nov).
  Check the live build at the start and right before each verdict:
  `curl -sk https://acme.qa.egalvanic.ai/ | grep -o 'index-[^"]*\.js'` and name it in the verdict. If it changes mid-run, say so.
- On 2 Nov (comments ~45864–45901) and 5 Nov (comments 46035–46056) QA commented on the 22 Eric tickets that the code was **not on
  QA yet**. That is no longer true: test everything fresh on today's build. Earlier evidence:
  `docs/bug-evidence/2026-11-02-rfq-web-w1..w3/`, `docs/bug-evidence/2026-11-05-rfq*/`.
- Never use production, stage, dev or the demo tenant. **No Jira writes, no Google Chat, no git commit/push, no LaunchDarkly changes,
  no messages to anyone.** Reading Jira with the Atlassian Rovo getJiraIssue tool is fine (fields description + comment): read the
  description AND EVERY comment (dev notes = test rows) right before testing each ticket.

## Seats (read at runtime from `src/main/java/com/egalvanic/qa/constants/AppConstants.java`; never print or log passwords/emails)
Helper: `.playwright-mcp/v24/assets-egforms/lib.js` (`L.pick(name)`, `L.SEATS`, `L.seat(page, tag)` signs in and returns helpers incl.
`api()` for in-page fetch with cookie auth). Seat tags: SA (staff Super Admin, is_eg_admin), ADMIN (staff Admin), PM, AM, EE, FM, CP,
TECH (no web access → "Web Access Restricted"), PS (Portal Sales). Staff seats see every company: useless for tenancy checks.
QA holds other companies' data (memory `project_other_companies_on_qa_tenancy.md`): use it for cross-tenant checks from a customer
seat, with a positive control (an authorised seat gets the same id as JSON through the same call).
Sign-in: email → "Use my password" → password → "Sign In" → "Set up later" on the MFA prompt.

## Browser runner (headed Chrome; never headless)
- `.playwright-mcp/runner/pwslot.sh <script.js> [timeoutSec]` runs `async (page) => {...}` in its own headed Chrome; prints
  `RESULT <json>` last. Write scripts under `.playwright-mcp/v26/<group>/`.
- Only 3 browser slots for ALL testers (pwslot waits). Keep each run under ~8 min; batch a whole flow per run; do API probes inside
  the run with `p.evaluate(fetch('/api/...'))`.
- **Video per ticket (required):** `.playwright-mcp/v25/video/vid.js` (read its header). `V.start(page,{seat:'SA',outDir})`,
  `v.card()`, `v.step()`, `v.hl()`, `v.finish('QA-12Nov-<KEY>-video-steps.mp4')`. 30–120 s, the ticket's steps in order with captions.
  Check every video with a contact sheet before handing it over:
  `ffmpeg -i x.mp4 -vf "fps=1/3,scale=480:-2,tile=5x3" -frames:v 1 sheet.jpg` and look at it.
- Gotchas: MUI selects open on mousedown; Escape closes MUI drawers; never click "first N" checkboxes or bare icon buttons blindly;
  test populated records, not empty states; wait for spinners/skeletons to clear; use exact role/name locators
  (`getByRole('combobox',{name:'…'})`, not getByLabel, which also matches the listbox).

## LaunchDarkly flags (ON and OFF on every gated feature; NEW flags in this release too)
- Find gates in the bundle (`Po("<name>")`/`useFeatureFlag`; LD key usually `feature-<name>`). Live values, no sign-in:
  `GET https://clientsdk.launchdarkly.com/sdk/evalx/6a34e78f93e3e00a6eef8fda/contexts/<base64url of
  {"kind":"organization","key":"d59d449b-09d8-45d6-8f0a-ef70024b1293","name":"Acme"}>`.
- Simulate the other state by rewriting the evalx response in Playwright (`ctx.route('**/sdk/evalx/**', ...)`), label the result
  SIMULATED. List every real flip needed (flag key, env QA, company Acme, ON/OFF, why); the main session asks Shubham.

## Data rules
- Create test data only when needed; name it `QA-DEMO <KEY> 12 Nov …` (no "delete me" suffix, no clean-up needed on QA).
- Never edit or delete records you did not create. No bulk destructive actions on shared data. Read-only API probes are fine; write
  probes only on your own QA-DEMO records. An action that cannot be undone and hits shared data (for example forking a GLOBAL service,
  which moves open work orders once): do it on QA-DEMO data you created, or describe it as not covered.

## Deliverables per ticket: folder `docs/bug-evidence/2026-11-12-v26-qa/<KEY>/`
1. `QA-12Nov-<KEY>-video-steps.mp4` (checked with a contact sheet).
2. 3–6 screenshots `QA-12Nov-<KEY>-<n>-<what>.png` (real app pixels; each one clearly expected (PASS) or defect (FAIL)).
3. `verdict.md`:
   - **Verdict:** PASS / FAIL / PARTLY / BLOCKED / NOT WEB-TESTABLE (backend-only, checked via API) + build.
   - **Checked** table (check · how · result): UI + every button/menu/filter the ticket touches, the description's "QA Review"/
     "How to test" rows one by one, API contract, tenant/foreign-id scope, roles (relevant seats; nav vs typed route vs API agree),
     flags ON/OFF (real or SIMULATED), performance (requests per load, duplicates, calls > 2 s, payload size).
   - **Bugs found:** plain title, numbered steps, actual vs expected, severity, evidence file, suggested assignee (Eric owns these
     tickets; for new bugs Avani = backend, Krunal = frontend; never Dharmesh), and the JQL you ran to make sure no ticket covers it.
     Re-prove each bug once more before listing it (same input, same seat).
   - **Enhancement suggestions:** concrete and user-facing, one line + why.
   - **Mobile impact:** what an iOS/Android tester should check, if anything.
   - **Not covered** and why.
   - **Proposed Jira comment** (`<KEY>.txt`, Jira WIKI markup, SHORT): line 1 = `*Result: PASS|PARTLY|FAIL on QA* (Web v2.6, build
     index-…, 12 Nov 2026)`; line 2 = `!QA-12Nov-<KEY>-video-steps.mp4|width=640!` + an italic one-line caption; then at most 5–8 short
     bullets (passed / failed / blocked / one question); then the screenshots `!QA-12Nov-<KEY>-<n>-<what>.png|width=640!` each with an
     italic caption line. Escape literal `{ } [ ]` with a backslash; never start a line inside a list with `-`; no `{{--x}}`.
   - **Recommended status:** READY TO RELEASE (pass, incl. pass with separate new bugs) / To Do (main purpose 100% fails) / stay In
     QA (blocked: needs a flag, a seat or data; say exactly what).
4. Final message back (max ~40 lines): per ticket verdict, bugs, suggestions, flags needed, blockers.
