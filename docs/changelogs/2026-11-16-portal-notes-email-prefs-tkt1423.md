# 16 Nov 2026 (IST): portal per-issue notes, Email Preferences bug, Eric's In-QA tickets, disk cleanup

## Requests (owner)
1. "i can see some eric ticket are in qa why?"
2. "youtube comments reply by yourself"
3. TKT-1203 (pasted): "Client Portal Notes, Per Issue". Check whether it exists on QA and reply to Dharmesh in the QA-internal group.
4. "only few user are getting screen like this create bug and assign to avani" (Notifications › Email Preferences shows an empty "General" box for mukul+kroon).
5. "open android simulator"
6. "space is almost full … delete the video that we are already edited and uploaded … free up to 4 gb"
7. TKT-1423 (pasted, report questions 5–7). Reply to Dharmesh by DM.

## 1. Eric's tickets still In QA (Web v2.6)
- 10 tickets: ZP-4831, 4833 (CM seat / second company), 5026, 5031, 5111 (no BCES company on QA), 4942, 4943 (feature-sld-assemblies off), 5107 (offline part is mobile), 5100 (needs a real SKM study), 5106 (fork is one-way, owner decision).
- The "updated today 00:26 CST" on 4831/4833/5026/5031 was only Dharmesh's board Rank change.

## 2. YouTube comments
- reply_pass: 162 threads. Posted 1 reply (Ugz8Rq9l6obfI1NVizR4AaABAg, "Hi bhai"). The only thread left is spam (*PromoSM*).
- The 3-hourly auto-reply CronCreate was denied again by the auto-mode classifier. Passes run when the owner asks.

## 3. TKT-1203: portal per-issue notes (QA, build index-BfuWG80P.js)
- **Partly there.** ZP-4707 (Web v2.5, re-scoped 4 Nov) = ONE `comment` field per issue. Internal users write it (Create/Edit Issue › Comments); portal users only read it (issue page › Comments tab, no input).
- Facility Manager portal seat: site "A", issue "NEC Violation on ATS 1" › Comments: "No comments on this issue", no add control.
- The Client Portal seat's account is on No License, so its portal Issues page says "Not included in your plan".
- Posted in QA-internal (tag @Dharmesh) with 2 screenshots: docs/bug-evidence/2026-11-16-portal-issue-notes/QA-16Nov-portal-*.png. Nothing saved on QA (the Edit Issue drawer was cancelled).

## 4. Bug ZP-5292 (Avani, Medium, Web v2.6, Z-26-11-S1, To Do)
- https://egalvanic.atlassian.net/browse/ZP-5292
- Cause: EmailPreferencesModal renders the General block when `platform.length > 0`, then hides each row whose LD flag (feature-weekly-ops-emails / feature-weekly-opportunity-emails) is off, which leaves an empty box. The "No email subscriptions configured yet." check uses the same unfiltered list.
- Repro on QA: Platform Users › kush.jadia@egalvanic.com › Email Preferences. Flags ON (EG-ACME today) shows 2 rows; with the two weekly flags overridden OFF in the browser only (evalx route + clientstream block), the box is empty.
- Role sweep on QA: users who own accounts (Admin, AM) get the table; users with no accounts and no weekly subs (staff Admin, PM, EE) get "No email subscriptions configured yet."; FM/CP land on the portal; Tech has no web.
- Evidence: docs/bug-evidence/2026-11-16-notifications-general-empty/ (3 screenshots + 49 s captioned video).

## 5. Android emulator
- quacky_test started (emulator-5554), booted, lock screen showing (owner unlocks the PIN). App Z Platform QA 1.39 (53).

## 6. Disk cleanup (603 MB free → 5.0 GB free)
- Deleted 4 source clips from ~/Downloads/MyAlbums/share to mac (4.5 GB). Each one's edited video is processed on YouTube:
  - VID_20260802_131906 (bazaar vagSrqsao38 + Short dLlI1k8OaiM)
  - lv_0_20260816184828 (Short dLlI1k8OaiM)
  - VID_20261012_001232 (auction g27k1Xiqeqo + Short 0f8lVp4vlOQ)
  - VID_20250517_135523 (EV Short rXULMekJ-NU)
