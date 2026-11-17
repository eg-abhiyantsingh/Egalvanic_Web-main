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
