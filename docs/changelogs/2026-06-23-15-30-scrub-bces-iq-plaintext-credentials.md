# Scrub plaintext BCES-IQ credentials (move to repo secrets, stop logging username)

- **Date:** 2026-06-23
- **Prompt:** "remove all log username and password for bces-iq"
- **Type:** Security cleanup — remove committed plaintext credentials + stop logging them.

---

## What was leaked

The BCES-IQ customer tenant's login **username** (`shubham.goswami+acme@…`) and **password**
(`Shubham@123`) were committed in plaintext across four files, and the username was printed at runtime.

## Changes

| File | Before | After |
|------|--------|-------|
| `.github/workflows/parallel-suite-2.yml` | matrix entry hardcoded `user_email` + `user_password`; env read `${{ matrix.user_password }}` | matrix keeps only `base_url`; env injects `${{ secrets.BCES_IQ_USER }}` / `${{ secrets.BCES_IQ_PASSWORD }}` **only for the `bces-iq-smoke` group** (other groups get `''` → `getEnv()` falls back to acme defaults) |
| `smoke-bces-iq-testng.xml` | local-run example with real email + `Shubham@123` | placeholders `$BCES_IQ_USER` / `$BCES_IQ_PASSWORD` |
| `BcesIqSmokeTestNG.java` | javadoc hardcoded the email; line 188 logged `"…for user: " + VALID_EMAIL` | javadoc says "from BCES_IQ_USER secret"; log is now `"Submitting configured credentials"` (username/password not logged) |
| `docs/changelogs/2026-04-23-…md` | email + password in prompt quote, examples, matrix snippet, and an insight note saying "password lives as plaintext" | all occurrences redacted to `«…secret»`; narrative updated to the secret-based approach |

## Safety / correctness

- `AppConstants.getEnv()` treats empty-string as "use default", so injecting `''` for the acme groups
  leaves them on the standard acme.qa credentials — **no other suite is affected**.
- Compiles clean (`mvn test-compile`); `parallel-suite-2.yml` is valid YAML.
- The **acme/PZ** hardcoded QA credentials in `AppConstants` are intentional (per CLAUDE.md) and were
  **left untouched** — this change is scoped to the BCES-IQ customer tenant only.
- `testcase/Jira.csv` (offline ticket export) references `bces-iq` URLs/tickets but contains **no test
  login credentials**, so it was left as-is.

## Action required by the repo owner

1. Set the `BCES_IQ_USER` and `BCES_IQ_PASSWORD` GitHub repo secrets (the workflow now reads them).
2. **Rotate the leaked `Shubham@123` password** — it remains in prior git history; scrubbing the
   working tree does not purge history.
