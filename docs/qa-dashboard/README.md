# Regression Signal Board — QA automation status dashboard

Client-facing, date-partitioned status board for the **full CI regression suite**
("Parallel Full Suite — Core Regression"). Pick a run date and see overall pass rate,
tests covered, and a per-module breakdown (healthy vs. needs-attention) with a drill-down
into every failing test.

- **`index.html`** — the dashboard. Self-contained (no network, no build step, works offline / drag-anywhere).
- **`dashboard-data.json`** — the aggregated data it embeds.
- **`build_dashboard.py`** — rebuilds both from real CI artifacts (see its header for the refresh steps).
- **`_template.html`** — HTML/CSS/JS shell; `build_dashboard.py` injects the data into `__DATA__`.

**Data is real, not estimated.** Every figure is the run's own recorded result, taken from the
`consolidated-client-report-after-rerun` artifact each full-suite run produces (flaky retries that
pass on re-run don't count against a module). Numbers reconcile three ways (global badge count,
per-module sums, and module-header mini-badges).