- Log: ~/Downloads/extrayou/share_plan/deleted_from_share_to_mac_2026-11-16.json.
- Kept: clips still needed by consent-blocked videos (L1, S-DXB1-3, S-OFFICE, S-TRAFFIC).

## 7. TKT-1423 report questions 5–7 (QA EG-ACME, read-only), sent to Dharmesh by DM (3 messages, one screenshot each)
- **5. Breaker size in the FINDING box:** no such field today. The Thermal Anomaly class has 9 fields, and none of the 77 classes has a breaker-size field. It can be added as config (Admin › Issue Classes › Thermal Anomaly › Core Attributes › Add). The global HTML IR report ("IR - Issue Detail" template) prints every filled class field in FINDINGS, so the new field should show there (not tested). Customer and DOCX templates may need a binding.
- **6. Thermal Anomaly 8 + Replacement Needed 1:** the counts are per Issue Class. Replacement Needed is its own class; each issue has one class, so the counts never overlap. The total (`total_issues_identified`) is the sum. The count is not AI and not read from notes. QA example: WO "Job - Jul 29, 11:58 am" = 3+1+2+1+2+1 = 10.
- **7. Problems vs Recommendations:** Problem = Title / Description; Recommendations = Proposed Resolution. Both come from the technician; the IR editor pre-fills them by fixed rules. The "Suggest with AI" resolutions are stored separately and never reach the report (0 of 105 QA issues).
- The customer's exact template isn't on QA (the closest lookalike is config 788832bf). A developer should confirm its bindings on their tenant.
- Evidence: docs/bug-evidence/2026-11-16-tkt1423-report/ (13 screenshots + DOCX PDFs). Side notes, not filed: the DOCX Results Summary leaves Priority blank for Critical; issue aec6e0a2 shows Severity Nominal at ΔT 21.8.

## 8. ZP-3021 (Android, equipment library picker when creating OCPs): reply to Rajat (comment 46197)
- Posted comment 46198 as Abhiyant, tagging @Rajat and @Kush (it also applies to iOS ZP-3020). Web ZP-3019 is still To Do, so there is no web screen to copy.
- 1) Fields: Rajat's plan confirmed (Use library switch → Manufacturer → device search list → linked card with Unlink). Manual and trip fields stay in Engineering. A pick saves the same link as Engineering.
- 2) Pole/Fuse Count: add it to Create New Child and Create Multiple, optional; it narrows the list and is saved on the asset.
- 3) Create Multiple: one pick plus Pole Count for the batch, no per-row change. In a mixed batch the pick applies only to rows of the same type, with an "Applies to N breakers" hint. Optional, and shown only when the engineering library is on.

## 9. ZP-5288 stage hotfix (Kroon: reset password with #), tested on stage only
- PR eg-pz-frontend #2032 (merged to cicd/stag 15:36 IST) is frontend only. ForgotPasswordForm and RegisterForm now use `passwordPolicy.meetsPasswordComplexity` (the 32 Cognito special characters), and the message now lists examples.
- Stage build index-Cx_eO71S.js:
  - Check A: a password with no special character gets the new message, and no server call is made. PASS.
  - Check B: a # password is sent as POST /api/auth/reset-password; the server answers only "Invalid verification code" (dummy 000000). PASS.
- Not covered: finishing the reset with a real code. Stage reset emails reached neither +cpstage nor +stage (not in Inbox or Spam; no app email in this mailbox since 27 Oct). Mobile is not in the hotfix. /register on stage only shows the sign-in page.
- The Android 1.39 (53) APK embeds a web "flow" bundle (assets/flow) that still has the old message. That is a hint only, not tested.
- Jira: moved In QA (Start Testing), then comment 46202 (2 screenshots + 32 s video), then READY TO RELEASE. Sign-off posted in eg-internal-dev tagging @Shubham, with the email question.
- No seat password was changed (both reset attempts stopped before a real code). Evidence: docs/bug-evidence/2026-11-16-zp5288-stage/.

