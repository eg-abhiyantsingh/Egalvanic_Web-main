# 2026-09-24 — Ready-for-QA re-test on index-CC4S9HsJ.js (31 → 28 tickets)

Seats: signed-out devtools browser (sign-in tests); "guest user" / Client Portal role (customer-portal tests);
+admin (Super Admin · Admin · **Portal Sales** — EE/PM/AM removed today) for everything else. Every screen reached
through the app's own navigation. Browser zoom was 80% / 66%, so staff captures are cropped to the app area.

| # | File | Ticket | What it shows |
|---|---|---|---|
| 01–02 | zp4353-login-empty / email-typed-main-button-device | ZP-4353 | Sign-in page; after typing the email, device sign-in is an outlined (secondary) button |
| 03–04 | zp4353-after-press / after-failure-full | ZP-4353 | "We couldn't use that sign-in method…" — email KEPT |
| 05 | zp4353-password-is-primary | ZP-4353 | "Use my password" is the dark primary button → PASS, moved (44489) |
| 06 | zp4353-password-form-email-carried | ZP-4353 | Password form pre-filled a•••@egalvanic.com |
| 07 | zp4352-continue-with-google | ZP-4352 | Google's own sign-in page; request carries prompt=login |
| 08 | portal-guest-site-health-addtioanl-site | ZP-4042/4061 | Customer seat opens the portal; Addtioanl Site 10 assets / 13 scheduled |
| 09 | portal-guest-reports-no-history-section | ZP-4042 | Reports: cards + WO list, no history section; /reporting/history → 500 (478816bb…) |
| 10 | portal-guest-work-orders-create-and-no-rows | ZP-4061 | Create Work Order offered + "No rows"; list POST → 422 company_data.view |
| 11 | portal-guest-assets-read-only | ZP-4061 | Assets read-only, 10 rows, no edit controls |
| 12 | portal-guest-condition-after-reload | ZP-4061 | F5 keeps portal + site; CA tabs clickable |
| 13 | portal-staff-free-tier-locks | ZP-4042 | Free: Site Health, Assets, Reports open; 7 padlocks |
| 14 | portal-staff-readonly-work-orders-locked | ZP-4061 | Read-Only: Work Orders locked |
| 15 | portal-staff-interactive-work-orders-two-rows | ZP-4061 | Interactive: 2 open WOs for the same site the customer saw as "No rows" |
| 16 | compliance-coverage-bars-nfpa70b | ZP-4045/4060 | 852 deviations, 3-part legend, sum(by_service.total)=852=expectations |
| 17 | compliance-acknowledge-dialog | ZP-4060 | Acknowledge dialog before the 400 |
| 18 | record-completion-future-date-refused | ZP-4045 | 15/01/2027 → "A completed service can't be dated in the future." (server 400) |
| 19 | zp4338-guest-portal-users-zz-ddd-first | ZP-4338 | Name Z→A: zz ddd row 1 → PASS, moved (44488) |
| 20 | zp4338-assets-yu-first | ZP-4338 | Asset Name Z→A: yu row 1 (373 assets) |
| 21 | zp4280-ring-tooltip-marked-complete | ZP-4280 | IR WO ring tooltip "Marked complete: 0 of 6 (0%)" → PASS, moved (44490) |
| 22 | zp4280-change-type-dialog-mark-complete | ZP-4280 | Change Service Type dialog: "Mark complete / Uncheck controls" |
| 23 | zp4292-add-asset-no-services-section | ZP-4292 | Add Asset drawer, no services section |
| 24 | zp4292-set-up-forms-sheet-all-ticked | ZP-4292 | NEW "Set Up Forms" sheet, 8 services pre-ticked |
| 25 | zp4292-sheet-all-unticked-not-now | ZP-4292 | All unticked, Create Forms disabled |
| 26 | zp4292-after-not-now-reload-three-checkoffs | ZP-4292 | After Not Now + reload: Forms/Arc Flash/IR check-offs on the new asset → still FAILS |
| 27 | zp4218-after-convert-sub-still-no | ZP-4218 | Sub still No after Convert (3rd build) |
| 28 | compliance-ack-400-toast | ZP-4060 | Raw toast "API call failed: 400 – 1 key(s) do not match a current deviation" |

## Request-level facts
- Portal WO list (customer): `POST /api/company/{id}/workorders/v2 {sld_id, page, filters, sort_by}` → 422 permission_denied "Required: company_data.view"; `GET /api/procedures-v2/services` → 422 procedures.view. Client Portal role = 35 perms, all *.view + notes.manage. No X-EG-Portal header on that request.
- Report history (customer, holds reports.view): `GET /api/reporting/history?limit=5&sld_id=fd1e25c4…` → 500 internal_error trace 478816bbe7184c2484393cf4a0a71bed; configs → 200.
- Compliance: `GET /api/program-compliance/aadcee4c…?pm_standard_id=e9ac475e…` → 852 deviations (all `missing_service`), totals.expectations 852, sum by_service/by_class = 852, ack_groups []. Without pm_standard_id → 0 deviations. `POST /api/program-compliance/aadcee4c…/acks {dev_keys:["d648bc58…"], justification}` → 400 "1 key(s) do not match a current deviation" (twice).
- Stated completion: `POST /api/asset-maintenance/stated-completion {date:"2027-01-15"}` → 400 "a completion cannot be in the future"; picker max=2026-09-24.
- Passkey: `POST /api/auth/v4/passkey/start` (2FA account) → 401 passwordless_start_failed "…Passkey sign-in isn't available for accounts that use two-factor authentication - sign in with your password instead." (x-ratelimit-limit 10). methods = passkey, google, password.
- ZP-4338 sort params seen on: /api/lookup/v2/nodes/{sld}, /api/connections/v2/sld/{sld}, POST /api/v2/issues/list, /api/panels/sld/{sld}, /api/eg-forms?sort_by=title&sort_dir=desc, /api/users/guest-portal — on each click and each page change.
- ZP-4360 timings from India (3 runs): WO list 292–733 ms; site-scoped 664–714; nodes p1 673–764; filter-options 358–762; sample-entities 283–778. All 200.
- ZP-4315 ring on 1a9c5d13: "Overall: 3 of 10 (30%)" (was 7 of 20 on 23 Sep).
- ZP-4280: 290 chunks scanned, 0 "check off" strings; "Marked complete" label in SessionDetail readiness, "mark-complete" error strings.

## Test data touched
- Created `QA-DEMO ZP-4292 recheck 24 Sep delete me` and `QA-DEMO ZP-4292 Not Now 24 Sep delete me` (Panelboards) on WO 8684d267-… (no forms created).
- Convert pressed once on plan b567d2a6-… (repriced, no change). Two ack attempts + one future completion refused (nothing written). Create WO wizard cancelled. Re-interpret never pressed. Guest seat: read-only navigation only.

## Seat / environment notes
- +admin roles changed today → Site Data → Compliance and Maintenance Program show Access Denied for it (menu still offers them). Compliance/Program checks were run through the portal on Interactive tier.
- Android Site 2 grew from 367 to 373 assets during the day; deviations 852 → 858 by the end.
