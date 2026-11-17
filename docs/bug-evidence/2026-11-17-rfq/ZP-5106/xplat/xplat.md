# ZP-5106 cross-platform (Android): forked service on a mobile WO
- Platform/build: Android 1.40 (54). Seat: Super Admin, EG-ACME. Site "QA-DEMO ZP-5108 ZP-5109 12 Nov". 17 Nov 2026 ~16:45 IST. Web build index-CkkAeA9o.js.
- Steps and results:
  1. Work Orders > search "ZP-5106": **Available (2)** = "QA-DEMO ZP-5106 17 Nov IRT open A" and "… open B", both with service "Insulation Resistance Testing". Closed WO C appears only under All (3), **not in the open list**. PASS
  2. Open WO A (Start Session) > Forms: **"Insulation Resistance Testing — 5 drafts — Circuit Breaker, Tie Breaker"**. The list: QA-DEMO ZP-5108 CB-joined 12 Nov, QA-DEMO ZP-5108 SWBD-1 CB-A, QA-DEMO ZP-5109 CB-1 / CB-2 / CB-3 (5 assets). PASS
  3. QA-DEMO ZP-5109 CB-2 > Edit Form > Test Voltage = **1000** > Save > Close: the row shows "In Progress, 1/3 sections". It synced (online, nothing pending).
  4. Web: WO A (https://acme.qa.egalvanic.ai/sessions/c26b2c5c-9a00-4bcc-9699-47c7b67aecf4) > Forms > Insulation Resistance Testing > QA-DEMO ZP-5109 CB-2: the Test Voltage input = **1000**. PASS
- WO B's forms were not opened. The list shows B with the same service.
- Parity: **same** (the forked company copy of the service and its forms reach Android, and a form value syncs to web).
- Data changed: WO A, CB-2, Test Voltage = 1000 (QA-DEMO WO; harmless value). The session was deactivated afterwards.
- Evidence: android-1-wo-list-open-A-B-only.png, android-2-wo-A-forms-irt.png, android-3-irt-5-assets.png, android-4-test-voltage-1000-before-save.png, web-1-wo-A-cb2-test-voltage.png

## iOS (17 Nov, iOS tester)
- Platform/build: iOS, Z Platform-QA 1.75 simulator build (binary 16 Nov 06:49 IST, from the owner), lane A. Seat: Super Admin, EG-ACME. Site "QA-DEMO ZP-5108 ZP-5109 12 Nov". 17:00–17:07 IST.
- Steps and results:
  1. Work Orders: **Available** lists "QA-DEMO ZP-5106 17 Nov IRT open A" and "… open B", both with "Insulation Resistance Testing" (plus "QA-DEMO ZP-5108 12 Nov AF WO"). The closed WO C ("IRT closed control C, Completed") appears **only under Show All**. PASS
  2. Open WO A (Start Work Order): Work Type "Insulation Resistance Testing", 0 of 5. Forms: "Insulation Resistance Testing – 5 drafts – 0/5 submitted – Circuit Breaker, Tie Breaker". The list: QA-DEMO ZP-5108 CB-joined 12 Nov, QA-DEMO ZP-5108 SWBD-1 CB-A, QA-DEMO ZP-5109 CB-1 / CB-2 / CB-3. PASS
  3. **Cross-device read-back:** CB-2 shows "1/3 sections, In Progress", and its form shows Test Voltage = **1000**. That is the value the Android tester entered. PASS
  4. CB-3 › Edit Form › Test Voltage = **500** › Save Draft: the row shows "1/3 sections, In Progress". API GET /api/eg-form-instance/by-session/c26b2c5c-9a00-4bcc-9699-47c7b67aecf4: CB-3 instance 6fd7c6e9 has Test Voltage "500", modified 11:36:31Z (17:06 IST). PASS
- Parity: **same** as Android and web. The forked company copy of the service, its 5 forms, and the closed-WO filter all reach iOS. A form value syncs both ways (Android → iOS, iOS → server/web).
- Data changed: WO A, QA-DEMO ZP-5109 CB-3, Test Voltage = 500 (QA-DEMO WO, harmless). WO A was left as my active iOS work order.
- Evidence: ios-1-wo-list-open-A-B-only.png, ios-2-wo-list-all-closed-C-only-here.png, ios-3-wo-A-forms-irt-5-drafts.png, ios-4-cb2-shows-android-test-voltage-1000.png, ios-5-cb3-test-voltage-500-ios-edit.png
