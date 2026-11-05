# 5 Nov 2026 (19:00–20:35 IST): Android/iOS Ready-for-QA + In-QA round 2

Owner prompts:
- ~19:00 IST: "continue testing android ios ready to qa and in qa ticket".
- ~20:00 IST: "ZP-5170 start testing this ticket and other ticket that are in ready to qa or in qa for ios and android".
- ~20:05 IST: "add video in starting of the ticket … make ticket length more short" (saved as memory `feedback_video_first_short_tickets`).

Build under test: Android Z Platform (QA) **1.39 (53)**, the Firebase upload from 5 Nov 15:30 IST (git 671624e). App Tester shows it as the latest and it is installed. Emulator `quacky_test`, SA seat, site "(s) Wild Goose Brewery".

## Results (drafts in docs/bug-evidence/2026-11-05-rfq4/<KEY>/, each with a step video)
| Ticket | Result | Status |
|---|---|---|
| [ZP-4988](https://egalvanic.atlassian.net/browse/ZP-4988) Android Engineering parity | PARTLY. Most checks pass and match web. 2 differences from web, 3 not testable on QA | stays In QA |
| [ZP-5170](https://egalvanic.atlassian.net/browse/ZP-5170) Android Duplicate with children | NOT IN BUILD (no Duplicate in row menu, Select mode or asset ⋮) | Ready for QA → In QA |
| [ZP-5207](https://egalvanic.atlassian.net/browse/ZP-5207) Android duplicate QR | fix works (edit, lower case, Create Multiple skip, row error, New Asset). Rajat's QR note text is not in this build | Ready for QA → In QA |
| [ZP-5201](https://egalvanic.atlassian.net/browse/ZP-5201) Android "(New)" label | PASS (field, after save, picker) | Ready for QA → In QA → READY TO RELEASE (after the post) |
| New bug: Android can't open site ZTest_28_07 | Rajat, High, sprint 1226, Android v1.39 | to be created |

## ZP-4988 detail (kept here, not in Jira)
- Notes-only save on CB5 (Android Site 2): `eqp_lib` stayed identical, all 11 keys. Then "Mark as reviewed" added `warnings_reviewed_at` (14:12:52Z). A second notes-only save (restoring "gg") kept all 12 keys.
- PNL-1: MCB + Square D / I-Line saved (13:37:51Z). Clearing the maker set both ids to null (13:40:31Z). Offline: Square D / NEHB synced at 13:44:58Z (after a manual "Sync 1 records"; auto-sync did not run for about 2 min).
- Pole cap: DP-1 copy at 1P3W gives 1P/2P only, and CB1's 3P was cleared → null on the server. At 3P4W, CB1 and CB2 3P saved as 3. A first CB1 save of 3P landed as 2; it could not be reproduced twice, so it is not reported.
- Busway: 17 sizes, the same as `/cable-library/busway-ampere-ratings` and web.
- 3-winding transformer created on Android ("QA-DEMO ZP-4988 3W TX 5 Nov"): no Type, Manufacturer or Asset Subtype. With Use library on, no matcher appears. Web is the same.
- Frames (CB5): 480V first, then the others with "— unmatched voltage". Web shows an "Unmatched voltage" heading.
- Different from web:
  - Panelboard-1, with inherited 5V (only 1P2W allowed): Android locks Phase Configuration; web leaves it editable.
  - After Mark reviewed: Android keeps listing the warnings; web hides them.
- Not testable:
  - The voltage-conflict banner: no conflict on 30 sites scanned. Building one on Android failed: New Connection › Create stays disabled.
  - The flag-off case: `feature-eng-lib` is true for acme and demo.
  - The "No trip-unit data" note: the device library was updated 22 h ago.
- Web parity table: background agent, screenshots in docs/bug-evidence/2026-11-05-rfq3/ZP-4988/web/.
- Fixtures created on QA (no delete needed):
  - QA-DEMO ZP-4988 3W TX 5 Nov
  - QA-DEMO ZP-4988 TX-A 480V 5 Nov
  - QA-DEMO ZP-4988 TX-B 208V 5 Nov (manufacturer "QA-DEMO Mfr ZP-5201")
  - QA-DEMO ZP-4988 conflict BUS 5 Nov

## New bug: Android site ZTest_28_07
- Switching to it fails at 30% with an NPE ("Failed to switch SLD", `si5.<init>` ← `eda.L`, R8 map 2274ecc…). The app is then left with "No Site Selected". Reproduced 4×, 19:32–19:35 IST.
- Controls:
  - Android Site 2 and Wild Goose load fine on Android.
  - iOS 1.75 opens ZTest_28_07 in about 20 s (checked by the ios-automation-site-29 session, 20:00 IST).
- Likely trigger: 3 IR photos with `node_id` null (IR session 8d8757c3…), which only this site has.

## iOS
- In QA: ZP-5219, ZP-5208, ZP-5203, ZP-5213. Nency moved them at 19:58 IST, so she is on them.
- Their fix (iOS PR #821) was merged to QA at 19:36 IST, after the 1.75 simulator build (17:49), so ZP-5219/5208 need a new build.
- ZP-5213 is flagged as a duplicate of ZP-5203.
- ZP-5203 hint: the simulator 1.75 Info.plist has only NSLocationWhenInUseUsageDescription and no "Always" keys. Background modes = remote-notification.

## Posting
- Nothing with media is posted yet. This session's Atlassian connector is Abhiyant's but cannot upload files.
- The jira-chrome-reconnect-tasks (3) session has no signed-in Jira tab either.
- Manifest: .playwright-mcp/v25/jira/post-manifest-5nov-rfq4.json. Waiting for the owner to sign in to Atlassian in the chrome-devtools window.
- Status moves already done through the connector: ZP-5170, ZP-5207 and ZP-5201 → In QA.

## 20:26–20:55 IST: new upload (Firebase 8:08 PM, installed 20:26 by the owner)
- **ZP-5170: PASS on the 8:08 PM upload.** The path is Edit Asset › ⋯ › Duplicate; the view-mode ⋯ has no Duplicate. The dialog reads "Creates a new asset with copies of its N sub-components. The copies get no QR code and no serial number." Server checks:
  - "QA-DEMO ZP-5215 5 Nov SWB (Copy 3)": 3 levels, 6 assets, line/load edges copied, QR/serial empty. The original keeps Q5215-SWB / SN-5215-SWB.
  - "QA-DEMO ZP-5204 5 Nov PNL-1 (Copy)": MCB, Square D (110), NEHB (5) copied.
  - "QA-DEMO 4 Nov ZP-5171 Main Distribution Panel (Copy)": 7 children, no QR.
  - "PN111 (Copy)": done offline (2 pending), then synced. Exactly 1 copy (counts checked).
  - Web shows the copies, QR Not Set.
  - Child names repeat "(Copy)" across copies, which matches web.
- **ZP-5207: PASS.** The QR note is now under every QR field, and used codes are still refused.
- **ZP-5201: PASS** (still no "(New)").
- **ZP-4988:** smoke check fine (PN111 MCB warning).
- Jira:
  - ZP-5201 and ZP-5207 were moved to READY TO RELEASE at 20:41 IST by someone else, with no comment.
  - Manifest updated: ZP-5170 → RTR; ZP-5201 and ZP-5207 comment only.
  - The bug waits for an 8:08 PM re-check of ZTest_28_07 (asked of the ios-automation-site-29 session, which has the emulator).
- Posting is still blocked:
  - jira-chrome-reconnect-tasks (3)'s permission check refused writes requested by another session.
  - The owner must approve there, or tell this session to post.
- ios-automation-site-29 is taking ZP-4863 and ZP-5168 on the 8:08 PM upload and will post the Android sign-off in eg-internal-dev.
- 20:48 IST: ZTest_28_07 still fails on the 8:08 PM upload (checked by ios-automation-site-29; op9/th9 NPE). Bug draft updated, manifest marks it ready to post.
- 20:57–20:59 IST: ZP-4863 re-checked on the 8:08 PM upload (WO session QA-DEMO ZP-5135 restarted at 20:56 to reach the EG form): phone landscape and tablet portrait still use the drawer, tablet landscape shows the list beside the form → PARTLY, unchanged, stays In QA. Screen reset (density 420, portrait, auto-rotate). Draft + 24 s video added to the manifest (comment only). The Android/iOS sign-off was posted in eg-internal-dev at 21:00 by ios-automation-site-29.
