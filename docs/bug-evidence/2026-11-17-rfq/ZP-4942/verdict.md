# ZP-4942: SLD assemblies (#197), simplified terminals (#198), Select Entire Run (#199), elbows follow (#200), Simplify Edge (#201)

**Verdict: PASS → recommend READY TO RELEASE.** The blocker from 12 Nov is cleared: a real `feature-sld-assemblies` OFF state (Demo Company) and ON state (EG-ACME), both read live from LaunchDarkly. The 12 Nov FAIL row is now fixed: the next-name suggestion after placing L1 is "L2" (ZP-5264).

- **Builds:**
  - EG-ACME runs: `index-LGhrn-48.js`, 17 Nov 2026 14:07–14:27 IST.
  - Demo Company OFF run: `index-CFMAmUdK.js`, 14:01 IST.
  - The two builds are code-identical. I diffed the main bundle and the GoJSGraphEditor / AuditLog / AiTemplateEdit chunks with chunk hashes ignored: the only change is the build timestamp constant.
- **Seats:**
  - EG-ACME: Super Admin test seat (VALID_EMAIL, `is_eg_admin` true).
  - Demo Company: the colleague's staff login (`is_eg_admin` true; Super Admin, Electrical Engineer, Admin). Read-only use: unlock, select, menus and typed URLs. Nothing was saved in Demo.
- **LaunchDarkly at sign-in (evalx):** `feature-sld-assemblies` = **false** for Demo Company (08:26:55Z) and **true** for EG-ACME (08:39:16Z).

- **Re-check on the newest build:** `index-CkkAeA9o.js` was deployed at about 14:50 IST, during this round. At 14:56 IST, Demo Company (flag OFF) still shows no Assembly button and /assemblies says "Feature Not Available". EG-ACME (flag ON) still shows the Assembly button, and the next suggested name is now **L3** (after L2). The SLD, AiTemplateEdit and AuditLog chunks of that build differ from `index-LGhrn-48.js` only by renamed imports: the files are the same size and the code is the same.

