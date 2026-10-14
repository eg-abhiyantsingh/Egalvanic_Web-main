# ZP-4514 — Cable drop inserts node buses on its own two halves only, never re-running rules over the whole component

**Verdict:** PASS. The ticket's repro is fixed and no unrelated connection changed. Two **out-of-scope, API-only** validation gaps on the same endpoint (Low).
**Build:** QA `index-C14cwR0U.js` (live 17:32 and 18:11 IST, 13 Oct).
**Tested:** 13 Oct 2026, 17:55–18:02 IST. Project Manager (main), Client Portal. The same result was seen on 12 Oct (`index-D1IWZ6SI.js`).
**Test data (mine, created today):** on site "QA-DEMO ZP-4800 SLD canvas v2.4 (delete me)" (`363d5350-…`): "QA-DEMO ZP-4514 MC / MOL / DS / CB-A / CB-B / PNL-A / PNL-B / PNL-C / CB-C / CB-D / PNL-D (delete me)".
**Ticket re-read live:** In QA, 0 comments. Backend only (eg-pz-backend #1404). The response shape is unchanged, so there is no frontend change.
**Flags:** none.

## Checked

| Check | How | Result |
|---|---|---|
| Repro step 1: MC → Motor Overload directly | Unlock, double-click MC, then MOL › "Connect directly" | PASS. One connection, no bus (`/edge/preview-rules` 200, `/edge/create` 201) (shot 1). |
| Repro step 2: Motor Overload → Disconnect Switch with a Cable | Double-click MOL, then DS › "Insert Cable" | PASS. `POST /edge/insert-pseudo-edge` 201. The result is MOL → **one node bus** → cable → DS. The older MC → MOL connection keeps its id (`c2112144`) and ends, with **no second bus** (shot 2). |
| Unrelated connections untouched | Compared every connection on the site before and after all the drops (35 before) | PASS. 0 removed, 0 re-pointed; only the new halves were added. |
| Other cable drops | CB-A → CB-B; PNL-A → PNL-B; PNL-C → CB-C … CB-D → PNL-D, then CB-C → CB-D | PASS, consistent with the rule check. No bus is inserted where the rule check says none is needed (`allow_direct:true`, or bus-to-bus `insertion_kind: impedance`) (shots 3, 4, 5). **Note:** I found no case on the web where **both** halves get a bus. Dev, please name one pair so it can be re-checked. |
| `/edge/create` still works | "Connect directly" | PASS (201). |
| AC/DC still refused in the UI | Double-click AC panel QA PNL2, then DC capacitor QA DC-CAP1 | PASS. Toast "AC and DC equipment can't be connected directly — insert a rectifier (AC→DC) or inverter (DC→AC)." (shot x3). |
| API negatives | `POST /edge/insert-pseudo-edge` | Class that is not a pseudo-edge class → 400 "class is not a pseudo-edge class". `source:"abc"` → 400 "invalid uuid". Missing class → 400. Random unknown source → refused (QA CDN masks it as HTML, no JSON, nothing created). **Source = target → 201** and **AC → DC → 201**: see out-of-scope gaps. |
| Roles | Client Portal `POST /edge/insert-pseudo-edge` on my assets | PASS. 422 permission_denied (`edges.manage`). |
| resolve-impedance-insertion, bulk upload / bulk edit "behave as before" | — | NOT COVERED. They have no UI path on my data; this ticket did not change them. |
| Performance | Network | Each drop = 1 `preview-rules` + 1 `insert-pseudo-edge` (300–800 ms). Nothing over 2 s. |

## Bugs found (in scope)
None.

## Out-of-scope gaps (API only; the UI blocks both; Low; possibly older than this ticket)
### A. The API accepts a cable from an asset back to itself
1. As a Project Manager, send `POST /api/edge/insert-pseudo-edge` with `source` = `target` = QA-DEMO ZP-4514 PNL-A and the Cable class.
- **What happens:** 201. A cable is created looping from PNL-A back to PNL-A (shot x1).
- **What should happen:** 400 "source and target must differ".
### B. The API accepts a Cable between AC and DC equipment
1. Send `POST /api/edge/insert-pseudo-edge` with source QA PNL2 (AC panel), target QA DC-CAP1 (DC capacitor) and the Cable class.
- **What happens:** 201. It even adds a node bus (shot x2). For the same pair, `/edge/preview-rules` answers `ac_dc_mismatch`, and the UI refuses with a toast (shot x3).
- **What should happen:** 400 `ac_dc_mismatch`, the same as the rule check.

Both are API-only, so a normal user cannot do them from the web. They matter for iOS/offline clients and scripts. I deleted the bogus cables afterwards (`/node/bulk-delete` 200). **Jira check:** no existing ticket found. **Assignee if filed:** Avani.

## Enhancement suggestions
1. Validate source ≠ target and the AC/DC match on the server in `insert-pseudo-edge`, the same way as `preview-rules`.
2. After a cable drop, briefly highlight the bus that was inserted, so the user sees why an extra node appeared.

## Mobile part for Nency
The backend now runs the same half-by-half plan as iOS. On iOS, repeat the repro on a fresh pair: connect a Motor Controller → Motor Overload directly, then Motor Overload → Disconnect Switch with a Cable. Expect one bus on the Motor Overload side only, and no new bus on the first connection. Then refresh the web SLD and check that it shows the same thing.

## Not covered
resolve-impedance-insertion and bulk upload/edit regression; a both-halves-need-a-bus example (needs the dev's rule pair).

## Proposed Jira comment
```
h3. QA 13 Oct, build index-C14cwR0U.js (QA): PASS

Repro on fresh QA-DEMO assets:
# Connect "QA-DEMO ZP-4514 MC" to "QA-DEMO ZP-4514 MOL" (Motor Overload) with Connect directly: no bus is added.
# Connect MOL to "QA-DEMO ZP-4514 DS" (Disconnect Switch) with Insert Cable: you get MOL > one node bus > cable > DS. The first MC > MOL connection is unchanged, with no second bus.
# I compared all 35 connections on the site before and after: none was removed or re-pointed.
# Other cable drops (breaker to breaker, panel to panel, panel-breaker-cable-breaker-panel) add a bus only where the rule check needs one. The UI still refuses AC to DC. Client Portal gets 422.

!QA-13Oct-ZP-4514-1-PASS-step1-MC-to-motor-overload-connected-directly.png|width=720!
!QA-13Oct-ZP-4514-2-PASS-step2-cable-adds-one-bus-on-new-half-only.png|width=720!
!QA-13Oct-ZP-4514-5-PASS-control-bus-breaker-cable-breaker-bus.png|width=720!

*Question:* which asset pair should get a bus on both halves? I could not find one from the web.
*API only (small, separate):* insert-pseudo-edge accepts source = target and AC to DC (201). The UI blocks both.
*Not checked:* resolve-impedance-insertion and bulk upload/edit.

*Status:* moved to READY TO RELEASE
```

## Recommended status
**READY TO RELEASE.** The repro and the "no unrelated edge changes" rows pass. The two API validation gaps are separate, Low tickets.
