# Claude Code Chat Log (Compressed)

> Auto-updated summary of AI-assisted debugging sessions. Read this for full context when starting a new chat.
> Last updated: 2026-04-05 (Session 7)

---

## Session: 2026-04-05 (Session 7) — @BeforeMethod Cascade Prevention + Headless Timing Fixes

### Context
CI run 24008066386 (Session 6 Keys.ESCAPE fixes) still in progress. While waiting, investigated the 3 biggest failure categories from baseline run 24004733217: Task cascade (27), WorkOrder+Issue (10), SLD (2). Root cause analysis revealed a systemic issue across ALL test modules.

### Root Cause: TestNG @BeforeMethod Cascade

**The Problem:** Every test class calls `ensureOnXxxPage()` from `@BeforeMethod` without try-catch. If `driver.get()` throws a `TimeoutException` or the grid/page doesn't load, the exception propagates up → TestNG marks `@BeforeMethod` as FAILED → **all remaining tests in the class get SKIP status (0ms duration)**. One transient failure kills 27+ tests.

**The Fix:** Wrap `ensureOnXxxPage()` in try-catch with dashboard round-trip recovery:
```java
try {
    ensureOnXxxPage();
} catch (Exception e) {
    driver.get(BASE_URL + "/dashboard");  // clear stuck state
    pause(3000);
    driver.get(MODULE_URL);               // retry
    pause(6000);
    waitForGrid();
}
```

### Files Modified (8 test classes)

| File | Changes |
|------|---------|
| TaskTestNG | Cascade prevention + pause 4s→6s + grid waits 15s→20s/10s→15s |
| SLDTestNG | Cascade prevention + page load 3s→5s |
| WorkOrderTestNG | Cascade prevention with grid retry |
| WorkOrderPart2TestNG | Cascade prevention with grid retry |
| IssueTestNG | Cascade prevention via issuePage.navigateToIssues() |
| ConnectionTestNG | Cascade prevention via connectionPage.navigateToConnections() |
| LocationTestNG | Cascade prevention via locationPage.navigateToLocations() |
| AssetPart3TestNG | Dropdown timing: accordion 800→1500ms, scroll 300→600ms, click 500→800ms, retry 1500→2500ms, detail read 2→3s, drawer close detection improved |

### Commits

| Commit | Description |
|--------|-------------|
| ec32cbf | Add @BeforeMethod cascade prevention + headless Chrome timing fixes |

### CI Runs
- 24008066386: Session 6 fixes only (Keys.ESCAPE purge + 360min timeout) — in progress
- 24008217849: Session 6 + 7 fixes (cascade prevention + timing) — triggered

### Baseline Comparison (from run 24004733217)
| Group | Module | Baseline Failures | Expected After Session 6+7 |
|-------|--------|-------------------|---------------------------|
| 1 | Auth+Site+Connection | 3 | 0 (Keys.ESCAPE + cascade prevention) |
| 2 | Location+Task | 27 | 0-2 (cascade prevention eliminates skip chain) |
| 3 | WorkOrder+Issue | 10 | 0-3 (Keys.ESCAPE + cascade prevention) |
| 4 | Asset Parts 1-2 | 0 | 0 (already clean) |
| 5 | Asset Part 3 | 2 | 0 (timing fixes) |
| 6 | Asset Parts 4-5 | never ran | should run now (360min timeout) |
| 7 | SLD | 2 | 0-2 (cascade prevention, but simulated events still fragile) |
| 8-10 | Dashboard+BugHunt+Load+Smoke | never ran | should run now |

---

## Session: 2026-04-05 (Session 6) — Full Suite Keys.ESCAPE Purge Across All Modules

### Context
Continuation of Session 5. CI run 24004733217 triggered on Session 5 code (180min timeout). While waiting, performed comprehensive audit of ALL test modules for `Keys.ESCAPE` — a known MUI hazard where Escape key propagates through DOM and closes drawers/dialogs unexpectedly.

### Codebase-Wide Keys.ESCAPE Audit

**Before: 35 total occurrences (24 in test files + 11 in page objects)**

