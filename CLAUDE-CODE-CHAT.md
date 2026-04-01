# Claude Code Chat Log (Compressed)

> Auto-updated summary of AI-assisted debugging sessions. Read this for full context when starting a new chat.
> Last updated: 2026-04-01 (Session 2)

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
