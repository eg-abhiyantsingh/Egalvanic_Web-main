# ZP-4548 — Simplify Subscription Page, tested on stage (2 Oct 2026)

**Prompt:** "https://egalvanic.atlassian.net/browse/ZP-4548 test this ticket in deepth".

Web v2.2.3 hotfix ticket (Highest, In QA, Avani). Frontend PR #1601 + backend #1416 on cicd/stag → tested on
acme.stage.egalvanic.ai (build `index-CBsky6m0.js`, web V2.2). QA (`index-B1nOPIG3.js`, V1.36) still has the old code and
was used as the "before".

Page: https://claude.ai/artifact/LxxHdpB2CiFitoRRfG2sff

## Verdict
All five acceptance criteria are met on stage. One question for the owner: AC5 names the third tier **"No License"**, but
the build (and Avani's fix comment) calls it **"Free"** everywhere. The Jira status stays **In QA** until that is answered.
No Jira edits were made.

## What was checked
- **Legacy (AC1–AC3):** QA acme's real legacy contract (QA-1, $30,000) on the old build shows "Rate card · Sept 2026"
  (×3), a Renewal row, "Renewal options · Module-based plan" and 5 Included-access rows. The same values rendered by the
  stage build (API answer replaced in my browser only) show none of that: Subscription card Plan / Term / Value, fee tile
  "Renews Jun 24, 2027", Included access Sites / Assets / Support. The new `Subscription.jsx` (from the stage sourcemap) has
  no `rate_card` reference at all.
- **AC4:** the T2 licenses card renders after Included access and Terms on the real Site-based and real Foundation plans
  and on the simulated Legacy and Module-based plans.
- **AC5:** Full Access / Lite / Free in Manage License, Create Customer, account details, Add Contact hints ("Choose Full
  Access or Lite first.", "…determined by the account's Lite license."), Guest Portal Users, the Maintenance Portal
  LICENSE picker and its Free lock message, and the T2 card. No tier-name "Interactive" / "Read-only" / "No license" left
  in any of the 303 stage JS files. The remaining hits are the ELK layout library, form-builder blocks and read-only modes.
- **Foundation design:** real plan EG-FND-005 (stage acme switched from Site-based to Foundation between 18:13 and 18:24
  IST) shows the tiles, the Subscription card with "90 days left", and the Modules card with no $. The coming-soon and
  Services rows are hidden. Simulated: ≤14 days (amber), extended +30, grace ("Shuts down in 9 days"), locked, ended,
  upcoming.
- **Modules card:** no price, total or rate-card chip on Site-based (real), Foundation (real) and Module-based
  (simulated) plans. The fee tile is unchanged.
- **T2 card:** columns, header counts, $8,100 / yr, rates and sort order. Portal users match Guest Portal Users
  (4/2/1/0/0). The pager works against the real `/api/subscription/t2-accounts` (page size forced to 2). Avani's
  107-account example: 50 / 5 / 52, $124,500 / yr, 11 pages.
- **T2 API:** no auth gets 401. Page ≤0 → page 1; past the end → empty list; page_size 0 → 10; page_size >100 → 100;
  non-integer → 400 "page and page_size must be integers". No duplicate or missing accounts across pages.
- **Roles (Selenium, `ZP4548RoleProbe`, visible Chrome, 2 at a time):** Admin and Super Admin see the page, the T2 card
  and the T2 JSON. AM, CP, PM, FM and EE get "Access Denied" and only the CDN's HTML from both APIs. Technician gets "Web
  Access Restricted". 8/8 PASS.

## Small notes (not blocking, not filed)
- French: "Définissez d'abord une licence." and "Ce compte n'a pas de licence…" were not updated with the English text.
- Free is described three ways: "No portal access…", "Site Overview and the latest report only" and "Free shows Site
  Health and the latest report".
- If a T2 page fails to load, the card stays on the current page with no message (forced 500 in my browser).
- At a 1366 px window, the Foundation tile row wraps and Annual fees sits alone on a second row.

## Not a product bug
Sentry `EGALVANIC-REACT-APP-5XN` (staging, "Failed to execute 'removeChild'", 1 event) was caused by my own script: it
deleted a tooltip element that React owned. Safe to ignore or resolve.

## Files
- `src/test/java/com/egalvanic/qa/testcase/ZP4548RoleProbe.java`: new read-only per-seat probe (page + both APIs,
  asserts admin-only T2). Compiled to `/tmp/zp4548/jc`; runner `/tmp/zp4548/run-roles.sh` (seats from
  `/tmp/zp4464stage/seats.txt`).
- Evidence (local only, has prices): `docs/bug-evidence/2026-10-02-zp4548-stage/` (30 images) and
  `test-output/zp4548-roles/`.

---

## Second pass — "test in deepth" + "check the design check for all role too" (2 Oct 2026, 18:45–19:30 IST)

**Real states while the developer switched setups.** `ZP4548StateWatcher` (Selenium, admin seat) polled
`/api/subscription` every 30 s and ran the ticket's checks on each new real state: Legacy **EG-LEG-005** (13 checks) and
Foundation **EG-FND-005** (25 checks), 0 failures. Real legacy on stage now confirms AC1–AC3 without simulation: the fee tile
reads "Renews Oct 2, 2027", the Subscription card is Plan / Term / Value, and there is no Renewal row and no Renewal options card.

**Design check** (Claude Design project d65a4ec6, files read via the editor's own GetFile call, view-only):
- Legacy matches the design except inside the T2 card. The design says "No License" ("2 Full Access · 2 Lite · 1 No
  License", "No License is free"), has a **License term** column, and mutes the No License rows. The build uses "Free" and
  has no License term column.
- The Foundation design has no $ anywhere. It has no Annual fees tile, no contract value in the header and **no T2 card**;
  the design chat says "I left the T2 licenses card off because it lists prices". It also uses the title "Foundation
  license" and the tile labels Full access / Term ends / Managed assets. Its states are "Ending soon" (≤14 days, amber)
  and then "Limited" with "Expired · Ended <date>"; the build has grace → "Locked". Its Terms rows differ from the build's.
  The Modules card, the footer note and Included access match.
- The Nav Model labels the T2 user types Full Access (FM) / Lite (CP) / No License. In the build, roles keep their names.
- The design chat's own ticket text says to check the "No License" tier and its $0 price with the business.

**All roles** (`ZP4548RoleSurfaces`, 8 seats): no old tier names for any role.
- Subscription and Guest Portal Users: Admin and Super Admin only.
- Manage License (EG ADMIN): staff only.
- AM, PM, EE and FM can open New Customer with the tier choice. This is older behavior from the 9 Sep audit; the Nav
  Model gives Customers only to PM, EE and AM.
- Maintenance Portal license picker: Free / Lite / Full Access for staff, AM, PM and EE; not shown to FM or CP.
- Technician: web access restricted.

**More simulated checks (no surprises):**
- Guest Portal Users chips render Lite and Free.
- T2 activity entries use the new names.
- The T2 card is hidden when there are no accounts or no `t2` data.
- Long customer names are cut with "…" and have a full-name tooltip.

Jira still **In QA**; questions for the owner listed on the page.

**Extras (`ZP4548ExtrasProbe`, admin seat):**
- **Phone width (420 px):** the T2 table is 420 px wide inside a 364 px card, so the Annual rate column is cut off
  ("$2,40…"). The Modules card fits, and the page doesn't scroll sideways.
- **America/Chicago:** the real Foundation plan's midnight-UTC dates show one day earlier (Oct 1 → Dec 30, "89 days")
  than in India (Oct 2 → Dec 31, "90 days"). This was documented as intended in ZP-4464.
- **Updates:** `/release-updates` is a DevRev PLuG / Beamer widget in cross-origin frames, so its entry text can't be
  read from the app. The design's "Updates entry text" rename is DevRev content.

Page republished (v2): https://claude.ai/artifact/LxxHdpB2CiFitoRRfG2sff. New probes (uncommitted):
`ZP4548RoleSurfaces`, `ZP4548StateWatcher`, `ZP4548ExtrasProbe`.

---

## Issues page — "got any bug" / "artifcat i told you to create for me for the issue" (2 Oct 2026)
https://claude.ai/artifact/K1RnvT9QufBLgckkkEEH8s: two low bugs and four decisions, each with real screenshots, numbered
steps and Actual / Expected. Nothing filed in Jira.

- **Bug 1:** in French, the Portal Access hint on a Free account still says "Définissez d'abord une licence." Confirmed live
  by switching my browser's language to French (`preferredLanguage=fr`, restored to en-GB afterwards); the tier itself does
  read "Gratuit".
- **Bug 2:** the Free tier is described three different ways (Manage License, T2 card, Maintenance Portal tooltip).
- **Decisions:** A "No License" vs "Free"; B no License term column; C prices on the Foundation page; D Foundation
  ending / after-90-days states.
- **Removed on the owner's request** ("we dont need to check in phone", "removve that issue"): the phone-width finding, from
  both pages (main page now v3). The silent T2 paging error is left off the issues page because it can't be reproduced in
  the real UI on stage (5 accounts, so no pager).

**Watcher finished (18:51–20:06 IST, 4 polls with a new state, 0 failed checks):**
- Legacy EG-LEG-005.
- Foundation EG-FND-005.
- Foundation **QA-1 with eg-ai Starter** (started 22 Sep, 80 days left): "eg-ai Starter → Expires in 80 days", so the paid AI
  tier follows the window on real data.
- One blank answer mid-switch (19:51); the re-check found QA-1 and passed.

Main page republished as v4.
