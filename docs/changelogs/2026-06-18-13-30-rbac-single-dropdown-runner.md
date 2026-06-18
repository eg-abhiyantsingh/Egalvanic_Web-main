# RBAC manual runner — one granular "Which RBAC to run" dropdown

- **Date:** 2026-06-18
- **Prompt:** "[screenshot of a single 'Which jobs to run' dropdown] give option like this to run"
- **Module:** Authentication → Role-Based Access (CI ergonomics)
- **Type:** GitHub Actions `workflow_dispatch` UX + TestNG suite plumbing.

---

## Ask

The user shared a screenshot of another workflow that uses a **single flat dropdown** (`all`,
`smoke`, `authentication-only`, `assets-p1-…`, …) and asked the RBAC manual runner to work the
same way — one granular picker — instead of the previous **two** dropdowns (`scope` + `role`).

## What changed

- **`.github/workflows/rbac.yml`** — replaced the `scope` + `role` inputs with a single `which`
  `choice` input. 16 options: `all`, `api-all`, `ui-all`, `login-e2e`, four single-API-slice
  options, two single-UI-slice options, and six `role-*` options (everything, one role only).
  The run step maps each option to Maven args via a `case "$WHICH"` over an env var
  (injection-safe; array keeps the `"Project Manager"` space intact). Summary / artifact-name /
  authoritative gate steps updated to the single input.

- **Six new single-slice suite files** (root): `testng-rbac-contract.xml`, `…-cells.xml`,
  `…-workorder-api.xml`, `…-actions.xml`, `…-nav-ui.xml`, `…-workorder-ui.xml`.

## Why suites, not `-Dtest=`

The pom hard-wires surefire to a `<suiteXmlFiles>` element (`${suiteXmlFile}`, default
`testng.xml`). When surefire has a suite configured it **ignores** the `-Dtest=` class filter —
so a per-class dropdown option built on `-Dtest=` would silently run nothing useful. Every option
therefore maps to a real suite XML; the per-class slices are tiny one-class suites.

## Validation (not just compile)

- `rbac.yml` parses as YAML; all six new suite XMLs are well-formed and reference existing classes.
- Ran the `action-enforcement-create-edit` option exactly as the workflow does
  (`mvn -B test -Dheadless=true -DSEND_EMAIL_ENABLED=false -DsuiteXmlFile=testng-rbac-actions.xml`):
  **20 passed / 0 failed / 4 skipped** (skips = mis-provisioned Admin + no-account Electrical
  Engineer × actions — expected). Proves the dropdown→suite mapping runs end-to-end.
- `role-*` options reuse the already-green `testng-rbac-all.xml` + `-Drbac.roles` machinery
  (validated in prior runs: all/all → 823/0; api/PM → 116).

## Catalog

`docs/test-coverage/RBAC_TEST_CASES.md` gained a "One-click manual runner" table mapping every
dropdown option → suite → what runs.
