# 2026-09-23 evening — Ready-for-QA walks with the corrected method (index-vDJBGpU_.js)

Seat: +admin@ on acme QA (Super Admin, Electrical Engineer, Project Manager, Account Manager, Admin; no Portal Sales).
Every screen reached through the app's own navigation (rail → list → search → click). No typed URLs.

| # | File | Ticket | What it shows |
|---|---|---|---|
| 01 | 01-create-wo-step1.jpg | ZP-4346/4347 | Create New Work Order, step 1 |
| 02 | 02-zp4347-first-service-general-first.jpg | ZP-4347 | First service to perform: General is option 1 of 18 → PASS, moved (comment 44453) |
| 03 | 03-zp4346-save-service-after-picking-ir.jpg | ZP-4346 | IR picked, 158 of 158 assets, Save service bar under the list |
| 04 | 04-zp4346-scrolled-save-service-still-pinned.jpg | ZP-4346 | List scrolled; header gone, Save service unmoved (sticky bottom) → owner's call |
| 05 | 05-zp4292-add-asset-no-services-section.jpg | ZP-4292 | Add Asset on General WO Sep 22 7:07 PM: no services section |
| 06 | 06-zp4292-created-no-tray.jpg | ZP-4292 | Create Asset → toast, no tray/sheet |
| 07 | 07-zp4292-after-reload-three-services.jpg | ZP-4292 | After reload (half scale) |
| 08 | 08-zp4292-same-wo-as-21sep-no-services-section.jpg | ZP-4292 | 21 Sep WO, Circuit Breaker: services section gone |
| 09 | 09-zp4291-rows-two-checkboxes.png | ZP-4291 | Zoom: 5-service rows draw 2 boxes |
| 10 | 10-zp4291-forms-badge-opens-form-picker.jpg | ZP-4291 | Forms badge "8" opens Select a Form (8 draft forms) |
| 11 | 11-zp4291-flat-view-same-count.jpg | ZP-4291 | Flat room list: same counts |
| 12 | 12-zp4309-ir-wo-children.jpg | ZP-4309 | IR WO 22 09, PNL expanded: CB1 no box, CB2 + PNL MCB boxes at 0 photos |
| 13 | 13-zp4309-cb1-no-pole-count.jpg | ZP-4309 | CB1: no Pole Count |
| 14 | 14-zp4309-cb1-no-condition.jpg | ZP-4309 | CB1: no Condition |
| 15 | 15-zp4309-cb2-3p.jpg | ZP-4309 | CB2: 3P |
| 16 | 16-zp4309-cb2-condition-3.jpg | ZP-4309 | CB2: Condition 3 → PASS, moved (comment 44454) |
| 17 | 17-zp4062-mark-as-lists-five-services.jpg | ZP-4062 | Bulk Ops → Mark As.. on the test asset: its five services only |
| 18 | 18-zp4062-after-reload.jpg | ZP-4062 | After reload, ring 7 of 20 |
| 19 | 19-zp4062-test-asset-arc-flash-ticked.jpg | ZP-4062 | Arc Flash still ticked on QA-DEMO ZP-4292 delete me → PASS, moved (comment 44455) |
| 20 | 20-zp4138-portal-tile-access-denied.jpg | ZP-4138 / 4061 | Rail tile → Site Health → Access Denied |
| 21 | 21-zp4218-convert-dialog.jpg | ZP-4218 | Convert to subcontracted labor dialog |
| 22 | 22-zp4218-after-convert-sub-still-no.jpg | ZP-4218 | After Convert: Sub = No |
| 23 | 23-zp4218-after-reload-sub-still-no.jpg | ZP-4218 | After reload: Sub = No |
| 24 | 24-zp4218-rate-menu-no-rfq.jpg | ZP-4218 | Rate menu: Auto / 2 Standard / Custom / Create new — no RFQ |
| 25 | 25-zp4292-full-row-three-checkoffs-none-chosen.jpg | ZP-4292 | Full-size: new asset row with Forms, Arc Flash, IR Photos boxes |
| 26 | 26-zp4291-full-grid-five-services-two-boxes.jpg | ZP-4291 | Full-size: FDR rows 2 boxes; ATS 123 (Checklist only) none |
| 27 | 27-zp4153-journal-nothing-interpreted.jpg | ZP-4153 | Journal ac758601: every location "Nothing interpreted yet" |

## Request-level facts recorded alongside

- ZP-4291 — `GET /api/ir_session/1a9c5d13-…/service-registry`: `by_node` has 5 services on nodes 54b6e316…, ec77d70b…,
  1497796d… (4 PM Forms + AF Data Collection); rows render `forms_status` + `svc_mask_arcFlash` only. Checklist
  service 9de69871… (Arc Flash Label Placement) on 69929074… / 22ca040f… → no mask cell.
- ZP-4062 — `PUT /api/ir_session/1a9c5d13-…/line-checks {node_ids:["1497796d-…"], service_id:"d625cfa0-…", checked:true}` → 200 `updated:1`.
- ZP-4218 — `POST /api/plans/b567d2a6-…/generate` sends `pricing.subcontracted: []`, conversion only in
  `subcontracted_overrides {"e08f8c64-7f48-47db-928a-b23946aab5ab": true}`; plan reads back `subcontracted:false` on every labor line.
- ZP-4346 — Save service parent `position: sticky; bottom: 0`; top 510 px at scrollTop 0/250/271, header 370→120→99 px.
- ZP-4042 — `GET /api/reporting/history?limit=5&sld_id=652a9ba9…` → 500 internal_error, trace 2f3011521075444194dcea190f4e98e7;
  `/api/reporting/configs` → 200.

## Test data touched

- Created "QA-DEMO ZP-4292 zero services delete me" (Panelboard) on /sessions/8684d267-4e6b-49dd-9ade-d48bbf4d8485.
- Marked Arc Flash Data Collection done on "QA-DEMO ZP-4292 delete me" (/sessions/1a9c5d13-…).
- Pressed Convert once on plan b567d2a6-… (QA-DEMO ZP4220 v2 EMP priced (delete me)); no lasting change.
- Create WO wizard cancelled; nothing created. Re-interpret NOT pressed on the journal (starts an AI run).
