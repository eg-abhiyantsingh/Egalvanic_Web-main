#!/bin/bash
# ═══════════════════════════════════════════════════════════════════════
# FULL TEST SUITE DASHBOARD — 1,060 TCs across 10 Groups
# ═══════════════════════════════════════════════════════════════════════
# Runs 10 module groups individually with LIVE per-test progress updates.
#   Group 1:  Auth + Site + Connection    (130 TCs)
#   Group 2:  Location + Task             (135 TCs)
#   Group 3:  Work Order + Issue          (234 TCs)
#   Group 4:  Asset Parts 1-2             ( 69 TCs)
#   Group 5:  Asset Part 3               ( 76 TCs)
#   Group 6:  Asset Parts 4-5            (141 TCs)
#   Group 7:  SLD                        ( 71 TCs)
#   Group 8:  Dashboard + BugHunt        (105 TCs)
#   Group 9:  Load + API + Critical Path  ( 62 TCs)
#   Group 10: Smoke Suites               ( 37 TCs)
#
# Architecture (same as smoke-dashboard.sh):
#   1. Maven runs in background → output to temp log file
#   2. Foreground monitors log for test completions (1s polling)
#   3. Per-test: prints test name + global progress counter
#   4. Per-group: prints group summary
#   5. After all: prints final dashboard + banner
#   6. Raw Maven output → collapsed ::group:: blocks
#
# Required: Chrome browser installed, headless mode via -Dheadless=true
# ═══════════════════════════════════════════════════════════════════════

set +e  # Don't exit on error — we handle failures ourselves

# ─────────────────────────────────────────────────────
# GROUP DEFINITIONS
# ─────────────────────────────────────────────────────
ALL_GROUPS=(
  "auth-site-connection"
  "location-task"
  "workorder-issue"
  "asset-1-2"
  "asset-3"
  "asset-4-5"
  "sld"
  "dashboard-bughunt"
  "load-api"
  "smoke"
)
ALL_GROUP_NAMES=(
  "Auth + Site + Connection"
  "Location + Task"
  "Work Order + Issue"
  "Asset Parts 1-2"
  "Asset Part 3"
  "Asset Parts 4-5"
  "SLD Module"
  "Dashboard + BugHunt"
  "Load + API + Critical Path"
  "Smoke Suites"
)
ALL_GROUP_TESTS=(130 135 234 69 76 141 71 105 62 37)
ALL_GROUP_XMLS=(
  "suite-auth-site-connection.xml"
  "suite-location-task.xml"
  "suite-workorder-issue.xml"
  "suite-asset-1-2.xml"
  "suite-asset-3.xml"
  "suite-asset-4-5.xml"
  "suite-sld.xml"
  "suite-dashboard-bughunt.xml"
  "suite-load-api.xml"
  "smoke-testng.xml"
)

# ─────────────────────────────────────────────────────
# GROUP SELECTION (via FULL_SUITE_GROUP env var)
# ─────────────────────────────────────────────────────
get_group_index() {
  case "$1" in
    auth-site-connection) echo 0 ;;
    location-task)        echo 1 ;;
    workorder-issue)      echo 2 ;;
    asset-1-2)            echo 3 ;;
    asset-3)              echo 4 ;;
    asset-4-5)            echo 5 ;;
    sld)                  echo 6 ;;
    dashboard-bughunt)    echo 7 ;;
    load-api)             echo 8 ;;
    smoke)                echo 9 ;;
    *)                    echo -1 ;;
  esac
}

SELECTED="${FULL_SUITE_GROUP:-all}"

if [ "$SELECTED" = "all" ] || [ -z "$SELECTED" ]; then
  # NOTE: Cannot use 'GROUPS' — it is a bash built-in (user group IDs).
  RUN_GROUPS=("${ALL_GROUPS[@]}")
  GROUP_NAMES=("${ALL_GROUP_NAMES[@]}")
  GROUP_TESTS=("${ALL_GROUP_TESTS[@]}")
  GROUP_XMLS=("${ALL_GROUP_XMLS[@]}")