## Checked today
| # | Check | How | Result |
|---|---|---|---|
| 1 | OFF: no "+ Assembly" | Demo › Engineering › SLD (site "Demo") › unlock | PASS: toolbar is Asset · Bus · Note · Edit · Delete |
| 2 | OFF: "Add to…" without Assembly | select 2 assets › Add to… | PASS: Add to Group, Add to View only |
| 3 | OFF: library route | typed https://demo.qa.egalvanic.ai/assemblies | PASS: "Feature Not Available" |
| 4 | OFF: Admin nav | Admin › Config | PASS: Asset Classes, Connection Classes, Issue Classes, Devices; no Assemblies |
| 5 | OFF: parts not behind the flag still work | Demo, locked SLD › right-click a connection | PASS: the menu shows only "Select Entire Run" (#199) |
| 6 | ON: "+ Assembly" and "Add to… › Assembly…" | EG-ACME, own SLD, unlock; select CB-2 + PNL-2 › Add to… | PASS: toolbar Asset · Bus · Assembly · Note; menu Add to Group · Add to View · Assembly… |
| 7 | ON: next-name suggestion (12 Nov FAIL, ZP-5264) | + Assembly › "QA4942-FDR-\{name\}" | **PASS (fixed):** Name box = **L2**; Will create "QA4942-FDR-L2 FUSE nosub / CB-2 / Load-1 / PNL-2"; `POST /api/assemblies/{id}/place-preview` → `"value":"L2"` |
| 8 | ON: place the assembly | Place QA4942-FDR-L2 › click the canvas | PASS: `POST /api/assemblies/{id}/place` → 201 in 648 ms. Result: 4 assets, 2 connections (FUSE nosub → Load-1 keeps `edge_manips {"hidden":true}`; CB-2 → PNL-2 keeps its elbow), group "QA-DEMO ZP-4942 grp 12 Nov QA4942-FDR-L2" recreated |
| 9 | ON: library + Admin nav | Admin › Config › Assemblies | PASS: the row "QA4942-FDR-\{name\}" (4 nodes, 2 edges); Assemblies is listed under Config |
| 10 | LaunchDarkly unreachable (fail closed) | EG-ACME with `*.launchdarkly.com` blocked (18 requests aborted) | PASS: the SLD falls back to the legacy view with no Assembly control; /assemblies → "Feature Not Available" |
| 11 | #201 Simplify / Unsimplify (re-check) | own SLD: Select Entire Run on CB-1 → CBL-2 › Simplify Edge, then Unsimplify Edge | PASS: 3× `PUT /api/node/update` 200 → one calculated edge with 3 markers; Unsimplify → batch-positions + 3 node updates, 200 |
| 12 | API with flag OFF (supporting) | Demo: `GET /api/assemblies`; `POST /api/assemblies {}`; `POST /api/assemblies/ai-templates {}` | 200 `[]`; 404 "SLD not found"; 400 "instruction is required". The backend routes answer normally with the flag OFF, so the flag gates the UI only |
| 13 | Console | all runs | no app errors. Noise only: DevRev CORS, Beamer CSP fonts, a few 429s and realtime WebSocket retries |

12 Nov rows not re-run today, all passed then: Save / Replace assembly, thumbnail and detail page, assemblies.manage gating per role, place gating (nodes.manage, site scope), #198 split / combine, #200 elbows follow, fullscreen pickers.

## Bugs found
- **NEW (iOS side, separate ticket):** `BUG-ios-assembly-placement-loses-hidden-and-elbow.txt` (in ../ZP-5107/).
  - When the iOS app places this assembly, the hidden connection becomes a normal visible line and the fixed elbow is lost on the server copy.
  - Web placement keeps both (row 8 is the control).
- No new web defect.

## Open questions (not defects)
- 12 Nov question, still open: PM holds `assemblies.manage` but `/assemblies` is Admin/Super Admin only ("Access Denied"). Should PM be able to open the library?
- The assemblies API answers with the flag OFF (row 12). This is fine if the flag is meant as a UI switch only. Confirm with Eric.

## Not covered
- #198 per-view persistence of combined terminals and dragging a combined terminal.
- Markers measured only by position on the route.

## Cross-platform
- **Twins:**
  - ZP-5102 [iOS] Task #205 (READY TO RELEASE 16 Nov).
  - ZP-5112 [Android] Task #205 (In QA, Android 1.40).
  - No mobile twin for #197–#201: the ticket says assembly controls are web-only. #205 later added mobile placement.
- **iOS source** (eg-pz-mobile-iOS `release/qa` 34bd67e, 1.75):
  - The iOS SLD is the shared web viewer bundle (WebViewBridge), so #198–#201 render through it on iOS.
  - Assembly placement exists natively (`AssemblyPlacementService`, `AssemblyCacheService`).
  - It is gated by the same LaunchDarkly flag: `FeatureFlagManager.boolVariation("feature-sld-assemblies")` in WebViewBridge.
- **Web read-back of an iOS placement** (Android Site 2, placed 16 Nov):
  - The assets and group arrive.
  - The hidden flag and the elbow do not (new iOS bug).
- **Android:** no source access. ZP-5112 is under test now.

### MOBILE-CHECKS
1. **iOS 1.75+, EG-ACME QA, Super Admin seat: flag ON placement fidelity.**
   - Steps: SLD "QA-DEMO v2.6 SLD editing 12 Nov" › unlock › Assembly › "QA4942-FDR-\{name\}" › name IOS17 › Place › tap an empty spot. Wait for sync, then open the same SLD on web.
   - Expected: "QA4942-FDR-IOS17 FUSE nosub → Load-1" shows as a hidden connection (badges, no line), and CB-2 → PNL-2 keeps its elbow, exactly like web placement L2.
   - Today's evidence predicts FAIL (BUG file).
2. **iOS, company code `demo` (flag OFF): Assembly button hidden.**
   - Steps: sign in with the Demo login › SLD (site "Demo") › unlock.
   - Expected: no Assembly button. Group may still show.
3. **iOS, EG-ACME: Select Entire Run on a locked SLD.**
   - Steps: SLD "QA-DEMO v2.6 SLD editing 12 Nov", locked › long-press the CB-1 → CBL-2 connection.
   - Expected: only "Select Entire Run" is offered, as on web, and the whole run is selected.
4. **Android 1.40 (ZP-5112):** checks 1–3 on the same SLD and with company code `demo`.
