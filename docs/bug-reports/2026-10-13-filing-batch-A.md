# Filing batch A — 13 Oct 2026: re-check of 12 Oct SKM / Bulk Edit findings before filing

- **Environment:** QA only, https://acme.qa.egalvanic.ai, live build **`index-COeCBn0z.js`** (checked with curl at the start and printed by every browser run).
- **Seats:** the "attacker" is the Project Manager seat (`is_eg_admin: false`, role Project Manager, 116 sites, company acme `d59d449b…`). Staff seat (Super Admin, `is_eg_admin: true`) was used only as the positive control and to look at QA-DEMO data.
- **Runner:** `.playwright-mcp/v24/filing13/r1..r5.js` (headed Chrome through `pwslot.sh`). Raw results: `.playwright-mcp/v24/filing13/out/r*.txt`.
- **Screenshots:** `docs/bug-evidence/2026-10-13-filing-batch/<n>-*.png` (real app pixels; API rows are drawn as an overlay on the signed-in page where the call was made).
- **Data written today (all QA-DEMO-labelled, all on the QA-DEMO site "QA-DEMO ZP-4582 SKM namespace (delete me)" `cfc07c0d-71a2-40bc-bd14-76ad2372ffbd`):**
  `QA-DEMO 13Oct PM scope probe (delete me)` = node `13e0e9e3-17a2-43f8-91f0-676f688957d5`;
  `QA-DEMO 13Oct G-BUS1..3 (delete me)` = `f6577dae-b272-4aed-b952-fb5ac5955cb1`, `bba8bbe1-3b21-40a0-a5e0-aaed90e9b0cf`, `b000e9ea-9fba-48bd-aac3-b97cb9b09bd3`;
  `QA-DEMO 13Oct H-BUS1..3 (delete me)` = `f2499865-2d1f-42c7-b460-f6aeed2fb7ac`, `9a560133-a49a-4297-8fed-7e903dfb01ae`, `b6674987-335c-4c52-91e5-e59c8f10398f`.
  Nothing was written to another company. The other company's XML was **not** downloaded (only the component count in the API reply was read). Bulk Import was **not** processed.

## Summary

| # | Finding | Result today | Severity (suggested) | Assignee | Overlapping Jira |
|---|---|---|---|---|---|
| 1 | SKM export of another company's site from a customer seat | **STILL REPRODUCES** | High | Avani | none exact; same family as ZP-4709 (CP sees other sites' assets), ZP-4723 (SLD data sent despite Access Denied) |
| 2 | SKM import writes to a site the user is not assigned to | **STILL REPRODUCES (same company only)**; other-company site is refused | High | Avani | none exact; family of ZP-4709 / ZP-4741 |
| 3 | SLD-page "Export Engineering XML" ignores `feature-skm-export` OFF | **STILL REPRODUCES (SIMULATED flag)**; confirm intent with Eric | Medium (Low if by design) | Krunal | none (noted in ZP-4482 / ZP-4760 QA verdicts only) |
| 4 | Import review count + button ignore the chosen existing project | **STILL REPRODUCES** | Medium | Krunal | ZP-4582 (parent, In QA) |
| 5 | Commit without a project name makes rows that a later, unrelated project silently takes over | **STILL REPRODUCES** (API-only trigger) | Low–Medium | Avani | ZP-4582 (parent, In QA) |
| 6 | Export dialog tooltip still promises "-1, -2" suffix and "keep existing label" | **STILL REPRODUCES** | Low | Krunal | ZP-4746 (parent, In QA), ZP-4760 |
| 7 | Re-importing an unchanged Bulk Export shows changes (and would overwrite data) | **STILL REPRODUCES** — root cause sharpened | Medium | Avani (+Krunal) | ZP-4592 (parent, RTR); not a dup of ZP-4840 |
| 8 | Bulk Edit endpoints 500 on a malformed site id | **STILL REPRODUCES at API level**; no screen can send it | Low (hygiene) | Avani | same pattern as ZP-4820 / ZP-4824 |

