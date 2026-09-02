# V1.36 locator re-map, MFA suite unblock, and a production smoke suite

**Date:** 2026-09-02
**Prompt:** "update all the test case as web design is updated so update xpath test case. also few test case for producation. too."
**Environment:** QA (`acme.qa.egalvanic.ai`, V1.36) — production suite written but NOT executed
**Commit:** `79fa4f7` on `main`

---

## The headline: it was not the XPaths

The task was to update locators for the redesign. What the investigation found first was that
**the entire Selenium suite was dead on QA for a reason unrelated to any selector.**

QA had begun serving a mandatory **"Set up your authenticator app"** TOTP enrollment screen after
a *successful* login. Authentication completes — the session cookie is set, the app routes to
`/dashboard` — and then that screen covers the application:

| Measurement on QA | Value |
|---|---|
| `#root` innerHTML | **178 bytes** |
| `a[href]` elements | 0 |
| `[role=tab]` | 0 |
| `.MuiDataGrid-root` | 0 |
| rail `nav[aria-label='navigation']` | 0 |

Even raw JavaScript, bypassing Selenium's visibility rules, saw nothing. So every UI test that
logged in was asserting against an empty application. The symptom — "every locator on every page
stopped matching" — is indistinguishable from a redesign having broken all the selectors, which is
exactly the wrong conclusion.

It was made worse by a safety net working as designed: `NavCatalog.navigateTo()` falls back to
`driver.get()` when a sidebar click fails, so routes still *arrived* and route-level checks still
reported PASS. The suite looked partly healthy while testing a blank page.

**Fix:** `LoginPage.login()` now ends with `dismissMfaEnrollmentIfPresent()`, clicking **"Set up
later"**. Enrollment is optional — the screen's own footnote reads "You'll be asked again next
time you sign in" — so declining is a real user path, not a bypass. Putting it in
`LoginPage.login()` fixes `BaseTest` and every standalone suite at a single point, because they
all authenticate through there.

**Measured effect:** `NavCatalogSelfValidation` went from **29 failing checks to 0**.

**Lesson worth keeping:** when a large swathe of UI tests fails on locators, check whether the app
shell actually mounted (`#root` size, anchor count) *before* touching a selector. An auth overlay,
a consent modal and a TLS interstitial all present as "all locators broke".

---

## NavCatalog: the route map had drifted in ~20 places

Re-harvested by clicking every rail button on the live app and reading the resulting
`a.MuiListItemButton-root[href]` set — so the map is now the anchors the app renders, not the
labels it used to.

- **A whole seventh category, Maintenance Portal, was missing.** `collectAllNavHrefs` therefore
  never opened it, under-reporting its five routes — which any RBAC visibility check scores as
  "this role cannot see them".
- **The three dashboards left the rail logo** and moved into ordinary categories: `/dashboard`
  under Site Data, `/ops-dashboard` under Operations, `/sales-overview` under Sales.
  `openDashboards` is now deprecated.
- **Category moves:** `/panel-schedules` Engineering→Site Data, `/customers` Sales→Admin,
  `/pm-plans` Admin→Builder. `/test-equipment` appears under *both* Operations and Admin.
- **14 routes were absent** from the catalog, including the five `/maintenance-portal/*`, four
  Engineering schedule pages, and `/guest-portal-users`, `/custom-devices`, `/pull-through-work`.
- **`/classes` split into three** (`/asset-classes`, `/connection-classes`, `/issue-classes`).
- **`/emps` and `/equipment-designations` left the nav but still render real content** (probed
  individually) — they are unlinked, *not* dead. New `UNLINKED_LIVE_ROUTES` records that
  distinction so "absent from nav" is never reported as "dead route".
- **Labels changed:** "SLDs"→"SLD", "Users"→"Platform Users".

### Labels stopped being unique

This is the structural finding. **"Reports" is on three different routes**
(`/maintenance/reports`, `/reporting/builder`, `/maintenance-portal/reports`), and "Condition
Assessment", "Maintenance Program", "Compliance" and "Test Equipment" are each on two. A locator
like `//a[normalize-space()='Reports']` is ambiguous *by construction* — it clicks whichever the
open category happens to render. **Href is the only unique key.**

Rail buttons now key on the `aria-label` the rail actually carries, scoped to
`nav[aria-label='navigation']`. The old locator matched category names anywhere in the document,
and four of them double as in-page tab labels — most sharply "Engineering", which is also a tab on
the asset detail page, so an unscoped match hit the rail button and silently navigated to
`/arc-flash` instead of switching tabs.