## 10. TKT-1598: panel schedule AI transcription of Spare / Space / blank (QA), sent to Dharmesh by DM
- QA › Panel Schedules › "QA-DEMO ZP-4710 switchboard (delete me)" › Upload Schedule Photo (a printed test schedule with SPARE/Spare, SPACE/space, a blank description with a breaker, and fully blank positions) › Transcribe Schedule Photos. Model claude-opus-5, 12 s.
- Result:
  - SPARE (any case, 1P and 2P) → breaker labelled as written, with the right size, no load type.
  - SPACE / space and fully blank → no placement ("empty").
  - A blank description with a breaker → breaker with an auto label "<panel name>-CB5".
  - 2P breakers and the 24/2-column layout were correct.
- Gaps: no Spare/Space status (SPACE and blank look the same), and blank breakers get a made-up name. Real customer photos (handwritten or faded) are not tested.
- The draft was discarded; the panel is back to "Get this panel started". The test photo stays under Media on that QA-DEMO panel.
- Evidence: docs/bug-evidence/2026-11-16-tkt1598-panel-spare-space/ (photo, result screenshot, transcribe_response.json, video).

## 11. Owner: "share video … always … even in chat" — videos sent where only screenshots went
- Rule saved in memory: video replaces screenshots in every reply (feedback_always_share_video_everywhere).
- Sent:
  - Dharmesh DM:
    - TKT-1598 video, trimmed from 4:04 to 1:04 (QA-16Nov-TKT1598-transcribe-spare-space-video-short.mp4);
    - TKT-1423 video, 55 s (issue class fields, IR report counts and total, issue page, customer-style report).
  - QA-internal (@Dharmesh): TKT-1203 video, 49 s (internal Edit Issue › Comments; FM portal Comments tab read-only; CP No License sees "Not included in your plan").
  - eg-internal-dev: ZP-5288 video, 32 s.

## 12. Mobile cross-check (owner: "always check cross in mobile too", saved in memory) + TKT-1423 customer reply
- TKT-1203, Android 1.39 (53), QA: issue "QA-DEMO ZP-4707 comment check 4 Nov" (Wild Goose Brewery) has sections Details / Properties / Description / Proposed Resolution / Safety / Photos and **no Comments**. The web comment does not appear anywhere in the app. Posted in QA-internal with a video.
- ZP-5288, Android 1.39 (53), QA: Forgot Password with Kroon#Qa5288 and dummy code 000000 → every checklist item green, then the server answers "Invalid or incorrect reset code". Android already accepts #. Posted in eg-internal-dev with a video.
- iOS: not checked for either; the simulator lane is down (Appium 4723 not responding).
- The Android app was signed out for the test and signed back in (SA seat, Field Technician, Wild Goose Brewery). The password manager save prompts were declined.
- TKT-1423: the suggested customer reply (points 5–7) went to the Dharmesh DM with the video. Point 5 was then corrected per the owner: the customer adds it themselves as a custom attribute (Admin › Issue Classes › Thermal Anomaly › Core Attributes › + Add), with a 20 s clip.

## 13. Tickets for the mobile gap, TKT-1262, disk + Xcode
- New Tasks (Medium, To Do, next sprint Z-26-11-S2, no FV, linked to ZP-4707 and to each other):
  - ZP-5294 [Android] issue page doesn't show the ZP-4707 Comments. Rajat; video at the top.
  - ZP-5295 [iOS] show the issue Comments as on web. Kush; marked "iOS not verified yet, simulator unavailable".
- TKT-1262 (all Account Managers see all accounts, BCES), QA:
  - Admin (staff) sees 142 accounts; a customer Account Manager seat sees 14, only the accounts it was added to.
  - Access is per account (Customers › ⋯ › Manage Access List). There is no company toggle, so new customers aren't shared with AMs.
  - Mobile has no accounts list.
  - Verdict: valid feature request, not a bug. Sent findings, a suggested customer reply and a 47 s video to the Dharmesh DM.
