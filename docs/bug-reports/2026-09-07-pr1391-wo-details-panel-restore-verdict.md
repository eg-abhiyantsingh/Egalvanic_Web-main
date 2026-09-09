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


---

## ROLE COVERAGE — added 2026-09-09 (the original run was single-seat)

The verdict above was produced on ONE seat (the `+admin` multi-role seat with Super Admin active).
Asked whether it holds for all roles, it does not, and the difference is a permission gate:

    const Yli = ["accounts.view", "features.accounts.view"];   // bundle index-jYhUcFb4.js
    const C = Sot(Yli);                                        // hasAnyPermission
    ... C && detailRow(t("common.account"), session.account_name) ...

The same gate hides the **Account column and Account filter** on the work-order list.

| Role | perms | accounts.view | WO list | Account column | Details panel | Account row |
|---|---|---|---|---|---|---|
| Super Admin / Admin | is_admin | yes | all | shown | renders | **shown** (8 rows) |
| Project Manager | 94 | yes | 996 | shown | renders | **shown** (8 rows) |
| Account Manager | 77 | yes (+features) | 988 | shown | not reached in UI this run | — |
| Electrical Engineer | 80 | yes | 71 | shown | not reached in UI this run | — |
| Facility Manager | 75 | **no** | 5 (all closed) | **absent** | renders | **absent** (7 rows) |
| Client Portal | 35 | no | `422 permission_denied` | n/a | n/a | n/a |
| Technician | 95 | yes | no web access | n/a | n/a | n/a |

So the checklist item "the Account row appears above Facility" needs the qualifier **"for roles with
`accounts.view`"** — otherwise a Facility Manager retest will be filed as a regression that is
actually the gate working as written.

**Also found (beyond this ticket):** `GET /api/ir_session/{id}/full`, `/team` and `/summary/v2`
return **200 with the full payload** — including `account_name` — for a work order on a site OUTSIDE
the caller's `accessible_sld_ids`, for every role tested including Client Portal, while the list
endpoint `POST /company/{id}/workorders/v2` scopes correctly (FM 5, EE 71, PM 996). Within one
tenant, and all those roles do hold `sessions.view` globally, so it may be intended that site scope
is a filter and not a boundary — but the two halves disagree and it is worth a decision.

**Checked and dismissed:** FM's Work Orders grid opens "0–0 of 0" — not a scoping bug. All five of
FM's work orders are `active: false` and the grid defaults to Status = Open; switching to Closed
shows 1–5 of 5.

**Role-coverage artifact:** https://claude.ai/code/artifact/7aa2ccd7-13c2-4357-a4f1-0a2e3ecda48c
· evidence `docs/bug-evidence/roles-wo-panel-account-gate/`
