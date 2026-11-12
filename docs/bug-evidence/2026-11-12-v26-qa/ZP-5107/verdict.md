# ZP-5107 — Task #205: offline-first SLD layout groups + assembly placement (web)

**Verdict:** PARTLY — online web part PASS on QA (Web v2.6, build `index-C1ZRIunF.js`, 12 Nov 2026 16:49 IST, Super Admin seat);
the offline-first part is NOT WEB-TESTABLE in a desktop browser (it runs through the iOS viewer bridge).

**Why the offline part is not web-testable:** the shipped assembly client (`AiTemplateEdit-*.js`) sends place/place-preview through the
native bridge (`window.webkit.messageHandlers` → `graphUpdate {type:"nativeApi"}`) only when the SLD runs inside the iOS app; in a browser it
calls `POST /api/assemblies/{id}/place` directly. Layout-group writes likewise go straight to `/node-group` in a browser. The frontend PR is
titled "iOS viewer - layout groups + offline assembly placement"; the iOS ticket is ZP-5102, Android ZP-5112. (Supporting only — the verdict
below is from the browser run.)

**Test data (own):** site "QA-DEMO v2.6 SLD editing 12 Nov" (`c4438cd3-…`); new group "QA-DEMO ZP-5107 grp 12 Nov" (Load-21 + Load-22).

## Checked
| Check | How | Result |
|---|---|---|
| Create a layout group (web) | select Load-21 + Load-22 › Add to… › Add to Group › Create new group › name › Create Group | PASS — `POST /api/node-group` 201 |
| Groups persist on reopen | reload the page | PASS — canvas + `GET /api/sld/{id}/node-groups`: "QA-DEMO ZP-5107 grp 12 Nov" [Load-21, Load-22], "QA-DEMO ZP-4942 grp 12 Nov" [CB-2, PNL-2] and the assembly copy |
| Assembly placement keeps groups (online) | placed QA4942-FDR-L1 (ZP-4942 run) | PASS — group recreated as "QA-DEMO ZP-4942 grp 12 Nov QA4942-FDR-L1" with the placed CB-2/PNL-2 |
| Place an assembly while offline, sync after its node/edge creates | — | NOT WEB-TESTABLE (iOS bridge path) → mobile |
| node_group/update mutation replays in tier order | — | NOT WEB-TESTABLE from a browser (native offline queue → backend); no web request uses it |
| Observation: desktop browser offline | `context.setOffline(true)`, create a group, back online, reload | the `POST /api/node-group` fails, no message is shown, the group is not created and nothing is queued (as expected for a non-offline web page) |
| Console | run | no app errors |

## Bugs found
None for the online web part.

## Enhancement suggestions
- When the browser is offline, "Create Group" fails silently — show "You're offline, the group was not saved" (the request just dies).

## Mobile impact
This is the main test for mobile: on iOS (ZP-5102) and Android (ZP-5112) place an assembly and create a layout group while offline, then
reconnect and check in Admin › Audit Log (Mutation Audit Log) that `node_group/update` replays after its node/edge creates, and that the
group and members appear on web.

## Not covered
Offline assembly placement and mutation replay order (mobile-only path).

## Flags needed
`feature-sld-assemblies` (same as ZP-4942) for the placement part.

## Proposed Jira comment
See `ZP-5107.txt`.

## Recommended status
Stay **In QA** — web online part passes; the offline rows need the iOS/Android check (ZP-5102 / ZP-5112) before this can be released.
