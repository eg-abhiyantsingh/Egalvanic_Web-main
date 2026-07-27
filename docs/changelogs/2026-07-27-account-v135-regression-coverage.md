# 2026-07-27 — Account Create & Management: Web v1.35 regression coverage (6 tickets)

**Prompt:** ticket "[QA Automation] Account Create & Management — Web v1.35 regression coverage"
— cover ZP-3156/3157/3185/3198/3210/3049, "cover all the test case web test case check qa is updated".

## Part 1 — "check qa is updated": live verification (QA is on V1.36 — all v1.35 changes deployed)

| Ticket | Live state verified 2026-07-27 (authenticated Super Admin session) |
|---|---|
| ZP-3156 create flow | ✅ CONTACT DETAILS mandatory (First/Last/Email/Job Title all starred, "becomes the account's Primary Contact"); **no subdomain anywhere**; address hidden behind optional "Add a site now" toggle (when ON: Site Name/Access Complexity/Addr 1/City/State/ZIP/Country required, Line 2 optional); NEW LICENSE TYPE radios (Interactive / Read-only / No license); Create disabled until required fields filled |
| ZP-3157 under Admin | ✅ Accounts in the ADMIN sidebar group; page cleaned to Account Name / Owner / Created / Actions + New Account |
| ZP-3185 no site dropdown | ✅ zero site/facility inputs and no "Site:" top-bar selector on /accounts |
| ZP-3049 pagination | footer "1–25 of 46" — QA tenant has 46 accounts (customer case was 555), so tests assert pagination BEHAVIOR not the count |
| ZP-3198 redirect | ✅ observed live: on /accounts, switching Super Admin → Project Manager navigates to /sites (user is NOT stranded) |
| ZP-3210 PM visibility | ⚠ after role switch, PM shows NO Accounts nav — whether that matches direct PM login is exactly what the new parity test pins |

Role switcher options on this tenant: Admin, Project Manager, Account Manager, Super Admin,
Electrical Engineer.

## Part 2 — new test class `AccountV135RegressionTestNG` (16 TCs, client-readable names)

- **ZP-3157/3185 (3):** Accounts in ADMIN menu group; cleaned columns exact; no site selector.
- **ZP-3156 (6):** dialog sections + required marks; subdomain hidden; contact gates Create;
  address optional behind the toggle (required set when ON, Line 2 optional, hides again OFF);
  license 3-way choice; full end-to-end create with contact only ("No license" chosen so no portal
  invite side-effects; unique QA_AV135_* name).
- **Edit + cleanup (2):** rename via row Actions → list reflects; delete (confirmation-gated) —
  the chain only ever touches its own QA_AV135_* account, and @AfterClass sweeps leftovers.
- **ZP-3049 (2):** footer range true + Next/Previous behavior (Previous disabled on page 1, page 2
  shows different rows); rows-per-page 25→50. Both SKIP with a clear data message if the tenant
  drops ≤25 accounts.
- **ZP-3198 (1):** on /accounts, switch to Project Manager → assert the app NAVIGATES AWAY and the
  landing page is healthy; always restores Super Admin (finally + @AfterClass).
- **ZP-3210 (1):** the real contract — PM's Accounts menu visibility AND /accounts reachability must
  be IDENTICAL via role switch (main session) and via DIRECT login (second Chrome, dedicated
  abhiyant.singh+project@ account). Whichever way product intends, the two paths may not disagree.
- **(1)** Super Admin regains full Accounts access after the role tests.

Also: `AccountsTestNG` TC_ACC_05 stale wording updated (required set is now Owner+Contact, not
Subdomain/Address); the other 15 legacy TCs were verified still-valid against the v1.35 dialog.

**EG Admin visibility** (ticket test-area): no dedicated EG Admin QA credential exists — the EG
Admin overlay is probed dynamically by `EgAdminSuperAdminContractTest` (API contract; skips with
instructions when unprovisioned). UI-level EG Admin checks need that account provisioned first.

## Part 3 — wiring
- `fullsuite-testng.xml` test 43 (Accounts) + `suite-accounts.xml`: new class added after
  AccountsTestNG (same known-product-bug exclusion).

## Validation (run live on QA, never headless)

First run: 16 tests, 4 failures — all **test-side**, diagnosed against the live DOM and fixed:
| Failure | Root cause | Fix |
|---|---|---|
| Accounts-in-ADMIN | sidebar group labels (DASHBOARDS/DATA/…/ADMIN) aren't clean `text()` nodes | read `.MuiDrawer-root` innerText (word-match) |
| contact-gates-create + create-E2E | Account Owner Autocomplete fetches ~170 contacts async; fixed 1.2s wait too short | real Selenium click to open + typed-search fallback + poll ~12s |
| PM parity (2nd browser) | submitted login before the form rendered on the fresh browser | wait for the email field (≤30s) before `login()` |

Second run: **16 tests, 0 failures, 5 skipped** — owner dropdown still skipped the create chain
(pure-JS indicator click didn't enter MUI's open/fetch state), so `selectFirstOwner` was rewritten
to a real WebElement click + typed-search fallback + ~12s poll.

Third run: **14 pass, 0 fail, 2 skip** — create→(gate)→create-E2E→delete all pass; PM parity
(ZP-3210) PASSES (direct-login and role-switch agree). Two skips remained: `testEditAccountRename`
(looked for a kebab menu) and `testRowsPerPage`. Live DOM showed the Actions column is direct icon
buttons (`aria-label="Edit Account"`/`"Delete Account"`), so the menu-based helper was replaced
with `clickRowIconAction(name, verb)`; edit now executes.

Final run: **15 pass, 1 skip** (rows-per-page selector genuinely absent on this grid — a legitimate
environment SKIP with a clear message, never a false fail).

## Depth notes
- **Parity over assumption (ZP-3210):** the live probe showed PM-after-switch has no Accounts nav.
  Rather than guessing the intended end-state, the test encodes the invariant the ticket actually
  fixed: direct-login and post-switch must agree. It fails on the bug's signature (disagreement)
  and passes on either consistent design.
- **Role tests run last + double restore:** a mid-test failure can strand the shared session on a
  PM persona and poison later classes; both a finally block and @AfterClass restore Super Admin.
- **"No license" in the E2E create:** the contact "receives portal access" on Interactive — using
  No license keeps QA free of invite emails/user provisioning while still exercising the mandatory
  contact + optional address contract.
