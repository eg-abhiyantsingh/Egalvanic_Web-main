# Arc Flash Readiness — automated coverage of the Engineering Mode / Asset Details / Asset Class workflow

- **Title:** Built a page object + 6 tests for the Arc Flash Readiness workflow (Engineering Mode toggle,
  Asset Details tab, Asset Class filter), passing 6/6 on the first headed run.
- **Date:** 2026-06-26
- **Time:** 09:00
- **Prompt:** Arc Flash workflow — /arc-flash → enable Engineering Mode → Asset Details tab → verify it
  stays on → filter by Asset Class.

---

## What changed (plain summary)

Arc Flash Readiness had only a "page loads" smoke and a dashboard-percentage check — nothing exercised
the actual workflow. Added `ArcFlashPage` (Engineering Mode toggle, tab switching, the Asset Class
MUI-Select filter, table reads) and `ArcFlashTestNG` (6 tests) covering dashboard + tabs render,
enabling Engineering Mode, the toggle persisting onto Asset Details, the Asset Class option list, the
table columns, and the full enable→tab→filter workflow. Wired into the `dashboard-bughunt` CI group.

## Depth explanation (for learning + manager review)

**Control taxonomy drove the locators.** Arc Flash mixes three different MUI control types, and each
needs a different interaction: the **Engineering Mode** toggle is a `Switch` (a real checkbox — JS click
toggles it and React's onChange fires, so reading `input.isSelected()` is the state of truth); the
**tabs** are `role="tab"` elements (with responsive duplicates, so the page object always acts on the
*visible* one and reads the active one via `aria-selected="true"`); the **Asset Class** filter is a
`Select` (a menu that opens only on a *trusted* pointer click — Selenium `Actions`, never a JS click).
Identifying which is which up front is why this passed first try.

**Persisted-state is a first-class assertion.** The spec explicitly says "verify Engineering Mode
remains enabled" after switching tabs. That's the kind of cross-step invariant that's easy to skip — so
AF_03 isolates exactly it (enable → switch to Asset Details → assert still on), and AF_06 re-checks it
mid-workflow. State that's supposed to survive a navigation deserves its own test.

**Assert what's observable, not what isn't.** Filtering by Asset Class doesn't echo the class name into
every row (the engineering-mode columns show ratings/manufacturer, not the class label), so asserting
"every row says Circuit Breaker" would be wrong. The robust signal is the filter's own value plus the
table re-rendering — AF_06 asserts `getAssetClassValue() == "Circuit Breaker"` and that the table is
still present. Choosing a checkable signal over a plausible-but-flaky one is the difference between a
test that means something and one that flakes.

**Reused the keystroke + discover-first playbook.** Site scoping uses real `sendKeys` (the ~130-site
virtualised picker ignores JS value-sets — carried over from the asset/work-order work), and the whole
DOM contract was discovered live before a line of assertion was written. 6/6 on the first run.

## Validation

_Headed (no headless), site "Android Qa Site1"._ **6/6 PASS — BUILD SUCCESS** (`Tests run: 6,
Failures: 0`). AF_01 1s · AF_02 1s · AF_03 2s · AF_04 2s · AF_05 7s · AF_06 14s (full workflow: Engineering
Mode ON → Asset Details, toggle persisted → filtered by Circuit Breaker).
