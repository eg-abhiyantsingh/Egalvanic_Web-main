# ZP-4315 filed + screenshots and steps added to the v2.2 board (2026-09-21)

**Prompt:** "add screenshot of testing with proper steps" … plus, mid-turn:
"create a bug — /sessions/1a9c5d13… value is showing different, assign to kush, assign priority".

## 1 · New bug: ZP-4315

<https://egalvanic.atlassian.net/browse/ZP-4315> — **Bug · Medium · Kush · To Do**
*Web: Work order completion disagrees with the iOS app — web shows 47% (7 of 15), iOS shows 38% (3 of 8)
for the same work order.*

Reproduced live before filing, on QA build `index-BV-phiFE.js`:

| Client | Ring | Denominator |
|---|---|---|
| Web | 47%, tooltip `Overall: 7 of 15 (47%)` | 15 |
| iOS (TestFlight, 12:58 PM) | 38%, `3 of 8` | 8 — EngData 2 + Checklist 2 + Panel Sched 1 + Forms 3 |

WO: *Work Order - Sep 8, 5:20 PM web ir check* (Multiple Services, Medium)
<https://acme.qa.egalvanic.ai/sessions/1a9c5d13-530d-4b7d-beb7-6a52ac84ebfc>

`GET /api/ir_session/{id}/summary/v2` returns 14 assets / 1 issue / 0 tasks and carries **no** completion
breakdown — so the completion maths lives in each client, which is why the two can drift.

Artifact page (screenshot + numbered steps): <https://claude.ai/artifact/6fMMxe8x2yWxEkEshhqDq2>
Evidence: `docs/bug-evidence/2026-09-21-zp4315-completion-mismatch/`

Jira changes made: create + assignee + priority + Backlog → To Do. Nothing else touched (no fixVersion,
labels or components) — those need their own yes.

## 2 · The v2.2 board now carries evidence

<https://claude.ai/artifact/FD2esA3TgCg2cenoDEHP26> (v2) gained an **Evidence and steps** section: for each
confirmed defect, a real frontend screenshot, numbered steps in plain words, What happens / What should
happen, and a *For the developer* line at the end.

Covered: ZP-4042 (2 shots), ZP-3782, ZP-4212, ZP-4189, ZP-4038 (positive evidence), ZP-4145.

New screenshots captured today on QA: `docs/bug-evidence/2026-09-21-v22-progress/` —
`/maintenance-portal/reports` Access Denied, `/maintenance/reports` catalog, and the single-site Dashboard.

## 3 · One over-report avoided

The `/maintenance/reports` page shows every row of its *Work order reports* table as "In progress". That
looked like a visible symptom of the ZP-4042 500 — it is not. Grepping the shipped bundle shows the
report-history loader is called only behind the portal's own flag (`latestReportOnly`), so nothing on the
staff page touches that endpoint. Not filed.
