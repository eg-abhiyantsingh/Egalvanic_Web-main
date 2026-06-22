# Issue-class taxonomy conformance — deep check + API test

- **Date:** 2026-06-22
- **Prompt:** "check in depth and update test case" (re `testcase/issue_classes_template.xlsx`)
- **Type:** Deep template/coverage audit + new deterministic conformance test.

---

## Deep check (template ↔ live ↔ tests)

- **Template** (`issue_classes_template.xlsx`): 7 classes, 21 attrs. Clean structurally; flagged
  earlier: `Options.Types` omits the used `multi_select`/`temperature`; `Ultrasonic Anomaly` has 0
  attrs; `Replacement Needed`'s 3 attrs are placeholders ("Replacement check 1/2/3").
- **Live** (`GET /api/issue_classes`): all **7 template classes present** (verified) — plus tenant
  **junk** not in the template: `DEVTOOL_TEST IssueClass Updated` ×9, `Test`/`Test 1`/`Test 2`/
  `Test 3`/`Test1`, `Other`, and duplicate global/override rows. (Live-tenant cleanup item.)
- **Tests**: all three issue suites only ever exercise **`NEC Violation`**; the only assertion on
  the class list was `IssuePart2.testISS_020` which merely **counted** dropdown options (no content
  check). `DashboardBugTestNG`'s hard-coded list was stale (fixed last commit to add Replacement
  Needed).

## Why the conformance test is at the API layer (not UI)

I first tried strengthening `testISS_020` to assert the 7 classes appear in the Create-Issue
dropdown. Verified live (headed): the MUI "Issue Class" Autocomplete is **unreliable to open** in
Selenium (the input locator times out; the form's class field doesn't open deterministically). So a
UI assertion would be flaky. Reverted that experiment (left only a pointer comment) and put the
check where it's deterministic — `/api/issue_classes` is exactly what populates the dropdown.

## New test — `api/IssueClassContractApiTest`

Issue-side analogue of the RBAC permission contract / asset gold conformance:
1. **`testIssueClassTaxonomyMatchesTemplate`** — `GET /api/issue_classes` must expose all 7 classes
   from `issue_classes_template.xlsx` (subset check — tenant junk ignored).
2. **`testViolationClassesHaveSubcategoryOptions`** — NEC/NFPA 70B/OSHA each expose a `Subcategory`
   attribute with a non-empty options list (the real, stable enums the app must offer).

Wired into `suite-load-api.xml` (the CI "Load + API" group) and `suite-api.xml`.

## Validation

- Both tests pass live (0s each — deterministic, no browser).
- **Red-proofed**: injecting a fake class made `testIssueClassTaxonomyMatchesTemplate` fail with
  *"MISSING template-defined classes [ZZ_DOES_NOT_EXIST Violation]"*, proving the assertion is real;
  reverted. `mvn test-compile` clean; `IssuePage` left byte-identical to original.
