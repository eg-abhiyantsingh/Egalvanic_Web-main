# ZP-4594 — SLD canvas notes: annotate single-line diagrams with sticky notes (web + API)

**Verdict:** FAIL. One reproducible defect in the main create-and-type flow. Everything else passes, including the API contract, tenant scope, roles and XSS.
**Build:** QA `index-D1IWZ6SI.js` (redeployed about 18:50 IST; its SLD chunks are identical to `index-BdXGlAq5.js` once hashes are ignored).
**Tested:** 12 Oct 2026, 18:50–19:20 IST, acme.qa.egalvanic.ai. Mainly the Project Manager seat; API checks from every role.
**Test data:** my own site `QA-DEMO ZP-4594 SLD notes groups realtime v2.4 (delete me)` (`bb593be6-4a12-4505-a2a9-d63ddeb64de9`) and its named view `QA-DEMO ZP-4594 view (delete me)` (`0db55cd0-…`). Notes kept: `110ab255-…` (All Nodes) and `a2d02522-…` (named view). Every other probe note was soft-deleted.
**Flags:** none. Notes have no LaunchDarkly gate; the editor needs `feature-gojs-sld`, which is live ON.

## Checked

| Check | How | Result |
|---|---|---|
| **+ Note** button | Open the SLD locked, then unlock it with the lock button at the bottom right | PASS. The button is hidden while the graph is locked and shows as "Note" once unlocked. Clicking it changes the cursor to a crosshair (staging). |
| Place a note (All Nodes) | Click + Note, then click empty canvas | PASS. `POST /api/canvas-notes` returns 201 with the client-minted id, `sld_view_id:null` and `author_name:"Abhiyant Singh"`. |
| **Show the typed text right after the first edit** | Type into the new note, then click empty canvas | **FAIL (bug 1).** The note shows the grey "Double-click to write a note" placeholder, although the text was saved (`PUT` 200). It appears only after a refresh. Reproduced 3/3: twice in All Nodes, once in a named view. Control: editing an **existing** note renders straight away (shot 7). |
| Markdown subset | ⌘B / ⌘I / ⌘U, the toolbar "Bulleted list" button, and a line starting with "# " | PASS. Saved body: `… **bold** *italic* <u>under</u>\n# Heading one\n- item one\n- item two`. After a refresh it renders bold, italic, underline and a heading (shot 5). The numbered-list button was not clicked separately (same code path as bullets). |
| Format menu (right-click on the note) | Colour swatch, text size +/−, align left/centre/right, vertical align top/middle/bottom | PASS. Each change sends one `PUT` (`color` / `text_style`) and survives a refresh (shots 3–5). |
| Move | Drag the note | PASS. `PUT {x,y}` returns 200 and the note is still there after a refresh. |
| Per-view placement | Make a named view (fixture), open it, add a note | PASS. `POST` carries `sld_view_id=<view>`. `?view_id=<view>` lists only that note, `?view_id=all` lists only the All-Nodes notes, and each view draws only its own notes (shot 9). |
| Delete (soft tombstone) | Right-click, then "Delete note" | PASS. `DELETE` returns 200, the note disappears and stays gone after a refresh (shot 10). The `/api/sld/{id}` and `/sld/v2/{id}` payloads still carry it with `is_deleted:true`, which the offline sync needs. |
| Bad colour / coordinates / body length | `POST` with `color:"red"`, `x:1e8`, a 5001-character body | PASS. All three return 400 with a clear message; a 5000-character body is accepted. |
| Size range 40–4000, `text_style` ranges | `POST` width 39 / height 4001, `text_style.size` 500 / 2, `PUT` width 5000 | **Differs from the QA Review row.** The values are **clamped** (40 / 4000 / 96 / 8 / 4000) and the call returns 201/200, not 400. Harmless, but the ticket says "return 400". Dev to confirm which is intended. |
| `text_style` enums | `align:"diagonal"`, `valign:"diagonal"` | PASS. 400 with a clear message. |
| View from a different SLD | `POST` with a view id of another SLD, or a random view id | PASS. 400 `invalid sld_view_id`. Note: `PUT` with another SLD's view returns 200 and silently **ignores** the field (the note stays in All Nodes), so nothing leaks. |
| Missing or malformed `sld_id` | `POST` with `sld_id:null` or `"not-a-uuid"` | PASS. 400 `sld_id required`. A random `sld_id` returns 404 (the HTML page on QA). |
| Idempotent create (offline replay) | The same `POST` twice | PASS. 201, then 200 with the same id; one row only. Replaying the create after a delete returns the tombstone (`is_deleted:true`) and does **not** bring the note back. |
| Delete twice / edit a deleted note | Second `DELETE`; `PUT` on a deleted note | Second delete returns 200 (fine). `PUT` on a deleted note returns 200 with the old body, but nothing is applied. 404 would be clearer (low). |
| Stored XSS | `POST` bodies with `<img onerror>`, `<script>`, `<u onmouseover>`, then open each in the in-place editor | PASS. The canvas and editor show them as plain text, with no `<img>`/`<script>` or handlers in the editor DOM, and `window.__qaXss` stays undefined (shot 11). |
| Site scope (same company) | PM on acme site "Test site A" (`b99f9c84-…`), which is not assigned to PM | PASS. 404 (HTML). Positive control: Super Admin gets 200 JSON from the same call. |
| Other company | `GET` notes for `f86052b7-…` | Refused for PM and for staff on the acme host (staff `GET /sld/{id}` also returns 422). |
| Roles | `/auth/v2/me` permissions plus a create/edit probe on my site from each seat | PM and Admin can add, edit and delete (`nodes.manage`). **Client Portal** (`slds.view` only): `POST`/`PUT` return **422 permission_denied**, and `/slds` shows "Access Denied" while the nav has no SLD link, so nav and route agree (shot 12). AM, EE and FM hold `nodes.manage` too, but my new site is not assigned to them (404). Technician and Portal Sales: "Web Access Restricted" (shot 13). With the graph locked, notes are read-only for everyone (`movable:false`, `editable:false`; shot 14). |
| Performance | Network log | `GET /sld/{id}/canvas-notes` runs **once** per site load at 266–693 ms. Create takes 0.5–0.7 s. Each format-menu click sends one `PUT` (0.6–0.75 s), so clicking size + twice sends 2 PUTs. No duplicate GETs. |

