# 6 Nov 2026: Android v1.39 finished (ZP-5224 retest) and Android sign-off

Owner asked:
- "our priority to complete android all ready to qa or in qa ticket";
- "complete this ticket and give sign off for android" (ZP-5224).

Build: Android Z Platform (QA) 1.39 (53), the Firebase upload of 5 Nov 11:16 PM ("v1.39 - site not loading fix"). The owner installed it on the emulator on 6 Nov at 12:20 IST.

## ZP-5224 (site ZTest_28_07 couldn't open): PASS, READY TO RELEASE
Tested 12:33–12:36 IST.
- ZTest_28_07 loads in about 25 s and shows 172 assets, 201 connections and 14 issues, the same as iOS.
- The Assets list and SLD open.
- Switching back to Wild Goose works.
- Logcat shows no "Failed to switch SLD".

Jira and chat:
- Comment [46085](https://egalvanic.atlassian.net/browse/ZP-5224?focusedCommentId=46085): a 55 s step video at the top plus 3 screenshots. Posted from the claude-in-chrome Jira tab (Abhiyant), then moved to READY TO RELEASE.
- Android v1.39 is now 31/31 READY TO RELEASE: https://egalvanic.atlassian.net/projects/ZP/versions/14293/tab/release-report-all-issues
- Android sign-off posted in eg-internal-dev at about 12:40 IST, mentioning Rajat and Dharmesh.

## Android v1.40 (ZP-5112, ZP-5144): still In QA, blocked by QA environment
- Check at 12:20 IST:
  - feature-sld-assemblies is false for EG-ACME.
  - /api/assemblies and node-groups return 404.
  - /auth/v2/me has no field_limits (checked with the Super Admin and Technician seats).
- Comments 46082 (ZP-5112) and 46083 (ZP-5144).
- Asked Shubham in eg-internal-dev (owner rule: mention Shubham for flag changes).

## Tooling
- Emulator: it was found closed at about 12:22 IST, restarted, and the owner unlocked the PIN.
- claude-in-chrome is back, and its Jira tab is signed in as Abhiyant. file_upload into a page `<input type=file>` plus POST /rest/api/2/issue/KEY/attachments works for mp4 and png from the repo folder.
- A video embed (`!x.mp4|width=360!`) does not show up in v2 renderedBody. Verify it through the v3 ADF instead: look for a mediaSingle node.
- In this Chrome, Chat /u/0 is abhiyant.singh@egalvanic.com. /u/1 is a sculptsoft account that asks to re-verify, so don't use it.
