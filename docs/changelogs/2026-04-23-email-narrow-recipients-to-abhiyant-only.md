# 2026-04-23 — Narrow email recipients to abhiyant.singh@egalvanic.com only

## Prompt
> update everywhere email to only abhiyant.singh@egalvanic.com. remove all the other email address. and push to main branch.

## Model in use
Claude Opus 4.7 (1M context), xhigh effort, always-thinking on.

## Branch safety
Work on `main` of the **QA automation framework repo**. Production website repo untouched.

## What changed

Narrowed the CI auto-email recipient list so automated test-report emails now go **only** to `abhiyant.singh@egalvanic.com`. The prior list (`dharmesh.avaiya`, `mukul`, `abhiyant.singh`) is removed from all live config.

### Files changed

| File | Line | Before | After |
|---|---:|---|---|
| [.github/workflows/parallel-suite.yml](.github/workflows/parallel-suite.yml) | 396 | `"dharmesh.avaiya@egalvanic.com, mukul@egalvanic.com, abhiyant.singh@egalvanic.com"` | `"abhiyant.singh@egalvanic.com"` |
| [.github/workflows/parallel-suite-2.yml](.github/workflows/parallel-suite-2.yml) | 369 | same | `"abhiyant.singh@egalvanic.com"` |
| [src/main/java/com/egalvanic/qa/constants/AppConstants.java](src/main/java/com/egalvanic/qa/constants/AppConstants.java) | 265-266 | `//public static final String EMAIL_TO = "dharmesh.avaiya..., mukul..., abhiyant...";`<br>`public static final String EMAIL_TO = " abhiyant.singh@egalvanic.com";` (leading space) | `public static final String EMAIL_TO = "abhiyant.singh@egalvanic.com";` (no leading space, no dead commented line) |

### Not changed

- Historical changelog files under `docs/changelogs/` — these are a record of what the config was on a given date, so rewriting them would falsify history
- Role-credential constants in AppConstants (`ADMIN_EMAIL`, `PM_EMAIL`, `TECH_EMAIL`, `FM_EMAIL`, `CP_EMAIL`, `VALID_EMAIL`) — these are test login identities, not notification recipients
- `EMAIL_FROM` — the sender address stays as `abhiyant.singh@egalvanic.com` (unchanged)
- The `getEnv()` override hook on `EMAIL_FROM` — runtime env overrides still work; just the default baked-in recipient list shrank

## Verification

```bash
# YAML parses cleanly
python3 -c "import yaml; yaml.safe_load(open('.github/workflows/parallel-suite.yml'))"    # → OK
python3 -c "import yaml; yaml.safe_load(open('.github/workflows/parallel-suite-2.yml'))"  # → OK

# Java still compiles
mvn clean test-compile   # → BUILD SUCCESS, 56 test sources

# No dharmesh/mukul leftovers in live code (docs/ excluded — those are historical)
grep -rn "dharmesh\|mukul" --include="*.java" --include="*.yml" --include="*.py" \
  --exclude-dir=target --exclude-dir=.git --exclude-dir=docs
# → empty output

# All live EMAIL_TO assignments point to only abhiyant.singh@egalvanic.com
grep -rn "EMAIL_TO\s*[=:]" --include="*.java" --include="*.yml" --include="*.py" \
  --exclude-dir=target --exclude-dir=.git
# → 3 lines, all "abhiyant.singh@egalvanic.com"
```

## Effect on next CI run

- Parallel Suite 1 (`parallel-suite.yml`) — sends summary email only to you
- Parallel Suite 2 (`parallel-suite-2.yml`) — sends consolidated + client-polished report only to you
- Local test runs using the Java framework — `EmailUtil.send()` reads `AppConstants.EMAIL_TO` and emails only you

## In-depth explanation (for learning + manager reporting)

### Why narrow the recipient list?
Two common reasons teams do this:
1. **Review gate before forwarding to client** — you want to inspect the report before it goes to the broader team or client. Getting it in your inbox alone lets you forward when ready.
2. **Reducing inbox noise during active QA churn** — while tests are unstable (like now, with smoke-test races and known-bug failures surfacing), emailing the whole team every run creates alert fatigue.

Either way, the same fix: one recipient, overridable at runtime.

### Why fix the leading space in the Java constant?
Previous value was `" abhiyant.singh@egalvanic.com"` (note the leading space). `EmailUtil.java` line 89 does:
```java
String[] recipients = AppConstants.EMAIL_TO.split("\\s*,\\s*");
```
That regex trims whitespace *around commas* but not at the start of the string. Result: `recipients[0] == " abhiyant.singh@egalvanic.com"` (with leading space).

Most SMTP servers trim it silently, but some (strict Postfix configs, some corporate relays) reject malformed addresses. Fixing the constant is cheaper than debugging a flaky send later.

### Why historical changelogs keep the old email list
Changelog files are an append-only record. Rewriting them to "match current state" would:
- Destroy the audit trail of what was configured on each date
- Break any future debugging ("when did we remove dharmesh from the list?")

Only live config (code + workflow YAML + runtime defaults) should reflect the current state. Historical narratives stay historical.

### Runtime override still works
The workflow YAMLs set `EMAIL_TO` as an env var, which `consolidated-report.py` reads via `os.environ.get('EMAIL_TO', 'abhiyant.singh@egalvanic.com')`. If you ever want to temporarily re-broaden the list (e.g., for a release announcement run), you can:
1. Re-dispatch the workflow with a manual override
2. Or edit the YAML EMAIL_TO for one run, then revert

The code still handles comma-separated lists correctly — the recipient-split logic is unchanged.

## Rollback
`git revert <this-commit>` restores the 3-person recipient list + the leading-space value + the dead commented line.
