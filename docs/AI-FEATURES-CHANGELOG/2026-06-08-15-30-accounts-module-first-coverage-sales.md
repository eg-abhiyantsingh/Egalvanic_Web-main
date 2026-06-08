# Accounts [SALES] — first dedicated test coverage (new module)

- **Date:** 2026-06-08
- **Time:** ~15:30 (local)
- **Author:** Claude Code (Opus), driven by abhiyant.singh@egalvanic.com
- **Prompt (follow-up):** "cover and check all the test case of all module also after this" — after the
  Opportunities expansion, extend coverage to the modules with gaps. **Accounts** (the third SALES
  nav item, alongside Goals + Opportunities) had **zero dedicated tests** and no page object.

## Why Accounts
The SALES area is Goals · Opportunities · **Accounts**. Opportunities was just deeply covered;
Accounts is its sibling and the source of an Opportunity quote's **Recipient** (Contacts live on
the account detail at `/accounts/:id?tab=contacts`). It was entirely uncovered → highest-value next gap.

## Live exploration (Playwright) — what Accounts actually is
- Grid columns: **Account Name · Owner · Created · Actions** (data-fields name / owner_name / created_at / actions). Search box + **New Account** button.
- **Create New Account** dialog (all `*` required): Account Name, **Subdomain** (e.g. "acme"), Account
  Owner (combobox), Address Line 1, City, State, ZIP, Country (+ optional Address Line 2).
- Account **detail** (`/accounts/:id`) tabs: **Details · Internal Team · Portal Access · Contacts ·
  Opportunities · Sites · Goals · Notes**.
- **API:** flat `GET /api/accounts/` returns **200 unauthenticated** (null-field template, no data) —
  the same auth inconsistency as `/opportunities/` & `/quotes/` (**BUG-E**, now generalized).

## Test-safety decision
Creating an account collects a **Subdomain** (tenant-ish), so the suite **never submits** a new
account — it exercises create-dialog **validation** (required-field gating) and cancels. Read/search/
detail run against existing accounts; delete is **confirmation-gated and cancelled** (never deletes
shared data). This mirrors the destructive-safety conventions of the rest of the suite.

## New: `AccountsPage` POM + `AccountsTestNG` (16 tests)
POM mirrors `OpportunitiesPage` proven patterns (bounded open-retry, async content wait, React-native
search setter, JS name-field finder + real keystrokes, tolerant JS clicks, tab helpers).

**Functional gate (green) — 12:**
- `testAcc02` grid columns · `testAcc03` create-dialog required fields · `testAcc04` empty name blocks
  Create · `testAcc05` name-only doesn't satisfy the required set · `testAcc06` cancel creates nothing ·
  `testAcc07` XSS in name not executed · `testAcc08` search filters · `testAcc09` no-result search ·
  `testAcc10` clear-search restores · `testAcc11` detail opens with Contacts/Opportunities/Sites/Goals
  tabs · `testAcc12` Contacts tab loads · `testAcc13` delete is confirmation-gated (cancelled).

**Quarantined tripwires (`known-product-bug`, RED by design) — 4:**
- `testAcc01` page-load health [BUG-A] · `testAcc15` account-detail health [BUG-A] ·
  `testAcc14` accessibility/WCAG [BUG-B] · `testAcc_ApiFlatEndpointRequiresAuth` flat `/accounts/`
  auth [BUG-E].

Functional tests assert FUNCTION and do NOT call `verifyPageHealth` (reserved for the tripwires), so
the gate stays green on real behaviour despite BUG-A console noise.

## CI / suite wiring
- New `suite-accounts.xml` (functional gate; excludes `known-product-bug`).
- Added `accounts` group to **parallel-suite.yml** (Core Regression; max-parallel 10→11) and
  **fullsuite-testng.xml** (module 43).

## Validation (live, non-headless)
`mvn -DsuiteXmlFile=suite-accounts.xml test` → **12 tests, 12 passed, 0 failed, 0 skipped.**
One triage iteration was needed: the account-detail tabs mount async (BUG-A render lag), so
`testAcc11`/`testAcc12` initially saw an empty tab list — fixed by polling `waitForTabContaining
("Contacts", …)` before reading tabs (mirrors the Opportunities quote-editor render-lag handling).
BUG-E updated in BUGS.md to include `/accounts/`. Run logs: `/tmp/acc_gate2.log`.
