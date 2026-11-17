# ZP-5227 [Android] Match v1.39 database numbering so v1.39→v1.40 upgrades don't re-run the custom-manufacturers migration

**Verdict: PASS** (the upgrade path QA can run: v1.39 (DB 77) → v1.40 (54))

- Build: Android Z Platform (QA) **1.40 (54)** (Firebase App Tester, installed 17 Nov 12:46 IST over 1.39 (53) with real data), SLD Viewer Version 90. Emulator quacky_test 1080x2400, density 420.
- Web QA builds seen during the checks: index-oFAZQ3rK.js → index-CFMAmUdK.js → index-LGhrn-48.js → index-CkkAeA9o.js (QA was redeployed several times today; the web read-backs are API reads and don't depend on the UI build).
- Seat: Super Admin QA seat, Field Technician experience, EG-ACME, site Android Site 2. 17 Nov 2026.
- Fix: eg-pz-mobile-android PR #601.

## What was run
| # | Row (QA Review) | Result |
|---|-----------------|--------|
| 1 | Upgrade 1.39 (schema 77) → 1.40: only 77→78 runs, no "duplicate column companyId" crash | Lead, 12:46 IST: the first launch after the upgrade did not crash, and there was no "duplicate column" or SQLite migration error in logcat (upgrade-logcat-excerpt.txt; its only SQLite line is Google's NetworkScheduler, not our app). The app stayed signed in. Home showed 632 assets / 249 connections, matching web Site Overview after a Refresh. The DB version can't be read directly (the build isn't debuggable). PASS |
| 2 | After the upgrade: custom manufacturers work (add a custom manufacturer to an asset and save) | On breaker "QA-DEMO ZP-3021 17 Nov Child CB3 no library" › Engineering › Manufacturer, I typed "QA-DEMO ZP-5227 17 Nov Mfr" and picked 'Use "QA-DEMO ZP-5227 17 Nov Mfr"' ("New manufacturer, added for your company when you save"), then Save Changes. Reopened: it was kept. Server: manufacturer = "QA-DEMO ZP-5227 17 Nov Mfr", manufacturer_id 182. A new breaker's Manufacturer picker lists it (added for the company). Web asset page › Engineering shows MANUFACTURER "QA-DEMO ZP-5227 17 Nov Mfr". PASS |
| 3 | After the upgrade: layout groups work (sld_node_groups) | Site "QA-DEMO ZP-4800 SLD canvas v2.4 (delete me)": the Android SLD draws the web groups "QA4940 GRP full" and "QA4940 GRP partial" (on 1.39 there were no frames). Creating a group, placing an assembly offline, the sync and reopening all work: see ZP-5112. PASS |
| 4 | Upgrade 1.38.1 (schema 76) → 1.40 | Not run: no 1.38.1 build was installed on the emulator. |
| 5 | Fresh install of 1.40 | Not run: it would wipe the upgraded data and sign-in, which the other tickets needed. |

## Not covered
- 1.38.1 → 1.40 and a fresh install (rows 4-5).
- Reading the DB version / sld_node_groups table directly (release build, no run-as).

## New defects
None.

## Cross-platform
- **Web:** the custom manufacturer made on Android reads back on web (the asset's Engineering card shows it). PASS. The web asset form has its own manufacturer field; adding a custom manufacturer on web was not tried.
- **iOS:** the migration renumbering is Android-only (Room migrations), so it is not on this platform. Custom manufacturers exist on iOS (ZP-5126, Done, iOS v1.75). Not checked on iOS today.
- MOBILE-CHECKS (iOS): (1) Asset › Engineering › Manufacturer › search "QA-DEMO ZP-5227 17 Nov Mfr": it should be listed, because it was saved for EG-ACME from Android. (2) Pick it on an iOS QA-DEMO breaker, save, and confirm web shows it.

## Test data
- Breaker https://acme.qa.egalvanic.ai/assets/30248abc-0157-415b-868b-58a2fd6be56c (manufacturer set to the custom one; it was blank before; QA-DEMO asset).
- Company custom manufacturer "QA-DEMO ZP-5227 17 Nov Mfr" (id 182) now exists for EG-ACME. I did not find a way to delete it from the app.

## Evidence
- QA-17Nov-ZP-5227-after-upgrade-custom-manufacturer-layout-groups.mp4 (57 s)
- QA-17Nov-ZP-5227-0-apptester-1.40-54-latest-before-install.png, -1-after-upgrade-home-632-249.png, upgrade-logcat-excerpt.txt (lead)
- QA-17Nov-ZP-5227-2-custom-manufacturer-picked-before-save.png, -3-custom-manufacturer-saved-after-reopen.png, -4-custom-manufacturer-offered-on-new-breaker.png, -5-layout-groups-drawn-after-upgrade.png, -6-web-cb3-custom-manufacturer.png
