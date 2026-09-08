# 2026-09-08 — QA: Method-first, multi-service work orders (derived WO type + session_method_lines ledger) — backend #1161 / frontend #1325 / iOS #517

**Prompt:** the ticket text with its 8-step QA review ("test this ticket too").

## What was done
1. **Migration check from the API** — `services.wo_view` is populated per service type and `session_method_lines` is served with per-line ids on QA, so smline_a1/a2/a3 have run here despite the dev-only note.
2. **Derived union UI, proven dynamically** — on the 3-service AF+IR work order the tabs, metric columns and completion rings were read off the registry; then a 4th service was added and a Forms tab, a `forms_status` column and a registration-scoped ring appeared live; removing it took the tab and the ring away.
3. **Add Service / Manage Services** via the asset right-click menu: rules evaluated, methods pre-checked; 2 methods minted 4 forms.
4. **No double-mint** — the identical add repeated returned `form_instances_created: 0` with the same line ids.
5. **Removal scope** — one of the four forms was submitted through the UI, then Remove Service returned `instances_removed: 3, lines_removed: 2`; the submitted Torque Record survived and is reachable via More → Forms.
6. **Uppercase-UUID casing bug** — add and remove both sent with uppercase ids from the authenticated session; both applied (2 lines / 2 instances each way). The reported silent no-op is fixed.
7. **Negatives** — two pre-ledger work orders open in the legacy view with empty registry; unknown/frozen method ids → 400 with a named error on both reachable paths (no 500), though a mixed batch is refused whole rather than skipped.
8. Verdict, evidence, artifact; adversarial refuter workflow (first run lost to the usage limit, re-run after the reset).

## Results (short)
- **All web-testable steps PASS**; iOS #517 not web-testable.
- **No defects.** 3 Low findings: a service's metric column outlives its last registration (survives reload); a batch with one unknown method id is refused whole, not skipped; the surviving submitted form leaves the derived tab set and is reachable only via More → Forms.
- Nuance stated plainly: "one metric column per service" is really one per distinct `data_mask` — the IR and IR-Checklist services share a column.

## Deliverables
- Verdict: `docs/bug-reports/2026-09-08-QA-method-first-multi-service-work-orders-verdict.md`
- Evidence: `docs/bug-evidence/zp-method-first-multi-service-wo/`
- Artifact: https://claude.ai/code/artifact/18b40ab9-f75e-4820-9995-6a8b98b58830
- Memory: `project_method_first_multi_service_wo.md`

## Depth notes (learning)
- **Change the input, watch the output.** Reading a registry proves a contract exists; adding a service and watching a tab appear proves the UI is derived from it. The second is the one a developer cannot argue with.
- **Pair every proof.** Add-then-add for minting, submit-then-remove for scope, uppercase-add-then-uppercase-remove for casing. A pair on the same object isolates the variable.
- **Quote the product's own promise.** The Manage Services dialog states the removal rule in its copy; testing against that sentence is stronger than testing against the ticket's paraphrase.
- **A browser fetch is a fair stand-in for the server half of an iOS bug, and only that half.** The casing fix is proven at the boundary; the iOS transport is not.
