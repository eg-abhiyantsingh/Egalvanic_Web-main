# Ticket rewrites, batch 1 (14 Oct 2026)

Owner's new bug-ticket format. **No Jira writes** - files only; the main session applies them.

Images in `docs/bug-evidence/2026-10-14-ticket-rewrite/`. Step screenshots reference each ticket's existing attachments by filename.

---

## ZP-4840 (High)

**New title:** [Web] Work order Services: on a large work order (hundreds of assets), adding a service to all of them fails and the whole work order screen is replaced by an error

**Files to attach (new):** ZP-4840-key-highlighted.png

**Description (Jira wiki markup):**

```
h3. Simple explanation
On a big work order (more than 500 assets), a user picks *Select all*, opens *Services* and adds a service such as Cleaning.
The service is not added. Instead the whole work order screen disappears and is replaced by an error line with only a *Back to Work Orders* button.
This happens because the web app sends every selected asset in one request, but the server only accepts 500 assets at a time.
It matters because adding a service to all assets is a normal, everyday action on large work orders, and right now it simply breaks on them.
It should add the service to all the assets (sending them in batches of 500), and if anything does go wrong it should show a small message and keep the work order open.

!ZP-4840-key-highlighted.png|width=720!

h3. Actual result
The work order page is replaced by "Failed to add service: API call failed: 400 - ... at most 500 assets per request ...", with only a *Back to Work Orders* button. No service is added to any asset.

h3. Expected result
The service is added to all the selected assets (the web app sends them in batches of up to 500). If something fails, a short message appears in the dialog or as a toast, and the work order page stays open.

h3. Test data
* Environment: https://acme.qa.egalvanic.ai
* Role / test login: a work-order user on production company Bass Electric (seen live); re-checked on QA as the Project Manager test login abhiyant.singh+project@egalvanic.com
* Work order where it happens (production): https://basselectric.egalvanic.ai/sessions/a898ad5e-e3dc-48a5-b0f3-7ab64233c9b7 (work order 1029, "D802 - 200 Paul Ave", 1,512 assets)
* QA work order used to prove the cause (no data changed): https://acme.qa.egalvanic.ai/sessions/22a22b9c-4eda-43ed-b131-4b7146912441 ("QA-DEMO ZP-4801 Tuned column (delete me)")
* QA build index-C14cwR0U.js, re-checked 13 Oct 2026

h3. Steps to reproduce
1. Sign in to https://basselectric.egalvanic.ai (production, company Bass Electric) and open *Work Orders* then work order *1029* (https://basselectric.egalvanic.ai/sessions/a898ad5e-e3dc-48a5-b0f3-7ab64233c9b7).
2. On the *Assets* tab click *Edit*, tick the header checkbox, then click *Select all 1512*. (Screenshot 1)
!1-manage-services-1512-assets-add-cleaning.png|width=720!
3. Click *Services*. The "Manage Services - 1512 assets" dialog opens. Under *Add a service* choose *Cleaning* and click *Add*.
4. The work order screen is replaced by the error, with only a *Back to Work Orders* button (the service is not added). (Screenshot 2, and the attached video bug_service_bulk.mp4)
!2-failed-to-add-service-at-most-500-assets.png|width=720!
5. Proof of the cause on QA, no data changed (Screenshot 3): on work order "QA-DEMO ZP-4801 Tuned column (delete me)" (https://acme.qa.egalvanic.ai/sessions/22a22b9c-4eda-43ed-b131-4b7146912441), signed in as the Project Manager, QA sent the same add-assets request with random asset ids and a random service id (so nothing can match):
!ZP-4840-1-PM-QA-add-assets-cap-500-mechanism.png|width=720!
{noformat}
add-assets with 501 asset ids  -> 400 "at most 500 assets per request"
add-assets with 500 asset ids  -> 400 "no addable assets (unknown, deleted, or on another site)"  (control)
{noformat}

h3. Notes for the developer
* The bulk *Manage Services > Add* handler sends ALL selected asset ids in ONE {{POST /api/ir_session/<work order id>/add-assets}} call; on failure it sets the page-level error, which replaces the whole work order view.
* The server limit (500 assets per request) is working as designed; the web app does not respect it when *Select all* picks more than 500.
* Removing a service from all assets in the same dialog does NOT hit this limit (it sends one request per asset), but with 1,512 assets that is 1,512 calls in a row; worth batching in the same fix.
```

---

## ZP-4841 (High)

**New title:** [Web] A customer user can read another company's quotes (titles, prices and scope of work) by asking for that company's site

**Files to attach (new):** ZP-4841-key-highlighted.png

**Description (Jira wiki markup):**