### Two smaller correctness fixes

- **`displayLabelFor()`** added. `labelFor()` returning null is meaningful ("the nav does not link
  this"), but it is the wrong value to hand a report: `/classes` is a live tabbed page the nav no
  longer links, so its null reached `ExtentReportManager.createTest()` and failed an otherwise
  passing test with "Test name must not be null or empty".
- **`/customers` removed from the tab catalog.** The Accounts|Sites split is a production/V2.0
  surface; QA renders a card list with zero `[role=tab]` and no grid. Listing it made the catalog
  assert a surface QA does not have — a guaranteed-red check that says nothing about QA. The split
  is now covered against the environment that has it, by the production suite.

---

## V136Locators (new): the vocabulary the redesign supports

A DOM audit of `/assets`, `/sessions` and `/tasks` established what is actually available, and the
answer reframes the whole task — the fix is not better XPaths one field at a time, it is to stop
guessing at structure.

| Attribute | Per page | Verdict |
|---|---|---|
| `data-testid` | 2 | Unusable — both are MUI's own `sentinelStart`/`sentinelEnd` focus traps. The app ships no test ids. |
| `id` | 24–32 | **Actively dangerous.** Mostly React 18 `useId` output — `input[id="«r4»"]` — regenerated per render. Only `#page-header-actions` and `#sld-root` are stable. |
| `aria-label` | 53–61 | Good, and the answer to the two-level rail. |
| `data-field` | 90–117 | **Best available.** DataGrid stamps the column's field on every cell: `status`, `priority`, `due_date`, `label`, `title`, `actions`. |
| `data-id` | 10–13 | The row's server id — the right way to target one known row. |

Four traps encoded in the class:

1. **Every list page is a `MuiDataGrid`, never a `<table>`** — `//tbody//tr` matches nothing, so a
   test asserting "0 rows" against it passes vacuously.
2. **Form inputs carry no `name`, `aria-label` or usable `id`**, so a field can only be found via
   its label — by climbing to the enclosing `MuiFormControl-root`, never
   `following::input[1]`, which leaves the container and returns the next section's input.
3. **`data-field` replaces column-index XPaths.** An index reads the wrong column the moment a
   column is added, reordered or hidden — and the test still passes, because a wrong value is
   rarely an empty one.
4. **XPath 1.0 cannot escape quotes**, so `quote()` assembles `concat()` for values containing
   both `'` and `"`. Asset and work-order names with apostrophes are routine.

---

## WorkOrderPage: Create Work Order is a gated four-step wizard

Walked live. The dialog *title* is unchanged ("Create New Work Order"), which is precisely why
this broke quietly — `WO_DIALOG` still resolves.

```
Step 1 "Work order"   WO Name / # *, Facility *, Due Date, First service to perform
Step 2 "Scope"        service selection + LIVE asset-scope resolution
Step 3 "Team"         unreachable without a scope-matching service
Step 4 "Review"       the only step with a "Create" button
```

- **Steps cannot be jumped.** The numbered step buttons render as buttons but clicking them is a
  no-op — verified by clicking "2 Scope", "3 Team", "4 Review" in turn from step 1; the wizard
  stayed on step 1 every time, with no error.
- **"Create" is four steps away**, so the old fill-the-form-then-click-Create shape can never
  complete a work order.
- **Four targeted fields are on neither reachable step:** Est. Hours, the Description textarea,
  Start Date and Equipment. Step 1 has exactly one date input, so the old
  `(…MM/DD/YYYY)[2]` locator for Start Date resolves to nothing while `[1]` still correctly means
  Due Date.
- **Step 2 gates on resolved scope, not field presence.** With a service matching no assets it
  reported "0 matching assets" and "This site has 14 open issues, but none are workable (3 not
  linked to an asset, 10 on deleted assets)", and Continue stayed disabled. A create test needs a
  facility/service pair resolving to ≥1 asset, or it stalls for a data reason that looks like a
  locator failure — `getScopeBlockReason()` now tells those apart.

Added step-aware navigation (`currentWizardStep`, `continueWizard`, `backWizard`,
`reachReviewStep`, `selectFirstService`, `cancelWizard`) and made `clickCreateWorkOrder()` throw
with the active step named, rather than producing an anonymous timeout that reads as "the Create
button is broken".

---

## Page objects: fixes, and dead code that was never going to work

- **Six sidebar-by-text nav locators → href locators** (`AssetPage` ×2, `WorkOrderPage`,
  `ArcFlashPage`, `IssuePage`, `LocationPage`).
