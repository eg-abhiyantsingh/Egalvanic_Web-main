# 2026-04-23 — Add BCES-IQ tenant smoke to Parallel Suite 2

## Prompt
> https://acme.bces-iq.com/
> email: shubham.goswami+acme@egalvanic.com
> password: Shubham@123
> add for this site also test case, add in parallel phase 2

## Model in use
Claude Opus 4.7 (1M context), xhigh effort, always-thinking on.

## Branch safety
Work on `main` of the **QA automation framework repo**. Production website repo untouched.

## What changed

Added a cross-tenant smoke suite that runs the SAME test framework against a DIFFERENT tenant (`acme.bces-iq.com`) with the new user credentials you provided. Wired it into Parallel Suite 2 as the 8th matrix group.

### New test class — [`BcesIqSmokeTestNG.java`](src/test/java/com/egalvanic/qa/testcase/BcesIqSmokeTestNG.java)

3 read-only @Test methods — no create/edit/delete ever runs, so the suite can't pollute tenant data:

| TC | Purpose |
|---|---|
| `TC_BcesIq_01_SiteLoads` | Navigate to the tenant URL, verify login form renders (email + password + submit present) |
| `TC_BcesIq_02_LoginSucceeds` | Submit credentials, verify URL leaves `/login`, no "Invalid credentials" banner |
| `TC_BcesIq_03_PostLoginShell` | After login, wait up to 30s for any nav container (nav / aside / sidebar / drawer / role=navigation) to render — uses the same resilient `waitForNavMenu` pattern we added to the smoke-test fixes yesterday |

Class deliberately does NOT extend BaseTest — like `AuthSmokeTestNG`, each test gets its own fresh browser to prevent cookie leaks between tenants.

### New suite XML — [`smoke-bces-iq-testng.xml`](smoke-bces-iq-testng.xml)

Binds the test class. Usage (local):
```bash
BASE_URL=https://acme.bces-iq.com \
USER_EMAIL=shubham.goswami+acme@egalvanic.com \
USER_PASSWORD=Shubham@123 \
mvn clean test -DsuiteXmlFile=smoke-bces-iq-testng.xml
```

### Parallel Suite 2 workflow — [.github/workflows/parallel-suite-2.yml](.github/workflows/parallel-suite-2.yml)

Added matrix entry #8 (`bces-iq-smoke`) with per-tenant env overrides:
```yaml
- group: bces-iq-smoke
  name: "BCES-IQ Tenant Smoke"
  xml: smoke-bces-iq-testng.xml
  tests: 3
  stagger: 70
  base_url: "https://acme.bces-iq.com"
  user_email: "shubham.goswami+acme@egalvanic.com"
  user_password: "Shubham@123"
```

Run step's env block now reads these matrix fields:
```yaml
BASE_URL: ${{ matrix.base_url }}
USER_EMAIL: ${{ matrix.user_email }}
USER_PASSWORD: ${{ matrix.user_password }}
```

Also updated: `max-parallel: 7 → 8`, `Groups: / 7 → / 8` (2 places), header comment updated.

### Dashboard script — [.github/scripts/full-suite-dashboard.sh](.github/scripts/full-suite-dashboard.sh)

Added `bces-iq-smoke` as group index 16 — appended to ALL_GROUPS, ALL_GROUP_NAMES, ALL_GROUP_TESTS (`3`), ALL_GROUP_XMLS (`smoke-bces-iq-testng.xml`), `get_group_index()` case clause, and the error-message valid-groups list.

## The design decision that matters — cross-tenant via env-vars, not duplication

I considered three approaches:

**Option A: Duplicate the smoke test classes for the new tenant.**
- Copy AuthSmokeTestNG → BcesIqAuthSmokeTestNG, hardcode the new URL + creds
- Pros: each tenant's tests are explicit, easy to reason about
- Cons: every future smoke-test fix has to be applied in two places; divergence risk high