```
h3. Simple explanation
A signed-in customer user (not Egalvanic staff) can read another company's quotes, including titles, status, prices and scope of work.
The app never offers that other company's site to this user, so they cannot reach it through any screen.
But if the site id is known, the server still answers the quotes request for it, exactly as it does for the user's own sites.
This matters because one customer can see another customer's commercial information (what they quoted and for how much).
The server already refuses this same user when they try to open that company's site, so it should refuse the quotes request in the same way.

!ZP-4841-key-highlighted.png|width=720!

h3. Actual result
The quotes request for another company's site returns 200 with that company's 6 quotes, including titles, status, description, scope of work, contacts, site and opportunity names, hours and totals (one quote, "Transformer-Repair", status accepted, shows a total of 116,134.11).

h3. Expected result
The quotes request is refused (permission denied), the same answer the server already gives this user when they try to open that company's site.

h3. Test data
* Environment: https://acme.qa.egalvanic.ai
* Roles / test logins (all customer users, not staff, none assigned to that site): Project Manager abhiyant.singh+project@egalvanic.com; also reproduced as Account Manager abhiyant.singh+accountm@egalvanic.com, Electrical Engineer abhiyant.singh+electric@egalvanic.com, Facility Manager abhiyant.singh+fm@egalvanic.com
* Quotes page: https://acme.qa.egalvanic.ai/opportunities
* Other company's site used: "Saveland Avenue Lofts", id f86052b7-1683-431f-a86f-f52c6dd5be39
* The user's own site (control): id aadcee4c-7dd0-45b3-81b9-309c5c166084 -> its own 17 quotes
* QA build index-C14cwR0U.js, re-checked 13 Oct 2026

h3. Steps to reproduce
1. Sign in to https://acme.qa.egalvanic.ai as the Project Manager test login abhiyant.singh+project@egalvanic.com.
2. Click *Quotes* in the left menu (https://acme.qa.egalvanic.ai/opportunities), then *New Quote*, and type {{Saveland}} in *Facility*. It says *No options*, because that site belongs to another company. (Control: typing {{Richmond}} lists "Richmond, CA".) (Screenshot 1)
!ZP-4841-1-PM-new-quote-facility-Saveland-no-options.png|width=720!
3. On the same page open the browser console (F12) and run the quotes request the *Planning* page makes:
{noformat}
fetch('/api/quote/by-sld/f86052b7-1683-431f-a86f-f52c6dd5be39').then(r=>r.json()).then(console.log)
{noformat}
4. The result is 200 with that company's 6 quotes (titles, status, scope of work, contacts, hours and totals). (Screenshot 2)
!ZP-4841-2-PM-quote-by-sld-foreign-site-returns-quotes.png|width=720!
5. Controls, same user and console:
{noformat}
fetch('/api/sld/f86052b7-1683-431f-a86f-f52c6dd5be39').then(r=>console.log(r.status))              // 422 - opening the site is refused
fetch('/api/opportunity/by-sld/f86052b7-1683-431f-a86f-f52c6dd5be39').then(r=>console.log(r.status)) // 422 - refused
fetch('/api/quote/by-sld/aadcee4c-7dd0-45b3-81b9-309c5c166084').then(r=>r.json()).then(console.log)  // own site - 17 quotes (correct)
{noformat}
6. First proof, earlier the same day. (Screenshot 3)
!b1-2-pm-quote-by-sld-other-company-proof.png|width=720!

h3. Notes for the developer
* {{quote/by-sld/<id>}} and {{quote/by-sld/<id>/lightweight}} lack the site-access check ({{@require_sld_access}}, which ZP-4585 extended with a company check) that {{sld/<id>}} and {{opportunity/by-sld/<id>}} already have.
* Same family as ZP-4709 ({{nodes/sld}}, library-designations), but this endpoint is not covered there.
```

---

## ZP-4842 (Low)

**New title:** [Web] Creating a quote with a broken site id makes the server fail (internal error) instead of saying 'invalid site' (API only)

**Files to attach (new):** ZP-4842-key-highlighted.png

**Description (Jira wiki markup):**