else
  IDX=$(get_group_index "$SELECTED")
  if [ "$IDX" -eq -1 ]; then
    echo "Unknown group: $SELECTED"
    echo "   Valid: all, auth-site-connection, location-task, workorder-issue, asset-1-2, asset-3, asset-4-5, sld, dashboard-bughunt, load-api, smoke"
    exit 1
  fi
  RUN_GROUPS=("${ALL_GROUPS[$IDX]}")
  GROUP_NAMES=("${ALL_GROUP_NAMES[$IDX]}")
  GROUP_TESTS=("${ALL_GROUP_TESTS[$IDX]}")
  GROUP_XMLS=("${ALL_GROUP_XMLS[$IDX]}")
  echo "  Running single group: ${GROUP_NAMES[0]}"
  echo ""
fi

TOTAL_TESTS=0
for tc in "${GROUP_TESTS[@]}"; do
  TOTAL_TESTS=$((TOTAL_TESTS + tc))
done
TOTAL_GROUPS=${#RUN_GROUPS[@]}

# ─────────────────────────────────────────────────────
# STATE TRACKING
# ─────────────────────────────────────────────────────
STATUS=()
M_PASSED=()
M_FAILED=()
M_SKIPPED=()
M_DURATION=()
for i in $(seq 0 $((TOTAL_GROUPS - 1))); do
  STATUS+=("pending")
  M_PASSED+=(0)
  M_FAILED+=(0)
  M_SKIPPED+=(0)
  M_DURATION+=(0)
done

SUITE_START=$(date +%s)
GLOBAL_COMPLETED=0
TOTAL_PASSED=0
TOTAL_FAILED=0
TOTAL_SKIPPED=0
HAS_FAILURE=0

# ─────────────────────────────────────────────────────
# FORMAT DURATION
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
# PRINT PER-TEST PROGRESS LINE
# ─────────────────────────────────────────────────────
print_test_progress() {
  local status_icon="$1"
  local test_name="$2"
  local duration="$3"
  local error_reason="$4"
  local elapsed=$(( $(date +%s) - SUITE_START ))
  local elapsed_fmt
  elapsed_fmt=$(fmt_duration $elapsed)

  if [ -n "$error_reason" ]; then
    printf "    %s  %s\n" "$status_icon" "$test_name"
    printf "         Reason: %s\n" "$error_reason"
  else
    printf "    %s  %-55s %ss\n" "$status_icon" "$test_name" "$duration"
  fi
  printf "    %d/%d completed   ⏱️ %s\n\n" "$GLOBAL_COMPLETED" "$TOTAL_TESTS" "$elapsed_fmt"
}

# ─────────────────────────────────────────────────────
# PRINT GROUP HEADER
# ─────────────────────────────────────────────────────
print_group_header() {
  local idx=$1
  local name="${GROUP_NAMES[$idx]}"
  local tc="${GROUP_TESTS[$idx]}"
  local num=$((idx + 1))

  echo ""
  echo "  ── Group ${num}/${TOTAL_GROUPS}: ${name} (${tc} tests) ──────────────────────────────────────"
  echo ""
}

# ─────────────────────────────────────────────────────
# PRINT GROUP COMPLETION LINE
# ─────────────────────────────────────────────────────
print_group_complete() {
  local idx=$1
  local dur=$2
  local num=$((idx + 1))
  local name="${GROUP_NAMES[$idx]}"
  local p=${M_PASSED[$idx]}
  local f=${M_FAILED[$idx]}
  local s=${M_SKIPPED[$idx]}
  local tc=${GROUP_TESTS[$idx]}
  local dur_fmt
  dur_fmt=$(fmt_duration $dur)

  if [ "$f" -gt 0 ]; then
    echo "  ── ❌ Group ${num}: ${name}  ${p} passed, ${f} failed  (${dur_fmt}) ─────────────────"
  elif [ "$s" -gt 0 ]; then
    echo "  ── ⚠️  Group ${num}: ${name}  ${p}/${tc} passed, ${s} skipped  (${dur_fmt}) ─────────────"
  else
    echo "  ── ✅ Group ${num}: ${name}  ${p}/${tc} passed  (${dur_fmt}) ──────────────────────"
  fi
  echo ""
}

# ─────────────────────────────────────────────────────
# DRAW FINAL DASHBOARD
# ─────────────────────────────────────────────────────
draw_final_dashboard() {
  local elapsed=$(( $(date +%s) - SUITE_START ))
  local elapsed_fmt
  elapsed_fmt=$(fmt_duration $elapsed)
  local completed=$((TOTAL_PASSED + TOTAL_FAILED + TOTAL_SKIPPED))
  local LINE="══════════════════════════════════════════════════════════════════════════════"

  echo ""
  echo ""
  echo "  ╔${LINE}"
  echo "  ║"
  echo "  ║   🔥  F U L L   T E S T   S U I T E   D A S H B O A R D   —   F I N A L"
  echo "  ║"
  echo "  ║   🌐  Chrome (headless) · eGalvanic Web          ${TOTAL_TESTS} tests · ${TOTAL_GROUPS} groups"
  echo "  ║"
  echo "  ╠${LINE}"
  echo "  ║"

  for i in $(seq 0 $((TOTAL_GROUPS - 1))); do
    local num=$((i + 1))
    local name="${GROUP_NAMES[$i]}"
    local st="${STATUS[$i]}"
    local tc="${GROUP_TESTS[$i]}"
    local dur_fmt
    dur_fmt=$(fmt_duration "${M_DURATION[$i]}")

    case "$st" in
      passed)
        printf "  ║   ✅  Group %2d │ %-25s    %d/%d passed              %s\n" \
          "$num" "$name" "${M_PASSED[$i]}" "$tc" "$dur_fmt"
        ;;
      failed)
        printf "  ║   ❌  Group %2d │ %-25s    %d passed, %d failed     %s\n" \
          "$num" "$name" "${M_PASSED[$i]}" "${M_FAILED[$i]}" "$dur_fmt"
        ;;
      *)
        printf "  ║   ⚠️   Group %2d │ %-25s    Did not complete\n" \
          "$num" "$name"
        ;;
    esac
  done

  echo "  ║"
  echo "  ╠${LINE}"
  echo "  ║"
  printf "  ║   %d/%d tests completed\n" "$completed" "$TOTAL_TESTS"
  echo "  ║"
  printf "  ║   ✅ %d passed   ❌ %d failed   ⏭️  %d skipped    ⏱️  %s elapsed\n" \
    "$TOTAL_PASSED" "$TOTAL_FAILED" "$TOTAL_SKIPPED" "$elapsed_fmt"
  echo "  ║"
  echo "  ╚${LINE}"
  echo ""
}

