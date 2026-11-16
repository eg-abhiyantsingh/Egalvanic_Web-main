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