```
h3. Simple explanation
When the create-quote request is sent with a broken site id (not a valid id), the server fails with an internal error instead of a clear "invalid site" message.
A normal user cannot reach this from the screen: the *New Quote* dialog only lets them pick a real site from a list, so it never sends a broken id.
It matters because a failing server request is noise in the error logs and is not the clean, clear answer the server should give for bad input.
Nothing is created by the request.
It should answer with a clear "invalid site id" message (the same as for a well-formed id the user is not allowed to use).

!ZP-4842-key-highlighted.png|width=720!

h3. Actual result
The create-quote request with a broken site id answers with an internal error and a trace id. Nothing is created.

h3. Expected result
A clear "invalid site id" answer (the same as a well-formed id the user cannot access).

h3. Test data
* Environment: https://acme.qa.egalvanic.ai
* Role / test login: Project Manager abhiyant.singh+project@egalvanic.com (a customer user, not staff)
* Quotes page: https://acme.qa.egalvanic.ai/opportunities
* Reachable only by a request typed in the browser console; nothing was created
* QA build index-C14cwR0U.js, re-checked 13 Oct 2026

h3. Steps to reproduce
1. Sign in to https://acme.qa.egalvanic.ai as the Project Manager abhiyant.singh+project@egalvanic.com and click *Quotes* (https://acme.qa.egalvanic.ai/opportunities). In *New Quote* the *Facility* box only offers real sites from the list (typing another company's site name gives "No options"), so the screen cannot send a bad id. (Screenshot 1)
!ZP-4841-1-PM-new-quote-facility-Saveland-no-options.png|width=720!
2. Open the browser console (F12) on the same page and run:
{noformat}
fetch('/api/quote/create-standard',{method:'POST',headers:{'Content-Type':'application/json','X-Direct-Write':'true'},body:JSON.stringify({sld_id:'not-a-uuid',title:'QA-DEMO probe (delete me)'})}).then(r=>console.log(r.status))
{noformat}
3. Repeat with {{sld_id:'12345'}}.
4. Both answer with an internal error and a trace id; nothing is created. (Screenshot 2: first run; Screenshot 3: re-check on the newer build)
!b2-1-create-standard-malformed-id-500.png|width=720!
!ZP-4842-ZP-4847-1-PM-malformed-id-500s.png|width=720!

h3. Notes for the developer
* API only: the Create Quote dialog only sends a site picked from the list, so no user sees this. Low.
* The same broken-id failure also happens on {{GET /api/opportunity/<id>}} (ZP-4847) and the Bulk Edit endpoints (ZP-4855). Same class as ZP-4820 / ZP-4824 (already fixed).
* One shared id check on incoming site ids would fix all of these at once (answer 400/422 for a malformed id).
```

---

## ZP-4843 (Low)

**New title:** [Web] With guest access turned off, typing the /guests address still opens the Guests admin page with an 'Invite guest' button

**Files to attach (new):** ZP-4843-key-highlighted.png, ZP-4843-expected-mockup.png

**Description (Jira wiki markup):**

```
h3. Simple explanation
Guest access is a feature that can be switched on or off per company. For this company it is OFF.
With it OFF the Admin menu correctly has no *Guests* item, so there is no link to the page.
But if a user types the /guests address straight into the browser, the Guests page still opens, with an *Invite guest* button and a search box.
It matters because a page for a switched-off feature should not open at all; it looks like the feature is half on.
It should show the normal "Feature Not Available" page, the same as other switched-off pages (for example /reporting).

!ZP-4843-key-highlighted.png|width=720!

*EXPECTED (mock-up):* !ZP-4843-expected-mockup.png|width=720!

h3. Actual result
Typing the /guests address opens the *Guests* page with an *Invite guest* button, a search box and an empty table ("No rows").

h3. Expected result
While guest access is off, /guests shows the normal "Feature Not Available" page, matching the hidden menu item.

h3. Test data
* Environment: https://acme.qa.egalvanic.ai
* Role / test login: Super Admin abhiyant.singh+admin@egalvanic.com
* Page (typed in the address bar): https://acme.qa.egalvanic.ai/guests
* Feature switch value is the real one (not simulated): guest access = OFF for Acme on QA
* QA build index-C14cwR0U.js, re-checked 13 Oct 2026

h3. Steps to reproduce
1. Sign in to https://acme.qa.egalvanic.ai as the Super Admin abhiyant.singh+admin@egalvanic.com.
2. Click *Admin* in the left rail. Under *People* there are only *Platform Users* and *Guest Portal Users*; there is no *Guests* item (correct). (Screenshot 1)
!b3-1-superadmin-flag-off-admin-menu-no-guests.png|width=720!
3. Type https://acme.qa.egalvanic.ai/guests in the address bar and press Enter.
4. The *Guests* page opens with *Invite guest*, a search box and "No rows". (Screenshot 2)
!b3-2-superadmin-flag-off-typed-guests-page-renders.png|width=720!
5. Re-check on the newer build, same result (Screenshot 3: menu without Guests; Screenshot 4: typed /guests opens the page).
!ZP-4843-1-SuperAdmin-flag-off-admin-menu-no-Guests.png|width=720!
!ZP-4843-2-SuperAdmin-flag-off-typed-guests-page-opens.png|width=720!

h3. Notes for the developer
* Parent feature: ZP-4585 (cross-company guest access). Its own QA Review says "Flag off: ... confirm there is no ... Admin > People > Guests page".
* The menu item checks the feature switch ({{guest-access}} + {{users.manage}}), but the {{/guests}} route only checks {{features.settings.users.view}}. Wrap the route in the same feature gate that /reporting and /issue-suggestions use.
* The guest requests behind the page are refused, so no data shows and nothing can be saved (QA did not click *Invite guest*). Impact is a dead page, not a data leak. Low.
```

---

## ZP-4844 (Medium)

**New title:** [Web] Assets: after opening an asset and pressing Back, the Search box still shows the text but the list is no longer filtered

**Files to attach (new):** ZP-4844-key-highlighted.png, ZP-4844-expected-mockup.png

**Description (Jira wiki markup):**

