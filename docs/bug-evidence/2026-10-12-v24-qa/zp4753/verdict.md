# ZP-4753 — Type-to-search multi-pick on dataset-backed EG form multiselects (Web + iOS)

**Verdict:** PASS with 1 Low UX bug (web). Checked on QA build `index-C-Cc1qD1.js`, 13 Oct 2026, 16:40–17:07 IST; the build was checked again just before this verdict.

In the work-order form drawer, a dataset-backed multiselect is a "Type to search…" box with chips:
- Typing filters the list, and several values can be ticked.
- Clearing the search brings the full list back with the ticked ones kept.
- Adding **and removing** values saves and survives a reload.

The UX bug: after each tick, the search text is wiped and the list jumps back to all 246 users, so you must retype the search for every pick. The iOS half goes to Nency (ZP-4818).

Jira: status In QA, no comments (read live 13 Oct). No LaunchDarkly gate on the renderer (`feature-eg-forms` only shows/hides the WO Forms tab, ON for Acme). **No flag flip is needed.**

## Test data (all created by me)
The same form and work order as ZP-4751:
- Form **QA-DEMO ZP-4751 ZP-4753 dataset type-to-search (delete me)** `dc24bb98-…`.
- Work order https://acme.qa.egalvanic.ai/sessions/22a22b9c-4eda-43ed-b131-4b7146912441 › More › Forms › asset **13N-H1-1** (instance `0006ffb0-…`).
- Fields:
  - M1 = Users multiselect (246 rows)
  - M2 = Test-equipment multiselect (10 rows)
  - M3 = Users multiselect with `display: checkbox_list`
  - C2 = static multiselect (control)

## Checked

| # | Check | How | Result |
|---|---|---|---|
| QA-1 | Dataset multiselect with many options | M1 Users (246) | Done |
| QA-2 | Search box appears; partial text filters | M1: "abhiyant ad" → 1, "shubh" → 1, "nency" → 20; M2: "dlr" → 3 | PASS. Placeholder is "Type to search…" |
| QA-3 | Tick several filtered options; all saved | M1: abhiyant admin + Shubham qafacility + Nency Sardhara (keyboard ArrowDown+Enter for the third) | PASS. 3 chips (shot 1). The patch sends `qa_user_multi:{<id>:true ×3}`. All 3 come back after reload (shot 5) |
| QA-3 | Same for test equipment | M2: "DLRO (56654)" + "Abhiyant Singh (abhiyant serial)" | PASS. Saved; still there after reload |
| QA-4 | Clear the search: full list returns, ticked stay ticked | Type "zzq" (shows "No options"), then delete the text | PASS. 246 options, the 3 ticked are `aria-selected=true`, and the 3 chips stay (shot 2) |
| Extra | Removing a value saves | Remove 1 chip in M2 (2 → 1) and in M1 (3 → 2) → Save Draft → reload | PASS. The patch sends the whole new set (`{c955…:true}`); the server replaces it, it doesn't merge, so a removed value doesn't come back (shots 3, 6). The stored row matches |
| Extra | Untick from the list / X on a chip / Backspace in an empty box | M1 | PASS. All three remove the value (Backspace on an empty box removes the last chip: standard behaviour, worth knowing) |
| Extra | Checkbox-list display with a dataset (M3) | M3 | Rendered as the same search box with chips (the dataset path wins over `checkbox_list`). Picks save |
| QA-5 | Regression: single select (ZP-4751) | S1/S2 | PASS (see ZP-4751) |
| Control | Static multiselect | C2 | Unchanged native multi-dropdown |
| UX | Search text after ticking one match | Type "abhiyant ad", tick it | **The search text is wiped and the list jumps back to all 246** (shot 4). To tick 3 people named "Abhiyant…" you type "abh" three times. See Bug 1 |
| UX | Can you see what's ticked in the open list? | Open list after 3 picks | Ticked rows look the same as the others (no tick mark); only the chips show the selection. Enhancement |
| Preview | Builder Web View | Edit Form › Web View | Out of scope: dataset multiselects show an empty dropdown; M3 shows no boxes (same as ZP-4751 finding C) |
| API | Data + save | Drawer open; save | One `GET /eg-form-sources/users` (619 ms, 7.7 KB) + one `GET /test-equipment` (626 ms); save = `PUT /eg-form-instance/bulk-patch` |
| Perf | Typing | Each key on the 246-row list | 8–44 ms per key, **0 requests while typing** |
| Tenant | Users list scope | Super Admin, PM, EE, FM, CP | Same 246 Acme users for all; no other company's users |
| Roles | PM | Opens the drawer | PASS. Sees the saved chips (Nency Sardhara, Shubham qafacility) and can search |
| Roles | Client Portal | | No Forms in the nav; `/test-equipment` 422. `/eg-form-sources/users` 200 (privacy lead, see ZP-4751 out-of-scope B) |
| Console | | All runs | No errors |

## Bugs found

