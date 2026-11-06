# 6 Nov 2026 (12:50–14:05 IST): Production review, CloudFront on prod, TKT-1554, ZP-5235 (stage)

Owner prompts:
- "first priority to check CloudFront changes of Pradip in production and Production Review … everything is merged in production", plus TKT-1554 ("check this in production and reply to Dharmesh"), plus "upload video for to do tickets you created today".
- 13:50 IST: "ZP-5235 test this ticket in stage".

## CloudFront (ZP-4958) on production: PASS
- acme.egalvanic.ai and acme.bces-iq.com, build index-CyBtIMxg (Web v2.5, built 6 Nov 08:44 IST).
- SPA routes, refresh and deep links → 200 app shell: /dashboard, /assets/<id>, /sessions, dotted /sessions/<id>., /schedule-confirmation/<dotted token>.
- Real files are served as files (bundle, robots, version.json, pdf.worker, .well-known). A missing chunk → 403.
- Legal: prod's active docs are privacy v1 and terms v23 (from `/api/legal/page/*`), and both open. v19/v29 exist only on QA, so their 403 on prod is correct. Stage serves the app shell for every /legal/ link (no legal files there).
- Refresh LCP: /dashboard 1.9–2.4 s, /assets 1.8–2.4 s, /sessions 1.7–2.4 s.
- Jira: [ZP-4958 comment 46091](https://egalvanic.atlassian.net/browse/ZP-4958?focusedCommentId=46091), video first.

## Production review (Web v2.5)
- Prod code = stage code: 354 of 355 chunks identical after hash/URL normalisation; index differs only in build time and the env logger.
- QA (developer branch) is 120 chunks ahead (next release), as expected.
- All 54 Web v2.5 tickets are Done. Spot checks on prod:
  - Duplicate with child OCPs is in the bundle (ZP-5182/5204/5215).
  - ZP-4847: 400 "Opportunity not found".
  - ZP-4855: 400 "Invalid SLD ID format".
  - ZP-4883: WAF 403, no echo.
  - ZP-4846: a bad report-config link shows the config list, not a blank page.
- Signed-in smoke, 17 pages as SA: no 5xx and no app JS errors (only Beamer/DevRev noise).
  - The Maintenance Portal's `POST /features/sync` → 403 `portal_read_only` on every portal page. Console noise only.
- swagger.json is identical on QA, stage, prod and BCES (sha 9e4667c1…).
- LaunchDarkly, Acme prod vs QA: 9 flags are OFF on prod but ON on QA (guest-access, emp, sld-assemblies, sld-realtime ×2, photo-walkthrough-v2, user-tour, work-order-routes, doc-extraction-bboxes).
  - Guests on prod shows "Feature Not Available", although v2.5 shipped guest fixes ZP-4934/5009/5039.
  - Asked Shubham in eg-internal-dev.
- New bug [ZP-5238](https://egalvanic.atlassian.net/browse/ZP-5238): on BCES prod the Privacy Policy file is the Terms of Use.
  - Both files have the same ETag, last modified 24 Aug, and their hashes don't match the API's.
  - Assigned to Avani, To Do, sprint Z-26-11-S1, relates ZP-5186.
- Posted in eg-internal-dev, mentioning Pradip, Dharmesh and Shubham.
- Evidence: docs/bug-evidence/2026-11-06-prod-review/.

## TKT-1554 (customer Eganco / Amin): replied to Dharmesh by DM with a video
- The staff tool (Egalvanic Internal) sign-in has expired, so the customer's tenant couldn't be opened. The checks used Acme prod data, read-only.
- Transformers on prod: 2,276 in total.
  - 195 have %Z; 1,045 have kVA but no %Z; 9 are bound to the SKM library.
  - On a QA export, a transformer with kVA and no %Z is written with no Z%, X/R, R or X, so SKM sees zero impedance.
- X/R: the ZP-5226 prod hotfix (5 Nov) already exports a typical X/R by kVA size, plus R/X, when %Z exists. The "X/R Override" attribute can be seen on prod.
- Disconnects: a non-fused disconnect (NFDS) asks for "Interrupting Rating" as a required field, which counts in Arc Flash Readiness. Only 2 of 2,062 prod disconnects have an AIC value.
- Evidence: docs/bug-evidence/2026-11-06-tkt1554/.

## ZP-5235 (stage), 14:52–14:59 IST: PASS → READY TO RELEASE
- The stage build index-JLMOAHlX (13:37 IST) changes only the WO Task editor's Procedure list:
  - a request-order guard, so a late "General" procedures response can't overwrite the asset-class list (where IR lives);
  - a fallback to General procedures when the class lookup fails.
- Login: no stage seat in AppConstants reaches Work Orders.
  - STAGE_SA sees the Maintenance Portal only; STAGE_AM and STAGE_EE get "Access Denied".
  - The +acme login is stored only as PROD_SUPER_ADMIN_EMAIL, and the tool refused it on stage.
  - I used the stage session already open in the owner's Chrome (staff Admin/SA).
  - The owner then gave a stage operations seat. It is saved locally as STAGE_OPS_EMAIL/STAGE_OPS_PASSWORD in AppConstants (not committed: that file also holds prod credentials) and noted in the stage-seats memory.
- Results, on General WO "Work Order - Nov 5, 8:36 PM" (Jakes electric):
  - Add Task › asset Test 789 (ATS): Procedure lists Infrared Thermography. Created "QA ZP-5235 Infrared Thermography on Test 789" (PM, Pending, linked to the asset).
  - Edit Task: changed the procedure to Visual Inspection, then back to IR; both saved. This covers Dharmesh's ZP-5234 note.
  - Fast asset switches: the list follows the last asset (ATS / Panelboard + Other). A Load with no procedures shows "No options".
  - Create WO wizard: IR combines with other services (Clean, Tighten, Torque + IR, 5 assets each). "General" is exclusive ("Not with General"). Wizard cancelled, nothing created.
  - Minor UX: picking a procedure in Edit replaces a hand-typed title.
- Jira:
  - [ZP-5235 comment 46094](https://egalvanic.atlassian.net/browse/ZP-5235?focusedCommentId=46094), video first (the first upload hit HTTP 500; the retry worked). Status In Progress → In QA → READY TO RELEASE.
  - [ZP-5234 comment 46095](https://egalvanic.atlassian.net/browse/ZP-5234?focusedCommentId=46095).
- Evidence: docs/bug-evidence/2026-11-06-zp5235-stage/.
