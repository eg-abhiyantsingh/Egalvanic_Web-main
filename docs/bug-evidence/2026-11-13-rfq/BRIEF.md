# Ready-for-QA round, 13 Nov 2026: deep QA brief for every tester

Owner, 13 Nov 2026: "check eric more ticket are come in ready to qa status" → "yes" (start deep testing).
Scope:
- Eric Ehlert's 11 tickets, moved Resolved (migrated) → Ready for QA by Avani at ~11:17 IST 13 Nov.
- Already moved to **In QA** by the main session. Do NOT change any status.
- None has a fix version; all are in sprint Z-26-11-S1.

Before anything else, read these memory files (`~/.claude/projects/-Users-abhiyantsingh-Downloads-Egalvanic-Web-main/memory/`):
- `feedback_release_deep_sanity_plan.md`: depth means UI + every button, API, tenant, ALL roles, flags ON/OFF, performance, enhancements.
- `feedback_qa_miss_checklist.md`: real past misses; run it before every verdict.
- `feedback_test_the_intended_path_first.md`, `feedback_positive_control_before_outage_claim.md`, `feedback_dont_overreport_sld_bugs.md`.
- `feedback_ignore_dev_only_deploy_notes.md`:
  - Several tickets say "dev only / not in QA branch". IGNORE that wording and test on QA.
  - If the feature is truly absent, that shows in the test itself. Fingerprint it: a UI string, an API field or a computed value only the new code produces.
- `feedback_video_first_short_tickets.md` + `feedback_web_tickets_add_video_with_steps.md`: every ticket gets a short step video.
- The module memories named in your task.

## Environment: QA ONLY
- App: https://acme.qa.egalvanic.ai.
- New QA web build since last night: **`index-CDvWXKDj.js`** (last-modified 12 Nov 18:51 GMT = 00:21 IST 13 Nov).
- Check the live build at the start and right before each verdict, and name it in the verdict:
  `curl -sk https://acme.qa.egalvanic.ai/ | grep -o 'index-[^"]*\.js'`
  If it changes mid-run, say so.
- Never use production, stage, dev or the demo tenant.
- **No Jira writes, no Google Chat, no git commit/push, no LaunchDarkly changes, no messages to anyone.**
- Reading Jira with the Atlassian Rovo getJiraIssue tool is fine (cloudId egalvanic.atlassian.net, fields description + comment).
  Re-read the description AND EVERY comment right before testing each ticket; dev notes become test rows.

## Seats
Read seats at runtime from `src/main/java/com/egalvanic/qa/constants/AppConstants.java`. Never print or log passwords or emails.
- Helper: `.playwright-mcp/v24/assets-egforms/lib.js`.
  - `L.pick(name)` and `L.SEATS`.
  - `L.seat(page, tag)` returns helpers; call `await s.signIn()`, then `s.api()` does an in-page fetch with cookie auth.
- Seat tags:
  - SA (staff Super Admin, is_eg_admin) and ADMIN (staff Admin). Staff seats see every company, so they're useless for tenancy checks.
  - PM, AM, EE, FM, CP.
  - TECH: no web access ("Web Access Restricted").
  - PS: Portal Sales.
- QA holds other companies' data (`project_other_companies_on_qa_tenancy.md`).
  Use it for cross-tenant checks from a customer seat, with a positive control: an authorised seat gets the same id as JSON through the same call.

## Browser runner (headed Chrome; never headless)
- `.playwright-mcp/runner/pwslot.sh <script.js> [timeoutSec]` runs `async (page) => {...}` in its own headed Chrome and prints `RESULT <json>` last.
  Write scripts under `.playwright-mcp/rfq13/<group>/`.
- Only 3 browser slots for ALL testers (pwslot waits). Keep each run under ~8 min and batch a whole flow per run.
  Do API probes inside the run with `page.evaluate(() => fetch('/api/...'))` (send header `X-Subdomain: acme` if needed).
- **Video per ticket (required):** `.playwright-mcp/v25/video/vid.js` (read its header first).
  - Calls: `V.start(page,{seat:'SA',outDir})`, `v.card()`, `v.step()`, `v.hl()`, `v.finish('QA-13Nov-<KEY>-video-steps.mp4')`.
  - 30–120 s: the ticket's steps in order, with captions.
  - Check every video with a contact sheet and look at it:
    `ffmpeg -i x.mp4 -vf "fps=1/3,scale=480:-2,tile=5x3" -frames:v 1 sheet.jpg`
- Gotchas:
  - MUI selects open on mousedown; Escape closes MUI drawers.
  - Never click "first N" checkboxes or bare icon buttons blindly.
  - Test populated records, not empty states. Wait for spinners and skeletons to clear.
  - Use exact role/name locators.
  - Don't open the report-config editor in a slow loop (heavy auto-preview).

