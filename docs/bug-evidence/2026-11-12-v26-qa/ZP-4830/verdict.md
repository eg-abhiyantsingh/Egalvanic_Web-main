# ZP-4830 · Collaborators: subcontractor & customer company links (QA, 12 Nov 2026)

**Verdict: FAIL** on QA, Web v2.6 build `index-C1ZRIunF.js` (checked 16:01, 16:16, 16:35, 16:42 and 17:04 IST, unchanged).
The pages, menus, sending a request and role gating work. But the row buttons on **Requests** and **Company Links** use the wrong object:
- Withdraw is broken. This was re-proved twice on real data.
- Because of the same defect, a receiving company never gets **Accept / Decline**, so no link can ever be accepted in the UI. Shown with SIMULATED rows and in the code.

Seats used:
- Super Admin (SA, staff, is_eg_admin true).
- Staff Admin.
- PM, AM, EE, FM and CP (customer seats, is_eg_admin false).

LaunchDarkly `feature-guest-access` = **true** for Acme (live evalx, 16:03 IST).

## Checked
| Check | How | Result |
|---|---|---|
| Collaborators menu entry | SA: Admin › People | PASS: "Collaborators" sits under People (screenshot 1) |
| `/guests` redirects | typed `/guests` | PASS: lands on `/collaborators` (SA, ADMIN, PM, AM, EE) |
| People / Company Links / Requests grids | SA | PASS: 3 tabs, MainDataGrid, empty states read well |
| New company link dialog | SA: Company Links › New company link | PASS: Customer / Subcontractor radio, "Your record" picker (140 customers / 11 subcontractors), email, Send request disabled until the email is valid |
| Send a subcontractor request | QA-DEMO sub "QA-DEMO ZP-4830 12 Nov sub" + a non-deliverable `.invalid` address | PASS: `POST /api/vendors/{id}/link` 201, toast "Request sent", Requests row = email · Subcontractor link · Sent · record · date (screenshots 2, 3) |
| Pending invite never names the company | the response from 3 different `.invalid` addresses (one `@acme.invalid`), plus GET `/api/subcontracting` | PASS: `sub_company_id/sub_company_name` null and the same response shape for all 3 |
| Customer link request (API the dialog uses) | `POST /api/accounts/{QA-DEMO ZP-4561 customer}/company-link` | PASS: 201 pending, `facility_company_name` null; DELETE 200 |
| **Withdraw a sent request (Requests tab)** | Withdraw icon › confirm. Ran twice: 16:28 and 16:36 IST | **FAIL**: the dialog reads "undefined's company can no longer accept it." Withdraw sends no request and shows an "Error · Something went wrong" dialog (console `[UserError] Operation failed`). The request is still listed after a reload (screenshots 4, 5) |
| **Incoming request: Accept / Decline** | SIMULATED: GET `/api/subcontracting` rewritten to add an invite from "SIMULATED Prime Co" | **FAIL (simulated)**: the row offers only "Withdraw" and no Accept / Decline (screenshot 6) |
| **Active link actions** (Manage / Shared SLDs / People / Unlink) | SIMULATED active sub link | **FAIL (simulated)**: only "End link" (a sub link should say "Unlink"). Clicking it throws `TypeError: Cannot read properties of undefined (reading 'account_name')` and no dialog opens (screenshot 7) |
| Root cause (code read, supporting) | `Collaborators-DnxEjmd4.js` | MainDataGrid passes MUI params `{row,…}` to `renderRowActions`. The People grid destructures `({row})`, but Requests and Company Links use the params as the row, so `incoming`, `kind`, `party` and `link` are all undefined |
| Workaround path | Admin › Subcontractors › the sub › link / unlink (`DELETE /api/vendors/{id}/link`) | PASS: 200. Used for clean-up, so no pending request is left behind |
| Roles: nav vs typed URL vs API | PM, AM, EE | PASS: no menu entry; `/collaborators` shows Access Denied; `GET /api/subcontracting` 403 `admin_only`; `POST /vendors/{QA-DEMO}/link` 422 `company_data.manage`; accepting a random link 403 |
| Roles: portal seats | FM, CP | PASS: typed `/collaborators` and `/safety/*` redirect to the portal; `/guest-access/*` and `/subcontracting` 422 |
| Roles: staff Admin | ADMIN | PASS: menu entry plus all tabs |
| Input validation (API) | `POST /api/vendors/{QA-DEMO}/link {"email":"not-an-email"}`, run at 16:28 and again at 16:59 IST | **FAIL (minor)**: 201 both times; a pending link to "not-an-email" was created and removed afterwards. The UI blocks this; the API does not. A second request while one is pending gets 409 `link_exists` (correct) |
| Unknown id | `POST /api/vendors/<random uuid>/link` | PASS: 404 JSON "Subcontractor not found" |
| Flag ON/OFF `feature-guest-access` | real ON (live). OFF is SIMULATED: evalx rewritten and the LD stream blocked | ON: menu entry shown. OFF (SIMULATED): the menu entry is hidden, but typed `/collaborators` and `/guests` still open the full page with "Invite individual" and "New company link", and the API answers 200 (screenshot 8, `QA-12Nov-ZP-4830-8-SIMULATED-flag-off-typed-url-still-opens-FAIL.png`). That is the ZP-4843 pattern again (Done ticket: guests page opened by URL with the flag off). Needs a real flip to confirm |
| Performance | full load of `/collaborators` | PASS: 4 feature calls, 0.5–0.7 s each, under 2 KB; no duplicate app calls; nothing over 2 s |

