# Full regression on QA after the v2.3 release: triage

- **Run:** https://github.com/eg-abhiyantsingh/Egalvanic_Web-main/actions/runs/38020658323
- **Timing:** started 08:58 IST, ended about 16:52 IST.
- **QA build:** index-BJGNnWD7.js
- **First pass:** 1,343 tests; 1,181 passed, 115 failed, 47 skipped.
- **After re-run:** 110 still failing, 5 recovered. Smoke and Suite 2 passed earlier the same day.

## What the 110 failures are (checked by hand where marked)

| Group | Count | What it is |
|---|---|---|
| Accessibility gates (Phase4QualityGates) | 50 | Long-standing WCAG findings per route; not new to v2.3 |
| Asset Engineering (exhaustive/matrix/CB/mains/transformer) | 46 | **Product behaviour changed, identical on QA and PROD (checked by hand).** VFD and Motor Starter no longer show Phase Configuration or Mains Type; Switch has no Manufacturer; Rectifier shows Primary/Secondary Voltage; Disconnect Switch lost the >1000V subtypes and Load-Interruptor and has a junk-looking "Scrubtype" subtype. Needs dev/product confirmation. If the change is intended, the tests need updating. |
| Locations (Part 2) | 18 | All fail on one floor the setup creates ("P2_Floor_3628" not found). Add Floor / Add Building exist (checked by hand). Test-side, needs a look. |
| Work order create/list/issue | 18 | The create form changed (Priority, Scope, Team, Schedule block); the list has no "Show planned". Test maintenance. |
| BugHunt login tests | 8 | Old one-step login locators (password field not present until "Use my password"). Test maintenance. |
| Accounts V135 | 6 | Role-switch helper "role input not found", grid columns read empty. Test maintenance (to confirm). |
| API contract opinions | 7 | 200 for unknown routes (SPA fallback), POST returns 200 not 201, etc. Long-standing design. |
| Critical path / dashboard | 5 | "Dashboard KPIs did not render" with counts 0. By hand the dashboard renders its numbers for the selected site, so this is likely the KPI locator or site data. |
| Equipment Designations | 1 | The page renders, but with a new table (not MUI DataGrid). Test maintenance. |
| Task create form | 1 | "Task Type" IS present by hand; likely a timing issue ("Loading form data…"). |
| Goals | 1 | New status "Needs Attention" is not in the test's allowed list. Test update. |
| Org lookup resilience | 2 | **Still open since 25 Sep (v2.2):** a failed `/api/company/alliance-config` shows "Organization Not Found" with no retry. No Jira ticket found. |
| Security / deep bug catalog | 4 | Known: no rate limit (ZP-2025), no 404 page, no title maxlength, cookie check. |
| Issues part 2 | 2 | "Edit drawer not interactable". Test-side. |

Evidence for the manual checks: `docs/bug-evidence/2026-10-10-ci-triage-qa/`.
