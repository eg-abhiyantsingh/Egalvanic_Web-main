# Deep review A: ZP-4840, 4841, 4842, 4843, 4844, 4845, 4846, 4848 (14 Oct 2026)

I reviewed each rewritten ticket twice: once as a skeptical developer trying to reject it, and once as a non-technical manager reading only the title and the Simple explanation. **No Jira writes were made.** The main session applies the corrections in `docs/bug-evidence/2026-10-14-deep-review/payload-A.json`.

**How the review was done**
- **Live tickets.** I read all 8 with getJiraIssue. None had comments, all are To Do, and all were last updated 14 Oct 03:54 CDT by the rewrite. All 35 image attachments were viewed (every embedded image, plus b1-1, which is attached but not embedded). The local copies match the Jira attachments byte for byte.
- **Builds.**
  - QA `index-G6uKkjPx.js` was live at the start; all screenshots come from it.
  - QA was redeployed to **`index-D-HsTlcR.js`** about 15:30 IST, during the review.
  - **Frontend on the new build:** a hash-normalised diff shows the grid, report-editor and SLD chunks have no code change. The work-order page chunk changed, but the bulk add still sends all ids in one call and still shows the page-level error.
  - **Server on the new build:** every server-side claim was re-run on `index-D-HsTlcR.js` (run `a7`) with the same results.
- **Seats.**
  - Project Manager `abhiyant.singh+project@`: `is_eg_admin=false`, 116 sites.
  - Super Admin `abhiyant.singh+admin@`: `is_eg_admin=true` (staff). Used only where a ticket uses it, and to check site ownership.
- **Flags (live LaunchDarkly, Acme on QA):**
  - `feature-guest-access=false`
  - `feature-planned-work-v2=false`
  - `feature-reporting-v2=true`
  - `feature-skm-export=true`
- **Runs.** All were headed `pwslot.sh` runs on QA only; scripts are in `.playwright-mcp/v24/deep-review-a/`:
  - `x0`: work-order discovery
  - `a1`: ZP-4840 mechanism and ZP-4844
  - `a2`, `a3`: ZP-4841, ZP-4842, ZP-4848, and /guests as the Project Manager
  - `a4`: search for an open work order with more than 500 assets
  - `a5`: ZP-4840 UI flow with a simulated request size
  - `a6`: proof that no probe quote was created
  - `a7`: server re-check on the new build
  - `s1`, `s2`: Super Admin checks for ZP-4843, ZP-4845, ZP-4846 and site ownership
- **Data touched.**
  - No site, asset or work-order data changed; the work-order counts were identical before and after.
  - The add-assets probes used random asset and service ids.
  - The `create-standard` probes used malformed ids, and nothing was created: the company quotes list has no "probe" title.
  - The `export-skm` calls created export files in QA storage only: 4 from the PM (5 calls, one refused) and 2 from the Super Admin. Nothing in any site changed.
  - Production was not opened.
- **Evidence:** `docs/bug-evidence/2026-10-14-deep-review/` holds the new highlighted images, with the raw frames in `raw/`.

| Ticket | Verdict | One line |
|---|---|---|
| ZP-4840 | **FIX** | Valid; mechanism re-proven on QA. But the steps send developers to a customer's production work order, step 2 says "Edit" where the button is "Bulk Ops", and the title says "hundreds" where the limit is 500. |
| ZP-4841 | **FIX** | Valid, High. But the text pins 116,134.11 on "Transformer-Repair" (it is 150.00; 116,134.11 is an abandoned EMP quote), the key image claims prices that it does not show, and the PM has no Quotes menu item for step 2. |
| ZP-4842 | **FIX** (small) | Valid: still 500. Remove the unverified "same as a well-formed id" comparison and the cluttered key image. The text says Low but the priority field is Medium. |
| ZP-4843 | **FIX** (small) | Valid. The example "/reporting" is wrong (Reporting is ON for Acme and opens normally); /planning-board is the real switched-off example. Say that it is admin-only. The text says Low but the field is Medium. |
| ZP-4844 | **OK** | Reproduced exactly on the current build: 408, then 12, then Back gives "Generator" in the box with 408 rows (CB 451, CT 450, Panelboard-29). All numbers match the images. |
| ZP-4845 | **OK** | Reproduced exactly: "test site" is the only row for `qa-nomatch-zzqq`, and "test site" comes before "Z - Hospital" for `z -`. The expected message format exists in the code. |
| ZP-4846 | **FIX** (small) | Valid. "No way back" is an overclaim (the side menu and Back still work). The well-formed-id case has no full URL, and the target list is named in two different ways. |
| ZP-4848 | **FIX** | Valid, High; 375/193 is exact. But the step-2 screenshot is from the Super Admin, not the PM, and the PM has no SLD menu item. Also "working download link" is unverified, and the title wording is awkward. |

