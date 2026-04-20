# Deep Bug Verification Round 2 — SLD, Issues CRUD, Accessibility

**Date:** April 20, 2026, 19:00–20:00 IST  
**Prompt:** "Find more better bugs — quality over quantity, deep interactive testing on SLDs and all pages"  
**Type:** Bug Verification & Testing (no code changes to production)

---

## What Was Done

### Goal
Round 2 of deep bug verification against LIVE eGalvanic platform (acme.qa.egalvanic.ai V1.21). Focused on previously unexplored areas: SLD interactions, Issues CRUD validation, Equipment Library, site switching, keyboard accessibility, and deeper form testing.

### Approach — 3-Part Interactive Testing

**Part 1: SLD + Initial Exploration (deep-test-round2-part1.js)**
- SLD page interaction — attempted to open "All Nodes" view
- Issues list page — found 113 items, tested "Add" button (matched 3 elements, none clickable)
- Attachments page — found 20 items, tested click-through
- Sales/Ops Overview — confirmed data renders
- Locations page — found 3 locations
- Work Orders — observed list rendering
- Condition Assessment — tested page load
- Site Selector analysis — identified MUI Autocomplete (role="combobox"), not native select
- Keyboard accessibility (Tab navigation) — **found focus indicator missing (BUG-007)**

**Part 2: Z1 Site + Equipment Library (deep-test-round2-part2.js)**
- Site switching to Z1 (MUI Autocomplete interaction)
- SLD on Z1 — loaded "All Nodes" view, found React Flow canvas with 133 SVGs
- Issues Create form — tested validation:
  - Empty submit → "Issue title is required" + "Proposed resolution is required" ✓
  - Title + Resolution only → **Issue Class NOT validated (critical finding)**
- Equipment Library — 675 items, searched "breaker", viewed ABB Formula detail (8 styles)
- Connections page — found 3 connections on Z1
- WO Planning, Commitments, Panel Schedules — all render correctly

**Part 3: Issue Class Validation Proof + Edge Cases (deep-test-round2-part3.js)**
- **Critical test**: Filled Issue form with Title + Proposed Resolution but NO Issue Class
  - Submitted → POST /api/issue/create returned **201 Created**
  - Issue appeared in list with Issue Class showing "—"
  - This is a frontend + backend validation gap
- Site switching Z1 → Yxbzjzxj confirmed working
- Beamer notification widget dismiss tested
- Task table sorting (by Due Date, by Name) — columns sort correctly
- Sidebar collapse/expand tested

---

## 2 New Verified Bugs Found (Round 2)

| # | ID | Severity | Bug | Page |
|---|-----|----------|-----|------|
| 6 | BUG-006 | MEDIUM | Issue created without required Issue Class — frontend + backend validation gap | /issues |
| 7 | BUG-007 | LOW | No visible keyboard focus indicator during Tab navigation — WCAG 2.4.7 failure | All pages |

### BUG-006: Issue Class Validation Gap (MEDIUM)
**Steps to reproduce:**
1. Navigate to Issues → click "Create Issue"
2. Fill only Title ("Test Issue Title") and Proposed Resolution ("Test resolution")
3. Leave Issue Class dropdown empty
4. Click Create

**Expected:** Validation error — "Issue class is required"  
**Actual:** Issue is created (HTTP 201). It appears in the list with Issue Class = "—". The backend accepted a record missing a field that is semantically required for issue categorization.

**Why this matters:** Issues without a class cannot be filtered, prioritized, or routed correctly. This creates data quality problems that compound over time. It's a dual validation failure — React form doesn't check it, and the API doesn't enforce it.

**Evidence:** Screenshot shows the issue in the list with "—" for Issue Class (`r2p3-issue-submit-no-class.png`)

### BUG-007: Keyboard Focus Indicator Missing (LOW)
**Steps to reproduce:**
1. Navigate to any page (e.g., /dashboard)
2. Press Tab key 10+ times

**Expected:** Each focused element has a visible outline (WCAG 2.1 SC 2.4.7 Level AA)  
**Actual:** `document.activeElement` shows focus moves (landed on `<A>` with text "Equipment Library") but `outline-style: none` and `outline-width: 0px`. No visible indicator.

**Why this matters:** Keyboard-only users cannot see where focus is. This is a WCAG 2.1 Level AA failure that may affect ADA/Section 508 compliance.

---

## Updated Cumulative Bug Count: 7