# ─────────────────────────────────────────────────────
# DRAW FINAL BANNER
# ─────────────────────────────────────────────────────
draw_final_banner() {
  local elapsed=$(( $(date +%s) - SUITE_START ))
  local elapsed_fmt
  elapsed_fmt=$(fmt_duration $elapsed)
  local LINE="══════════════════════════════════════════════════════════════════════════════"

  echo ""
  if [ $HAS_FAILURE -eq 0 ]; then
    echo "  ╔${LINE}"
    echo "  ║"
    echo "  ║   🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉"
    echo "  ║"
    echo "  ║     ✅  A L L   F U L L   S U I T E   T E S T S   P A S S E D  !"
    echo "  ║"
    echo "  ║     ${TOTAL_PASSED}/${TOTAL_TESTS} tests passed in ${elapsed_fmt}"
    echo "  ║     All ${TOTAL_GROUPS} groups verified successfully"
    echo "  ║"
    echo "  ║   🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉🎉"
    echo "  ║"
    echo "  ╚${LINE}"
  else
    echo "  ╔${LINE}"
    echo "  ║"
    echo "  ║     ❌  S O M E   T E S T S   F A I L E D"
    echo "  ║"
    printf "  ║     %d/%d passed, %d failed, %d skipped in %s\n" \
      "$TOTAL_PASSED" "$TOTAL_TESTS" "$TOTAL_FAILED" "$TOTAL_SKIPPED" "$elapsed_fmt"
    echo "  ║"
    echo "  ║     Failed groups:"
    for i in $(seq 0 $((TOTAL_GROUPS - 1))); do
      if [ "${STATUS[$i]}" = "failed" ]; then
        echo "  ║       ❌ Group $((i + 1)): ${GROUP_NAMES[$i]} (${M_FAILED[$i]} failed)"
      fi
    done
    echo "  ║"
    echo "  ╚${LINE}"
  fi
  echo ""
}

