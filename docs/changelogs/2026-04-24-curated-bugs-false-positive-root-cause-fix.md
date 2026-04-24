# 2026-04-24 — Curated bug tests were FALSELY PASSING. Root causes found via live Playwright verification.

## Prompt
> ✅ BUG003/004/005/006 ... are u sure this is passing

## Model in use
Claude Opus 4.7 (1M context), `effortLevel: max`, always-thinking on.

## Branch safety
`main` of QA framework repo. Production repo untouched.

## Your instinct was correct

You challenged "are you sure these are passing" and **you were right**. I manually logged into acme.qa.egalvanic.ai via Playwright MCP and verified every one of BUG-003/004/005 is still present in the product. My CI tests were **all three falsely passing** due to three distinct test-logic bugs.

## Live verification summary

| Bug | Reproducible on live acme.qa? | Root cause in my test |
|---|---|---|
| **BUG-003** (Issue Class validation) | ✅ YES — drawer closes on submit with blank Issue Class | 2 test bugs: wrong textarea filled + nav-drawer matches "still open" selector |
| **BUG-004** (Raw API error leak) | ✅ YES — body.innerText contains `"Failed to fetch enriched node details: 400"` | Selenium's `WebElement.getText()` filtered it out; need JS `innerText` |
| **BUG-005** (No maxLength) | ✅ YES — title input has `maxlength: null`, accepts 2000 chars | Selenium's `getAttribute("maxlength")` returns DOM `.maxLength` property (`"-1"` when unset), not the HTML attribute (null). `-1` was treated as "maxlength set" and passed the `<= 1000` check |

## Evidence from live Playwright session

### BUG-003 evidence
1. Opened Create Issue drawer → 1 Title input, 1 Issue Class combobox (empty), 4 textareas (Description, Proposed Resolution, 2 hidden).
2. Filled Title (2000 chars), clicked Create Issue submit with Issue Class BLANK and PropRes BLANK → drawer stayed open with validation message "Proposed resolution is required" (NOT "Issue Class is required").
3. Filled Proposed Resolution, clicked Create Issue submit → **drawer CLOSED** with no validation message, no error toast. Form accepted submit with blank Issue Class.

**The bug is live.** Issue Class is not enforced as required even though the UI marks it with `*`.

### BUG-004 evidence
Navigated to `/assets/invalid-uuid-test-12345`, waited 6s, read `document.body.innerText`:
```
...
V1.21
Failed to fetch enriched node details: 400
IMPROVEMENT
Improvements in Andr...
```
Literal string match. The raw internal API error IS in the rendered page.

### BUG-005 evidence
Create Issue drawer, Title input:
```
maxlength: null          ← HTML attribute NOT set
maxlengthProp: -1        ← DOM .maxLength default (no limit)
valueLen: 2000           ← input accepted 2000 chars via native setter
```
Waited 2 additional seconds — value stayed at 2000 chars, confirmed React doesn't reset it.

## The 3 test fixes

### Fix 1: `testBUG003_IssueClassValidationGap`
**Problem (a)**: `textAreas.get(0).sendKeys("Regression proposed resolution")` filled the FIRST textarea, which is `placeholder="Describe the issue"` (the Description field). `placeholder="Describe the proposed resolution"` is the SECOND textarea. Result: Proposed Resolution was left blank, so the submit was rejected on PropRes validation — the drawer stayed open — my test interpreted this as "Issue Class validation caught it".

**Problem (b)**: "Still open" detection used `.MuiDrawer-root:not([style*='display: none'])`. The left sidebar navigation IS a `MuiDrawer`. So this selector ALWAYS matches, making `dialogClosed = false` regardless of the Create Issue drawer's actual state. `assertFalse(false)` = PASS forever.

**Fix**: Target textareas by placeholder (`textarea[placeholder*='proposed resolution' i]` and `textarea[placeholder*='Describe the issue' i]`) to fill them correctly. Replace the buggy selector with JS that specifically looks for a drawer containing a Title input (unambiguously the Create Issue drawer).

### Fix 2: `testBUG004_RawAPIErrorLeaked`
**Problem**: `driver.findElement(By.tagName("body")).getText()` uses Selenium's text-extraction heuristic which can filter text based on CSS visibility. The error text renders in the app-update ticker near the sidebar version indicator — technically visible, but in a layout that Selenium's `getText()` evidently skipped. My subsequent `getPageSource()` fallback also missed it (possibly because pageSource returns HTML as the server responded, before client-side hydration added the error text).

