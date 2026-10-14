# ZP-4800 — Task #159: Hide/Show connections on the SLD — hidden edges draw as stubs with jump-to badges

**Verdict:** PASS on the canvas, view scope and PDF content, with **one Low PDF bug** (the tag on a left-side terminal is partly hidden by the asset symbol). Separately, an **out-of-scope access bug** on the API this feature saves through.
**Build:** QA `index-C-Cc1qD1.js` (live 16:40 and 17:06 IST, 13 Oct).
**Tested:** 13 Oct 2026, 16:45–17:05 IST. Project Manager (main), Super Admin, Electrical Engineer, Client Portal.
**Test data (mine):** site "QA-DEMO ZP-4800 SLD canvas v2.4 (delete me)" (`363d5350-b42d-4e48-850f-b9f72d27ea08`), view "QA-DEMO ZP-4800 view (delete me)" (`21281f97-…`), my connection "QA-DEMO 4591 SRC CB → QA-DEMO 4799 JBOX" (`30fcae76-…`, left **hidden** for re-checks), older hidden fixture edges `370cd90f` / `5398b751` (All Nodes) and `17478b09` (view only).
**Ticket re-read live:** In QA, 0 comments. Mobile siblings: ZP-4816 (iOS, Ready for QA) and ZP-4804 (Android, Backlog).
**Flags:** GoJS editor only (`feature-gojs-sld` live ON). **SIMULATED OFF** → the older editor ("Select a View to Load Assets"), with no Hide/Show (shot 8).

## Checked

| Check | How | Result |
|---|---|---|
| Edge menu | Unlock, right-click a connection | PASS. "Edit / Hide Connection / Insert Cable / Delete" (shot 1); on a hidden one, "Show Connection". Super Admin sees the same. |
| Hide → stubs + badges | Hide SRC CB → JBOX, clear the selection | PASS. A short stub at each terminal with "QA-DEMO 4799 JBOX (delete me) →" and "QA-DEMO 4591 SRC CB (delete me) →"; no route line (shot 2). `PUT /edge/update` `{edge_manips:{hidden:true}}` 200, saved, still hidden after reload. |
| Badge click | Click "QA-DEMO 4799 JBOX →" at the SRC CB end | PASS. The canvas pans (centre moved from −2182,1476 to −2560,1640) and selects JBOX (shot 3). |
| Select hidden edge | Select it | PASS. The full route shows as a dashed outline `[6,4]` (shot 4). |
| "Show Hidden Connections" toggle | Toolbar toggle on/off | PASS. Ghosted dashed route at opacity 0.4, back to stubs when off (shot 5). |
| Show Connection | Right-click hidden → "Show Connection" | PASS. Full route back; `edge_manips:null` saved (shot 6). |
| Scope isolation (view vs All Nodes) | Add SRC CB + JBOX to my view; open the view | PASS. In the view only the view's own hide (`17478b09`) is a stub; the All-Nodes hides (`30fcae76`, `370cd90f`) draw normally (shot 9). The All Nodes canvas does not hide `17478b09`. Note: `/sld-view/{id}/graph` still returns the All-Nodes `edge_manips` for edges with `has_view_specific_points:false`; the web client correctly ignores them (iOS must too, see Nency). |
| Straighten / magic layout ignore hidden edges | Straighten Downstream (S) and Magic Layout (M) on SRC CB with its only link hidden | PASS. JBOX did not move; no position writes. |
| Tracing / selection still treat them as real | Trace Lineage (toolbar) from SRC CB | PASS. JBOX highlighted through the hidden stub (shot 4803-2). Select Up/Downstream now skips hidden links by design (ZP-4803). |
| PDF export, All Nodes | `POST /sld/export/pdf/start` (same body as the dialog) → complete in 16 s, 276 KB | PASS. Hidden edges print as short stubs ending in "TO QA PNL1", "TO FDR-QA PNL1", "TO QA-DEMO 4799 JBOX (delete me)" (shot 10). **Low bug:** at the JBOX left terminal the tag runs to the right, into the symbol, so "TO QA" is covered (shot 11). |
| PDF export, named view | Same with `view_id` → 8.6 s, 100 KB | PASS. Only the view's own hide prints as "TO QA CB1 / TO QA SWBD1"; the All-Nodes hidden `370cd90f` is drawn normally (shot 12). |
| DXF export | `POST /sld/export/dxf/start` → complete in 15 s, 3.9 MB | Job completes and the file downloads. Content not checked: the text is vectorised (21,792 LINE entities) and there is no DXF viewer here. |
| Bad payloads (my edge) | `edge_manips:{hidden:"yes"}`, `edge_manips:[1,2]` | Both 200 and stored as sent (`[1,2]` read back). No validation on this JSONB. Restored to `{hidden:true}`. Low; not filed. |
| Bad ids | `PUT /edge/update/<random uuid>`, `/not-a-uuid` | 200 HTML (QA CDN masks 404 as the app page). No JSON, so nothing written. |
| Roles | Client Portal `PUT /edge/update` on my edge | 422 permission_denied (correct). Client Portal `/slds` = Access Denied, and the nav has no SLD link. |
| **Site scope** | Electrical Engineer **not assigned** to my site: `GET /sld/{id}/graph` → 422, but `PUT /edge/update/30fcae76…` `{edge_manips:{hidden:true,qa_probe:"EE-unassigned-ZP4800"}}` | **200, and saved** (read back by PM after 9 s). See out-of-scope bug 2. Restored. |
| Performance | Network | Hide/Show = one `PUT` each (300–700 ms). SLD load: 34 calls, graph 925 ms, no slow calls; duplicates `translations ×4`, `plug-config ×2` (not this ticket). |

