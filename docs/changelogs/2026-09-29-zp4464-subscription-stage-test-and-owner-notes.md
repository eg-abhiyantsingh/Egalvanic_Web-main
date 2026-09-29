# ZP-4464 Subscription module: tested on stage, plus the owner's two notes

**Date:** 2026-09-29, 19:40–22:35 IST
**Artifact:** https://claude.ai/artifact/EhE9HsXzVG2W3QpTDbSNbx (v3)
**Build:** stage `index-DbeWys95.js` (frontend PR #1563 merged 14:06 UTC, live 14:08 UTC)

## Stage results (all 8 stage logins, headed Chrome, read-only)
- The banner shows on every page for all 7 web roles (Admin, Super Admin, PM, AM, EE, FM, Client Portal/T2). Technician has no
  web access at all.
- "View subscription" and Admin › Billing › Subscription appear only for Admin and Super Admin. Every other role gets Access Denied,
  including on the direct URL.
- The Foundation numbers match Avani's rules: full access to Dec 28, grace to Jan 11, term Sep 29 2026 – Sep 29 2027, Ops Core
  Included, advanced modules "Until Dec 28", eg-ai Free Included, fees $25,000 (contract), assets 814/1,500, sites 10/3 (red),
  one activity event. The API payload matches the page field for field.
- A US Central-time viewer sees the same dates. At phone width nothing scrolls sideways.
- To fix or confirm: the order number shows as "EG"; the site overage shows only in Included access; the banner implies shutdown on
  Dec 28 though grace runs to Jan 11; "Grace" is cut to "Grac"; at phone width the menu button covers the page title.

## Owner notes (checked against the live build)
1. **Must have: the banner must match the company theme.** The banner uses fixed colours (`#1565c0` / `#b45309` / `#dc2626`). The app's
   theme comes from `/api/company/alliance-config/<subdomain>`; for acme that is grey `#828282` (primary) and red `#bf1e2e`
   (accent). Suggested fix: use the theme's `primary.dark`. The fixed blue is the default theme's `primary.dark`, so unbranded
   companies don't change, and acme gets `#4f4f4f` (8.19:1 contrast). The raw `#828282` would be only 3.84:1.
2. **Good to have: a close button that comes back on refresh.** Today Foundation has no close, and other plans save the dismissal in
   localStorage, so it stays hidden after a refresh. Suggested fix: keep "closed" in memory only. This needs a decision, because it
   reverses "Foundation banners can't be dismissed".

## Test code
`ZP4464SubscriptionStageTest` (d7d48f5, 4d09086) runs once per login with -DUSER_EMAIL / -DUSER_PASSWORD. The runner and the seat
list live outside the repo (/tmp), so no passwords are committed.

## Not tested yet
Module-based, site-based and legacy plans; the renewal notice and dismissal; grace, locked and extended states; the flag switched
off; a company with no subscription. Each needs test subscriptions loaded on stage with the runbook.
