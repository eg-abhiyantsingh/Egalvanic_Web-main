# Bug audit edits: 15 VALID-NEEDS-EDIT tickets (13 Oct 2026)

Corrected full descriptions for the tickets the 13 Oct QA audit (`2026-10-13-bug-audit-A/B/C.md`) marked VALID-NEEDS-EDIT. **Nothing was written to Jira.**

- Machine-readable payload: `docs/bug-evidence/2026-10-13-bug-audit/edit_payload.json` (key, summary, priority, desc in Jira wiki markup, files_to_attach).
- Every `!file.png|width=720!` is an attachment already on that ticket or a file listed in files_to_attach (all in `docs/bug-evidence/2026-10-13-bug-audit/`).
- Two supporting files were copied into the audit folder: `4848-0-sld-export-menu-export-engineering-xml.png` (from 2026-10-12-v24-qa/zp-4760) and `4853-3-export-three-buses-same-label-no-suffix.png` (from .playwright-mcp/v24/skm/evidence/zp4746-xml-af-labels-qademo.png).
- Apply order per ticket: upload files_to_attach first, then set the description (wiki renderer), then summary/priority.

## Summary

| Ticket | Priority | What changed | New files to attach |
|---|---|---|---|
| ZP-4840 | High | Removed the wrong note "removing a service probably has the same limit" (remove sends one call per asset). Added the QA proof (501 ids -> 400 "at most 500...", 500 ids -> different 400) as Screenshot 3, and the remove-is-1,512-calls note. | `ZP-4840-1-PM-QA-add-assets-cap-500-mechanism.png` |
| ZP-4841 | High | Main step now uses `/api/quote/by-sld/<id>` (the call the Planning page makes); `/lightweight` kept as secondary. Named the 116,134.11 total; own-site control corrected to 17 quotes; build updated. | `ZP-4841-1-PM-new-quote-facility-Saveland-no-options.png`, `ZP-4841-2-PM-quote-by-sld-foreign-site-returns-quotes.png` |
| ZP-4842 | Low | Dropped the misleading `/api/plans` "refuses cleanly" control and the unproven "shows in error monitoring" claim. Says API only; links the same 500 on ZP-4847 / ZP-4855. | `ZP-4841-1-PM-new-quote-facility-Saveland-no-options.png`, `ZP-4842-ZP-4847-1-PM-malformed-id-500s.png` |
| ZP-4843 | Low | Priority Medium -> Low. Notes now cite ZP-4585's own QA Review (flag OFF = no Guests page) instead of ZP-4138; flag value stated as real. ACTION (not in payload): add issue link "relates to ZP-4585". | `ZP-4843-1-SuperAdmin-flag-off-admin-menu-no-Guests.png`, `ZP-4843-2-SuperAdmin-flag-off-typed-guests-page-opens.png` |
| ZP-4848 | High | Added the copy-paste console commands (POST + JSON body), the 422 control, and the full unassigned-site id b99f9c84-a4d9-40fe-8f1b-7ef9e38de3f0 ("Test site A"). Added an SLD Export menu screenshot. | `4848-0-sld-export-menu-export-engineering-xml.png`, `ZP-4848-1-PM-export-skm-foreign-site-200-with-download-link.png` |
| ZP-4849 | Medium | Says API only; verified console snippet added; untested "update" claim removed (stated as not tested); priority High -> Medium to match ZP-4865. | `A1-4849-pm-site-picker-qademo-not-offered.png`, `A2-4849-pm-import-commit-on-unassigned-site.png` |
| ZP-4850 | Medium | States the OFF state was simulated (flag is ON for Acme); asks for a real flip; framed as "please confirm intended: the SLD export existed before the flag". Suggested assignee Krunal (frontend) - not in payload. | `B4-4850-simulated-flag-off-sld-menu.png` |
| ZP-4853 | Low | Scoped to the tooltip text only; "keep existing label" moved to ZP-4746. Added export steps + export-result screenshot. Suggested assignee Krunal (frontend) - not in payload. | `B3-4853-tooltip-text.png`, `4853-3-export-three-buses-same-label-no-suffix.png` |
| ZP-4854 | Medium | Added the file proof: "56"/"67"/"89" are text cells read as numbers; Generator-1 has a separate voltage=200 attribute that overwrites 240V. Click-by-click steps. | `C1-4854-unchanged-export-reimport-review.png`, `C2-4854-generator-1-change.png` |
| ZP-4866 | Medium | Added that the CP already sees a non-clickable "Arc Flash Readiness 13%" card on Assets, so the bug is the full page + export; /equipment-designations denied as control. | `D4-4866-cp-assets-page-cards.png`, `D5-4866-cp-click-assets-arc-card.png` |
| ZP-4870 | Low | Corrected: datasets DO load (Test Equipment shows 10); only the Users source is empty. Removed the wrong static-select control and dev note. | `4870-1-web-view-users-select-empty.png`, `4870-2-control-web-view-test-equipment-loads.png` |
| ZP-4871 | Low | Replaced "created through the API" with real data: 40 of 658 forms have no key, 33 of them real (not QA-DEMO). | - |
| ZP-4876 | Medium | Real full-export steps ("Update 94 assets"), table of the 12 class changes (incl. arrester / shunt reactor / instrument transformer / motor controllers -> Load); says the commit was NOT run with the full file (proven earlier on a 4-asset cut). | `4876-1-full-export-reimport-review.png` |
| ZP-4880 | Medium | Steps now use Project Manager + site "QA-DEMO ZP-4800 SLD canvas v2.4 (delete me)" + asset QA PNL1 URL; notes that the date box has no max date; FDR-Circuit Breaker-3 kept as a Super Admin note. | `4880-3-pm-customer-seat-same-raw-error.png` |
| ZP-4882 | Low | Removed the false "user can't fix it" claim (Edit Asset has Secondary Voltage); wording-only bug. Replaced the Engineering-tab screenshot with the Edit Asset one. | `4882-1-sld-issues-inverter-rectifier-called-transformer.png`, `4882-2-inverter-edit-has-secondary-voltage.png` |

## ZP-4840

**Summary:** [Web] Work order bulk edit › Services: adding a service to more than 500 selected assets fails ("at most 500 assets per request") and replaces the page with an error  
**Priority:** High  
**Files to attach:** `ZP-4840-1-PM-QA-add-assets-cap-500-mechanism.png`  
**What changed:** Removed the wrong note "removing a service probably has the same limit" (remove sends one call per asset). Added the QA proof (501 ids -> 400 "at most 500...", 500 ids -> different 400) as Screenshot 3, and the remove-is-1,512-calls note.