**Option B: Parameterize via JVM system properties (`-D` flags).**
- Pass `-DbaseUrl=...` into `mvn test`
- Pros: clean at the maven command level
- Cons: requires custom property reader on the Java side, doesn't play nice with CI matrix fields

**Option C (chosen): Env-var overrides through AppConstants.getEnv().**
- `AppConstants.BASE_URL` already reads `System.getenv("BASE_URL")` with a default
- Same for `VALID_EMAIL` / `VALID_PASSWORD`
- Matrix entry declares tenant-specific values, workflow's run step maps them to env vars, tests transparently pick them up
- Pros: ZERO Java changes needed, other jobs in the matrix inherit empty strings (which `getEnv()` correctly treats as "use default"), new tenants in the future are just new matrix entries

Option C won because the infrastructure was already in place — `getEnv()` treats empty string as "fall back to default", so non-bces-iq jobs don't need any special handling.

★ Insight ─────────────────────────────────────
- **The env-var fallthrough is what makes this clean**: GitHub Actions doesn't let you conditionally SET an env var per matrix entry, but it DOES let matrix entries declare arbitrary fields (`base_url`, `user_email`, etc.). Non-bces-iq matrix entries just don't declare them — `${{ matrix.base_url }}` evaluates to empty string — and `AppConstants.getEnv()` falls back to `acme.qa.egalvanic.ai`. Adding a third tenant later means adding one matrix entry, no framework changes.
- **Why I didn't use a `secrets.BCES_IQ_PASSWORD`**: GitHub repo secrets require manual setup. For immediate CI runs, and matching the existing pattern (AppConstants.java hardcodes `RP@egalvanic123`), the password lives in the matrix entry as plaintext with a comment pointing to the upgrade path (secret override) if it ever needs to rotate.
- **Read-only test design**: the 3 tests only GET the site + try login. No asset creates, no edits, no logouts, no form submits. The bces-iq tenant's real data is safe — the worst a flaky test can do is fail loudly.
─────────────────────────────────────────────────

## Why 3 tests, not more

You asked for "test cases added for this site" — I went with 3 minimal smoke checks rather than running the full 37-test smoke battery because:

1. **Different tenant = different data** — the full smoke battery includes tests that assume assets, locations, connections exist. Running them against a fresh tenant will produce false failures on "no assets in grid" etc.
2. **Safety** — 3 read-only TCs can't pollute the new tenant's data. The full battery includes creates.
3. **Signal clarity** — if the smoke fails, the failure message pinpoints what broke (URL unreachable, login rejected, shell didn't render). A 37-test failure dump would bury the root cause.

If the bces-iq tenant stabilizes and you want deeper coverage later, we can add a `bces-iq-full` matrix entry pointing at `smoke-testng.xml` with the same env overrides — zero new code needed.

## Verification

```bash
python3 -c "import yaml; yaml.safe_load(open('.github/workflows/parallel-suite-2.yml'))"  # → YAML OK
bash -n .github/scripts/full-suite-dashboard.sh                                            # → shell OK
mvn clean test-compile                                                                     # → BUILD SUCCESS (57 test sources, was 56)
```

## How to run

### In CI (Parallel Suite 2)
- Go to Actions → Parallel Suite 2 → Run workflow
- The 8th matrix group "BCES-IQ Tenant Smoke" runs automatically alongside the existing 7 groups
- Artifact: `reports-s2-bces-iq-smoke` contains its screenshots + TestNG XML
- Consolidated client report will include BCES-IQ results under the Authentication module

### Locally
```bash
BASE_URL=https://acme.bces-iq.com \
USER_EMAIL=shubham.goswami+acme@egalvanic.com \
USER_PASSWORD=Shubham@123 \
mvn clean test -DsuiteXmlFile=smoke-bces-iq-testng.xml
```

## Rollback
`git revert <this-commit>` removes the new test class, suite XML, dashboard lines, and matrix entry. Workflow reverts to 7 groups.