## Bugs found

### 1 (in scope, Low): in the PDF, the "TO …" tag on a left-side terminal is partly covered by the asset symbol
1. Open an SLD with an asset whose **left** terminal has a connection (e.g. a Junction Box fed on "Left In").
2. Right-click that connection → **Hide Connection**.
3. Click **Export** → PDF.
- **Actual:** at the junction box end the tag starts at the terminal and runs right, across the symbol. The filled symbol hides "TO QA", so it reads "-DEMO 4591 SRC CB (delete me)". On the canvas the badge is correctly drawn to the left (shot 2).
- **Expected:** the tag points away from the asset (to the left for a left terminal), fully readable, like on the canvas.
- **Evidence:** `QA-13Oct-ZP-4800-11-FAIL-pdf-TO-tag-covered-by-symbol-on-left-terminal.png`, `QA-13Oct-ZP-4800-2-PASS-hidden-edge-stubs-with-badges.png`.
- **For the developer:** backend PDF export (#1533) places the TO tag at a fixed right offset regardless of the terminal side.
- **Suggested assignee:** Avani (owner rule 13 Oct: this batch goes to Avani); ticket owner Eric. **Jira check:** no existing ticket (searched "hidden edge" / "TO tag", 60 days).

## Out-of-scope bugs (not caused by this ticket; the feature saves through these endpoints)
### 2 (Medium, security): a user not assigned to a site can still change its connections and assets
1. Sign in as a seat **not** assigned to the site (here the Electrical Engineer for "QA-DEMO ZP-4800 SLD canvas v2.4 (delete me)"). Opening the site's SLD is refused (`GET /sld/{id}/graph` → 422).
2. Send the same save the canvas sends when you hide a connection: `PUT /api/edge/update/{edgeId}` with `edge_manips`.
- **Actual:** 200, and the change is saved (the site's PM sees it after reload). A Project Manager also gets 200 on `PUT /api/node/update/{id}` for an asset on an unassigned site (tested with a no-op body only).
- **Expected:** refused (422/403) for sites outside the user's `accessible_sld_ids`, like the read.
- **Why it matters:** someone without access to a building can still hide or rewire its one-line diagram.
- **Evidence:** API read-back in `.playwright-mcp/v24/sld5/out/g1.out` (`edgeAfterEE`) and `d1.out`; screen the site owner sees: `QA-13Oct-ZP-4800-2-PASS-hidden-edge-stubs-with-badges.png`.
- **Jira check:** related but different: ZP-4849 (SKM import to an unassigned site), ZP-4709 (CP read). No ticket for `node/update` / `edge/update`. Suggested assignee: Avani.

## Enhancement suggestions
1. A "Hidden connections (N)" list in the toolbar, with jump-to, so users can find and un-hide them on large SLDs.
2. Validate `edge_manips` on the backend (`hidden` must be boolean; object only).
3. Optional toggle on the PDF export dialog: "print hidden connections as stubs / full lines".

## Mobile part for Nency
ZP-4816 (iOS, Ready for QA) and ZP-4804 (Android, Backlog). After the OTA viewer publish: open my view "QA-DEMO ZP-4800 view (delete me)" on site "QA-DEMO ZP-4800 SLD canvas v2.4 (delete me)". Only the SWBD1 → CB1 connection must be a stub with badges. SRC CB → JBOX must be a full line in the view, although it is hidden in All Nodes. Pull to refresh and check that elbows/hides stay. In All Nodes, SRC CB → JBOX, cable → PNL1 and PNL3 → FDR-QA REC1 must be stubs.

## Not covered
- DXF content (vector text, no viewer).
- The "un-hide a fixed-edge chain keeps its elbows" row: no elbow chain exists on my data, and creating one needs the elbow tool. The code restores `edge_manips.elbows` on show (`Hb` → `or()`), but this was not proven live.

## Proposed Jira comment
```
h3. QA 13 Oct, build index-C-Cc1qD1.js (QA): PASS, 1 small PDF issue filed separately

# Right-click a connection > Hide Connection: it turns into short stubs with "<other end> →" badges, and the line is gone. It stays hidden after a reload.
# Clicking a badge pans to and selects the asset at the other end. Selecting the hidden connection shows the full route dashed.
# "Show Hidden Connections" shows hidden ones as faded dashed lines. Show Connection brings the line back.
# A hide in All Nodes does not show inside a view, and a hide in a view does not show in All Nodes.
# Straighten / Magic Layout ignore hidden connections; Trace Lineage still follows them.
# PDF: hidden connections print as stubs with "TO <other end>"; a view's PDF only uses that view's hides.
# Client Portal cannot hide connections (422).

!QA-13Oct-ZP-4800-2-PASS-hidden-edge-stubs-with-badges.png|width=720!
!QA-13Oct-ZP-4800-9-PASS-view-scope-isolation.png|width=720!
!QA-13Oct-ZP-4800-10-PASS-pdf-hidden-edges-print-TO-tags.png|width=720!

*Small issue (new ticket):* in the PDF, on a left-side terminal the "TO ..." tag runs over the asset symbol and "TO QA" is hidden.
!QA-13Oct-ZP-4800-11-FAIL-pdf-TO-tag-covered-by-symbol-on-left-terminal.png|width=720!

*Not checked:* DXF content (no viewer), un-hiding an elbow chain. Mobile goes to Nency (ZP-4816 / ZP-4804).

*Status:* READY TO RELEASE (web); PDF tag issue tracked separately.
```

## Recommended status
**READY TO RELEASE** (web). The PDF tag overlap is a Low, separate ticket. The site-scope write gap is pre-existing and goes in its own ticket. Mobile goes to Nency.
