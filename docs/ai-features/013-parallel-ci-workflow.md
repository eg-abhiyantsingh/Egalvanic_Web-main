# Parallel CI Execution Workflow

> **Author:** Abhiyant Singh (with Claude AI assistance)  
> **Date:** April 9, 2026  
> **Prompt:** "Create .yml file to run parallel execution"  
> **File:** `.github/workflows/parallel-suite.yml`

---

## The Problem

The existing `full-suite.yml` workflow runs all 10 test groups **sequentially** on a single GitHub Actions runner. Each group starts a fresh Chrome session, runs its tests, saves results, then the next group starts. Total wall-clock time: **~4-4.5 hours**.

```
Sequential Timeline (single runner):
┌──────┬──────┬──────┬──────┬──────┬──────┬──────┬──────┬──────┬──────┐
│ G1   │ G2   │ G3   │ G4   │ G5   │ G6   │ G7   │ G8   │ G9   │ G10  │
│ 25m  │ 30m  │ 55m  │ 20m  │ 20m  │ 35m  │ 15m  │ 30m  │ 15m  │ 10m  │
└──────┴──────┴──────┴──────┴──────┴──────┴──────┴──────┴──────┴──────┘
                        Total: ~4 hours
```

## The Solution

A new `parallel-suite.yml` workflow that runs all 10 groups **simultaneously** using GitHub Actions `strategy.matrix`. Each group gets its own Ubuntu runner with its own Chrome browser.

```
Parallel Timeline (10 runners):
Runner 1:  ┌── G1 (25m) ──┐
Runner 2:  ┌── G2 (30m) ───┐
Runner 3:  ┌── G3 (55m) ────────────┐  <- bottleneck
Runner 4:  ┌── G4 (20m) ─┐
Runner 5:  ┌── G5 (20m) ─┐
Runner 6:  ┌── G6 (35m) ────┐
Runner 7:  ┌── G7 (15m) ┐
Runner 8:  ┌── G8 (30m) ───┐
Runner 9:  ┌── G9 (15m) ┐
Runner 10: ┌── G10 (10m)┐
Summary:                              ┌── Aggregate (2m) ──┐
                        Total: ~1 hour
```

**Speedup: ~4x** (wall-clock time drops from ~4h to ~1h)

## Architecture: 2-Phase Design

### Phase 1: `test-group` Job (10 parallel instances)

Uses `strategy.matrix.include` to define all 10 groups with their properties:

```yaml
strategy:
  fail-fast: false    # CRITICAL: don't cancel other groups if one fails
  max-parallel: 10    # All 10 simultaneously

  matrix:
    include:
      - group: auth-site-connection
        name: "Auth + Site + Connection"
        xml: suite-auth-site-connection.xml
        tests: 130
      # ... 9 more groups
```

Each parallel job:
1. **Sets up its own environment**: JDK 17 + Chrome + Maven cache
2. **Compiles the project**: Each runner compiles independently (fast due to Maven cache)
3. **Runs via dashboard script**: `bash .github/scripts/full-suite-dashboard.sh` with `FULL_SUITE_GROUP` set to the group key — same clean per-test progress UI as the sequential workflow
4. **Writes a result JSON**: Small file with group name, status, and counts (read from dashboard's `GITHUB_ENV` output)
5. **Uploads artifacts**: Test reports + result JSON, named uniquely per group

**Why `fail-fast: false` is critical:** By default, GitHub Actions cancels remaining matrix jobs when one fails. We need ALL groups to complete so we see the full picture — not just the first failure.

### Phase 2: `summary` Job (1 instance, runs after all groups)

```yaml
summary:
  needs: test-group    # Waits for ALL 10 matrix jobs
  if: always()         # Runs even if some groups failed
```

This job:
1. **Downloads all result JSONs** from the 10 parallel jobs
2. **Aggregates** passed/failed/skipped counts across all groups
3. **Generates a combined GitHub Actions step summary** with a table
4. **Sets the final pass/fail status** for the workflow

## Key Design Decision: Result Passing via JSON Artifacts

GitHub Actions doesn't let you pass outputs between matrix job instances. The solution: each job writes a tiny JSON file and uploads it as an artifact. The summary job downloads all 10 JSON files and aggregates.

```json
{
  "group": "workorder-issue",
  "name": "Work Order + Issue",
  "expected_tests": 234,
  "passed": 230,
  "failed": 3,
  "skipped": 1,
  "status": "failed",
  "duration_seconds": 3250
}
```

## Dashboard Script Reuse

Both `full-suite.yml` (sequential) and `parallel-suite.yml` (parallel) now use the **same dashboard script**: `.github/scripts/full-suite-dashboard.sh`.

The script supports single-group execution via the `FULL_SUITE_GROUP` environment variable:
- `FULL_SUITE_GROUP=all` (or unset) — runs all 10 groups sequentially (used by full-suite.yml)
- `FULL_SUITE_GROUP=auth-site-connection` — runs only that group (used by parallel-suite.yml)

This means:
- **Identical clean UI** in both workflows: per-test progress icons, collapsed raw Maven output
- **Zero code duplication**: one script, two workflows
- **Consistent result format**: `SUITE_PASSED`, `SUITE_FAILED`, etc. written to `GITHUB_ENV`

## Security: No Command Injection

All dynamic values in `run:` blocks are passed via `env:` variables (not `${{ }}` interpolation). This follows GitHub's security best practices. Example:

```yaml
# SAFE: matrix value goes through env
env:
  FULL_SUITE_GROUP: ${{ matrix.group }}
run: |
  bash .github/scripts/full-suite-dashboard.sh

# UNSAFE (we avoid this): direct interpolation
run: |
  mvn test -DsuiteXmlFile="${{ matrix.xml }}"
```

## Cost Comparison

| Metric | Sequential | Parallel |
|--------|-----------|----------|
| Wall-clock time | ~4 hours | ~1 hour |
| Total runner-minutes | ~240 min | ~300 min |
| Runners used | 1 | 10 + 1 (summary) |
| Feedback speed | 4h | 1h |

Parallel uses ~25% more total runner-minutes but delivers results **4x faster**. On GitHub Free (2,000 min/month), you get ~6 full parallel runs. On GitHub Pro/Teams (3,000 min/month), ~10 runs.

## When to Use Which Workflow

| Workflow | Use When |
|----------|----------|
| `parallel-suite.yml` | Want fastest results. Typical for PR validation or daily CI. |
| `full-suite.yml` | Want the sequential dashboard or lower runner cost. Good for overnight runs. |
| `smoke-tests.yml` | Quick critical-path check (37 TCs, ~10 min). Pre-merge validation. |

## Files Created/Modified

| File | Type | Description |
|------|------|-------------|
| `.github/workflows/parallel-suite.yml` | NEW | Parallel execution workflow with matrix + summary |

---

## Commit History

| Commit | Description |
|--------|-------------|
| `a9eb331` | Initial parallel-suite.yml (295 lines, raw mvn output) |
| *(this commit)* | Refactored to use dashboard script — clean UI matching full-suite.yml |
