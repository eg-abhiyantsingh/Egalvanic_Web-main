# ZP-4833 · Planned work: a schedule can be worked by a linked sub's Team; "Schedule locked for confirmation" copy (QA, 17 Nov 2026)

**Verdict: PASS → recommend READY TO RELEASE**, with one caveat for the lead.

The linked-Team row was run for real today. It used the company link the ZP-4830 tester created (`docs/bug-evidence/2026-11-17-rfq/ZP-4830/LINKS.md`, "Link A"). In that link **Demo Company is the prime and EG-ACME is the subcontractor**, so the roles are reversed from the ticket's wording. The opposite direction (EG-ACME as prime) is still blocked, because LaunchDarkly `feature-guest-access` reads **false for EG-ACME** today (it was true on 12 Nov): `POST /api/vendors/<id>/link` returns 403 `guest_access_disabled`. The fix itself (crews resolved through `subcontracting.assignable_crews`) is not tied to a company.

Builds:
- EG-ACME copy rows on `index-LGhrn-48.js` (14:42 IST), re-checked on `index-CkkAeA9o.js` (14:49 IST).
- Demo linked-Team run on `index-CkkAeA9o.js` (15:13 IST).

Seats:
- EG-ACME: Super Admin test seat (staff).
- Demo Company: the approved Demo admin login (`is_eg_admin` true; colleague's account; only small QA-DEMO actions).

## Checked
| QA Review row | How | Result |
|---|---|---|
| A linked sub's Team on a planned WO's schedule: it saves (no 404 "Crew not found") and the WO shows scheduled days | Demo › Planned Work › Create Planned Work Order. Site "test"; name "QA-DEMO ZP-4833 17 Nov linked-team schedule"; Infrared Thermography; window 1–19 Dec. Assignment › Subcontractor › "QA-DEMO ZP-4830 17 Nov sub (EG-ACME)" (the linked company, 7 Teams listed) › Team "QA-DEMO ZP-4583 r8 team" (EG-ACME's Team, 2 people). Schedule: weekdays 06:00–14:30. Review › Plan Work Order | **PASS**. `POST /ir_session/create` 201. `PUT /ir_session/af67d719…/crews` 200 (`subs:[{vendor 7ea55eda…, crew 74017043…}]`). **`PUT /ir_session/af67d719…/work-schedules` 200** with `crew_ids:["74017043…"]`; no 404. The Schedule dialog shows "14 days · 119 h", "Who: QA-DEMO ZP-4830 17 Nov sub (EG-ACME) · QA-DEMO ZP-4583 r8 team", and 1–18 Dec day by day (screenshots 4 and 5, video 2). The list row reads "14 days · Dec 1 – Dec 18 · QA-DEMO ZP-4583 r8 team, QA-DEMO ZP-4830 17 Nov sub (EG-ACME) · Tentative · Not emailed · confirm by hand" |
| Only in-house crew members are placed on the work blocks; the linked Team's people are reached through the field-team step | `GET /ir_session/af67d719…` (`work_blocks`), `/team`, `/release-team` (read only, not released) | **PASS** for the linked half. 14 work blocks (one per day) were created, and **no person is placed on them**: `/team` returns `assignments: []`. The field-team list (`/release-team`) offers the Team as `kind: "linked"`, "Subcontractor team · Lead: Abhiyant Singh", with its 2 people `assigned: false`. Demo has no in-house crews, so the "in-house members are placed" half was not re-run today |
| Notifications NOT configured → "Schedule locked for confirmation" | (a) EG-ACME "QA-DEMO ZP-4583 any-crew demo 19 Oct" (`notify_enabled` false, all notices off). (b) The new Demo WO: notifications were off by default, `notify.enabled: false`, then `schedule-lock` 200 | **PASS** in both. Activity reads "Schedule locked for confirmation" (screenshots 1 and 5). On 12 Nov the notify-ON state was only SIMULATED |
| Notifications configured → still "Schedule sent for confirmation" | EG-ACME "QA-DEMO ZP-4583 r8 CA holiday Jul 2027" (`notify_enabled` true, lead-0 notice sent) | **PASS**. "Schedule sent for confirmation to N recipients" is unchanged (screenshot 2) |
| EG-ACME as prime (the ticket's wording) | EG-ACME › Collaborators › New company link › Subcontractor › my QA-DEMO sub record › Demo admin email › Send request | **BLOCKED by a flag, not by the fix.** 403 `guest_access_disabled`. Collaborators reads "Collaboration isn't turned on for your company yet" (screenshot 3). The live evalx reads `feature-guest-access` false for EG-ACME (14:28, 14:41 IST). No link was created by me |
| Flags | — | `feature-planned-work-v2` is true in EG-ACME and Demo. The copy change has no flag of its own |

## Bugs found
None in this ticket.

To pass on: `feature-guest-access` = false for EG-ACME on QA. If that was not a deliberate flip by someone testing ZP-4830's flag, Shubham should turn it back ON (env QA, org `d59d449b-09d8-45d6-8f0a-ef70024b1293`). The ZP-4830 tester has asked for the same.

## Not covered
- **EG-ACME as prime** scheduling Demo's Team: blocked by the flag above. Demo has no Teams of its own anyway.
- **The in-house half** ("in-house members ARE placed on blocks"): Demo has no in-house crews.
- **Releasing to the linked Team** through the field-team step: not done on purpose. It would give the PM and FM test seats Technician guest access to Demo, and the ZP-4830 tester is covering that path.

## Test data
- Demo planned work order "QA-DEMO ZP-4833 17 Nov linked-team schedule" (id `af67d719-1146-4ac8-9f2d-05413ab41ec4`, site "test"). Schedule locked; notices off, so nobody was emailed. https://demo.qa.egalvanic.ai/planned-work
- EG-ACME subcontractor record "QA-DEMO ZP-4833 17 Nov sub (Demo Company)" (vendor `9767989d…`). No link (the send was refused). https://acme.qa.egalvanic.ai/subcontractors
- Copy-row work orders (EG-ACME, from 19 Oct): https://acme.qa.egalvanic.ai/planned-work (search "QA-DEMO ZP-4583").

## Cross-platform
- **Twins:** none for task #169 (Jira search for "task #169", "planned work", "linked sub", "sent for confirmation"). Related mobile tickets for sub logins: ZP-5143 [iOS] and ZP-5144 [Android], "Task #161: Hide site & work order creation for guest Technicians and sub logins", both In QA.
- **Web:** PASS (above).
- **iOS:** the Activity copy is **not on iOS**: source `release/qa` 34bd67e (1.75) has no planning or "confirmation" strings. iOS shows a technician's scheduled days from `work_blocks` (`SessionWorkBlockDTO`, `HomeView`). Today's linked-Team blocks carry no people, so no EG-ACME phone should show these days until the WO is released to someone. Not checked on a device.
- **Android:** not checked. The source is not readable here; same expectations.

### MOBILE-CHECKS
1. **iOS and Android, EG-ACME PM seat** (member of the linked Team "QA-DEMO ZP-4583 r8 team"), **before any release.**
   - Home / schedule for 1–18 Dec, and the company switcher.
   - Expected: no Demo work order and no work blocks for "QA-DEMO ZP-4833 17 Nov linked-team schedule". Its people are not on the blocks and not released.
2. **iOS and Android, same seat, after the Demo admin releases this WO to that seat** (Demo › the WO › field team › tick the PM person; coordinate with the ZP-4830 tester).
   - Expected: the seat can switch to Demo (guest) and the WO appears; it cannot create sites or WOs there (ZP-5143 / ZP-5144).
   - Note whether the 1–18 Dec days show on Home. They may not, because the blocks are crew-level.
3. **iOS and Android:** expected: no "Schedule locked / sent for confirmation" text on mobile (web Activity only).
