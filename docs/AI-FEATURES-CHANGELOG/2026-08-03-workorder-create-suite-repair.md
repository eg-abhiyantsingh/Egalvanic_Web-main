# Work Order Create suite — repair after the V1.36 role rename broke navigation

**Date/time:** 2026-08-03, ~21:00 local
**Prompt:** "check everything on live work order and fail test case"

## Symptom
All of `WorkOrderCreateTestNG` (7+ TCs) failed at the first step — "Create New Work Order dialog should open. expected [true] but found [false]" — with logs showing the session sitting on `/dashboard` and "Backdrop/drawer detected", never reaching `/sessions`.

## Root cause (verified live + in-run)
`WorkOrderPage.navigateToWorkOrders()` reached the WO list **only** by clicking a sidebar link whose text is "Work Orders"/"Sessions". After the V1.36 role rename (see `project_rbac_role_rename_v136`), the account's active role is now **"Admin" = the config/setup console**, whose sidebar has **no Work Orders item**. So the JS link-click was a no-op, the session stayed on `/dashboard`, and the Create dialog was never reachable → every test failed at dialog-open. (Confirmed in-run: `Work Orders sidebar link not found (setup-console role?) — navigating to /sessions by URL. Was: https://acme.qa.egalvanic.ai/dashboard`.)

## Fixes
1. **`WorkOrderPage.navigateToWorkOrders()` — direct-URL fallback (the key fix).** After the sidebar-click attempt, if `!isOnWorkOrdersPage()`, `driver.get(BASE_URL + "/sessions")`. `/sessions` renders the WO grid + "Create Work Order" for any role holding `features.jobs.view` (verified live under the Admin role). Role-agnostic; fixes every caller across all WO test classes.
2. **`openFreshCreateForm()` reinstalls health hooks** after the possible `driver.get()` full reload (the JS console/network capture hooks are dropped on reload; the `@AfterMethod` gates read them).
3. **`ensureScheduleSectionVisible()`** — defensive: clicks the "Advanced Settings" header once only if the Schedule heading/＋ is absent, so `clickScheduleAddButton()`/`addScheduleBlock()` never miss a not-yet-mounted Schedule section. No-op when already present.
4. **WOC_06 returns to the WO list before verifying persistence** — on success the app redirects to the new WO's detail page (`/sessions/{id}`), which has no grid/search box; `isOnWoDetailPage()` → `navigateToWorkOrders()` before `searchWorkOrder()` (also refreshes the list so the new row shows).
5. **`pickDate()` reveals the Start Date calendar** — the Start Date field lives inside Advanced Settings; when that content isn't mounted only the Due calendar exists, so calendar index 2 can't resolve. `pickDate` now clicks the Advanced Settings header **only while the wanted calendar is absent** (never toggles an already-expanded section shut), then proceeds.

## Results
**First re-run (nav fix only): 9 run → 6 PASS, 1 SKIP, 2 FAIL.**
- PASS: WOC_01, WOC_02, WOC_04 (Schedule ＋ "revealed=true"), WOC_05, WOC_09, WOC_10.
- SKIP: WOC_03 (equipment catalog empty on the test site — the test's own by-design skip, `feedback_dont_overreport_sld_bugs`: 0 data ≠ bug).
- FAIL: WOC_06 (grid search) + WOC_07 (date picker) — this surfaced two *more* redesign gaps the nav fix alone couldn't cover:
  - WOC_06: after Create the app redirects to the new WO's `/sessions/{id}` detail page (no grid/search box) → `searchWorkOrder` self-healed 15× on a search box that wasn't on-screen. Fixed by fix #4 (return to list first). The list search box placeholder is unchanged (`"Search work orders..."`) — locator was fine, the page was wrong.
  - WOC_07 + WOC_06's `pickDate(2,…)`: in the test's fresh dialog the **Start Date calendar (index 1) is unmounted** (Advanced Settings content not rendered), so only the Due calendar exists and index 2 never resolves (25s timeout). Fixed by the `pickDate` Advanced-expansion guard (fix #5, added after this run).

**Second re-run (fixes 1–4): WOC_06 now PASS; only WOC_07 red.** That last red exposed the true date-field contract, which differs from what the test assumed:
- There is **no Advanced Settings accordion** — it's a plain label; every field (including both date pickers) is always mounted. My fix-4 "expand Advanced" premise was wrong (harmless no-op — the calendar was present, just obscured).
- Both "Choose date" buttons are present on open, in DOM order **[1] = Due Date (top-level, empty), [2] = Start Date (lower, pre-filled today)** — the *inverse* of the test's `[1]=Start, [2]=Due` comment.
- After the first pick, the MUI calendar **popper stays up briefly and overlays the other date button**, so `elementToBeClickable([2])` timed out (25s).

**Final fixes for WOC_07:**
6. **`pickDate()` waits for the picker popper to close** — `waitPickerClosed()` before locating the button and again after selecting a day, so sequential picks don't collide (replaces the wrong accordion guard).
7. **WOC_07 uses the real index mapping** — Due = calendar `[1]` (15th of next month), Start = calendar `[2]` (today); getters are label-based so the Due assertion reads the right field.

**Final re-run (all fixes): 8 PASS, 1 SKIP (WOC_03, by-design), 0 FAIL.**

## Not a product bug
The redesigned nav (Admin = setup console without Work Orders) is the tenant role-model change already documented in memory, not a WO defect. The test broke because it navigated by sidebar text instead of by route. No product bug filed for this.

## Depth notes (learning)
- **Locator-by-visible-text is brittle across role/nav redesigns.** The durable fix is to navigate by stable route (`/sessions`) and fall back to it whenever the nav-driven click doesn't land — the URL contract outlives sidebar labels.
- **A full-reload `driver.get()` silently voids injected JS health hooks;** any nav helper that may hard-navigate must be paired with a hook reinstall or the post-test health gates read a dead page.
