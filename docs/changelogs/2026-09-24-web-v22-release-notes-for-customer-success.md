# Web V2.2 release notes for customer success (2026-09-24)

**Prompt:** "share artifacts - what's new web v2.2", then "take example of this that you already created in past for
web 2.1 version" (the V2.1 Release Notes page) and "you need to create
https://egalvanic.atlassian.net/projects/ZP/versions/14156/tab/release-report-all-issues for this".

**Published:** <https://claude.ai/artifact/XEn8wHNw9mRG4ahL3onxTT> — "What customers get in V2.2" (tab title
"V2.2 Release Notes"). Private until shared from its Share menu. Same structure and styling as
<https://claude.ai/artifact/Hq4i2PsWXSN9fcGgYz3Tp4> (V2.1): features grouped by product area, each with a plain
description, "Where to find it" steps, a real QA screenshot and the ticket key.

Evidence: `docs/bug-evidence/2026-09-24-v22-release-notes/` — 21 cropped captures, NOTES.md, page template.

## How the list was built

1. Read Jira fixVersion 14156 live: **78 tickets** (40 READY TO RELEASE · 25 Ready for QA · 3 In QA · 2 In Progress ·
   7 To Do · 1 Backlog). New since yesterday: ZP-4338, ZP-4353, ZP-4360, ZP-4372, ZP-4373.
2. Kept only user-visible features in READY TO RELEASE, then **re-captured each one live on QA by clicking through
   the app** (no typed URLs), on `index-DQkd_M4m.js` and — after QA deployed mid-session — `index-B26Vje7o.js`.
3. Dropped anything I could not show working: ZP-4149 (the report chooser defaults to a QA test config), ZP-4174
   (Bus Duct not re-checked), ZP-4186 / ZP-4322 (only "not reproducible"), ZP-4181 (web half found unfixed 23 Sep),
   ZP-3675 (blocked, no panel with circuits), ZP-4344 / ZP-4176 / ZP-4216 (internal).

## Contents — 20 features in 8 areas

| Area | Features (ticket) |
|---|---|
| Links from emails | Work-order link opens the right WO on the right site (ZP-4030) |
| Site walks | Journal walks + Journal Review; long notes save (ZP-4068, ZP-4208) |
| Maintenance | Open Issues by Type + MAINTENANCE menu (ZP-4038) · programme search (ZP-4044) · plain past-date deferral message (ZP-4212) · Compliance Visualizer (ZP-4112) |
| Condition assessment | Overview reorganised, Assessment-expired bucket, auto revisions (ZP-4043/4041/4084) · Findings pencils + Asset Details filters/Bulk Ops (ZP-4039) · separate issue / assessment reports (ZP-4113) |
| Work orders | General first (ZP-4347) · IR box on in-scope child assets at 0 photos (ZP-4309) · Mark As on location view (ZP-4062) |
| Services & devices | All methods + Edit N selected (ZP-4109) · Rename from the service menu (ZP-4152) · device-specific rules under Admin → Devices (ZP-4110/4111) |
| Assets & engineering | Junction Box hides panel-only fields (ZP-4167) · breaker manufacturer list, 52 options (ZP-4170) · core attributes per subtype (ZP-4171) · Settings Verifier in SLD Export (ZP-4131) |
| Customers | Paged, server-side customer search that matches site names (ZP-4165) |

Plus **Also fixed** (ZP-4059, 4159, 3990, 4066) and **Not ready yet — please don't promise these** (ZP-3919/4353
emailed-code sign-in, ZP-4042/4061 customer portal, ZP-4151 multi-account sites, ZP-4272 column memory on WO grid,
ZP-4292/4291 services on new assets, ZP-4218 RFQ, ZP-4338 sort across pages, ZP-4153/4207 journal interpretation).

## Why the "not ready" list is on a customer-success page

V2.1 only listed working things. V2.2 has several headline items in the Jira release that do not work on QA yet
(passwordless sign-in, the customer portal). Customer success reads Jira too; naming them in plain words stops
anyone promising them to a customer on Friday.

## Noticed, not investigated

- Site Overview "Open Issues by Type": 87 open, but the legend's seven types add up to 36.
- Android Site 2: 370 assets in Admin → Customers, 367 on the Site Overview.

## Test data

Nothing saved. Create Asset / Create Work Order / Add device / Generate Report / Pricing setup were cancelled; the
Defer to a past date was refused by the server. Selections ticked on the All methods table were UI-only.
