#!/usr/bin/env bash
# Summarize RBAC test results into the GitHub step summary and export FAIL_COUNT / NO_RESULTS.
# Sums across ALL testng-results.xml files (a head -1 single-file parse can miss failures).
# Usage: rbac-summary.sh "<title>"
set -u
TITLE="${1:-RBAC}"
echo "## ${TITLE}" >> "$GITHUB_STEP_SUMMARY"

FILES=$(find target/surefire-reports -name 'testng-results.xml' 2>/dev/null)
if [ -z "$FILES" ]; then
  echo "No results produced." >> "$GITHUB_STEP_SUMMARY"
  echo "NO_RESULTS=1" >> "$GITHUB_ENV"
  exit 0
fi

PASS=0; FAIL=0; SKIP=0
for R in $FILES; do
  p=$(grep -oE '<testng-results[^>]*passed="[0-9]+"' "$R" | grep -oE '[0-9]+' | head -1)
  q=$(grep -oE '<testng-results[^>]*failed="[0-9]+"' "$R" | grep -oE '[0-9]+' | head -1)
  s=$(grep -oE '<testng-results[^>]*skipped="[0-9]+"' "$R" | grep -oE '[0-9]+' | head -1)
  PASS=$((PASS + ${p:-0})); FAIL=$((FAIL + ${q:-0})); SKIP=$((SKIP + ${s:-0}))
done
echo "FAIL_COUNT=$FAIL" >> "$GITHUB_ENV"

{
  echo ""
  echo "| Result | Count |"
  echo "|--------|------:|"
  echo "| ✅ Passed | $PASS |"
  echo "| ❌ Failed | $FAIL |"
  echo "| ⏭️ Skipped | $SKIP |"
  echo ""
} >> "$GITHUB_STEP_SUMMARY"

if [ "$FAIL" != "0" ]; then
  echo "### ❌ Failing tests" >> "$GITHUB_STEP_SUMMARY"
  echo '```' >> "$GITHUB_STEP_SUMMARY"
  grep -rhoE '<test-method status="FAIL"[^>]*name="[^"]*"' $FILES \
    | grep -oE 'name="[^"]*"' | sed -E 's/name="(.*)"/- \1/' | sort -u >> "$GITHUB_STEP_SUMMARY" || true
  echo '```' >> "$GITHUB_STEP_SUMMARY"
fi