No ticket is INVALID-NOW. All 8 still reproduce on `index-D-HsTlcR.js`.

---

## ZP-4840: adding a service to more than 500 selected assets breaks the work order page (High)

**What I checked live** (Project Manager, `is_eg_admin=false`)
- **Server cap, still in place.** On QA-DEMO WO `22a22b9c-…` with random ids:
  - 501 ids → `400 "at most 500 assets per request"`
  - 500 ids → `400 "no addable assets…"`
  - Same result on `index-D-HsTlcR.js`.
- **Code, both builds.**
  - The bulk Manage Services > Add handler sends `Xt.map(b=>b.id)` (all selected ids) in ONE `add-assets` call.
  - Its `catch` sets the page-level error state (`[vl,wt]=useState(null)`), and `if(vl) return <error + Back to Work Orders>` replaces the whole view.
  - Bulk remove loops one request per asset; the ticket's note about this is correct.
- **Real UI flow on QA (run `a5`, nothing written).**
  - Path: QA-DEMO work order > **Bulk Ops** (the button then reads **Cancel**) > header checkbox > **Select all 222** > **Services** > "Manage Services — 222 assets" > Cleaning > Add.
  - The app sent all **222 real ids in one call**. I intercepted it and forwarded it to the server as 501 random ids.
  - The server answered 400 "at most 500 assets per request", and the app **replaced the whole page** with "Failed to add service: API call failed: 400 Bad Request - …" plus Back to Work Orders.
  - Work-order counts were unchanged afterwards (214 assets).
- **No open QA work order has more than 500 assets.** There are 97 open ones; the largest has 342. The >500 ones, e.g. AutoTest_WO_85105 with 1,076 assets on "test site", are all closed.

**Images**
- The key image and Screenshots 1 and 2 are production frames (Bass Electric). The labels match the pixels: 1,512 assets, the "at most 500" text.
- The key banner "Same code on QA web v2.4" is true: confirmed in the code of both QA builds.
- The mechanism image (13 Oct) matches today's answers.
- No secrets are visible.
- **Gap:** no QA screenshot of the failing screen existed. Added: `ZP-4840-3-QA-14Oct-same-error-replaces-work-order-page.png`, labelled SIMULATED request size.

**Links:** the QA work-order link opens "QA-DEMO ZP-4801 Tuned column (delete me)". The production link was not opened (QA only).

**Problems (FIX)**
1. **Steps 1–4 point developers at a customer's production work order and tell them to click Add.** If the fix ships, following them would add Cleaning to 1,512 real customer assets. The steps must be on QA, with production kept only as "first seen, do not repeat".
2. **Step 2 says "click Edit".** The button that starts selection is **Bulk Ops** (it turns into Cancel). "Edit" is a separate bulk action that appears after selection, on both QA and production.
3. **The title says "hundreds of assets".** It only fails above 500, and a developer testing 300 assets would close it as "cannot reproduce". New title: "…on a work order with more than 500 assets…".
4. **The role line "a work-order user on production company Bass Electric (seen live)" is vague**, and the Environment line says QA while the steps are on production.
5. **No guidance on how to get a >500-asset open work order on QA.** QA has none. Now stated, with the 1,076-asset "test site" example.

