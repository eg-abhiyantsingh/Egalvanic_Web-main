# eGalvanic Web QA Automation Suite

Comprehensive Selenium WebDriver test automation for the [eGalvanic Platform](https://acme.qa.egalvanic.ai) - an electrical asset management SPA built with React and Material UI (MUI).

## Tech Stack

| Component | Version | Notes |
|-----------|---------|-------|
| Java | 11 (compile) / 17 (CI) | Maven 3.x build |
| Selenium WebDriver | 4.29.0 | Chrome, EAGER page load strategy |
| TestNG | 7.8.0 | Test orchestration, priority-based ordering |
| ExtentReports | 5.1.2 | Dual report system (QA detailed + Client clean) |
| REST Assured | 5.4.0 | API tests |
| Target App | React SPA | MUI components: DataGrid, Drawers, Dialogs, Autocomplete |

## Project Structure

```
src/main/java/com/egalvanic/qa/
  constants/AppConstants.java          # Config, env vars, credentials (5 roles)
  pageobjects/                         # Page Object Model (7 page objects)
    AssetPage.java                     # Largest: kebab menu, edit drawer, grid search
    IssuePage.java                     # CRUD, native confirm() for delete
    WorkOrderPage.java                 # CRUD, IR Photos upload
    ConnectionPage.java                # CRUD, MUI dialog delete
    LocationPage.java                  # Building/Floor/Room hierarchy
    LoginPage.java                     # PageFactory + React nativeInputValueSetter
    DashboardPage.java                 # PageFactory, site overview
  utils/
    ExtentReportManager.java           # Dual report (ThreadLocal, Module>Feature>Test)
    ScreenshotUtil.java                # File + base64 capture
    EmailUtil.java                     # Gmail SMTP with client report attachment

src/test/java/com/egalvanic/qa/
  testcase/
    BaseTest.java                      # One browser per class, login/site, error recovery
    *SmokeTestNG.java                  # 7 smoke classes (37 TCs) - critical path
    AssetPart{1-5}TestNG.java          # 295 TCs - asset CRUD, core attributes, subtypes
    Connection{Smoke,TestNG,Part2}.java # 74 TCs - connection management
    Issue{s,}*TestNG.java              # 109 TCs - issue lifecycle
    Location{Smoke,TestNG,Part2}.java  # 80 TCs - building/floor/room hierarchy
    WorkOrder{Smoke,TestNG,Part2}.java # ~120 TCs - work order lifecycle
    SLD*.java, Task*.java, etc.        # Additional modules
    api/                               # 5 API test classes (17 TCs)
  listeners/
    ConsoleProgressListener.java       # GitHub Actions progress bar
```

## Test Suites

| Suite | XML File | Test Count | Purpose |
|-------|----------|------------|---------|
| Smoke | `smoke-testng.xml` | 37 TCs | Critical path validation |
| Full | `fullsuite-testng.xml` | ~1,000+ TCs | Comprehensive regression |
| Per-Module Smoke | `smoke-{module}-testng.xml` | Varies | Isolated module testing |
| Bug Hunt | `bughunt-testng.xml` | 60 TCs | Targeted bug verification |

### Running Tests

```bash
# Smoke suite (default)
mvn clean test

# Full suite
mvn clean test -DsuiteXmlFile=fullsuite-testng.xml

# Single module smoke
mvn clean test -DsuiteXmlFile=smoke-asset-testng.xml

# Full suite specific modules via CI
# Use GitHub Actions workflow_dispatch with module selection
```

## CI/CD

- **GitHub Actions** on `ubuntu-latest`, JDK 17, headless Chrome
- **Workflows:**
  - `smoke-tests.yml` — workflow_dispatch with module selection, 35-min timeout
  - `full-suite.yml` — full regression, per-module parallel jobs
- **Artifacts:** ExtentReports (detailed + client), surefire reports, failure screenshots

## Architecture Decisions & Known Patterns

### MUI Edit Drawer DOM Structure

The edit drawer (opened via kebab menu > "Edit Asset") has this field layout:
- **BASIC INFO**: Asset Name (textbox), QR Code (textbox), Asset Class (combobox), Asset Subtype (combobox), COM, Location
- **CORE ATTRIBUTES**: Dynamic per asset class. Fields are `<p>` label + `<div>` containing `<input>` as siblings inside `<div class="MuiBox-root css-8atqhb">`
- **Labels are NOT inside MuiFormControl/MuiTextField** for Core Attributes - they use a flat MuiBox layout
- Some labels are lowercase (e.g., `manufacturer`, `configuration`, `voltage`) while others are capitalized (e.g., `K V A Rating`, `Power Factor`, `Serial Number`)

### Field Lookup Strategy (Priority Order)

```
1. findInputInDrawerByLabel()  — Drawer-scoped: MuiDrawer//p[label]/following-sibling::div//input
2. findInputByPlaceholder()    — Global: input[@placeholder contains label]
3. findInputByLabel()          — Global: label text ancestor MuiFormControl//input (CAUTION: can match wrong element)
4. findInputByAriaLabel()      — Global: input[@aria-label contains label]
```

**Critical:** `findInputInDrawerByLabel()` must be used FIRST for Core Attribute fields. The generic `findInputByLabel()` uses `contains()` on text content which matches high-level ancestor divs and returns wrong inputs (e.g., the NOTES textarea instead of the intended field).

### Kebab Menu (Three-Dot / MoreVert) Detection

The kebab button has no `aria-label` or `data-testid`. Detection strategy:

```
Strategy 0b (fastest): Find SVG path starting with "M12 8c1.1" (MoreVert icon),
                       traverse to closest button, click it.
```

### React Native Value Setter

React controlled inputs ignore Selenium `sendKeys()`. Must use:
```javascript
var setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set;
setter.call(input, newValue);
input.dispatchEvent(new Event('input', {bubbles: true}));
input.dispatchEvent(new Event('change', {bubbles: true}));
```

### MUI Escape Key Behavior

**NEVER send `Keys.ESCAPE` to dismiss focus inside the edit drawer** unless a dropdown is actually open. If no dropdown/popover is open, Escape propagates to the MUI Drawer and **closes the entire edit form**. Instead, click the "Edit Asset" heading (`<h6>`) to safely dismiss focus.

### Asset Grid Search Ambiguity

Searching for an asset class name (e.g., "Panelboard") in the grid may return assets where the term appears in other columns (parent asset name, asset name, etc.). The `clickRowWithAssetClass()` helper iterates grid cells to find an exact match on the Asset Class column before clicking.

### Subtype Dropdown Behavior

- Subtype options vary per asset class and **do NOT include a "None" option**
- An unset subtype shows as empty string `""`, not "None"
- Assets are reused across CI runs, so subtypes may be pre-set from prior runs
- Tests verify the field exists and current value is a valid option (or empty)

### Delete Confirmation Patterns

1. **Native `window.confirm()`** - Issues module. Handle with `driver.switchTo().alert().accept()`
2. **MUI Dialog** - Assets, Connections. Use `[role="dialog"]` selector, NOT `[role="presentation"]`
3. **NEVER use `[role="presentation"]`** in dialog detection - MUI DataGrid uses it for structural elements

## Recent Fix History

### Escape Key Closes Drawer (a5be530)
- **Root cause:** `selectDropdownValue()` sent `Keys.ESCAPE` when no dropdown options found, closing the MUI Drawer
- **Fix:** Click "Edit Asset" heading instead of sending Escape

### Subtype Assertion Failures (c7645a4)
- **Root cause:** Tests expected `"None"` as default subtype, but no "None" option exists; assets retain subtypes from prior CI runs
- **Fix:** Validate current value is a valid option (or empty) instead of demanding "None"

### Wrong Asset Class Navigation (a970753, 66a38fd)
- **Root cause:** Grid search for "Panelboard" returned Circuit Breakers with Panelboard parents; code clicked first row blindly
- **Fix:** `clickRowWithAssetClass()` matches exact Asset Class column text

### Kebab Menu Not Found (a7ea14d)
- **Root cause:** MoreVert button has no aria-label; previous strategies found 0 candidates
- **Fix:** Strategy 0b matches SVG path `"M12 8c1.1"` for MoreVert icon

### Generator Tests Had No Assertions (a7c42aa)
- **Root cause:** Tests called `editTextField()` + `saveAndVerify()` but just logged "pass" regardless of outcome
- **Fix:** Added `Assert.assertNotNull`, `Assert.assertEquals`, post-save `readDetailAttributeValue()` verification

### Stale Element in Edit Fields (75e9e4f)
- **Root cause:** React re-renders DOM after scroll, causing stale element references
- **Fix:** Re-find elements after each scroll via `findInputInDrawerByLabel()`

## Debugging Tips

### Using Playwright/Chrome DevTools MCP for Live Inspection

When a test fails, use browser automation tools to inspect the live DOM:
1. Navigate to the asset page
2. Search for the asset class
3. Click into detail page
4. Open edit drawer via kebab menu
5. Take a11y snapshot to see exact field labels, types, and values
6. Verify XPath selectors work against the live DOM

### Common Failure Patterns

| Pattern | Likely Cause | Fix |
|---------|-------------|-----|
| "Field not found" | Label mismatch (case, spacing) | Check live DOM for exact `<p>` text |
| Drawer closes unexpectedly | Escape key sent without open dropdown | Use heading click instead |
| Wrong asset selected | Grid search ambiguity | Use `clickRowWithAssetClass()` |
| Save fails silently | Required fields missing | Check Core Attributes mandatory fields |
| Stale element after scroll | React re-render | Re-find via drawer-scoped XPath |
| Delete dialog not dismissed | Using `role="presentation"` selector | Use `role="dialog"` only |

## Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `TEST_EMAIL` | Yes (CI) | Login email |
| `TEST_PASSWORD` | Yes (CI) | Login password |
| `BASE_URL` | No | Override base URL (default: `https://acme.qa.egalvanic.ai`) |
| `BROWSER` | No | Browser type (default: `chrome`) |
| `HEADLESS` | No | Run headless (CI only, never for local dev) |
