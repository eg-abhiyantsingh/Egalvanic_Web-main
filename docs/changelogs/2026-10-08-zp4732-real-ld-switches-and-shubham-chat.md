# ZP-4732 (hotfix v2.2.5): Maintenance Portal checked with REAL LaunchDarkly switches on stage, all roles PASS

- **Date:** 2026-10-08, 08:13–08:46 UTC
- **Prompts:**
  - owner: "Tag shubham say to disable maintance protol flag in lanuch darkly when we reply yes then check its not accessibe"
  - owner: "you just need to tag shubham and tell to enable maintance protol in lanuch darkly"
  - owner: "now try" (after restarting Claude Code with the Chrome extension)
- **Build:** stage `index-D-TgQbk4.js` (unchanged through every run below)
- **Flag:** `feature-maintenance-portal`, org EG-ACME `d59d449b…`, read live from LaunchDarkly's evalx reply during each login

## How the flag was switched
- Posted in Google Chat space eg-internal-dev (thread with Shubham), through the Claude in Chrome extension.
- Shubham switched the flag; a background poll of LD evalx logged each change:

| UTC | Value | LD version | Who |
|---|---|---|---|
| 08:13 | ON | v319 | (state before the request) |
| 08:35:29 | **OFF** | v320 | Shubham ("Disabled for Stage and QA") |
| 08:39:00 | ON | v321 | flipped back before FM/CP were checked |
| 08:44:01 | **OFF** | v322 | Shubham, 2nd request ("Disabled for stage") |

- After the check I replied in the thread and asked him to turn it back ON for stage and QA (stage needs it ON for ZP-4731).

## Results (every row = real flag value read at login, no simulation)
| Flag | Roles | Left rail | Typed `/maintenance-portal/overview` | Site Data › Maintenance |
|---|---|---|---|---|
| ON v319 | Admin, Super Admin, PM, AM, EE, FM, CP, reporter | Maintenance Portal shown | opens Site Health | enabled |
| ON v319 | Technician | Web Access Restricted (ZP-4703 rule) | n/a | n/a |
| OFF v320 | Admin, Super Admin, PM, AM, EE | **no** Maintenance Portal | "Feature Not Available" | locked (grey + lock icon) |
| OFF v322 | FM, CP | **no** Maintenance Portal | "Feature Not Available" | FM/CP menus have no Maintenance group |
| OFF v322 | Technician | Web Access Restricted | n/a | n/a |
| LD blocked | Admin, PM, FM, CP | hidden | "Feature Not Available" (fails closed) | locked |

Verdict: **PASS**. The portal follows the real flag for every role in both directions.

## Evidence hygiene
- 6 screenshots from the first OFF sweep (FM, CP, reporter) were taken AFTER the flag went back ON at 08:39 and show the portal.
- They were renamed `flag-back-on-0839-NOT-off-evidence-*`, so nobody reads them as an OFF failure.
- Valid OFF evidence:
  - `real-off-after-switch-{admin,superadmin,pm,am,ee}-*`
  - `real-off-2nd-window-{fm,cp}-*`

## Side observation (not filed)
- Flag OFF, Admin typing the portal address: the Site Data panel still shows the portal's **LICENSE** selector ("Lite") next to "Feature Not Available".
- Cosmetic; reported to the owner only.

## Files
- `docs/bug-evidence/2026-10-08-zp4732-stage/`
  - `real-on-*`
  - `real-off-after-switch-*`
  - `real-off-2nd-window-*`
  - `ld-blocked-*`
  - `flag-back-on-0839-NOT-off-evidence-*`
- Scripts: `.playwright-mcp/zp4697stage/mp-real-on.js`, `mp-real-off2.js`, `mp-real-off3.js`

## Outcome
- Jira comment **44974** on ZP-4732, with three screenshots.
- ZP-4732 moved In QA → **READY TO RELEASE** (transition 10). The ticket had already been put back to In QA by someone else; On Hold was not used.
- Shubham turned the flag back ON: stage v323 at 08:47:33, QA v707. The LD watch was stopped.

## ZP-4731 (still In Progress, Krunal): re-checks with the flag ON (v323), Client Portal seat `+cpstage@`
The seat is `is_eg_admin: false`, has the single role "Client Portal", 35 permissions and 7 sites.

