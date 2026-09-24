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

## ZP-4390 (filed 24 Sep, owner's request)
| 29 | zp4390-site-data-reports-access-denied-admin-seat-roles | ZP-4390 | Site Data → Reports = Access Denied for +admin (profile popover shows roles Super Admin · Admin · Portal Sales) while the menu lists Maintenance Program / Compliance / Reports |
Owner: "instead of lock just hide the tab that is not accessible to user … for all pages". Assigned to Avani, Medium, sprint 1222, fixVersion Web v2.2, To Do. Capture 29 attached (id 37559).

## ZP-4391 (filed 24 Sep, owner's request)
| 30 | zp4391-password-form-back-is-a-text-link | ZP-4391 | Password form: Sign In, terms, "Forgot your password?", then the small "Back to faster options" text link — owner wants a visible Back button directly below Sign In |
Assigned to Avani, Medium, sprint 1222, fixVersion Web v2.2, To Do. Capture 30 attached (id 37560).

## Afternoon pass (owner: "if ticket are passed then move ready to qa to ready to release") — same build index-CC4S9HsJ.js
| 31 | zp4360-assets-374-rows-android-site-2 | ZP-4360 | Site Data → Assets, 374 rows page 1 (node listing correct); timing set in the ZP-4360 comment |
| 32 | zp4045-portal-add-service-history-future-date-picker-unbounded-server-400 | ZP-4045 | Portal asset → Maintenance → Add record: "Last serviced" picker has NO max, 15/01/2027 accepted by the picker, server refuses: PUT /api/asset-maintenance/node/{id}/history → 400 "last_serviced_on cannot be in the future" (raw toast) |
| 33 | zp4045-pm-plan-apply-future-last-performed-server-400 | ZP-4045 | Staff asset → Set Up PM Plan → Custom → Clean, Tighten, Torque, Last performed forced to 15/01/2027 (picker max = today): POST /api/asset-maintenance/apply → 400 "service_dates[…] cannot be in the future" |

Timings (end-to-end from India, 3 runs, best/worst): work-order list POST workorders/v2 310/376 ms; asset list lookup/v2/nodes 285/735 ms; site dashboard lookup/site-overview/{site} 470/944 ms; empty-site asset list (network floor) 272/689 ms.
Nav: secondary panel 264 px wide (rail 88 px); "Maintenance Program" text 164 px, scrollWidth = clientWidth → no ellipsis.
Pickers: Record completion "Performed on" max=2026-09-24 ✔; PM-plan "Last performed" max=2026-09-24 ✔; Add Service History "Last serviced" max="" (portal AND staff) — not in the ticket's list, server refuses anyway.
Test data: asset `QA-DEMO ZP-4045 shutdown rule - delete me` (Panelboard, id 8b54aac9-2655-40ac-857d-d937dbffd8d2) created on Android Site 2; shutdown restriction set to "never" (PUT …/shutdown → 200). No PM program applied (both applies refused).
Moves: ZP-4346 (comment 44498), ZP-4360 (comment 44499). ZP-4315: Krunal's 24 Sep image is a WEB capture (ring 56%), not iOS.
| 34 | zp4045-zp4060-compliance-acknowledged-3-shutdown-rule-segment | ZP-4045/4060 | Compliance overview after the shutdown rule: score 0.3% (3 of 865), legend "Acknowledged 3 deviations with written justification", bar shows the acknowledged sliver next to 862 red |
| 35 | zp4045-deviations-acknowledged-filter-qa-demo-asset-three-acked-rows | ZP-4045 | Deviations → Acknowledged filter: Clean, Tighten, Torque / De-Energized Visual Inspection / NETA Testing on the QA-DEMO asset read "Never serviced · Acked"; Infrared Thermography (energized) stays open; each row has a "Withdraw acknowledgment" icon |

Shutdown-rule pre-ack (API): deviations for node 8b54aac9… carry `ack:{by_name:"Shutdown rule", system:true, justification:"This asset can never be taken out of service, so a de-energized procedure cannot be performed on it. Acknowledged automatically from its shutdown rule."}` on the three de-energized services; IR stays `acknowledged:false`.
Human ack on the same pair: `POST /api/program-compliance/{site}/acks {dev_keys:[88979daf…], justification}` → 400 "1 key(s) do not match a current deviation" — three shapes tried (body only, body + pm_standard_id, ?pm_standard_id=) — key taken from the current list seconds earlier. Same defect as this morning; not a picker-path artefact.
Withdraw the automatic ack (row icon "Withdraw acknowledgment"): `POST /api/program-compliance/{site}/acks/remove {dev_keys:[88979daf…]}` → **500** internal_error trace f418ae516fbc18a7d5ba4a70baf7b2dd; no toast shown; row still "Acked". Acknowledgements tab reads "0 justifications cover 3 deviations · No acknowledgments yet" (system acks are not listed there).

## Comments + screenshots on the 20 remaining Ready-for-QA tickets (owner: "add comment and screenshot in 20 remainings ticket")
| 36 | zp4315-ring-tooltip-overall-3-of-10 | ZP-4315 | WO 1a9c5d13 ring tooltip "Overall: 3 of 10 (30%)" on CC4S9HsJ |
| 37 | zp4040-pm-forms-wo-forms-tab-column-ring | ZP-4040 | QA-DEMO PM Forms wheel check-offs: Forms tab (4), Forms column, ring 13% |
| 38 | zp4086-circuit-breaker-class-editor-no-condition-flags | ZP-4086 | Admin → Asset Classes → Circuit Breaker editor, no Listed/Required condition controls |
| 39 | zp4261-admin-activity-logs-request-log-with-acks-500-and-400s | ZP-4261 | Admin → Activity Logs: request log only (also shows today's acks/remove 500 f418ae51… and two ack 400s; tiles say BLOCKED·5XX 0 beside a 500 row) |
| 40 | zp4176-updates-whats-new-panel-on-qa-build-v136 | ZP-4176 | In-app Updates ("What's new on Z platform") panel on the QA build V1.36 |
| 41 | zp4127-cb4-powerpact-hj-engineering-tab-thermal-magnetic-no-dial | ZP-4127 | CB4 (Square D PowerPact HJ HJL36150, thermal magnetic) Engineering tab — library-matched, no continuous setting |
Comments 44501–44520 (one per ticket; ZP-4042's first attempt timed out in the connector and was re-posted as 44520). Screenshots attached via the Jira page on 14 tickets; none possible for ZP-4185, 4363, 4128, 4183, 4190, 4216 (no web screen) — said so in each comment.
