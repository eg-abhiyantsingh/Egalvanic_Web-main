# Bug audit A: ZP-4840 to ZP-4848 (13 Oct 2026)

Skeptical-developer re-check of the 9 bugs QA filed on 13 Oct. No Jira writes were made.

- **Build used:** QA `index-C14cwR0U.js`. The tickets were filed on `index-COeCBn0z.js`, and every claim below was re-run on the newer build.
- **Live flags (Acme, QA):** `feature-guest-access` = false, `feature-reporting-v2` = true, `feature-skm-export` = true.
- **Seats:** Project Manager / Account Manager / Electrical Engineer / Facility Manager, all `is_eg_admin=false` (customer users). Super Admin `is_eg_admin=true` (staff) was used only where the ticket says Super Admin, and to look up site ids.
- **Evidence:** `docs/bug-evidence/2026-10-13-bug-audit/ZP-48xx-*.png`. Run scripts: `.playwright-mcp/v24/audit-a/{pm,pmc,sa,multi}.js`.
- **Code checks:** the live bundle and all 330 lazy chunks were downloaded and searched.
- **Duplicate search (JQL):** "500 assets", add-assets, Manage Services, quote/by-sld, export-skm, /guests, guest-access, search+asset/back/persist, site picker, report config blank/500, opportunity 500, create-standard. No duplicates were found. Related tickets are noted per bug.

| Bug | Verdict | One line |
|---|---|---|
| ZP-4840 | VALID-NEEDS-EDIT | Mechanism proven on QA: one request for all selected assets, server caps at 500, and the page is replaced by the error. One note in the ticket is wrong, and a QA proof should be added. |
| ZP-4841 | VALID-NEEDS-EDIT | Reproduced: PM, AM, EE and FM (all non-staff) each get the other company's 6 quotes with totals. Step 3 uses `/lightweight`, which the web app never calls. |
| ZP-4842 | VALID-NEEDS-EDIT | 500 reproduced, API-only Low, honest. The `/api/plans` "control" and "shows in error monitoring" claims are shaky. Merge with ZP-4847. |
| ZP-4843 | VALID-NEEDS-EDIT | Reproduced. Parent ZP-4585's own QA Review says flag OFF means no Guests page. Link ZP-4585, not ZP-4138, and lower to Low. |
| ZP-4844 | VALID-CLEAR | Reproduced exactly: 12 rows, then open an asset, Back, and 408 rows while the box still says "Generator". |
| ZP-4845 | VALID-CLEAR | Reproduced exactly. The code has a `No sites match "…"` message that can never show. Cosmetic Low. |
| ZP-4846 | VALID-CLEAR | Reproduced: blank page and API 500 for `not-a-uuid`; blank page for an unknown well-formed id. |
| ZP-4847 | DOUBTFUL | True 500, but no screen can reach it: the app guards the id twice and the route redirects. It was already noted as a side note on ZP-4777. Fold it into one "malformed id gives 500" ticket, or close it. |
| ZP-4848 | VALID-NEEDS-EDIT | Reproduced exactly (375 components / 193 connections). The ticket needs the console command and the full id of the unassigned site (it is truncated). |

---

## ZP-4840 Work order bulk edit › Services: >500 assets fails and replaces the page

**Verdict: VALID-NEEDS-EDIT.** Build `index-C14cwR0U.js` (QA). Production was not touched.

**What I verified on QA**
- **The web app sends every selected asset in one request.** In the `SessionDetail-2CnuKxRQ.js` chunk, the bulk "Manage Services — N assets › Add" handler calls `cn(o, Kt.map(b=>b.id), [Pn], !Ic)`. That is `POST /api/ir_session/{wo}/add-assets` with `node_ids` set to all selected ids, sent once, with no batching.
- **On failure, the whole page is replaced.** The handler calls `wt("Failed to add service: …")`. `wt` sets the page-level error state, and `if(ml) return <error text + "Back to Work Orders">` then replaces the whole work order view. This matches the screenshot.
- **The server caps the request at 500 assets.** I sent a no-op request to my own WO "QA-DEMO ZP-4801 Tuned column (delete me)" (`22a22b9c-4eda-43ed-b131-4b7146912441`), with random asset ids and a random service id:
  - 501 ids → `400 {"error":"at most 500 assets per request"}`.
  - 500 ids → `400 "no addable assets (unknown, deleted, or on another site)"`. This is the control: the cap check runs first, and nothing was added.
