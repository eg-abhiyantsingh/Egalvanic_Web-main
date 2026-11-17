# Ready-for-QA round, 17 Nov 2026: shared brief for every tester

QA build at start: `index-oFAZQ3rK.js` (re-check `curl -sk https://acme.qa.egalvanic.ai/ | grep -o 'index-[^"]*\.js'` before each verdict; name the build in every verdict).

## Hard rules
- QA only: https://acme.qa.egalvanic.ai (company EG-ACME). Never production or stage. Never open DevRev or trigger anything that files a DevRev ticket (e.g. Upload Anything "Request revision").
- Do NOT write to Jira, Google Chat, Gmail or git. Read Jira freely (getJiraIssue with comments; read EVERY comment, dev notes are test rows). The lead posts after checking your evidence.
- Creating QA test data is fine (prefix names with "QA-DEMO <ticket> 17 Nov"). Don't delete other people's data. Don't change company-wide settings unless the ticket needs it, and restore them after.
- Never print passwords. Seats come from AppConstants via the helpers below.

## Tools
- Playwright runner: `.playwright-mcp/runner/pwslot.sh <script.js> <timeoutSec>`; script = `async (page) => { …; return result }`.
- Seat helper: `const L=require('/Users/abhiyantsingh/Downloads/Egalvanic_Web-main/.playwright-mcp/v24/assets-egforms/lib.js'); const s=await L.seat(page,'SA'); await s.signIn();` → `s.p` page, `s.api('/path')` (/api fetch with session), `s.net` (API calls log), `s.cons` (console errors), `s.pickSite(name)`, `s.build()`. Seats: SA, ADMIN, PM, AM, EE, FM, CP, TECH, PS.
- Video with step captions: `.playwright-mcp/v25/video/vid.js` (V.start(page,{seat,outDir}) → v.p, v.card(text), v.step(text,0), v.hl(locator,ms), v.finish(file)). A v.card() replaces the page, so navigate again after it. Keep videos 20–60 s; trim idle time; captions must match what actually happens.
- Traps: a "User guide" tour modal may appear (click "Maybe later"); a Beamer "Heads-up" announcement pill covers the bottom-left avatar menu (click its X); the avatar sits under the round Z button (click its top edge); the site picker `input[placeholder='Select facility']` only shows on Site Data pages.
- Memory notes worth reading for context: ~/.claude/projects/-Users-abhiyantsingh-Downloads-Egalvanic-Web-main/memory/ (MEMORY.md index; feedback_qa_miss_checklist.md before every verdict).

## What to do per ticket (depth over speed)
1. Read the ticket + every comment. Find the fix's PR if linked (dev panel / comments).
2. Run the exact repro from the ticket on the current build, plus the developer's test rows, plus edge cases. Use real, populated data.
3. Check the API behind it (status codes, payload, bad input, other company/site id where relevant), and the roles that can reach it.
4. Feature flags: if the feature is behind a LaunchDarkly flag, test ON and OFF (or say which flag needs flipping).
5. Note enhancement ideas separately.
6. Mobile: say whether iOS/Android have the same screen; check Android only if cheap (emulator lane is busy, don't use it); otherwise write "mobile not checked".

## Output per ticket, in docs/bug-evidence/2026-11-17-rfq/<KEY>/
- `verdict.md`: PASS / FAIL / PARTLY, build, seat(s), what was run, what happened, what was not covered, any NEW defects (separate from the ticket's own fix) with repro steps.
- `<KEY>.txt`: the Jira comment draft in Jira wiki markup, plain words: first a video embed `!QA-17Nov-<KEY>-...mp4|width=720!` with an italic caption, then result line, build, numbered steps, actual, not covered, full test-data links (full https URLs), then 1–4 screenshots `!file.png|width=640!` each with an italic caption saying PASS (expected) or FAIL (defect). Escape { } [ ] with a backslash.
- The MP4 and PNGs named `QA-17Nov-<KEY>-...`.
- For a NEW bug: `BUG-<short>.txt` in the same format (simple explanation, where, environment, steps, actual, expected, controls, possible cause), plus its own video/screenshots.

Return to the lead: per ticket the verdict, one-line reason, evidence paths, and any new bugs.

## Cross-platform (MANDATORY, added 17 Nov after owner feedback; replaces "mobile: check only if cheap")
For every ticket, the verdict must have a **Cross-platform** section:
- Twins: search Jira for `[iOS]` / `[Android]` / web tickets with the same Task # or summary.
- Does the feature exist on web / iOS / Android? Use the app UI. The eg-pz-mobile-iOS source is readable with `gh` (e.g. `gh search code --repo Egalvanic/eg-pz-mobile-iOS <term>`).
- Web testers can't drive devices: list the exact iOS and Android checks needed (screen, steps, expected result). The lead dispatches them to the device testers.
- Mobile testers: check the same feature on web (UI parity + web read-back of the data), and list what the other mobile app needs.
- End the Jira draft with: `*Cross-platform:* web <result> · iOS <result> · Android <result>`. Each result is PASS / differs (details) / not on this platform (how you know) / not checked (why).