## LaunchDarkly flags (ON and OFF on every gated feature, including NEW flags)
- Find gates in the bundle (`Po("<name>")`/`useFeatureFlag`; the LD key is usually `feature-<name>`).
- Live values, no sign-in:
  `GET https://clientsdk.launchdarkly.com/sdk/evalx/6a34e78f93e3e00a6eef8fda/contexts/<base64url of {"kind":"organization","key":"d59d449b-09d8-45d6-8f0a-ef70024b1293","name":"Acme"}>`
- Simulate the other state by rewriting the evalx response with `ctx.route('**/sdk/evalx/**', ...)`.
  ALSO block `**clientstream.launchdarkly.com**`, or streaming restores the real values. Label the result SIMULATED.
- List every real flip needed (flag key, env QA, company Acme, ON/OFF, why); the main session asks Shubham.

## Data rules
- Create test data only when needed. Name it `QA-DEMO <KEY> 13 Nov …`; no "delete me" suffix, no clean-up needed on QA.
- Never edit or delete records you did not create. No bulk destructive actions on shared data.
- Read-only API probes are fine; write probes only on your own QA-DEMO records.
- **Company-level settings** (EG-ACME Account page: labor rounding default/mode, mobilization minutes, etc.) affect every QA user:
  1. Write down the original value first (API + screenshot).
  2. Change it only as long as the test needs.
  3. Restore it before you finish, and prove the restore (API read-back).
  4. Say in the verdict what you changed and when it was restored.
  - Prefer the per-plan override where the ticket allows it.
- An action that can't be undone and hits shared data: do it on QA-DEMO data you created, or describe it as not covered.
- **NEVER open DevRev and NEVER create a DevRev ticket** (owner, 13 Nov: TKT-1600 was created by a test). Don't submit Upload Anything "Request revision" or anything that files to DevRev, even if the ticket's QA Review asks for it (owner reaffirmed 13 Nov); mark that step not covered.
- Anything else that sends to an outside system:
  - At most ONE submission for the whole round.
  - Clearly labelled `QA TEST <KEY> 13 Nov – automated QA check, please ignore`.
  - Report its id.

## Deliverables per ticket: folder `docs/bug-evidence/2026-11-13-rfq/<KEY>/`
1. `QA-13Nov-<KEY>-video-steps.mp4`, checked with a contact sheet.
2. 3–6 screenshots `QA-13Nov-<KEY>-<n>-<what>.png`: real app pixels, each one clearly expected (PASS) or defect (FAIL).
3. `verdict.md`:
   - **Verdict:** PASS / FAIL / PARTLY / BLOCKED / NOT WEB-TESTABLE (backend-only, checked via API), plus the build.
   - **Checked** table (check · how · result). It covers:
     - UI and every button/menu/field the ticket touches.
     - The description's "QA Review" rows, one by one.
     - API contract.
     - Tenant/foreign-id scope.
     - Roles (relevant seats; nav vs typed route vs API agree).
     - Flags ON/OFF (real or SIMULATED).
     - Performance (requests per load, duplicates, calls > 2 s, payload size).
   - **Bugs found:**
     - Plain title, numbered steps, actual vs expected, severity, evidence file.
     - Suggested assignee: Eric owns these tickets; for new bugs, Avani = backend, Krunal = frontend; never Dharmesh.
     - The JQL you ran to make sure no ticket already covers it.
     - Re-prove each bug once more before listing it (same input, same seat).
   - **Enhancement suggestions:** concrete and user-facing, one line + why.
   - **Mobile impact:** what an iOS/Android tester should check, if anything.
   - **Not covered** and why.
   - **Proposed Jira comment** (`<KEY>.txt`, Jira WIKI markup, SHORT):
     - Line 1: `*Result: PASS|PARTLY|FAIL on QA* (build index-…, 13 Nov 2026)`.
     - Line 2: `[^QA-13Nov-<KEY>-video-steps.mp4]` plus an italic one-line caption.
     - Then at most 5–8 short bullets: passed / failed / blocked / one question.
     - Then the screenshots `!QA-13Nov-<KEY>-<n>-<what>.png|thumbnail!`, each with an italic caption line.
     - Escape literal `{ } [ ]` with a backslash. Never start a list line with `-`. No `{{--x}}`.
   - **Recommended status:**
     - READY TO RELEASE: pass, including pass with separate new bugs.
     - To Do: the main purpose 100% fails.
     - Stay In QA: blocked (needs a flag, a seat or data; say exactly what).
4. Final message back (max ~40 lines): per-ticket verdict, bugs, suggestions, flags needed, blockers, shared settings you changed and restored.
