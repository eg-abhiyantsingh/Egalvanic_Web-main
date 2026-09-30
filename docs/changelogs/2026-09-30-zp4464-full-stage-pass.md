# ZP-4464: full stage pass, in depth (30 Sep 2026, 13:14–13:56 IST)

**Prompts:** "test everything in stage" · "start testing subscription module in stage" · "in depth" · "you just need to check everything in subscription module and the data I can tell developer to change" · "[dev.stage login] stage save this too"
**Build:** `index-sIOLawvH.js` (unchanged since 00:00). Artifact v7: https://claude.ai/artifact/EhE9HsXzVG2W3QpTDbSNbx

## What ran
- 8 stage logins (`ZP4464SubscriptionStageTest`), 3 browsers at a time, then the six that ran during a data change were re-run one at a time.
- 12 simulated plan states × Admin and Account Manager (`ZP4464SubscriptionDeepTest.simulatedStates`), incl. two new scenarios: `e1_expired_cloudfront_masked` and `l2_legacy_midnight_utc`.
- Clean real-data checks + flag off (`realDataChecks`, `flagOff`) for both roles.
- The midnight-UTC Legacy scenario in a Chicago-timezone browser (`TZ=America/Chicago`).
- The second stage company, dev.stage.egalvanic.ai (seat saved in memory only).

## Stage data moved during the run
13:14–13:19 no subscription on acme → from ~13:25 a new Foundation row QA-1 (started Sep 20, 80 days left). Earlier: Legacy EG-LEG-001 at 10:32, Site-based EG-SITE-001 at 23:30 the day before. Both windows were captured; the no-subscription window is the live proof for ZP-4486.

## Results (short)
- All 4 filed bugs still present on this build, now with real-data evidence: ZP-4479 ($25,000 tile vs $64,000 list), ZP-4483 (marker 25 px from the edge, 2.42:1), ZP-4485 (✕ for all 7 web roles with `dismissible:false`), ZP-4486 (live at 13:16, and again on dev.stage).
- Owner notes verified again, and the gap closed: after closing the banner a real click to Assets changes the route without a reload and the banner stays hidden; refresh brings it back; the ✕ is keyboard-operable.
- Real data: 401 for no/forged token, foreign company id and X-Subdomain; one banner call per page load; axe 0 violations; CLS 0.04 / 0.08; flag off hides everything.
- **New finding N1:** term dates stored at midnight UTC show a day early to US viewers (Sep 30 – Sep 30 · 365 days in Chicago vs Oct 1 – Oct 1 · 366 days in India). Not filed yet (owner to decide).
- Still open from last night: module plan "Next module expiry" uses the term end; amber banner + red tile; legacy "auto-renew" vs "Contact us to renew"; "Grac" clipped; T2 customers see the banner (Q1); "Active" before the start date.
- Jira: real-data screenshots and comments added to ZP-4483, ZP-4485, ZP-4486 (no field or status changes).

## Test-code changes and why
- `ZP4464SubscriptionDeepTest`
  - `TIMELINE_JS`/`timeline()` (from the previous prompt) now runs in every simulated state and in the seat test; it measures the "today" marker instead of eyeballing it.
  - Keyboard check of the ✕ (focus + Enter). The first version crashed: `new Actions(driver)` can't take the framework's `SelfHealingDriver` (it doesn't implement `Interactive`), so the key press goes through the raw `ChromeDriver`.
  - The in-app navigation step now clicks a real page (Assets / Work Orders / Issues) and proves the route changed AND no reload happened. The previous version clicked the first visible link, which never left the dashboard.
  - **Override safety.** The simulated-answer fetch override is now removed in a `finally`, and `realDataChecks` refuses to run if `window.__qaSim` is still present. Why: when the keyboard step crashed mid-loop, the override stayed installed and the real-data probes that followed in the same browser were answered by the simulation ("no token → 200 with an expired banner"). Those numbers were discarded and re-run; without the guard they would have been reported as real.
  - Simulated answers may be a raw non-JSON body (`page.raw`, `contentType`) so CloudFront's HTML error page can be copied exactly.
- `ZP4464SubscriptionStageTest`
  - Raw API answers are saved before the "View subscription" skip (so a plan with no banner still leaves evidence).
  - Phone-width check falls back to CDP device emulation when chromedriver refuses to resize a maximized window ("failed to change window state to 'normal'").
  - Icon-only buttons (the ✕) are listed by their aria-label.
- Runner scripts (in /tmp, not the repo): the seat-label collision (EE and Super Admin both map to "stage") is handled by relabelling files between runs; a wrong path substitution in the first version was caught before it moved anything.

## Evidence folders
`test-output/zp4464-full-1349` (8 seats), `zp4464-deep-full-1346` (24 simulated runs), `zp4464-live-couldnt-load-1316` (no-subscription window), `zp4464-l2-ist` + `zp4464-l2-chicago` (timezone), `zp4464-deep` (clean real-data + Chicago + dev.stage), `zp4464-stage` (re-runs + dev.stage). Bug screenshots in `docs/bug-evidence/zp4464-*` (local only: prices).