- **Evidence:** `ZP-4840-1-PM-QA-add-assets-cap-500-mechanism.png`.

**How a developer could push back**
- "The 500 cap is by design." The ticket already says the cap is fine and the web app is at fault. Good.
- "Recorded on a customer's production tenant, so I can't reproduce it." Adding the QA proof answers this.
- **The Notes bullet "Removing a service … probably has the same limit" is wrong.** Bulk remove (`$h`) loops and sends one `POST /ir_session/{wo}/remove-service` per asset. There is no 500 cap, but 1,512 assets means 1,512 sequential calls. A developer who checks the code will see this claim is false.

**Duplicates:** none.

**Replacement text for the Notes section:**
```
h3. Notes
* Seen on production (web V2.3), company Bass Electric, recorded 13 Oct 2026.
* Same code on QA web v2.4 (build index-C14cwR0U.js, 13 Oct): the bulk *Manage Services › Add* sends ALL selected asset ids in ONE {{POST /api/ir_session/<work order id>/add-assets}} call, and on failure it uses the page-level error, which replaces the whole work order screen.
* QA proof, no data changed (work order "QA-DEMO ZP-4801 Tuned column (delete me)", random asset ids and a random service id, so nothing can match):
||Request||Answer||
|add-assets with 501 asset ids|400 "at most 500 assets per request"|
|add-assets with 500 asset ids (control)|400 "no addable assets (unknown, deleted, or on another site)"|
* The server limit (500 assets per request) is working as designed; the web app does not respect it when "Select all" picks more than 500 assets.
* Removing a service from all assets in the same dialog does NOT hit this limit (it sends one request per asset), but with 1,512 assets that is 1,512 calls in a row; worth batching in the same fix.
```

---

## ZP-4841 Any customer user can read another company's quotes (`quote/by-sld`)

**Verdict: VALID-NEEDS-EDIT** (small edit; the bug itself is solid, High is justified).

**What I reproduced** (Project Manager, `is_eg_admin=false`, 116 sites, the other-company site is not one of them)
- **Quotes › New Quote › Facility "Saveland":** "No options". Control: typing "Richmond" lists "Richmond, CA". Screenshot: `ZP-4841-1-PM-new-quote-facility-Saveland-no-options.png`.
- **`GET /api/quote/by-sld/f86052b7-1683-431f-a86f-f52c6dd5be39`:** 200 JSON with 6 quotes. Fields include `description`, `sow`, `calculated_total`, `total_hours`, `contact_ids`, `opportunity_name` and `sld_name`.
  - Example: "Transformer-Repair | accepted"; `calculated_total` 116,134.11 is visible.
  - `/lightweight` also returns 200 with 6 quotes.
- **Controls:**
  - Own site `aadcee4c-…` → 17 quotes.
  - Random UUID → 0.
  - `GET /api/sld/f86052b7-…` → 422 permission_denied.
  - `GET /api/opportunity/by-sld/f86052b7-…` → 422.
- **Other seats:** AM, EE and FM (all `is_eg_admin=false`, none assigned to the site) each got 6 quotes from both URLs, and 422 on `sld/<id>`.
- **Evidence:** `ZP-4841-2-PM-quote-by-sld-foreign-site-returns-quotes.png`.

**How a developer could push back**
- **The main step uses `/lightweight`, which does not appear anywhere in the web bundle.** Only `/quote/by-sld/<id>` exists (Planning chunk, `l7`). A developer who greps the web code could say "web never calls this URL". Lead with the full URL the web uses.
- The "sensitive" claim is stronger when the total is named in the ticket.
- **Developer hint:** ZP-4585 added a company check to `@require_sld_access` on 52 routes. `quote/by-sld` evidently lacks that decorator.

**Duplicates:** none. ZP-4709 (nodes/sld, Client Portal) and ZP-4723 are related but are different endpoints.

**Replacement text (Steps + Actual):**
```
h3. Steps
# Sign in to https://acme.qa.egalvanic.ai as the Project Manager test seat (a customer user, is_eg_admin = false).
# Click *Quotes* in the left menu (https://acme.qa.egalvanic.ai/opportunities) › *New Quote* › type {{Saveland}} in *Facility*. It says *No options*: site "Saveland Avenue Lofts" belongs to another company. (Screenshot 1)
# On the same page open the browser console and run (this is the call the *Planning* page makes):
{code}fetch('/api/quote/by-sld/f86052b7-1683-431f-a86f-f52c6dd5be39').then(r=>r.json()).then(console.log){code}

h3. Actual
200 with that company's 6 quotes, including title, status, description, scope of work ({{sow}}), contacts, site and opportunity names, hours and totals (one shows {{calculated_total}} 116,134.11). {{GET /api/quote/by-sld/f86052b7-…/lightweight}} also returns the same 6 quotes. Same result for the Account Manager, Electrical Engineer and Facility Manager seats (all customer users, none assigned to that site).
```
Also change the Notes build to `index-C14cwR0U.js (re-checked 13 Oct)`.

