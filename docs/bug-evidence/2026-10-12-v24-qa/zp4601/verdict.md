# ZP-4601 — Task #87: SLD canvas notes show who wrote them (author byline, web + API)

**Verdict:** PASS (web byline + API readable name). The email / "Unknown" fallback could not be exercised on QA (see Not covered).
**Build:** QA `index-D1IWZ6SI.js` (SLD chunks identical to `index-BdXGlAq5.js` with hashes ignored).
**Tested:** 12 Oct 2026, 18:50–19:17 IST, acme.qa.egalvanic.ai, Project Manager seat (`first/last = Abhiyant Singh`), plus Admin and Super Admin.
**Test data:** notes on my site `QA-DEMO ZP-4594 SLD notes groups realtime v2.4 (delete me)` (`bb593be6-…`).
**Flags:** none.

## Checked

| Check | How | Result |
|---|---|---|
| Byline on a new web note comes from the create response | + Note, then read the canvas immediately | PASS. `POST /api/canvas-notes` 201 returns `author_name:"Abhiyant Singh"`, and the byline shows on the new note before any refresh (shot 1). The byline is right; the placeholder text on that note is ZP-4594 bug 1. |
| Full name, not the Cognito username | `GET /sld/{id}/canvas-notes`, `GET /api/sld/{id}` (`comments[]`), `GET /api/sld/v2/{id}` (`comments[]`) | PASS. All three return `author_name:"Abhiyant Singh"`, while `author` holds the Cognito sub (`113b6590-…`). The two `/sld/v2` comment builders use the same name. |
| Placement and style | Look at the note | PASS. Small grey italic text at the bottom-right. Per the later fe#1646 (listed on ZP-4618) it now sits **inside** the note, not under it as this ticket's text says (shots 1, 3). |
| A long byline must not resize or shift the note | Narrow a note to 60 px (`PUT width:60`) and reload | PASS. The byline is cut to "Abhiyant…" (text width 47 ≤ max 48) and the note stays 60 px wide (shot 2). Width restored to 200 afterwards. |
| Notes written by another seat | Admin seat creates a note on my site | PASS. Its `author_name` resolves the same way. The Admin seat is also named "Abhiyant Singh", so this does not prove a different name. |
| Performance | Network | The byline needs no extra call; the name arrives in the existing `GET canvas-notes` / `POST` responses. |

## Bugs found
None for this ticket.

## Observations
- When the API returns `"Unknown"`, the web hides the byline entirely (code: `author_name !== "Unknown"`). That is reasonable, but the ticket's QA row says the byline shows "Unknown". Dev to confirm the intent.

## Enhancement suggestions
1. Show **"edited by <name>, <date>"** as a tooltip on the byline. `modified_by` is already stored, and today only the creator is shown even after someone else edits the note.
2. Clicking the byline could open the author's contact card or filter notes by author. Useful once an SLD has many notes.

## Mobile part for Nency
- **ZP-4602** ([iOS] pass `author_name` to the SLD viewer byline, Ready for QA). On iPad, open site `bb593be6-…`; the web note must show "Abhiyant Singh" in its bottom-right corner, inside the note.
- Create a note on iPad and check that web shows the iPad user's full name.

## Not covered
- **Email fallback** for a user with no first/last name, and the **"Unknown"** case. Every QA seat I can use has a first and last name, and renaming a seat would change user data, which is not allowed. Dev could add a backend unit test, or the owner could approve a temporary nameless QA user.

## Re-check 13 Oct on index-C-Cc1qD1.js
Live QA build `index-C-Cc1qD1.js` (deployed 13 Oct 15:21 IST, newer than `index-COeCBn0z.js`). Re-run 13 Oct 15:57–16:05 IST, PM seat (`first/last = Abhiyant Singh`), my site `bb593be6-…`. Live ticket re-read: still In QA, no comments. Scripts: `.playwright-mcp/v24/sld-13oct/a.body.js`, `a2.body.js`.

| Check | Result 13 Oct |
|---|---|
| Byline on a brand-new note, before any refresh | PASS. `POST /api/canvas-notes` 201 returns `author_name:"Abhiyant Singh"`; the canvas byline reads "Abhiyant Singh" at once (shot 1). The note body in that shot shows the placeholder: that is ZP-4594's bug, not this ticket's. |
| Byline after refresh | PASS. Grey italic "Abhiyant Singh", bottom-right, inside the note, next to the saved text (shot 2). |
| Full name in all three payloads | PASS. `GET /sld/{id}/canvas-notes`, `GET /api/sld/{id}` `comments[]` and `GET /api/sld/v2/{id}` `comments[]` all return `author_name:"Abhiyant Singh"`; `author` stays the Cognito sub (`113b6590…`). |
| No extra request for the byline | PASS. The name rides on the existing create/list responses. |

What still holds: every 12 Oct claim. What changed: nothing. Still not testable on QA: the email / "Unknown" fallback (every seat has a first and last name).
Out-of-scope bugs: none (the placeholder seen in shot 1 is ZP-4594 bug 1, reported there).

## Proposed Jira comment
```
h3. QA re-check (13 Oct, QA build index-C-Cc1qD1.js) — PASS

Tested as Project Manager on my site *QA-DEMO ZP-4594 SLD notes groups realtime v2.4 (delete me)*.

||Check||Result||
|New note: byline shows at once, before any refresh|PASS — "Abhiyant Singh" (name comes from the create response)|
|After refresh: grey italic byline, bottom-right, inside the note|PASS|
|Name is the full name, not the login id|PASS — canvas-notes, /api/sld/{id} comments and /api/sld/v2/{id} comments all say "Abhiyant Singh"|

Note: in the first picture the note text shows "Double-click to write a note". That is the separate ZP-4594 bug (new note does not draw its text until refresh), not this ticket.
Not tested: the email / "Unknown" fallback (every QA seat has a first and last name). When the API says "Unknown" the web hides the byline; please confirm that is intended.

!QA-13Oct-ZP-4601-1-PASS-byline-on-new-note-before-refresh.png|width=720!
!QA-13Oct-ZP-4601-2-PASS-byline-full-name-after-refresh.png|width=720!
```

## Recommended status
READY TO RELEASE. The fallback path is not testable on QA; the owner may prefer to keep it In QA until a nameless test user exists.
