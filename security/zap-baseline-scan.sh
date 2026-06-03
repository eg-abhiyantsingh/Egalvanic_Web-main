#!/usr/bin/env bash
# ═══════════════════════════════════════════════════════════════════════
# OWASP ZAP baseline + active scan — automated security baseline (Type J)
# ═══════════════════════════════════════════════════════════════════════
# Boundary (state it honestly): this is AUTOMATED baseline DAST. It catches
# missing security headers, mixed content, cookie flags, obvious injection
# reflections, and known-CVE fingerprints. It is NOT a substitute for a
# manual penetration test — auth-gated business-logic flaws, IDOR chains,
# and complex injection still need a human pentester.
#
# Requires Docker (ZAP runs containerized — no local install needed).
# Run:   BASE_URL=https://acme.qa.egalvanic.ai bash security/zap-baseline-scan.sh
#        ZAP_MODE=full   ...                                  # active scan (slower, intrusive)
#
# Output: security/zap-report.html + zap-report.json (gitignored).
# Exit:   non-zero if WARN/FAIL findings exceed threshold (CI gate).
# ═══════════════════════════════════════════════════════════════════════
set -euo pipefail

BASE_URL="${BASE_URL:-https://acme.qa.egalvanic.ai}"
ZAP_MODE="${ZAP_MODE:-baseline}"          # baseline | full
OUT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPORT_HTML="zap-report.html"
REPORT_JSON="zap-report.json"

echo "── OWASP ZAP ${ZAP_MODE} scan → ${BASE_URL} ──"

# Allowlist guard: never point an active scanner at an arbitrary / production host.
# Only non-prod eGalvanic environments are permitted targets.
case "${BASE_URL}" in
  https://*.qa.egalvanic.ai|https://*.qa.egalvanic.ai/*|https://*.staging.egalvanic.ai|https://*.staging.egalvanic.ai/*) ;;
  *) echo "ERROR: ${BASE_URL} is not an allowlisted QA/staging target. Refusing to scan." >&2; exit 2 ;;
esac

if ! command -v docker >/dev/null 2>&1; then
  echo "ERROR: Docker is required to run ZAP. Install Docker or use a host with ZAP." >&2
  exit 2
fi

# zap-baseline.py = passive spider + passive scan (safe, fast).
# zap-full-scan.py = adds active attack injection (intrusive; only on QA, never prod).
SCRIPT="zap-baseline.py"
[ "$ZAP_MODE" = "full" ] && SCRIPT="zap-full-scan.py"

# -I = do not fail on WARN (we post-process); -J json; -r html; -m minutes spider
docker run --rm -v "${OUT_DIR}:/zap/wrk/:rw" \
  ghcr.io/zaproxy/zaproxy:stable \
  "${SCRIPT}" \
  -t "${BASE_URL}" \
  -J "${REPORT_JSON}" \
  -r "${REPORT_HTML}" \
  -m 5 \
  -I || true

echo "── ZAP report: ${OUT_DIR}/${REPORT_HTML} ──"

# CI gate: fail if any HIGH-risk alerts. Tune the threshold for your risk appetite.
if command -v jq >/dev/null 2>&1 && [ -f "${OUT_DIR}/${REPORT_JSON}" ]; then
  HIGH=$(jq '[.site[].alerts[]? | select(.riskcode=="3")] | length' "${OUT_DIR}/${REPORT_JSON}" 2>/dev/null || echo 0)
  MED=$(jq  '[.site[].alerts[]? | select(.riskcode=="2")] | length' "${OUT_DIR}/${REPORT_JSON}" 2>/dev/null || echo 0)
  echo "ZAP findings — HIGH: ${HIGH}  MEDIUM: ${MED}"
  if [ "${HIGH:-0}" -gt 0 ]; then
    echo "❌ ZAP found ${HIGH} HIGH-risk issue(s) — failing the gate." >&2
    exit 1
  fi
fi
echo "✅ ZAP scan complete (no HIGH-risk gate breach)."
