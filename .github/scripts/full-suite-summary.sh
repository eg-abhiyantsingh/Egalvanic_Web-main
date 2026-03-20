#!/bin/bash
# ═══════════════════════════════════════════════════════════════════════
# FULL TEST SUITE — Summary Aggregator
# ═══════════════════════════════════════════════════════════════════════
# Reads JSON results from each parallel group and generates a
# GitHub Actions step summary with a dashboard table.
#
# Expected input: all-results/*.json files with this structure:
#   { "group": "...", "name": "...", "expected": N,
#     "passed": N, "failed": N, "skipped": N,
#     "duration": N, "exit_code": N }
# ═══════════════════════════════════════════════════════════════════════

set +e

RESULTS_DIR="all-results"

# ─────────────────────────────────────────────────────
# Totals
# ─────────────────────────────────────────────────────
TOTAL_EXPECTED=0
TOTAL_PASSED=0
TOTAL_FAILED=0
TOTAL_SKIPPED=0
TOTAL_DURATION=0
TOTAL_GROUPS=0
FAILED_GROUPS=0

# ─────────────────────────────────────────────────────
# Format duration (seconds → Xm Ys)
# ─────────────────────────────────────────────────────
fmt_duration() {
  local secs=$1
  if [ "$secs" -ge 60 ]; then
    printf "%dm %ds" $((secs / 60)) $((secs % 60))
  else
    printf "%ds" "$secs"
  fi
}

# ─────────────────────────────────────────────────────
# Parse JSON files
# ─────────────────────────────────────────────────────

# Ordered list of groups for consistent display
GROUP_ORDER=(
  "auth-site-connection"
  "location-task"
  "workorder-issue"
  "asset-1-2"
  "asset-3"
  "asset-4-5"
  "dashboard-bughunt"
  "api"
)

declare -A G_NAME G_EXPECTED G_PASSED G_FAILED G_SKIPPED G_DURATION G_EXIT

for f in "$RESULTS_DIR"/*.json; do
  [ -f "$f" ] || continue

  GROUP=$(sed -n 's/.*"group": "\([^"]*\)".*/\1/p' "$f")
  NAME=$(sed -n 's/.*"name": "\([^"]*\)".*/\1/p' "$f")
  EXPECTED=$(sed -n 's/.*"expected": \([0-9]*\).*/\1/p' "$f")
  PASSED=$(sed -n 's/.*"passed": \([0-9]*\).*/\1/p' "$f")
  FAILED=$(sed -n 's/.*"failed": \([0-9]*\).*/\1/p' "$f")
  SKIPPED=$(sed -n 's/.*"skipped": \([0-9]*\).*/\1/p' "$f")
  DURATION=$(sed -n 's/.*"duration": \([0-9]*\).*/\1/p' "$f")
  EXIT=$(sed -n 's/.*"exit_code": \([0-9]*\).*/\1/p' "$f")

  G_NAME[$GROUP]="$NAME"
  G_EXPECTED[$GROUP]="${EXPECTED:-0}"
  G_PASSED[$GROUP]="${PASSED:-0}"
  G_FAILED[$GROUP]="${FAILED:-0}"
  G_SKIPPED[$GROUP]="${SKIPPED:-0}"
  G_DURATION[$GROUP]="${DURATION:-0}"
  G_EXIT[$GROUP]="${EXIT:-1}"

  TOTAL_EXPECTED=$((TOTAL_EXPECTED + ${EXPECTED:-0}))
  TOTAL_PASSED=$((TOTAL_PASSED + ${PASSED:-0}))
  TOTAL_FAILED=$((TOTAL_FAILED + ${FAILED:-0}))
  TOTAL_SKIPPED=$((TOTAL_SKIPPED + ${SKIPPED:-0}))
  TOTAL_GROUPS=$((TOTAL_GROUPS + 1))

  # Track max duration (parallel = wall-clock is the slowest group)
  if [ "${DURATION:-0}" -gt "$TOTAL_DURATION" ]; then
    TOTAL_DURATION=${DURATION:-0}
  fi

  if [ "${EXIT:-1}" -ne 0 ] || [ "${FAILED:-0}" -gt 0 ]; then
    FAILED_GROUPS=$((FAILED_GROUPS + 1))
  fi
done

