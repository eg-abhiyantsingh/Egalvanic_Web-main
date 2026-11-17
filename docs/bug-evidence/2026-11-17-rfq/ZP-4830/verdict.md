# ZP-4830 · Collaborators: subcontractor & customer company links, assignment-driven guest access (QA, 17 Nov 2026)

**Verdict: FAIL → To Do** (1 new High bug in the ticket's own scope; 1 Medium guest-scope bug, separate).

- All four 12 Nov FAILs are fixed: Withdraw, Accept / Decline, End link / Unlink, bad email.
- Linking, crews, guest access gained and lost, close / reopen, customer SLD sharing and negatives all PASS on real data across two companies.
- What fails: **once a linked subcontractor's person is taken off a work order, they can't be put back on that same work order.**
  - Field team › Save returns HTTP 500 `internal_error`. Reproduced 6 times on 4 work orders.
  - The edit-drawer path saves the assignment but never gives the guest access back.
  - The brief's row "untick → access gone; **tick again**" fails.

**Environment**
- QA web build `index-CkkAeA9o.js`, the same on acme and demo. Live from 14:48 IST; rechecked at 15:30 and 15:43. The run started on `index-LGhrn-48.js`.
- Fix PRs:
  - eg-pz-frontend #2042, read: `({ row: r })` in both row-action callbacks.
  - eg-pz-backend #1820, not readable with my GitHub access. It's the invite email check, and that check is verified below.
- Companies: EG-ACME (`acme`, d59d449b…) and Demo Company (`demo`, 93611164…).

**Seats and staff checks**
| Seat | Account | is_eg_admin | Used for |
|---|---|---|---|
| SA | abhiyant.singh+admin@ | true (staff) | EG-ACME admin actions only |
| ADMIN | +adminqa@ | true (staff) | Accepting Link A as the sub admin; an action, not a judgement |
| Demo admin | shubham.goswami@ | true (staff) | Small QA-DEMO actions only |
| PM | +project@ | false | Sub-link guest |
| AM | +accountm@ | false | Customer-link guest |
| EE | +electric@ | false | Old-invite guest; menus |

I didn't use TECH.

**LaunchDarkly `feature-guest-access`, read live from evalx before each ON-dependent row**
| Company | Reads | Value |
|---|---|---|
| EG-ACME | 14:44, 14:48, 15:09 IST | **false** (real OFF) |
| EG-ACME | 15:26, 15:29, 15:32, 15:41 IST | **true**, after the lead asked Shubham to turn it on |
| Demo | every read | true |

- Link A and the customer link depend on the flag of the company receiving guests, which is Demo (ON). They ran for real while EG-ACME was OFF.
- Link B was created only after EG-ACME read true. Nothing was simulated.

## 12 Nov FAILs (comment 46130): re-check
| 12 Nov FAIL | 17 Nov result | Evidence |
|---|---|---|
| Withdraw says "undefined's company" and the request stays | **PASS** (Demo admin, UI). The dialog reads "qa-demo-zp4830-withdraw@example.com's company can no longer accept it." Clicking Withdraw sends `DELETE /api/subcontracting/links/0eab38eb…` → 200, the toast says "Request withdrawn", and the request is gone after a reload | 1, 2, video 1 |
| Incoming request shows no Accept / Decline | **PASS**. EG-ACME admin › Requests: the row "Demo Company · Wants you as a subcontractor · Received" has Accept and Decline. Decline → `POST …/decline` 200, "Request declined", and the row disappears. Demo re-sent (201), Accept → `POST …/accept` 200, "You're now a subcontractor for Demo Company". Customer request (Demo side): Accept / Decline shown, Accept 200 | 3, 11, video 1 |
| Active link shows only "End link", which throws a TypeError | **PASS**. The prime row (Demo) shows **Unlink**; its dialog reads "Unlink QA-DEMO ZP-4830 17 Nov sub (EG-ACME)? EG-ACME's Teams leave your crew pickers…". The sub row (EG-ACME) shows **People + End link**; the dialog reads "Stop subcontracting for Demo Company?…" and the People dialog says "Nobody has access yet". The provider row in Demo shows Shared SLDs + People + End link. No console TypeError. All dialogs were cancelled | 4, 5, video 1 |
| API accepts `{"email":"not-an-email"}` | **PASS**. `POST /api/vendors/{id}/link` returns 400 `invalid_email` "Enter a valid email address." for `not-an-email`, `a@b`, `user@@example.com`, `""`, `x@y`, `qa@example.invalid` and a missing email. The customer-link API gives the same 400 | r03-demo.json, r02-acme.json |
| (12 Nov, simulated) With the flag OFF, typed `/collaborators` still offers Invite | **PASS, now a real OFF check**. The menu entry is hidden. The People tab says "Collaboration isn't turned on for your company yet…" and offers no Invite. `GET /guest-access/guests` → 403 `guest_access_disabled`. `/guests` redirects to `/collaborators`. The Company Links / Requests tabs still show "New company link": a subcontractor link is refused inside the dialog with "Guest access must be enabled for this company to link subcontractors" (403); a customer link is allowed (201). This is consistent: the gate is on the company that receives guests. With the flag ON (15:29) the menu entry and "Invite individual" are back | 9, 15, 17, r02, r09, r20 |

## QA Review rows
| Row | Result | How / evidence |
|---|---|---|
| Prime invites a sub by email; the sub admin sees a pending request and can accept | **PASS** | Demo admin › Collaborators › New company link › Subcontractor › "QA-DEMO ZP-4830 17 Nov sub (EG-ACME)" › EG-ACME ADMIN email → 201. The response keeps `sub_company_name`/`prime_company_name` null and the Sent row shows only the email. EG-ACME's admin menu shows Collaborators because a request is waiting (flag still OFF), then Accept works (above). Link A `14d77ee3…` active 14:55 IST |
| After accept, the sub's Teams appear as crews on a work order | **PASS** | Create Work Order › Assignment › Subcontractor › the linked vendor › the crew list shows all **7 EG-ACME Teams** ("Team · n"). `/crews/assignable` returns them with `linked: true`. WO "QA-DEMO ZP-4830 17 Nov WO (linked sub)" opened on the Field team step with the Team's member PM ticked (6, 7, video 2) |
| A sub member gains Technician guest access only once ticked onto an open WO | **PASS** | PM before the tick: workspaces = EG-ACME only. After the tick: "Demo Company · Guest · Technician" in the switcher (8). In Demo `/auth/v2/me` shows is_guest true, role Technician, 72 permissions (Demo's Technician set, no `platform.web`; the Android tester saw the same 72 for TECH), `field_limits` can_create_sites false / can_create_work_orders false, `accessible_sld_ids` = the WO's site only |
| …loses it on **untick** | **PASS** | Field team saved with nobody (UI and API) → workspaces = EG-ACME only, `POST /guest-access/switch` → 404 `not_found`, Demo's `/me` falls back to the home company (r16 E2, r18 0b, 10) |
| …loses it on **unassign** | **PASS** | `DELETE /users/{PM}/session-assignments/{wo}?mapping_type=field_technician` → 200 "Assignment removed", and access is gone (r16 D) |
| …loses it on **close**; **reopening restores it** | **PASS** | Close (`PUT /ir_session/update` active false) → access gone. Reactivate → access back, and the WO lists again (r16 C2/C3) |
| …**tick again** (brief) | **FAIL → BUG-retick-500** | Tick → untick → tick on the same WO: Field team › Save → 500 `internal_error` |
| …single-role replace | Not run | Needs the edit drawer's replace-a-role flow. Its API is the same `PUT /mapping/user-session/update {is_deleted:true}` checked in the next row |
| …work order deleted | Not run | The brief allows no deletes on Demo |
| Edit-drawer removal (`PUT /mapping/user-session/update`) revokes access | **PASS** | For a person ticked through the Field team (WO 8) and for a person added directly the way the drawer adds them (WO 10): access is gone within 4 s and still gone at 30 s (r18, r19). Seen once (r16 B), not reproduced: on WO 1, after the 500s, removing a direct mapping left the membership in place until another WO closed. Not filed |
| Customer link: the provider's chosen people open exactly the shared SLD and no other | **PASS** | EG-ACME (SA) › New company link › Customer "QA-DEMO ZP-4561 customer 16 Oct" › Demo admin email → 201 (facility name null). Demo accepts. Shared SLDs › "test" only. EG-ACME adds AM as "Electrical Engineer". AM's `accessible_sld_ids` = [test], and the site picker lists only "test". `/sld/{test}/graph`, `views`, `view-mappings` → 200. The same calls for "Demo" and "Riverside Plant" → 422 permission_denied (11–14, r17). Caveat: "test" has 0 nodes for the Demo admin too, so the diagram is empty for both |
| No duplicate sub row in the Field team dialog; Field team / release can be saved with nobody ticked | **PASS** | One group, "QA-DEMO ZP-4830 17 Nov sub (EG-ACME) · QA-DEMO ZP-4755 in-house crew", lists PM once. Saving with nobody ticked → 200 (UI in the bug video, step 2; API in r16/r18) |
| Negative: public / egalvanic.com invites resolve to nobody, with identical responses | **PASS** | All 201 with the same 12 keys and `sub_company_id/name` and `prime_company_name` null, then listed as invited with no company: `qa-demo-zp4830-nobody-17nov@egalvanic.com`, `qa-demo-zp4830-public-17nov@gmail.com` (hyphens, so it can't be a real Gmail account), `…@example.com`. The existing user (ADMIN seat) got the identical shape. Exact-user email wins over the egalvanic.com rule, as the ticket describes |
| Negative: ambiguous domain | Not run | No known ambiguous domain on QA |
| Link B (EG-ACME prime, Demo sub) for ZP-4833 | **Created** | After the flag read true: `c5d50283…` active 15:30 IST. Demo has **no Teams**, so EG-ACME sees `linked_teams: []`. Details in LINKS.md |

## New defects
1. **BUG-retick-500.txt, High, in scope (task #161 assignment reconcile): a linked subcontractor's person can't be put back on a work order they were taken off.**
   - Field team › Save → HTTP 500 `internal_error`, shown raw in the dialog.
   - Trace ids: 6375f4c8… (15:12), 670250d4… and b00276d6… (15:14), b51936aa… (15:15), c3994b2a… (15:25), a5e371b9… (15:33).
   - Re-adding the person in the edit drawer (`POST /mapping/user-session/create` → 200) lists them as Field technician on WO 10, but their Demo guest access never comes back (checked after 1 min).
   - A different, never-used WO works first time, so the record left by the earlier removal is the likely cause.
2. **security finding ZP-5315 (details in Jira only; public repo rule), Medium.**

Minor notes (not filed):
- The Field team dialog shows the raw text "internal_error".
- Right after `PUT /ir_session/{id}/crews`, a field-team PUT can get 400 "Some of those people are not in this company" until the async write lands. API-only race; the UI path is slower.
- Withdraw copy reads "x@y.com's company can no longer accept it". Suggest "x@y.com can no longer accept it".

## Cross-platform
- Twins: there is no [iOS]/[Android] Collaborators ticket. Company links are a web Admin page.
- The mobile guest work is **ZP-5143** (iOS, In QA) and **ZP-5144** (Android, In QA): hide Create Site / Create WO when `field_limits` is false.
  - The web API returns `field_limits` false/false for the sub-link Technician guest (PM). It returns true/true for Electrical Engineer guests (EE, AM).
- The Android tester ran the TECH guest in Demo (GUEST-MENUS/android.md). Create Site and Start WO are hidden, which matches the web `/me`.
- MOBILE-CHECKS for the device testers: GUEST-MENUS/verdict.md, section MOBILE-CHECKS.

`Cross-platform: web FAIL (re-tick 500; see ZP-5315) · iOS not checked (no Collaborators on iOS; guest checks listed for ZP-5143) · Android Create Site/Start WO hidden for TECH guest per android.md; sub-link guest checks listed`

## Not covered
- Single-role replace through the drawer UI.
- WO delete (no deletes on Demo).
- Ambiguous-domain invite.
- Customer-link extras: pairing a shared SLD with our own site, "Let Demo Company staff its own jobs with your Teams".
- Old-invite **Technician** guest on web: TECH is reserved for the device testers. PM shows the same role.
- Backend PR #1820 diff: no GitHub access to eg-pz-backend.

## Test data created (all left in place)
Demo Company:
- Subcontractor "QA-DEMO ZP-4830 17 Nov sub (EG-ACME)" `7ea55eda-2f37-46f2-accf-95ac838525b3`, first contact qa-demo-zp4830-contact@example.com.
- **Link A** `14d77ee3-ee52-4b5d-92dc-4c6105bb01ea`, active.
  - Earlier requests: one declined (`2db7a59d…`), one withdrawn (`0eab38eb…`), three negative invites withdrawn.
- Accepted **customer link** `a3a138ef-4863-4697-b0e1-669ee828eb6c` and shared SLD "test" with EG-ACME.
- Accepted **Link B** `c5d50283-62d5-4783-9d98-216e4a0db6aa`.
- Work orders on site "Demo", all open, named "QA-DEMO ZP-4830 17 Nov WO …":
  - WO (linked sub) https://demo.qa.egalvanic.ai/sessions/38f48f6f-0be4-4d3b-becf-edae3a3114b6
    - The Demo admin's auto Certifier assignment on it was removed by the "nobody" save.
  - WO 2 e93116a5-99ce-4dd7-a86c-108532f669ec
  - WO 3 d99ac331-4bbb-46bf-877c-923041fee9f2
  - WO 4 5f4d7f08-35c0-46b6-9e12-4f8526d80a4f
  - WO 5 e07576c0-fcd1-4990-85d5-3b2ae4e6f44c
  - WO 6 16cec4d1-e769-45dc-98fd-8b326c308c9d
  - WO 7 1ce761a1-da25-4b81-a66d-8562cf9e5938 (bug video)
  - WO 8 021c6a9d-9fcd-48e6-a8e0-e659ecf095a8
  - WO 9 ab762c42-52f5-4f4a-9d54-4a7ffcbb1adf
  - WO 10 aa4b6391-6b11-44a6-9344-5c086079eaf1
  - WO 11 46a9d2d0-e4bb-48d3-b44b-7ef53508d2b0
- **PM is currently assigned on WO 10 (direct) and WO 11 (linked Team)**, so PM is a guest Technician in Demo.

EG-ACME:
- Customer link on account "QA-DEMO ZP-4561 customer 16 Oct" (`3cb8a94f…`) with AM added as "Electrical Engineer". AM is a customer-link guest in Demo on "test".
- Link B sent from the ZP-4833 tester's vendor `9767989d…`.
- Gate-check requests sent and deleted at once: customer link `0eb9cd66…`, plus one more from the UI check.

Not changed: company settings, roles, the EE seat's existing Demo guest membership.

## Evidence (this folder)
- Videos:
  - `QA-17Nov-ZP-4830-video-1-recheck-withdraw-accept-decline.mp4` (63 s)
  - `QA-17Nov-ZP-4830-video-2-linked-team-guest-access.mp4` (60 s)
  - `QA-17Nov-ZP-4830-BUG-retick-video.mp4` (53 s)
  - `QA-17Nov-ZP-4830-BUG-wo-video.mp4` (33 s)
- Screenshots 1–17 `QA-17Nov-ZP-4830-*.png`, BUG-retick-1, BUG-wo-1/2.
- Raw JSON per run in `raw/` (r01–r22); scripts in `.playwright-mcp/guest17/`.
- Jira draft `ZP-4830.txt`. Links for ZP-4833: `LINKS.md`.