| Check | Result |
|---|---|
| Issues tab on own site "test" | **PASS**: 3 issues. Columns: Title, Issue Class, Priority, Asset, Status, Actions. |
| Own issue detail (61810ec7 "titke ddj") | **PASS**: read-only. No edit, save or resolve controls; no editable fields. Tabs: Details, Class Details, Photos, Status History. |
| Issues list for a site NOT assigned (ead61188) | **PASS**: `POST /api/v2/issues/list` → 200, empty |
| Issue from that unassigned site opened by id (27588f70 "NEC Violation on ATS…") | **FAIL**: `GET /api/issue/{id}` → 200 JSON with full details; `/status-history` → 200; the portal page shows it (description, session name, photos, status history) |

Controls:
- Admin gets the same issue as JSON (it exists).
- A random UUID returns the HTML fallback (no data).
- The user's own issue returns JSON.

### Corrected: the earlier "foreign issue" id was not an issue
- `d295936c-…` (taken from an `open-by-site` reply by z4731b) returns the HTML fallback **for the Admin too**, so it is not a fetchable issue.
- Any "refused" verdict built on it would have been false. That screenshot is renamed `INCONCLUSIVE-d295936c-is-not-an-issue-id.png`.
- The z4731b issue-detail and foreign-issue shots ran while the flag was OFF; they are renamed `INVALID-flag-was-off-*`.

### Overlap and filing
- Closest existing bug: **ZP-4709** ("Client Portal user can see assets from sites they are not assigned to"). It covers assets, not issues.
- Not filed yet. The owner will decide: a new bug, or added to ZP-4709.

Evidence:
- `docs/bug-evidence/2026-10-08-zp4731-stage/E-cp-foreign-site-issue-opens-FAIL.png`
- `E-cp-own-issue-detail-PASS.png`

Scripts: `.playwright-mcp/zp4697stage/z4731c.js` … `z4731g.js`

## ZP-4723: Avani asked (Google Chat DM) "Can you please check if its happening in STAGE? I checked no api is being called /sld/{id} /sld/{id}/graph no view"
Re-checked on stage `index-D-TgQbk4.js`, flag ON. Seat: Client Portal `+cpstage@` (`is_eg_admin: false`, site Atest db9b8488 assigned).

**What the portal screens call:** I walked Overview, Assets, Asset detail + Connections tab, Locations, Panel Schedules and Issues. The only SLD-related calls are `/api/users/{id}/slds`, `/api/location/sld/{id}` and `/api/panels/sld/{id}`. Avani is right that no screen uses `/sld/{id}`, `/edges` or `/graph`.

**Calling them directly with the CP's own token:**

| Call | Client Portal | PM (control) |
|---|---|---|
| `GET /api/sld/{id}` | **200**, 62 KB (edges, issues, ir_sessions, ir_photos, comments, mappings) | 200 |
| `GET /api/sld/{id}/edges` | **200**, 2.3 KB | 200 |
| `GET /api/sld/{id}/graph` | 422 permission_denied | 200 |

**Verdict:** still open on stage, API side only. Blocking `/sld/{id}` and `/edges` for CP will not break the portal, because its screens do not use them.

Replied to Avani in the DM with these points. Script: `.playwright-mcp/zp4697stage/z4723.js`.

### Avani's answer (DM + Jira comment 44978): ZP-4723 "working as designed"
- `/sld/{id}` is the mobile app's site loader (`slds.view`).
- `/edges` is a shared connections endpoint (NFPA 70E tables, quote Work Units). Its rows are the same ones the CP already sees in Connections (`/connections/v2/sld/{id}`).
- The diagram itself (`/graph`, views, view-mappings, sld-view, SKM export) is blocked by `sld_diagrams.view`.

**QA cross-check of her reasoning** (site Atest, CP vs PM, same record):
- nodes 9, edges 3, photos 5 for both.
- ir_sessions, ir_photos, comments, issues, quotes and tasks are all **empty for both roles**. The endpoint does not carry work-order, issue or quote data, so the CP gets nothing beyond the assets and connections the portal already shows.
- **Conclusion: her explanation holds.** ZP-4723 was an over-report; the real requirement (no SLD diagram for CP) is met.
- No Jira change was made by QA; the closing status is for the owner to decide.

Scripts: `z4723b.js`, `z4723c.js`
