# After-rerun MERGED reports — detailed report now updates like the client report

**Date:** 2026-07-02
**Time:** 19:05 IST
**Author:** Claude Code (automation), for abhiyant.singh@egalvanic.com

## What was asked
> "After rerun of full automation I didn't get the client and detailed report updated. E.g. my full
> suite has 100 failed tests; rerun all 100; if 50 pass, then the details report and client report
> should update — all test cases shown, the 50 still-failing visible as FAIL and the rest PASS. The
> updated report shows 1000 pass + 50 fail. Now my report will be more accurate."

## What existed vs. what was missing
- **Client report:** already merged. `consolidated-report.py --rerun-dir` overrides each re-run
  test's status over the original full-run results (proven in run 28588272715: 131 tests → after
  rerun 116 pass / 13 fail). No change needed.
- **Detailed report:** NOT merged — the rerun job never produced an after-rerun detailed report at
  all. `consolidated-detailed-report.py` had no rerun support. **This was the gap.**

## What was built

### `consolidated-detailed-report.py` — new `--rerun-detail` / `--rerun-results` flags
1. **Exact merge math from TestNG XMLs** (not HTML scraping): parses every
   `testng-results.xml` under the originals dir and the rerun dir, keyed
   `(class, method, params)` — the same invocation-level key the client report uses, so
   data-driven tests (one @Test × N param rows) merge per-row, not per-method.
   Rerun status **overrides** the original for the same key.
2. **Authoritative merged-totals banner** at the top of BOTH outputs (navigable index +
   single-file): `N tests · P passed · F failed · S skipped`, plus explicit name lists of
   *recovered on re-run* (flaky) and *still failing* (real).
3. **Recovered tests re-badged** inside the original module pages: a small injected
   script/style flips matching failed items to a green "PASS ON RE-RUN (flaky in parallel
   pass)" badge. Name matching Extent-display-name ↔ TestNG-method is a best-effort token
   heuristic — the banner carries the exact truth regardless (honest reporting: the flip is
   visible, not silent).
4. **The rerun's own detailed reports appended** as `[re-run]` modules — so the recovered
   tests' fresh passing steps + screenshots are in the same document.
5. **Hardened XML parse:** refuses any XML carrying `<!DOCTYPE`/`<!ENTITY` (XXE /
   billion-laughs both require a DTD; TestNG never emits one) — stdlib-only, no defusedxml
   dependency on the runner.
6. **Zero-flag behavior unchanged** (regression-checked): without the new flags the output
   is exactly the old report.

### Workflow wiring (both suites)
- `parallel-suite-2.yml` rerun-failed job → generates + uploads
  **`consolidated-detailed-report-suite2-after-rerun`**.
- `parallel-suite.yml` (Suite 1, the 848-TC full automation) rerun-failed job → generates +
  uploads **`consolidated-detailed-report-after-rerun`**.
- Run-summary pages now list all four artifacts (client/detailed × before/after rerun).

## Validation (synthetic, exact)
Fabricated a first pass of 10 invocations (8 PASS / 2 FAIL — one plain method + one
data-driven param row) and a rerun where 1 recovers and 1 still fails. Output asserted:
- banner shows **10 tests · 9 passed · 1 failed** in both index and single-file ✔
- recovered list = `FooTestNG.testFlaky`; still-failing = `BarMatrixTestNG.testColumn(Status)`
  (param-level precision) ✔
- `[re-run]` module appended; re-badge script injected into original modules only ✔
- plain invocation (no flags): no banner, no shim — unchanged ✔

## Where to find it after a run
Actions run page → Artifacts:
- `consolidated-client-report[-suite2]` / `...-after-rerun` — client report before/after
- `consolidated-detailed-report[-suite2]` / `...-after-rerun` — detailed report before/after

The **after-rerun** variants are the accurate ones for reporting: full-suite totals with
flaky recoveries counted as PASS and only genuine failures left as FAIL.
