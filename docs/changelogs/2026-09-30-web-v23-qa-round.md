# Web v2.3 — Ready-for-QA round on QA (30 Sep 2026, 17:55–21:15 IST)

**Prompt:** "https://egalvanic.atlassian.net/projects/ZP/versions/14236/tab/release-report-all-issues start testing web 2.3
version ticket".

Scope (standing rule): only the 18 tickets in **Ready for QA** were tested; READY TO RELEASE (6), To Do (14) and Done (19)
are listed by key only. Environment: acme.qa.egalvanic.ai, build `index-DXSz_B6n.js`.

Release page: https://claude.ai/artifact/3e3VwLmEk5fDa7pQEyb24y (Web v2.3 Release Check)

**Jira:** 10 moved Ready for QA → READY TO RELEASE (transition 7, status only): ZP-4472, 4367, 4366, 4370, 4341, 4365,
4449, 4214, 4433, 4302. The other 8 stay in Ready for QA for the owner's decision.

## Verdicts

| Ticket | Result on QA |
|---|---|
| ZP-4472 Asset Name column hidden | pass (nested, flat, Bulk Ops, reporter's site) |
| ZP-4367 Mains Type / Phase Configuration legend | pass (small a11y note: info icon not focusable) |
| ZP-4366 Issues stable paging + Export CSV | pass (tied timestamps across page breaks; CSV formula-escaping) |
| ZP-4370 Location photos from the asset | pass (room upload, caption, delete; no confirm on delete) |
| ZP-4341 Newkirk: NFPA 70B names, nested Assets, Copy Last Row, verdict-aware progress | pass (web) |
| ZP-4365 Create-mode extraction uses profile photos | pass (profile-only, other-only, nameplate+profile; 2 paid runs) |
| ZP-4449 Client Portal 422 on report generation | pass (200 + PDF, both toasts) |
| ZP-4214 Bulk configure skips unchanged assets | pass, web half (Configure → Apply → Configure = "Skipped — information unchanged") |
| ZP-4433 Dev → QA merge | pass (smoke across 9 seats; migrations visible) |
| ZP-4303 service→method resolver (API) | pass on QA data; Eaton numbers / >600 V busway not on QA; no seat without site access (Client Portal reaches 234 sites) |
| ZP-4368 Newkirk + /lookup gates | pass on QA; other-tenant 404 not runnable (demo off-limits) |
| ZP-4394 EG form class filter / Section Done | partly tested: Section Done + section_states pass; builder class filter not tested |
| ZP-4302 collapsed-node deletes from list / page / bulk | pass: list, asset page and bulk deletes of collapsed assets succeed with `?allow_collapsed=true`; the SLD's own `bulk-delete` is still refused (400 naming the view); on-screen SLD refusal not reachable (editing locked in the test session) |
| ZP-4301 delete-refusal reasons | partly: the server names the view (400 `collapsed_node_not_deletable`); the named toast can no longer appear on the web (list/page send allow_collapsed); "+N more" toast and PM plans not tested |
| ZP-4305 Resolve issues from the issue form | pass with 2 small defects (stale "Resolved" header after reopen; /pull-through-work still opens) |
| ZP-4398 AI report-config editor on SLD configs | pass except: preview starts on the A–Z-first of 50 sites, not the site with most assets |
| ZP-4421 Search in "Add Form" dialog | fails (no "Applies to" matching; no PR) |
| ZP-4148 Portal Sales gates the portal preview | fails as written; duplicate of ZP-4138, superseded by ZP-4390 |

## Code changes

### `V23RoleProbe` (new, `src/test/java/com/egalvanic/qa/testcase/`)
One sign-in (the seat from `-DUSER_EMAIL` / `-DUSER_PASSWORD`), then the checks named in `-Dv23.checks`:
- `nav`, `mp` — the rail, the Maintenance Portal items and padlocks, what each portal route shows (ZP-4148).
- `asset` — the Location card / Location Photos dialog per role (ZP-4370).
- `report`, `genreport` — Maintenance Portal › Reports; presses a card and the dialog's Generate Report, records every
  `/report…` call and the Sonner toasts, and catches the download link in the page (nothing saved) (ZP-4449).
- `bulkskip` — re-runs the bulk AI extraction on an already configured + applied asset and records the dialog (ZP-4214).
- `collapse`, `collapse2` — builds QA-DEMO nodes and a custom SLD view through the app's own API, collapses them in the view,
  then deletes from the /assets row, the asset page, a bulk delete, and the SLD editor (ZP-4302 / ZP-4301).
- `sldguard` — opens the QA-DEMO custom view in the SLD editor, selects all and presses Delete (the refusal case).
- `sites` — the seat's site picker list and site-scope fields in `/auth/me`.
- `shots` — plain page captures, `-Dv23.shots="route|caption;…"` (`site=<id>` switches the active site).
Why: most v2.3 checks depend on the role or need a visible browser for screenshots; the user's Chrome window was hidden
for part of the evening (`document.visibilityState = hidden`, screenshots time out), so the probe's own Chrome carried the
evidence. Check names are matched as whole words (`hasCheck`), so `collapse` no longer also runs `collapse2`.

## Evidence
`docs/bug-evidence/2026-09-30-v23-qa/zp<number>/` — screenshots + `results.txt` per ticket; role-probe captures in
`test-output/v23-role/` (local).

## Test data left on QA (labelled, sandbox)
- QA-DEMO work order `51d54087-6a74-4ddd-9698-f34e40e14015` gained a Torque Record form (2 copied rows, both sections done)
  and 2 linked QA-DEMO issues.
- "Annual Maintenance Report (Copy)" config cover now has the line "QA-DEMO ZP-4398 AI edit test (delete me)".
- Site "10 SEP 2026": QA-DEMO ZP-4302 nodes and views (tags 77203, 63552).
