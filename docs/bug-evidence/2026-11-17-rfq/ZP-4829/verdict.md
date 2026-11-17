# ZP-4829 [iOS] Drop local company data when the server resets it (data epoch): verdict

**Verdict: PARTLY.** TC2 (no bump) passes for the epoch logic. TC1, TC3 and TC4 are **blocked: they need a backend DB bump**. The ticket **stays In QA**. No new defects.

- **Build:** Z Platform-QA 1.75 simulator build (binary 16 Nov 06:49 IST, from the owner). Lane A: iPhone 17, iOS 26.2. API https://eg-pz.qa.egalvanic.ai/api.
  - It was installed as an upgrade over the 5 Nov 1.75 build, so the local data from before was still there.
- **Seat:** SA (EG-ACME, company d59d449b-09d8-45d6-8f0a-ef70024b1293). Site (s) Wild Goose Brewery.
- **Server:** /api/auth/v2/me returns company_data_epoch = 0 for EG-ACME (and for Demo).
- **Date:** 17 Nov 2026, 13:51–14:06 IST.
- **Method:** each step was checked three ways:
  - the screen;
  - the app's unified log (`xcrun simctl spawn <udid> log show --predicate 'process == "Z Platform-QA"'`);
  - copies of the local SwiftData store (row counts and primary-key ranges). Kept rows keep their keys; a wipe and re-download gives new keys above the old maximum.

## TC2 – no reset, no wipe
| Step | What happened | Result |
|---|---|---|
| First start of the new build | The refresh token had expired, so the app showed "Session Expired". The SA signed in again (same user). | n/a |
| First /me on this build ("first sight") | The UserDefaults key `companyDataEpoch.d59d449b-…` appeared with value **0**; it did not exist before. No "Company data epoch changed", "[CompanySwitch] …" or "local data dropped" line in the log. Same rows and keys (nodes 1847, keys 12897–14743). Home shows the same 286 assets / 130 connections. | PASS: first sight only records the epoch |
| Relaunch (terminate + open) | Opens straight to the site home, no loading screen. Same rows, same keys. | PASS |
| Settings › Logout | The local store is unchanged after sign-out (same rows and keys). | PASS (no wipe) |
| Sign in again (same SA) | Same rows and keys. No epoch messages. The app then shows **Select Site**. | see the note |
| Pick the same site | "Switching Sites" → "Loading (s) Wild Goose Brewery", 42 s. All rows get new keys (nodes 14744–16611): the site was cleared and downloaded again. | note, not the epoch code |

**Note for Kush on TC2 "sign out and in → nothing reloads from scratch":**
- The full reload after sign-out/in is **not** the epoch reset. The log shows no epoch or CompanySwitch lines.
- It is the normal site switch:
  - `SLDService.switchToSLD` "Switching from SLD 00000000-… to 9138FD14-…";
  - then `clearSLDs` "Clearing totals - Nodes: 1847 …";
  - then `upsertAllData(isSiteSwitch: true)`.
- It happens because Logout deliberately clears the active site. AppStateManager.swift lines 166–168: "Reset active SLD to force site selection on next login".
- That is existing design. If TC2 is meant to cover only the epoch logic, it passes. If "nothing reloads" is meant for the whole sign-out/in flow, it needs a design decision, not a fix to this ticket.

## TC1 / TC3 / TC4: blocked, needs a backend DB bump
- Nothing in the app or the QA UI bumps `companies.data_epoch`.
- **Exact ask for a dev (backend):** on QA, increment `companies.data_epoch` for EG-ACME (company id d59d449b-09d8-45d6-8f0a-ef70024b1293). It is 0 now. Bump it once before each of TC1, TC3 and TC4 (or tell me how to trigger the demo reset for one company), and tell me the time of each bump. For TC1, also hard-delete one QA-DEMO room/asset first, so its disappearance can be seen.
  - Alternative: use Demo Company (company code `demo`, also epoch 0) if a demo reset is easier.
- With the bump, I will run:
  - TC1: relaunch → site list reloads, deleted items gone, no screen fills and empties, an edit saves and syncs.
  - TC3: Go Offline (the in-app switch), edit an asset (1 change queued), bump, go online and relaunch → reset runs, queued change dropped, log ".error server reset dropped 1 queued change(s)", nothing stuck.
  - TC4: switch company with a bump pending.
- The baseline is ready: the key is recorded as 0 and the logging works. The app log stream for the epoch lines is in the excerpt file.

## Cross-platform
- **Web:** not applicable. Web has no local company cache. The backend part is ZP-4834 ([Web] companies.data_epoch on /auth/v2/me), which is **READY TO RELEASE**.
- **Android:** the twin ZP-4838 ([Android] Drop local company data when the server resets it) is **Done** (Android v1.39). Not re-run by me. It needs the same DB bump for its TC1/3/4.

## MOBILE-CHECKS (Android, only once the backend bump is available)
1. Before any bump: relaunch and sign out/in → no wipe.
2. After a bump: relaunch → local company data rebuilt, no ghost rows.
3. Offline edit queued, then a bump → the queue is dropped and nothing is stuck.

## Evidence (this folder)
- `QA-17Nov-ZP-4829-TC2-no-bump-relaunch-signout-signin.mp4` (57 s, step captions).
- `QA-17Nov-ZP-4829-1-first-signin-on-build-same-data.png` (PASS, expected).
- `QA-17Nov-ZP-4829-2-after-relaunch-same-data.png` (PASS, expected).
- `QA-17Nov-ZP-4829-3-after-signout-signin-asks-for-site.png`: existing design (amber, question).
- `QA-17Nov-ZP-4829-4-same-site-picked-full-site-switch-reload.png`: the site switch reload (amber, question).
- `QA-17Nov-ZP-4829-local-store-snapshots.txt`: counts and keys at each step, plus the UserDefaults epoch key.
- `QA-17Nov-ZP-4829-app-log-excerpt.txt`: the log lines (emails removed).

## Test data
- None created. The site's local data was re-downloaded by the site switch.