- Disk: 1.4 GB → 26 GB free. Removed:
  - app caches (Brave, Comet, qoder, drata, ChatGPT, GeoServices, Homebrew, Android Studio);
  - Chrome profile caches;
  - ~/.gradle/caches (wrapper kept);
  - .playwright-mcp media older than 7 days;
  - the broken /Applications/Xcode.app (2.5 GB, Developer/usr/bin was missing);
  - 26 more share-to-mac clips whose edits are processed on YouTube (log: extrayou/share_plan/deleted_from_share_to_mac_2026-11-16b.json).
- Xcode: opened the Mac App Store Xcode page. The owner must click Get/Install (Apple ID). The iOS 26.2 simulator runtime (iOS_23C54, 16 GB) is still installed, so only Xcode itself is needed.
- Owner: "assign to current sprint". ZP-5294 and ZP-5295 moved to Z-26-11-S1 (1227) with release fix versions Android v1.40 / iOS v1.76 (current sprint → release FV rule). Told Dharmesh in QA-internal with both links and the Android video.

## 14. Technician permissions: web vs the Android app (owner: "if technician in web have extra permission … share the list to Dharmesh")
- Seat: the QA Technician seat Dharmesh shared (avani.patel+techplat). Its password is kept only in the gitignored `.playwright-mcp/techweb/seat.json` and is never written in the repo or in chat.
- Web (QA, v1.36 rail): `/api/auth/v2/me` gives `platform.web` and 99 permissions; `field_limits` has can_create_sites and can_create_work_orders = true. Every rail menu page was opened and its action buttons were listed (d1.js / d2.js).
- **Web only (the Android app has none of these):**
  1. Quotes: New Quote, Delete.
  2. Report Builder.
  3. Forms builder.
  4. Customers: New / Edit / Add Site / Add Contact.
  5. Planned Work: create, Planning Board, Maintenance Calendar.
  6. Maintenance Program: Edit, Compliance, Reports.
  7. Dashboards with report exports.
  8. Assets: Bulk Edit, Bulk Ops, Upload Anything.
  9. Create Connection; Locations: Add Building / Add Floor.
  10. Issues: Add to WO / Add to Quote / Export CSV / Bulk Upload.
  11. Add Tasks, Attachments (folders, upload, delete), Torque Rules.
- **Blocked on web (correct):** the admin settings pages: issue classes, asset classes, users, labor, materials, offices, teams, guest portal users.
- **Android 1.39 (53), same seat (Android Site 2):**
  - Create New Site, Start New Work Order, Issues (create/edit one), Add Assets / edit one asset.
  - Connections, Arc Flash, Schedule, Scan Asset Code, Support, Settings.
  - SLD is view only.
- **iOS:** not checked. Xcode was removed (broken) and is waiting for the owner to reinstall it from the App Store.
- Nothing was saved or deleted; screens and dialogs were only opened.
- Sent the list to the Dharmesh DM with two videos, asking which of the 11 the Technician should keep.
- Evidence (`docs/bug-evidence/2026-11-16-tech-web-vs-mobile/`):
  - QA-16Nov-tech-web-extras.mp4 (62 s, captions 1–8);
  - QA-16Nov-tech-android-app.mp4 (80 s, captioned);
  - raw-android-tech.mp4;
  - raw-web-tech-landing.png.
  - All were checked: no frame shows the password.
- The Android emulator is still signed in as the Technician seat.

## 15. iOS TestFlight on the Mac + new bug ZP-5297 (portal users can't open Account & settings)
- **TestFlight lane.** Xcode 26.6 is back. TestFlight put Z Platform-QA 1.75 (128) on the Mac as an iPhone/iPad app.
  - Its binary is FairPlay-encrypted (cryptid 1), so it can't go into the simulator.
  - It can be driven with System Events accessibility. Memory: project_ios_testflight_mac_lane.