**Test files (24 occurrences across 9 files):**
- AssetPart1TestNG: 5 (3 HIGH — body.sendKeys, 2 MEDIUM — input.sendKeys)
- ConnectionTestNG: 2 (both HIGH — body.sendKeys in delete dialogs)
- LocationTestNG: 1 (HIGH — body.sendKeys after cancel)
- TaskTestNG: 2 (MEDIUM — search/dropdown input.sendKeys)
- WorkOrderTestNG: 3 (1 HIGH — body.sendKeys on filter, 2 MEDIUM — input.sendKeys on dropdowns)
- WorkOrderPart2TestNG: 7 (4 HIGH — body.sendKeys on filters/forms, 3 MEDIUM — input.sendKeys)
- SiteSelectionTestNG: 2 (LOW — facility combobox, no drawer)
- SiteSelectionSmokeTestNG: 1 (LOW — facility combobox)
- DashboardBugTestNG: 1 (LOW — search field, no drawer)

**Page objects (11 occurrences across 4 files):**
- AssetPage: 5 (dismissPopup, dismissAnyDrawerOrBackdrop, dismissAnyDialog, closeAddAssetPanel)
- LocationPage: 2 (dismissAnyDrawerOrBackdrop)
- IssuePage: 2 (dismissAnyDrawerOrBackdrop)
- WorkOrderPage: 2 (dismissAnyDrawerOrBackdrop)

**After: 4 safe occurrences kept (SiteSelection + DashboardBug — no drawer context)**

### Replacement Patterns Used
| Old Pattern | New Pattern | When Used |
|------------|-------------|-----------|
| `body.sendKeys(Keys.ESCAPE)` | Click Cancel/No button, then MuiBackdrop-root | Dialog dismiss |
| `body.sendKeys(Keys.ESCAPE)` | Click header/h5/h6 element | Filter/popover close |
| `input.sendKeys(Keys.ESCAPE)` | Click drawer heading (e.g., "Add Asset", "Add Task") | Dropdown close in drawer |
| `input.sendKeys(Keys.ESCAPE)` | `search.clear()` + click heading | Search field |

### Commits

| Commit | Description |
|--------|-------------|
| 6dbdcf3 | Remove dangerous Keys.ESCAPE from 6 test files (20 occurrences) |
| e7c2cb2 | Remove Keys.ESCAPE from page object dismissAnyDrawerOrBackdrop methods (11 occurrences) |

### CI Runs
- 24004733217: Full suite on Session 5 code (baseline)
- 24006585710: Full suite with test-file Keys.ESCAPE fixes
- 24006670306: Full suite with complete fix (test + page objects)

---

## Session: 2026-04-02 (Session 5) — Deep Audit + saveAndVerify Hardening + CI Textarea Consistency

### Context
Continuation of Session 4. Comprehensive deep audit of ALL 208 asset tests across AssetPart2/3/4/5 to verify field lookups, subtype handling, and save reliability.

### Findings from 4-Part Parallel Audit

**CLEAN across all files:**
- Zero `Keys.ESCAPE` in Part2/3/4/5
- All `verifyAssetSubtype` callers use `null`, not `"None"`
- All dropdown closures use heading-click pattern
- All edit/select methods have drawer-scoped lookup + stale-element re-find
- Autocomplete empty-string retry present in all parts

**Medium (non-blocking) findings:**
- `expectedDefault` parameter in `verifyAssetSubtype` is dead code — never asserted against (all parts)
- Part4 Relay `Manufacturer` uses `editTextField` while Motor uses `selectFirstDropdownOption` — potential freetext-vs-dropdown mismatch
- Part4 Motor inline subtype check has `"None"` in expected list (soft-logged only, no assertion)

### Fixes Applied

**1. Part3 `verifyAssetSubtype` rewrite** — replaced soft-logging with proper Assert:
- `Assert.assertNotNull` for subtype field
- `Assert.assertTrue` validating current value against actual dropdown options
- Fixed all 6 callers from `"None"` to `null` (GEN, JB, LC, MCC×2, MCCB)
- Added missing `ArrayList` import

