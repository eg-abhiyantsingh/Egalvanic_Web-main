# iOS Ready-for-QA tickets, 17 Nov 2026: tester brief

The owner put a new iOS simulator build in `~/Downloads/Z Platform-QA.app`:
- 1.75 (CFBundleVersion 1.0), Debug, iphonesimulator, API https://eg-pz.qa.egalvanic.ai/api;
- binary linked 16 Nov 06:49 IST.

The lead confirmed the build contains the fix code:
- `can_create_sites`, `can_create_work_orders`, `company_data_epoch`, "server reset deferred", "server reset dropped";
- none of these were in the 5 Nov build;
- the fix PRs eg-pz-mobile-iOS #767 / #768 / #729 / #734 / #736 are merged; #826 promoted dev → qa 16 Nov.

## Tickets (iOS v1.76, assignee Kush; test cases in Kush's Jira comments addressed to Nency; read EVERY comment)
1. **ZP-5104**: work type follows every join; connection cables join the WO.
   - TC1: every add path gets the work type's service.
   - TC2: a New Connection cable/busway joins the WO and shows in full colour on the WO's SLD.
   - TC3: offline.
   - TC4: General type adds nothing; changing the type → the new service.
   - Verify on web too (the asset is in the WO with the service).
2. **ZP-5143**: hide Create New Site / Start New Work Order (and the empty-list "Start your first work order" prompt) for guest Technicians and sub logins.
   - QA /api/auth/v2/me now sends field_limits.
   - Guest Technician: the TECH seat is a guest in Demo Company (company code `demo`). Check field_limits while acting in Demo (curl/web API) first.
   - Normal user (SA) must still see both. Also: a Technician acting in their HOME company must keep whatever field_limits says there.
   - Sub login (subcontractor with no own company): look for one on QA. If none, "not covered".
   - The Android tester is checking the Android twin ZP-5144 at the same time with the same TECH seat; that's fine.
3. **ZP-4829**: drop local company data when the server bumps company_data_epoch.
   - Only TC2 (no bump → relaunch / sign out-in keeps local data, no reload from scratch, no needless wipe) can run without a backend DB bump.
   - Run TC2. Read the app's unified log (`xcrun simctl spawn <udid> log stream/show --predicate 'process == "Z Platform-QA"'`) for epoch messages; first sight should only record the epoch.
   - TC1/TC3/TC4 need a backend epoch bump: write "blocked: needs DB bump" and the exact ask. The lead will ask.

## Lane
- Simulator lane A: "iPhone 17 - ZP 1.70", UDID B9AA2053-964E-4635-BC9F-9B6CB9052D25, iOS 26.2. It is BOOTED with the new build installed (an upgrade over the 5 Nov 1.75 build, so local data from before is still there).
- The lead holds `/tmp/ios170-lane-A.lock` for you. Don't remove it.
  - Run scripts with `cd .playwright-mcp/ios170 && node iosrun.js A <script> [timeoutSec]`.
  - NOT `iosrun.sh`: its mkdir lock deadlocks on the lock file.
- Appium: start `appium -p 4723 --log-level warn` in the background if it's not running. Never use `--relaxed-security`. Android's Appium is on 4725; don't touch it.
- Client and helpers:
  - `.playwright-mcp/ios170/ios.js` (fetch-based Appium client);
  - `.playwright-mcp/ios170/v175/_nav.js` (1.75 navigation helpers);
  - example scripts in `.playwright-mcp/ios170/zp-*`, `rfq26/`, `v175/`.
- Seats: AppConstants (never print passwords). Demo company code `demo`.
- Memory to read first (~/.claude/projects/-Users-abhiyantsingh-Downloads-Egalvanic-Web-main/memory/):
  - reference_ios_lane_lock_shared.md (1.75 nav traps)
  - project_ios26_search_field_automation_trap.md
  - project_ipad_simulator_lane_recipe.md (hidden 2FA screen trap: terminate + reactivate, tap "Set up later")
  - project_ios_device_build_to_simulator_recipe.md
  - feedback_ios_tickets_never_to_do.md
  - feedback_qa_miss_checklist.md

## Rules
- QA only. Don't write to Jira/Chat/Gmail/git; the lead posts.
- QA-DEMO names for anything you create ("QA-DEMO ZP-5104 17 Nov ..."). Don't delete others' data.
- Simulator offline: there is no airplane mode on the simulator, and changing Mac network/firewall settings is NOT allowed. Use an in-app offline toggle if the app has one; otherwise mark TC3 "not covered on simulator".
- Evidence:
  - videos with `xcrun simctl io <udid> recordVideo --codec h264`, 20–60 s, idle trimmed;
  - step captions as PNG bars overlaid with ffmpeg (no drawtext; see `.playwright-mcp/qrmob` caption scripts for the pattern);
  - screenshots with `xcrun simctl io <udid> screenshot`.
  - NO text drawn on screenshots: outline boxes only.
- iOS verdicts: PASS → RTR; FAIL → stays In QA with a new bug (never To Do).
- Output in `docs/bug-evidence/2026-11-17-rfq/<KEY>/`: verdict.md, <KEY>.txt (Jira wiki comment draft, format as in BRIEF.md), MP4 + PNGs named `QA-17Nov-<KEY>-...`.
- Name the build in every verdict: "Z Platform-QA 1.75 simulator build (binary 16 Nov 06:49 IST, from the owner)".

Return per ticket: verdict, one-line reason, evidence paths, new bugs, and anything the lead must ask a dev for.