```
h3. Simple explanation
On the Assets page a user types a word in Search, for example "Generator", and the list narrows to the matching assets.
They open one asset, then press the browser Back button to return to the list.
The Search box still shows "Generator", but the list is no longer filtered: it shows all the assets again.
It matters because the box and the list now disagree, so the user thinks they are seeing matches when they are seeing everything.
It should either bring back the matching assets (so the box and list agree) or clear the Search box.

!ZP-4844-key-highlighted.png|width=720!

*EXPECTED (mock-up):* !ZP-4844-expected-mockup.png|width=720!

h3. Actual result
After pressing Back, the Search box still says "Generator" but the list shows all 408 assets. Typing one more character applies the search again.

h3. Expected result
The box and the list agree: the 12 matching assets come back, or the Search box is cleared.

h3. Test data
* Environment: https://acme.qa.egalvanic.ai
* Role / test login: Project Manager abhiyant.singh+project@egalvanic.com
* Site: "Richmond, CA"
* Assets page: https://acme.qa.egalvanic.ai/assets
* QA build index-C14cwR0U.js, re-checked 13 Oct 2026 (first noted on index-COeCBn0z.js while testing ZP-3424, which passes)

h3. Steps to reproduce
1. Sign in to https://acme.qa.egalvanic.ai as the Project Manager, choose site *Richmond, CA* and open *Assets* (https://acme.qa.egalvanic.ai/assets).
2. Type {{Generator}} in Search. The list narrows to 12 assets. (Screenshot 1)
!QA-13Oct-ZP-3424-14-PM-search-Generator-before-opening-asset-PASS.png|width=720!
3. Click any asset row to open it, then press the browser *Back* button.
4. The Search box still says *Generator*, but the list shows all 408 assets (first rows are CB 451, CT 450, Panelboard-29). (Screenshot 2)
!QA-13Oct-ZP-3424-15-PM-search-box-says-Generator-but-list-shows-all-assets-FAIL.png|width=720!

h3. Notes for the developer
* The grid restores the box text from session storage ({{mainDataGrid_assets_searchQuery}}), but the Assets page's applied search starts empty, so the list request has no {{search=}}.
* Keeping the search in the URL ({{q=}}) would fix it and also make shared links complete (same approach as sort/filters after ZP-3424).
```

---

## ZP-4845 (Low)

**New title:** [Web] Report config Site picker: a search with no match shows the current site instead of a 'No sites match' message

**Files to attach (new):** ZP-4845-key-highlighted.png, ZP-4845-expected-mockup.png

**Description (Jira wiki markup):**

```
h3. Simple explanation
When editing a report configuration, there is a Site picker where the user types to search for a site.
If the user types something that matches no site at all, the picker still shows the site that is currently selected.
So a search with no match never shows a "No sites match" message, which is confusing: it looks like that site matched.
It matters because the user can be misled into thinking their typed text found a site, when it did not.
It should show "No sites match ..." when nothing matches, and not keep listing the current site as if it were a search result.

!ZP-4845-key-highlighted.png|width=720!

*EXPECTED (mock-up):* !ZP-4845-expected-mockup.png|width=720!

h3. Actual result
For a no-match search ("qa-nomatch-zzqq") the only row is the current site ("test site"). For the search "z -" the list shows "test site" first, then "Z - Hospital".

h3. Expected result
"No sites match "qa-nomatch-zzqq"". The current pick is not shown as a search result (or is clearly marked as the current choice).

h3. Test data
* Environment: https://acme.qa.egalvanic.ai
* Role / test login: Super Admin abhiyant.singh+admin@egalvanic.com
* Report configuration: https://acme.qa.egalvanic.ai/reporting/config/0eee082a-af4d-4805-9ae0-c19d3996af9a
* QA build index-C14cwR0U.js, re-checked 13 Oct 2026 (first noted on index-COeCBn0z.js while testing ZP-4680, which passes); same on work-order-level configs
* Cosmetic only; no data is changed

h3. Steps to reproduce
1. Sign in as Super Admin and open https://acme.qa.egalvanic.ai/reporting/config/0eee082a-af4d-4805-9ae0-c19d3996af9a.
2. In the preview *Site* picker, pick *test site*.
3. Click the picker again and type {{qa-nomatch-zzqq}} (a value no site matches). The only row shown is "test site". (Screenshot 1)
!QA-13Oct-ZP-4680-5-no-match-shows-selected-site-not-empty-state-FAIL.png|width=720!
4. Now type {{z -}}: the list shows "test site" first, then "Z - Hospital". (Screenshot 2)
!QA-13Oct-ZP-4680-6-search-z-finds-z-hospital-but-current-site-pinned-FAIL.png|width=720!

h3. Notes for the developer
* The options are built so the selected site is always added to the results, with {{disableClearable}}, so the "No sites match" message (which the picker already has) can never show.
* Fix, for example, by only prepending the selected site when the search box is empty.
```

---

## ZP-4846 (Low)

**New title:** [Web] Opening a report configuration link with a wrong or deleted id shows a completely blank page

