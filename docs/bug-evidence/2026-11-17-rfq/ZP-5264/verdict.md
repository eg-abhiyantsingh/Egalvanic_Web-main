# ZP-5264: SLD Assemblies, the second placement suggests "L1 CB-3" instead of the next free name

**Verdict: PASS on web.** There is one lead for iOS: see "Mobile" below.

- **Build:** QA `index-oFAZQ3rK.js`, 17 Nov 2026, 11:15–11:52 IST.
- **Seat:** Super Admin (`VALID_EMAIL`, is_eg_admin true).
- **Fix:** eg-pz-backend PR #1815 ("Merged to QA", Dharmesh, 16 Nov). The backend repo is not readable from this machine (`gh` can see only eg-pz-frontend and eg-pz-mobile-iOS), so the PR diff was not read. That comment is the only one on the ticket, and it adds no test rows.
- **LaunchDarkly:** `feature-sld-assemblies` = **true** for Acme on QA. This is the live evalx response, read at 11:15:44 and again at 11:15:53 IST. The flag OFF state was not tested: I did not flip a company flag. The OFF state only hides the feature, which ZP-4942 covers (simulated there).

Evidence: video `QA-17Nov-ZP-5264-next-assembly-name-video-steps.mp4` (59 s), screenshots `QA-17Nov-ZP-5264-1…4-*.png` (outline boxes only, no text drawn in; re-taken at 11:51 IST), Jira draft `ZP-5264.txt`.

## What was run
**Ticket repro.** Site "QA-DEMO v2.6 SLD editing 12 Nov" (SLD c4438cd3-59b3-4639-9e60-2752dc5aa478). The assembly "QA4942-FDR-{name}" (cd981de3…) is placed there once as L1 (nodes "QA4942-FDR-L1 CB-2 / FUSE nosub / Load-1 / PNL-2").
- `POST /api/assemblies/cd981de3…/place-preview {"sld_id": c4438cd3…}` → 200 `"value":"L2"`, instance "QA4942-FDR-L2". Labels: "QA4942-FDR-L2 FUSE nosub / CB-2 / Load-1 / PNL-2". A second identical call also returns "L2".
- UI: unlock › + Assembly › pick the assembly › the Name box is pre-filled **"L2"**, and "Place QA4942-FDR-L2" is shown. On 12 Nov this was "L1 CB-3".

**Edge rows on my own site** "QA-DEMO ZP-5264 17 Nov" (SLD 753d9c41-b46b-4e5b-b144-75d8e84eea14). It started with one anchor transformer.

| Row | Action | Suggested name |
|---|---|---|
| E1 | No copy on the SLD yet | blank: "Type a name to see what will be created." The API value is "". |
| E1 | Typed L1 › Place (UI) | 4 nodes "QA4942-FDR-L1 …" created |
| E2 | + Assembly again (UI) | **L2** → placed |
| E3 | + Assembly again (UI) | **L3** |
| E3b | Typed L1 (already used) | each device is marked "exists", plus "4 label(s) already exist on this SLD — they will be duplicated." Place stays enabled (a warning, by design) |
| E4 | API place "L9" | **L10** (multi-digit increment, not "L91" or "L9 CB-3") |
| E5 | API place "Feeder A" (no number) | still **L10** |
| E5b | API place "L1" a second time | still **L10** |
| E6 | Delete the 4 L9 nodes (`DELETE /api/node/delete/{id}` ×4, 200) | **L3**. Deleted copies are ignored. |
| E7 | API place with an empty name (the server picks) | the server created **L3**, then suggested **L4** |
| E8 | Ticket SLD at the same time | **L2**. The suggestion is per SLD. |
| E9 | UI after all of the above | **L4** |
| Video | Placed L4 in the UI, then + Assembly again | **L5** |
| Screenshots run (11:51) | Placed L5 in the UI, then + Assembly again | **L6** |

- Bad input: place-preview with no `sld_id`, or with a zero UUID → 404 "SLD not found".
- Console: no app errors. Only third-party `ERR_FAILED` and realtime WebSocket retries, as on every SLD load.

## Not covered
- Flag OFF, as above.
- Other roles. Placement gating (nodes.manage, site scope) was covered on 12 Nov in ZP-4942. This fix only changes the suggested value.
- An assembly whose name has no `{name}` placeholder (the "QA{#}" pattern path). Only one assembly exists in the company library, and I did not create a second company-wide assembly.

## Mobile (lead for iOS, not verified on a device)
The web SLD code computes this suggestion **on the device** when it runs inside the iOS app (`window.webkit.messageHandlers` present). This is the native-bridge / offline path in chunk `AiTemplateEdit-C7uZ-LBW.js`: function `Y` is called through `place-preview` when `D()` is true. That client-side function is byte-identical to the 13 Nov build (`AiTemplateEdit-xhFdbZ-u.js`, same 10,445 bytes), so the backend fix does not reach it.

I ran the shipped function in Node on the ticket's data. With labels "QA4942-FDR-L1 CB-2 / FUSE nosub / Load-1 / PNL-2" it still returns **"L1 CB-3"**, with or without member label templates. With only "QA4942-FDR-L1" it returns "L2". The cause: the base pattern `^QA4942-FDR-(.+?)$` also matches the member labels, and the code keeps the highest trailing number.

This needs an iOS check: SLD › + Assembly after one placement, in the iOS app (iOS placement ticket ZP-5102). If it reproduces, it should be filed as a separate iOS/frontend bug. I did not file it, because there was no device run (rule: no verdict from bundle reading alone). Android was not checked.

## New defects
None confirmed. See the iOS lead above.

## Test data left behind (QA-DEMO, mine)
Site "QA-DEMO ZP-5264 17 Nov" with copies L1 (×2), L2, L3, L4, L5 and "Feeder A", plus the anchor transformer. The ticket site was only read (no placement).
