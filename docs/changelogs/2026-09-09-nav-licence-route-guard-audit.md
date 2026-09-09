# Full-coverage audit of the promotion board — nav, licence and route guards

**Date:** 2026-09-09
**Time:** 15:40 – 17:05 IST
**Prompt:** "done check everything don't miss anything. and you can add more screenshot if needed"

---

## How this was checked

Rather than re-reading my own board, I ran a 6-agent audit workflow that reconstructed the product
from the *outside*: one agent extracted the entire navigation and router configuration from the
shipped QA bundle, one read every verdict file, one read the board's own source, one swept the
changelogs for features with no ticket. Three of the six agents finished before the session limit hit
(nav, board, changelogs); I did the reconciliation myself from their output and then **checked every
claim that mattered live in the browser** — which is what caught the one claim that was wrong.

## Result: three new High findings

**1. On the Free plan, the paid Maintenance Portal pages open by typing the address.** The LICENSE
selector (Free / Premium) correctly padlocks Condition Assessment, Maintenance Program and Compliance
in the menu — and those three pages then open by URL with real data. Compliance showed **638
deviations, 11 programme elements, a 0.6% compliance score** on a Free licence. The lock lives in the
menu filter; the routes carry no licence check.

Answering the question left open this morning: the selector writes
`eg.maintenancePortal.previewLicense` into **localStorage** (`no_license` = Free, `read_only` =
Premium), i.e. it is a client-side preview on this tenant; a real customer's plan comes from their
account record. That scopes *who can flip their own plan* but not the URL bypass.

**2. A page hidden from every role, fully editable by URL.** The Builder rail is filtered on
`company_data.manage` — **no role tested holds it**, and Project Manager has no Builder section at all
— yet `/issue-suggestions` opens on that seat with all ten sets and working Create Set / Import /
Export / edit / delete. The route only asks for `company_data.view`, which everyone has. These sets
are tenant-wide configuration.

**3. Twenty-one routes have no page-level guard.** `/assets`, `/issues`, `/slds`, `/opportunities`,
`/notes`, `/goals`, the four Engineering designation pages, `/custom-devices` … and `/agent`, an AI
page with **no menu entry anywhere**. This is the general case of the work-order scope gap found
earlier today.

Plus five more (Medium/Low): the role switcher uses a **different rulebook** than the router (per-role
exclusion map, consulted only after a switch); **three more role-NAME gates** (Arc Flash Readiness
moves category for Electrical Engineers; the portal appears off-tier for five named roles; the guards'
own name lists) in the release that renamed Admin ↔ Super Admin; **two page titles change with
permissions** (Work Orders → "Assessments", Customers → "Sites" — which explains an earlier confusion
in the multi-role work); "Pull-Through Work" is hard-coded English; the Admin → Organization group
skips the settings permission.

## One claim refuted by checking

The bundle defines no dedicated route for `/maintenance-portal/condition`, so the audit flagged the
portal's own Condition Assessment link as dead. **It renders correctly** — Overview, Findings 29, 274
assets — served by the parent layout route. Recorded as a non-defect. Also refuted: a
`/test-equipment` menu-vs-route divergence, which is latent rather than live because only Project
Manager holds its permission and Project Manager is in its role list.

## What changed on the deliverables

- **Board** (same URL): three new decision cards (11 total, was 8) · **a new section, "Every page in
  the product — and who can actually reach it"**, mapping all **84 menu entries across 7 sections**
  plus the five rules that make the product look different to two people · the LICENSE question
  answered on the portal card · **screenshots added to all eight original decision cards** (they were
  text-only) · counts re-derived against the ledger (66 verdicts, 14 defect-led).
- **New audit page** — https://claude.ai/code/artifact/e13e2861-33d2-43ae-9c08-2ebde7d4fa5d
- **New verdict file** `docs/bug-reports/2026-09-09-QA-nav-licence-and-route-guard-audit-verdict.md`
- **ZP-4123 verdict + page generalised**: it is one of several instances. The router gates pages
  **five different ways** and the menu uses a sixth; three more live divergences are named.

## Depth explanation

**1. Building the coverage list from the product, not from my inbox.** This is the fix for the miss
the owner caught earlier (2FA, Issue Suggestions, Maintenance Portal, Connections graph were all
absent). Extracting the nav config from the bundle gives a list that cannot be short: 84 entries,
7 sections, every gate named. Anything on that list with no verdict row is now visibly untested rather
than invisibly untested.

**2. The workflow's most valuable output was a wrong answer.** The agent read the router and concluded
`/maintenance-portal/condition` had no route, therefore a dead link — a plausible, well-evidenced,
*wrong* conclusion. One click refuted it. Two of the audit's candidate gaps died this way. Reading
minified code tells you where to look; only the product tells you what happens.

**3. Verifying in the direction that can prove a defect.** For the licence finding, the useful test was
not "does Free lock the menu" (it does) but "does Free stop the page" (it does not). Testing the
happy direction would have produced a PASS and closed the question.

**4. Restoring what I changed.** The licence test required writing a client-side value; it was set back
to Premium afterwards, and the verdict says so. Nothing on the server was touched.

## Files

- `docs/report-artifacts/2026-09-09-QA-nav-licence-route-guard-audit.html` (new, 205 KB)
- `docs/report-artifacts/2026-09-09-QA-V136-promotion-board.html` (rebuilt, 1.8 MB, 27 screenshots)
- `docs/report-artifacts/2026-09-09-QA-ZP-4123-role-based-access-rendering.html` (generalised)
- `docs/bug-reports/2026-09-09-QA-nav-licence-and-route-guard-audit-verdict.md` (new)
- `docs/bug-reports/2026-09-09-QA-ZP-4123-role-based-access-rendering-verdict.md` (generalisation appended)
- `docs/bug-evidence/v136-nav-licence-audit/` (5 QA screenshots)
- `docs/qa-review-board.html` rebuilt — 88 reports