**Files to attach (new):** ZP-4846-key-highlighted.png, ZP-4846-expected-mockup.png, ZP-4846-2-SuperAdmin-reporting-config-api-500.png, ZP-4846-3-SuperAdmin-reporting-config-unknown-uuid-blank.png

**Description (Jira wiki markup):**

```
h3. Simple explanation
A report configuration has its own link, which people can bookmark or share.
If the id in that link is wrong, or points to a configuration that was later deleted, the page opens completely blank, with no message at all.
The user has no idea what happened and no way back from the page.
The realistic way this is hit is a saved or shared link to a configuration that no longer exists.
It should show a clear "Report configuration not found" message with a link back to the list of configurations.

!ZP-4846-key-highlighted.png|width=720!

*EXPECTED (mock-up):* !ZP-4846-expected-mockup.png|width=720!

h3. Actual result
The page stays blank with no message. The server request for the configuration answers with an internal error. A well-formed id that does not exist gives the same blank page.

h3. Expected result
A clear "Report configuration not found" message with a link back to the Builder, and the server answers 400/404 for the request.

h3. Test data
* Environment: https://acme.qa.egalvanic.ai
* Role / test login: Super Admin abhiyant.singh+admin@egalvanic.com
* Bad link (typed in the address bar): https://acme.qa.egalvanic.ai/reporting/config/not-a-uuid
* A well-formed id that does not exist gives the same blank page
* QA build index-C14cwR0U.js, re-checked 13 Oct 2026 (first noted on index-COeCBn0z.js while testing ZP-4680)

h3. Steps to reproduce
1. Sign in to https://acme.qa.egalvanic.ai as Super Admin.
2. Open https://acme.qa.egalvanic.ai/reporting/config/not-a-uuid (for example a mistyped or cut-off link). The page stays blank. (Screenshot 1)
!QA-13Oct-ZP-4680-12-malformed-config-id-in-url-api-500-FAIL.png|width=720!
3. Open the browser console (F12). The request {{GET /api/reporting/configs/not-a-uuid}} (and {{.../preview-entities}}) answers with an internal error. (Screenshot 2)
!ZP-4846-2-SuperAdmin-reporting-config-api-500.png|width=720!
4. A well-formed id that does not exist gives the same blank page. (Screenshot 3)
!ZP-4846-3-SuperAdmin-reporting-config-unknown-uuid-blank.png|width=720!

h3. Notes for the developer
* When the configuration is not found, the editor only does {{console.error("Config not found:", id)}} and renders nothing.
* ZP-3347 (502 on this page, Done) was a different error.
```

---

## ZP-4848 (High)

**New title:** [Web] A customer user can download the full engineering file of another company's site, although they are refused to open that site

**Files to attach (new):** ZP-4848-key-highlighted.png

**Description (Jira wiki markup):**

```
h3. Simple explanation
A signed-in customer user (not Egalvanic staff) can get a download link for another company's whole engineering model (the SKM file: equipment names, ratings and connections).
The app never offers that other company's site to this user, so they cannot reach it through any screen.
But if the site id is known, the export request still answers with a working download link.
The same thing happens for a site of the user's own company that they are not assigned to.
It matters because one customer can pull another customer's full electrical design, which is sensitive engineering data.
The export should be refused (the same as opening the site) for other companies' sites and for sites the user is not assigned to.

!ZP-4848-key-highlighted.png|width=720!

h3. Actual result
The export request for another company's site answers 200 with a working download link and the model size (375 equipment items, 193 connections). The same user is refused when they try to open that site.

h3. Expected result
The export is refused (the same as opening the site) for other companies' sites and for sites the user is not assigned to.

h3. Test data
* Environment: https://acme.qa.egalvanic.ai
* Role / test login: Project Manager abhiyant.singh+project@egalvanic.com (a customer user, not staff)
* SLD page: https://acme.qa.egalvanic.ai/slds
* Other company's site: "Saveland Avenue Lofts", id f86052b7-1683-431f-a86f-f52c6dd5be39
* Acme site the user is NOT assigned to: "Test site A", id b99f9c84-a4d9-40fe-8f1b-7ef9e38de3f0
* The user's own site (control): id aadcee4c-7dd0-45b3-81b9-309c5c166084 -> 200 (correct)
* QA build index-C14cwR0U.js, re-checked 13 Oct 2026. QA did not open the downloaded file.

h3. Steps to reproduce
1. Sign in to https://acme.qa.egalvanic.ai as the Project Manager abhiyant.singh+project@egalvanic.com.
2. Open the SLD page (https://acme.qa.egalvanic.ai/slds) and click *Export*. *Export Engineering XML* is the button that runs the export for the current site. (Screenshot 1, menu shown on site "Richmond, CA")
!4848-0-sld-export-menu-export-engineering-xml.png|width=720!
3. Open the browser console (F12) on the same page and run this for another company's site "Saveland Avenue Lofts" (not in this user's site list):
{noformat}
fetch('/api/sld/f86052b7-1683-431f-a86f-f52c6dd5be39/export-skm',{method:'POST',headers:{'Content-Type':'application/json'},body:'{}'}).then(r=>r.json()).then(console.log)
{noformat}
4. The result is 200, success true, with a download link (presigned_url) and the model size (375 equipment items, 193 connections). (Screenshot 2: first run; Screenshot 3: re-check)
!1-export-skm-other-company.png|width=720!
!ZP-4848-1-PM-export-skm-foreign-site-200-with-download-link.png|width=720!
5. Control, same user: opening that site is refused:
{noformat}
fetch('/api/sld/f86052b7-1683-431f-a86f-f52c6dd5be39').then(r=>console.log(r.status))   // 422 permission denied
{noformat}
6. Same check with an Acme site the user is NOT assigned to, "Test site A" (b99f9c84-a4d9-40fe-8f1b-7ef9e38de3f0): opening it is refused (422), but the export answers 200 with a download link.

h3. Notes for the developer
* {{export-skm}} is missing the company and site-assignment check that {{GET /api/sld/<id>}} already has.
* Controls: own site (aadcee4c-7dd0-45b3-81b9-309c5c166084) -> 200 (correct); a random id -> 400 "SLD not found".
* Same family as ZP-4709 / ZP-4723, different endpoint.
```

