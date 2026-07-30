# Account v1.35 automation — rename adaptation + validation/edit/API/role extension

**Date:** 2026-07-30 · **Time:** ~18:05 IST
**Prompt:** "Update and extend QA automation test suites to cover Account creation and
management changes shipped in Web v1.35" (ZP-3156/3157/3185/3198/3210/3049).

## Changes
- `AccountV135RegressionTestNG`: route/label-agnostic helpers (`onAccountsRoute`,
  `sidebarHasAccountsItem`); TC_AV135_01 accepts legacy [Accounts under ADMIN] OR
  renamed [Customers under OPERATIONS] contracts.
- `AccountsPage`: javadoc rewritten (route history + v1.35 dialog + safe-create).
- NEW `AccountV135ExtendedTestNG` (8 TCs) wired into `suite-accounts.xml` and
  `fullsuite-testng.xml`.
- Memory `project_account_module_v135.md` updated with all newly pinned contracts.

## Depth explanation (why each decision)
1. **Probe before pinning.** Every new assertion came from a live authenticated probe
   first: the gmail rejection copy was read from the real `[role=alert]`, the create/list
   API bodies from the network log, the edit-dialog inputs from a DOM inventory. Tests
   that pin guessed contracts become permanent flakes; tests that pin observed contracts
   only red on real change.
2. **Dual-contract instead of chasing the rename.** QA renamed Accounts→Customers
   mid-version (badge still V1.36). Hard-coding the new name would break if the rename
   rolls back (or ZP-3157 is re-asserted); asserting only behavior ("an account item
   exists; its deep link works") survives both worlds and logs which contract matched —
   that log line is the drift detector.
3. **Two validation layers, two test shapes.** A malformed email (bad FORMAT) keeps the
   Create button DISABLED — button-gating; a well-formed email on a banned domain
   (gmail) passes the gate and is rejected at CLICK time as an in-dialog alert with no
   network call. TC_AVX_03 asserts the disabled state (and that fixing the email
   re-enables it — proving the email was the gating field); TC_AVX_02 asserts
   {dialog open + alert copy + nothing created}. Lesson learned live: the first
   malformed-email probe read a leftover alert from the gmail attempt — the fixed test
   came from re-probing on a CLEAN dialog after the suite run falsified the assumption.
4. **Verified defect, quarantined tripwire.** Duplicate account name → two 201s
   (reproduced, then cleaned up). Encoded as a `known-product-bug` group test so the
   functional gate stays green while the red tripwire documents ZP-3156 AC-6 until the
   backend dedupes — the repo's established quarantine pattern.
5. **No hardcoded tenant data in the API test.** TC_AVX_06 sniffs the companyId from
   the page's own `by-company/{id}/v2` request via `performance.getEntriesByType`
   inside `executeAsyncScript` — the test works on any tenant and stays cookie-authed
   (the app moved to HTTP-only-cookie auth, so REST Assured token replay gets 401).
6. **Role tests last + restore in finally** — same discipline as the base class; a
   half-switched session poisons every later test in the shared browser.
