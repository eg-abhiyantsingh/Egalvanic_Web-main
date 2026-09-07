# 2026-09-07 — QA: ZP-4018 per-service user-checked completion check-offs (backend #1165, frontend #1336)

**Prompt:** ticket ZP-4018 pasted with its 6-step QA review ("test this ticket").

## What was done
1. **Deployment fingerprint** (ticket said dev-only): bundle has `user_checked`, both `line-checks` PUT shapes and the indeterminate cell renderer; `procedures-v2` methods carry `user_checked: true`; `service-registry`, `method-lines`, `line-checks`, `asset-checks`, `service-completion` all answer on QA.
2. **Discovered that no existing AF/IR work order has ledger lines** (all created before materialisation; registry `user_checked: false`) → created two wizard work orders on the 10-asset "Addtioanl Site" (AF + IR; AF + Label Placement).
3. **Per-service tick / persistence / untick** verified with request capture and ledger read-back (`executed_at`, `executed_by`, `status`), header % and `service-completion` masks.
4. **Bulk Mark As..**: row-selection path → 400 `invalid id` (row ids `loc-0-node-<uuid>`), Select-all path → 200; the same body with raw uuids → 200 — isolates the defect to the frontend id mapping.
5. **Session scope**: second WO on the same assets starts fully unchecked while the first has three assets ticked.
6. **Indeterminate**: staged a second same-mask line on one node with `add-assets` (no QA procedure has two user-checked methods) → `data-indeterminate="true"`; click → all lines done.
7. **Older-backend degrade**: Playwright route interception stripped `user_checked` from the registry and 404'd `method-lines` → legacy node-grain checkbox rendered for IR-Checklist rows; its `PUT asset-checks` was accepted and **fanned out** to both flagged lines on the node; rollup true.
8. Adversarial verification workflow (4 refuters + completeness critic) over the evidence file and the public bundle before publishing.
9. Verdict doc, evidence folder (17 screenshots + `api-captures.md`), artifact, memory.

## Results (short)
- #1165 PASS · #1336 PASS on all reachable web paths.
- **DEFECT 1 (High):** Bulk "Mark As.." after ticking rows sends DataGrid row ids → 400, silent.
- **FINDING 2 (Medium):** Arc Flash Label Placement (user-checked, no `data_mask`) has no per-asset checkbox; only Select all → Mark As.. can tick it.
- iOS #519/#520/#521 and pipeline #87: not testable from web.

## Deliverables
- Verdict: `docs/bug-reports/2026-09-07-QA-ZP-4018-user-checked-checkoffs-verdict.md`
- Evidence: `docs/bug-evidence/zp-4018-user-checked-checkoffs/`
- Artifact: https://claude.ai/code/artifact/7f694e75-8a90-4523-94f6-fefbb0af4628
- Memory: `project_user_checked_line_checks_zp4018.md`

## Depth notes (learning)
- **Fixtures must be born after the feature.** Ledger lines are materialised at creation; testing on July work orders would have produced a false "nothing renders". Always check `method-lines` count before judging the UI.
- **Two selection models, two id spaces.** DataGrid selection returns row ids (`loc-0-node-…`), "Select all" returns node ids. The bug is invisible unless you capture the request body — the UI shows no error.
- **Simulating an older backend at the network layer** (route interception) is the honest way to test "graceful degrade" on a single-backend QA — and it also exercised the backend's fan-out from a node-grain write to the ledger.
- **Staging a state the data cannot reach** (indeterminate) with the product's own API (`add-assets`) is legitimate when it is a real supported configuration (two IR services on one asset); say so in the verdict.
- **Rollup = AND across a node's flagged lines** — a node is "checked" only when every service on it is done; the header % is line-based, not service-based.
