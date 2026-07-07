# 2026-07-07 — Parallel Suite 3: pagination behavior + error & transport contracts land in CI

## Prompt
"api testing in parallel suite 3 you should also add pagination api testing also."

## What was actually wrong
Parallel Suite 3 (`suite-api-health.xml`, `.github/workflows/parallel-suite-3.yml`) *appeared*
to have pagination testing locally — but none of it had ever been committed. A previous session
built three test classes and wired two into the suite XML, then stopped before `git add`:

- `PaginationBehaviorApiTest.java` — **untracked**
- `ErrorContractApiTest.java` — **untracked** (but referenced by the uncommitted suite-XML diff —
  committing the XML alone would have broken CI with a missing class)
- `SecurityHeadersApiTest.java` — **untracked AND truncated mid-line at line 250** (unclosed
  string literal), which broke `mvn test-compile` for the entire test tree
- `suite-api-health.xml` — modified only in the working tree

So CI's Suite 3 was still running only the health check + list-API contract + full catalog.

## Changes
1. **Completed `SecurityHeadersApiTest`** (~30 missing lines): closed the interrupted
   `catch` in `testVersionLeakage`, added the version-leak verdict (REPORT row, STRICT
   escalation, WARN otherwise) and the `@AfterClass writeReport()` →
   `reports/security-headers-report.md`, all matching the sibling classes' pattern.
2. **Wired `SecurityHeadersApiTest` into `suite-api-health.xml`** as
   "API Transport Contract (headers/CORS/content-type)" — its javadoc explicitly targets
   Suite 3; leaving it unwired would repeat the half-finished-work problem.
3. **Workflow artifact upload** now includes `pagination-behavior-report.md`,
   `error-contract-report.md`, `security-headers-report.md`.
4. Committed all three test classes + suite XML + workflow.

## Validation (ran live against QA, not just compiled)
- `PaginationBehaviorApiTest`: **45 tests (9 endpoints × 5 checks), 0 failures, 33 skipped.**
  The skips are the point: 8/9 endpoints ignore `per_page` entirely (sessions returned 827
  records, workorders 806, node_classes 555 when asked for 5) → WARN + skip in report mode,
  exactly the 2026-07-01 contract-audit gap. The one paginating endpoint
  (`sld.library-designations`) passed all 5 semantic checks. All 9 survived 6 abusive
  param combos with zero 5xx. Report: `reports/pagination-behavior-report.md`.
- `SecurityHeadersApiTest`: **4/4 pass** in report mode with real findings — 1 API path
  answering HTML on 200, 4 endpoints serving cacheable tenant JSON, 1 version-leaking
  header, 0 credentialed CORS reflections (the only hard-fail condition).

## Depth explanation (for learning / manager review)
**Why a separate "behavior" class when `ListApiContractApiTest` already exists?**
The contract test audits *policy* (is `per_page` honored, is the default bounded). The
behavior test audits *semantics* — the bugs users actually hit while paging a grid:

- **Disjoint pages** — page 1 ∩ page 2 must be empty. SQL `OFFSET/LIMIT` without a
  deterministic `ORDER BY` shuffles rows between fetches, so users see some rows twice
  and *never see others* (silent data loss no single-request health check can catch).
- **Stable ordering** — the same request twice must return identical order; catches the
  same unstable-sort root cause directly.
- **Beyond-the-end page** — `page=999999` must be an empty 2xx or clean 4xx, never a 5xx
  and never the full dump.
- **Abusive params** — `page=0/-1/abc`, `per_page=0/-5/abc` must never 5xx (client input
  must never crash the server).
- **Total consistency** — envelope `total` identical across pages and consistent with
  page maths (a total that changes between page 1 and page 2 means per-page counting or
  cache races).

**Escalation design:** unpaginated endpoints WARN + skip under the *same*
`-DSTRICT_LIST_API_CONTRACT=true` flag as the contract audit — one switch gates both
suites once the backend complies, and until then Suite 3 stays green on known gaps while
hard-failing anything objective (5xx, >8s hang, semantic violations on paginating endpoints).
