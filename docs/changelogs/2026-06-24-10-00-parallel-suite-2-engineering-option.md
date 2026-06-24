# Add "Engineering" run option to Parallel Suite 2 (+ register engineering groups in the dashboard)

- **Date:** 2026-06-24
- **Prompt:** "add option to parallel 2 to run engineer test case too"
- **Type:** CI wiring — new dispatch checkbox + dashboard-script group registration.

---

## What changed

### `parallel-suite-2.yml`
- New `engineering` dispatch checkbox (**default off** — it's heavy: 386 TCs incl. the 308-case
  exhaustive suite). Description lists what it runs.
- `ENGINEERING: ${{ inputs.engineering }}` env mapping in the `prep` build step.
- 5 matrix entries tagged `"toggle":"ENGINEERING"` (run as 5 parallel groups under `max-parallel: 11`):
  `asset-engineering` (12) · `asset-transformer` (10) · `asset-engineering-matrix` (39) ·
  `asset-mains-config` (17) · `asset-engineering-exhaustive` (308).
- `ENGINEERING` added to the `ENABLED` toggle loop.

### `.github/scripts/full-suite-dashboard.sh` (the runner both parallel suites call)
- Registered the 5 engineering groups in `ALL_GROUPS` / `ALL_GROUP_NAMES` / `ALL_GROUP_TESTS` /
  `ALL_GROUP_XMLS` (now 24 aligned entries) + `get_group_index()` (indices 19–23) + the "Valid:" list.
- **This also fixes Parallel Suite 1:** its engineering groups (added earlier) run via the same
  dashboard script, which derives the suite XML from its own registry and `exit 1`s on unknown groups —
  so without this registration, *both* suites' engineering jobs would have failed with "Unknown group".

## Verified
- Both workflow YAMLs parse; the `parallel-suite-2.yml` CATALOG is valid JSON (15 entries, no stagger
  collisions).
- `full-suite-dashboard.sh` passes `bash -n`; all 4 group arrays have **24 aligned entries**; each
  engineering group resolves `group → index → suite XML` to an **existing** file.
- PS1 and PS2 use **identical** engineering group names, matching the registry.

To run: Actions → Parallel Suite 2 → tick **"Asset Engineering …"**. The 5 engineering groups run in
parallel alongside any other ticked modules.
