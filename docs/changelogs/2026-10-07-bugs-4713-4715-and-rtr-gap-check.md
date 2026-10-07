# Three bugs filed (Services ×2, Work Order ×1) + Ready-to-Release gap check — 7 Oct 2026, 14:45–15:10 UTC

**Prompts:** "check ready to release ticket if we miss anything and create two bug … service … 409" + screenshot;
"assing to avani in this 2.3 web"; "when you add exisitg asset then form popup dont show up create that bug too in work order".

## Bugs filed (all: Avani, Medium, sprint Z-26-09-S3 id 1223, Web v2.3, Backlog → To Do)
| Key | Title | Reproduced on QA |
|---|---|---|
| [ZP-4713](https://egalvanic.atlassian.net/browse/ZP-4713) | Update Service: after hiding the "AI is revising" dialog, the page doesn't show the update is running and lets you send another | test service 85d67289 "QA-DEMO builder-dialog test - delete me": A submitted 14:53:52, dialog hidden, server `running`, page shows nothing, reopened = blank form |
| [ZP-4714](https://egalvanic.atlassian.net/browse/ZP-4714) | Second update while running shows raw "API call failed: 409 - { build_job … }" | B at 14:53:55 → 409 `build_already_running`, whole JSON printed (matches owner's Cleaning Services screenshot) |
| [ZP-4715](https://egalvanic.atlassian.net/browse/ZP-4715) | WO › Add Asset › Link Existing Assets: no "Set Up Forms" pop-up, linked asset gets no forms | WO b2c2657a "QA-DEMO PM Forms wheel check-offs": control new Panelboard → pop-up; linked existing ATS "test kd check 2" → no pop-up, Forms count stays 4 |
ZP-4713 ↔ ZP-4714 linked (Relates). Screenshots staged for drag-in: `docs/bug-evidence/2026-10-07-jira-attach-zp4713-4714-4715/` (connector cannot attach).
Correction made during repro: the open dialog DOES show "The AI is revising your service…" — the bug is only after hiding/closing it.
Owner's Cleaning Services was NOT touched (its build had already applied); all repro on QA-DEMO data.
Test data: QA-DEMO asset "QA-DEMO form-popup control new asset (delete me)" (Panelboard) + linked "test kd check 2" on WO b2c2657a; several QA-DEMO service versions on 85d67289.

## Ready-to-Release gap check (release 14277)
- **ZP-4572 / ZP-4703:** hold on the new stage build (menu = route for every role; Technician has no web access). Release note:
  ZP-4703's fix is a server-side permission grant (`maintenance_portal.manage` for PM/AM/EE) — it must be applied on production too.
- **ZP-4701 — gaps vs its QA Review:**
  - Step 3 (role matrix) **not met for Account Manager and Electrical Engineer** on QA AND stage: both have `sessions.manage` but not
    `features.site_visits.view`, so Operations › Work Orders is Access Denied and Create Work Order is never offered. (PM has both.)
  - Tier-switch acceptance (Facility Manager → add Admin → remove FM) still unverified (role edits blocked here); moved to RTR by the owner 08:28 CDT.
  - Not covered: plan check (step 5), other entry points (step 7), BCES-IQ (step 8), production check after release.