**Fix**: Read via JS: `document.body.innerText` gets the real rendered text that a user sees. Also read `document.documentElement.outerHTML` via JS to get the post-hydration full HTML. Added one more marker variant (`enrichedNodeDetails` camelCase).

### Fix 3: `testBUG005_NoMaxLengthOnIssueTitle`
**Problem**: `titleInput.getAttribute("maxlength")` in Selenium 4 returns the DOM `.maxLength` property when the HTML attribute is absent. For an input without maxlength, that property defaults to `-1`. So `maxLen = "-1"`, `maxLenSet = true`, `maxLenInt = -1`. My tightened rule checked `maxLenInt > 1000` for "unreasonable" — which is false for -1. `bugPresent = false || false = false`. `assertFalse(false)` = PASS.

**Fix**: Read raw HTML attribute via JS: `arguments[0].getAttribute('maxlength')` — returns null if no `maxlength=""` in HTML, regardless of DOM property. Also: reject `"-1"` as a sentinel meaning "no limit set" in the `maxLenSet` check. Added diagnostic logging of both HTML attr + DOM property so future failures surface with full context.

## Before/after: what happens on next CI run

| Test | Before | After |
|---|---|---|
| testBUG003_IssueClassValidationGap | PASS (false-positive: filled wrong textarea + nav-drawer pollutes selector) | FAIL with evidence: "form CLOSED without error = validation did NOT catch the missing required field" |
| testBUG004_RawAPIErrorLeaked | PASS (false-positive: Selenium getText missed visible rendered text) | FAIL with evidence: `body.innerText[Failed to fetch enriched node details]` |
| testBUG005_NoMaxLengthOnIssueTitle | PASS (false-positive: DOM .maxLength=-1 sentinel misread as "set") | FAIL with evidence: `htmlMaxlength=null domMaxLengthProp=-1 valueLen=2000` |

## What I did NOT change
- **BUG-006 (slow page loads)**: env-dependent timing. CI network is faster than user's; pages genuinely load under thresholds in CI. Not a test-logic bug — a measurement-env fact.
- **BUG-002 & BUG-007**: already correctly FAILing with "Cannot verify" prereq.
- **BUG-001 & BUG-008**: already correctly FAILing with product evidence.

After this commit, expected curated result: **7 FAIL, 1 PASS** (BUG-006 only, which is fast-in-CI-only).

## In-depth explanation (for learning + manager reporting)

### Why live verification caught what automation missed
Three subtle framework quirks, each a different failure mode:

1. **Selenium `getText()` ≠ JS `innerText`**: Selenium's `getText()` is a Selenium-specific heuristic with its own visibility rules. For a user-facing bug report, what the USER SEES (innerText) is the right truth. When in doubt, execute JS directly — it's what the browser evaluates.

2. **Selenium `getAttribute()` ≠ HTML attribute**: Selenium 4's `getAttribute(name)` is actually `Element.getAttribute()` with fallback to the DOM property. For `"value"` this is desired (current value, not the initial value). For `"maxlength"` it's a trap — the property default `-1` masquerades as "set to -1". Use `js.executeScript("return el.getAttribute('maxlength')")` to get the raw HTML attribute unambiguously.

3. **CSS selectors that accidentally match framework chrome**: `.MuiDrawer-root` doesn't mean "the modal drawer I care about" — it means "any element MUI styled as a drawer, including the permanent nav sidebar". When writing selectors for modal/dialog detection, target something the nav doesn't have (like a Title input, or a close-button with a specific aria-label).

### Pattern: "assertion should fail on expected path"
All 3 tests had a logic gap where the expected-failing path was disguised as passing. The cure is to write assertions such that:
- PASS requires AFFIRMATIVE evidence of correctness (e.g., "I saw the form reject with message X")
- FAIL triggers on EITHER bug present OR verification impossible

This is what my earlier "Cannot verify — …" guards did for BUG-002/007. The same discipline applied to BUG-003/004/005 would have caught these earlier.

## Rollback
`git revert <this-commit>` restores the 3 false-positive-passing tests. Not recommended.