## Bugs found

### Bug 1: a new sticky note shows "Double-click to write a note" instead of the text you just typed
- **Severity:** Medium. The user believes the text was lost and may type it again; the text only appears after a page refresh.
- **Steps:**
  1. Open any SLD (All Nodes or a view) and click the lock button (bottom right) to unlock editing.
  2. Click **+ Note**, then click an empty spot on the canvas.
  3. Type some text, e.g. "test note".
  4. Click an empty spot on the canvas to finish.
- **Actual:** the note shows the grey placeholder "Double-click to write a note". While you type, that placeholder is also drawn behind your text. The text **is** saved (`PUT /api/canvas-notes/{id}` returns 200 with the body) and shows after a refresh.
- **Expected:** the note shows the typed text straight away, as it does when you edit an existing note.
- **Control:** double-click an existing note, change its text and click away. The new text shows at once.
- **Reproduced:** 3/3 (All Nodes ×2, named view ×1).
- **Evidence:** `QA-12Oct-ZP-4594-6-FAIL-new-note-shows-placeholder-after-typing.png`, `QA-12Oct-ZP-4594-2-FAIL-first-sighting-new-note-shows-placeholder.png`, `QA-12Oct-ZP-4594-8-PASS-repro-note-text-after-refresh.png`, `QA-12Oct-ZP-4594-7-PASS-control-edit-existing-note-renders.png`.
- **Likely cause (from the bundle):** the `POST` response handler sets `author_name` and calls the notes redraw (`_syncCanvasNotes`), which rebuilds every note part **while the text editor is still open**. `TextEdited` then updates the removed part, and the new part keeps the empty body.
- **Suggested assignee:** Krunal (frontend), ticket owner Eric Ehlert.
- **Jira check:** no existing ticket (searched "canvas note" / "sticky note", last 30 days).

## Observations (not filed)
- The size and text-size ranges are clamped rather than rejected with 400, unlike the QA Review row.
- `PUT` on a deleted note answers 200 without changing anything.
- Bodies written outside the web editor are read as Markdown. A raw `__x` came back as italic, because the web editor escapes `_` but other writers may not. iOS notes with underscores (e.g. "MCC_1_A") could be reformatted on web; see Nency's list.

## Enhancement suggestions
1. **Notes panel with a count and jump-to,** like the Groups panel. Notes are hard to find on large SLDs.
2. **"Edited by / when" on hover.** `modified_by` and `modified_at` are already stored but never shown.
3. **Undo for delete** (a toast "Note deleted · Undo"). The delete is soft, so a restore is cheap.
4. **Show when text is cut off.** A 200×120 note at 16 px clips list items silently; offer auto-grow or a "…more" marker.
5. **Fewer calls from the format menu.** Batch or debounce the size/align clicks (one `PUT` per click today).
6. **Delete key for a selected note.** Notes are `deletable:false`, so the right-click menu is the only way.

## Mobile part for Nency
- **ZP-4595** (iOS notes, Ready for QA), **ZP-4596** (Android, Backlog), **ZP-4620** (iOS deletes never synced, Ready for QA).
- Open site `bb593be6-…` on iPad. The All-Nodes note (blue, "Heading one") and the named-view note must show in their own view only, at the same spot.
- Edit or delete on iPad, then refresh web: the change or tombstone must sync. A replayed offline create must not duplicate.
- Type `MCC_1_A` and `*text*` on iOS and check web shows them literally (web reads the body as Markdown).
- On an older app build without `supportsCanvasNotes`, notes must be read-only.

## Not covered
- How Client Portal sees notes in the Maintenance Portal one-line (read-only). No CP-reachable site has a QA-DEMO note; CP writes are proven refused through the API.
- Resizing a note by its drag handles (sizes were set through the API), the "T" key shortcut, and the numbered-list button.
- The `cnote_a1` migration chain (a deploy step; the new columns `color`, `text_style`, `modified_by` and `sld_view_id` are present in the API).