```
h3. What happens
On a work order with more than 500 assets, adding a service to all assets with *Edit* (bulk edit) › *Services* fails. The whole work order page is replaced by the error text, with only a *Back to Work Orders* button. The service is not added.

h3. Steps
1. Sign in to https://basselectric.egalvanic.ai (production, company Bass Electric) and open *Work Orders* › work order *1029* (D802 - 200 Paul Ave, 1,512 assets): https://basselectric.egalvanic.ai/sessions/a898ad5e-e3dc-48a5-b0f3-7ab64233c9b7
2. On the *Assets* tab click *Edit* (bulk edit), tick the header checkbox, then click *Select all 1512*.
3. Click *Services*. The "Manage Services — 1512 assets" dialog opens.
4. Under *Add a service* choose *Cleaning* and click *Add*. (Screenshot 1)
!1-manage-services-1512-assets-add-cleaning.png|width=720!
5. The work order screen is replaced by: "Failed to add service: API call failed: 400 - ... at most 500 assets per request ..." with only a *Back to Work Orders* button. (Screenshot 2, and the attached video bug_service_bulk.mp4)
!2-failed-to-add-service-at-most-500-assets.png|width=720!
6. QA proof of the cause, on QA with no data changed (Screenshot 3): on work order "QA-DEMO ZP-4801 Tuned column (delete me)" (https://acme.qa.egalvanic.ai/sessions/22a22b9c-4eda-43ed-b131-4b7146912441), signed in as the Project Manager, QA sent the same add-assets call with random asset ids and a random service id (so nothing can match):
||Request||Answer||
|add-assets with 501 asset ids|400 "at most 500 assets per request"|
|add-assets with 500 asset ids (control)|400 "no addable assets (unknown, deleted, or on another site)"|
!ZP-4840-1-PM-QA-add-assets-cap-500-mechanism.png|width=720!

h3. What should happen
* The service is added to all 1,512 selected assets: the web app sends the assets in batches of up to 500 (the server accepts at most 500 per request), with progress if it takes a while.
* If something does fail, a short message shows in the dialog or as a toast, and the work order page stays open.

h3. Notes
* Seen on production (web V2.3), company Bass Electric, recorded 13 Oct 2026. Same code on QA web v2.4 (QA build index-C14cwR0U.js, re-checked 13 Oct 2026).
* The server limit (500 assets per request) is working as designed; the web app does not respect it when *Select all* picks more than 500 assets.
* Removing a service from all assets in the same dialog does NOT hit this limit (it sends one request per asset), but with 1,512 assets that is 1,512 calls in a row; worth batching in the same fix.
* For the developer: the bulk *Manage Services › Add* handler sends ALL selected ids in ONE {{POST /api/ir_session/<work order id>/add-assets}} call, and on failure sets the page-level error, which replaces the whole work order view.
```

## ZP-4841

**Summary:** [Web] Any customer user can read another company's quotes by asking for quotes of that company's site  
**Priority:** High  
**Files to attach:** `ZP-4841-1-PM-new-quote-facility-Saveland-no-options.png`, `ZP-4841-2-PM-quote-by-sld-foreign-site-returns-quotes.png`  
**What changed:** Main step now uses `/api/quote/by-sld/<id>` (the call the Planning page makes); `/lightweight` kept as secondary. Named the 116,134.11 total; own-site control corrected to 17 quotes; build updated.

```
h3. What happens
A customer user (not Egalvanic staff) can read another company's quotes, with prices and scope of work. The app never offers that company's site to them, but the server answers {{GET /api/quote/by-sld/<site id>}} (the call the *Planning* page makes) for any site id, including other companies' sites.

h3. Steps
1. Sign in to https://acme.qa.egalvanic.ai as the Project Manager test seat abhiyant.singh+project@egalvanic.com (a customer user, is_eg_admin = false).
2. Click *Quotes* in the left menu (https://acme.qa.egalvanic.ai/opportunities) › *New Quote* › type {{Saveland}} in *Facility*. It says *No options*: site "Saveland Avenue Lofts" belongs to another company. Control: typing {{Richmond}} lists "Richmond, CA". (Screenshot 1)
!ZP-4841-1-PM-new-quote-facility-Saveland-no-options.png|width=720!
3. On the same page open the browser console (F12) and run (this is the call the *Planning* page makes):
{noformat}
fetch('/api/quote/by-sld/f86052b7-1683-431f-a86f-f52c6dd5be39').then(r=>r.json()).then(console.log)
{noformat}
4. Result: 200 with that company's 6 quotes, including title, status, description, scope of work ({{sow}}), contacts, site and opportunity names, hours and totals (one quote, "Transformer-Repair", status accepted, shows {{calculated_total}} 116,134.11). The lighter call {{GET /api/quote/by-sld/f86052b7-1683-431f-a86f-f52c6dd5be39/lightweight}} returns the same 6 quotes. Same result for the Account Manager, Electrical Engineer and Facility Manager seats (all customer users, none assigned to that site). (Screenshot 2)
!ZP-4841-2-PM-quote-by-sld-foreign-site-returns-quotes.png|width=720!
5. Controls, same user and console:
{noformat}
fetch('/api/sld/f86052b7-1683-431f-a86f-f52c6dd5be39').then(r=>console.log(r.status))              // 422 - opening the site is refused
fetch('/api/opportunity/by-sld/f86052b7-1683-431f-a86f-f52c6dd5be39').then(r=>console.log(r.status)) // 422
fetch('/api/quote/by-sld/aadcee4c-7dd0-45b3-81b9-309c5c166084').then(r=>r.json()).then(console.log) // own site - 17 quotes (correct)
{noformat}
6. First proof from earlier the same day (build index-COeCBn0z.js), same 6 quotes. (Screenshot 3)
!b1-2-pm-quote-by-sld-other-company-proof.png|width=720!


h3. What should happen
422 "permission denied", the same answer the server already gives this user for {{GET /api/sld/f86052b7-…}} and {{GET /api/opportunity/by-sld/f86052b7-…}}.

h3. Notes
* QA build index-C14cwR0U.js, re-checked 13 Oct 2026. Controls: own site → its own 17 quotes; a random id → 0 quotes; {{sld/<id>}} and {{opportunity/by-sld/<id>}} → 422.
* Impact: one customer can read another customer's prices and scope of work just by knowing a site id.
* Same family as ZP-4709 ({{nodes/sld}}, library-designations), but this endpoint is not covered there.
* For the developer: {{quote/by-sld/<id>}} and {{/lightweight}} lack the site-access check ({{@require_sld_access}}, which ZP-4585 extended with a company check) that {{sld/<id>}} and {{opportunity/by-sld/<id>}} already use.
```

## ZP-4842

**Summary:** [Web] Creating a quote with a malformed site id returns a server error (500) instead of "invalid site" (API only)  
**Priority:** Low  
**Files to attach:** `ZP-4841-1-PM-new-quote-facility-Saveland-no-options.png`, `ZP-4842-ZP-4847-1-PM-malformed-id-500s.png`  
**What changed:** Dropped the misleading `/api/plans` "refuses cleanly" control and the unproven "shows in error monitoring" claim. Says API only; links the same 500 on ZP-4847 / ZP-4855.

