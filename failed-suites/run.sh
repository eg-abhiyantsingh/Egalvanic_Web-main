#!/usr/bin/env bash
# Re-run failed tests locally.
#   failed-suites/run.sh            # most recent failures (failed-tests-latest.xml)
#   failed-suites/run.sh 2026-06-03 # a specific day
# Any extra args after the date are passed through to mvn.
set -euo pipefail
cd "$(dirname "$0")/.."
ARG="${1:-latest}"
if [ "$ARG" = "latest" ]; then
  SUITE="failed-suites/failed-tests-latest.xml"
else
  SUITE="failed-suites/failed-tests-${ARG}.xml"; shift || true
fi
[ -f "$SUITE" ] || { echo "No such suite: $SUITE"; echo "Available:"; ls failed-suites/failed-tests-*.xml 2>/dev/null; exit 1; }
echo "Running $SUITE"
exec mvn test -DsuiteXmlFile="$SUITE" "${@:2}"