**Payload:** new title, QA steps with the correct labels, two new QA screenshots, production kept as background, and the build lines updated.

---

## ZP-4841: a customer can read another company's quotes (High)

**What I checked live** (PM, `is_eg_admin=false`; the foreign site is not in the PM's 116 sites)
- **The leak, re-run on both builds.** `GET /api/quote/by-sld/f86052b7-…` → 200 with **6 quotes**. Totals:

| Quote | Status | Type | Total |
|---|---|---|---|
| "EMP for SLD f86052b7…" | abandoned | EMP | **116,134.11** |
| **"Transformer-Repair"** | accepted | standard | **150.00** |
| EMP | accepted | EMP | 120,411.81 |
| "New Quote - 2025-09-20" | abandoned | standard | 450.00 |
| "New Quote - 2025-09-19" | abandoned | standard | 45.00 |
| EMP | accepted | EMP | 113,191.64 |

- **Fields in the response.**
  - `sow`, `contact_ids` and `total_hours` are present but empty (0) on all 6.
  - `description` is filled on 3.
  - `sld_name` is null.
  - `opportunity_name` is filled.
  - `/lightweight` also returns 6.
- **Controls.**
  - `sld/<id>` → 422.
  - `opportunity/by-sld/<id>` → 422.
  - Own site `aadcee4c` ("Android Site 2") → 17 quotes.
  - Random id → 0.
- **"Another company" is solid.**
  - Saveland is not in Acme's `/api/company/<acme>/slds` list.
  - Even the Acme Super Admin gets 422 on it.
  - Memory records its owner as company `fdcadc86…`.
- **New Quote > Facility:** "Saveland" gives "No options"; "Richmond" lists "Richmond, CA".
- **The PM's menu has no Quotes item.** There are 0 links to /opportunities in the menu (no `features.opportunities.view`), but typing the address opens the page.
- **Code:** the only caller of `quote/by-sld` is the Planning page (/planning), when one site is picked.

**Images**
- **Key image:** it is captioned "Another company's 6 quotes come back (titles, status, **prices**)", but the overlay shows **no prices** (the total column is blank). This is a caption/pixel mismatch, so it was replaced.
- **Screenshot 3 (b1-2)** uses a different own-site control (75fc21d3, 2 quotes) from the text (aadcee4c, 17) and an older build. It is redundant, so it was removed from the description.
- **Screenshot 1** is fine, but it had no highlight; replaced with a highlighted copy.

**Links:** /opportunities works by typed URL. The site ids are correct.

**Problems (FIX)**
1. **Wrong number.** "one quote, 'Transformer-Repair', status accepted, shows a total of 116,134.11" is false. Transformer-Repair = 150.00, and 116,134.11 belongs to an abandoned EMP quote. A developer would catch this immediately.
2. **Overclaim:** "scope of work, contacts … hours" are leaked. Those fields are empty on all 6 quotes, and the title promised "scope of work". New wording: titles, status, prices and opportunity names, with the other fields named as present but empty on these quotes.
3. **Key image mismatch** (no prices visible): replaced by `ZP-4841-key-highlighted-v2.png`, which shows all 6 totals.
4. **Step 2, "Click Quotes in the left menu", is impossible for this role.** Changed to typing the address.
5. **The title ("…by asking for that company's site") is unclear to a manager.** New title: "…including prices, just by knowing that company's site id".

---

## ZP-4842: create-quote with a broken site id gives 500 (API only)

**What I checked live:** as the PM, `create-standard` with `not-a-uuid` and with `12345` both give **500 `internal_error`** with a trace id, on both builds. No probe quote exists in the company quotes list (79 checked).

**Problems (FIX, small)**
1. **"…(the same as for a well-formed id the user is not allowed to use)"** (Simple explanation and Expected) was never verified.
   - A well-formed foreign id would most likely get `422 permission_denied` "You do not have permission…", not "invalid site id".
   - I did not run it, because it is a write probe against another tenant.
   - Removed. Expected is now "A clear 'invalid site id' answer (400)".
2. **The key image includes ZP-4847's opportunity lines and a confusing `/api/plans` "control"** (400 "title is required"). Screenshot 2 (b2-1) shows a different `/api/plans` result (200 HTML).
   - A developer re-running the "control" sees a different endpoint failing for a different reason.
   - Replaced with a clean, fresh key image of only the two 500s. b2-1 and the 13 Oct mixed image are no longer embedded.
3. **The Notes say "Low" while the Jira priority is Medium.** The word was removed from the text. **Main session:** the owner's rule (API-only = Low) points to Low. Ask before changing the field.
4. The step-1 screenshot is now the highlighted "No options" frame; the steps say to type the Quotes address.

---

## ZP-4843: /guests opens although guest access is off

**What I checked live** (Super Admin; the flag value is real)
- Admin > People shows only Platform Users and Guest Portal Users.
- `/guests` opens with Invite guest and "No rows"; still the same on `index-D-HsTlcR.js`.
- The guest APIs return the masked `200 text/html`.
- **Control:** `/planning-board` (flag `feature-planned-work-v2=false`) shows "Feature Not Available".
- **`/reporting` is NOT switched off for Acme.** It opens "Reporting – Reports — Coming Soon".
- **As the PM,** `/guests` shows "Access Denied": the route needs `features.settings.users.view`, which only Admin-type roles hold.
- **Code:** the menu item is `show: guest-access && users.manage`, while the route only checks `features.settings.users.view` (unchanged on both builds).

**Problems (FIX, small)**
1. **"…the same as other switched-off pages (for example /reporting)" is wrong for Acme.** A manager who opens /reporting sees a normal page. Replaced with /planning-board, which is really off, plus a new green control screenshot.
2. **Unclear who can reach the page.** Now says "only admin users can open this page; the Project Manager gets Access Denied". This stops a developer from rating it as "any user".
3. **"Low" in the Notes vs Medium in the field.** Same as ZP-4842: removed from the text, flagged for the owner.
4. **Four step images were two duplicate pairs** (b3-1/ZP-4843-1, b3-2/ZP-4843-2). Trimmed to one highlighted menu image, the key image, and the control image.

The expected mock-up is good and clearly labelled, so it was kept.

---

## ZP-4844: Assets search box keeps its text after Back, but the list is unfiltered (OK)

**What I checked live** (PM, Richmond, CA)
- Before searching: "1–25 of 408". Searching "Generator" gives "1–12 of 12".
- Opened Generator-14, then pressed Back: the box still says "Generator", the footer shows "1–25 of 408", and the first rows are CB 451, CT 450, Panelboard-29. This is exactly the ticket.
- **Code:** the grid restores the box from `sessionStorage` `mainDataGrid_<grid>_searchQuery`. The chunk is unchanged on the new build.

**Review:** the images, captions and numbers all match, and the mock-up is labelled. The title and Simple explanation are clear to a manager, and the links work.

**Optional:** add "re-checked 14 Oct on index-G6uKkjPx.js / index-D-HsTlcR.js, same numbers" to Test data. No payload.

---

## ZP-4845: report config Site picker shows the current site for a no-match search (OK)

**What I checked live** (Super Admin, config `0eee082a-…` "Annual Maintenance Report (Copy)")
- The picker starts on "Performance sld". I picked "test site · 14 Nov 2025".
- `qa-nomatch-zzqq` → the only row is "test site · 14 Nov 2025".
- `z - ` → "test site", then "Z - Hospital".
- **Code (unchanged on the new build):**
  - `options = selected && !results.includes(selected) ? [selected, …results] : results`
  - `noOptionsText = No ${type}s match "${q}"`, so the mock-up text is correct.

**Review:** all images are accurate. The issue is cosmetic, which is honest at Low. Developer pushback ("MUI needs the value in options") is answered by the ticket's "or clearly mark it" option.

**Optional:**
- Step 2 could name the option exactly ("test site · 14 Nov 2025"). Several similar names exist: "Test site", "Test site 4/12", "test site 6-10".
- Any picked site shows the same bug: even typing "test site" lists "Performance sld" first.

No payload.

---

## ZP-4846: report config link with a wrong or deleted id shows a blank page

**What I checked live** (Super Admin)
- `/reporting/config/not-a-uuid` gives a blank page; the API answers 500 for both the config and `preview-entities`.
- `/reporting/config/0b3f5d3e-…` (well-formed, no such config) gives a blank page; the API returns the masked `200 text/html`.
- **Control:** config `0eee082a-…` opens normally.
- The report list is at `/reporting/builder` ("Report Builder").
- Both checks repeated on the new build with the same results.

**Problems (FIX, small)**
1. **"no way back from the page" (Simple explanation and key image caption) is an overclaim.** The whole side menu and the browser Back button still work. Now "no message and no link back to the list of report configurations".
2. **The realistic case (a deleted configuration's link) had no URL** ("a well-formed id that does not exist gives the same blank page"). The full URL is added and leads the steps.
3. **The target list was named "list of configurations" in one place and "the Builder" in another.** Unified as Report Builder, with its URL.
4. **New positive control step** (a real config opens). The key image was replaced.

The expected mock-up is fine and was kept.

---

## ZP-4848: a customer can export another company's full SKM engineering file (High)

**What I checked live** (PM, `is_eg_admin=false`)
- **Other company's site** (Saveland Avenue Lofts):
  - `export-skm` → 200, `success:true`, `presigned_url`, **375 / 193**.
  - `GET sld/<id>` → 422.
- **"Test site A"** (b99f9c84, Acme, not assigned; confirmed as an Acme site by the Super Admin): export → 200 with a link (7 / 0); `GET` → 422.
- **Controls:** own site "Android Site 2" → 200 (470 / 189); random id → 400 "SLD not found".
- **Re-checked on the new build:** 200, 375 / 193.
- **The PM has no SLD menu item** (lacks `features.slds.view`). Typing `/slds` opens the SLD page, and its Export menu does show **Export Engineering XML**.

**Problems (FIX)**
1. **Screenshot 1 (`4848-0-…`) is from the Super Admin** (avatar "aa", Engineering rail), while step 2 says Project Manager. A developer can say "your proof is an admin". Replaced with the PM's own SLD Export menu, highlighted.
2. **Step 2, "Open the SLD page", does not say how.** This user has no SLD menu item; the step now says to type the address.
3. **"a working download link": QA never opened the file**, so "working" is not proven. Changed to "a download link".
4. **The title "although they are refused to open that site" is awkward English.** New: "even though they are not allowed to open that site".
5. The key image was rebuilt from the current build, with red and green boxes on the exact lines. The old one said build COeCBn0z in its table.

---

## For the main session
- **Priority fields.** ZP-4842 and ZP-4843 say "Low" in the text but Medium in Jira. The payload drops the word, and nothing else changes on its own.
  - Recommended: set both to Low (API-only; admin-only dead page).
  - Ask the owner first, per the "ask before every extra Jira change" rule.
- **Old misleading attachments stay on the tickets** after the update. They are no longer embedded, but they are still listed.
  - `ZP-4841-key-highlighted.png`: claims prices it doesn't show.
  - `ZP-4846-key-highlighted.png`: "no way back".
  - `4848-0-sld-export-menu-export-engineering-xml.png`: Super Admin.
  - `ZP-4842-key-highlighted.png`, `b2-1-…`: confusing control.
  - Consider removing them, with the owner's OK.
- **Side observation, not filed (needs a check first).** On "QA-DEMO ZP-4801" the Assets grid says "1–20 of **214**", but Select all and the dialog say **222** assets (the summary API reports `assets 214`, `asset_nodes 222`). It may be child components counted in.
- **Side observation, likely by design.** The PM has no Quotes or SLD menu items, but `/opportunities` and `/slds` open by typed address.