---

## ZP-4849 (Medium)

**New title:** [Web] SKM Import lets a Project Manager add equipment to a site they are not assigned to (API only)

**Files to attach (new):** ZP-4849-key-highlighted.png

**Description (Jira wiki markup):**

```
h3. Simple explanation
A Project Manager who is not assigned to a site cannot open that site in the app, and the server refuses to show them its diagram.
But the SKM import requests (preview and commit) still accept that site and create new equipment on it.
A normal user cannot reach this from the screen: the site is not offered in the site picker, so it can only be done by sending the requests from the browser console.
It matters because a user who should have no access to a site can still change that site's equipment list.
The import should be refused for a site the user is not assigned to, exactly like the diagram read already is.

!ZP-4849-key-highlighted.png|width=720!

h3. Actual result
The diagram read is refused, but the SKM import preview answers "will create 1" and the commit answers "created 1" - a new bus is now on a site this user was never given.

h3. Expected result
Preview and commit are refused (permission denied) for a site that is not in the user's assigned sites, exactly like the diagram read.

h3. Test data
* Environment: https://acme.qa.egalvanic.ai
* Role / test login: Project Manager abhiyant.singh+project@egalvanic.com (a customer user, not staff, 116 assigned sites)
* Site (not assigned to this user, not offered in the picker): "QA-DEMO ZP-4582 SKM namespace (delete me)", id cfc07c0d-71a2-40bc-bd14-76ad2372ffbd
* Reachable only by requests typed in the browser console; running them creates one clearly labelled QA-DEMO bus on that QA-DEMO site
* QA build index-C14cwR0U.js, re-checked 13 Oct 2026

h3. Steps to reproduce
1. Sign in to https://acme.qa.egalvanic.ai as the Project Manager abhiyant.singh+project@egalvanic.com.
2. Open the *Current site* box and type {{QA-DEMO ZP-4582}}. The app says "No sites available": this site is not assigned to this user. (Screenshot 1)
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
console.log('commit', cm.status, await cm.json());                             // 200, created 1 - should be refused
{noformat}
4. Result: read refused, preview "will create 1", commit "created 1". (Screenshot 2: re-check table; Screenshot 3: first run)
!A2-4849-pm-import-commit-on-unassigned-site.png|width=720!
!2-skm-import-unassigned-and-other-company-site.png|width=720!
5. Sign in as an admin, pick site "QA-DEMO ZP-4582 SKM namespace (delete me)", open *Site Data > Assets* and search {{PM scope}}: the new bus is on the site. (Screenshot 4)
!2b-node-written-by-pm-on-unassigned-site.png|width=720!

h3. Notes for the developer
* {{/api/skm/import-xml/preview}} and {{/commit}} check the company but not caller_can_reach_sld(sld_id). Please cover them in the same pass as ZP-4865 and the ZP-4709 follow-up.
* The Project Manager has data.import; the missing check is the site assignment, not the permission.
* Not tested: whether re-import matching can also UPDATE existing equipment on such a site. This ticket only claims "create". Same kind of gap as ZP-4865 (SLD edits), different endpoints.
```

---

## ZP-4850 (Medium)

**New title:** [Web] SKM export switch OFF hides Export on Assets but not 'Export Engineering XML' on the SLD page (please confirm intended)

**Files to attach (new):** ZP-4850-key-highlighted.png

**Description (Jira wiki markup):**