## Bugs found
1. **Collaborators › Requests / Company Links: the row buttons don't work. Withdraw fails, and a receiving company gets no Accept / Decline.** High, in the ticket's scope (frontend, Eric's PR #1749/#1750).
   - Steps:
     1. Admin › Collaborators › Company Links › New company link › Subcontractor › pick a subcontractor › enter an email › Send request.
     2. Open the Requests tab.
     3. Click the Withdraw icon on the row, then Withdraw.
   - Actual: the dialog says "undefined's company…", then "Something went wrong", and the request stays.
   - Expected: the request is withdrawn.
   - The same cause means incoming rows show "Withdraw" instead of Accept / Decline, and Company Links rows show only "End link", which crashes when clicked.
   - Fix: use `({row})` in both `renderRowActions` callbacks, as the People grid does.
   - Evidence: screenshots 4–7 and the video.
2. **The link-request API accepts a non-email value**: `POST /api/vendors/{id}/link {"email":"not-an-email"}` → 201 pending link. Low, backend: suggest Avani.
3. **SIMULATED, needs confirmation:** with `feature-guest-access` OFF, `/collaborators` (and `/guests`) still opens with Invite and New company link by typed URL. Medium if a real flip confirms it (ZP-4843 regression). Frontend: suggest Krunal.

JQL run (no existing ticket for these):
`project = ZP AND (summary ~ "Collaborators" OR summary ~ "company link" OR summary ~ "Withdraw" OR summary ~ "Safety" OR summary ~ "Construction Manager" OR summary ~ "BCES" OR summary ~ "guest access") ORDER BY created DESC`
That search found ZP-4843 (Done, guest page with the flag off) and ZP-5009 (Done, "() will lose access" copy in the old Guests dialog): same family, different page.

## Enhancement suggestions
- On the Subcontractor / Customer record, show "Pending link to <email> · sent <date>" with Resend / Withdraw. Today you have to know to look in Collaborators › Requests.
- Withdraw dialog copy: name the email ("x@y can no longer accept it").
- Validate the invite email on the server, not only in the dialog.
- Requests tab: add a filter for Sent / Received and show who sent it.

## Mobile impact
Accepted subcontractor members get Technician guest access, and Android ZP-5144 hides site and work-order creation for guest Technicians. Once a link can be accepted, the iOS and Android testers should check a sub member's app:
- the company switcher;
- access disappears after untick or close, and comes back on reopen.

## Not covered (and why)
- **Accepting a link, linked Teams as crews, guest access following assignments** (untick, unassign, single-role replace, close, delete, reopen), **customer SLD sharing / pairing, edit-drawer removal, field-team dialog with nobody ticked.**
  - Why: these need a second QA company with a non-staff admin login, other than the demo tenant (which is excluded). Today the Accept button cannot render anyway (bug 1).
- **Public mailbox / egalvanic.com / "identical response whether or not the email exists" negatives.**
  - Why: these would send invites to deliverable addresses (gmail.com, real @egalvanic.com users). The run was stopped by the safety classifier and not retried. Needs the owner's OK to use owner-controlled addresses.
- **Cross-tenant link ids.**
  - Why: there are no links in any company on QA, so there is no foreign id to probe. A random id gave 404.

## Test data created
- Subcontractor "QA-DEMO ZP-4830 12 Nov sub", whose first contact is a non-deliverable `@acme.invalid` address. Its pending links were all withdrawn through `DELETE /vendors/{id}/link`.
- One customer-link request on "QA-DEMO ZP-4561 customer 16 Oct", deleted.

## Proposed Jira comment
See `ZP-4830.txt`.

## Recommended status
**To Do.** Fix the row-action bug (bug 1). Then re-test the accept flow with a second QA company admin seat. Real LD flip needed: `feature-guest-access`, env QA, company Acme, OFF then back ON, to confirm bug 3.