JQL duplicate checks run (cloud egalvanic.atlassian.net): `project = ZP AND (text ~ "SKM" OR summary ~ "SKM") AND created >= -60d`; `project = ZP AND created >= -45d AND (text ~ "another company" OR "cross-tenant" OR "other company" OR "not assigned" OR "bulk-edit" OR "Bulk Import" OR "Bulk Export" OR "import-xml" OR "export-skm" OR "skm_namespace" OR "label number")`; `project = ZP AND created >= -2d`. No existing ticket covers any of the eight; related tickets are listed per item.

---

## 1. [Web] A Project Manager can download the SKM engineering file of another company's site

**Result: STILL REPRODUCES.** 13 Oct, build `index-COeCBn0z.js`.

**Steps**
1. Sign in at https://acme.qa.egalvanic.ai as the Project Manager seat (customer, not Egalvanic staff).
2. Open Engineering › SLD (https://acme.qa.egalvanic.ai/slds). The normal Export › "Export Engineering XML" calls `POST /api/sld/{site id}/export-skm` for the current site.
3. Make the same call with another company's site id `f86052b7-1683-431f-a86f-f52c6dd5be39` (company `fdcadc86…`, not Acme; not in this PM's site list).

**What happens:** the server answers 200 with a download link for that company's whole engineering model (375 components / 193 connections). The same PM opening that site normally (`GET /api/sld/f86052b7…`) is refused with 422 permission_denied. An Acme site the PM is not assigned to (`b99f9c84…`) also exports (200).

**What should happen:** refused (422/404) exactly like opening the site, for other-company sites and for sites the user is not assigned to.

**Why it matters:** a customer user can pull another customer's full electrical model (equipment names, ratings, connections).

Controls: own site → 200 (64 components); random uuid → 400 "SLD not found"; malformed id → 400; staff seat → 200 (staff, by design).

**Severity:** High · **Assignee:** Avani (backend) · **Screenshot:** `1-export-skm-other-company.png`
For the developer: `POST /api/sld/{sld_id}/export-skm` is missing the company + site-assignment check that `GET /api/sld/{id}` already has.
Related: ZP-4709 (CP sees assets of unassigned/other-company sites), ZP-4723 — same family, different endpoint.

---

## 2. [Web] SKM Import lets a Project Manager add equipment to a site they are not assigned to

**Result: STILL REPRODUCES for Acme sites outside the user's assignment. Another company's site is refused (so not cross-company).**

**Steps**
1. Sign in as the Project Manager seat. Note the site "QA-DEMO ZP-4582 SKM namespace (delete me)" (`cfc07c0d-71a2-40bc-bd14-76ad2372ffbd`) is **not** in this user's site list.
2. Upload an SKM XML to `POST /api/skm/import-xml/preview` with `sld_id=cfc07c0d…` (the same call Assets › SKM › Import (Full) › Analyze makes), then `POST /api/skm/import-xml/commit` with the returned token.
3. Sign in as an admin and open Site Data › Assets on that site, search "PM scope probe".

**What happens:** preview returns 200 ("will create 1"), commit returns 200 `created_nodes: 1`; the new asset "QA-DEMO 13Oct PM scope probe (delete me)" (`13e0e9e3-17a2-43f8-91f0-676f688957d5`) is now on that site.

**What should happen:** both calls refused for a site the user cannot open.

**Why it matters:** any PM can add (or, through re-import matching, update) equipment on any Acme site, including ones they were deliberately not given.

Controls: own site preview → 200; another company's site `f86052b7…` → refused (masked 404 HTML, no token, nothing written — tested with an empty no-op file); random uuid → refused the same way.

**Severity:** High (unauthorised write, same company) · **Assignee:** Avani · **Screenshots:** `2-skm-import-unassigned-and-other-company-site.png`, `2b-node-written-by-pm-on-unassigned-site.png`
For the developer: preview/commit check the company but not `accessible_sld_ids` / site assignment.

---

## 3. [Web] Turning off the SKM export feature hides it on Assets but not on the SLD page

**Result: STILL REPRODUCES with the flag OFF simulated** (QA rewrote the LaunchDarkly answer in the test browser; live value for Acme QA today is `feature-skm-export = true`). **Ask Eric to confirm** the flag is meant to switch off both entry points; if the SLD-page item is intentionally ungated, close as by design.

