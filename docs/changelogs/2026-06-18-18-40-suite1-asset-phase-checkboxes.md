# Parallel Suite 1 — split Assets into per-phase checkboxes

- **Date:** 2026-06-18
- **Prompt:** "can you provide option to asset phases also like phase 1 or 2 etc"
- **Component:** `.github/workflows/parallel-suite.yml`
- **Type:** CI ergonomics (finer multi-select granularity).

---

## Ask

In Suite 1's Run-workflow form, the single **Assets** checkbox bundled all four asset parts. Make
each asset phase individually selectable.

## Change

Replaced the one `assets` checkbox with four: **`asset_1_2`, `asset_3`, `asset_4`, `asset_5`** (each
default ON). The `prep` job's catalog now tags each asset group with its own toggle, and the env
mapping + enabled-toggle loop were updated accordingly. Pick any subset (e.g. just Part 3 + Part 5)
and only those asset jobs spin up via the dynamic matrix.

This brings Suite 1 to **exactly 10 inputs** (`auth_site`, `location_task`, `work_orders`,
`asset_1_2`, `asset_3`, `asset_4`, `asset_5`, `dashboard_bughunt`, `sales`, `send_email`) — the
GitHub Actions `workflow_dispatch` maximum, so this is the most asset granularity that fits without
dropping/merging another module. (`work_orders` and `sales` stay merged for the same reason.)

## Validation

- YAML parses; 10 inputs, all boolean.
- `prep` jq simulated under bash (the runner shell): selecting only `asset_3` + `asset_5` →
  groups `["asset-3","asset-5"]`; the indirect-expansion toggle loop resolves correctly.
- Also removed a stray duplicate "Phase 1" comment left above the `prep` job.
