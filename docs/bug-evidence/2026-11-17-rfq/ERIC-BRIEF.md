# Eric's In QA tickets, 17 Nov 2026 afternoon: finish them

Owner, 17 Nov: "eric ticket are still in qa complete that too". These 10 Web v2.6 tickets (assignee Eric Ehlert) have been In QA since 12 Nov.
Each one PASSED most rows on 12 Nov (build index-C1ZRIunF.js) but stayed In QA because of a blocker.
Your job: remove the blocker the legitimate way, run the missing rows for real, quickly re-check the main rows on today's build, and write the verdict.

Read `docs/bug-evidence/2026-11-17-rfq/BRIEF.md` first: hard rules, tools, output format. Everything there applies.
The 12 Nov evidence for each ticket is in `docs/bug-evidence/2026-11-12-v26-qa/<KEY>/` (verdict.md, <KEY>.txt). Read it so you only redo what is needed.
Also read every Jira comment on the ticket, including Eric's or devs' replies after 12 Nov.

QA web build now: `index-CFMAmUdK.js` (new since this morning). Re-check before each verdict.
Output: `docs/bug-evidence/2026-11-17-rfq/<KEY>/` (verdict.md, <KEY>.txt, MP4, PNGs named QA-17Nov-<KEY>-...).
- NO text drawn on screenshots: outline boxes only.
- Step captions in videos are fine. Videos 20–60 s, idle time trimmed, captions must match what happens.

## Facts the lead checked on 17 Nov
- LaunchDarkly `feature-sld-assemblies`, live evalx at sign-in: **true for EG-ACME**, **false for Demo Company** (demo.qa.egalvanic.ai). The OFF state is real in Demo, not simulated.
- Demo Company login: https://demo.qa.egalvanic.ai/login, email (the Demo admin email, see memory reference_second_tenant_demo.md), password from memory file `reference_second_tenant_demo.md`.
  - This is a colleague's own account and a staff login (is_eg_admin true).
  - Read and small QA-DEMO actions only. Do NOT change Demo's company settings.
  - Sign-in flow: email → "Use my password" → password → "Sign In"; "Set up later" if the MFA prompt shows. Same as lib.js.
- BCES tiers: EG-ACME and Demo both offer only `read_only`, `no_license` (GET /api/features/access → offered_license_types).
- ZP-4830 (Collaborators: subcontractor/customer company links, the Accept button that didn't render) is **In Progress** with Avani (updated 17 Nov).
- The Android tester is on the emulator; don't touch it. The TECH seat is in use on Android, so don't change the TECH seat's roles or sites.

## Per ticket: what's missing (from the 12 Nov verdicts)
- **ZP-4831 Safety / Construction Manager:** BLOCKED on 12 Nov on "Safety menu for a real Construction Manager login" (the QA-DEMO CM user had no deliverable email).
  - Legit fix: give a spare QA test seat (NOT TECH/SA/ADMIN; e.g. the PS seat or the second Technician seat in AppConstants) the Construction Manager role ONLY, via Admin › Platform Users › Edit User.
  - Sign in as it and test the Safety menu and pages. Then RESTORE its original roles exactly; record before/after roles in verdict.md.
  - Also re-check the minor FAIL (typed /safety/sld renders the canvas for seats without CM) and the Admin question on today's build.
- **ZP-4833 Planned work:** PASSED except "a linked subcontractor's Team on a schedule" (no accepted company link on QA; the Accept button didn't render, ZP-4830).
  - Check whether an accepted subcontractor link exists now, or whether Collaborators › Requests › Accept works on today's build. If it works, link a QA-DEMO subcontractor (EG-ACME ↔ Demo is approved for second-company tests) and schedule a planned WO on the linked sub's Team.
  - If still impossible, say exactly why and that it depends on ZP-4830 (In Progress).
- **ZP-5106 Fork a global service:** stayed In QA because a real fork moves open WOs (one-way). The owner's "complete that too" means go ahead, on SAFE data:
  - Pick a global service EG-ACME has NOT forked that has 0 open WOs today.
  - Create 1–2 QA-DEMO open WOs with it, plus a control WO in a non-open state if the ticket distinguishes open vs closed.
  - Fork it, then verify the open WOs repoint to the fork (UI + API) and Set Up Forms matches the fork to its global.
  - Do NOT fork UPS Maintenance or any service with other people's open WOs. Read the ticket's QA Review for the exact expected rows.
- **ZP-4942 / ZP-4943 SLD assemblies:** stayed In QA only for a real flag OFF/ON check.
  - Do the real OFF check in Demo Company: no "+ Assembly", "Add to…" without Assembly, /assemblies "Feature Not Available", no Assemblies in Admin nav. ON = EG-ACME.
  - Quickly re-run the main ON rows on today's build.
  - ZP-4943 also lists "hidden-edge badges inside a locked group" as not covered; cover it.
  - ZP-5264 (next suggestion "L1 CB-3") is filed separately; just note whether it still happens.
- **ZP-5107 offline-first layout groups (web):** web online rows passed. Remaining:
  - the real flag OFF/ON for assembly placement (Demo vs EG-ACME);
  - the offline rows. Read the QA Review carefully: if the offline part is mobile-only, say the Android side is ZP-5112 (being tested on Android 1.40 now) and iOS has its own ticket if any (search Jira).
- **ZP-5100 SKM import hotfix:** only "transformer Type and reserved core attributes on import" were unverified, because the hand-made study had no model name or electrode/winding fields. Find a REAL SKM study:
  - Jira attachments on ZP-5100 and its linked tickets;
  - Google Drive search ("Koch", "SKM", "PTW", ".zip");
  - repo/Downloads (`docs/bug-evidence/**/*.zip` are mostly app exports, not real studies);
  - memory `project_skm_roundtrip_zp3836.md`.
  - Import it into a NEW QA-DEMO site and verify those two rows.
- **ZP-5026 / ZP-5031 / ZP-5111 BCES tiers:** every BCES row was SIMULATED; the real rows are BLOCKED because no QA company offers BCES tiers.
  - Look for a legitimate in-app path on QA (staff company/subscription settings visible to the SA seat) to offer BCES tiers on EG-ACME. If one exists, you may use it on EG-ACME only. Set one QA-DEMO account to BCES-Lite / BCES-Viewer, run the real rows with the CP seat or a portal preview, then RESTORE.
  - Never change Demo. Never use the "Egalvanic - Internal" MCP tools: they act on customer tenants.
  - If no legit path exists, write precisely what's needed (e.g. "a QA company with offered_license_types incl. bces_lite, bces_free") so the lead can ask Shubham.

## Verdict guidance
- PASS → recommend READY TO RELEASE.
- Still blocked on something only a dev/admin can provide → recommend "stays In QA" with the exact ask.
- New defects → a separate BUG-<short>.txt (format in BRIEF.md). Double-check the defect on today's build before writing it.
