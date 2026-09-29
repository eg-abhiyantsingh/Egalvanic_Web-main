# ZP-4464 Subscription Module: design walkthrough before testing

**Date:** 2026-09-29 (IST morning)
**Ticket:** [ZP-4464](https://egalvanic.atlassian.net/browse/ZP-4464), "[Web] Subscription Module — Admin Billing, Plan Management & Access Control". In Progress, Web v2.2.2, no PR yet.
**Artifact:** https://claude.ai/artifact/6Cv6JGKEE6GZhU7qgYmxJa
**Prompt:** "all our changes is merge to qa now … ZP-4464 ticket we will test soon so understand full design"

## What was done
- Read the Claude Design project "Nav Model" at the **source** level, not from screenshots. Route: Nav Model ▾ ›
  All project files › `nav-model-app.js` (6,228 lines) › Open › Copy, then `pbpaste` to a local file.
  The canvas is a cross-origin iframe, so its DOM can't be read directly.
- Ran the design's own subscription code in node (with small stubs) for all 10 preview states. This gives the
  **exact** banner wording, colours, dates and prices for each state. Those are the expected results for the real build.
- Rendered the design locally at full size (Playwright, served from 127.0.0.1) and captured each state. This covered
  Platform Admin, Operations Manager (banner shown, no Subscription page) and a T2 portal user (banner shown).
- Re-read the ticket and both comments (no new ones). Searched eg-pz-frontend PRs (none for ZP-4464). Checked the live
  QA bundle `index-ypMk4Gu6.js` (last modified 28 Sep 16:58 UTC, the PR #1561 deploy). It has no subscription,
  Foundation or grace code. Its existing `/user/me/subscriptions` and `/account/{id}/subscriptions` calls are
  email-notification preferences, which only share the name.

## What the design says (short)
- Four plans: Foundation, Module-based, Site-based, Legacy.
- Foundation runs 12 months. The first 90 days have full access (Sales can extend). Then a 14-day grace, then only
  Ops Core works until the term ends. Data is kept, and eg-ai drops to Free.
- Foundation allows 3 sites and 1,500 assets across all sites. Module-based has no caps.
- The banner shows on every page for every user type, T1 and T2. Only Platform Admin gets Admin › Billing ›
  Subscription and the "View subscription" button.
- Foundation banners can never be dismissed. Other plans show a dismissible notice from 30 days before a term ends.

## Design problems found (in the artifact, with screenshots)
1. The banner says advanced features shut down after the full-access date, but the code locks them 14 days later.
2. During grace, eg-ai shows "Until <full-access end>", a date already past, while the modules say "Shuts down <grace end>".
3. The "Module-based · active" preview actually renders as Expiring (Engineering Core has 27 days left).
4. Legacy: the header says "renews automatically" but the banner says "Contact us to renew".
5. The module-based Renewal line quotes the whole-account renewal but dates it by one module's expiry.
6. The ticket text is older than the design: the "1,500 per account" and "site above 1,500" lines survive only on
   Legacy, the modules table gained a License term column, there are new cards, and the Contact sales / Order form
   buttons are gone.
7. Small: the "Grace" label is cut to "Grac" on the term bar.

Also flagged: Dharmesh's comment (banner only for OM and AM, API-gated) conflicts with Avani's comment and the design
(banner for all T1 and T2).

## Why it matters
Settling these before the build lands avoids filing design choices as bugs later. The 10-state table gives testers an
exact oracle, so testing can start the moment the build reaches QA.

## Not committed on purpose
The design source and the price-level harness output stay in the session scratchpad. This repo is public and the
design carries the internal rate card.
