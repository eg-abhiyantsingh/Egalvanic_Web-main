# ZP-4943 — #202 locked-group members stay clickable + #203 Assemblies "Edit with AI" for name templates

**Verdict:** PASS (with one Low side bug) — QA, Web v2.6, build `index-C1ZRIunF.js`, 12 Nov 2026 16:39–16:49 IST, Super Admin seat
(+ role probes from the ZP-4942 run). `feature-sld-assemblies` is ON for Acme (live); #203 sits behind it.

**Test data (own):** site "QA-DEMO v2.6 SLD editing 12 Nov" (`c4438cd3-…`), layout group "QA-DEMO ZP-4942 grp 12 Nov" = CB-2 + PNL-2 with a
fixed edge (one elbow); assembly "QA4942-FDR-{name}" (`cd981de3-…`).

## Checked
| Check | How | Result |
|---|---|---|
| Group is locked | right-click inside the group box | Group menu: Unlock · Delete (the group made with Add to Group was already locked) |
| Group rect on Background layer | GoJS `layerName` | PASS — "Background" (5 Nov: default layer) |
| Click a member inside the locked group | click PNL-2 | PASS — selects `QA26 PNL-2` itself (5 Nov: selected the group) |
| Right-click a connection inside the locked group | right-click CB-2 → PNL-2 | PASS — connection menu Select Entire Run · Edit · Hide Connection · Insert Cable · Delete (5 Nov: group menu) |
| Drag a member moves the whole group | drag CB-2 | PASS — CB-2 and PNL-2 both moved (+140,−42) and the elbow moved with them (`PUT edge/update` elbows [[184,946]] → [[324,899]]) |
| Negative: elbow inside a locked group does not drag alone | drag the elbow point | PASS — no request sent, elbow and members unchanged |
| Hidden-edge badges / pseudo-edge markers inside a locked group | — | not covered (no hidden edge inside the group) |
| #203 Edit with AI in Save as Assembly | select CB-2 + PNL-2 › Add to… › Assembly… › Edit with AI "Name the breaker {name}-CB-1 and the panel {name}-PNL-1" | PASS — `POST /api/assemblies/ai-templates` 200 in 1.3–1.7 s; templates → "{name}-CB-1", "{name}-PNL-1"; message "AI updated 2 template(s). Review them, then save." |
| Nothing saved until Save | Cancel, then `GET /api/assemblies` | PASS — modified_at unchanged |
| #203 on the assembly detail page | /assemblies/{id} › Edit with AI "Number every asset {name}-1 … top to bottom" | PASS — 4 templates filled; reload shows the original templates (not persisted) |
| Negative: instruction > 2000 chars | API, 2001 chars | PASS — 400 "instruction must be at most 2000 characters" (UI input has maxLength 2000) |
| Negative: unsafe instruction | API | PASS — 422 "The AI model declined this request (general_harms). Try rephrasing it…" |
| Negative: no nodes | API | 400 "nodes is required" |
| Positive control | API, 2 nodes, "Name the fuse {name}-F1" | 200 `{"templates":{"n0":"{name}-F1"}}` (only known refs returned) |
| Permission assemblies.manage | AM / EE / FM / CP call ai-templates | PASS — 422 permission_denied "Required: assemblies.manage"; Admin/PM allowed (400 "instruction is required" on an empty body) |
| Flag OFF | SIMULATED (see ZP-4942) | Save as Assembly / + Assembly are gone, so Edit with AI is unreachable |
| Console | runs | no app errors |

## Bugs found
**Bug — dragging a locked layout group sends `PUT /api/node/update/{group id}` → 400** — Low (no visible effect)
1. Lock a layout group (here "QA-DEMO ZP-4942 grp 12 Nov").
2. Drag one of its members.
- Actual: besides the two member updates (200) and the edge update (200), the app sends `PUT /api/node/update/57cfdf63-2af8-4067-a5e5-d06ee7e988b1`
  (the GROUP id) with `{"x":…,"y":…}` → **400**. Re-proved twice (16:39 and 16:41 IST, same seat, same group). The members and elbow are saved correctly.
- Expected: no node update for the group rect (or a node-group endpoint).
- Suggested assignee: Krunal (frontend) — Eric owns the ticket.
- JQL run: `project = ZP AND created >= -60d AND (text ~ "layout group" OR text ~ "locked group" OR summary ~ "Assemblies page" OR text ~ "Manage assemblies")` → only the feature tickets themselves (ZP-4943/4942/4940/4947/5107/5102/5112); no bug covers it.

## Enhancement suggestions
- Edit with AI: show a one-line diff (old → new) before applying, so a user can see what the AI changed.
- Newly created layout groups start locked; a short hint ("Group is locked — drag moves the whole group") would explain it.

## Mobile impact
#202 is in src/sld (OTA): check on iOS/Android that tapping a member of a locked group selects the member and that dragging moves the group.

## Not covered
- Hidden-edge badges / pseudo-edge markers inside a locked group; terminal-handle and floating-label drag negatives.
- > 2000-node selection bound.
- Real LaunchDarkly OFF for `feature-sld-assemblies` (simulated only).

## Flags needed
Same as ZP-4942: `feature-sld-assemblies`, QA, Acme, OFF → check → ON.

## Proposed Jira comment
See `ZP-4943.txt`.

## Recommended status
READY TO RELEASE once the real `feature-sld-assemblies` OFF/ON check (shared with ZP-4942) is done; file the 400 as a separate Low bug.