**Steps**
1. Set `feature-skm-export` OFF for Acme in LaunchDarkly (QA).
2. Sign in as the Project Manager, site "(s) Wild Goose Brewery". Open Site Data › Assets › SKM: there is no "Export" item (correct).
3. Open Engineering › SLD (https://acme.qa.egalvanic.ai/slds) › Export.

**What happens:** the SLD Export menu still lists "Export Engineering XML" and it still works.
**What should happen:** hidden on both pages when the flag is OFF.

Bundle proof (`GoJSGraphEditor-CpSz-aCj.js`, `App-B12g-5Jf.js`): the SLD menu item is `{key:"xml", …, hidden:k}` where `k = pdfOnly = !(isAdmin || nodes.manage)`; no flag check. The Assets item uses `ot("skm-export") && sld_diagrams.view`.

**Severity:** Medium (a feature switch that only half works; Low if by design) · **Assignee:** Krunal · **Screenshots:** `3a-flag-off-simulated-assets-skm-menu-no-export.png`, `3b-flag-off-simulated-sld-export-menu-still-shows-xml.png` (both labelled SIMULATED)
Real flip needed to confirm without simulation: `feature-skm-export`, env QA, company Acme, → false, then back to true.

---

## 4. [Web] SKM Import review says "Import 3 assets" when re-importing into the same project, but creates nothing

**Result: STILL REPRODUCES.**

**Steps**
1. Sign in as an admin, pick site "QA-DEMO ZP-4582 SKM namespace (delete me)", open Site Data › Assets › SKM › Import (Full).
2. Upload a study with 3 buses, name the project `QA-DEMO-13OCT-G`, import (creates 3 — correct).
3. Import the same file again and pick the existing project `QA-DEMO-13OCT-G` from the list.

**What happens:** the helper line correctly says "3 components already on this SLD from this project will be linked, not duplicated", but the tile shows "3 Assets" and the button says **"Import 3 assets"**. After clicking it: "Created 0 assets and 0 connections".
**What should happen:** after an existing project is picked, the tiles and button show what will really happen (e.g. "Link 3 existing" / "Import 0 new").

**Severity:** Medium (misleading count on a write button) · **Assignee:** Krunal · **Screenshots:** `4-reimport-same-project-says-import-3-new.png`, `4b-reimport-result-0-created.png`
For the developer: tiles/button use `will_create_nodes` from preview, computed before a project is chosen.

---

## 5. [Web] An SKM import saved without a project name is later taken over by a different project

**Result: STILL REPRODUCES.** Trigger is API-only (the screen requires a project name), so Low–Medium.

**Steps**
1. Send `POST /api/skm/import-xml/commit` **without** `skm_namespace` for a study with buses "QA-DEMO 13Oct H-BUS1..3" (SKM ids 821–823) on the QA-DEMO site → 3 assets created with no project.
2. In the app, Assets › SKM › Import (Full) a **different** study (buses "QA-DEMO 13Oct I-BUS1..3", same ids, 208 V instead of 480 V), new project name `QA-DEMO-13OCT-I`.

**What happens:** the review says "3 components already on this SLD **from this project** will be linked" (they are not from this project) and the button says "Import 3 assets"; result "Created 0 assets". The I-buses never appear; the unrelated H-buses are silently moved into project I.
**What should happen:** the server rejects a commit without a project name (the screen already requires it), so no new unnamed rows can be created.

**Severity:** Low–Medium · **Assignee:** Avani · **Screenshots:** `5-other-project-review-will-link-null-rows.png`, `5b-other-project-result-adopted.png`, `5c-assets-after-adoption.png`
Note: adopting *old* (pre-namespace) unnamed rows is the intended back-compat; the bug is only that new unnamed rows can still be created.

---

## 6. [Web] SKM Export: tooltip still promises a "-1, -2" suffix and that existing label numbers are kept

**Result: STILL REPRODUCES.**

**Steps**
1. Sign in as the Project Manager, site "(s) Wild Goose Brewery", open Site Data › Assets › SKM › Export.
2. Hover the (i) next to "Generate arc-flash label numbers from location".

**What happens:** "Each bus gets Building/Floor/Room plus -1, -2, … as its SKM arc-flash label number. Buses that already have a label number keep it; …". Today's export of the QA-DEMO site with this option ON gives the three buses that share one room the identical label `QA Bldg-North/Fl 1-2/Rm A-B` (no suffix), and bus QDA-BUS-MAIN, which came in with label "QA-PRESET-LABEL-A", is given the location label instead.
**What should happen:** tooltip matches the shipped behaviour (ZP-4760 removed the suffix): e.g. "Each bus gets Building/Floor/Room as its SKM arc-flash label number; buses with no location are left blank." Drop "keep it" unless that is fixed.

**Severity:** Low · **Assignee:** Krunal · **Screenshot:** `6-label-from-location-tooltip-promises-suffix.png`
Same text exists in the French locale (`labelFromLocationHint`).

---

## 7. [Web] Bulk Import of an unchanged Bulk Export shows 20 asset changes and would overwrite values

**Result: STILL REPRODUCES** (did NOT click Process Import).

**Steps**
1. Sign in as the Project Manager, site "(s) Wild Goose Brewery" (https://acme.qa.egalvanic.ai/assets).
2. Bulk Edit › Bulk Export (downloads `(s)_Wild_Goose_Brewery_bulk_edit.xlsx`).
3. Without opening the file, Bulk Edit › Bulk Import › upload it › skip the one ambiguous name › Review.

**What happens:** "Assets 20 to update, Connections 4 to update" — e.g. `voltage: (empty) → 240.0`, `ampere_rating: (empty) → 800`, `Catalog Number: 56 → 56.0`, `Size: 67 → 67.0`, `Serial Number: 89 → 89.0`, and **Generator-1 `voltage: 240.0 → 200.0`**. Clicking Process Import would write these.
**What should happen:** an unchanged export re-imports as "No changes".

**Why it matters:** a user who round-trips the sheet to edit one cell silently changes many others (text "56" becomes "56.0"; a generator's voltage drops from 240 to 200).

Root cause seen in the file: each asset has both a `node.voltage` / `node.ampere_rating` column (empty) and a same-meaning attribute column `Voltage`="240V" / `Ampere Rating`="800A" (Generator-1: `node.voltage`="240V", attribute `voltage`="200"); import copies the attribute into the node field. Text cells like "56" are read back as numbers (56.0).

**Severity:** Medium (data-integrity) · **Assignee:** Avani (validate/process mapping) + Krunal (export columns) · **Screenshot:** `7-unchanged-bulk-export-reimport-shows-changes.png`
Related: ZP-4592 (tenant gate on the same endpoints, RTR) — not a duplicate.

---

## 8. [Web] Bulk Edit export/validate/process return a server error (500) for a malformed site id

**Result: STILL REPRODUCES at the API level.** No screen can send a malformed id (the app always sends the current site's UUID), so this is API hygiene, not a user-facing bug. File as Low only if the owner wants these (precedent: ZP-4820, ZP-4824).

**Steps** (signed in as the Project Manager, from the browser console on any page)
1. `GET https://acme.qa.egalvanic.ai/api/bulk-edit/export?sld_id=not-a-uuid` → **500** `internal_error`.
2. `POST /api/bulk-edit/validate` and `/api/bulk-edit/process` with `sld_id=not-a-uuid` → **500** `internal_error`.

**What should happen:** 400 "Invalid SLD ID format" (what `POST /api/sld/not-a-uuid/export-skm` already returns). Empty `sld_id=` already gives a proper 400.

**Severity:** Low · **Assignee:** Avani · **Screenshot:** `8-bulk-edit-malformed-site-id.png`

---

## Housekeeping note for the main session
Yesterday's run left two copies of the **other company's** full SKM XML on disk: `.playwright-mcp/v24/skm/out/xml_foreign_pm.xml` and `xml_foreign_staff.xml` (223,776 bytes each), plus `foreign_export_fingerprint.txt`. Suggest deleting them (not deleted here because this agent did not create them).
