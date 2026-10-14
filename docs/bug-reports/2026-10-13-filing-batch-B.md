# Filing batch B: ticket drafts (13 Oct 2026)

- **Status:** drafts only. Nothing filed: no Jira writes from this session. File from abhiyant.singh@.
- **Re-verified on** QA build `index-COeCBn0z.js` (checked with curl at the start and inside every browser run), 13 Oct 2026, ~14:05–14:40 IST.
  Headed Chrome via `pwslot.sh`. Scripts and raw output: `.playwright-mcp/v24/filing-b/` (`a/b/c/d.body.js`, `res_a.json`, `out_*.txt`).
- **New screenshots:** `docs/bug-evidence/2026-10-13-filing-batch/`.
- **Duplicate check (Rovo JQL, cloud egalvanic.atlassian.net):**
  1. `project = ZP AND (text ~ "DescribeExecution" OR text ~ "cicd-devops" OR text ~ "\"Z University\"" OR text ~ "z-university")` → finds ZP-4823 and ZP-4824 (item 4).
  2. `project = ZP AND created >= "2026-07-01" AND (summary ~ "quote*" OR summary ~ "guest*" OR summary ~ "opportunit*" OR summary ~ "\"report config*\"" OR summary ~ "\"reporting config*\"" OR summary ~ "\"site picker\"" OR (summary ~ "search" AND summary ~ "asset*"))` → 50 results. None covers B1, B2, B3, B5, B6, B7 or B8.
  3. `text ~ "quote/by-sld" OR text ~ "by-sld" OR summary ~ "cross-tenant" OR …` → closest is ZP-4709 (To Do), which covers `nodes/sld`, `library-designations` and the lookup routes, but **not** `quote/by-sld`.
- **Fields for all drafts** (same as ZP-4820/4823/4824): Bug · sprint Z-26-10-S1 (1224) · fixVersion Web v2.4 · labels as noted.

| # | Finding | Result today | Draft |
|---|---|---|---|
| B1 | Quotes of another company returned by `quote/by-sld` | **STILL REPRODUCES** (all 4 customer seats) | yes, High |
| B2 | `quote/create-standard` with a malformed site id → 500 | **STILL REPRODUCES** | yes, Low |
| B3 | Flag OFF but typed `/guests` still renders the Guests page | **STILL REPRODUCES** (real flag OFF, not simulated) | yes, Low |
| B4 | Report status shows AWS details / Z University unknown page 500 | **ALREADY FILED**: ZP-4823 (In Progress), ZP-4824 (To Do) | none |
| B5 | ZP-3424: Assets search text restored but not applied | fresh today | yes, Medium |
| B6 | ZP-4680 B1: picker shows the selected site instead of "No sites match" | fresh today | yes, Low |
| B7 | ZP-4680 B2: `/reporting/config/not-a-uuid` blank page + API 500 | fresh today | yes, Low |
| B8 | ZP-4777 B1: `GET /api/opportunity/not-a-uuid` → 500 | fresh today (screenshot added) | yes, Low (optional) |

---

## B1 — High (labels `security`, `api`, `backend`; link "relates to" ZP-4709)

**Title:** [Web] Any customer user can read another company's quotes by asking for quotes of that company's site

**Re-verify result:** STILL REPRODUCES on `index-COeCBn0z.js`. Same 6 quote ids for PM, AM, EE and FM (all `is_eg_admin: false`), byte-for-byte the same as the staff seat's answer.

