# Asset "Select or Create Location" modal — automated coverage of the Building › Floor › Room cascade

- **Title:** Modelled and tested the asset-creation Location picker (a cascading MUI Select dialog)
  that had no real coverage — including the enable/disable gating and an end-to-end create.
- **Date:** 2026-06-25
- **Time:** 09:30
- **Prompt:** Asset-creation workflow spec (Basic Info + the "Select Location" → "Select or Create
  Location" Building/Floor/Room flow).

---

## What changed (plain summary)

Added a full page-object API for the asset **"Select or Create Location"** modal and a 6-test class
(`AssetLocationTestNG`) that exercises it: opening the modal, the **Building → Floor → Room** cascade,
the confirm-button gating, the real building list, applying a location to the drawer, cancelling, and
an end-to-end create-with-location that persists. Wired it into the existing `asset-1-2` CI group.

## Depth explanation (for learning + manager review)

**Discover-live-then-assert.** Before writing a single locator I drove the real app (Playwright MCP)
through the whole flow and dumped the DOM at each step. That turned guesses into facts: the picker is
a MUI **Dialog** (not the autocomplete the Work Order page uses), the three fields are MUI **Selects**
with `aria-disabled` cascade gating, the confirm button shares the exact label "Select Location" with
the drawer's trigger, and the option lists live in a body-level `ul[role=listbox]`. Each of those facts
became a precise assertion or a scoping decision in the page object. Writing tests first and discovering
later is how you get flaky locators; this order is the opposite.

**The trusted-event trap.** MUI `<Select>` opens its menu on a *trusted* pointer event. A JavaScript
`element.click()` — which the codebase leans on heavily to dodge MUI backdrops — does **not** open it
(`aria-expanded` stayed false in discovery). So the location methods deliberately use Selenium
`Actions().moveToElement().click()` for the Select triggers and the option `li`s, while still using JS
for scrolling. Knowing *when* a synthetic click is and isn't acceptable is the difference between a
method that works and one that hangs for 25s.

**Label collision → scope everything.** Both the drawer trigger and the modal's confirm button read
"Select Location". Every modal query is scoped under
`//div[@role='dialog'][.//*[normalize-space()='Select or Create Location']]`, and the drawer trigger
is scoped with `not(ancestor::div[@role='dialog'])`. Without that scoping the two buttons are
indistinguishable and the tests would interact with the wrong one.

**Iterate-to-green taught the real failure modes.** The first headed run was 2/6. None of the failures
were the modal logic (LOC_03/LOC_04 — the option list and the full select-and-apply — passed first
try). They were *harness* bugs worth calling out:
- `openCreateAssetForm()`'s success check matches the always-present toolbar "Create Asset" text, so it
  reports success even when the drawer didn't open. Fix: a real `isCreateDrawerOpen()` (Asset Name field
  visible) with retry.
- **State carryover** — a prior test left the drawer open with a location applied; the next test reused
  it. Fix: always re-navigate (the away-and-back tears the drawer down) so every test starts clean.
- **Test-data choice** — picking the *first* building hit one whose floor has no rooms. Real data, wrong
  choice for a cascade test. Fix: drive the verified-complete B1 › F1 › R1 chain explicitly.
These are exactly the bugs that, left in, make a suite "flaky" in CI. Catching them on a headed local
run (per the no-headless rule) is the point of validating instead of just compiling.

**Site scoping for deterministic data.** The class scopes itself to "Android Qa Site1" (verified to
have a complete B1 › F1 › R1 chain) so the assertions have stable ground truth, while
`selectFirstAvailableLocation()` remains as a site-agnostic fallback for reuse elsewhere.

## Validation

_Headed (no headless), site "Android Qa Site1"._ **6/6 PASS — BUILD SUCCESS** (`Tests run: 6,
Failures: 0`). LOC_01 93s · LOC_02 16s · LOC_03 13s · LOC_04 22s · LOC_05 15s · LOC_06 51s.

The end-to-end create (LOC_06) was independently confirmed against the live app: a real-keystroke grid
search shows the created asset with its location columns populated —
`LocTest_20260625_141505 · Circuit Breaker · B1 · F1 · R1`. The earlier "not found" failure was a
broken *verification* (JS-typed search doesn't trigger the grid filter), not a broken create — exactly
the kind of false negative that headed validation catches and "it compiled" would not.