```
h3. What happens
{{POST /api/quote/create-standard}} with a malformed {{sld_id}} (not a valid id) answers 500 {{internal_error}} instead of a clear 400. Nothing is created.
This is API only: no screen can send a malformed id, because the *New Quote* dialog only lets the user pick a site from the list.

h3. Steps
1. Sign in to https://acme.qa.egalvanic.ai as the Project Manager abhiyant.singh+project@egalvanic.com and click *Quotes* (https://acme.qa.egalvanic.ai/opportunities). In *New Quote* the *Facility* box only offers real sites from the list (here: typing another company's site name gives "No options"), so the screen cannot send a bad id. (Screenshot 1)
!ZP-4841-1-PM-new-quote-facility-Saveland-no-options.png|width=720!
2. Open the browser console (F12) on the same page and run:
{noformat}
fetch('/api/quote/create-standard',{method:'POST',headers:{'Content-Type':'application/json','X-Direct-Write':'true'},body:JSON.stringify({sld_id:'not-a-uuid',title:'QA-DEMO probe (delete me)'})}).then(r=>console.log(r.status))
{noformat}
3. Repeat with {{sld_id:'12345'}}.
4. Both answer 500 with "error": "internal_error" and a trace id. (Screenshot 2: first run; Screenshot 3: re-check on the newer build, which also shows the same kind of 500 on {{GET /api/opportunity/<bad id>}}, ZP-4847)
!b2-1-create-standard-malformed-id-500.png|width=720!
!ZP-4842-ZP-4847-1-PM-malformed-id-500s.png|width=720!

h3. What should happen
400 "invalid site id" (or 422), the same as a well-formed id the user cannot access.

h3. Notes
* QA build index-C14cwR0U.js, re-checked 13 Oct 2026. Nothing was created by these calls.
* API only: the Create Quote dialog only sends a site picked from the list, so no user sees this. Low.
* The same malformed-id 500 also happens on {{GET /api/opportunity/<id>}} (ZP-4847) and the Bulk Edit endpoints (ZP-4855). Same class as ZP-4820 / ZP-4824 (already fixed).
* For the developer: one shared UUID check on incoming ids would fix all of these at once.
```

## ZP-4843

**Summary:** [Web] With guest access turned off, typing /guests still opens the Guests admin page with an "Invite guest" button  
**Priority:** Low  
**Files to attach:** `ZP-4843-1-SuperAdmin-flag-off-admin-menu-no-Guests.png`, `ZP-4843-2-SuperAdmin-flag-off-typed-guests-page-opens.png`  
**What changed:** Priority Medium -> Low. Notes now cite ZP-4585's own QA Review (flag OFF = no Guests page) instead of ZP-4138; flag value stated as real. ACTION (not in payload): add issue link "relates to ZP-4585".

```
h3. What happens
With the guest-access feature turned OFF (LaunchDarkly flag {{feature-guest-access}} = false, the real value for Acme on QA today), the Admin menu correctly has no *Guests* item. But typing https://acme.qa.egalvanic.ai/guests in the address bar still opens the *Guests* page with an *Invite guest* button.

h3. Steps
1. Sign in to https://acme.qa.egalvanic.ai as the Super Admin abhiyant.singh+admin@egalvanic.com.
2. Click *Admin* in the left rail. Under *People* there are only *Platform Users* and *Guest Portal Users*. No *Guests* item (correct). (Screenshot 1)
!b3-1-superadmin-flag-off-admin-menu-no-guests.png|width=720!
3. Type https://acme.qa.egalvanic.ai/guests in the address bar and press Enter.
4. The *Guests* page opens with *Invite guest*, a search box and an empty table ("No rows"). (Screenshot 2)
!b3-2-superadmin-flag-off-typed-guests-page-renders.png|width=720!
5. Re-check on the newer build, same result: menu without Guests (Screenshot 3), typed /guests opens the page (Screenshot 4).
!ZP-4843-1-SuperAdmin-flag-off-admin-menu-no-Guests.png|width=720!
!ZP-4843-2-SuperAdmin-flag-off-typed-guests-page-opens.png|width=720!

h3. What should happen
While guest access is off, {{/guests}} shows the normal "Feature Not Available" page (like other flag-gated pages such as /reporting), matching the hidden menu item.

h3. Notes
* QA build index-C14cwR0U.js, re-checked 13 Oct 2026. The flag value is the real one (not simulated): {{feature-guest-access}} = false for Acme on QA.
* Parent feature: ZP-4585 (cross-company guest access). Its own QA Review says: "Flag off: … confirm there is no … Admin > People > Guests page".
* The guest APIs behind the page are refused, so no data shows and nothing can be saved (QA did not click *Invite guest*). Impact is a dead page, not a data leak. Low.
* For the developer: the menu item checks the flag ({{guest-access}} + {{users.manage}}), but the {{/guests}} route only checks {{features.settings.users.view}}. Wrap the route in the same feature gate that /reporting and /issue-suggestions use.
```

## ZP-4848

**Summary:** [Web] A customer user can download the SKM engineering file of another company's site  
**Priority:** High  
**Files to attach:** `4848-0-sld-export-menu-export-engineering-xml.png`, `ZP-4848-1-PM-export-skm-foreign-site-200-with-download-link.png`  
**What changed:** Added the copy-paste console commands (POST + JSON body), the 422 control, and the full unassigned-site id b99f9c84-a4d9-40fe-8f1b-7ef9e38de3f0 ("Test site A"). Added an SLD Export menu screenshot.

```
h3. What happens
A customer user (not Egalvanic staff) gets a download link for another company's whole engineering model from {{POST /api/sld/<site id>/export-skm}}. The same user is refused when they try to open that site. The same happens for a site of their own company that they are not assigned to.
This needs a call from the browser console: the app never offers these sites to the user.

h3. Steps
1. Sign in to https://acme.qa.egalvanic.ai as the Project Manager test seat abhiyant.singh+project@egalvanic.com (a customer user, is_eg_admin = false).
2. Open the SLD page (https://acme.qa.egalvanic.ai/slds) and click *Export*. *Export Engineering XML* is the button that calls {{POST /api/sld/<site id>/export-skm}} for the current site. (Screenshot 1, menu shown on site "Richmond, CA")
!4848-0-sld-export-menu-export-engineering-xml.png|width=720!
3. Open the browser console (F12) on the same page and run this for another company's site "Saveland Avenue Lofts" (not in this user's site list):
{noformat}
fetch('/api/sld/f86052b7-1683-431f-a86f-f52c6dd5be39/export-skm',{method:'POST',headers:{'Content-Type':'application/json'},body:'{}'}).then(r=>r.json()).then(console.log)
{noformat}
4. Result: 200, success: true, with a presigned_url (a download link) and component_count 375, connection_count 193. (Screenshot 2: first run; Screenshot 3: re-check)
!1-export-skm-other-company.png|width=720!
!ZP-4848-1-PM-export-skm-foreign-site-200-with-download-link.png|width=720!
5. Control, same user: opening that site is refused:
{noformat}
fetch('/api/sld/f86052b7-1683-431f-a86f-f52c6dd5be39').then(r=>console.log(r.status))   // 422 permission_denied
{noformat}
6. Same check with an Acme site the user is NOT assigned to, "Test site A" (b99f9c84-a4d9-40fe-8f1b-7ef9e38de3f0):
{noformat}
fetch('/api/sld/b99f9c84-a4d9-40fe-8f1b-7ef9e38de3f0').then(r=>console.log(r.status))   // 422 - read refused
fetch('/api/sld/b99f9c84-a4d9-40fe-8f1b-7ef9e38de3f0/export-skm',{method:'POST',headers:{'Content-Type':'application/json'},body:'{}'}).then(r=>r.json()).then(console.log)   // 200 with presigned_url
{noformat}

h3. What should happen
The export is refused (422, the same as opening the site) for other companies' sites and for sites the user is not assigned to.

h3. Notes
* QA build index-C14cwR0U.js, re-checked 13 Oct 2026. Controls: own site (aadcee4c-7dd0-45b3-81b9-309c5c166084) → 200 (correct); a random id → 400 "SLD not found".
* Impact: a customer user can pull another customer's full electrical model (equipment names, ratings, connections).
* Same family as ZP-4709 / ZP-4723, different endpoint.
* For the developer: {{export-skm}} is missing the company and site-assignment check that {{GET /api/sld/<id>}} already has.
```

