# Plain-language bug pages with steps and real screenshots

**Date:** 2026-09-10
**Prompt:** *"proper screenshot of bugs that you found with steps front end screenshot more bugs are not clearly understanable try to use simple lanuage more save all this memory"*

## What was wrong with what I'd delivered

The open-defect register described 49 defects as **API route tables** — `GET /api/contact/by-sld/{id}` →
"returns another tenant's contact PII" — with **zero screenshots and zero steps**. Accurate but unusable
for anyone who has to reproduce or fix it. This was the **third** time this correction has landed, so it
is now saved as a hard rule: `feedback_bugs_need_screenshot_steps_plain_words`.

## Delivered

**Artifact:** https://claude.ai/code/artifact/86992e10-f895-4c12-8940-da109bbbcdb1

Six bugs, each **re-reproduced live today**, each with: a plain-language title, numbered clicks a
non-engineer can follow, a real screenshot of the broken screen, *What happens* / *What should happen*,
why it matters to a person, and the endpoint in a small "For the developer" line at the **end**.

1. **Sorting a list only sorts the 25 rows you can see** — 274 assets; Z–A puts `7N-H1-2` first, which is
   only the last of the loaded page. Proof: **0 network requests** on the sort click, and page 1 in A–Z
   order holds the alphabetically *first* 25, so a real Z–A sort could not start with one of them.
2. **A page hidden from the menu opens by address** — `/issue-suggestions`, fully editable.
3. **A padlocked feature opens by address** — Maintenance Portal Compliance, **638 deviations** on screen.
4. **Four engineering pages open without the permission** — 162 / 40 / 111 / 2 assets on a PM seat that
   lacks `features.equipment_designations.view`.
5. **Two Manufacturer boxes on one panel** — the ZP-3902 migration never ran on QA.
6. **Report setup says there are no infrared work orders** — while Work Orders → Closed finds **9**.

The cross-tenant family is described in plain words with an explicit note that it **cannot** be
screenshotted (no product screen ever asks for another company's id) and needs a second tenant.

## Verification changed two things

- **`/goals` no longer crashes.** An older verdict says it falls to the error boundary; it loads normally
  today. **Dropped** from the register, which now lists **5** refuted claims rather than 4.
- The sort bug needed two clicks to demonstrate: the first click is a no-op because the grid is already
  A–Z, which is why a careless check would call it fixed.

## Screenshot capability

`page.screenshot` had been timing out on these pages ("waiting for element to be stable" — they animate
continuously). Fixed by freezing animation over CDP first (`Animation.enable` +
`setPlaybackRate {playbackRate: 0}`), which is now in memory. All 6 captures succeeded.

## Footprint

Read-only. Every step is looking, not saving. Screenshots in
`docs/bug-evidence/2026-09-10-bug-screenshots/`.
