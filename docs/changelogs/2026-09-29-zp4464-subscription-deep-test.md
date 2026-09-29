# ZP-4464 Subscription module: deep test on stage

**Date:** 2026-09-29, 22:35–22:50 IST · **Artifact:** https://claude.ai/artifact/EhE9HsXzVG2W3QpTDbSNbx (v4)
**Prompt:** "Test everything in module deeply"

## How
- Stage's acme company has only a Foundation subscription, and QA has no stage database access. So the other 10 states were rendered by
  the **real stage frontend** from **simulated API answers**: a fetch() override for `/api/subscription/banner` and
  `/api/subscription`, built from acme's real payload with only the dates, plan and access codes changed
  (`src/test/resources/zp4464-sim/`). Nothing reached the server. This proves the UI, not the backend's state machine.
- Real data: banner fetch count across in-app navigation, layout shift, axe WCAG 2.1 A/AA, API refusal probes (GET only),
  LaunchDarkly blocked (flags default off), and a UTC+14 viewer.
- Unit: the shipped `subscriptionDates.js` in 5 timezones and across US daylight-saving.
- Test: `ZP4464SubscriptionDeepTest` (simulatedStates / realDataChecks / flagOff). Admin and Account Manager seats, headed Chrome.

## Passed
Wording, colour and role for all 10 states (amber at 14 days or fewer; red + role=alert for grace/locked/expired); Foundation never
dismissible; "View subscription" only for admins; the renewal maths ($19,158 and $9,991); extension shown as "+30 days"; the
no-subscription and flag-off states hide everything; one banner call across 6 page changes; axe 0 violations; 401 for no or
forged token; cross-company attempts (`?company_id`, `X-Subdomain: demo`) refused; dates follow the viewer's timezone.

## Findings
1. Module plan with nothing expiring: the "Next module expiry" tile shows the subscription end (305 days) instead of the
   earliest module end (45 days). The tile falls back to `subscription.end_at`.
2. Expired: the red banner links to a page that says "Subscription details aren't available", because the page API
   returns 404 once nothing is active.
3. Last 14 days: the banner is amber, but the tile is red.
4. Legacy: the tile says "auto-renew", while the banner says "Contact us to renew".
5. A dismissed notice stays hidden after a refresh (localStorage). This is the opposite of the owner's note 2.
6. The banner appears about 4 s after load and shifts the page down (CLS 0.04–0.08).