**2. `saveAndVerify` hardened in Part2/4/5** — replaced fixed `pause(2000)` with Part3's proven approach:
```java
// Poll for drawer close up to 10 seconds
for (int i = 0; i < 20; i++) {
    pause(500);
    Boolean drawerGone = (Boolean) js.executeScript(
        "var d = document.querySelector('.MuiDrawer-anchorRight .MuiDrawer-paper');"
        + "return !d || d.getBoundingClientRect().width === 0;");
    if (Boolean.TRUE.equals(drawerGone)) break;
}
// + page refresh after successful save
```

**3. Strategy 6 (CI textarea fallback) added to Part2/3/4** — matching Part5's complete 6-strategy set in `findInputInDrawerByLabel`

### Commits

| Commit | Description |
|--------|-------------|
| c1195e1 | Rewrite Part3 verifyAssetSubtype to match Part4/5 robust approach |
| 89540cc | Harden saveAndVerify + add CI textarea fallback across all asset parts |

### CI Runs
- 23899076240: Full suite on pre-Session-5 code (in progress)
- 23899706230: Full suite on Session 5 code (triggered)

---

## Session: 2026-04-02 (Session 4) — Drawer-Scoped Field Lookup + Autocomplete Trigger

### Context
User reported `testSWB_05_EditAmpereRating` not working — test opened edit drawer but immediately went to save without editing anything. Log showed `saveChanges` called directly, meaning `selectFirstDropdownOption` and `editTextField` both returned null.

---

### Root Causes (Playwright-verified on live Switchboard edit drawer)

**1. Missing `findInputInDrawerByLabel()` in Part2/4/5**
Only Part3 had the drawer-scoped 5-strategy XPath lookup. Part2/4/5 used generic `findInputByPlaceholder` + `findInputByLabel` + `findInputByAriaLabel` which all fail for MUI Drawer Core Attribute fields because:
- Labels are `<p>` elements with asterisk suffixes (e.g., `"Ampere Rating*"`)
- Placeholder is `"Select..."` (not the field name)
- No `aria-label` attribute
- Fields use `<p>` + sibling `<div>` layout, not `MuiFormControl`/`MuiTextField`

**2. Asterisk in label text (`*` suffix)**
Required fields render as `"Ampere Rating*"` in the DOM but tests search for `"Ampere Rating"`. Exact match `normalize-space()='Ampere Rating'` fails. Fixed by using `starts-with()` instead.

**3. Server-populated autocomplete needs text input to trigger**
`selectFirstDropdownOption` clicks the combobox and immediately checks for `li[role='option']`. MUI Autocomplete fields that fetch from the server don't show options on click alone — they require an input event. Dispatching `value=''` + input event triggers the full option list (32 options for Ampere Rating).

### Switchboard Core Attribute DOM Discovery

| Label | Type | Strategy | Placeholder |
|-------|------|----------|-------------|
| Voltage | combobox | 2 (parent/sibling) | Select voltage |
| Ampere Rating* | combobox | 2 (parent/sibling) | Select... |
| Catalog Number* | text | 1 (following-sibling) | (empty) |
| Configuration | combobox | 2 (parent/sibling) | Select... |
| Fault Withstand Rating* | combobox | 2 (parent/sibling) | Select... |
| Mains Type* | combobox | 2 (parent/sibling) | Select... |
| Manufacturer* | combobox | 2 (parent/sibling) | Select... |
| Notes | text | 1 (following-sibling) | (empty) |
| Serial Number | text | 1 (following-sibling) | (empty) |
| Size | text | 1 (following-sibling) | (empty) |
| Voltage* | combobox | 2 (parent/sibling) | Select... |

### Fixes Applied

**`findInputInDrawerByLabel()` added to Part2/4/5** (Part3 updated to `starts-with`):
```java
// Strategy 1: starts-with + following-sibling (standard text fields)
drawerPrefix + "//p[starts-with(normalize-space(),'" + label + "')]/following-sibling::div//input"
// Strategy 2: starts-with + parent/following-sibling (combobox layout)
drawerPrefix + "//p[starts-with(normalize-space(),'" + label + "')]/parent::div/following-sibling::div//input"
// + CI variants and textarea fallback
```