**Bug 1 (in scope, frontend, Low): ticking one match wipes the search text, so the next match has to be typed again**
1. Open a work-order form with a dataset multiselect, e.g. "M1 Users multiselect" on QA-DEMO ZP-4801 › More › Forms › 13N-H1-1.
2. Type "abh". 25 matching users show.
3. Tick one of them.
- What happens: the box empties and the list jumps back to all 246 users. To tick a second "Abhiyant", you type "abh" again.
- What should happen: the search text stays after a tick, so you can tick several matches from the same filtered list. The ticket says "type to filter the option list and tick multiple matches", and its own check "clear the search text … full list returns" assumes the text stays.
- Why it matters: on long lists (users, test equipment), picking several similar items takes many extra keystrokes.
- Evidence: `QA-13Oct-ZP-4753-4-after-ticking-one-search-text-clears-full-list-FAIL.png`.
- For the developer: the MUI Autocomplete `multiple` + `disableCloseOnSelect` resets `inputValue` on select. Keep it controlled (`inputValue`/`onInputChange`, ignore `reason === "reset"`).
- Suggested: Krunal (frontend). Per the owner's 13 Oct rule, file to Avani.
- No existing ticket found (JQL: dataset / multiselect / type-to-search, last 60 days).

## Out-of-scope findings
These are shared with ZP-4751; see its verdict for steps:
- B: the Client Portal can download the full 246-name user list.
- C: the builder preview shows dataset fields empty.
- D: forms with no key are grouped under another form's name in WO › Forms.

## Enhancement suggestions
- Show a tick (checkbox) beside options that are already chosen in the open list; today only the chips show it.
- Show the email under duplicate user names (85 of 246 names repeat).
- Add a "Select all matches" action for the current search (e.g. tick all 3 "DLRO" meters at once).
- Trim leading/trailing spaces in the search text.

## Mobile part for Nency
- **ZP-4818 [iOS] Searchable multi-pick sheet for dataset-backed EG form multiselects** (Ready for QA). On iPad, use the same form/work order (asset 13N-H1-1). Open M1 (Users, 246) and M2 (Test equipment), search, and tick several. Clear the search and confirm the ticked ones stay ticked. Save, and check the web shows the same chips. Today the web shows M1 = Nency Sardhara + Shubham qafacility, M2 = DLRO (56654), M3 = Shubham qafacility.
- Also check whether the iOS sheet keeps the search text after a tick (the web does not; Bug 1).
- Android: ZP-4754 is Resolved; a smoke check of the same form is worthwhile.

## Not covered
- iOS/Android: for Nency.
- Submit: kept as a draft (same `bulk-patch` call as Save Draft).
- A real LaunchDarkly OFF/ON: there is no flag.

## Proposed Jira comment
```
h3. QA result on QA (build index-C-Cc1qD1.js, 13 Oct 2026): PASS (web), 1 small UX bug

Test form: "QA-DEMO ZP-4751 ZP-4753 dataset type-to-search (delete me)" on work order "QA-DEMO ZP-4801 Tuned column (delete me)", asset 13N-H1-1. Users dataset = 246 rows.

||Check||Result||
|Search box shows; typing filters the list|PASS|
|Tick several matches (3 users, 2 test equipment); all saved and still there after reload|PASS|
|Clear the search: full list returns, ticked ones stay ticked|PASS|
|Remove a chip, save, reload: it stays removed|PASS|
|Keyboard (arrow + Enter) works|PASS|
|No server call while typing|PASS|
|Single select (ZP-4751) still works|PASS|

!QA-13Oct-ZP-4753-1-three-users-picked-by-search-PASS.png|width=720!
!QA-13Oct-ZP-4753-2-search-cleared-full-list-three-ticked-PASS.png|width=720!
!QA-13Oct-ZP-4753-4-after-ticking-one-search-text-clears-full-list-FAIL.png|width=720!

*Small bug (filed separately):* after you tick one match, the search text is wiped and the list jumps back to all 246 users.
# Type "abh" in the Users multiselect (25 matches).
# Tick one.
*Actual:* the box empties and all 246 users show again; you have to type "abh" again for the next one.
*Expected:* the search text stays, so you can tick several matches in a row.

The iOS part (ZP-4818) is with Nency.

*Status:* READY TO RELEASE (web); the search-reset bug goes in as a separate Low ticket.
```

## Recommended status
**READY TO RELEASE** for the web part. The feature works and saves correctly, including removals. The search-reset is a Low UX follow-up, to be filed separately. If the owner reads the ticket's "tick multiple matches" strictly, keep it In QA until that is fixed. No flag is involved.

## Evidence (this folder)
1 three users picked by search · 2 search cleared, 3 still ticked · 3 removed equipment chip stays removed · **4 search text wiped after a tick (FAIL, Low)** · 5 three users persist after reload · 6 removed user chip stays removed.
The builder screenshot is in `../zp4751/` (shot 2). Scripts/JSON: `.playwright-mcp/v24/q13-4581-4751/` (e5–e8, r1).
