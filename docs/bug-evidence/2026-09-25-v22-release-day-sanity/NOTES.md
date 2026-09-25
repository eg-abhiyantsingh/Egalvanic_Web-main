# 2026-09-25 — Web v2.2 release day: sanity sweep, ticket re-tests, multi-select checks

Builds: morning on `index-CC4S9HsJ.js`; QA shipped **`index-C2Lqd_s1.js`** mid-morning (in-app "A new app update is
available" banner appeared during a test). Seat: +admin (Super Admin · Admin · Portal Sales). Signed-out checks in the
devtools browser (isolated context). No password typed anywhere; a test address `qa.sanity.check@example.com` was typed
on the sign-in page to reach the password form.

| # | File | Ticket / area | What it shows |
|---|---|---|---|
| 01 | zp4218-labor-both-lines-sub-yes-generate-rfq-after-reload | ZP-4218 | After Convert + F5: Journeyman + NETA lines Sub = Yes, red Generate RFQ on both, "Pending: 21 sub quotes to enter" |
| 02 | zp4218-generate-rfqs-dialog-template-menu | ZP-4218 | Generate RFQs dialog per WO + template menu ("Abhiyant rfq 17 aug", "Rfq") |
| 03 | issues-add-to-wo-two-work-orders-created-same-name | multi-select | Two WOs named "QA-DEMO multi-issue WO double-click…" from ONE scripted double-submit |
| 04 | issues-add-to-wo-dialog-create-new-two-issues | multi-select | Add to Work Order dialog, Create new, 2 issues — before a REAL mouse double-click (→ only ONE WO) |
| 05 | wo-multi-issue-issues-tab-lists-qa-rec-a-b | multi-select | An earlier WO still lists QA rec A/B after they were added to a newer WO |
| 06 | issue-qa-rec-a-details-session-names-only-last-wo | multi-select | Issue page "Session" = only the newest WO; Status History 1 (initial Open only) |
| 07 | compliance-acknowledge-400-raw-toast-25sep | ZP-4045/4060 | Acknowledge → raw toast 400 "1 key(s) do not match a current deviation" (re-verified 25 Sep, real click) |
| 08 | zp4391-password-form-back-button-below-sign-in | ZP-4391 | New build: full-width Back button directly below Sign In |
| 09 | zp4391-after-back-method-picker-email-kept | ZP-4391 | After Back: method picker, email kept |

## Facts
- Sanity sweep #1 (CC4S9HsJ, +admin, every menu link clicked, 51 pages): 0 failed API calls, 0 JS errors; only flags = Access
  Denied on Site Data → Maintenance Program / Compliance / Reports (ZP-4390, In Progress). Detail pages (asset 9 tabs, issue 4,
  WO 3, quote, site walk, service, PM plan, panel schedule, task 3): 0 failed calls, 0 JS errors.
- Multi-select: `POST /api/plans/from-issues` 409 "No corrective service is configured for this account" (AutoOpp account) and
  409 "Every issue needs an accepted resolution before it can be quoted" (existing quote) — both shown as raw JSON toasts, twice.
  Scripted same-tick double click → 2× `POST /api/ir_session/create` 201 + 2× add-issues; REAL mouse double-click → 1 WO. Not a bug.
  Issue↔WO is many-to-many (`PUT /api/mapping/issue-session/update/{issue}/{session} {is_deleted:true}` on unlink; the issue
  then shows the previous WO). Issue Status History records no add/unlink.
- Compliance 25 Sep: withdraw → `POST …/acks/remove` 500 trace f378700ca2ced752127ebb3f0c124c06 (silent); ack → 400 as 24 Sep.
  Without a standard picked the page says "Nothing is waiting on a decision — every deviation carries a justification" (0 of 0).
- Report history 25 Sep: `GET /api/reporting/history?limit=5&sld_id=…` 500 trace 6e5b1420…, `?limit=5` 500 trace 83029af9…;
  the portal Reports page for +admin makes no history call at all.
- New on C2Lqd_s1 sign-in page: "Email me a code" option.

## Test data created (sandbox, labelled)
- WOs: `QA-DEMO multi-issue WO double-click 25 Sep delete me` ×2 (3e88337e…, 9337e2d5…), `QA-DEMO real-mouse double-click 25 Sep delete me` (42a6ab07…, issues unlinked again).
- Quote QA-DEMO ZP4220 v2: NETA Technician converted to subcontracted (plan repriced).
- One acknowledge attempt + one withdraw attempt (both refused by the server, nothing written).
| 10 | zp4390-portal-free-tier-locked-entries-still-shown | ZP-4390 | S8OG5dN0, portal LICENSE = Free: Condition Assessment, Locations, Panel Schedules, SLD, Maintenance Program… still listed, greyed with padlocks (not hidden) |

## Later on 25 Sep
- Third build **index-S8OG5dN0.js** (~07:45 UTC). Sweep #3 on it: 52 pages, 0 failed calls, 0 JS errors.
- ZP-4391 re-checked on S8OG5dN0: Sign In → Back order kept.
- ZP-4272 (hide QR Code on a WO Assets grid, leave, return → QR Code back), ZP-4315 (3 of 10), ZP-4040 (PM Forms tabs/column/ring) unchanged.
- The +admin seat's roles changed again during the morning: now **Super Admin · Admin · Portal Sales · Electrical Engineer · Project Manager · Account Manager**. After a reload the nav gains Engineering (6 pages), Ops Overview, Sales Overview, and Site Data → Maintenance Program / Compliance / Reports now OPEN. So the ZP-4390 Access-Denied repro no longer exists on this seat.
- S8OG5dN0 wraps HTMLInputElement value setter → scripted `set.call(input, v)` throws "Illegal invocation"; `document.execCommand('insertText')` works.
- Jira: ZP-4390 → Ready for QA (PR #1539, 02:45 CDT). Others moved ZP-4291, 4292, 4351, 4352, 4207 to READY TO RELEASE today (QA's last verdicts on 4291/4292 were fails).
