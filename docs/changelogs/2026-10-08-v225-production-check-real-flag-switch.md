# Web v2.2.5 hotfix: production check with real LaunchDarkly switches (ZP-4732, ZP-4731)

- **Date:** 2026-10-08, 11:40–12:35 UTC (17:10–18:05 IST)
- **Prompts:**
  - owner: "internal dev dhamresh sir told to test in producation so test it by enabling disabling etc in producation"
  - owner: "no need to reply in any group other than eg-internal-dev group"
  - owner: "if you need any help to enable or disable lanch darkly then feel free to post it on eg-internal-dev group by tagging shubham"
- **Environment:** PRODUCTION `https://acme.egalvanic.ai`, company Acme (`0a61e613-7887-4c10-99bf-59cedc4460f2`), build **`index-DLY0EjHj.js`** (shows "Fixes in Web v2.2.4" pop-up; v2.2.5 deployed by Krunal ~11:20 UTC).
- **Flag:** `feature-maintenance-portal`, LaunchDarkly production environment (client-side id `684b33fc2e402a092d86ca91`), org context Acme. The value was read from LaunchDarkly's own reply before every login, and a background poll watched for each change.
- **Read-only:** no production data was changed. The flag was switched by Shubham (dev) on request, for the Acme org only.

## Timeline
| UTC | Flag for Acme | Who |
|---|---|---|
| before 11:53 | **ON** (flag version 17) | as found |
| 11:54 | request posted in eg-internal-dev, tagging @Shubham Goswami: switch OFF for the Acme org only, not the whole environment | QA |
| 12:03:14 | **OFF** (flag version 18), "Disabled for Acme Prod" | Shubham |
| 12:09 | thread reply tagging Shubham: OFF check done, please switch back ON | QA |
| 12:15:43 | **ON** again (flag version 19) | Shubham |

## Results: 7 production seats
Seats: Super Admin (`+acme`, staff), Admin (`+adminprod`, staff), Project Manager (`+pmprod`), Account Manager (`+fmprod`), Electrical Engineer (`+eeprod`), Client Portal (`+cp1`), Technician + Admin (`+tech`, staff). There is no Facility Manager seat on production.

| Seat | Flag ON (v17 and again v19): portal in left menu / typed `/maintenance-portal/overview` | Flag OFF (v18): portal in menu / typed address / Site Data › Maintenance |
|---|---|---|
| Super Admin | shown / opens Site Health (9 portal pages, no SLD, no Work Orders) | hidden / "Feature Not Available" / locked |
| Admin | shown / opens Site Health | hidden / "Feature Not Available" / n/a (no Site Data, 0 sites) |
| Technician + Admin | shown (through Admin) / opens | hidden / "Feature Not Available" / locked |
| Project Manager | **hidden / "Access Denied"** | hidden / "Feature Not Available" / locked |
| Account Manager | **hidden / "Access Denied"** | hidden / "Feature Not Available" / locked |
| Electrical Engineer | **hidden / "Access Denied"** | hidden / "Feature Not Available" / locked |
| Client Portal (+cp1) | **hidden / "Access Denied"** | hidden / "Feature Not Available" / locked |

The ON-again run (v19, 12:16–12:19 UTC) gave exactly the same result as the first ON run, including the Site Data › Maintenance items unlocking again.

**LaunchDarkly unreachable (Super Admin, every launchdarkly.com request blocked in the test browser only):** the Maintenance Portal is hidden and the typed address shows "Feature Not Available", so the gate fails closed. Control in the same run, with LaunchDarkly reachable: the portal is shown.

**ZP-4732 verdict on production: the flag gate works.** With the flag OFF, nobody sees the Maintenance Portal and the typed address shows "Feature Not Available". This is the opposite of the bug, where companies with the flag off still saw it. Same behaviour as stage (comment 44974).

## ZP-4731 on production (flag ON v19, read-only)
The production CP seat cannot open the portal (see finding 2), so portal screens were checked with the Admin seat and the CP seat was used for its own menu and the server-side refusals.