- **Provably-dead fields deleted**, with grep evidence: `private` and declaration-only, so no test
  could reach them. Twelve such fields sat in `WorkOrderPage` alone — updating them would have
  manufactured the appearance of coverage.
- **The Arc Flash role selector was retired.** Verified live: zero
  `input[placeholder='Select role']`, the word "role" appears nowhere in `<main>`, only the
  Engineering Mode switch remains. Its two suites now **skip with the real reason** instead of
  blaming "slow recompute", which would have sent someone chasing a timing bug that does not
  exist.
- **One of those tests was passing vacuously** — the more serious case. With the selector gone,
  `getRoleOptions()` returns empty, the "fewer than two options" branch logged a PASS and
  returned, so a role-switch test reported green on a page with no role switch. A vacuous pass
  hides the gap; a failure at least surfaces it.
- **Two `contains(text(),…)` button locators fixed.** MUI wraps a button's label in a nested
  `<span>`, so `text()` reads the button's own empty text node and the locator matched nothing.
- **A locator built by string-surgery on `By.toString()`** (stripping Selenium's `"By.xpath: "`
  debug prefix) whose computed value was then **discarded** — the code typed into the
  dialog-scoped locator regardless, so in inline edit mode the location rename silently never
  happened and the following Save asserted an unchanged name. The compiler had been flagging the
  unused variable all along.

---

## Production smoke suite (new, opt-in, read-only)

`ProductionSmokeTestNG` + `prod-smoke-testng.xml` — 7 checks: availability, login, the nav rail,
nav-route parity against the QA catalog, core list pages, a regression net for the confirmed
production first-role-only Customers tab gate, and JS error hygiene.

**Three interlocks, because this points at real infrastructure:**

1. **Opt-in only** — every test skips unless `-DPROD_SMOKE=true`. Not wired into `testng.xml`, so
   a plain `mvn test` cannot reach it. This encodes the standing rule that production is touched
   on request, never autonomously.
2. **Refuses a non-production target** — a QA/staging/local `BASE_URL` skips rather than runs, so
   a misconfigured job can never produce a green "verified on production" report from QA data.
3. **Credentials must come from the environment** — the committed defaults are QA's and cannot
   authenticate on prod, so running with them would fail on login and read as an outage.

Nothing creates, edits or deletes; that is a property of the file, not a convention.

**On version skew:** production runs *ahead* of QA (V2.0 vs V1.36), so the parity check treats
"prod has routes QA does not" as information and fails only on a **lost core route**.

**The multi-role check refuses to pass vacuously.** It asserts the seat's role count first and
skips when it is below two — because a first-role-only gate is *mathematically invisible* on a
single-role account, which is why the defect went unnoticed. All eight QA RBAC seats are
single-role.

---

## Validation — run, not just compiled

| Check | Result |
|---|---|
| `mvn -o test-compile` | BUILD SUCCESS |
| `NavCatalogSelfValidation` (live QA) | **ALL CHECKS PASSED** (was 29 failures) |
| — routes harvested | 51 across 7 categories; 13 on a single read; **38 a single read misses** |
| — every category contributes | including Maintenance Portal, previously 0 |
| — dashboards via their categories | anchor present in DOM after expanding, all 3 |
| — tab catalog vs live pages | 18/18 tabs present and clicked |
| `TabbedModulesSmokeTestNG` (live QA) | **17/17 passed** |
| All 37 `V136Locators` resolved live | no selector errors; all 3 `quote()` branches valid |
| Prod suite, no flag | 7/7 skipped, no production contact |
| Prod suite, flag on + QA `BASE_URL` | 7/7 refused |

The locator resolution run matters on its own: an invalid XPath compiles fine and only throws at
`findElements()` time, so resolving each one against a live page is the only way to know the
vocabulary is usable.

---

## Not done / open

- **The production suite has not been run against production.** Standing rule: prod is tested only
  on explicit instruction. It is one command away.
- **Wizard steps 3 (Team) and 4 (Review) are unmapped** — reaching them needs a facility/service
  pair whose scope resolves to ≥1 asset. Their field lists are deliberately not guessed at; the
  four relocated fields are marked as unreachable-until-confirmed rather than given invented
  locators.
- **58 MEDIUM-risk locators remain** (mostly text-matched buttons inside dialogs). They work
  today; they are the next tranche if button labels churn.
- `ARC_FLASH_NAV` is now correct but also unreferenced — flagged rather than deleted.
