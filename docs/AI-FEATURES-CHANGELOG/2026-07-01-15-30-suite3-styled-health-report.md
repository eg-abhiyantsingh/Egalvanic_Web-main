# Parallel Suite 3 — styled Z-Platform health report (PDF format)

- **Title:** Parallel Suite 3 now renders its API health check as a polished multi-section HTML report in
  the Egalvanic "Z Platform API Health Check Report" format (cover + metrics, executive summary, performance
  overview + donut, category performance, critical/warning/info findings, latency analysis, endpoint
  inventory, action plan, appendix + health score). Live QA run: 49 endpoints, 48 pass, score 89/100.
- **Date:** 2026-07-01
- **Time:** 15:30
- **Prompt:** "I want report like this in this format for api testing only in parallel suite 3."

## What changed
- `.github/scripts/gen_api_health_report.py` — parses the health check's `reports/api-health-report.md`
  and emits `reports/api-health-report.html` styled like the reference PDF (computes latency bands,
  per-category pass/warn/fail, top-10 slowest, findings cards, action plan, and the composite health score
  70/20/10 formula — all from the real run data, nothing faked).
- `parallel-suite-3.yml` — added a "Render styled HTML report" step (ubuntu has python3) after the health
  run, echoes the summary, and uploads the HTML + markdown + detailed reports as the `api-health-report`
  artifact.

## Why a Python renderer (not Java string-building)
The health test (`ApiHealthCheckApiTest`) already writes a clean markdown source of truth. Keeping the
report *design* in a small standalone template makes it easy to iterate visually and keeps the Java test
focused on probing/classification. The workflow renders md → html at the end; the report is fully
reproducible from each run's data and environment-aware (title reflects QA/Prod from BASE_URL).

## Validation
Rendered locally from the live QA run: 10 sections, 49 endpoints, 48/1/0, 2 critical / 21 warning / 7 info,
score 89/100. Published as a shareable Artifact. Workflow YAML validated.
