# ZP-5274 [Web] Company-level role permissions (PR #1813): verdict

**Result: PARTLY.**
- Regression check: PASS. Every QA seat on EG-ACME except Technician gets exactly the permissions, menu features and `field_limits` it had before PR #1813.
- Company scoping: works in the one override that is live on QA. EG-ACME's Technician grant of `platform.web` does not reach Demo Company, and a guest gets Demo's permissions.
- Two things stop a full PASS:
  1. For that guest, `/auth/v2/me` `has_web_access` is still `true`. It does not match the permissions list. NEW bug draft below.
  2. Adding or removing an override is DB-only (ticket §5, no UI or API), so QA cannot run plan rows 2, 3, 4 and 6 without a developer inserting and deleting rows on QA.
- The Technician role also changed on QA after 16 Nov, in both companies. That is ZP-5293 work, not this PR, but 6 of the 10 removed permissions were not announced (details below).

- **Build:** QA `index-oFAZQ3rK.js`, 17 Nov 2026, 11:15–11:45 IST. Backend PR #1813 reached QA on 16 Nov at about 12:35 IST (Dharmesh's comment). I could not read the PR: eg-pz-backend isn't visible to the QA GitHub account.
- **Seats:** SA, ADMIN, PM, AM, EE, FM, CP, TECH, PS (AppConstants), plus the second EG-ACME Technician seat the owner gave on 16 Nov. The TECH user was also checked as a guest in Demo Company (demo.qa.egalvanic.ai), through the app's own switch calls (POST /api/guest-access/switch + /auth/switch handoff).
- **Flag:** none (the ticket uses no LaunchDarkly flag).

## 1. Regression: every role on EG-ACME (main risk)
Sources:
- Today: `GET /api/auth/v2/me`, `/api/auth/me` and `/api/features/access` (the menu builder), per seat.
- Before PR #1813: permission counts from 12 Nov (`docs/bug-evidence/2026-11-12-v26-qa/_eric6/r02-*.json`) and 13 Nov (`.playwright-mcp/rfq13/sld-skm-upload/r08-out.json`); full sets from 22 Oct (`2026-10-22-zp4700-deep/p2-entitlements-*.json`) and 5 Nov (`2026-11-05-rfq3/ZP-5144/raw/me-*.json`).

| Seat | 12 Nov | 13 Nov | Today | Full set | Menu (`/features/access` company_features) |
|---|---|---|---|---|---|
| SA (6 roles) | 138 | 138 | 138 | = 5 Nov + assemblies.manage (already counted on 12 Nov) | 40 features, identical to 12 Nov |
| ADMIN | 114 | – | 114 | = 22 Oct + assemblies.manage | identical to 22 Oct |
| PM | 101 | 101 | 101 | = 22 Oct + assemblies.manage | identical to 22 Oct |
| AM | 83 | 83 | 83 | SLD permissions identical to 13 Nov | – |
| EE | 84 | 84 | 84 | SLD permissions identical to 13 Nov | – |
| FM | 79 | 79 | 79 | SLD permissions identical to 13 Nov | – |
| CP | 36 | 36 | 36 | identical to 22 Oct | identical to 22 Oct |
| PS (Portal Sales) | – | – | 1 (`maintenance_portal.manage`) | Web Access Restricted, same as 12 Oct | – |
| **TECH** | (98 on 5 Nov; Web Access Restricted on 16 Nov 14:00 IST) | – | **89** | +platform.web, −10 (see §3) | 6 menu features gone |

Also checked:
- `/auth/me`, `/auth/v2/me` and `/features/access` user_permissions give the same set for every seat. The three readers the plan names all agree.
- `field_limits` is still `{can_create_sites:true, can_create_work_orders:true}` for every EG-ACME seat. No new keys (`can_view_sld` is deferred, as the plan says).
- Backend enforcement didn't change: `GET /mutations` → 200 for SA/ADMIN and 422 `permission_denied` for PM/AM/EE/FM/CP/TECH/PS. Onboarding → 422 for CP/PS (needs bulk_operations.view).
- The role picker `GET /users/roles/company/<EG-ACME>` lists the same 10 platform roles as on 12 Nov (all `company_id: null`, `is_system: true`). No company-owned role exists.
- SA's per-role header (`x-active-role-id`) gives the union (138) for each role. That is the post-ZP-4033 union behaviour, unrelated to this PR.