**`editTextField` + `selectDropdownValue` updated in all parts:**
- Use `findInputInDrawerByLabel` as primary lookup
- All re-find-after-scroll chains also use drawer lookup first

**Autocomplete trigger retry in all parts:**
```java
if (options.isEmpty()) {
    js.executeScript("...s.call(arguments[0],''); ...dispatchEvent(new Event('input',...));", input);
    pause(1500);
    options = driver.findElements(By.xpath("//li[@role='option']"));
}
```

### Commits

| Commit | Description |
|--------|-------------|
| 8f01d13 | Add findInputInDrawerByLabel to Part2/4/5 + fix autocomplete trigger |

---

## Session: 2026-04-01 (Session 3) — Comprehensive Escape→Heading + Subtype "None" Fix

### Context
Deep investigation of all AssetPart2/3/4/5 test files after CI showed 7 failures (134 pass, 7 fail, 0 skip). User requested thorough multi-part review divided into 5 parts.

---

### Root Causes Found

**1. `Keys.ESCAPE` closing MUI Drawer (all Asset Parts)**
Every `closeEditFormIfOpen()`, `verifyAssetSubtype()`, and subtype option test sent `Keys.ESCAPE` to dismiss focus or close dropdowns. When no dropdown was open, Escape propagated to the MUI Drawer and closed the entire edit form.

**2. Subtype "None" doesn't exist (Part4 + Part5)**
`verifyAssetSubtype("None", ...)` asserted the current value equals "None" or empty. But:
- "None" is never a real dropdown option
- Assets persist subtype values from prior CI runs (e.g., SWB → "Unitized Substation")
- Assertion failed: `"Default subtype should be 'None' but was 'Dry Transformer'"`

### Fixes Applied

**Escape→Heading Click (Part2/3/4/5):**
Replaced ALL `Keys.ESCAPE` and `body.sendKeys(Keys.ESCAPE)` inside MUI Drawer context with:
```java
WebElement heading = driver.findElement(By.xpath(
    "//div[contains(@class,'MuiDrawer')]//h6[normalize-space()='Edit Asset']"));
heading.click();
```
- Part2: 1 location (closeEditFormIfOpen)
- Part3: 2 locations (closeEditFormIfOpen + verifyAssetSubtype)
- Part4: 5 locations (closeEditFormIfOpen + verifyAssetSubtype + MOT_02 + PB_02 + REL_02)
- Part5: 6 locations (closeEditFormIfOpen + verifyAssetSubtype + SWB_02 + TRF_02 + UPS_02)
- Removed unused `import org.openqa.selenium.Keys` from Part3/4/5

**Part5 verifyAssetSubtype Rewrite:**
Replaced fragile "None" assertion with robust Part4-style approach:
- Validates current value is either empty OR a valid dropdown option
- Handles CI-persisted subtypes gracefully
- Skips "None" in expected options list
- All callers changed from `"None"` to `null`

**Part5 Subtype Options Corrected:**
- SWB_AST_01: Added "Unitized Substation", removed "None"
- TRF_AST_01: Added "Dry-Type Transformer", removed "None"
- UPS_AST_01: Removed "None"

**AssetPart1 Escape Usages — Verified Safe:**
Part1 uses Escape on the Create Asset panel (different from Edit MUI Drawer) and on confirmed-open dropdowns — no changes needed.

### CI Failures Fixed (all 7)

| Test | Error | Fix |
|------|-------|-----|
| Part4: MOT_AST_01 | "None" vs "Motor Control Equipment" | null default + valid-option check |
| Part4: PB_AST_01 | "None" vs "Panelboard" | null default + valid-option check |
| Part4: PB_AST_02 | "Should have None option" | Removed "None" from expected |
| Part4: REL_AST_01 | "None" vs "Solid-State Relay" | null default + valid-option check |
| Part5: SWB_AST_01 | "None" vs "Unitized Substation" | Rewrite + null default |
| Part5: TRF_AST_01 | "None" vs "Dry Transformer" | Rewrite + null default |
| Part5: UPS_AST_01 | "None" vs "Static UPS System" | Rewrite + null default |

