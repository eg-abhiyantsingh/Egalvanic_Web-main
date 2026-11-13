# 2026-11-13: Check two customer requests — "QR Code Visible Wherever Possible" and "Link Existing Issue to a Work Order"

**Prompts:**
1. Owner pasted both requests: "test that this two issue is resolved or not".
2. Mid-way: "check this for bces iq in producation" (the QR request).
3. Mid-way: save the BCES-IQ production Account Manager seat in code. Done in `AppConstants` as `BCES_PROD_URL`, `BCES_PROD_AM_EMAIL` and `BCES_PROD_AM_PASSWORD`. It stays local only and is not committed, because the repo is public.

**Environments:**
- QA: acme.qa.egalvanic.ai, build index-BmlIaa75.js. Super Admin seat, site "(s) Wild Goose Brewery".
- BCES-IQ production: acme.bces-iq.com, build index-Bc-4hnLR.js (badge V2.5). Acme test company only.
  - **Read-only.** Every write-like API call was blocked by the script. Every dialog was cancelled.
  - Sites used: Saveland Avenue Lofts, Super Caremark and Test Site.

## Request 1: QR Code Visible Wherever Possible (web list) — NOT DONE on BCES-IQ production
| Place the customer listed | BCES-IQ prod | Evidence |
|---|---|---|
| Locations › room (Room ABC: Basement Panel QR EG-60810, LP-PA QR 183246) | No QR. Columns: Name, Asset Class, Asset Subtype, Condition | QR-1 |
| Inside an issue (TA1 on Basement Panel) | Asset card shows only "Basement Panel" | QR-3 |
| Issue form › Asset picker | "Basement Panel", "Basement Panel 2": no QR. Typing EG-60810 finds nothing | QR-4, QR-5 |
| Quote › Planned Work Orders › Line Items › Add Line Item › From Procedure › Asset | Names only (DPH has QR biq-hq-1, SW L1A has QR 182456). Typing 182456 finds nothing | QR-7 |
| Quote › Bulk Add Procedures › Specific assets | "SW L1A — Disconnect Switch — Building A…", no QR | QR-8 |
| Work order › Actions › Add Tasks › Asset | "3-Pole Breaker 1" listed twice, nothing tells them apart. Typing ABC001 finds nothing | QR-9 |
| Arc Flash Readiness › Asset Details | No QR column (Label, Notes, Ampere Rating, kA Rating, %) | QR-6 |

Already there:
- The Issues list has a QR Code column (ZP-4991).
- The work order Assets tab has a QR Code column.
- The work order Issues tab has a QR Code column (seen on QA).
- Asset pickers changed by ZP-5181 (Link Existing Assets / Node, connection source and target, parent) show "Name (QR)".

Also seen without QR (not in the customer's list):
- Work order › Add Issues › Existing Issues: "NEC Violation · 3-Pole Breaker 1".

QA shows the same gaps: the Locations building list, issue details and the issue form picker (QR search finds nothing). Arc Flash on QA has no QR either.

**Mobile list:** not checked. There is no production mobile build on the simulators.
- The mobile tickets ZP-5168 (Android) and ZP-5177 (iOS) put the QR number beside names in pickers; both are Done.
- ZP-5231 is still To Do: on Android the QR shows on its own line.

## Request 2: Link an existing issue to a work order (ZP-5286, Backlog) — MOSTLY ALREADY POSSIBLE on web
- **Issues list › "Add to Work Order"** (QA, full run): tick the issue › "Add 1 to Work Order" › choose an existing open work order, or "Create new".
  - Call: `POST /ir_session/<wo>/add-issues {issue_ids}`.
  - The issue then shows "Session = Work Order - Nov 3, 5:15 PM".
  - The work order's Issues tab lists it, with its QR code.
- **Unlink** (QA): the "Unlink issue from work order" button on the work order Issues tab › confirm "Unlink".
  - Call: `PUT /mapping/issue-session/update/<issue>/<wo> {is_deleted: true}`.
  - The issue's Session row is gone afterwards.
- **Second way:** work order › Actions › Add Issues › **Existing Issues** ("Pick open issues at this site to add to this work order"). It lists the unlinked issue.
- **BCES-IQ prod (read-only):** both entry points exist. The Add to Work Order dialog opened with real open work orders and was cancelled.
- **Not there:** a "Link to Work Order" action on the issue details page. The ⋮ menu has only "Edit Issue", and the edit form has no work order field. The ticket suggested exactly this.
- **Not checked:** the Support Agent's answers. The in-app support chat is DevRev PLuG, and the owner rule says never use DevRev.

## Test data (QA)
- Issue "QA-DEMO 13 Nov QR + link-to-WO check", fa642558-ac0c-4d90-a42f-a5efeba95a58.
  - Asset: QA-DEMO 4 Nov ZP-5171 Main Distribution Panel (QR QA5171-1002).
  - Linked to and unlinked from work order 48e9907c-dd41-4479-baa6-fb544e32d929. It ends unlinked.

## Evidence
- `docs/bug-evidence/2026-11-13-qr-bces-prod/`:
  - PROD-BCES-13Nov-QR-video-steps.mp4 (198 s) plus QR-1…9;
  - PROD-BCES-13Nov-LINK-video-steps.mp4 (87 s) plus LINK-1…3.
- `docs/bug-evidence/2026-11-13-qr-and-issue-link/`: QA-13Nov-ISSUE-LINK-video-steps.mp4 (95 s) plus ISSUE-LINK-1…8.

## Traps
- **BCES-IQ prod list calls are POSTs:** `/company/<id>/quotes/v2` and `/workorders/v2`. The read-only guard allows a POST only when its body has paging and no record fields. `/api/s3/urls/batch` is allowed so photos load.
- **BCES-IQ sites:** the staff seat's `accessible_sld_ids` is empty and `/api/sld/list` returns 404. Get site ids by picking each site and reading `localStorage.activeSiteId`.
- **Locations tree:** the expander sits about 24 px left of the label. A click on the label only selects the node.
- **Quote Bulk Add Procedures:** the Apply To radios (all / classes / select) appear only after a procedure is picked.