---

## ZP-4842 `POST /api/quote/create-standard` with a malformed `sld_id` gives 500

**Verdict: VALID-NEEDS-EDIT** (valid, honest Low, API-only; best merged with ZP-4847).

**What I reproduced:** as PM, `sld_id:"not-a-uuid"` → 500 `internal_error` with a trace id, and `"12345"` → 500. Nothing was created. Screenshot: `ZP-4842-ZP-4847-1-PM-malformed-id-500s.png`.

**How a developer could push back**
- "No screen can send this." The ticket admits it, and the create helper `U2e` only ever sends a picked site. So this is a hygiene fix only.
- **The "`POST /api/plans` with the same bad id already refuses it cleanly" control does not look clean from the browser.** On QA it comes back as `200 text/html` (the CDN masks a 403/404 as the app's index.html). A developer re-running the step from the console would be confused.
- **"It shows as a 500 in error monitoring" is speculative.** The app never sends a malformed id, so it only shows if someone sends one by hand. Remove the claim unless there is a Sentry event.
- Same class as ZP-4847 and ZP-4855 (filed by another auditor). A developer would rather fix "validate UUID inputs" once.

**Duplicates:** none. Same class as ZP-4820/ZP-4824 (both READY TO RELEASE).

**Replacement text for Expected + Notes:**
```
h3. Expected
400 "invalid site id" (or 422), the same as a well-formed id the user cannot access.

h3. Notes
* QA build index-C14cwR0U.js, 13 Oct 2026.
* API only: the Create Quote dialog only sends a site picked from the list, so no user sees this. Low.
* Same malformed-id 500 also on {{GET /api/opportunity/<id>}} (ZP-4847) and the Bulk Edit endpoints (ZP-4855); one shared UUID check would fix all three.
```

---

## ZP-4843 With guest access OFF, typing /guests still opens the Guests admin page

**Verdict: VALID-NEEDS-EDIT** (valid; re-link and lower the priority).

**What I reproduced** (Super Admin, flag `feature-guest-access` really false for Acme on QA)
- The Admin menu › People shows only Platform Users and Guest Portal Users. Screenshot: `ZP-4843-1-SuperAdmin-flag-off-admin-menu-no-Guests.png`.
- Typing `/guests` opens "Guests" with **Invite guest**, a search box and "No rows". Screenshot: `ZP-4843-2-SuperAdmin-flag-off-typed-guests-page-opens.png`.
- `GET /api/guest-access/guests` and `/roles` answer `200 text/html`, which is QA's CDN mask for a 403/404, so no data loads. I did not click Invite.

**Code**
- **Menu item:** `show: de && W("users.manage")`, where `de = br("guest-access")`.
- **Route:** `<Uo permission:"features.settings.users.view">` only, with no flag check.
- **Other flag-gated pages are different:** `/reporting` and `/issue-suggestions` wrap the route in the feature gate `Qr feature="…"`, which shows "Feature Not Available". So `/guests` is the odd one out.

**By design?** No. The parent ticket **ZP-4585** (In QA) says in its QA Review: "Flag off: … confirm there is no switcher, no Admin > People > Guests page, and that /guest-access/* routes deny."

**How a developer could push back**
- "Part of ZP-4585, comment there instead." The answer is to link ZP-4585 as the parent.
- "Only admins reach it, it's empty, and nothing works." That is true, so Medium is high; Low fits.
- The ticket links ZP-4138, which is less relevant than ZP-4585.

**Duplicates:** none.

**Replacement text for Notes:**
```
h3. Notes
* QA build index-C14cwR0U.js, 13 Oct 2026. Flag value is the real one (not simulated): {{feature-guest-access}} = false for Acme on QA.
* Parent feature: ZP-4585 (cross-company guest access). Its QA Review says: "Flag off: … confirm there is no … Admin > People > Guests page".
* The guest APIs behind the page are refused (no data shows, nothing can be saved), so impact is a dead page, not a data leak. Low.
* For the developer: the menu item checks the flag ({{guest-access}} + {{users.manage}}), but the {{/guests}} route only checks {{features.settings.users.view}}. Other flag-gated pages (e.g. /reporting, /issue-suggestions) wrap the route in the feature gate that shows "Feature Not Available"; do the same here.
```
Change the priority to Low and add the issue link "relates to ZP-4585".

---

## ZP-4844 Assets: after opening an asset and going back, the search text stays but the list is unfiltered

**Verdict: VALID-CLEAR.**

**What I reproduced** (PM, site Richmond, CA)
1. Before searching: "1–25 of 408".
2. Typed `Generator`: "1–12 of 12". Screenshot: `ZP-4844-1-PM-assets-search-Generator-filtered.png`.
3. Opened "Generator-14", then pressed browser Back: the box still says "Generator", the footer says "1–25 of 408", and the first rows are CB 451, CT 450, Panelboard-29. Screenshot: `ZP-4844-2-PM-after-back-box-says-Generator-list-unfiltered.png`.

The numbers in the ticket (12 / 408) match exactly. The mechanism note is confirmed in `MainDataGrid-B3qFonmn.js`: the box is restored from `sessionStorage mainDataGrid_assets_searchQuery`.

**How a developer could push back:** "Search is not in ZP-3424's scope." That doesn't matter: the box and the list disagree, which is a bug either way. ZP-3424's own Definition of Done also says "URL-encoded state, not session/local storage".

**Duplicates:** ZP-3652 (Forms list, Done) is the mirror symptom on another page. Not a duplicate.

---

## ZP-4845 Report config Site picker: a no-match search shows the current site, not "No sites match"

**Verdict: VALID-CLEAR** (Low, cosmetic).

**What I reproduced** (Super Admin, config `0eee082a-…`)
- The picker defaulted to "Performance sld". I picked "test site".
- `qa-nomatch-zzqq` → the only row is "test site · 14 Nov 2025". Screenshot: `ZP-4845-1-SuperAdmin-site-picker-no-match-shows-test-site.png`.
- `z - ` → "test site" first, then "Z - Hospital". Screenshot: `ZP-4845-2-SuperAdmin-site-picker-z-search-test-site-pinned-first.png`.

**Code** (`ReportingConfigEditor-CyF4oIvV.js`)
- The options are built as `options = selected && !results.includes(selected) ? [selected, ...results] : results`, with `disableClearable`.
- The picker also has `noOptionsText: No sites match "<q>"`, but with a site always selected that message can never show.

**How a developer could push back:** "MUI needs the value in the options list." That is fixable (for example, only prepend the selected site when the input is empty). The dead "No sites match" text shows that the intended behaviour was an empty state.

**Duplicates:** none. Follow-up to ZP-4680 (READY TO RELEASE).

---

## ZP-4846 A report configuration link with a bad id shows a blank page (API 500)

**Verdict: VALID-CLEAR** (Low).

**What I reproduced** (Super Admin)
- `/reporting/config/not-a-uuid` → only the "Reporting" header and an empty body, with no message. Screenshot: `ZP-4846-1-SuperAdmin-reporting-config-not-a-uuid-blank.png`.
- `GET /api/reporting/configs/not-a-uuid` → 500 "An internal error occurred.", and `…/preview-entities` → 500. Screenshot: `ZP-4846-2-SuperAdmin-reporting-config-api-500.png`.
- A well-formed unknown id gives the same blank page. Screenshot: `ZP-4846-3-SuperAdmin-reporting-config-unknown-uuid-blank.png`. The API returns the CDN-masked 404 (`200 text/html`).
- **Code:** when the config isn't found, the editor only does `console.error("Config not found:", id)` and renders nothing.

**How a developer could push back:** "Nobody types bad ids." The realistic path is a bookmark or shared link to a config that was later deleted, which is the well-formed-unknown case. That case is already in the ticket.

**Duplicates:** none. ZP-3347 (502, Done) is different, as the ticket already says.

---

## ZP-4847 `GET /api/opportunity/<malformed id>` gives 500

**Verdict: DOUBTFUL.** The fact is true; the value as a separate bug is weak.

**What I reproduced** (PM)
- `not-a-uuid`, `undefined`, `null` and `123` → 500 "An internal error occurred.".
- A random well-formed id → `200 text/html` (the CDN-masked 404).
- `/opportunities/not-a-uuid` redirects to `/opportunities` (the Quotes list).
- Screenshot: `ZP-4842-ZP-4847-1-PM-malformed-id-500s.png`.

**Why a developer (or the owner) would see it as weak**
- **No screen can send a bad id.**
  - The opportunity helper (`H2e`) throws if the id is empty.
  - QuoteDetail only calls `/opportunity/${o.opportunity_id}` inside `if (o?.opportunity_id)`.
  - The `/opportunities/<bad>` route redirects.
  - So the "undefined"/"null" variants cannot come from the app either.
- **"It shows as a 500 in error monitoring" is unproven.** No Sentry event was cited.
- **ZP-4802 (READY TO RELEASE) hides opportunities** ("mint one per quote"), so this is a backstage endpoint.
- **It was already recorded** as a side note in the ZP-4777 QA comment. The owner reassigning it to himself fits this assessment.

**Recommendation:** close it, or fold it into ZP-4842 as one Low ticket, "Malformed ids return 500 instead of 400 on create-standard, opportunity/<id>, bulk-edit (ZP-4855)". Do not keep it as a separate developer ticket.

**Duplicates:** none as a ticket. The same class as ZP-4842, ZP-4855, ZP-4820 and ZP-4824.

---

## ZP-4848 A customer user can download the SKM engineering file of another company's site

**Verdict: VALID-NEEDS-EDIT** (bug solid, High justified; step 3 cannot be reproduced from the ticket alone).

**What I reproduced** (PM, `is_eg_admin=false`)
- **The other company's site:** `POST /api/sld/f86052b7-1683-431f-a86f-f52c6dd5be39/export-skm` → 200, `success:true`, a `presigned_url`, `component_count` 375 and `connection_count` 193. These numbers match the ticket exactly.
- **Controls:**
  - `GET /api/sld/f86052b7-…` → 422 permission_denied.
  - Own site → 200 (470 / 189).
  - Random id → 400 "SLD not found: …".
- **The unassigned acme site:** `b99f9c84-a4d9-40fe-8f1b-7ef9e38de3f0` ("Test site A", not in the PM's 116 sites).
  - `GET /api/sld/<id>` → 422.
  - `export-skm` → 200 with a `presigned_url` (7 components).
- **Evidence:** `ZP-4848-1-PM-export-skm-foreign-site-200-with-download-link.png`.

**How a developer could push back**
- **"Make the same call" gives no command.** The export also needs `POST` with a JSON body. A new developer has to dig for this.
- The unassigned site id is truncated (`b99f9c84-…`) and unnamed.

**Duplicates:** none. Same family as ZP-4709 / ZP-4723, but a different endpoint.

**Replacement text for Steps 3 and Actual:**
```
# In the browser console on the same page run (another company's site "Saveland Avenue Lofts", not in this user's site list):
{code}fetch('/api/sld/f86052b7-1683-431f-a86f-f52c6dd5be39/export-skm',{method:'POST',headers:{'Content-Type':'application/json'},body:'{}'}).then(r=>r.json()).then(console.log){code}
# Control: {{fetch('/api/sld/f86052b7-1683-431f-a86f-f52c6dd5be39').then(r=>console.log(r.status))}} → 422 (the same user is refused when opening the site).

h3. Actual
200 {{success: true}} with a {{presigned_url}} that downloads that company's model (375 components, 193 connections). The same user gets 422 permission_denied on {{GET /api/sld/f86052b7-…}}. An Acme site the user is not assigned to, "Test site A" ({{b99f9c84-a4d9-40fe-8f1b-7ef9e38de3f0}}), also exports with 200 (while {{GET /api/sld/b99f9c84-…}} is 422).
```
Also change the Notes build to `index-C14cwR0U.js (re-checked 13 Oct)`.

---

## Cross-cutting notes for the owner
- **Merge the malformed-id 500s.** ZP-4842, ZP-4847 and ZP-4855 are one class. One Low ticket with a table of endpoints is harder to reject than three.
- **CDN masking.** QA's CDN turns 403/404 into `200 text/html`. Any ticket that tells a developer "this is refused cleanly" from the console should say "returns the app page (refused)", or give the 4xx seen on a JSON route. Otherwise developers re-running it see a 200 and get confused (affects ZP-4842's `/api/plans` control, ZP-4843's guest APIs, and the unknown-id cases in ZP-4846/4847).
- **Build.** Every ticket still names `index-COeCBn0z.js`. All findings still reproduce on `index-C14cwR0U.js`.
