# Consolidated detailed report — combine all modules into ONE single-file HTML

- **Date:** 2026-06-18
- **Prompt:** "also in consolidated report combine all this into single file. like html file. so that i can see everything in single file and capture full screenshot" (screenshot of the `modules/` folder of per-module HTML reports).
- **Component:** `.github/scripts/consolidated-detailed-report.py`
- **Type:** Reporting / CI artifact ergonomics.

---

## Ask

The consolidated *detailed* report was an index page + an iframe that loads **one
module at a time** from a `modules/` folder (Asset_Management.html, Authentication.html,
…). You can't see everything at once or capture it in a single screenshot. The ask:
combine every module into **one HTML file** that shows everything on a single
scrollable page so it can be screenshotted (or saved as PDF) in one shot.

## Why it wasn't trivial

Each per-module report is an **ExtentReports Spark SPA** (`<body class="spa -report dark">`):
an app-shell with a left test-list, a right "viewer" pane, tabbed views, internal
scrolling, and JS-driven show/hide. Naively stacking them does **not** screenshot —
Spark shows one test at a time inside fixed-height, internally-scrolling containers.

I reverse-engineered the Spark DOM (walking it live in a browser) to ground truth:

```
.test-wrapper.row            (display:flex: left list + right viewer)
  .test-list (~395px)
    .test-list-wrapper.ps-container
      ul.test-list-item
        li.test-item              <- one per test (ALWAYS in the DOM)
          .test-detail            <- summary row (name / time / status)
          .test-contents.d-none   <- FULL detail, hidden via Bootstrap d-none
            .detail-body          <- step table + inline base64 screenshots
  .test-content (ps-container)    <- right-hand VIEWER (perfect-scrollbar)
```

Key findings that drove the implementation:
- Every test's full content is **statically present** in `li.test-item > .test-contents.d-none > .detail-body` — no JS needed to materialise it, just un-hide it.
- The right-hand `.test-content` viewer is a perfect-scrollbar element that **balloons to ~88,000px** when its overflow is unset — so it's hidden entirely.
- The list sits in a `display:flex` wrapper pinned to ~395px — must be un-flexed and widened.

## What changed

`consolidated-detailed-report.py` now emits **two** files into the same output dir
(both captured by the existing CI artifact upload — no workflow changes needed):

1. `Consolidated_Detailed_Report.html` — the original navigable index (unchanged,
   kept for backward compatibility; now links to the single file at the top).
2. **`Consolidated_Detailed_Report_SingleFile.html`** — NEW. One self-contained file:
   a summary header + jump-to-module nav, then every module embedded as an
   **auto-resizing `srcdoc` iframe**. A small **flatten shim** is injected into each
   iframe that: hides the app-shell chrome + the ballooning viewer pane + non-test
   views; un-flexes the wrapper and widens the list to full width; and forces every
   test's `.test-contents`/`.detail-body` visible (overriding `d-none`). Each iframe is
   sized to its true content height (measured from `.test-wrapper`, with a parent-side
   reconcile pass), so the whole page is **one continuous scroll with no nested
   scrollbars** — a single full-page screenshot / "Save as PDF" captures everything.

Using srcdoc iframes (not DOM re-parsing) means **zero data loss** and CSS stays
sandboxed per module — robust to any Spark content. No `bs4`/`lxml` dependency.

## Validation (rendered + screenshotted, not just compiled)

Generated from local `reports/detail-report` (Asset Management — has inline
screenshots; Authentication — 25 tests) and rendered in a real browser via Playwright:
- Iterated visually three times, fixing: a perfect-scrollbar height runaway
  (33k→sane), `d-none`-hidden step tables, the 395px width pin, and ~1.5k px of
  empty-gap height overestimation.
- Final: full-page screenshot captured all **9,951px** in one shot. Every test's step
  table renders flat and full-width; Asset Management's two inline screenshots render
  in full colour; iframe height matches true content to within the 28px padding; no
  empty gaps, no stray dashboard card.

## Notes

- Spark styling still loads from its CDN (jsdelivr), same as the original per-module
  reports — open online for full styling (content shows regardless).
- For the full 11-module CI run the page is very tall; a single PNG may hit browser
  capture limits, so the header recommends **"Save as PDF"** (paginates to any height).
  Locally generated now at `reports/consolidated-detailed/Consolidated_Detailed_Report_SingleFile.html`.
