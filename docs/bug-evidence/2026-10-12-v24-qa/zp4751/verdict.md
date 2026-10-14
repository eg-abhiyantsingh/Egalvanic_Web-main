# ZP-4751 — Type-to-search on dataset-backed EG form selects (Web + iOS)

**Verdict:** PASS (web). Checked on QA build `index-C-Cc1qD1.js`, 13 Oct 2026, 16:40–17:07 IST; the build was checked again just before this verdict. In the work-order form drawer, a dataset-backed select is now a searchable box ("Type to search…"). Typing filters the list, a match can be picked by mouse or keyboard, and the value saves and comes back after a reload. The iOS half goes to Nency (ZP-4817).

Jira: status In QA, no comments (read live 13 Oct). No LaunchDarkly gate on the renderer. `feature-eg-forms` (older, ON for Acme) only shows/hides the WO Forms tab. **No flag flip is needed.**

## Test data (all created by me, label "delete me")
- EG form **QA-DEMO ZP-4751 ZP-4753 dataset type-to-search (delete me)** `dc24bb98-e4a8-47e5-a528-39db9bb2e56e` (https://acme.qa.egalvanic.ai/eg-forms). It has 9 blocks:
  - S1: users select
  - S2: test-equipment select
  - M1/M2: users and test-equipment multiselects
  - M3: users multiselect with checkbox_list display
  - C1/C2: static select and multiselect (controls)
  - T1: text
- Two form instances, both mine, on asset **13N-H1-1** in the QA-DEMO work order **QA-DEMO ZP-4801 Tuned column (delete me)**: https://acme.qa.egalvanic.ai/sessions/22a22b9c-4eda-43ed-b131-4b7146912441 › More › Forms. The instances are `0006ffb0-…` (used) and `b42e3ab7-…` (a duplicate from my second create call, empty). Both are drafts and were never submitted.
- Datasets on Acme QA: **Users = 246 rows** (the large list), Test equipment = 10 rows.

## Checked

| # | Check | How | Result |
|---|---|---|---|
| QA-1 | Form with a dataset select that has many options | S1 = Users (246) | Done |
| QA-2 | Web: typing filters the list and a match can be chosen | WO drawer, S1 | PASS. Open shows 246; "abh" → 25; "iyan" (middle of a word) → 25; "abhiyant ad" → 1 → picked (shot 3) |
| QA-2 | Case | "abh" / "ABH" / "aBh" | PASS. 25 each, no difference |
| QA-2 | No match | "zzqx9" | PASS. Shows "No options" (shot 4) |
| QA-2 | Special characters | `(` `.*` `[` `\` `%` `'` `<b>` `+` | PASS. Taken as plain text, no error, no crash. `+` finds the 17 users whose name is an email with + |
| QA-2 | Leading/trailing space | " abh", "abh " | 0 results (the text is not trimmed). Listed as an enhancement |
| QA-2 | Search by the second part of the label | S2 test equipment, "5665" | PASS. Finds "DLRO (56654)": the serial number is part of the label |
| QA-2 | Keyboard | Type, then ArrowDown + Enter; Tab out; Escape | PASS. Picks the option. Escape closes the list and not the drawer |
| QA-2 | Clear | X button on S2 | PASS. Value cleared |
| QA-5 | Selected value saves | Save 1 as Draft → `PUT /api/eg-form-instance/bulk-patch` | PASS. Sends `qa_user_select: {_ext:"users", value:"77e99d86…"}` and `qa_te_select: {_ext:"test_equipment", value:"c955fd0a…"}` (shot 5) |
| QA-5 | Value comes back after reload | Reload, reopen the drawer | PASS. "abhiyant admin" and "DLRO (56654)" are shown (shot 6) |
| QA-5 | Clearing saves too | Clear S2 → save → reload | PASS. Patch `qa_te_select:""`; stays empty after reload (shot 8). The stored row matches |
| Control | Static (non-dataset) select | C1 | Still a normal dropdown (unchanged) |
| Preview | Forms list side preview | /eg-forms › click the row | Shows the searchable boxes, read-only (disabled), as expected for a preview (shot 1) |
| Preview | Builder Web / Mobile view | Edit Form › Web View, click S1 | **Out of scope:** the builder preview shows dataset selects as a plain dropdown with only "Select…" and no data (shot 2). The builder has its own preview, not EGFormRendererV2, which is the part this ticket changed. See out-of-scope C |
| API | Dataset calls | Network, drawer open | `GET /api/eg-form-sources/users` once: 200, 619 ms, 7.7 KB on the wire (30 KB JSON, keys id/name/is_current). `GET /api/test-equipment` once: 200, 626 ms, 1.9 KB. A `?search=` param is ignored (returns all 246); filtering is done in the browser |
| Perf | Requests per keystroke | Typed 30+ strings | **0** requests while typing. Each key updates the list in 8–44 ms (246 options). Clearing back to the full 246 takes 26 ms |
| Tenant | Users list is company-scoped | Super Admin (staff), PM, EE, FM, CP | All get the same 246 Acme users (no other company's users) |
| Roles | PM | Signs in, opens the WO drawer, types | PASS: "abh" → 25 (shot 7). Saved values are shown |
| Roles | EE / FM | | EE has the form permissions but no Operations menu, so can't open the WO from the UI. FM is not assigned to the WO's site. Both get the datasets by API (200) |
| Roles | Client Portal | | No Work Orders/Forms in the CP nav. `/test-equipment` → 422 (refused). `/eg-form-sources/users` → 200 with all 246 names (see out-of-scope B) |
| Console | | All runs | No errors from the form |

## Bugs found
None in this ticket's scope.

## Out-of-scope findings (not caused by this ticket; for the owner to decide)
- **B. The Client Portal user can download the full company user list (Low, privacy lead).** As `CP_EMAIL`, `GET /api/eg-form-sources/users` returns 200 with all 246 Acme user names. 57 of them are email addresses, e.g. Egalvanic staff "+acme" logins. The CP has no screen that uses this list. Expected: the call is refused for the Client Portal, or limited to what a CP form needs. For the developer: backend source endpoint, Avani. Check with the owner whether CPs are meant to fill forms with a users field.
- **C. Builder preview shows dataset selects/multiselects empty (Low).** Edit Form › Web View: S1/S2 show only "Select…", M3 (checkbox list) shows no boxes. The author can't see or try the real options before publishing (shot 2).
- **D. Forms tab groups forms that have no key under one name (Low).** My form has no `key` (it was created through the API, like several real forms, e.g. "03 july"). Its instances appear under "QA-DEMO ZP-4717 visible normalization" in WO › Forms › Form Types (both forms have key = null).

## Enhancement suggestions
- Show a second line (email) for users with the same name. 85 of the 246 names are duplicates ("A P" ×3, "Abhiyant Singh" ×17), so the user can't tell which person they are picking.
- Trim spaces in the search text (" abh" finds nothing today).
- Data-table cells backed by a dataset (`Tn`, e.g. a test-equipment column) are still a plain native dropdown with no search. Give them the same search box.
- Lead: at least one seed NETA form ("Low Voltage Circuit Breaker Inspection") declares "Test Equipment Used" as a **text input** with `options_source: test_equipment`. It renders as free text, so that form gets no picker at all.

## Mobile part for Nency
- **ZP-4817 [iOS] Searchable picker for dataset-backed EG form selects** (Ready for QA). Use my form above on WO "QA-DEMO ZP-4801 Tuned column (delete me)", asset 13N-H1-1, on iPad/iPhone. Check that S1 (Users, 246) and S2 (Test equipment) open a searchable picker, that typing narrows the list (try "abh", "ABH", "5665", "zzz" for no match), and that the picked value shows on web after sync. The web saves `{_ext:"users", value:<id>}`.
- **iOS regression from the ticket:** a **users-sourced** select must still load its list (the `.task` loader / `options_filter` was ported by hand) and still filter.
- Android: ZP-4752 is Resolved; a smoke check of the same form is worthwhile.
- The web shows S1 = "abhiyant admin" and S2 empty now; the iOS app should show the same saved values.

## Not covered
- iOS/Android: for Nency.
- A real LaunchDarkly OFF/ON: there is no flag for this change.
- Submit: I kept the instance as a draft so the form stays reusable. Save Draft and Submit use the same `bulk-patch` payload.

## Proposed Jira comment
```
h3. QA result on QA (build index-C-Cc1qD1.js, 13 Oct 2026): PASS (web)

Test form: "QA-DEMO ZP-4751 ZP-4753 dataset type-to-search (delete me)" on work order "QA-DEMO ZP-4801 Tuned column (delete me)", asset 13N-H1-1. The select is backed by the Users dataset (246 rows).

||Check||Result||
|Typing filters the list ("abh" → 25 of 246, any part of the name)|PASS|
|Upper/lower case gives the same result|PASS|
|No match shows "No options"; special characters do not break it|PASS|
|Pick with mouse or keyboard (arrow + Enter); clear with X|PASS|
|Test equipment can be found by serial number ("5665" → DLRO (56654))|PASS|
|Picked value saves and is still there after reload; a cleared value stays cleared|PASS|
|No server call while typing (list loads once, 0.6 s)|PASS|
|Project Manager can search and pick|PASS|

!QA-13Oct-ZP-4751-3-wo-form-users-select-typed-abh-filters-PASS.png|width=720!
!QA-13Oct-ZP-4751-6-after-reload-values-persist-PASS.png|width=720!

Notes: the builder's Web View preview still shows dataset selects empty (separate, not this change). 85 of the 246 user names are duplicates, so showing the email next to the name would help. The iOS part (ZP-4817) is with Nency.

*Status:* READY TO RELEASE (web).
```

## Recommended status
**READY TO RELEASE** for the web part. No flag is involved. The iOS half is tracked in ZP-4817 (Nency).

## Evidence (this folder)
1 list preview (read-only) · 2 builder Web View, dataset select empty (out of scope) · 3 typed "abh" filters · 4 no match · 5 values chosen · 6 persist after reload · 7 PM search · 8 cleared select stays empty.
Scripts/JSON: `.playwright-mcp/v24/q13-4581-4751/` (e2–e8, r1, eg_lib.js, wo_lib.js, formdef.js).
