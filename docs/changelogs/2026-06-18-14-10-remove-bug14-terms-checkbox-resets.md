# Remove obsolete BUG-14 (Terms checkbox resets after failed login)

- **Date:** 2026-06-18
- **Prompt:** "BUG14_TermsCheckboxResets remove this test case"
- **Module:** Authentication → Login UX (`BugHuntTestNG`)
- **Type:** Test removal (obsolete — exercises UI removed from the product).

---

## Ask

Remove the `testBUG14_TermsCheckboxResetsAfterFailedLogin` test case.

## Why it's correct to remove (not just asked)

The test checked whether the **Terms & Conditions checkbox** stays checked after a failed login.
But the **T&C checkbox was removed from the product per ZP-1828** — confirmed in two places in
this same codebase:

- [`BugHuntTestNG.java`](../../src/test/java/com/egalvanic/qa/testcase/BugHuntTestNG.java) already
  notes "T&C checkbox removed per ZP-1828; `clickTermsCheckbox()` is a no-op".
- [`LoginConsentTestNG.testTC_LC_02_TermsCheckboxAbsent`](../../src/test/java/com/egalvanic/qa/testcase/LoginConsentTestNG.java)
  is a **regression tripwire** asserting the checkbox is now absent.

So BUG-14 tested a control that no longer exists: it would always find no checkbox, fall into its
"checkbox not found" path, and log a misleading pass — pure noise.

## What changed

- **`BugHuntTestNG.java`** — deleted the `@Test testBUG14_TermsCheckboxResetsAfterFailedLogin`
  method (was `priority = 30`). Replaced the section's BUG-14-specific sub-comment with a note
  explaining the removal and pointing to the ZP-1828 absence tripwire. Kept the
  "SECTION 4: LOGIN UX BUG VERIFICATION" header (BUG-15 password-toggle and later UX tests remain).

## Scope deliberately left untouched

- `clickTermsCheckbox()` helper — still used by other tests (now a documented no-op); kept.
- `failed-suites/failed-tests-2026-06-12.xml` / `…-06-14.xml` still `<include>` the method by
  name. These are **immutable, auto-generated historical records** of what failed on those dates;
  TestNG silently ignores an `<include>` that matches no method, so they don't break, and future
  regenerations won't list the deleted test. Editing them would falsify the failure history.
- `BUG-14` references in `docs/` (SLD double-mount, Roles-API-2x) are **unrelated bugs** with
  their own numbering — untouched.

## Validation

`mvn -B test-compile` — clean (the removed method's `List`/`By`/`WebElement` usage is still needed
by sibling tests, so no import breakage). No active source references the removed method.