# ─────────────────────────────────────────────────────
# Console output
# ─────────────────────────────────────────────────────
WALL_CLOCK=$(fmt_duration "$TOTAL_DURATION")
COMPLETED=$((TOTAL_PASSED + TOTAL_FAILED + TOTAL_SKIPPED))

echo ""
echo "  ╔══════════════════════════════════════════════════════════════════════════════"
echo "  ║"
echo "  ║   FULL TEST SUITE DASHBOARD — FINAL RESULTS"
echo "  ║"
echo "  ║   ${TOTAL_EXPECTED} expected tests · ${TOTAL_GROUPS} parallel groups"
echo "  ║"
echo "  ╠══════════════════════════════════════════════════════════════════════════════"
echo "  ║"

for grp in "${GROUP_ORDER[@]}"; do
  [ -n "${G_NAME[$grp]}" ] || continue
  local_name="${G_NAME[$grp]}"
  local_expected="${G_EXPECTED[$grp]}"
  local_passed="${G_PASSED[$grp]}"
  local_failed="${G_FAILED[$grp]}"
  local_dur=$(fmt_duration "${G_DURATION[$grp]}")
  local_exit="${G_EXIT[$grp]}"

  if [ "$local_exit" -eq 0 ] && [ "$local_failed" -eq 0 ]; then
    ICON="PASS"
  else
    ICON="FAIL"
  fi

  printf "  ║   %-4s  %-40s  %d/%d passed    %s\n" \
    "$ICON" "$local_name" "$local_passed" "$local_expected" "$local_dur"
done

echo "  ║"
echo "  ╠══════════════════════════════════════════════════════════════════════════════"
echo "  ║"
printf "  ║   %d/%d tests completed\n" "$COMPLETED" "$TOTAL_EXPECTED"
printf "  ║   %d passed   %d failed   %d skipped    Wall-clock: %s\n" \
  "$TOTAL_PASSED" "$TOTAL_FAILED" "$TOTAL_SKIPPED" "$WALL_CLOCK"
echo "  ║"

if [ "$FAILED_GROUPS" -eq 0 ]; then
  echo "  ║   ALL GROUPS PASSED"
else
  echo "  ║   ${FAILED_GROUPS} GROUP(S) HAD FAILURES"
fi

echo "  ║"
echo "  ╚══════════════════════════════════════════════════════════════════════════════"
echo ""

# ─────────────────────────────────────────────────────
# GitHub Step Summary
# ─────────────────────────────────────────────────────
if [ -n "$GITHUB_STEP_SUMMARY" ]; then
  {
    echo "## Full Test Suite Results"
    echo ""

    if [ "$FAILED_GROUPS" -eq 0 ]; then
      echo "### ALL ${TOTAL_GROUPS} GROUPS PASSED"
    else
      echo "### ${FAILED_GROUPS} GROUP(S) FAILED"
    fi

    echo ""
    echo "| Group | Expected | Passed | Failed | Skipped | Duration | Status |"
    echo "|-------|----------|--------|--------|---------|----------|--------|"

    for grp in "${GROUP_ORDER[@]}"; do
      [ -n "${G_NAME[$grp]}" ] || continue
      local_name="${G_NAME[$grp]}"
      local_expected="${G_EXPECTED[$grp]}"
      local_passed="${G_PASSED[$grp]}"
      local_failed="${G_FAILED[$grp]}"
      local_skipped="${G_SKIPPED[$grp]}"
      local_dur=$(fmt_duration "${G_DURATION[$grp]}")
      local_exit="${G_EXIT[$grp]}"

      if [ "$local_exit" -eq 0 ] && [ "$local_failed" -eq 0 ]; then
        STATUS="Pass"
      else
        STATUS="Fail"
      fi

      echo "| $local_name | $local_expected | $local_passed | $local_failed | $local_skipped | $local_dur | $STATUS |"
    done

    echo ""
    echo "| **Totals** | **$TOTAL_EXPECTED** | **$TOTAL_PASSED** | **$TOTAL_FAILED** | **$TOTAL_SKIPPED** | **$WALL_CLOCK** | |"
    echo ""
    echo "### Reports"
    echo "Download per-group reports from the \`reports-*\` artifacts."
  } >> "$GITHUB_STEP_SUMMARY"
fi