## ZP-4849

**Summary:** [Web] SKM Import lets a Project Manager add equipment to a site they are not assigned to (API only)  
**Priority:** Medium  
**Files to attach:** `A1-4849-pm-site-picker-qademo-not-offered.png`, `A2-4849-pm-import-commit-on-unassigned-site.png`  
**What changed:** Says API only; verified console snippet added; untested "update" claim removed (stated as not tested); priority High -> Medium to match ZP-4865.

```
h3. What happens
A Project Manager who is *not* assigned to a site cannot open that site in the app, and the server refuses to show them its diagram (422). But the SKM import calls ({{/api/skm/import-xml/preview}} and {{/commit}}) still accept that site and create equipment on it.
This is API only: the site is not offered in the site picker, so it can only be done by calling the API from the browser console. Same kind of gap as ZP-4865 (SLD edits), different endpoints.

h3. Steps
1. Sign in to https://acme.qa.egalvanic.ai as the Project Manager test user abhiyant.singh+project@egalvanic.com (a customer user, is_eg_admin = false, 116 assigned sites).
2. Open the *Current site* box and type {{QA-DEMO ZP-4582}}. The app says "No sites available": site "QA-DEMO ZP-4582 SKM namespace (delete me)" is not assigned to this user. (Screenshot 1)
!A1-4849-pm-site-picker-qademo-not-offered.png|width=720!
3. Open the browser console (F12) on any app page and paste:
{noformat}
const SITE = 'cfc07c0d-71a2-40bc-bd14-76ad2372ffbd'; // "QA-DEMO ZP-4582 SKM namespace (delete me)"
const xml = `<?xml version="1.0" encoding="UTF-8"?><s:SKM xmlns:s="http://www.skm.com/schemas/powersystem" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"><s:PowerSystem><s:Components><s:Bus s:action="create" s:id="961" s:name="QA-DEMO PM scope test (delete me)" s:altId="" s:altName="" xsi:ID="skm961"><s:Field s:id="37552142" s:name="NodeBus" s:value="False"/></s:Bus></s:Components><s:Topology></s:Topology></s:PowerSystem></s:SKM>`;
console.log('read', (await fetch(`/api/sld/${SITE}/graph`)).status);           // 422 - read refused (correct)
const fd = new FormData(); fd.append('xml_file', new File([xml], 'probe.xml', {type: 'text/xml'})); fd.append('sld_id', SITE);
const pv = await (await fetch('/api/skm/import-xml/preview', {method: 'POST', body: fd})).json();
console.log('preview', pv.summary.will_create_nodes);                           // 1 - should be refused
const cm = await fetch('/api/skm/import-xml/commit', {method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({token: pv.token, sld_id: SITE, skm_namespace: 'QA-DEMO-PM-SCOPE-TEST'})});
console.log('commit', cm.status, await cm.json());                             // 200, created_nodes: 1 - should be refused
{noformat}
4. Result: read 422, preview 200 ("will create 1"), commit 200 with created_nodes: 1. (Screenshot 2: re-check table; Screenshot 3: first run)
!A2-4849-pm-import-commit-on-unassigned-site.png|width=720!
!2-skm-import-unassigned-and-other-company-site.png|width=720!
5. Sign in as an admin, pick site "QA-DEMO ZP-4582 SKM namespace (delete me)", open *Site Data › Assets* and search {{PM scope}}: the new bus is on the site. (Screenshot 4, from the first run: "QA-DEMO 13Oct PM scope probe (delete me)")
!2b-node-written-by-pm-on-unassigned-site.png|width=720!

h3. What should happen
Preview and commit are refused (422 permission_denied) for a site that is not in the user's assigned sites, exactly like the diagram read.

h3. Notes
* QA build index-C14cwR0U.js, re-checked 13 Oct 2026. Running step 3 creates one clearly labelled QA-DEMO bus on the QA-DEMO site. Earlier proofs: "QA-DEMO 13Oct PM scope probe (delete me)" (13e0e9e3-17a2-43f8-91f0-676f688957d5) and "QA-DEMO 13Oct audit-B PM probe (delete me)" (af69c4f3-cb0c-487b-8815-3c6464ab496c).
* Controls: another company's site is refused (so this is inside one company only); the user's own site works (200).
* Not tested: whether re-import matching can also UPDATE existing equipment on such a site. This ticket only claims "create".
* The Project Manager has data.import; the missing check is the site assignment, not the permission.
* For the developer: {{/api/skm/import-xml/preview}} and {{/commit}} check the company but not caller_can_reach_sld(sld_id); please cover them in the same pass as ZP-4865 and the ZP-4709 follow-up.
```

## ZP-4850

**Summary:** [Web] SKM export flag OFF hides Export on Assets but not "Export Engineering XML" on the SLD page (please confirm intended)  
**Priority:** Medium  
**Files to attach:** `B4-4850-simulated-flag-off-sld-menu.png`  
**What changed:** States the OFF state was simulated (flag is ON for Acme); asks for a real flip; framed as "please confirm intended: the SLD export existed before the flag". Suggested assignee Krunal (frontend) - not in payload.

```
h3. What happens
The LaunchDarkly flag {{feature-skm-export}} controls the *Export* item under *Site Data › Assets › SKM*. The same SKM file can also be exported from the SLD page (*Export › Export Engineering XML*), and that item does not look at the flag. So turning the flag OFF does not fully turn SKM export off.
Important: the flag is ON for Acme on QA, so QA *simulated* the OFF state in its own test browser only. A real flip is requested to confirm (see Notes). The SLD-page export existed before this flag was added, so this may be intended; please confirm with product.

