# Accounts v1.35 — suite adaptation to the /customers rename + 8-TC extension

**Date:** 2026-07-30 · **Prompt:** extend QA automation for the v1.35 Account changes
(ZP-3156/3157/3185/3198/3210/3049) across create-flow, list, role access, role switching, edit.

## What changed on QA since the suite was built (2026-07-27)
Still badge V1.36, but the account list was **renamed and moved**: sidebar item is now
**"Customers" under OPERATIONS** at `/customers`; the ADMIN group no longer lists Accounts;
`/accounts` redirects to `/customers`. Page internals (tabs, search placeholder, buttons)
still say "Accounts". This contradicts ZP-3157's letter ("Accounts under Admin") and looks
like a deliberate later UX iteration — **flagged for the team, not filed as a bug**.

## Code changes
1. **`AccountV135RegressionTestNG`** — made label/route-agnostic (`onAccountsRoute()`
   accepts /accounts|/customers; sidebar checks accept Accounts|Customers; TC_AV135_01
   now passes on either the legacy [ADMIN+Accounts] or renamed [Customers] contract and
   logs which one matched). Without this the suite reds purely on the rename.
2. **`AccountsPage`** — javadoc rewritten to the current reality (route history, v1.35
   dialog contract, safe-create note). `open()` still enters via `/accounts` deliberately,
   so every test also exercises the legacy-deep-link redirect.
3. **NEW `AccountV135ExtendedTestNG`** (8 TCs, added to `suite-accounts.xml` +
   `fullsuite-testng.xml` test 43):
   - TC_AVX_01 old `/accounts` deep link lands on the live list (ZP-3157 AC-3).
   - TC_AVX_02 gmail contact email blocks Create — exact spec copy verified live:
     "Enter a valid business email address. Generic domains (e.g., Gmail, Outlook) are
     not allowed." (in-dialog role=alert, click-time validation, nothing created).
   - TC_AVX_03 malformed email cannot produce an account. Final design is OUTCOME-based
     after two falsified attempts: the button-gate is the real mechanism, but it settles
     only AFTER a debounced re-validation, so a single snapshot right after filling
     races the transition (seen 2026-07-30: enabled for one instant, then disabled). The
     test now POLLS up to 5s for Create to settle disabled; if a build ever leaves it
     enabled it falls back to clicking, and either path ends with the decisive assertion
     that no row with that name exists.
   - TC_AVX_04 Edit-dialog contract: sections, stable name= inputs, prefilled name,
     **email domain locked** (local part editable + fixed @domain suffix), Manage
     Contacts / Save Changes, no subdomain.
   - TC_AVX_05 facility selector: absent on the account list, present on /assets,
     absent again on return (ZP-3185 cross-page).
   - TC_AVX_06 v2 list API pagination is server-side: `POST /api/account/by-company/
     {id}/v2 {page,page_size,search,...}` → `{success,data:{items,page,page_size,total}}`,
     page_size honored, pages disjoint (ZP-3049; companyId sniffed from the page's own
     request — no hardcoded tenant data).
   - TC_AVX_07 duplicate-name tripwire (`known-product-bug` group, quarantined from the
     gate): **verified defect** — POST /api/account/v2 answered **201 twice for the same
     name** on 2026-07-30 (ZP-3156 AC-6 violated); tenant already holds colliding
     subdomains ("egalvanic" ×3). Test stays RED until the backend dedupes.
   - TC_AVX_08 role matrix (Admin, Account Manager): a visible account menu item must
     imply a working deep link; always restores Super Admin.

## Probe findings that shaped the tests (all live, authenticated, 2026-07-30)
- Create = one atomic `POST /api/account/v2` (account + contact together); the license
  is a separate follow-up `PUT /api/account/{id}` — worth knowing for rollback tests.
- Subdomain is auto-derived from the **contact email domain** (spec said account
  name/domain) — e.g. `@egalvanic.com` → `"subdomain":"egalvanic"`.