# ─────────────────────────────────────────────────────
# PARSE RESULTS FROM SUREFIRE XML (fallback)
# ─────────────────────────────────────────────────────
parse_results_xml() {
  local xml="target/surefire-reports/testng-results.xml"
  if [ -f "$xml" ]; then
    local p f s
    p=$(sed -n 's/.*passed="\([^"]*\)".*/\1/p' "$xml" | head -1)
    f=$(sed -n 's/.*failed="\([^"]*\)".*/\1/p' "$xml" | head -1)
    s=$(sed -n 's/.*skipped="\([^"]*\)".*/\1/p' "$xml" | head -1)
    echo "${p:-0} ${f:-0} ${s:-0}"
  else
    echo "0 0 0"
  fi
}

# ═══════════════════════════════════════════════════════
# MAIN EXECUTION
# ═══════════════════════════════════════════════════════

# -- Print Header --
echo ""
echo "  ┌──────────────────────────────────────────────────────────────────────────"
echo "  │  🚀  Full Test Suite Dashboard"
echo "  │  🌐  Chrome (headless) · eGalvanic Web App"
echo "  │  📦  ${TOTAL_TESTS} tests across ${TOTAL_GROUPS} groups"
echo "  │"
for i in $(seq 0 $((TOTAL_GROUPS - 1))); do
  printf "  │  📋  Group %2d: %-25s (%d TCs)\n" \
    "$((i + 1))" "${GROUP_NAMES[$i]}" "${GROUP_TESTS[$i]}"
done
echo "  │"
echo "  │  ⏰  $(date '+%Y-%m-%d %H:%M:%S')"
echo "  └──────────────────────────────────────────────────────────────────────────"
echo ""