```
h3. Simple explanation
There is a switch that turns the SKM export feature on or off for a company.
With it OFF, the *Export* item correctly disappears from *Site Data > Assets > SKM*.
But the same SKM file can also be exported from the SLD page (*Export > Export Engineering XML*), and that menu does not look at the switch, so it stays and still downloads the file.
So turning the switch off does not fully turn SKM export off.
Important: the switch is really ON for this company on QA, so QA could only simulate the OFF state in its own test browser; a real flip is requested to confirm.
The SLD-page export existed before this switch was added, so this may be intended - please confirm with product: either hide it too while the switch is OFF, or close as by design.

!ZP-4850-key-highlighted.png|width=720!

h3. Actual result
With the switch simulated OFF, the Assets > SKM menu has no *Export* (correct), but the SLD page *Export* menu still lists *Export Engineering XML*, which still downloads the SKM file.

h3. Expected result (please confirm with product)
If the switch is meant to turn SKM export off everywhere, the SLD-page *Export Engineering XML* item is hidden too while the switch is OFF. If the SLD-page item is meant to stay (it existed before the switch), close this as by design.

h3. Test data
* Environment: https://acme.qa.egalvanic.ai
* Role / test login: Project Manager abhiyant.singh+project@egalvanic.com
* Site: "(s) Wild Goose Brewery"
* Assets page: https://acme.qa.egalvanic.ai/assets ; SLD page: https://acme.qa.egalvanic.ai/slds
* The OFF state was *simulated* in QA's own test browser (the switch is really ON for Acme on QA); a real flip is requested to confirm
* QA build index-C14cwR0U.js, re-checked 13 Oct 2026

h3. Steps to reproduce
1. Turn the SKM export switch OFF for Acme (QA). (QA simulated this in its own browser only, because the real value is ON; a real flip is requested.)
2. Sign in to https://acme.qa.egalvanic.ai as the Project Manager abhiyant.singh+project@egalvanic.com, site "(s) Wild Goose Brewery".
3. Open *Site Data > Assets* (https://acme.qa.egalvanic.ai/assets) then *SKM*: the menu shows Import (Full), Import (XML to Excel), Settings Verifier. No *Export* (correct). (Screenshot 1, simulated OFF)
!3a-flag-off-simulated-assets-skm-menu-no-export.png|width=720!
4. Open the SLD page (https://acme.qa.egalvanic.ai/slds) and click *Export*. The menu still lists *Export Engineering XML*, and it still downloads the SKM file. (Screenshot 2, simulated OFF; Screenshot 3, re-check, labelled SIMULATED)
!3b-flag-off-simulated-sld-export-menu-still-shows-xml.png|width=720!
!B4-4850-simulated-flag-off-sld-menu.png|width=720!

h3. Notes for the developer
* The SLD export menu item reads no switch (it is only hidden in PDF-only mode); the Assets SKM item reads the SKM-export switch. If a fix is wanted, it is frontend only.
* Real flip needed to confirm without simulation: the SKM-export switch -> OFF, QA, company Acme, for a few minutes, check both menus, then back ON.
* The SLD-page XML export was already there before this switch (QA used it on 3 Sep 2026 while testing ZP-3836). Raised first as a gap on ZP-4482.
```

---

## ZP-4853 (Low)

**New title:** [Web] SKM Export: the 'label numbers from location' help text still promises a '-1, -2' suffix that the export no longer adds

**Files to attach (new):** ZP-4853-key-highlighted.png, ZP-4853-expected-mockup.png

**Description (Jira wiki markup):**

```
h3. Simple explanation
In the SKM Export dialog there is an option "Generate arc-flash label numbers from location" with a small round "i" help icon.
Hovering the icon shows help text that says each bus gets its Building/Floor/Room plus "-1, -2, ..." added to make the label.
Since ZP-4760 the export no longer adds that "-1, -2" suffix; it writes the location label exactly as it is.
So the help text now describes behaviour that does not happen, which can mislead the user about what the export will produce.
It should be updated to match what the export really does (no "-1, -2"), including the French version.

!ZP-4853-key-highlighted.png|width=720!

*EXPECTED (mock-up):* !ZP-4853-expected-mockup.png|width=720!

h3. Actual result
The help text says: "Each bus gets Building/Floor/Room plus -1, -2, ... as its SKM arc-flash label number. Buses that already have a label number keep it; buses with no location are left blank." But the export gives three buses in the same room the same label, with no "-1, -2" suffix.

h3. Expected result
The help text matches the export, for example: "Each bus gets its Building/Floor/Room as its SKM arc-flash label number; buses with no location are left blank." The same change in the French text.

h3. Test data
* Environment: https://acme.qa.egalvanic.ai
* Role / test login: Project Manager abhiyant.singh+project@egalvanic.com (for the dialog); Super Admin abhiyant.singh+admin@egalvanic.com (to run the export on the QA-DEMO site)
* Site for the dialog: "(s) Wild Goose Brewery" ; Assets page: https://acme.qa.egalvanic.ai/assets
* Site for the export proof: "QA-DEMO ZP-4582 SKM namespace (delete me)", id cfc07c0d-71a2-40bc-bd14-76ad2372ffbd
* QA build index-C14cwR0U.js, re-checked 13 Oct 2026

h3. Steps to reproduce
1. Sign in to https://acme.qa.egalvanic.ai as the Project Manager abhiyant.singh+project@egalvanic.com, site "(s) Wild Goose Brewery".
2. Open *Site Data > Assets* (https://acme.qa.egalvanic.ai/assets) then *SKM* then *Export*. The "Export to SKM XML" dialog opens with the option "Generate arc-flash label numbers from location" and a small round "i" icon. (Screenshot 1)
!B3-4853-tooltip-text.png|width=720!
3. Hover the "i" icon. The text promises the "-1, -2" suffix. (Screenshot 2)
!6-label-from-location-tooltip-promises-suffix.png|width=720!
4. To see what the export really does: sign in as Super Admin, pick site "QA-DEMO ZP-4582 SKM namespace (delete me)", run *Site Data > Assets > SKM > Export* with that option ticked, and open the file. The three buses in the same room all get the same label, with no "-1, -2". (Screenshot 3)
!4853-3-export-three-buses-same-label-no-suffix.png|width=720!

h3. Notes for the developer
* Frontend text only, key {{labelFromLocationHint}} (EN + FR).
* The sentence "Buses that already have a label number keep it" is a requirement of ZP-4746 and is tracked there; keep or drop that sentence depending on the ZP-4746 outcome.
```