## 2. Company scoping (plan rows 1 and 5, with the one override live on QA)
- **Two Technicians on EG-ACME** (TECH seat and the owner-given seat) have identical permission sets: 89, including `platform.web`.
- **The same Technician user as a guest in Demo Company** has 88 permissions, **without** `platform.web`, and the web app shows "Web Access Restricted". EG-ACME's grant does not reach another company, and the guest gets the acting (host) company's permissions, as the plan says. PASS.
- **Defect:** in Demo, `/auth/v2/me` still returns `has_web_access: true` while `permissions` has no `platform.web`. EG-ACME (home) correctly returns true/true. The plan says `has_web_access` must use the acting company like `permissions`. It looks resolved with the home company. → `BUG-guest-has-web-access.txt` (Low). The web UI follows the permissions list and blocks correctly. The risk is any backend check or client that reads `has_web_access`. ZP-5293 says the backend's web check uses it.
- The guest's `field_limits` in Demo = `{false, false}`. Not part of this ticket; noted only.

## 3. Technician changed on QA after 16 Nov (ZP-5293 work, for the lead and the dev)
- **Platform-wide**, lost in BOTH companies: `bulk_operations.manage`, `folders.manage`, `forms.manage`, `reports.manage`. These 4 are announced in Avani's ZP-5293 comment ("Already removed on QA").
- Also lost in both companies, **not announced**: `data.export`, `features.opportunities.view`, `features.planning.view`, `features.scheduling.view`, `features.site_overview.view`, `features.tasks.view`. The same comment says schedules and tasks "stay" for Technician. Please confirm these 6 are intended. They change the menu (`/features/access` lost opportunities, report-builder, scheduling, settings-forms, site-overview, tasks) for every company's Technicians.
- **EG-ACME only:** `platform.web` granted (the ZP-5293 recipe, with EG-ACME standing in for EC Powers Life).
- Side lead for ZP-5293 (not this ticket): the EG-ACME Technician opening the root URL `/` lands on `/ops-dashboard` → "Access Denied". After sign-in it lands on `/pm-readiness`, which is fine. `/opportunities`, `/tasks` and `/scheduling` still open by URL even though their menu permissions were removed. That is the known unguarded-routes family (`raw/tech-routes.json`).

## 4. Admin UI for roles
- By the ticket's own decision (§5) there is **no UI or API to edit a role's permissions**. Overrides are SQL only. So "edit a role for one company and check the other" can't be done by QA.
- The only role UI is Admin › Platform Users › Edit User › Roles (assignment). It opens and offers Technician, Admin, Portal Sales, Account Manager, Electrical Engineer, Project Manager, Construction Manager. I cancelled without saving (`raw/roles-picker-edit-user.png`). No role was edited, so nothing needed restoring.

## Not covered (needs a developer on QA)
- Plan rows 2/3 (new `mobile.slds.view` / `can_view_sld`): deferred by the developer.
- Plan row 4, old app builds: `field_limits` has no new keys yet, so there is nothing to test.
- **Plan row 6 and a clean grant/revoke test:** please insert one override row on QA, for example revoke `features.customers.view` from Project Manager on EG-ACME (§9 of the plan). I'll then re-run the PM menu and API on EG-ACME, plus the Demo guest. Then delete the row and check the platform default returns on the next request.
- Admin-role and non-grantable guard rails (resolver ignores them): need DB rows.
- Demo Company's own native seats (only its staff login and the TECH guest are available; the staff login is is_eg_admin, so it can't show role limits).
- Mobile: not checked. iOS ZP-5276 and Android ZP-5275 depend on this ticket; `field_limits` is unchanged for them.

## Test data / links
- App: https://acme.qa.egalvanic.ai/assets (Technician view), https://demo.qa.egalvanic.ai/ (guest view)
- API: https://acme.qa.egalvanic.ai/api/auth/v2/me · https://acme.qa.egalvanic.ai/api/features/access · https://acme.qa.egalvanic.ai/api/users/roles/company/d59d449b-09d8-45d6-8f0a-ef70024b1293 · https://demo.qa.egalvanic.ai/api/auth/v2/me (as the guest)
- Companies: EG-ACME d59d449b-09d8-45d6-8f0a-ef70024b1293 · Demo Company 93611164-13e6-47da-b2cd-a150e73173f6. Technician role e84a0fbb-0b89-4433-807c-4b7886e1cd6d.

## NEW defect
`BUG-guest-has-web-access.txt`: for a guest, `has_web_access` comes from the home company, not the acting company (Low).

## Evidence
- `QA-17Nov-ZP-5274-company-roles-video.mp4` (45 s)
- `QA-17Nov-ZP-5274-1-role-regression-table.png` (PASS), `-2-tech-eg-acme-web-app.png` (context), `-3-tech-guest-demo-web-restricted.png` (PASS), `-4-guest-has-web-access-mismatch.png` (FAIL row in red)
- Raw: `raw/cap-a.json`, `raw/cap-b.json` (all seats), `raw/guest-TECH.json`, `raw/techplat.json`, `raw/tech-routes.json`, `raw/roles-ui.json`, `raw/landing-*.png`
- ZP-5274: screenshots re-saved without text tags (17 Nov, owner rule); outlines only, PASS/FAIL wording only in the .txt captions.
