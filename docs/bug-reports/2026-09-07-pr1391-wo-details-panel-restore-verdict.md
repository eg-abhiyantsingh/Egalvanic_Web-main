# QA Verdict — PR #1391: Restore the work order details panel behind the header chevron

**Ticket:** [Web] Work order detail summary card was dropped from dev/qa by the compact-header rewrite and would vanish from prod on promotion
**Fix under test:** eg-pz-frontend PR #1391 (merged to `cicd/qa` only) + Account row from eg-pz-backend #1214
**Environment:** acme.qa.egalvanic.ai · V1.36 · 2026-09-07 · admin seat
**Method:** live UI walkthrough (Playwright-driven real browser clicks) + API cross-checks
**Artifact:** https://claude.ai/code/artifact/a98c031d-5c16-43af-aa1e-38965850e188

## Verdict: PASS — 5/5 QA-review checks verified live

| # | Check | Verdict | Evidence |
|---|-------|---------|----------|
| 1 | Chevron in header between Close Work Order and ⋮ | PASS | DOM button order verified by SVG path data: `Close Work Order` → ExpandMore → MoreVert |
| 2 | Panel populates Priority/Service/Facility/Timeframe/Certifier/Back Office/Field | PASS | 3 WOs covering populated + em-dash fallback paths; team rows match `/api/ir_session/{id}/team` verbatim |
| 3 | Account row above Facility, reads `session.account_name`, em dash if absent | PASS | Above Facility; equals grid Account column + `/full` API. Absent-field case not exercisable on QA (backend live), but same `detailRow` helper renders “—” for empties |
| 4 | Service shows `workTypeService` registry value | PASS | “test 3 sep” → **Infrared Thermography**; untyped WOs → “General” fallback as coded |
| 5 | No Quote / Job rows | PASS | Full label sweep: exactly the 8 specified rows |

### Extra checks
- Chevron toggle works both directions (MuiCollapse `hidden ↔ entered`).
- Timeframe verified with real due date (“Sep 07, 2026 — Sep 30, 2026” on a wizard WO) and the “No due date” fallback.
- Single `/team` request — restore re-reads a response #1325 already fetched and discarded.

### WOs exercised
- `d75b61a2-ebd8-44b7-b30c-b7318bf73cbb` (Job - Sep 7, 12:07 PM) — fallback paths
- `5a490a79-6d2a-456e-a3f3-e9eb3ac8a2c7` (test 3 sep) — service-typed, certifier populated
- `07beee76-086d-44cb-b46b-7cae8ed8ad77` (QA-DEMO ZP3978 WO-1) — due date + account row

## Flags for the merge decision
1. **Dev-parity gap (action needed):** `showDetailsInCompact` exists only on `cicd/qa`. Next dev→qa promotion reverts the restore. Must land on `cicd/dev` first.
2. **Known minor (from PR self-review, code-verified):** `sessions.service` and `sessions.noDueDate` missing from both locale files → French renders English inline defaults. Could not re-prove live: setting `i18nextLng=fr` in localStorage does not switch the app language (whole UI stayed English, including keys that DO have fr translations) — the login-page toggle appears to be the only surfaced switch.
3. **Open design question:** #1325 dropped the card deliberately; stag/prod still carry it. Eric's call whether the removal was intentional-permanent. This verification only establishes the restore works *if* the panel is wanted.

## Evidence
- `docs/bug-evidence/zp-1391-wo-details-panel/01-panel-expanded-populated.jpg`
- `docs/bug-evidence/zp-1391-wo-details-panel/02-service-typed-wo-certifier.jpg`
- `docs/bug-evidence/zp-3978-site-ownership/01-wo1-under-acct-a-before-transfer.jpg` (account row + due date)
