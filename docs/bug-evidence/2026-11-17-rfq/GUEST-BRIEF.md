# Guest users + ZP-4830, 17 Nov 2026 afternoon

The owner asked: "for guest user the menu that are visible is incoorect or not can you check that too".
ZP-4830 (Collaborators: subcontractor & customer company links, assignment-driven guest access, Task #161) moved to Ready for QA at ~14:05 IST. The fixes are eg-pz-frontend #2042 and eg-pz-backend #1820, on top of #1553 / #1554 / #1749 / #1750. The lead moved it to In QA.
It FAILED on 12 Nov. My comment 46130 on the ticket has the FAIL rows to re-check:
- Withdraw shows "undefined's company";
- the row buttons read grid params instead of the row: Accept/Decline missing, End link throws a TypeError;
- the API accepts a bad email.

Read BRIEF.md (rules, tools, output format, MANDATORY Cross-platform section) and every Jira comment.
QA web build: index-CFMAmUdK.js or newer.

## Who is a guest on QA (facts so far)
- **Technician guests:**
  - the TECH seat (and Rajat's "+acmeqa" users) are EG-ACME home users who are Technician guests in **Demo Company** (company code `demo`);
  - in Demo, the Technician role has NO platform.web, so web shows "Web Access Restricted" (seen 17 Nov for ZP-5274);
  - on EG-ACME, the company override grants platform.web to Technician.
- **Demo admin login:** https://demo.qa.egalvanic.ai/login, (the Demo admin email, see memory reference_second_tenant_demo.md), password in memory `reference_second_tenant_demo.md`.
  - It is a **staff** login (is_eg_admin true) and a colleague's own account. Use it only to send/accept links, tick people onto Demo WOs, and share a QA-DEMO SLD. Keep changes small, name everything "QA-DEMO ZP-4830 17 Nov …", don't delete, and don't change Demo's company settings.
  - Never use a staff login to judge what a guest sees: staff skews permissions. The guests must be our non-staff EG-ACME seats. Check is_eg_admin in /auth/v2/me.
- **What a guest should get** (ZP-4830, ZP-5143/5144, ZP-5274):
  - the host company's Technician permissions;
  - a subcontractor member gets Technician guest access ONLY while ticked onto an OPEN work order, and loses it on untick / unassign / single-role replace / close / delete; reopening restores it;
  - a customer-link guest opens exactly the shared SLDs;
  - field_limits can_create_sites / can_create_work_orders = false for a guest Technician.

## Web tester: ZP-4830 + guest menus on web
1. **ZP-4830:** run every QA Review row for real.
   - **Link A, Demo = prime, EG-ACME = sub:** the Demo admin invites the EG-ACME ADMIN seat's email as a subcontractor; the EG-ACME admin accepts. Then the EG-ACME Teams appear as crews on a QA-DEMO Demo WO.
     - Tick a non-staff EG-ACME seat (EE or the second Technician seat; NOT TECH, which the device testers are using right now) onto that open Demo WO. Check it gains Technician guest access in Demo: /auth/v2/me companies, then the Demo site/WO lists.
     - Untick → access is gone; tick again; close the WO → gone; reopen → back.
   - **Link B, EG-ACME = prime, Demo = sub:** needed by ZP-4833 (a linked sub's Team on an EG-ACME planned-work schedule).
     - When it's active, write the link details to `docs/bug-evidence/2026-11-17-rfq/ZP-4830/LINKS.md`. The ZP-4833 tester watches that file.
   - **Customer link:** EG-ACME (provider) ↔ Demo (customer). Demo shares one QA-DEMO SLD. The chosen EG-ACME person opens exactly that SLD and no other Demo SLD (try another Demo SLD id → 403/404).
   - Re-check the 12 Nov FAILs:
     - Withdraw: dialog text, request gone after reload;
     - Accept / Decline / End link buttons;
     - bad email → 400/422.
   - Negatives:
     - public / egalvanic.com domain invites → nobody, identical responses;
     - field-team dialog: no duplicate sub row, save with nobody ticked;
     - edit-drawer removal revokes access.
2. **Guest menus on web.** For each guest state (Technician guest in Demo via the old Guests invite; sub-link guest from link A; customer-link guest):
   - record exactly what the web shows. In Demo, expect "Web Access Restricted". Check that this screen still lets the user switch back to their home company and sign out.
   - Check that typed URLs (/sites, /sessions, /assets, /admin…, /collaborators) don't leak host-company data.
   - Check /api/features/access, /api/auth/v2/me (permissions, field_limits) and list APIs (sites, sessions) while acting in Demo: only assigned sites/WOs, nothing else.
   - Compare with the same user at home in EG-ACME.
   - Every menu entry a guest sees that its permissions don't allow, or that the spec says guests mustn't have (Create Site, Start WO, Admin, Collaborators, Sales/Quotes, company settings, unassigned sites), is a finding.
   - Write a table: menu item | permission behind it | guest sees? | expected | verdict.
3. **Cross-platform:** write the exact iOS and Android guest checks into "MOBILE-CHECKS" in verdict.md. The device testers are already running the guest Technician in Demo for ZP-5143 / ZP-5144.

Output: `docs/bug-evidence/2026-11-17-rfq/ZP-4830/` and `docs/bug-evidence/2026-11-17-rfq/GUEST-MENUS/` (verdict.md, table, MP4, PNGs, Jira draft). Outline boxes only, no burned-in text.