**Steps**
1. Sign in to https://acme.qa.egalvanic.ai as the Project Manager (abhiyant.singh+project@egalvanic.com, not staff).
2. Open **Quotes** (https://acme.qa.egalvanic.ai/opportunities) → **New Quote** → type `Saveland` in **Facility**. It says **No options**: that site belongs to another company, so the app never lets this user pick it.
3. In the browser console on the same page, run:
   `fetch('/api/quote/by-sld/f86052b7-1683-431f-a86f-f52c6dd5be39/lightweight').then(r=>r.json()).then(console.log)`
4. Look at the answer.

**Actual:** 200 with that company's 6 quotes (ids `2bf46cd8…`, `2936d015…`, `6dafbd3e…`, `98e3a507…`, `5e9c2486…`, `413ecf11…`): title, status, type, total, date. The full version `GET /api/quote/by-sld/f86052b7-…` also returns the description, scope of work, contact ids, site name, opportunity name, totals and hours.
Same result for the Account Manager, Electrical Engineer and Facility Manager seats.

**Expected:** 422 "permission denied", the same answer the app already gives for this site on `GET /api/sld/f86052b7-…` and `GET /api/opportunity/by-sld/f86052b7-…`.

**Why it matters:** one customer can read another customer's prices and scope of work just by knowing a site id.

**Controls (same seat, same minute):** own site `75fc21d3…` → 200, 2 quotes (works) · random id → 200, 0 quotes · `GET /api/sld/f86052b7…` → 422 · `GET /api/opportunity/by-sld/f86052b7…` → 422 · site `f86052b7…` is **not** in Acme's own site list (Acme has 281 sites; it belongs to company `fdcadc86…`).

**Screenshots**
- `docs/bug-evidence/2026-10-13-filing-batch/b1-1-pm-new-quote-facility-other-company-site-not-offered.png` (the PM cannot choose the site)
- `docs/bug-evidence/2026-10-13-filing-batch/b1-2-pm-quote-by-sld-other-company-proof.png` (results for 4 customer seats plus controls; quote names hidden, ids only)

**Severity:** High · **Assignee:** Avani (backend)

**For the developer:** `quote/by-sld/<sld_id>` and `/lightweight` do not use `@require_sld_access` (or the caller-company check) that `sld/<id>` and `opportunity/by-sld/<id>` already use. The web app calls `quote/by-sld` (`fetchQuotesBySLD` in the Planning chunk), so the endpoint is live. ZP-4709 (To Do) is the same kind of gap on `nodes/sld` and `library-designations`; Avani's note there says ~25 more `sld_id` routes need the same audit. This route should be added to that audit, but it is a different endpoint and is not covered by ZP-4709 today.

---

## B2 — Low (labels `api`, `backend`)

**Title:** [Web] Creating a legacy quote with a malformed site id returns a server error (500) instead of "invalid site"

**Re-verify result:** STILL REPRODUCES. Two malformed ids both → 500 `internal_error`. Nothing was created.

**Steps**
1. Sign in to https://acme.qa.egalvanic.ai as the Project Manager and open **Quotes**.
2. In the browser console run:
   `fetch('/api/quote/create-standard',{method:'POST',headers:{'Content-Type':'application/json','X-Direct-Write':'true'},body:JSON.stringify({sld_id:'not-a-uuid',title:'QA-DEMO probe (delete me)'})}).then(r=>console.log(r.status))`
3. Repeat with `sld_id:'12345'`.

**Actual:** 500 `{"error":"internal_error","trace_id":…}` both times.
**Expected:** 400 "invalid site id" (or 404). The newer `POST /api/plans` with the same bad id already refuses it cleanly.

**Who sees it:** nobody in normal use (the Create Quote dialog only offers real sites). It shows up as a 500 in error monitoring whenever a caller sends a bad id.

**Screenshot:** `docs/bug-evidence/2026-10-13-filing-batch/b2-1-create-standard-malformed-id-500.png`
**Severity:** Low · **Assignee:** Avani (backend) · Same class as ZP-4820 / ZP-4824 (500 on bad input).

---

## B3 — Low (labels `frontend`; link "relates to" ZP-4585)

**Title:** [Web] With guest access turned off, typing /guests still opens the Guests admin page with an "Invite guest" button

**Re-verify result:** STILL REPRODUCES with the **real** flag value: `feature-guest-access` = **false** for Acme on QA (live evalx, flag version 2; 13 Oct). Not simulated.

**Steps**
1. Sign in to https://acme.qa.egalvanic.ai as Super Admin (or the Admin seat).
2. Click **Admin** in the left rail. Under **People** there are only **Platform Users** and **Guest Portal Users**. There is no **Guests** item (correct while the feature is off).
3. Type https://acme.qa.egalvanic.ai/guests in the address bar.

**Actual:** the **Guests** page opens with an **Invite guest** button, a Search box and an empty table (Name, Email, Their company, Role, Sites, Status). The guest APIs behind it are refused (`/api/guest-access/guests` and `/roles` → not found), so no data is shown and no invite can be sent.
**Expected:** while guest access is off, `/guests` shows the normal "not available" / Access Denied page, the same as the hidden menu item.

**Screenshots**
- `docs/bug-evidence/2026-10-13-filing-batch/b3-1-superadmin-flag-off-admin-menu-no-guests.png` (menu: no Guests item)
- `docs/bug-evidence/2026-10-13-filing-batch/b3-2-superadmin-flag-off-typed-guests-page-renders.png` (typed URL: page opens)
- Same for the Admin seat: `b3-3-admin-flag-off-admin-menu-no-guests.png`, `b3-4-admin-flag-off-typed-guests-page-renders.png`

**Severity:** Low (no data shown; page shell only) · **Assignee:** Krunal (frontend)
**For the developer:** the menu item uses `show: <guest-access flag> && users.manage`, but the route is `path:"/guests"` wrapped only in `permission:"features.settings.users.view"`. Put the route behind the same guest-access flag. Same nav-vs-route class as ZP-4138.

---

## B4 — ALREADY FILED (no new ticket)

- Draft 1 (report status shows AWS account number / IAM user / region) = **ZP-4823** "[Web] Report status check shows internal AWS details (account number, IAM user, region) in its error message", status **In Progress**.
- Draft 2 (Z University unknown page id → 500) = **ZP-4824** "[Web] Z University: asking for a page that does not exist returns a server error (500) instead of "not found"", status **To Do**.
- Not re-tested today (already filed, per instruction).

---

## B5 — Medium (labels `frontend`; link "relates to" ZP-3424)

**Title:** [Web] Assets: after opening an asset and going back, the Search box still shows the text but the list is no longer filtered

**Result:** fresh today (ZP-3424 verdict, build `index-COeCBn0z.js`, 13 Oct 13:53 IST).

**Steps**
1. Sign in to https://acme.qa.egalvanic.ai as the Project Manager, select site **Richmond, CA**, open **Assets** (https://acme.qa.egalvanic.ai/assets).
2. Type `Generator` in **Search Assets…**. The list shows 12 generators (1–12 of 12).
3. Click any asset row to open it.
4. Press the browser **Back** button (or go to Issues and back to Assets from the sidebar).

**Actual:** the Search box still says **Generator**, but the list shows **all 408 assets**. The user thinks they are looking at generators. Typing one more character applies the search again.
**Expected:** the box and the list agree: either the 12 generators come back (like sort and filters do), or the box is cleared.

**Screenshots**
- `docs/bug-evidence/2026-10-12-v24-qa/zp-3424/QA-13Oct-ZP-3424-14-PM-search-Generator-before-opening-asset-PASS.png` (before)
- `docs/bug-evidence/2026-10-12-v24-qa/zp-3424/QA-13Oct-ZP-3424-15-PM-search-box-says-Generator-but-list-shows-all-assets-FAIL.png` (after Back)

**Severity:** Medium · **Assignee:** Krunal (frontend)
**For the developer:** the shared grid restores the box text from sessionStorage (`mainDataGrid_assets_searchQuery`), but the Assets page keeps the applied search in its own state, which starts empty, so the list call goes out with no `search=`. Restoring search from the URL (`q=`) would fix this and make shared links complete.
**JQL duplicate check:** closest is ZP-3652 (Forms list, opposite symptom, Done). Nothing for Assets.

---

## B6 — Low (labels `frontend`; link "relates to" ZP-4680)

**Title:** [Web] Report config Site picker: a search with no match shows the current site instead of "No sites match"

**Result:** fresh today (ZP-4680 verdict, build `index-COeCBn0z.js`).

**Steps**
1. Sign in as Super Admin and open https://acme.qa.egalvanic.ai/reporting/config/0eee082a-af4d-4805-9ae0-c19d3996af9a
2. In the preview **Site** picker, pick **test site**.
3. Click the picker again and type `qa-nomatch-zzqq` (or `z - `).

**Actual:** the list shows "test site · 14 Nov 2025" as the only row for a search that matches nothing. For `z - ` it shows "test site" first, then "Z - Hospital".
**Expected:** "No sites match "qa-nomatch-zzqq"". The current pick should not appear as a search result (or should be marked as the current choice).

**Screenshots**
- `docs/bug-evidence/2026-10-12-v24-qa/zp-4680/QA-13Oct-ZP-4680-5-no-match-shows-selected-site-not-empty-state-FAIL.png`
- `docs/bug-evidence/2026-10-12-v24-qa/zp-4680/QA-13Oct-ZP-4680-6-search-z-finds-z-hospital-but-current-site-pinned-FAIL.png`

**Severity:** Low · **Assignee:** Krunal (frontend; Avani wrote PR #1748)
**For the developer:** PR #1748 always adds the selected option to `options` when the search doesn't return it, so `noOptionsText` can never show. The same happens on WO-level configs.
**JQL duplicate check:** none.

---

## B7 — Low (labels `api`, `backend`, `frontend`)

**Title:** [Web] Opening a report configuration link with a bad id shows a blank page (the server answers 500)

**Result:** fresh today (ZP-4680 verdict, build `index-COeCBn0z.js`).

**Steps**
1. Sign in as Super Admin.
2. Open https://acme.qa.egalvanic.ai/reporting/config/not-a-uuid (for example, a mistyped or cut-off link).

**Actual:** the page stays blank with no message. `GET /api/reporting/configs/not-a-uuid` (and `…/preview-entities`) → 500 "An internal error occurred.", and the page sends Sentry events. A well-formed id that doesn't exist also gives a blank page.
**Expected:** the API answers 400/404, and the page says "Report configuration not found" with a link back to the Builder.

**Screenshot:** `docs/bug-evidence/2026-10-12-v24-qa/zp-4680/QA-13Oct-ZP-4680-12-malformed-config-id-in-url-api-500-FAIL.png`
**Severity:** Low · **Assignee:** Avani (500) + Krunal (empty state)
**JQL duplicate check:** ZP-3347 (502 on `/reporting/config/…`, Done) is a different error. Nothing covers this.

---

## B8 — Low, optional (labels `api`, `backend`)

**Title:** [Web] Looking up an opportunity with a malformed id returns a server error (500) instead of "not found"

**Result:** fresh today (ZP-4777 verdict), re-captured with a screenshot at ~14:40 IST on `index-COeCBn0z.js`.

**Steps**
1. Sign in as the Project Manager.
2. Type https://acme.qa.egalvanic.ai/opportunities/not-a-uuid. The app redirects to the Quotes list (correct; the user sees no error).
3. In the console: `fetch('/api/opportunity/not-a-uuid').then(r=>console.log(r.status))`. Also try `undefined`, `null`, `123`.

**Actual:** 500 "An internal error occurred." for all four. A well-formed unknown id is refused cleanly (not found).
**Expected:** 400 "invalid id" or 404, like the 24 other routes checked under ZP-4777.

**Who sees it:** nobody in the UI today. Only error-monitoring noise. File it only if the owner wants API-only 500s ticketed (as was done for ZP-4824).

**Screenshot:** `docs/bug-evidence/2026-10-13-filing-batch/b8-1-opportunity-malformed-id-api-500.png`
**Severity:** Low · **Assignee:** Avani (backend)
**JQL duplicate check:** none for `/opportunity/{id}`.