## Re-check 13 Oct on index-C-Cc1qD1.js
Live QA build `index-C-Cc1qD1.js` (deployed 13 Oct 15:21 IST, newer than `index-COeCBn0z.js`; re-checked again before writing this). Re-run 13 Oct 15:57–16:07 IST, Project Manager seat, my site `QA-DEMO ZP-4594 SLD notes groups realtime v2.4 (delete me)` (`bb593be6-…`), All Nodes. Live ticket re-read: still In QA, no comments. LD: notes have no flag; `feature-gojs-sld` ON. Scripts: `.playwright-mcp/v24/sld-13oct/a, a2, e, f .body.js`; Client Portal / scope probes in `c.body.js`.

**Bug 1 still holds, and it is worse than reported on 12 Oct: the typed text can be lost for good.**

| Check | Result 13 Oct |
|---|---|
| New note, type straight away, click empty canvas | **FAIL 5/5** (runs a, a2, e ×2, f). The note draws the grey placeholder "Double-click to write a note". The text **was** saved: `POST` 201 then `PUT` 200 with the typed body. The placeholder is also drawn behind the text while typing (shot 1). |
| Same, but wait 3 s after placing before typing | **FAIL** (1/1). So it is not just a race with fast typing. |
| Refresh the page | The saved text appears (shot 3). |
| **New:** double-click the placeholder note to check it, then click away without typing | **FAIL 2/2** (runs a, f). The editor opens **empty** (shot 4). Clicking away sends `PUT /api/canvas-notes/{id}` with `{"body":""}` and the server text is **erased**. After a refresh the note is empty for good (shot 5). |
| Positive control: an EXISTING note (same note after a refresh) | PASS. Double-click → the editor holds the text; click away without typing → body unchanged. Replace the text and click away → the new text is drawn at once (shot 2). |
| Contract spot checks | Unchanged. Bad colour 400; body 5001 400; `text_style.align:"diagonal"` 400; width 39 → 201 clamped to 40. |
| Roles / scope | Unchanged. Client Portal `POST /api/canvas-notes` → 422 `permission_denied`. PM on acme "Test site A" (not assigned) gets the HTML page; Super Admin gets 200 JSON on the same call. |
| Probe: "second note does not place" | **Not a bug.** It only happened when my script clicked on an existing note or asset, or clicked "+ Note" twice (the button toggles placement off). In clear canvas space a second note places normally. |

What still holds: bug 1 (now with data loss), the clamping observation, all passes. What changed: severity raised from Medium to **High**: a user who double-clicks the blank-looking note to check it, then clicks away, loses the note text.
Every 13 Oct test note was soft-deleted at the end; only the 12 Oct note `110ab255-…` and the view note remain.
Out-of-scope bugs: none. Bug 1 is inside this ticket (core create-and-type flow); if the owner prefers, it can also be filed as its own Bug for Avani and linked here.

## Proposed Jira comment
```
h3. QA re-check (13 Oct, QA build index-C-Cc1qD1.js) — FAIL (1 bug, can lose the note text)

Tested as Project Manager on my own site *QA-DEMO ZP-4594 SLD notes groups realtime v2.4 (delete me)*, All Nodes view.

*Bug: a new note shows "Double-click to write a note" instead of the text you typed, and if you open it again the text is deleted*
# Open the SLD and unlock editing (lock button, bottom right).
# Click *+ Note*, then click an empty spot on the canvas.
# Type some text, e.g. "test note", then click an empty spot to finish.
# The note shows the grey "Double-click to write a note". (The text was saved; it shows after a page refresh.)
# Now double-click that note to check it: the editor is *empty*.
# Click an empty spot without typing. Refresh the page: the note is now empty for good. The typed text is deleted on the server.

Expected: the note shows the typed text at once, and opening it shows that text (this already works for notes that existed before the page loaded).
Seen 5 of 5 times today for step 4 (also when waiting 3 s before typing) and 2 of 2 times for steps 5–6.

||Check||Result||
|Edit a note that existed before the page loaded|PASS — text shows at once, opening it keeps the text|
|Format menu, move, per-view notes, delete|PASS (12 Oct, unchanged)|
|API: bad colour, body > 5000, bad alignment|PASS — 400|
|API: size 39|accepted and set to 40 (not 400 as the QA review says) — please confirm|
|Client Portal create note|PASS — refused (permission_denied)|
|Project Manager on a site not assigned to them|PASS — refused (Super Admin gets the data on the same call)|

!QA-13Oct-ZP-4594-1-FAIL-new-note-shows-placeholder-after-typing.png|width=720!
!QA-13Oct-ZP-4594-4-FAIL-reopened-new-note-editor-is-empty.png|width=720!
!QA-13Oct-ZP-4594-5-FAIL-typed-text-erased-after-reopen-and-refresh.png|width=720!
!QA-13Oct-ZP-4594-2-PASS-control-edit-existing-note-shows-text.png|width=720!
```

## Recommended status
To Do. The bug is inside this ticket's own scope (place a note and type in it), reproduced 5/5 with a positive control, and it can delete the user's text.