- The 2026-07-27 search anomaly (`testAcc08`, 'check' → "Sam email account") does NOT
  reproduce — search is name-scoped today; treated as test-data drift, no bug filed.

## Test fixes that came out of the first gate run (35 tests: 29 pass / 3 fail / 3 skip)
All three fails were contract drift, not product bugs; the 07-30 auto-generated
ready-bug files for them were deleted:
- `testAcc08_SearchFilters` (+ proactively `testAcc09`): search became SERVER-side with
  the v2 list API — the tests asserted rows immediately and read the stale unfiltered
  grid. Now they poll up to 10s for the filtered result. **This same race — not a
  matching bug — explains the 2026-07-27 'Sam email account' red.**
- `testRoleSwitchLeavesAccountsPage`: post-rename, switching PM away no longer leaves
  the route — the app stays on /customers and flips to the **Sites tab** (?tab=sites).
  The test now accepts navigate-away OR tab-flip, and still fails if the Accounts tab
  stays active for a role without access.
- `testEditDialogContract`: dialog innerText uppercases section headings (CSS
  text-transform) — section matching is now case-insensitive.
- `selectFirstOwner` (extension class): the async owner list occasionally misses the
  first open gesture — the open-and-poll sequence now retries once (~24s budget).

## Tripwire arm-test (expected red) + bug evidence
`-Dtest=AccountV135ExtendedTestNG#testDuplicateAccountNameRejected` fired exactly as
designed: "rejected=false, copies=2 … POST /api/account/v2 returned 201 twice" — and its
cleanup deleted both rows. Separately re-proved at the API layer (two identical
`POST /api/account/v2` → 201, 201; list total=2) and written up in
**`docs/bug-evidence/account-duplicate-name-not-rejected/EVIDENCE.md`**. All probe rows
deleted; post-cleanup `QA_AVX` list search returns total 0.

## CI triage (2026-07-30 run 30525367682 — Parallel Suite 3, the only recurring CI red)
1623 API tests, 4 failures, **all real product defects, correctly red — no test fixes needed,
none account-related**:
- `DuplicateApiAuditTest.testFixCheckUnderscoreListResponds` — `/planned_workorder_line/`
  unbounded-list timeout (35s socket timeout even WITH page/per_page). Same defect as this
  morning's ticket + frontend evidence pack (`docs/bug-evidence/planned-workorder-line-timeout/`);
  the fix-check tripwire stays red until the backend paginates.
- `CrudLifecycleApiTest.testTaskCrudLifecycle`, `MutationSemanticsApiTest.
  testAsyncWriteEventuallyConverges`, `MutationSemanticsApiTest.testDeleteIdempotency` —
  ONE shared root cause: **GET /tasks/{sld} → HTTP 500** (tasks LIST endpoint down; write
  path fine — the tests' own bracket messages say so). Re-verified live 2026-07-30 ~18:20
  IST from an authenticated session: 500 `{"error":"internal_error","trace_id":
  "c72b3a9e34524c4aba761d84eaf8ed39"}` — fresh trace_id for the backend team.

## Validation
`mvn test -DsuiteXmlFile=suite-accounts.xml` (headed, never headless):
- Run 1: 35 tests — 29 pass / 3 fail / 3 skip (fails = search race, role-switch tab-flip,
  edit casing — all test-contract drift, fixed).
- Run 2: 2 fail (malformed-email validation-transition race; role-switch async option
  race) — both fixed (poll for the settled state / poll+retry the option).
- **Run 3 (final, in-sequence): 35 tests — 0 failures, 2 skips, BUILD OK.**
  The 2 skips are environmental and pre-existing: `testAcc12_ContactsTabLoads` (detail
  contacts-tab nav unavailable on the row) and `testRowsPerPage` (rows-per-page selector
  not offered for the tenant's page size). Every previously-failing test verified green
  both in isolation and in full-suite order.
Duplicate tripwire (quarantined) exercised separately and RED as designed.