| Check | Result |
|---|---|
| Portal menu | **PASS**: Site Health, Condition Assessment, Assets, Locations, Panel Schedules, Issues, Maintenance Program, Compliance, Reports. No SLD, no Work Orders |
| License selector | **PASS**: Lite, No License (no Full Access) |
| Asset page tabs | **PASS**: Basic Info, Maintenance, Inspections, Issues, Connections, Photos, Attachments. No Engineering, and `?tab=engineering` stays on Basic Info |
| Connections | **PASS**: asset "Chilr" (one lineside connection): the portal shows the row with no actions menu and no Engineering tab. Control: the same asset in Site Data has the Engineering tab and a ⋮ actions menu on the row. (The first asset tried, "hhh", had no connections, so it proved nothing.) |
| Typed `/maintenance-portal/sld` and `/maintenance-portal/work-orders` | **PASS**: "Access Denied" |
| CP menu (main app) | **PASS**: no SLD, no Work Orders; typed `/sld` and `/sessions` → "Access Denied" |
| CP API `GET /api/sld/{site}/graph` | **PASS**: 422 "Required: sld_diagrams.view" |
| CP API `POST /api/company/{id}/workorders/v2` | **PASS**: 422 "Required: company_data.view" |

## ZP-4741 on production (read-only API check): still present, as expected (known bug shipped with v2.2.5)
- The CP seat (160 assigned sites) reads issue "Repair Needed on a1" (`f02dd2f8-14c7-461c-bba2-af5654b6885d`) on site **test4** (`dcbca0a2-…`), which is not one of its sites: `GET /api/issue/{id}` → 200 with the issue, `/status-history` → 200.
- Controls: a PM assigned to test4 gets the same issue (200 JSON); the CP's own issue → 200 JSON; a random id → 200 text/html (the web page, no data).

## Findings (production only)
1. **PM, AM and EE cannot see the Maintenance Portal even with the flag ON.**
   - Their production roles do not have `maintenance_portal.manage`: PM 98 perms, AM 81, EE 84, none include it. Super Admin, Admin and Tech+Admin have it.
   - The live production menu rule (bundle `index-DLY0EjHj.js`, read to explain the screens) is: section shown when `flag ON && (has maintenance_portal.manage || tier === "T2")`.
   - On stage, ZP-4703 (owner rule 7 Oct: "visible for all roles other than technician") was fixed by granting that permission to the PM/AM/EE roles. That was a data change, so it did not travel with the code release. It is missing on production.
2. **The production Client Portal seat `+cp1` cannot be used to judge the CP experience.**
   - `/api/features/access` returns `tier: "T1"` and `account_detail: {account_id: null, license_type: null}`, and `/api/auth/v2/me` has `account_id: null`.
   - It is a bare Client Portal role, not an account contact with Portal Access (T2), so "Access Denied" is the expected result for it.
   - A real account-linked portal contact on production is needed to check the CP path. Creating one would change production data, so it was not done.
3. **The `+tech` seat is Technician + Admin.** It sees the portal through Admin, so the "not for Technician" rule cannot be checked on production (there is no pure Technician seat).

## Evidence
- `docs/bug-evidence/2026-10-08-v225-prod/prod-flag-current-*.png` (flag ON v17, 7 seats × menu + typed address)
- `docs/bug-evidence/2026-10-08-v225-prod/prod-flag-off-*.png` (flag OFF v18, 7 seats × menu + typed address)
- `docs/bug-evidence/2026-10-08-v225-prod/ld-blocked-superadmin-*.png`, `zp4731-*.png`
- Scripts (gitignored, contain seats): `.playwright-mcp/prod225/sweep-*.js`, `probe.js`, `cptier.js`, `ldblock.js`, `zp4731.js`, `zp4741.js`, `conn2.js`
- Background poll: scratchpad `poll_prod_ld.sh` (curl of the LaunchDarkly client-side evaluation for org Acme, no credentials)
