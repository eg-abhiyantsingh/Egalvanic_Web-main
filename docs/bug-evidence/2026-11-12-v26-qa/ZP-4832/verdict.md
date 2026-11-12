# ZP-4832 — SLD viewer: photo-count badge becomes a "Photos" status overlay; photo popover dropped; badge hidden in the iOS embed

**Verdict (web):** PASS — QA, Web v2.6, build `index-C1ZRIunF.js`, 12 Nov 2026 16:42 IST, Super Admin seat. The non-eng-lib menu is
verified only SIMULATED; the iOS embedded viewer part is mobile-only (not web-testable).

**Test data:** read-only on the shared QA site "Test 123456" (SLD `42a01b1d-1678-4fae-81c9-3e89ddd51cea`, 17 assets, 8 with photos —
found by scanning `GET /api/sld/{id}/graph` `photo_count` over 31 SLDs). Nothing was edited (overlay mode is a view setting).

## Checked
| Check | How | Result |
|---|---|---|
| Photos mode in the overlay menu (eng-lib ON) | SLD › status overlay button | PASS — Arc Flash Readiness · Custom Attributes Only · Engineering Status · Shutdown Rules · Condition Assessment · **Photos** · Power Scheme · Off (5 Nov: no Photos) |
| Photos mode shows count badges | select Photos | PASS — photo-count badges on the assets with photos (`_photoBadgeModelData.showPhotoBadges = true`) |
| Photos mode does not paint readiness | compare with Arc Flash Readiness | PASS — Arc Flash Readiness paints a red readiness border on "Fuse 1"; Photos shows no readiness border/fill, only badges |
| Other modes keep working | Arc Flash Readiness, Off | PASS — badges hidden (`showPhotoBadges = false`) |
| ZP-4435 popover removed | rest on "Test 1234", press Space, click it (SLD locked) | PASS — no second photo pop-up; the only card is the Task #23 hover preview ("Test 1234 · Other · 9 photos +5"); after the click nothing is open. Bundle: 0 × `photoQuickView` |
| Task #23 hover preview still works | rest the mouse 3 s | PASS — card with thumbnails (`GET /api/photo/by_entity/…` 200, 630 ms) |
| Non-eng-lib company | **SIMULATED** `feature-eng-lib=false` (evalx rewritten, LD stream blocked) | PASS (simulated) — Arc Flash Readiness · Photos · Power Scheme · Off; Photos still shows badges |
| Permission photos.view | role probe | every web seat (Admin, PM, AM, EE, FM, CP) holds photos.view, so the "no photos.view → no Photos item" case could not be run |
| Console | during the whole run | no app errors (no bridge/listener errors) |

## Bugs found
None for this ticket.

Observation (question, not filed): on "Test 1234" the badge/`photo_count` says **12** while the hover preview says **9 photos**
(`/photo/by_entity` is filtered to node_profile, node_nameplate, node_arc_flash_sticker, node_other). The badge probably counts
other photo types too (e.g. IR). Ask Eric which number the Photos badge should show.

## Enhancement suggestions
- The hover preview loads the full 2048-px originals for 3 small thumbnails (images still loading after 4 s); serve thumbnails.
- Badges are tiny at fit-to-screen zoom; a minimum on-screen size for the badge would help on large SLDs.

## Mobile impact
iOS/Android (embedded flow-only viewer, after the OTA publish of the SLD viewer bundle): confirm **no** photo-count badge and that tapping a
node does not open the native photo sheet; Nency should check on both apps.

## Not covered
- iOS embedded viewer (mobile).
- A real non-eng-lib company (eng-lib OFF only simulated).

## Flags needed
None to flip for Acme; to see the real non-eng-lib menu, check a QA company without `feature-eng-lib` (or Shubham flips `feature-eng-lib`
OFF for a test company, QA env).

## Proposed Jira comment
See `ZP-4832.txt`.

## Recommended status
READY TO RELEASE for web (non-eng-lib row simulated — say so in the comment); iOS embed check goes to Nency.