h3. Steps
1. Turn {{feature-skm-export}} OFF for Acme in LaunchDarkly (QA). (QA simulated this in its own browser by rewriting the flag answer, because the real value is ON; a real flip is requested.)
2. Sign in to https://acme.qa.egalvanic.ai as the Project Manager abhiyant.singh+project@egalvanic.com, site "(s) Wild Goose Brewery".
3. Open *Site Data › Assets* (https://acme.qa.egalvanic.ai/assets) › *SKM*: the menu shows Import (Full), Import (XML to Excel), Settings Verifier. No *Export* (correct). (Screenshot 1, simulated OFF)
!3a-flag-off-simulated-assets-skm-menu-no-export.png|width=720!
4. Open the SLD page (https://acme.qa.egalvanic.ai/slds) and click *Export*. (Screenshot 2, simulated OFF)
!3b-flag-off-simulated-sld-export-menu-still-shows-xml.png|width=720!
5. The SLD Export menu still lists *Export Engineering XML*, and it still downloads the SKM file. Re-check on the newer build, same result. (Screenshot 3, labelled SIMULATED)
!B4-4850-simulated-flag-off-sld-menu.png|width=720!

h3. What should happen
Please confirm with product:
* If the flag is meant to switch SKM export off everywhere, the SLD-page *Export Engineering XML* item is hidden too while the flag is OFF.
* If the SLD-page item is meant to stay (it existed before the flag), close this ticket as by design.

h3. Notes
* QA build index-C14cwR0U.js, re-checked 13 Oct 2026. The OFF state was *simulated* (LaunchDarkly answer rewritten in the test browser only). Real QA values for Acme: {{feature-skm-export}} = true.
* Real flip needed to confirm: {{feature-skm-export}} → OFF, QA, company Acme, for a few minutes, check both menus, then back ON.
* The SLD-page XML export was already there before this flag (QA used it on 3 Sep 2026 while testing ZP-3836). Raised first as a gap on ZP-4482.
* For the developer: the SLD export menu item reads no flag (it is only hidden in PDF-only mode); the Assets SKM item reads {{skm-export}}. If a fix is wanted, it is frontend only.
```

## ZP-4853

**Summary:** [Web] SKM Export: the "label numbers from location" help text still promises a "-1, -2" suffix that the export no longer adds  
**Priority:** Low  
**Files to attach:** `B3-4853-tooltip-text.png`, `4853-3-export-three-buses-same-label-no-suffix.png`  
**What changed:** Scoped to the tooltip text only; "keep existing label" moved to ZP-4746. Added export steps + export-result screenshot. Suggested assignee Krunal (frontend) - not in payload.

```
h3. What happens
The help text next to "Generate arc-flash label numbers from location" (SKM Export dialog) still describes the old numbering. Since ZP-4760 the export writes the location label exactly as it is, with no {{-1, -2}} suffix, so the help text is wrong.

h3. Steps
1. Sign in to https://acme.qa.egalvanic.ai as the Project Manager abhiyant.singh+project@egalvanic.com, site "(s) Wild Goose Brewery".
2. Open *Site Data › Assets* (https://acme.qa.egalvanic.ai/assets) › *SKM* › *Export*. The "Export to SKM XML" dialog opens with the option "Generate arc-flash label numbers from location" and an (i) icon next to it. (Screenshot 1)
!B3-4853-tooltip-text.png|width=720!
3. Hover the (i) icon. The text says: "Each bus gets Building/Floor/Room plus -1, -2, … as its SKM arc-flash label number. Buses that already have a label number keep it; buses with no location are left blank." (Screenshot 2)
!6-label-from-location-tooltip-promises-suffix.png|width=720!
4. To see what the export really does: sign in as a Super Admin, pick site "QA-DEMO ZP-4582 SKM namespace (delete me)", open *Site Data › Assets › SKM › Export*, tick "Generate arc-flash label numbers from location", click *Export* and open the downloaded XML. Search for {{AF_LabelNumber}}.
5. The three buses in the same room (QDA-BUS-MAIN, QDA-BUS-LOAD, QDB-BUS-MAIN) all get the same label {{QA Bldg-North/Fl 1-2/Rm A-B}}, with no {{-1, -2}} suffix. (Screenshot 3)
!4853-3-export-three-buses-same-label-no-suffix.png|width=720!

h3. What should happen
The help text matches the export, for example: "Each bus gets its Building/Floor/Room as its SKM arc-flash label number; buses with no location are left blank." Same change in the French text.

h3. Notes
* QA build index-C14cwR0U.js, re-checked 13 Oct 2026 (export re-checked through the same export call on the QA-DEMO site; Screenshot 3 is QA's evidence page from 12 Oct, build index-BdXGlAq5.js, with the same result).
* This ticket is only about the wrong text. The sentence "Buses that already have a label number keep it" is a requirement of ZP-4746 and is tracked there; keep or drop that sentence depending on the ZP-4746 outcome.
* Test data: site "QA-DEMO ZP-4582 SKM namespace (delete me)" (cfc07c0d-71a2-40bc-bd14-76ad2372ffbd).
* For the developer: frontend text only, key {{labelFromLocationHint}} (EN + FR).
```

## ZP-4854

**Summary:** [Web] Bulk Import of an unchanged Bulk Export shows 20 asset changes and would overwrite values  
**Priority:** Medium  
**Files to attach:** `C1-4854-unchanged-export-reimport-review.png`, `C2-4854-generator-1-change.png`  
**What changed:** Added the file proof: "56"/"67"/"89" are text cells read as numbers; Generator-1 has a separate voltage=200 attribute that overwrites 240V. Click-by-click steps.

```
h3. What happens
Re-importing a Bulk Export file without editing it shows many changes on the *Review* step: 20 assets and 4 connections "to update". Clicking *Process Import* would write them, for example lowering a generator's voltage from 240 to 200.

h3. Steps
1. Sign in to https://acme.qa.egalvanic.ai as the Project Manager abhiyant.singh+project@egalvanic.com, site "(s) Wild Goose Brewery", and open *Site Data › Assets* (https://acme.qa.egalvanic.ai/assets).
2. Click *Bulk Edit › Bulk Export*. The file {{(s)_Wild_Goose_Brewery_bulk_edit.xlsx}} downloads.
3. Do not open the file. Click *Bulk Edit › Bulk Import* and upload that same file.
4. On *Resolve Conflicts*, skip the one ambiguous name ("Panelboard-1" matches 2 assets), then go to *Review*.
5. The Review says *Assets: 20 to update*, *Connections: 4 to update*. (Screenshot 1: first run; Screenshot 2: re-check)
!7-unchanged-bulk-export-reimport-shows-changes.png|width=720!
!C1-4854-unchanged-export-reimport-review.png|width=720!
6. Scroll the list. Examples: asset "abhiy ant": voltage (empty) → 240.0, ampere_rating (empty) → 800, Catalog Number 56 → 56.0, Size 67 → 67.0, Serial Number 89 → 89.0. *Generator-1: voltage 240.0 → 200.0*. Connections: Length (ft) 89 → 89.0. (Screenshot 3)
!C2-4854-generator-1-change.png|width=720!
7. Do NOT click *Process Import* (QA did not).

h3. What should happen
An unchanged export re-imports as "No changes".

h3. Notes
* QA build index-C14cwR0U.js, re-checked 13 Oct 2026: same 20 / 4 changes. Process Import was *not* clicked.
* Proof from the untouched file: Catalog Number, Size and Serial Number are text cells ("56", "67", "89"), so "56 → 56.0" comes from the import reading text as a number. Text fields such as Serial Number would really change on Process Import.
* Generator-1 row: node.voltage = "240V" and a separate attribute column "voltage" = "200". The import lets the attribute overwrite the node value, so an untouched sheet lowers the generator's voltage from 240 to 200. The problem is the round trip, not the data: the user did not change anything.
* Most other rows: node voltage is empty and the attribute "Voltage" = "240V", so the import fills node voltage with 240.0. Even if that sync is wanted, it should not show as a change for a file the user did not edit.
* Related to ZP-4592 (same feature), not a duplicate.
* For the developer: compare each cell to the exported value as text before treating it as a change, and do not let attribute columns overwrite node fields on import.
```

## ZP-4866

**Summary:** [Web] A Client Portal user can open the Arc Flash Readiness page by typing /arc-flash  
**Priority:** Medium  
**Files to attach:** `D4-4866-cp-assets-page-cards.png`, `D5-4866-cp-click-assets-arc-card.png`  
**What changed:** Added that the CP already sees a non-clickable "Arc Flash Readiness 13%" card on Assets, so the bug is the full page + export; /equipment-designations denied as control.

```
h3. What happens
A Client Portal user has no Engineering menu, so no "Arc Flash Readiness" link. But if they type the address /arc-flash, the full Arc Flash Readiness page opens with the site's data, the per-class breakdown, the building filter, Engineering Mode and an "Export readiness report" button.

h3. Steps
1. Sign in to https://acme.qa.egalvanic.ai as the Client Portal user abhiyant.singh+clientportal@egalvanic.com (is_eg_admin = false), site "Android Site 2". The left menu has only "Site Data" and "Maintenance Portal": no Engineering section and no "Arc Flash Readiness" link. (Screenshot 1)
!2-1-cp-menu-has-no-engineering.png|width=720!
2. Control, same user: type https://acme.qa.egalvanic.ai/slds (another Engineering page) in the address bar. It is correctly refused with "Access Denied". (Screenshot 2)
!2-2-control-cp-typed-slds-access-denied.png|width=720!
3. Now type https://acme.qa.egalvanic.ai/arc-flash in the address bar. The full Arc Flash Readiness page opens: the gauges, the per-class cards with real numbers, the building filter, Engineering Mode and the "Export readiness report" button. (Screenshot 3)
!2-3-cp-typed-arc-flash-page-opens.png|width=720!
4. Click "Export readiness report": the report window opens for the Client Portal user too (not submitted in this test). (Screenshot 4)
!2-4-cp-export-readiness-report-window.png|width=720!
5. Control: a Super Admin has an Engineering menu with "Arc Flash Readiness"; the page is meant for roles that have it, which the Client Portal does not. (Screenshot 5)
!2-5-control-super-admin-menu-has-arc-flash.png|width=720!
6. For context: on *Site Data › Assets* the Client Portal user already sees the headline card "Arc Flash Readiness 13% — 261 of 2048 fields • 4 unclassified" (Screenshot 6). Clicking that card does not open the page for this user (Screenshot 7). So the headline number is meant to be visible; the bug is the full page and its export.
!D4-4866-cp-assets-page-cards.png|width=720!
!D5-4866-cp-click-assets-arc-card.png|width=720!

h3. What should happen
Typing /arc-flash is refused for a Client Portal user (Access Denied or a redirect), the same as typing /slds and /equipment-designations. The Client Portal role does not have the arc-flash view permission.

h3. Notes
* QA build index-C14cwR0U.js, re-checked 13 Oct 2026: still reproduces. /equipment-designations is correctly denied for the same user, so /arc-flash is the odd one out.
* This ticket is about the full page (per-class breakdown, Engineering Mode, building filter, "Export readiness report"), which needs {{features.arc_flash.view}}; the Client Portal role does not have it. The headline card on Assets is not the problem.
* While the page is open as the Client Portal user, {{/api/lookup/nodes/<site>}}, {{/api/sld/<site>/edges}} and {{/api/sld/<site>/library-designations}} all answer 200.
* Related: ZP-4699 (removed Engineering surfaces from the Client Portal); it did not cover the /arc-flash route.
* For the developer: /arc-flash is also in the layout's direct-render list (pathname check), so the route's permission check ({{features.arc_flash.view}}) is skipped; /slds is guarded correctly.
```

## ZP-4870

**Summary:** [Web] EG form builder: Web View preview shows no options for fields that use the company "Users" list (Test Equipment fields work)  
**Priority:** Low  
**Files to attach:** `4870-1-web-view-users-select-empty.png`, `4870-2-control-web-view-test-equipment-loads.png`  
**What changed:** Corrected: datasets DO load (Test Equipment shows 10); only the Users source is empty. Removed the wrong static-select control and dev note.

```
h3. What happens
In the form builder, *Web View* preview, a field that takes its options from the company *Users* list shows no options ("Select..." only; the multi-pick shows 0). A *Test Equipment* dataset field in the same preview lists its 10 items, so the preview does load datasets. Only the Users source is missing.

h3. Steps
1. Sign in to https://acme.qa.egalvanic.ai as a Super Admin (abhiyant.singh+admin@egalvanic.com). Go to *Builder › Forms* (https://acme.qa.egalvanic.ai/eg-forms), search "QA-DEMO ZP-4751" and open "QA-DEMO ZP-4751 ZP-4753 dataset type-to-search (delete me)". (Screenshot 1)
!6-1-eg-forms-list-test-form.png|width=720!
2. Click the pencil *Edit Form*, then *Web View*. (Screenshot 2)
!6-2-builder-web-view-preview.png|width=720!
3. Open *S1 Users select (dataset, 246 rows)*: only "Select..." is listed. Open *M1 Users multiselect (dataset)*: 0 options. (Screenshot 3)
!4870-1-web-view-users-select-empty.png|width=720!
4. Control, same preview: open *S2 Test equipment select (dataset)*. It lists the company's 10 test-equipment items ("test (3445)", "DLRO (56654)", …). (Screenshot 4)
!4870-2-control-web-view-test-equipment-loads.png|width=720!
5. Control, real form: the same S1 field inside work order "QA-DEMO ZP-4801 Tuned column (delete me)" (https://acme.qa.egalvanic.ai/sessions/22a22b9c-4eda-43ed-b131-4b7146912441) › More › Forms › 13N-H1-1 lists all 246 users with a "Type to search" box. (Screenshot 5)
!6-4-control-wo-form-same-field-lists-users.png|width=720!

h3. What should happen
Users-list fields show the same options in Web View as in a real work order, like Test Equipment fields already do.

h3. Notes
* QA build index-C14cwR0U.js, re-checked 13 Oct 2026. Low: preview only; real forms on work orders are fine. Nothing was saved (the form was only opened).
* The builder's "Options source" picker offers only Test Equipment and Linked Asset, so a Users-list field cannot be created in the web builder today; this test form was built through the API. Worth fixing together.
* Test data (QA only): form "QA-DEMO ZP-4751 ZP-4753 dataset type-to-search (delete me)". Found while testing ZP-4751.
* For the developer: the builder's preview dataset hook (EGFormBuilderV2) only knows {{test_equipment}}; the work-order renderer (EGFormRendererV2) also has {{users}} → {{/eg-form-sources/users}}.
```

## ZP-4871

**Summary:** [Web] Work order Forms tab: forms with no key are listed under another form's name  
**Priority:** Low  
**Files to attach:** none  
**What changed:** Replaced "created through the API" with real data: 40 of 658 forms have no key, 33 of them real (not QA-DEMO).

```
h3. What happens
In a work order's *Forms* tab, the "Form Types" list on the left groups form instances by their form key. Two different forms that both have no key are grouped under one name, so instances of one form are listed under the other form's name.

h3. Steps
1. Sign in to https://acme.qa.egalvanic.ai as the Project Manager abhiyant.singh+project@egalvanic.com. Open work order "QA-DEMO ZP-4801 Tuned column (delete me)" (https://acme.qa.egalvanic.ai/sessions/22a22b9c-4eda-43ed-b131-4b7146912441) › *More* › *Forms*.
2. "Form Types" shows ONE name, "QA-DEMO ZP-4717 visible normalization (delete me)", with 3 forms under it. (Screenshot 1)
!7-1-wo-forms-tab-one-form-type.png|width=720!
3. Click the first "13N-H1-1" row. The form that opens is "QA-DEMO ZP-4751 ZP-4753 dataset type-to-search (delete me)", a different form from the one named in the list. (Screenshot 2)
!7-2-row-opens-a-different-form.png|width=720!
4. Control: in *Builder › Forms* (https://acme.qa.egalvanic.ai/eg-forms), search "QA-DEMO ZP-47". These are two separate forms. (Screenshot 3)
!7-3-control-two-separate-forms-in-builder.png|width=720!

h3. What should happen
"Form Types" groups by the form itself (its id or title), not by its key. Forms that have no key each show their own name and are not merged under one name.

h3. Notes
* QA build index-C14cwR0U.js, re-checked 13 Oct 2026: still reproduces. The three rows belong to two different forms (one e293697b, two dc24bb98), both with key = null.
* Not only QA data: on acme QA, 40 of 658 forms have no key, and 33 of those are real (not QA-DEMO) forms, e.g. "Untitled Form" (4 of them), "70B-DA", "Test eg form 1". Any of them can be added to an asset on a work order (the Add form picker uses the form id, not the key), so a real work order can end up with two keyless forms.
* Test data (QA only): forms "QA-DEMO ZP-4717 visible normalization (delete me)" and "QA-DEMO ZP-4751 ZP-4753 dataset type-to-search (delete me)" on the work order above. No existing ticket (ZP-3880 fixed a related NULL-key rules-matching bug in a different place).
* For the developer: the "Form Types" list keys each group by {{eg_form_key}}; when it is null for several forms they collapse into one group labelled with the first form's title.
```

## ZP-4876

**Summary:** [Web] SKM re-import of an unchanged export changes the class of existing assets (12 of 94 on the test site, e.g. PDU → Panelboard, Lightning Arrester → Load), with no warning  
**Priority:** Medium  
**Files to attach:** `4876-1-full-export-reimport-review.png`  
**What changed:** Real full-export steps ("Update 94 assets"), table of the 12 class changes (incl. arrester / shunt reactor / instrument transformer / motor controllers -> Load); says the commit was NOT run with the full file (proven earlier on a 4-asset cut).

```
h3. What happens
Re-importing an SKM file that was just exported from the same site, without editing it, changes the class of existing assets. On the test site 12 of 94 assets would change class (e.g. Capacitor → Capacitor Bank, Motor Starter → Motor Controller, PDU / Other / MV Switch / Junction Box → Panelboard, and Lightning Arrester / Shunt Reactor / Instrument Transformer / Motor Controller → Load). The review screen does not warn that any class will change.

h3. Steps
1. Sign in to https://acme.qa.egalvanic.ai as the Project Manager abhiyant.singh+project@egalvanic.com. Pick site "QA-DEMO ZP-4800 SLD canvas v2.4 (delete me)". Open *Site Data › Assets* and note the classes: QA G-SeriesCap = *Capacitor*, QA G-MotorStarter = *Motor Starter*, QA G-PDU = *PDU*, QA G-Arrester = *Lightning Arrester*. (Screenshots 1 and 2 show some of these assets on the SLD before any import)
!QA-13Oct-ZP-4603-2-PASS-instrument-xfmr-capbank-relay-meter.png|width=720!
!QA-13Oct-ZP-4603-7-PASS-pdu-panelboard-other-boxed-switches.png|width=720!
2. Open the SLD page (https://acme.qa.egalvanic.ai/slds), click *Export › Export Engineering XML*, confirm, and save the file. Do not open or edit it.
3. Go to *Site Data › Assets › SKM › Import (Full)*, choose that file and click *Analyze*.
4. The review says "94 Updated", "94 existing assets will be updated", and the button reads *Update 94 assets*. The class chips above the table count the classes the import will give (e.g. "Panelboard: 12", "Load: 9", "Capacitor Bank: 2"). There is no "class will change" warning; the banner only says engineering values will be overwritten. (Screenshot 3)
!4876-1-full-export-reimport-review.png|width=720!
5. Comparing the class the preview gives each asset with its current class shows these 12 changes (the other 82 assets keep their class):
||Asset||Class now||Class after re-import||SKM type||
|QA G-PDU, QA G-Other, QA G-MVSwitch, QA-DEMO 4799 JBOX (delete me)|PDU, Other, MV Switch, Junction Box|Panelboard|Bus|
|QA G-SeriesCap|Capacitor|Capacitor Bank|Pi|
|QA G-MotorStarter|Motor Starter|Motor Controller|MotorController|
|QA G-Arrester, QA G-ShuntReactor, QA G-InstrXfmr|Lightning Arrester, Shunt Reactor, Instrument Transformer|Load|Load|
|QA MC1, QA G-MotorController, QA-DEMO ZP-4514 MC (delete me)|Motor Controller|Load|Load|
6. QA did NOT click *Update 94 assets* with the full file (it would change data on the site). The class change on commit was proven earlier the same day with a 4-asset cut of this export: the review said *Update 4 assets* (Screenshot 4), and after the update QA G-SeriesCap, QA G-MotorStarter and QA G-PDU had the new classes (Screenshot 5). Those 4 assets were then put back.
!QA-13Oct-ZP-4603-x1-FAIL-skm-reimport-review-unchanged-export.png|width=720!
!QA-13Oct-ZP-4603-x2-FAIL-after-reimport-classes-changed.png|width=720!

h3. What should happen
An unchanged round trip changes nothing. When an imported row matches an existing asset (by Alternate UID), the asset keeps its class, or the review lists each "class will change from X to Y" and lets the user keep the current class.

h3. Notes
* QA build index-C14cwR0U.js, re-checked 13 Oct 2026. Preview checked on the full 94-component export; commit NOT run with the full file; commit proven on 4 of these assets earlier on 13 Oct (then reverted).
* Why it matters: export → fix a few values in SKM → import back is the normal workflow. It would turn arresters, reactors, instrument transformers and starters into Loads/Panelboards and change their symbols, forms and maintenance plans.
* Found while testing ZP-4603 (which passes); not caused by that ticket. Related: ZP-2689 (SKM export class mapping defaults), ZP-4854 (Bulk Edit round trip).
* For the developer: several EG classes share one SKM type (Pi, Bus, Load, MotorController), and on update the importer writes its default class for that type over the existing class.
```

## ZP-4880

**Summary:** [Web] Asset › Maintenance › Add Service History: a future "Last serviced" date shows the raw server error ("API call failed: 400 - { error: last_serviced_on cannot be in the future… }") instead of a clear message  
**Priority:** Medium  
**Files to attach:** `4880-3-pm-customer-seat-same-raw-error.png`  
**What changed:** Steps now use Project Manager + site "QA-DEMO ZP-4800 SLD canvas v2.4 (delete me)" + asset QA PNL1 URL; notes that the date box has no max date; FDR-Circuit Breaker-3 kept as a Super Admin note.

```
h3. What happens
On an asset's *Maintenance* tab, adding a service history record with a date in the future shows the raw server reply inside the dialog:
{noformat}
API call failed: 400 - { "error": "last_serviced_on cannot be in the future \u2014 it records work that has already happened", "success": false }
{noformat}
The user sees "API call failed", the status code, JSON, the field name last_serviced_on and even the code \u2014 instead of a dash. The date box itself lets the user pick a future date.

h3. Steps
1. Sign in to https://acme.qa.egalvanic.ai as a Project Manager (e.g. abhiyant.singh+project@egalvanic.com). In the site picker choose "QA-DEMO ZP-4800 SLD canvas v2.4 (delete me)".
2. Go to *Site Data › Assets*, search "QA PNL1" and open it (https://acme.qa.egalvanic.ai/assets/4b84bf4a-ad24-4c31-944d-5d65ba29a846).
3. Click the *Maintenance* tab. On the right, under *Service history*, click *+ Add record*. (Screenshot 1: same tab, shown on asset FDR-Circuit Breaker-3)
!1-asset-maintenance-tab-add-record.png|width=720!
4. In *Add Service History* pick Service = *Cleaning*. (Screenshot 2)
!2-add-service-history-dialog.png|width=720!
5. In *Last serviced* pick tomorrow's date. The date box accepts it: future days are not greyed out.
6. Click *Save*. The red box shows the raw server error. (Screenshot 3: Project Manager on QA PNL1; Screenshot 4: first run; Screenshot 5: close-up)
!4880-3-pm-customer-seat-same-raw-error.png|width=720!
!3-future-date-shows-raw-api-error.png|width=720!
!4-raw-api-error-close-up.png|width=720!

h3. What should happen
* The date box does not allow dates after today (or turns red as soon as a future date is picked).
* If a future date is still sent, the window shows a short, plain message under the date, for example: *"Last serviced can't be a future date. Pick today or an earlier date."*
* Users never see "API call failed", the status code, JSON or field names like last_serviced_on.

h3. Notes
* QA build index-C14cwR0U.js, re-checked 13 Oct 2026. Save returned 400, so nothing was stored. Video attached (future_date_infrared.mov).
* Also reproduced as Super Admin on asset FDR-Circuit Breaker-3 (https://acme.qa.egalvanic.ai/assets/713ff57d-288d-47b3-b7b4-ef12958f0b9c), which a Project Manager cannot open (that asset is a Cable-class asset despite its name).
* The server check itself is correct (a future date must be refused); only the way the error is shown is wrong. Same kind of issue as ZP-4730, ZP-4714 and ZP-4412 (raw "API call failed" text shown to users).
* For the developer: the *Last serviced* box is a plain date input with no max date; set max = today and map this 400 to a field message.
```

## ZP-4882

**Summary:** [Web] SLD Issues panel calls an Inverter and a Rectifier a "Transformer" ("no secondary winding voltage"): wording only  
**Priority:** Low  
**Files to attach:** `4882-1-sld-issues-inverter-rectifier-called-transformer.png`, `4882-2-inverter-edit-has-secondary-voltage.png`  
**What changed:** Removed the false "user can't fix it" claim (Edit Asset has Secondary Voltage); wording-only bug. Replaced the Engineering-tab screenshot with the Edit Asset one.

```
h3. What happens
On the SLD the *Issues* panel shows "Transformer without secondary voltage on QA G-Inverter" (and on QA G-Rectifier, QA INV1, QA REC1), with the text "This transformer has no secondary winding voltage set". These assets are an Inverter and a Rectifier, not transformers, so the wording is confusing. The value the warning checks (Secondary Voltage) does exist in *Edit Asset* and setting it clears the warning, but the message never says so.

h3. Steps
1. Sign in to https://acme.qa.egalvanic.ai as the Project Manager abhiyant.singh+project@egalvanic.com. Pick site "QA-DEMO ZP-4800 SLD canvas v2.4 (delete me)" and open the SLD page (https://acme.qa.egalvanic.ai/slds).
2. Click the issues box at the top right (it shows the issue counts, 18 issues today) to open the list. Find "Transformer without secondary voltage on QA G-Inverter", "... on QA G-Rectifier", "... on QA INV1" and "... on QA REC1". (Screenshot 1)
!4882-1-sld-issues-inverter-rectifier-called-transformer.png|width=720!
3. Open asset QA G-Inverter: Asset Class = *Inverter*. (Screenshot 2)
!QA-13Oct-ZP-4593-8-inverter-asset-class-inverter-FAIL.png|width=720!
4. Click the ⋮ menu (top right) › *Edit Asset*. Under ENGINEERING the form has *Primary Voltage* and *Secondary Voltage*: this is the value the warning checks. (Screenshot 3)
!4882-2-inverter-edit-has-secondary-voltage.png|width=720!

h3. What should happen
The warning names the real class and the field, e.g. "Inverter without output voltage on QA G-Inverter: set Secondary Voltage in Edit Asset" (Rectifier: "Rectifier without output voltage ..."). Transformers keep today's text.

h3. Notes
* QA build index-C14cwR0U.js, re-checked 13 Oct 2026. Wording only: the check itself is right, and the user can fix it in *Edit Asset*. Low.
* Modelling converters with a primary and secondary voltage is by design (ZP-4593); only the "Transformer" / "winding" wording is wrong. Found while testing ZP-4593.
* For the developer: Transformer, Inverter and Rectifier all have primary_secondary_voltage = true, and the issue builder uses one transformer title/detail for all of them (code {{voltage.secondary_missing}}).
```
