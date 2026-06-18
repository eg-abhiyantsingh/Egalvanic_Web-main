# Parallel Suite 1 & 2 — module multi-select (checkboxes) via dynamic matrix

- **Date:** 2026-06-18
- **Prompt:** "I want multi selector here. means i can select what i want to run — api testing for rbac or rbac test case or any other module in parallel suite 2 and same apply for parallel suite 1"
- **Component:** `.github/workflows/parallel-suite.yml`, `.github/workflows/parallel-suite-2.yml`
- **Type:** CI ergonomics (workflow_dispatch multi-select).

---

## Ask

In the "Run workflow" form, be able to pick **which modules to run** (any combination), for both
parallel suites — not just the 2 RBAC dropdowns that existed on Suite 2.

## Constraint discovered

GitHub Actions `workflow_dispatch` has **no native multi-select** input, and a workflow can have at
most **10 inputs**. The canonical "pick any combination" UI is therefore **boolean checkboxes**, one
per module. Suite 2 has 10 matrix groups + 2 RBAC jobs (13 selectable) and Suite 1 has 12 groups —
both exceed 10 if each group were its own checkbox, so closely-related groups are merged into one
module checkbox.

## What changed (both suites)

- Inputs are now **boolean checkboxes**, all defaulting **ON** (so a plain "Run workflow" runs
  everything, exactly as before; untick to skip a module).
- A new **`prep` job** reads the checkboxes and builds a **dynamic matrix** (`jq`-filtered catalog →
  `strategy.matrix.include: fromJSON(needs.prep.outputs.matrix)`), so **only the ticked modules spin
  up runners** (no wasted jobs). `test-group` is gated `if: needs.prep.outputs.any == 'true'`.

### Suite 2 — `.github/workflows/parallel-suite-2.yml` (10 inputs)
Checkboxes: `curated_bug`, `smoke` (Smoke + BCES-IQ tenant), `load_api`, `ai` (Form + Page Analyzer),
`exploratory` (Monkey + Visual Regression), `quality_gates`, `doc_inspired`, plus `rbac_api` and
`rbac_all` (now booleans, default **off** — were skip/run choices), and `send_email`. The RBAC jobs'
gates moved from `github.event.inputs.rbac_* == 'run'` to `inputs.rbac_api` / `inputs.rbac_all`.

### Suite 1 — `.github/workflows/parallel-suite.yml` (7 inputs)
Checkboxes: `auth_site`, `location_task`, `work_orders` (Work Order + Issue + Planning), `assets`
(Parts 1‑2/3/4/5), `dashboard_bughunt`, `sales` (Opportunities + Accounts + Goals), plus `send_email`.

## Why a dynamic matrix (not per‑step `if`)

Gating only the heavy step would still spin up a runner per group and show misleading "success"
rows. Building the matrix from the selection means the skipped modules never start a job at all.

## Validation

- Both files parse as YAML; input counts are 10 / 7 (≤ 10); all inputs are booleans.
- The `prep` `jq` filter was simulated locally for several combinations: all‑on → all groups; a
  single merged checkbox (`smoke`) → its 2 groups (Smoke + BCES‑IQ, with tenant overrides intact);
  arbitrary combos → exactly those groups; none → empty array → `any=false` (test‑group skipped).
- Followed by a live test dispatch of a single‑module selection to confirm only that group runs
  end‑to‑end (`prep` → dynamic matrix → one `test-group` job).
