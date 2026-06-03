# [Bug | HIGH] Planning search throws uncaught `TypeError: Qe is not a function` on non-empty queries

**Test:** `com.egalvanic.qa.testcase.Phase4QualityGatesTestNG.testSearchInputBoundary`
**Date:** 2026-06-03
**Classification:** REAL_BUG (98% confidence — reproduced on ~every input across two live runs)
**Severity:** HIGH (uncaught runtime exception fires on essentially every keystroke in a core search box)
**Page URL:** https://acme.qa.egalvanic.ai/planning
**Bundle:** `assets/index-D8s0l98b.js` at `2729:221111`

## Summary

Firing **any input event** on the **Work Order Planning** search box throws an
**uncaught `TypeError: Qe is not a function`** from the production bundle. Across
two live runs (2026-06-03) it reproduced on essentially every input — single
char, special, unicode, injection probes, long repeated strings, whitespace, and
even **empty** input (it threw twice for empty in the second run). That breadth
means the crash is in the **search `onChange`/input handler itself**, not the
result-render branch: a minified symbol `Qe` is invoked as a function when it
isn't one (an undefined/renamed callback or a binding that's `undefined` at the
moment the search handler runs).

## Steps to Reproduce

1. Login to `https://acme.qa.egalvanic.ai` with the standard QA credentials.
2. Navigate to `https://acme.qa.egalvanic.ai/planning`; wait for the grid.
3. Open DevTools → Console.
4. Type any non-empty term into **"Search Work Order Planning"** — e.g. `a`.
5. Observe the console.

## Actual Result

```
Uncaught TypeError: Qe is not a function
    at assets/index-D8s0l98b.js:2729:221111
```
Thrown as an uncaught exception (not caught/handled by the app).

## Expected Result

Searching must never throw. A query either filters the grid or shows a graceful
empty-state — with zero uncaught console exceptions.

## Reproduction matrix (two live runs, 2026-06-03)

| Input case        | Term                        | Run 1 | Run 2 |
|-------------------|-----------------------------|-------|-------|
| empty             | `""`                        | no    | **YES (×2)** |
| single-char       | `a`                         | **YES** | **YES** |
| max-255           | `x`×255                     | no    | **YES** |
| over-255          | `y`×256                     | no    | **YES** |
| huge-5k           | `z`×5000                    | no    | **YES** |
| unicode           | `测试🚀Ωüc-ñ`                 | **YES** | **YES** |
| sql-injection     | `' OR 1=1 --`               | **YES** | **YES** |
| xss               | `<script>window.__qg=1</script>` | **YES** (script did NOT execute — good) | **YES** |
| whitespace        | `"     "`                   | no (transient branding noise only) | **YES** |
| special           | `` !@#$%^&*()_+{}|:"<>?~` `` | **YES** | no |

Run 2 crashed on 9/10 inputs (including empty). The set that crashes is not tied
to query content, so the fault is in the **search input handler itself**, fired
on every input event — not a result-rendering edge case.

## Likely Root Cause

Minified `Qe` is called as a function in the Planning search/filter handler but
resolves to a non-function (undefined export, renamed helper after a refactor, or
a result-row callback that's `undefined` when results are present). De-minify
`index-*.js:2729:221111` against the sourcemap to get the symbol; inspect the
search `onChange`/filter→render chain on the Planning route.

## Suggested Fix

- Map `2729:221111` via sourcemap to the real identifier behind `Qe`.
- Guard the call site (`typeof fn === 'function'`) and fix the undefined binding.
- Add a unit/component test that drives the Planning search with a matching term.

## Note on test handling

This case is left **intentionally red** in `Phase4QualityGatesTestNG`. The transient
SLD `502` (unicode case) and `BrandingService` branding-fetch failure (whitespace
case) were whitelisted in `AppConstants.HEALTH_GATE_IGNORE` as environment noise,
but `Qe is not a function` is **not** whitelisted because it is a genuine app crash.

## Screenshot

The boundary test captures a screenshot on failure under `test-output`/Extent
report for each failing input (`Boundary_<case>`).

---

_Authored from the live Phase-4 quality-gates run on 2026-06-03. Review/edit before
filing. **Do not** push this file's contents to Jira via tooling — per project rule,
Jira modifications need explicit per-ticket approval._