### Commits

| Commit | Description |
|--------|-------------|
| 6ace3b4 | Fix Escape→heading-click in AssetPart3/4/5 + fix Part5 subtype verification |
| 33d88d2 | Fix Escape→heading-click in AssetPart2 closeEditFormIfOpen |

### CI Run Triggered
Run 23859234301 — full suite on commit 33d88d2

---

## Session: 2026-04-01 (Session 2) — GEN_EAD_09 Post-Save Verification Fix

### Context
GEN_EAD_09 (Edit Manufacturer) still FAIL after Escape key fix (a5be530). CI log showed save succeeding but test marked FAIL. User reported "this test is failing why it is working correctly i think".

---

### Root Cause (Playwright-verified with live API inspection)

**The save works perfectly.** Network inspection confirmed:
- PUT `/api/node/update/{id}` sends `"name": "manufacturer", "value": "Caterpillar"` in `core_attributes` array
- API returns 200 with the saved value
- Edit drawer shows the value correctly

**The failure is a race condition in post-save verification:**

1. `waitForEditSuccess()` has condition `ExpectedConditions.urlContains("/assets")` — this ALWAYS matches immediately because we're already on `/assets/{id}` detail page
2. So it returns `true` instantly without waiting for save to complete
3. `readDetailAttributeValue("manufacturer")` runs with only ~1s delay
4. React hasn't re-fetched from API yet → detail table still shows stale "Not specified"
5. `Assert.assertFalse("Not specified".equals(persisted))` → FAIL

**Key insight:** The detail table initially renders with pre-edit data. After save, React re-fetches from the enriched API endpoint (`/api/graph/nodes/{id}/enriched`). Until that completes, the table shows stale values.

### Fix (d075d8e)

**saveAndVerify():**
- Polls for edit drawer to actually close (`.MuiDrawer-anchorRight .MuiDrawer-paper` disappears)
- After success, forces `driver.navigate().to(detailUrl)` — full page reload guarantees fresh React state
- Added 3s pause after reload for page render

**readDetailAttributeValue():**
- Increased initial wait to 2s
- Added polling: up to 8 attempts × 1s for the Core Attributes table to render
- Better error logging per attempt

### Verification
Live Playwright test confirmed full flow:
1. Edit manufacturer via React setter → value set ✓
2. Save Changes → PUT returns 200 with value ✓
3. Page reload → detail table shows saved value ✓
4. `readDetailAttributeValue("manufacturer")` returns correct value ✓

**Files changed:** AssetPart3TestNG.java (saveAndVerify + readDetailAttributeValue)
**Commit:** d075d8e

---

### Additional Discovery: MUI Drawer DOM Structure

The left sidebar nav and right edit drawer are BOTH `MuiDrawer` elements:
- Left sidebar: `MuiDrawer-anchorLeft MuiDrawer-docked` (0 inputs, always present)
- Right edit form: `MuiDrawer-anchorRight MuiDrawer-modal` (16 inputs, opened on Edit Asset)

The XPath `//div[contains(@class,'MuiDrawer')]` matches BOTH. The `findInputInDrawerByLabel` works correctly because the left sidebar has no `<p>manufacturer</p>` label, so it only matches in the right drawer.

---

## Commits This Session

| Commit | Description |
|--------|-------------|
| d075d8e | Fix GEN_EAD post-save verification: reload page + wait for drawer close |

---

## Session: 2026-04-01 — Asset Test CI Fixes (Part 3, 4, 5)

### Context
Continuing from a prior session that fixed GEN_EAD tests (commit a7c42aa) and kebab menu detection (commit a7ea14d). This session focused on fixing remaining CI failures in AssetPart3/4/5 tests.

---

### Fix 1: navigateToAssetByClass clicks wrong asset (a970753, 66a38fd)

**Problem:** `testPB_AST_01_DefaultSubtype` navigated to a Circuit Breaker instead of a Panelboard. CI log showed `getAssetClassValue: Circuit Breaker`.

