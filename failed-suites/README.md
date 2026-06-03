# Failed-test suites — re-run CI failures locally & track them over time

Every CI run (Parallel Suite + Parallel Suite 2) collects the tests that **failed**
and writes a dated, runnable TestNG suite here. Use it to (a) re-run just the
failures on your machine, and (b) track how failures change day to day.

## Files

| File | What |
|------|------|
| `failed-tests-YYYY-MM-DD.xml` | All tests that failed on that date (one consolidated file across every CI group). |
| `failed-tests-latest.xml` | A copy of the most recent dated suite — handy default. |
| `HISTORY.md` | One line per day: date + failure count + link. The tracker. |

## Run the failures locally

```bash
# the most recent failures
mvn test -DsuiteXmlFile=failed-suites/failed-tests-latest.xml
# or a specific day
mvn test -DsuiteXmlFile=failed-suites/failed-tests-2026-06-03.xml
# convenience wrapper (latest, or pass a date)
failed-suites/run.sh
failed-suites/run.sh 2026-06-03
```

Data-driven tests are included by method name, so re-running covers **all** of that
test's data rows (TestNG can't re-run a single data-row by index).

## Regenerate locally after your own run

After running any suite locally, rebuild today's failed suite from the surefire
reports your run produced:

```bash
python3 .github/scripts/collect-failed-tests.py --label "local"
```

> Tip: `mvn clean` (or clear `target/surefire-reports/`) before a run so the
> collected list reflects only that run — surefire keeps the last result per class,
> so stale results from earlier runs can otherwise linger.

## How it works (CI)

1. **Each group job** runs its tests, then extracts the failures from its own
   `target/surefire-reports/` into a flat `FQCN#method` list (uploaded as an artifact).
2. **The summary job** downloads every group's list, merges them (`--merge` unions
   with any file already committed for the day, so both pipelines contribute to the
   same dated file), writes the dated suite, updates `HISTORY.md`, and commits it
   back to `main` (same `[bot]` pattern as the daily analysis).

Engine: [`.github/scripts/collect-failed-tests.py`](../.github/scripts/collect-failed-tests.py)
(`--extract-only` per group, `--build --merge` in the summary).