# -- Run each group --
for i in $(seq 0 $((TOTAL_GROUPS - 1))); do
  GROUP_IDX=$i
  GROUP_KEY="${RUN_GROUPS[$i]}"
  GROUP_NAME="${GROUP_NAMES[$i]}"
  GROUP_XML="${GROUP_XMLS[$i]}"
  TEST_COUNT="${GROUP_TESTS[$i]}"

  # Mark as running
  STATUS[$i]="running"

  # Print group header
  print_group_header $i

  # Clean previous reports to get fresh results
  rm -rf target/surefire-reports 2>/dev/null || true

  GROUP_START=$(date +%s)
  LOG_FILE="/tmp/fullsuite_group_${i}.log"
  > "$LOG_FILE"

  # -- Run Maven in background with quiet mode --
  mvn test -B -q \
    -DsuiteXmlFile="${GROUP_XML}" \
    -Dheadless=true \
    > "$LOG_FILE" 2>&1 &
  MVN_PID=$!

  # -- Monitor for per-test completions --
  LAST_COUNT=0
  MOD_PASSED=0
  MOD_FAILED=0
  MOD_SKIPPED=0

  while kill -0 $MVN_PID 2>/dev/null; do
    # Match ConsoleProgressListener output format:
    # "PASSED: ClassName.methodName (Xs)"
    CURRENT=$(grep -cE '(PASSED|FAILED|SKIPPED): [A-Za-z_0-9]+\.[A-Za-z_0-9]+ \([0-9]+s\)' "$LOG_FILE" 2>/dev/null)
    CURRENT=${CURRENT:-0}

    if [ "$CURRENT" -gt "$LAST_COUNT" ]; then
      while [ "$LAST_COUNT" -lt "$CURRENT" ]; do
        LAST_COUNT=$((LAST_COUNT + 1))

        LINE=$(grep -E '(PASSED|FAILED|SKIPPED): [A-Za-z_0-9]+\.[A-Za-z_0-9]+ \([0-9]+s\)' "$LOG_FILE" | sed -n "${LAST_COUNT}p")

        if [ -z "$LINE" ]; then
          continue
        fi

        if echo "$LINE" | grep -q "PASSED:"; then
          ICON="✅"; MOD_PASSED=$((MOD_PASSED + 1)); TOTAL_PASSED=$((TOTAL_PASSED + 1))
        elif echo "$LINE" | grep -q "FAILED:"; then
          ICON="❌"; MOD_FAILED=$((MOD_FAILED + 1)); TOTAL_FAILED=$((TOTAL_FAILED + 1))
        else
          ICON="⏭️"; MOD_SKIPPED=$((MOD_SKIPPED + 1)); TOTAL_SKIPPED=$((TOTAL_SKIPPED + 1))
        fi

        GLOBAL_COMPLETED=$((GLOBAL_COMPLETED + 1))

        TEST_NAME=$(echo "$LINE" | sed 's/.*: [A-Za-z0-9_]*\.//' | sed 's/ (.*//' | sed 's/^test//')
        DURATION=$(echo "$LINE" | sed 's/.*(\([0-9]*\)s).*/\1/')
        [ -z "$DURATION" ] || [ "$DURATION" = "$LINE" ] && DURATION="?"

        ERROR_REASON=""
        if [ "$ICON" = "❌" ]; then
          NEARBY=$(grep -F -A5 "$LINE" "$LOG_FILE" 2>/dev/null | tail -n +2)
          ERROR_REASON=$(echo "$NEARBY" | grep -i "Error:" | sed 's/.*Error: //' | head -1)
          [ -z "$ERROR_REASON" ] && ERROR_REASON=$(echo "$NEARBY" | grep -i "assert\|exception\|timeout\|not found\|not visible\|NoSuchElement\|could not be located" | sed 's/^[[:space:]]*//' | head -1)
          [ -z "$ERROR_REASON" ] && ERROR_REASON="Test failed (check raw output for details)"
          ERROR_REASON=$(echo "$ERROR_REASON" | cut -c1-120)
        fi

        print_test_progress "$ICON" "$TEST_NAME" "$DURATION" "$ERROR_REASON"
      done
    fi

    sleep 1
  done

  # Wait for Maven to finish
  wait $MVN_PID
  MVN_EXIT=$?

  # -- Final check: catch any tests we missed --
  FINAL_COUNT=$(grep -cE '(PASSED|FAILED|SKIPPED): [A-Za-z_0-9]+\.[A-Za-z_0-9]+ \([0-9]+s\)' "$LOG_FILE" 2>/dev/null)
  FINAL_COUNT=${FINAL_COUNT:-0}
  while [ "$LAST_COUNT" -lt "$FINAL_COUNT" ]; do
    LAST_COUNT=$((LAST_COUNT + 1))
    LINE=$(grep -E '(PASSED|FAILED|SKIPPED): [A-Za-z_0-9]+\.[A-Za-z_0-9]+ \([0-9]+s\)' "$LOG_FILE" | sed -n "${LAST_COUNT}p")

    if echo "$LINE" | grep -q "PASSED:"; then
      ICON="✅"; MOD_PASSED=$((MOD_PASSED + 1)); TOTAL_PASSED=$((TOTAL_PASSED + 1))
    elif echo "$LINE" | grep -q "FAILED:"; then
      ICON="❌"; MOD_FAILED=$((MOD_FAILED + 1)); TOTAL_FAILED=$((TOTAL_FAILED + 1))
    else
      ICON="⏭️"; MOD_SKIPPED=$((MOD_SKIPPED + 1)); TOTAL_SKIPPED=$((TOTAL_SKIPPED + 1))
    fi

    GLOBAL_COMPLETED=$((GLOBAL_COMPLETED + 1))
    TEST_NAME=$(echo "$LINE" | sed 's/.*: [A-Za-z0-9_]*\.//' | sed 's/ (.*//' | sed 's/^test//')
    DURATION=$(echo "$LINE" | sed 's/.*(\([0-9]*\)s).*/\1/')
    [ -z "$DURATION" ] || [ "$DURATION" = "$LINE" ] && DURATION="?"

    ERROR_REASON=""
    if [ "$ICON" = "❌" ]; then
      NEARBY=$(grep -F -A5 "$LINE" "$LOG_FILE" 2>/dev/null | tail -n +2)
      ERROR_REASON=$(echo "$NEARBY" | grep -i "Error:" | sed 's/.*Error: //' | head -1)
      [ -z "$ERROR_REASON" ] && ERROR_REASON=$(echo "$NEARBY" | grep -i "assert\|exception\|timeout\|not found\|not visible\|NoSuchElement\|could not be located" | sed 's/^[[:space:]]*//' | head -1)
      [ -z "$ERROR_REASON" ] && ERROR_REASON="Test failed (check raw output for details)"
      ERROR_REASON=$(echo "$ERROR_REASON" | cut -c1-120)
    fi

    print_test_progress "$ICON" "$TEST_NAME" "$DURATION" "$ERROR_REASON"
  done

  GROUP_END=$(date +%s)
  M_DURATION[$i]=$((GROUP_END - GROUP_START))

  # -- If real-time monitoring caught nothing, fall back to XML parsing --
  DETECTED=$((MOD_PASSED + MOD_FAILED + MOD_SKIPPED))
  if [ "$DETECTED" -eq 0 ]; then
    RESULTS=$(parse_results_xml)
    read -r P F S <<< "$RESULTS"
    MOD_PASSED=$P; MOD_FAILED=$F; MOD_SKIPPED=$S
    TOTAL_PASSED=$((TOTAL_PASSED + P))
    TOTAL_FAILED=$((TOTAL_FAILED + F))
    TOTAL_SKIPPED=$((TOTAL_SKIPPED + S))
    GLOBAL_COMPLETED=$((GLOBAL_COMPLETED + P + F + S))

    if [ "$((P + F + S))" -gt 0 ]; then
      local_elapsed=$(( $(date +%s) - SUITE_START ))
      local_elapsed_fmt=$(fmt_duration $local_elapsed)
      echo "    (parsed from results XML — real-time output was not available)"
      printf "    %d/%d completed   ⏱️ %s\n\n" "$GLOBAL_COMPLETED" "$TOTAL_TESTS" "$local_elapsed_fmt"
    fi
  fi

  M_PASSED[$i]=$MOD_PASSED
  M_FAILED[$i]=$MOD_FAILED
  M_SKIPPED[$i]=$MOD_SKIPPED

  # Determine group status
  if [ "$MOD_FAILED" -gt 0 ] || [ $MVN_EXIT -ne 0 ]; then
    STATUS[$i]="failed"
    HAS_FAILURE=1

    if [ "$DETECTED" -eq 0 ] && [ "$MOD_PASSED" -eq 0 ] && [ "$MOD_FAILED" -eq 0 ]; then
      M_FAILED[$i]=$TEST_COUNT
      TOTAL_FAILED=$((TOTAL_FAILED + TEST_COUNT))
      GLOBAL_COMPLETED=$((GLOBAL_COMPLETED + TEST_COUNT))
    fi
  else
    STATUS[$i]="passed"
  fi

  # Print group completion summary
  print_group_complete $i ${M_DURATION[$i]}

  # -- Collapse raw Maven output --
  echo "::group::${GROUP_NAME} -- raw output (click to expand)"
  cat "$LOG_FILE" 2>/dev/null || echo "(no output)"
  echo "::endgroup::"

  # -- Save group reports --
  mkdir -p "reports/groups/group-$((GROUP_IDX + 1))-${GROUP_KEY}"
  cp -r target/surefire-reports/* "reports/groups/group-$((GROUP_IDX + 1))-${GROUP_KEY}/" 2>/dev/null || true

done

# -- Final Results --
draw_final_dashboard
draw_final_banner

# -- Write summary for downstream steps --
if [ -n "$GITHUB_ENV" ]; then
  echo "SUITE_PASSED=${TOTAL_PASSED}" >> "$GITHUB_ENV"
  echo "SUITE_FAILED=${TOTAL_FAILED}" >> "$GITHUB_ENV"
  echo "SUITE_SKIPPED=${TOTAL_SKIPPED}" >> "$GITHUB_ENV"
  echo "SUITE_TOTAL=${TOTAL_TESTS}" >> "$GITHUB_ENV"
  echo "SUITE_DURATION=$(( $(date +%s) - SUITE_START ))" >> "$GITHUB_ENV"
fi

if [ "$TOTAL_TESTS" -eq 0 ]; then
  echo "FATAL: No tests were configured to run (TOTAL_TESTS=0)"
  [ -n "$GITHUB_ENV" ] && echo "SUITE_RESULT=failed" >> "$GITHUB_ENV"
  exit 1
elif [ "$TOTAL_PASSED" -eq 0 ] && [ "$TOTAL_TESTS" -gt 0 ]; then
  [ -n "$GITHUB_ENV" ] && echo "SUITE_RESULT=failed" >> "$GITHUB_ENV"
  exit 1
elif [ $HAS_FAILURE -eq 1 ]; then
  [ -n "$GITHUB_ENV" ] && echo "SUITE_RESULT=partial" >> "$GITHUB_ENV"
  exit 0
else
  [ -n "$GITHUB_ENV" ] && echo "SUITE_RESULT=passed" >> "$GITHUB_ENV"
  exit 0
fi