**Root cause:** Searching "Panelboard" in the MUI DataGrid returned rows where "Panelboard" appeared in ANY column (e.g., parent asset name). The code called `assetPage.navigateToFirstAssetDetail()` which blindly clicked the first row.

**Fix:** Added `clickRowWithAssetClass(String assetClassName)` helper that iterates grid cells with JS to find exact Asset Class column match:
```java
js.executeScript(
    "var rows = document.querySelectorAll('.MuiDataGrid-row[data-rowindex]');" +
    "var result = [];" +
    "for (var row of rows) {" +
    "  var cells = row.querySelectorAll('.MuiDataGrid-cell');" +
    "  for (var cell of cells) {" +
    "    if (cell.textContent.trim().toLowerCase() === arguments[0].toLowerCase()) {" +
    "      result.push(row); break;" +
    "    }" +
    "  }" +
    "}" +
    "return result;", assetClassName);
```

**Files changed:** AssetPart2TestNG, AssetPart3TestNG, AssetPart4TestNG, AssetPart5TestNG
**Commits:** a970753 (Part4), 66a38fd (Part2/3/5)

---

### Fix 2: verifyAssetSubtype expects "None" but fails (c7645a4)

**Problem:** PB_AST_01 assertion `"Default subtype should be 'None' but was 'Panelboard'"`.

**Root cause (Playwright-verified):** Navigated to live Panelboard edit drawer and discovered:
1. Subtype dropdown has **NO "None" option** — actual options: Branch Panel, Control Panel, Panelboard, Power Panel
2. An unset subtype is empty string `""`, not "None"
3. This asset already had `value="Panelboard"` from a prior CI run (PB_AST_03 sets it)

**Fix:**
- `verifyAssetSubtype()` rewritten: validates current value is a valid dropdown option (or empty) instead of demanding specific default
- All callers changed from `"None"` to `null` for expected default
- PB_AST_01 passes correct options: `"Branch Panel", "Control Panel", "Panelboard", "Power Panel"`
- PB_AST_02 hardcoded assertions updated with actual dropdown options
- MOT_AST_01, OCP_AST_01, PDU_AST_01, REL_AST_01 all fixed

**Files changed:** AssetPart4TestNG.java
**Commit:** c7645a4

---

### Fix 3: Escape key closes MUI Drawer (a5be530) — CRITICAL DISCOVERY

**Problem:** GEN_EAD_09 (Edit Manufacturer) fails. CI log shows kebab menu and edit drawer open successfully, then nothing.

**Root cause (Playwright-verified with live DOM inspection):**

