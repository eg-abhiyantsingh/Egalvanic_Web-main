# Ghost EG-form instances on work-order delete (#1094) — QA verdict: PASS

**Tested:** 2026-09-02 · **Env:** acme.qa.egalvanic.ai (fix markers on all 4 branches per ticket)
**Ticket:** eg-pz-backend #1094 (HOTFIX) · companion eg-pz-reporting-lambdas #335 (out of scope here)
**Method:** wizard-created (job-backed) work orders + real UI deletes; instances created through the
product's own service machinery plus the task-chain and direct APIs. This supersedes the 2026-09-01
INCONCLUSIVE attempt, whose bare API sessions were invisible to every JSON view.

## Fixtures

- **CORE** `31d01ad8` — wizard WO on Android Site 2, service "De-Energized Visual Inspection"
  (service machinery materialized **261 instances**), plus 1 task-chain instance `90807c50`
  (born session_id NULL via task `c5b660a9`) and 1 direct instance `63e9c6b2`
  (create-for-asset, 201). 263 instances total — same scale as the prod case (236).
- **SHARED-A** `7ff97161` / **SHARED-B** `0d6042e1` — wizard WOs; ONE task `932125cf` mapped into
  BOTH, carrying instance `7599cf30`; before deletes it resolved in A and B and on the node.

## Results (delete via the real UI dialog, list row → Delete Work Order)

| Checklist item | Result |
|---|---|
| Core: task-chain instance soft-deleted | ✅ `is_deleted: true` |
| Core: its `mapping_eg_form_instance_node` soft-deleted | ✅ gone from `by-node/{transformer}` |
| Mixed: direct (session_id set) shape removed too | ✅ `is_deleted: true`, gone from node |
| Service-born instances swept | ✅ CORE `by-session`: **263 → 0** |
| **Shared-task negative (most important)** | ✅ instance **alive** (`is_deleted:false`), still on WO-B's `by-session`, still node-mapped |
| Performance (bulk issue lookup) | ✅ UI delete of the 263-instance WO completed in **1.35 s** (SHARED-A: 0.84 s) |
| Ghost re-attachment mechanism | ✅ severed — reports resolve pages by asset through the node mappings, and none of the deleted WO's mappings remain |
| Pre-fix orphans untouched | ✅ per ticket, out of scope; none were altered |

Bonus check from cleanup: deleting SHARED-B ("Also delete tasks" ticked) after A was gone correctly
swept the previously-shared instance — once a task stops being multi-session, the sweep takes it.

## Honest boundaries

- The literal end-to-end "generate a report PDF and count pages" was not run; the verified severing
  point (node mappings) is the exact mechanism the ticket names for re-attachment, and the RENDER
  half belongs to reporting-lambdas #335 (explicitly out of this ticket's scope).
- Chain/task wiring used the same APIs the product uses (task-session mapping + task-linked
  instance create), on real wizard WOs; 261 of the 263 swept instances were born purely by the
  product's own service machinery.
- Yesterday's side-finding resolved: create-for-asset 500s occurred ONLY against bare API sessions;
  on a real WO it returns 201. Not a user-facing defect.

**Test-data footprint: zero.** All three WOs deleted (that was the test), both tasks deleted,
all three of my instances soft-deleted, Transformer node carries none of them.
