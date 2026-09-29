# ZP-4464: price check, new stage build re-test, bug ZP-4479

**Date:** 2026-09-29, 23:05–23:40 IST
**Prompts:** "i think the total price is wrong can you check" · "create a bug assign to avani" · "total Site-based in total not showing create a bug assign to avani"

## What was checked
1. **Foundation price (23:11, real stage data).** The "Annual fees" tile shows $25,000, which is the order's contract value. That follows the rule "Foundation and legacy = contract value". The Modules "Annual rate" column lists the rate card, which adds up to $64,000 (11,500 + 16,200 + 10,200 + 18,600 + 7,500), plus eg-ai Free $0. So the maths isn't wrong, but the page shows two totals that don't match and doesn't explain why. The page does no fee maths itself; it prints `fees.annual_cents` from the API.
2. **New stage build `index-BFc32lrB.js`.** Both owner notes are now live:
   - The banner colours follow the company theme: normal = `primary.dark` (#4f4f4f on acme), amber = `warning.dark` (#b26a00), red = `error.dark` (#aa2e25). The amber banner's white text is 4.24:1, under the 4.5:1 minimum.
   - Every banner has a close ✕ ("Hide until the next page load"). Closing hides it, nothing is saved to browser storage, and a refresh brings it back. Verified on 6 simulated states with the real stage frontend.
3. **Stage switched to Site-based (~23:25).** Order EG-SITE-001. The API returns `sites: []` and annual fees 0. The page shows 0 licensed sites, a $0 total and no Licensed sites table, but still says "Next site expiry 367 days". Filed **ZP-4479** (High, assigned to avani.patel, sprint Z-26-09-S3, Web v2.2.2, To Do, 3 screenshots).

## Test code changes
- `ZP4464SubscriptionDeepTest`
  - It now finds the new icon close button (aria-label "Close subscription notice") as well as the old "Dismiss" text button, and lists icon buttons by their label.
  - After closing, it takes a screenshot.
  - It does a real in-app click (a same-document marker proves there was no reload) instead of `driver.get`, which is a full reload.
- `ZP4464SubscriptionStageTest`
  - It saves the raw `/api/subscription*` answers **before** the "View subscription" skip, so a plan with no banner (Site-based) still leaves the full JSON.
  - It also captures the "Licensed sites", "Modules by site" and "Customer portal licences" sections.

## Why these changes (in depth)
- **The close check could not see the new button.** The first build had a text button, "Dismiss". The new build uses an icon-only `IconButton` whose `innerText` is empty. The old check matched on text, so it would have silently skipped every close test and reported nothing. Matching on `aria-label` is also what a screen reader uses, so the test now finds the control the way an accessible user would.
- **`open()` was not in-app navigation.** `driver.get` loads a fresh document, which throws away in-memory state. With the old build, the dismissal lived in `localStorage` and survived that reload, so the mistake didn't show. With the new in-memory design, the same step looked like "the banner came back after navigation", which was a false alarm. Setting a `window.__qaSameDoc` marker before the click and reading it afterwards proves no reload happened.
- **Evidence was lost when there was no banner.** The API dump sat behind the "View subscription" button check. When the plan changed to Site-based there was no banner, so no button, and the test skipped before saving anything. Saving first means the evidence always exists. That is how `sites: []` was found for ZP-4479.

## Evidence
- `test-output/zp4464-site-2330/`: the Site-based page and API (23:30)
- `test-output/zp4464-deep-build2/`: the new build's close and colour runs
- `test-output/zp4464-deep-build1/`: the old build's deep run
- `test-output/zp4464-foundation-2311/`: the Foundation API at 23:11
- Artifact v5: https://claude.ai/artifact/EhE9HsXzVG2W3QpTDbSNbx
- The ZP-4479 screenshots are kept locally in `docs/bug-evidence/zp4464-site-total/` and are not committed, because they show unreleased rate-card prices and this is a public repo.
