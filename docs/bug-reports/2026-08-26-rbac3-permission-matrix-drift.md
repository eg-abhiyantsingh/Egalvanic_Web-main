# RBAC-3 — the recorded permission matrix no longer matches any live role

**Env:** `https://acme.qa.egalvanic.ai` · **Date:** 2026-08-26 · **Severity:** Medium · **Writes:** none
**Register:** https://claude.ai/code/artifact/363a5868-d749-4db0-a9be-d9635c87ea88

Baseline: `testcase/prod_permissions-by-role_202606151113.csv` — 555 grants, 7 roles,
**exported from production 2026-06-15**. Measured: all six provisioned roles logged in live through
the real login form, cookies + storage cleared between each.

## Full diff (live QA vs recorded matrix)

| Role | Live | Matrix | Live has, matrix doesn't | Matrix has, live doesn't | role_id |
|---|---|---|---|---|---|
| Admin (reports "Super Admin") | 111 | 98 | 13 | 0 | matches |
| Project Manager | 90 | 93 | 5 | 8 | matches |
| Technician | 93 | 90 | 3 | 0 | matches |
| Facility Manager | 75 | 77 | 4 | 6 | matches |
| Account Manager | 80 | 77 | 9 | 6 | **MISMATCH** |
| Client Portal | 36 | 37 | 2 | 3 | matches |
| **Total** | | | **36** | **23** | **59 cells** |

CI reported 33 failing cells for 3 roles. The real figure is **59 cells across all 6**.

## Three things the CI summary did not convey

### 1. Twenty-three grants went the *other* way — roles lost permissions
```
Project Manager   lost  forms.manage · reports.manage · features.arc_flash.view
                        features.audit_log.view · features.condition_assessment.view
                        features.panel_schedules.view · features.settings.pm.view
                        features.settings.view
Facility Manager  lost  forms.manage · reports.view · features.locations.view
                        features.site_overview.view · features.equipment_designations.view
                        folders.manage
Account Manager   lost  attachments.manage · attachments.view · features.slds.view
                        features.jobs.view · features.settings.pm.view · features.settings.view
```
**This revises an earlier verdict.** I recorded that the PM's Access Denied on `/eg-forms` and
`/reporting/builder` was "correct behaviour — PM genuinely lacks `forms.manage`". True of the live
state, but the matrix says the PM *should* hold both. That denial may be unintended capability loss
rather than correct gating — and it cannot be settled without a current baseline.

### 2. The Account Manager role was recreated, not edited
```
Account Manager   matrix role_id  92f38105-0c53-475b-ae57-52dcc4a96f11
                  live   role_id  392a2233-e4a3-4322-a440-7fd62b4bed7e
```
Every other role kept its identifier. A changed id means the row was dropped and rebuilt — a
different kind of event, worth asking about specifically.

### 3. "PRIVILEGE ESCALATION" overstates what the evidence supports
The baseline is a **prod export from 2026-06-15**; I measured **QA today**. The difference mixes
three things the test's wording collapses into one: genuine unreviewed drift, ordinary prod↔QA
divergence, and ten weeks of legitimate product change. The last is demonstrably present —
`features.planned_work.view` appears as "extra" on **five of six** roles and `site_walks.*` on three,
which is what a newly-shipped feature looks like, not an escalation.

**Do not read 36 extras as 36 escalations.** Exactly one extra is independently suspicious on its own
merits — the Technician's `platform.web` — and that is already filed as RBAC-1, because the role's
stated purpose contradicts it.

## Evidence
- `evidence/2026-08-26-rbac3-matrix-drift/tech_authme_drift.jpeg` — live `/api/auth/v2/me` from a real
  Technician session: 93 entries vs the matrix's 90, with the three extras marked.
- `evidence/2026-08-26-rbac3-matrix-drift/live-permissions-2026-08-26.json` — full live sets for all
  six roles, so this diff is reproducible without re-running the browser.

## Fix
Re-export the matrix from the environment under test, commit it beside the prod one, and have the
contract test diff QA against QA. Until then the suite reports a number nobody can act on — which is
why `rbac-*` workflows have been majority-red since July without anyone triaging them.

**Not tested:** Electrical Engineer — no QA account exists (login 401), documented in `RbacFixtures`.