- **Owner's video `~/Downloads/pri_bug.mp4`** (Nency's portal account): avatar › Account & settings bounces back to Site Health.
- **Reproduced on QA** (build index-oFAZQ3rK.js) with our own seats:
  - Facility Manager (+fm@) and Client Portal (+clientportal@): /account → /maintenance-portal → /maintenance-portal/overview (about 20 ms).
  - The same happens for a direct /account, /account/profile, /account/security, /account/notifications and /settings.
  - Super Admin control: /account opens Profile / Security / Notifications / Company.
- **Cause (bundle hint):** the portal confinement guard allows only "/maintenance-portal" and "/release-updates" (`ZFe=[Ec,"/release-updates"]`), and the menu item navigates to "/account".
- **Mobile, same FM seat:**
  - Android 1.39 (53): the Settings tab shows Account, Security (Passkeys, 2FA) and Language, so it is not affected.
  - iOS TestFlight 1.75: the FM can sign in, and the 2FA screen points to "Security in your account menu". The iOS Settings screen was not reached (the app doesn't render while the owner's windows cover it).
- **Filed ZP-5297:** Bug, High, Avani, To Do, Z-26-11-S1, Web v2.6, relates to ZP-5040 and ZP-5292.
  - The description is wiki markup with the 85 s web video, the 33 s Android video and 3 screenshots, all attached; it renders with no errors.
- **Side observation, not filed:** while a Beamer "Heads-up" announcement pill is showing, it sits on top of the avatar menu's "Account & settings" at 1600×1000, so clicking there opens "What's new" instead.
- The Android emulator is now signed in as the QA FM seat (it was the techplat Technician).
- Evidence: docs/bug-evidence/2026-11-16-portal-account-settings/.

## 16. Dharmesh's Yes/No check on six DevRev tickets ("Claude told me these are already done")
The owner allowed read-only DevRev viewing for this task. Results were checked live on QA (web build index-oFAZQ3rK.js; Android 1.39 (53)):

| Ticket | Request | Answer | Evidence |
|---|---|---|---|
| TKT-1585 (= Jira ZP-5260, Backlog) | IR report: an issue's images next to its description, near the front | **Yes** | The Frontloaded DOCX and the HTML "Infrared Thermography Report" put issues near the front, each with its linked thermal/visual photos on the same page. Nested keeps them inside the asset section. Gap: asset-only photos print in the Assets section. A subagent generated 7 reports from WO "Job - Jul 29, 11:58 am" and WO "Job - Oct 29, 9:11 PM". |
| TKT-1597 | Asset attributes viewable from work orders (tablet/phone) | **No** | Android: an asset inside a WO shows only Maintenance Records and Issues, and its menu has only "View on SLD". iOS not checked. |
| TKT-1423 | Report points 5–7 | **Yes** | Nothing to build: point 5 = custom attribute; 6 and 7 are explanations (§11–12). |
| TKT-1270 | Keep the selected site when switching pages | **Yes** | Web: the site stays across Issues → Work Orders → Issues / Assets and after a reload. |
| TKT-1262 | All AMs see all accounts | **No** | §13: per-account access list only. |
| TKT-1169 | Sticky location fields | **No** | Web and Android: after creating an asset in 712 5th Ave › Floor 1 › t1, the next New Asset starts with an empty location. |

- Sent to the Dharmesh DM as a Yes/No list with short notes and 3 videos:
  - QA-16Nov-TKT1270-TKT1169-web.mp4 (80 s);
  - QA-16Nov-TKT1597-TKT1169-android.mp4 (91 s);
  - QA-16Nov-TKT1585-ir-report-images.mp4 (56 s).
- QA test data created on Android Site 2 (left in place per the owner's rule): assets "QA-DEMO TKT-1169 sticky A / A2 / W / W2 / W3 16 Nov" (ATS, 712 5th Ave › Floor 1 › t1).
- The Android emulator is now signed in as the SA seat (Field Technician), with an active session on WO "QA-DEMO ZP-5244 13 Nov check-off".
- The generated report PDFs/DOCX (34 MB, they print a seat email) stay local and are not committed.
