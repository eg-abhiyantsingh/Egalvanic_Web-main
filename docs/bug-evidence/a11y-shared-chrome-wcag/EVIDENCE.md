# BUG-B, pinned down: two WCAG violations live in the SHARED sidebar/chrome, so every page inherits them

**Severity:** MEDIUM (WCAG 2.1 AA — one `critical`, one `serious`)
**Environment:** `acme.qa.egalvanic.ai`, badge **V1.36**
**Verified:** 2026-08-08, live DOM inspection on two unrelated routes
**Reported by:** `TC_ACC_14`, `TC_OPP_43` (dedicated a11y tripwires, tagged `known-product-bug`)

## What was previously unclear

BUG-B was recorded as "app-wide WCAG violations" with no node-level ownership, so it was impossible
to tell whether the violations were **eGalvanic's own markup** or came from the **third-party widgets
the app embeds** (Beamer, DevRev — both whitelisted in the app's CSP and both present in the DOM).
That distinction decides whether it is our defect at all.

## Finding — the violating nodes are eGalvanic's own components, not third-party

Beamer and DevRev **are** on the page (`beamer: true`, `devrev: true`), but **no violating node
belongs to either**. Every one traces to app-owned MUI markup:

| axe rule | Impact | Node | Owner |
|---|---|---|---|
| `listitem` | serious | `li.MuiListItem-root` — text **"Legacy Procedures"** — parent is `div.MuiBox-root`, inside `MuiDrawer-root` / `nav` | **app sidebar** |
| `button-name` | critical | `button.MuiFab-root` directly under `div#root`, no accessible name | **app (global FAB)** |
| `button-name` | critical | `button.MuiIconButton-root` (icon-only, no `aria-label`) | **app (page-level)** |
| `color-contrast` | serious | 1 node | app |

## Proof it is SHARED chrome, not per-module

The same probe run on two unrelated routes returns the sidebar violation **byte-identically**:

```
/customers : strayLi = ["Legacy Procedures [sidebar]", "Legacy Procedures [sidebar]"]
             namelessButtons = IconButton, Fab
/assets    : strayLi = ["Legacy Procedures [sidebar]", "Legacy Procedures [sidebar]"]
             namelessButtons = IconButton, IconButton, Fab
```

`listitem` and the `Fab` `button-name` are therefore **one defect each**, not one per module. The
per-page `IconButton` count does vary (1 on `/customers`, 2 on `/assets`), so some `button-name`
violations really are page-specific.

## The two concrete fixes

1. **Sidebar** — wrap the nav items in a `<ul>` (or give the containing `div` `role="list"`).
   A `<li>` whose parent is a `<div>` is invisible to screen-reader list navigation.
   The item involved is **"Legacy Procedures"**.
2. **Global FAB + icon buttons** — add `aria-label` (e.g. `aria-label="Create"`). An icon-only
   `MuiFab`/`MuiIconButton` with no text and no label is announced as just "button".

## Why this changed the test suite

Because the violations sit in shared chrome, a whole-page axe scan re-reported them in **every**
module that ran an a11y check. Two consequences, both fixed on 2026-08-08:

- **Duplicate noise** — 2 real defects surfaced as N failures across modules.
- **Lost functional coverage (the worse one)** — `TC_OPP_30` ("Detail opens healthy; quote editor
  tabs render") ran `verifyAccessibility()` *before* its tab walk, so the shared sidebar bug threw
  every single run and **the quote-editor tabs were never actually tested**. The a11y check now runs
  last, scoped to the page's own markup.

`A11yVerifier` now offers both scopes:
- `assertNoBlockingViolations` — whole page. Used by the dedicated a11y tripwires (`TC_ACC_14`,
  `TC_OPP_43`), which is where whole-page reporting belongs. **These stay legitimately red.**
- `assertNoPageSpecificViolations` — excludes sidebar/drawer/FAB/third-party. Used by functional
  tests, so they only go red on their own page's markup.

Failure messages now include each node's CSS target, so an a11y red is directly actionable instead
of reading "2 node(s): Buttons must have discernible text".

## Status
`TC_ACC_14` and `TC_OPP_43` are **correctly red** and should stay red until the two fixes above land.
They are already tagged `groups = {"known-product-bug"}`, so they report as known reds, not surprises.