---

## ZP-4854 (Medium)

**New title:** [Web] Bulk Import of an unchanged Bulk Export shows 20 asset changes and would overwrite values (e.g. a generator's voltage)

**Files to attach (new):** ZP-4854-key-highlighted.png, ZP-4854-expected-mockup.png

**Description (Jira wiki markup):**

```
h3. Simple explanation
A user exports the assets with *Bulk Export*, does not edit the file at all, and imports the same file back with *Bulk Import*.
On the *Review* step the app says 20 assets and 4 connections are "to update", even though nothing was changed.
Some of these are harmless (a number shown as "56" becoming "56.0"), but at least one is a real change: a generator's voltage would drop from 240 to 200.
If the user clicks *Process Import*, those changes would be written to real assets.
It matters because a round trip with no edits should change nothing; here it silently proposes changes, including a wrong one.
An unchanged export should re-import as "No changes".

!ZP-4854-key-highlighted.png|width=720!

*EXPECTED (mock-up):* !ZP-4854-expected-mockup.png|width=720!

h3. Actual result
The Review step shows "Assets: 20 to update" and "Connections: 4 to update" for a file that was not edited. Examples: Catalog Number 56 to 56.0, Size 67 to 67.0, Serial Number 89 to 89.0, and "Generator-1: voltage 240.0 to 200.0".

h3. Expected result
An unchanged export re-imports as "No changes".

h3. Test data
* Environment: https://acme.qa.egalvanic.ai
* Role / test login: Project Manager abhiyant.singh+project@egalvanic.com
* Site: "(s) Wild Goose Brewery"
* Assets page: https://acme.qa.egalvanic.ai/assets
* File used: {{(s)_Wild_Goose_Brewery_bulk_edit.xlsx}} (its own fresh Bulk Export)
* QA build index-C14cwR0U.js, re-checked 13 Oct 2026. QA did NOT click *Process Import*.

h3. Steps to reproduce
1. Sign in to https://acme.qa.egalvanic.ai as the Project Manager abhiyant.singh+project@egalvanic.com, site "(s) Wild Goose Brewery", and open *Site Data > Assets* (https://acme.qa.egalvanic.ai/assets).
2. Click *Bulk Edit > Bulk Export*. The file {{(s)_Wild_Goose_Brewery_bulk_edit.xlsx}} downloads.
3. Do not open the file. Click *Bulk Edit > Bulk Import* and upload that same file.
4. On *Resolve Conflicts*, skip the one ambiguous name ("Panelboard-1" matches 2 assets), then go to *Review*.
5. The Review says *Assets: 20 to update*, *Connections: 4 to update*. (Screenshot 1: first run; Screenshot 2: re-check)
!7-unchanged-bulk-export-reimport-shows-changes.png|width=720!
!C1-4854-unchanged-export-reimport-review.png|width=720!
6. Scroll the list. Example of a real change: *Generator-1: voltage 240.0 to 200.0*. (Screenshot 3)
!C2-4854-generator-1-change.png|width=720!
7. Do NOT click *Process Import* (QA did not).

h3. Notes for the developer
* Proof from the untouched file: Catalog Number, Size and Serial Number are text cells ("56", "67", "89"), so "56 -> 56.0" comes from the import reading text as a number. Text fields such as Serial Number would really change on Process Import.
* Generator-1 row: node.voltage = "240V" and a separate attribute column "voltage" = "200"; the import lets the attribute overwrite the node value, so an untouched sheet lowers the generator's voltage from 240 to 200. The problem is the round trip, not the data: the user did not change anything.
* Compare each cell to the exported value as text before treating it as a change, and do not let attribute columns overwrite node fields on import. Related to ZP-4592 (same feature), not a duplicate.
```