1. `selectFirstDropdownOption("manufacturer")` finds the manufacturer input (correct — it's a plain `<input type="text">`)
2. Clicks it — no `<li role='option'>` appears (it's NOT a combobox)
3. Code sends `input.sendKeys(Keys.ESCAPE)` to "close dropdown"
4. **Since no dropdown was ever opened, Escape propagates to the MUI Drawer → drawer closes**
5. Fallback `editTextField("manufacturer", "Caterpillar")` can't find anything → returns null
6. `Assert.assertNotNull(val)` fails

**Verification steps in Playwright:**
```javascript
// After clicking text input and sending Escape:
{
  inputFound: true,
  hadDropdownOptions: false,
  drawerStillVisible: false,      // <-- DRAWER IS GONE
  hasSaveChangesButton: false     // <-- FORM IS GONE
}
```

**Fix:**
- Replace `input.sendKeys(Keys.ESCAPE)` with clicking drawer heading:
```java
WebElement heading = driver.findElement(By.xpath(
    "//div[contains(@class,'MuiDrawer')]//h6[normalize-space()='Edit Asset']"));
heading.click();
```
- Also fixed GEN_EAD_09 to skip dropdown attempt (manufacturer is a text field)

**Files changed:** AssetPart2TestNG, AssetPart3TestNG, AssetPart4TestNG, AssetPart5TestNG
**Commit:** a5be530

---

### Fix 4: README rewrite (1e8ff73)

Replaced outdated 22-test-case README with comprehensive documentation covering full 1000+ TC suite architecture, MUI interaction patterns, field lookup strategies, debugging tips, and fix history.

**Commit:** 1e8ff73

---

## Key Discoveries from Playwright Live Inspection

### Generator Edit Drawer Core Attributes
```
Voltage          — combobox (Select voltage), value "120V"
Ampere Rating    — textbox, no placeholder
configuration    — textbox (lowercase label!)
K V A Rating     — textbox (spaces between letters!)
K W Rating       — textbox (spaces between letters!)
manufacturer     — textbox (lowercase label!, NOT a dropdown)
Power Factor     — textbox
Serial Number    — textbox
voltage          — textbox (lowercase, second voltage field)
```

### Panelboard Subtype Options
```
Branch Panel | Control Panel | Panelboard | Power Panel
(NO "None" option — empty = unset)
```

### MUI Drawer Field DOM Pattern
```html
<div class="MuiBox-root css-8atqhb">           <!-- field container -->
  <p class="MuiTypography-root">manufacturer</p>  <!-- label -->
  <div>                                          <!-- input wrapper -->
    <input type="text" value="" />               <!-- the actual input -->
  </div>
</div>
```
- Labels are `<p>` elements, NOT inside MuiFormControl/MuiTextField
- Must use `findInputInDrawerByLabel()` which scopes to MuiDrawer + p[label]/following-sibling::div//input
- Generic `findInputByLabel()` matches ancestor divs containing the text and returns WRONG inputs

---

## Commits This Session (chronological)

| Commit | Description |
|--------|-------------|
| a970753 | Fix navigateToAssetByClass: exact class match in grid row (Part4) |
| 66a38fd | Apply clickRowWithAssetClass to Part2/3/5 |
| c7645a4 | Fix verifyAssetSubtype: handle persisted values, correct options |
| a5be530 | Fix Escape closing MUI Drawer in selectDropdownValue (all parts) |
| 1e8ff73 | Rewrite README with full architecture and debugging guide |

---

## Previous Session Commits (for reference)

| Commit | Description |
|--------|-------------|
| a7c42aa | Properly implement GEN_EAD tests with real assertions and post-save verification |
| a7ea14d | Add MoreVert SVG kebab detection (Strategy 0b) |
| 4914118 | Fix ECR_32 CancelAssetCreation: JS-click toolbar button |
| 2d1f118 | Fix AssetPart1 location field tests: button picker |
| 105580e | Add ensure{Building,Floor,Room}Exists guards to LocationPart2 |
| 591ad79 | Fix LocationPart2 cascading failures: rename-back + expandNode |
| 38fa204 | Fix ConnectionPart2 CI failures: search assertion + ensureConnectionExists |
| 75e9e4f | Fix StaleElementReference in editTextField/selectDropdownValue |
| 5e6e56f | Fix cascading test failures: URL check instead of stale flag |
| b99ae22 | Fix core attribute editing: select asset class + expand accordions |

---

## Critical Rules for Future Sessions

1. **NEVER send `Keys.ESCAPE`** inside MUI Drawer unless a dropdown is confirmed open → click heading instead
2. **NEVER use `[role="presentation"]`** in dialog detection → use `[role="dialog"]` only
3. **ALWAYS use `findInputInDrawerByLabel()`** for Core Attribute fields → generic lookup returns wrong elements
4. **ALWAYS verify asset class** in grid row before clicking → use `clickRowWithAssetClass()`
5. **Subtype "None" doesn't exist** → empty string = unset, validate against actual dropdown options
6. **Labels can be lowercase** → `manufacturer`, `configuration`, `voltage` (check live DOM)
7. **Labels have spaces** → `K V A Rating`, `K W Rating` (not "KVA", "KW")
8. **Kebab button** has no aria-label → detect via SVG path `"M12 8c1.1"` (MoreVert icon)
9. **After commit, always push** → user prefers auto-push without asking
10. **Never run headless locally** → only headless in CI

---

## Pending/Known Issues

- Some full-suite modules not yet verified on CI after latest fixes
- Other asset classes (Motor, Relay, OCP, PDU) may have similar label mismatches — verify with Playwright if tests fail
- `findInputByLabel()` (generic) is still used as fallback — may cause issues for fields where text appears in ancestor elements
