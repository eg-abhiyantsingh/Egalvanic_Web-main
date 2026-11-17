# ZP-4943: #202 locked-group members stay clickable + #203 Assemblies "Edit with AI"

**Verdict: PASS → recommend READY TO RELEASE.** Both open items from 12 Nov are now done:
- The real `feature-sld-assemblies` OFF/ON check (Demo Company OFF, EG-ACME ON; see ZP-4942).
- The row not covered on 12 Nov, "hidden-edge badges and pseudo-edge markers inside a locked group", now passes.

- **Build:**
  - EG-ACME `index-LGhrn-48.js`, 17 Nov 2026 14:09–14:11 IST.
  - Demo OFF run: `index-CFMAmUdK.js` 14:01 IST (same code, only the build stamp differs).
- **Seats:** Super Admin (EG-ACME). Demo staff login, read only.
- **Test data (own):** site "QA-DEMO v2.6 SLD editing 12 Nov" (SLD `c4438cd3-59b3-4639-9e60-2752dc5aa478`).
  - New group "QA-DEMO ZP-4943 badge grp 17 Nov": QA4942-FDR-L1 FUSE nosub + Load-1, with the hidden connection between them.
  - New group "QA-DEMO ZP-4943 marker grp 17 Nov": QA26 CB-1 + PNL-1, run simplified for the test, then unsimplified again.

- **Re-check on the newest build:** `index-CkkAeA9o.js` was deployed at about 14:50 IST, during this round. At 14:56 IST, Demo Company (flag OFF) still shows no Assembly button and /assemblies says "Feature Not Available". EG-ACME (flag ON) still shows the Assembly button, and the next suggested name is now **L3** (after L2). The SLD, AiTemplateEdit and AuditLog chunks of that build differ from `index-LGhrn-48.js` only by renamed imports: the files are the same size and the code is the same.

## Checked today
| Check | How | Result |
|---|---|---|
| New group starts locked, on the Background layer | Add to… › Add to Group › Create new group | PASS: `POST /api/node-group` 201; group `_locked` true, layer "Background" |
| Control (no group): hidden-connection badge | click the badge "QA4942-FDR-L1 Load-1 →" | jumps to and selects `QA4942-FDR-L1 Load-1`; the tooltip "Hidden connection — go to …" shows |
| **Hidden-edge badge inside the locked group** | same badge, group locked | **PASS:** selects `QA4942-FDR-L1 Load-1`, not the group box |
| Other badge inside the locked group | badge "← FUSE nosub" end | PASS: selects `QA4942-FDR-L1 FUSE nosub` |
| **Pseudo-edge marker inside a locked group** | Select Entire Run on CB-1 → PNL-1 › Simplify Edge (calculated edge, 3 markers); group CB-1 + PNL-1 (locked); click a marker | **PASS:** selects the calculated edge `pseudo:3ad0222c…`, not the group |
| Right-click a marker inside the locked group | right-click | PASS: the connection menu opens (Select Entire Run · Unsimplify Edge · Edit/Reveal QA26 CBL-2 · Node Bus · CBL-3 · Delete), not the group menu (Unlock/Delete) |
| Restore | Unsimplify Edge | PASS: batch-positions + 3 node updates, all 200; the cables and bus are back on the line |
| #203 Edit with AI API (re-check) | `POST /api/assemblies/ai-templates` | PASS: 200 in 2.7 s → `{"n0":"{name}-F1","n1":"{name}-CB1"}`; 2001 chars → 400 "instruction must be at most 2000 characters"; no nodes → 400 "nodes is required"; the assembly's `modified_at` is unchanged (12 Nov 11:01Z), so nothing was saved |
| Flag OFF (real) | Demo: Add to… has no "Assembly…", /assemblies "Feature Not Available" | PASS: Save as Assembly and the library are absent, so Edit with AI can't be reached |
| Flag ON | EG-ACME: Add to… › Assembly… present | PASS |

12 Nov rows, not re-run today and passed then:
- Member click and connection right-click in a locked group.
- Drag a member moves the whole group with its elbow.
- The elbow does not drag alone.
- Edit with AI in the Save dialog and on the detail page.
- 422 for unsafe instructions and for seats without assemblies.manage.

## Bugs found
None new. The 12 Nov side note (dragging a locked group also sends `PUT /api/node/update/{group id}` → 400, no visible effect) was not re-checked today.

## Not covered
- Terminal-handle and floating-label drag negatives.
- The >2000-node selection bound.

## Cross-platform
- **Twins:** no iOS/Android twin for #202/#203 (JQL on "#202", "#203", "locked group", "assembl…" in the last 60 days). The related mobile tickets are only #205: ZP-5102 iOS (RTR) and ZP-5112 Android (In QA).
- **#202** lives in the shared SLD viewer bundle, which iOS loads in its WebView (eg-pz-mobile-iOS `WebViewBridge.swift`), so the same behaviour should reach iOS/Android with the viewer OTA.
- **#203 (Edit with AI)** is web-only: there is no assembly Save dialog on mobile, and mobile only places assemblies.

### MOBILE-CHECKS
1. **iOS 1.75+, EG-ACME: tap a badge inside a locked group.**
   - Steps: SLD "QA-DEMO v2.6 SLD editing 12 Nov" › find group "QA-DEMO ZP-4943 badge grp 17 Nov" › tap the badge "QA4942-FDR-L1 Load-1 →".
   - Expected: the viewer jumps to and selects Load-1, not the group.
2. **iOS: tap a member inside a locked group.**
   - Steps: same group › tap "QA4942-FDR-L1 FUSE nosub".
   - Expected: the asset is selected and its details open, not the group.
3. **iOS: drag a member of a locked group** (unlock editing first).
   - Expected: the whole group moves together.
4. **Android 1.40:** same three checks.
