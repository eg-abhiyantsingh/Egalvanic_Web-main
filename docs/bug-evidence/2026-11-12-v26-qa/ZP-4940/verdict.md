# ZP-4940 — Task #195: SLD copy/paste keeps groups, edge routing and per-view layout

**Verdict:** PASS — QA, Web v2.6, build `index-C1ZRIunF.js`, 12 Nov 2026 16:50–17:00 IST, Super Admin seat (+ Facility Manager for the
no-access negative). On 2 and 5 Nov the paste dropped routing/hidden flags, kept clones in the source group, sent no dx/dy and accepted a
foreign-SLD node (201); all of that is fixed today.

**Test data (own):** site "QA-DEMO v2.6 SLD editing 12 Nov" (`c4438cd3-…`): group "QA-DEMO ZP-4942 grp 12 Nov" = CB-2 + PNL-2 (fixed edge,
elbow [[324,899]]), FUSE nosub → Load-1 hidden (`{"hidden":true}`), view "QA-DEMO ZP-4947 view A 12 Nov".
Site A (`7c08bce2-…`, Magic Layout site) used as "another SLD" for the negatives.

## Checked
| Check | How | Result |
|---|---|---|
| Paste sends the delta | select CB-2, PNL-2, FUSE nosub, Load-1 › Copy › click canvas; captured `POST /api/node/copy-set` | PASS — body keys sld_id, view_id, **dx −260.8, dy 68.2**, 4 nodes, 2 edges → 201 |
| Routing kept (moved by dx/dy) | API read-back | PASS — clone CB-2 (copy 2) → PNL-2 (copy) `{"elbows":[[63.2,967.2]]}` = source [[324,899]] + (dx,dy) |
| Hidden flag kept | API + canvas | PASS — clone FUSE nosub (copy) → Load-1 (copy) `{"hidden":true}`, drawn as pills |
| Fully-selected group → "(copy)" | `GET /api/sld/{id}/node-groups` | PASS — new group "QA-DEMO ZP-4942 grp 12 Nov (copy)" holding the two clones; clones are not in the source group |
| Partially-selected group not carried | copy only CB-2 › paste | PASS — "QA26 CB-2 (copy 3)" in no group |
| Paste inside a view keeps per-view layout | view A: Rotate Right on FUSE nosub (`PUT /sld-view/{id}/positions` rotation 90), copy FUSE + Load-1, paste | PASS — request carries view_id; clone "QA26 FUSE nosub (copy)" is in view A with rotation 90, 150×80 like the source view row |
| Tenant gate: source node from another SLD | API (own sites) | PASS — 400 "source nodes must belong to the target SLD" |
| Tenant gate: view from another SLD | API | PASS — 400 "view does not belong to this SLD" |
| User without access to the target SLD | Facility Manager → copy-set into my site | PASS — 404 "SLD not found" |
| Positive control | same call, own node, own site | 201 (clone "QA-DEMO ZP-4940 positive control 12 Nov" created on my site) |
| Console | runs | no app errors |

## Bugs found
None.

## Enhancement suggestions
- Clone names stack up as "(copy)", "(copy 2)", "(copy 3)"; offering a rename-on-paste or a {name}-style suffix would keep labels tidy.
- Pasting places the selection's top-left at the click point; a ghost preview of the selection while in "Click to Paste" mode would help aim.

## Mobile impact
Native iOS parity is ZP-4939 (iOS copyNodes bridge now sends dx/dy) — iOS tester should paste a group with an elbowed/hidden edge on iOS.

## Not covered
- Paste into a location inside another group ("clone lands in the pasted location's group") — pasted onto empty canvas only.
- Terminal/label placement manips (only rotation/size checked).
- Old offline payloads without dx/dy (server-derived delta) — not reproducible from the web UI.

## Proposed Jira comment
See `ZP-4940.txt`.

## Recommended status
READY TO RELEASE.
