# API write-path coverage — CRUD lifecycle, input validation, filter/search consistency, mutation semantics

- **Date:** 2026-07-08
- **Prompt:** "cover remaining api testing test case, divide [into parts] … go deeper, use Playwright to understand the end-to-end flow, trial-and-error until it works, never headless, don't ask."
- **Type:** New authenticated CRUD / write-path API contract coverage (4 classes) wired into Parallel Suite 3.

---

## Gap closed
Suite 3 was entirely read-only (health / pagination / error / headers / spec-catalog probes — the
catalog even hits mutating endpoints *unauthenticated*, so it's an auth-gate check only). Nothing
proved an **authenticated write round-trips correctly**. Added, in `src/test/.../testcase/api/`:

| Class | Tests | Gate | What it proves |
|---|---|---|---|
| `CrudLifecycleApiTest` | 3 | hard-fail | task/issue/building create→read→update→delete persists & delists |
| `InputValidationApiTest` | 5 | report + `STRICT_INPUT_VALIDATION` | bad body → 4xx-not-5xx, no internal leakage |
| `FilterSearchConsistencyApiTest` | 6 | hard-fail + `STRICT_CONSISTENCY_CONTRACT` | filter purity, search, pagination, list==detail |
| `MutationSemanticsApiTest` | 4 | hard-fail | async-queue convergence vs sync direct-write, delete idempotency, DELETE media-type |

## How the real contracts were obtained
Swagger bodies are unpinned, so I drove the **UI with Playwright** on the sandbox site
"test site for api check" and captured the real create/update/delete network calls; every
negative/edge case was **curl-verified before being encoded**.

## Live validation
Each class run individually + all four together (**18/18 green, zero sandbox residue**), plus the
full 10-class `suite-api-health.xml`. Report-by-default → daily monitor stays green; `STRICT_*`
flags escalate the real backend findings.

## Verified backend defects surfaced (curl ×2+, not by-design — building/create validates cleanly)
- **500 + psycopg2/SQL leakage** on malformed input: `task/create` `{}`/bad-uuid/array;
  `/v2/issues/list` `page=-1`/`"abc"`/`size=-5`; array-valued filter `{status:["Open"]}`.
- **Over-permissive create** (missing title / missing issue_class / bad status enum → 201).
- **`PUT /task/update/{unknown}` → 200 + HTML** SPA shell (no-op indistinguishable).
- **Read-after-write lag** on `GET /task/{id}` after a direct-write create (~3s; list shows it).

## Safety
Mutations only on the by-name-resolved sandbox (SKIP if absent); markers `ApiCrud_`/`ApiInval_`/`ApiMut_`;
`@AfterClass` deletes this run's records + sweeps strays from crashed runs.

## Wiring
`suite-api-health.xml` (+4 `<test>` blocks) and `.github/workflows/parallel-suite-3.yml`
(4 new `reports/*-report.md` uploaded + appended to run summary).
