# Authenticated CRUD / write-path API coverage — 4 new Suite-3 contract suites

**Date:** 2026-07-08
**Author:** Claude Code (automation), for abhiyant.singh@egalvanic.com

## What was asked
> "cover remaining api testing test case, divide [into parts] … go deeper, use Playwright to
> understand the end-to-end flow, trial-and-error until it works, never headless, don't ask."

The API suite so far was **read-only**: health check, list-API pagination contract, pagination
behavior, error contract, security headers, and the spec-driven full-catalog probe (which hits
mutating endpoints *unauthenticated* — auth-gate only). Nothing proved that an **authenticated
write actually works and stays correct**. This fills that gap with four new classes under
`src/test/.../testcase/api/`, wired into Parallel Suite 3 (`suite-api-health.xml`).

## Method (why this took the long path, not the lazy one)
Swagger marks every write body `"contract not yet pinned … any JSON object accepted"`, so the spec
is useless for real payloads. I drove the **actual UI with Playwright** (logged in, switched to the
dedicated sandbox site "test site for api check") and captured the real create/update/delete
requests off the network panel:
- `POST /task/create {sld_id, task_type, completed, title, task_description}` → 201 `{data:{id}}`
- `PUT /task/update/{id}` (partial ok) · `DELETE /task/delete/{id}` (needs a JSON Content-Type)
- `POST /issue/create {…, issue_class, details:[…]}` → 201 · issues have **no** DELETE (soft-delete
  via `PUT {is_deleted:true}`) · list via `POST /v2/issues/list`
- `POST /location/building/ {name, access_notes, sld_id}` → `{building:{…}}` envelope
- discovered the **dual write path**: header `x-direct-write: true` = synchronous (full object back);
  without it the write is queued → `{_mutation:{status:"received"}}` and converges a moment later.

Then every negative/edge case was **curl-verified before being encoded** (per the don't-over-report
rule) and each suite was run live and iterated until correct.

## What was built

### 1. `CrudLifecycleApiTest` (3 tests, hard-fail)
Authenticated create → read → list-visible → partial-update-sticks → delete → delisted, for **task**,
**issue** (soft-delete path), and **building** (hard DELETE is 405 by design). Uses `x-direct-write`
for deterministic assertions. Live: **3/3 green**.

### 2. `InputValidationApiTest` (5 tests, report-by-default + `-DSTRICT_INPUT_VALIDATION`)
Malformed/negative authenticated bodies must yield **4xx, never 5xx**, and must not **leak internals**.
Live default **5/5 green**; STRICT **5/5 fail** on real, curl-verified backend defects (see Findings).

### 3. `FilterSearchConsistencyApiTest` (6 tests, read-only)
Correctness of `POST /v2/issues/list` (the one genuinely paginated+filterable list API): filter purity,
status/priority partition == total, search finds-what-exists / invents-nothing, pagination disjoint +
bounded beyond-end, **list-projection == detail record**, filter∧search intersection. Invariants hold
live → hard-fail as a regression guard. The array-valued-filter case is a report-by-default DEFECT
(`-DSTRICT_CONSISTENCY_CONTRACT` gates). Live default **6/6 green**.

### 4. `MutationSemanticsApiTest` (4 tests, hard-fail)
Async-queue **eventual convergence** vs synchronous `x-direct-write`, **delete idempotency**
(double-delete → both 2xx, delisted, no resurrection), and the **DELETE media-type contract**.
Live: **4/4 green**. Documents a real read-after-write lag: a direct-write create is not reflected by
`GET /task/{id}` within ~3s though it is visible in the list (warn-only).

Combined run of all four (18 tests) in one JVM: **18/18 green, zero sandbox residue** afterward.

## Findings (VERIFIED with curl before labelling — [[feedback_dont_overreport_sld_bugs]])
Real backend defects surfaced (reproduced ≥2×; `POST /location/building/` proves the platform *can*
validate cleanly, so these are gaps, not by-design):
1. **5xx + DB/stack leakage on client input** — `POST /task/create` with `{}` / missing `sld_id` /
   bad-UUID / a top-level array → **500** leaking `psycopg2 … column "sld_id" of relation "tasks"` /
   `invalid input syntax for type uuid` / `'list' object has no attribute 'get'`.
   `POST /v2/issues/list` with `page=-1` / `page="abc"` / `page_size=-5` → **500** leaking
   `OFFSET/LIMIT must not be negative` and Python type errors. An **array-valued filter**
   (`filters:{status:["Open"]}`) → **500** leaking `operator does not exist: character varying = text[]`.
   → information disclosure (OWASP API8) + unhandled-exception robustness.
2. **Over-permissive create** — `task/create` accepts a missing/mistyped title (201); `issue/create`
   accepts `{}`, a missing `issue_class`, and an invalid `status` enum (201). No server-side validation.
3. **SPA-shell masking on a mutation** — `PUT /task/update/{unknown-id}` → **200 + HTML**, so a client
   cannot tell the write was a no-op.
4. **Read-after-write lag** — noted above (warn-only).

Non-findings I *did not* report (verify-first paid off): `search="test"` returning all 16 rows is
**correct** — the site is named "Android **Test** nauu" and search covers `sld_name`; a `thermal`
hit on a row titled "yyii" is **correct** — it's a Thermal-class issue (matched via `issue_class_name`).

## Safety / CI hygiene
- All mutations run **only** on the sandbox SLD "test site for api check", resolved **by name** at
  runtime — each class **skips** if it is absent, so a clean CI image never writes to a real site.
- Every record carries a class marker (`ApiCrud_` / `ApiInval_` / `ApiMut_`); `@AfterClass` deletes this
  run's records **and** sweeps markers/junk a previously-crashed run may have stranded.
- Report-by-default gating (mirrors `ListApiContractApiTest`): the daily monitor stays **green**; the
  `STRICT_*` flags escalate the backend findings to hard failures for enforcement / regression-gating.

## Wiring
- `suite-api-health.xml` (Parallel Suite 3): added the four `<test>` blocks (read-only consistency near
  the other contract tests; the three mutating classes grouped with an explicit sandbox-safety note).
- `.github/workflows/parallel-suite-3.yml`: the four new `reports/*-report.md` files are uploaded in the
  `api-health-report` artifact and appended to the run summary.

## Depth note (learning / manager review)
The load-bearing decision was **how to test writes on a shared live QA tenant without leaving a mess or
turning the daily monitor red**. Three levers made it safe: (a) resolve a *named* sandbox and refuse to
run anywhere else, (b) marker + `@AfterClass` sweep so a crash mid-test still self-heals next run,
(c) report-by-default with a STRICT enforcement gate so real backend defects are documented loudly every
run but only block a build when you opt in. The calibration lesson repeats the SLD one: a surprising
result (`search=test`→all rows) is not a bug until you've found the field that explains it — here,
`sld_name`. Verifying first kept two false "search is broken" claims out of the report.