| # | ID | Sev | Bug | Round |
|---|-----|-----|-----|-------|
| 1 | BUG-001 | MEDIUM | No 404 error page | Round 1 |
| 2 | BUG-002 | MEDIUM | Audit Log duplicate search fields | Round 1 |
| 3 | BUG-003 | MEDIUM | DevRev PLuG SDK fails to initialize (17x) | Round 1 |
| 4 | BUG-004 | MEDIUM | CSP blocks Beamer fonts (102 violations) | Round 1 |
| 5 | BUG-005 | MEDIUM | Task creation API 400 on task-session mapping | Round 1 |
| 6 | BUG-006 | MEDIUM | Issue Class validation gap (frontend + backend) | Round 2 |
| 7 | BUG-007 | LOW | No keyboard focus indicator (WCAG 2.4.7) | Round 2 |

---

## Files Created / Modified

| File | Location | Purpose |
|------|----------|---------|
| `deep-test-round2-part1.js` | /tmp/bug-verification/ | Part 1 — SLD, Issues, Attachments, keyboard a11y |
| `deep-test-round2-part2.js` | /tmp/bug-verification/ | Part 2 — Z1 site, Equipment Library, Issue form validation |
| `deep-test-round2-part3.js` | /tmp/bug-verification/ | Part 3 — Issue Class proof, sorting, edge cases |
| `build-final-report.js` | /tmp/bug-verification/ | Updated: added BUG-006/007, multi-dir screenshot support |
| `screenshots-round2/*.png` | /tmp/bug-verification/ | 24 screenshots from round 2 testing |
| `eGalvanic_Deep_Bug_Report_20_April_2026.html` | bug pdf/ | **Regenerated: now 7 bugs, 1.15 MB** |

---

## Technical Explanation — What Was Learned

### How the Site Selector Works (MUI Autocomplete)
The site selector in the top navigation is **not** a native `<select>` or MUI `<Select>`. It's an **MUI Autocomplete** component:
```html
<input role="combobox" class="MuiInputBase-input MuiOutlinedInput-input 
  MuiInputBase-inputAdornedEnd MuiAutocomplete-input" />
```
To interact with it programmatically:
1. Click the input (or the dropdown arrow button)
2. Clear existing text
3. Type the site name (e.g., "Z1")
4. Wait for the dropdown to populate
5. Click the matching option in the listbox

This is different from a regular `<select>` where you can use `selectOption()`. Playwright needs explicit text input + option click.

### How SLDs Use React Flow
The SLD (Single Line Diagram) page uses the **React Flow** library for rendering electrical diagrams:
```
.react-flow → container
  .react-flow__renderer → rendering layer
    .react-flow__node → each electrical component (panel, generator, etc.)
    .react-flow__edge → connections between components
  2 × <canvas> → background grid rendering
  133 × <svg> → component icons and connection lines
```
React Flow is a React library for building node-based UIs. Each electrical asset becomes a "node" and each connection becomes an "edge" in graph terms. The "All Nodes" view shows every asset on the site with their electrical connections.

### How Form Validation Gaps Happen
The Issue form has **partial** validation:
- ✅ Title → required, shows "Issue title is required"
- ✅ Proposed Resolution → required, shows "Proposed resolution is required"
- ❌ Issue Class → NOT required on frontend, NOT validated by API

This typically happens when:
1. The field was added after the original form was built
2. The validation schema (likely Yup/Zod) wasn't updated when the field was added
3. The API model marks the field as nullable instead of required
4. No integration test covers the "partial form" scenario

### How Keyboard Focus Testing Works
```javascript
await page.keyboard.press('Tab'); // Move focus
const el = await page.evaluate(() => {
  const ae = document.activeElement;
  return {
    tag: ae?.tagName,
    text: ae?.textContent?.substring(0, 50),
    outline: getComputedStyle(ae).outlineStyle,
    outlineWidth: getComputedStyle(ae).outlineWidth
  };
});
// Result: { tag: 'A', text: 'Equipment Library', outline: 'none', outlineWidth: '0px' }
```
This reveals that MUI's default theme has `outline: none` on focused elements. The fix is to add `:focus-visible` CSS rules, which only show outlines for keyboard navigation (not mouse clicks).

---

## What Was Correctly NOT Reported as Bugs

- **SLD "Select View" dual controls** — Intentional UX: center prompt for initial state + persistent toolbar control
- **Empty tables on Z1** — Z1 has limited test data; empty state is correct behavior
- **Issue form "Loading form data..." banner** — Transient loading state that resolves normally
- **Site selector requiring text input** — MUI Autocomplete is the intended pattern (search + select)

---

## Pages Verified as Working Correctly (Round 2)

SLDs (React Flow rendering), Issues (list + create form validation), Equipment Library (675 items + detail view), Connections (Z1), Attachments, Sales/Ops Overview, Locations, WO Planning, Commitments, Panel Schedules, Site Switching (MUI Autocomplete), Scheduling (all 4 views)
