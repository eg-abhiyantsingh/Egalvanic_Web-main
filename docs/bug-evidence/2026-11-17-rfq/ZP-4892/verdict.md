# ZP-4892 — [User Guide] Walkthrough

**Verdict: PARTLY**
The Tier 1 entry points work:
- auto-open once per user;
- Help → Getting started;
- "Here's how EG-ACME works" with the company name and brand colour;
- copy that matches the real menus;
- reduced motion.

Much of what the ticket describes is no longer in the product. ZP-5249 (eg-pz-frontend #1948, merged to QA) replaced it with the 5-workflow progress tracker. Gone are the 7-step / 4-phase journey graphic, the app mock with a spotlight, the form sketch, the "Start this tour" hand-off, and the whole Tier 2 per-screen walkthrough (graduation-cap icon on menu rows). This needs a PM/dev call: close ZP-4892 as superseded by ZP-5249, or bring Tier 2 back. The role and gating bugs found under ZP-5249 apply to this same guide.

- **Build:** `index-oFAZQ3rK.js` (QA, re-checked 11:58 IST, 17 Nov).
- **Seats:** SA, ADMIN, PM, AM, EE, TECH, FM, CP (PS has no web access). Company EG-ACME.
- **Flag:** LaunchDarkly `feature-user-tour` = true for EG-ACME (live evalx).
- **History:** RTR on 26 Oct on build index-B982OeHb.js; back In QA on 16 Nov with no fix version. Comments read: Dharmesh (PR #1798 frontend, PR #1653 backend), Nency's cut-off buttons (22 Oct, "fixed"), and our 26 Oct QA comment. That comment's follow-up bugs ZP-5006 and ZP-5007 no longer exist in Jira (both return "does not exist"), which fits with the per-screen walkthrough having been removed.

## Ticket points vs current build
| Ticket says | Current QA build | Result |
|---|---|---|
| Dialog opened from Help → Getting started | Yes (every seat with workflows) | PASS |
| Auto-shown once on first sign-in, remembered per user | Yes. Remembered per user **per browser** (localStorage); a new browser shows it again | PASS (same note as 26 Oct) |
| Welcome "Here's how {Company} works" | "Here's how EG-ACME works" | PASS |
| Framed as set it up / inspect / fix / prove | Now "One tour through all N workflows open to you… the progress card ticks off each step as soon as it's saved" | CHANGED |
| Journey graphic: 7 steps under Set up / Inspect / Fix / Prove, each clickable | 5 workflows (Customer & Site Setup, Asset Onboarding, Work Order Setup, Quote Creation, One-Line Diagram), each clickable; no phases. "Issues" and "Reports" are no longer steps | CHANGED (ZP-5249) |
| Per-step mock of the app with spotlight + numbered badge + coach card | A stage opens the real screen with a floating progress card of written steps | CHANGED |
| Sketch of the form each step opens | Not present | GONE |
| "Start this tour" → live on-screen walkthrough | No "Start this tour" anywhere in the dialog or card | GONE |
| Tier 2: graduation-cap icon on any menu row launches a per-screen walkthrough (spotlight, beacon, animated diagrams) | 0 caps across all 7 menus, no "tour" control on any screen checked. Layout.jsx says the caps were removed on purpose; Help is the only entry | GONE |
| Copy written from current code, e.g. no "Sites tab" | Menu names match: Admin › Customers, Site Data › Locations / Assets, Operations › Work Orders, Sales › Quotes, Engineering › SLD; button names checked on the real screens (see ZP-5249) | PASS |
| Tenant brand colour | Eyebrow, buttons, progress bars use EG-ACME's orange brand colour (real; on 26 Oct only simulated) | PASS |
| prefers-reduced-motion respected | Hover transform/transition = none under reduced motion | PASS |
| Nency 22 Oct: Start this tour / Back / Next cut off on the Assets step | That view no longer exists. New dialog at 1366×768: Maybe later bottom 712 px, content scrolls, progress card fits (bottom 748 px) | PASS (n/a) |
| LaunchDarkly `user-tour` gate (our 26 Oct question) | Web now reads it. OFF (simulated: evalx patched + LD stream blocked) → no auto-open, Help links to Z University | PASS (simulated) |

## Defects
No new defect filed under ZP-4892 itself. Four new bugs were found in the same guide and drafted under ZP-5249 (folder `../ZP-5249/`):
- another user's new site completes your step;
- the Facility Manager is sent to screens it cannot open (plus a 422 poll);
- the Client Portal gets an empty guide whose button throws;
- the company "Guided tours" switch OFF still auto-opens the guide.

## Not covered
- Demo company and French locale not checked.
- LD flag OFF simulated only.
- Mobile: web-only feature, not checked.

## Evidence
- Video: `QA-17Nov-ZP-4892-guide-vs-spec.mp4` (35 s)
- Screenshots:
  - `QA-17Nov-ZP-4892-1-welcome-company-name.png`
  - `-2-five-workflows-not-seven-steps.png`
  - `-3-progress-card-no-start-this-tour.png`
  - `-4-no-caps-on-menu-rows.png`
  - `-5-overview-1366x768.png`
  - `-6-card-1366x768.png`
- Raw: `p18.json`. Role sweep: `../ZP-5249/work/p3-*.json`. Tier-2 cap count: `../ZP-5249/work/p8.json`.
