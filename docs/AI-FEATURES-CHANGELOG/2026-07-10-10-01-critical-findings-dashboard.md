# Critical-Findings dashboard — the report now leads with the problems

**Date:** 2026-07-10
**Time:** 10:01 UTC
**Author:** Claude Code (automation, Fable 5), for abhiyant.singh@egalvanic.com

## What was asked
Make the consolidated report impressive: a section on the first pages listing all critical API issues
(slowness, pagination, 502, leaks); ship it as the single CI Suite 3 report with screenshots.

## What shipped
A "Critical & High-Priority Findings" dashboard as the first section (after the cover) that aggregates
every real issue across all report sources into a ranked, grouped table — **54 critical + 64 high** live.
Cover headline changed from green "PASS" to a red "54 CRITICAL · 64 HIGH FINDINGS" stamp. Leak detection
gated to error responses only (removed a false 200-leak). All in `api-suite-report.html`, generated every
Suite 3 run.

## Depth explanation (for learning + manager review)

**Why a green report felt "not impressive".** Suite 3 is a report-mode monitor — it stays green so a
transient QA blip doesn't page anyone. But that means the top-line verdict is PASS even when the API has
54 critical issues. A test *pass* and an API *finding* are different axes: the suite ran fine (pass) AND
the API has problems (findings). The fix separates them — the cover now leads with findings for client
impact, and keeps a small "test suite: PASS (monitor)" note for accuracy.

**Aggregation over duplication.** The issues already existed in the report — scattered across the catalog
Recommendations, the pagination audit's VIOLATION rows, the evidence captures, and the duplicate audit.
The dashboard doesn't re-test anything; it PARSES those existing outputs, normalizes them into
{severity, group, endpoint, issue}, dedupes by (endpoint, group), and ranks. One place to see everything,
sourced from the same evidence — no new claims, no double-counting.

**A credibility bug I caught in review.** The first render showed "GET /mutations — CRITICAL: leaks SQL on
HTTP 200 — stable, no 5xx". Contradictory: a 200 with legit data that happens to contain a keyword is not
an information-disclosure leak. Gated leak-detection to status ≥ 400 (only error responses expose
internals). A single self-contradictory row destroys client trust in the whole report — worth the fix.

**Severity calibration.** Slow endpoints ≥ 8 s → critical, 2.5–8 s → high; unpaginated collections > 500
rows → critical, else high; SQL-leak 500s and unauthenticated writes → always critical. This keeps the 54
"critical" list genuinely actionable rather than inflated.

## The single report already merges the catalog
`api-suite-report.html` embeds the full `api-catalog-report.html` data as Appendix A (1,077 ops) plus every
area, evidence cards, and embedded PNG screenshots — so CI produces ONE beautiful, print-to-PDF report with
proof. The prior CI artifact showed the pre-dashboard version only because this change wasn't pushed yet.

## Validation
Rendered from CI run 29082576370's data: 54 critical / 64 high across 5 groups; false /mutations leak
removed; 3 real SQL-leak 500s retained; cover + dashboard visually verified in Playwright.

## Files
- `.github/scripts/gen_api_suite_report.py`
