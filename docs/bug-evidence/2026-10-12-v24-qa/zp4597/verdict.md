# ZP-4597 — Node groups in the All Nodes SLD view; tenant-gate node-group routes

**Verdict:** PASS
**Build:** QA `index-D1IWZ6SI.js` (redeployed about 18:50 IST; its SLD chunks are byte-identical to `index-BdXGlAq5.js` once chunk hashes are ignored. Only `ReportingConfigEditor` changed).
**Tested:** 12 Oct 2026, 19:05–19:10 IST, acme.qa.egalvanic.ai
**Test data:** site/SLD `QA-DEMO ZP-4594 SLD notes groups realtime v2.4 (delete me)`, `bb593be6-4a12-4505-a2a9-d63ddeb64de9`. It has 5 QA-4594 assets and the named view `QA-DEMO ZP-4594 view (delete me)` (`0db55cd0-fee7-4a24-8111-2c3b4d0f0fa6`). I created all of them. The test group `QA-DEMO ZP-4597 group (delete me)` was deleted at the end of the run.
**Flags:** none. Grouping has no LaunchDarkly gate; the whole editor sits behind `feature-gojs-sld`, which is live ON for Acme.

## Checked

| Check | How | Result |
|---|---|---|
| Create a group in **All Nodes** | PM selects Bus 1 + Cable 1, presses **G**, fills the "Layout Group" dialog and clicks **Create Group** | PASS. `POST /api/node-group` returned 201 with `sld_view_id:null`. The dashed group frame and its title appear (shots 1–2). |
| Add a node to an existing group | Select Panelboard 1, press **G**, choose "Add to existing group", pick the group, click **Add to Group** | PASS. `PUT /api/node-group/assign {view_id:"all"}` returned 200, and Panelboard 1 joined the frame (shot 3). |
| Membership survives a refresh | Reload, reopen the site, read the GoJS model and `GET /api/sld/{id}` | PASS. All 3 members are still grouped, and `nodes.node_group_id` holds the group id on all 3 rows (shot 4). |
| Named view is not affected | `GET /sld/{id}/node-groups?view_id=<named view>` and `GET /sld-view/{view}/graph` | PASS. The named view has no groups, and every view node has `node_group_id:null`, so All-Nodes membership does not leak into the view. |
| Remove a node from the group | Select Panelboard 1, press **Shift+G** | PASS. `PUT assign {node_group_id:null}` returned 200 and the node left the frame. |
| Delete the group | Right-click the frame, choose **Delete**, then "Delete Group Only" | PASS. `DELETE /api/node-group/{id}` returned 200 and the frame disappeared. About 9 s later every node row reads `node_group_id:null`, so no stale membership is left (shots 5–6). |
| View-scope check on assign | Create a group inside the named view (API), then assign an All-Nodes membership to it | PASS. 400 `invalid node_group_id`. |
| Group with a view from nowhere | `POST /node-group` with an unknown `sld_view_id` | PASS. 400 `invalid sld_view_id`. |
| Malformed ids in the request body | assign with `node_id:"bad"` or `node_group_id:"bad"`, plus an unknown group id | PASS. All three return 400 JSON (`node_id and view_id required` / `invalid node_group_id`). None returns 500. |
| Malformed `view_id` in the query | `GET /sld/{id}/node-groups?view_id=not-a-uuid` | PASS. 400 `invalid view_id`. |
| Malformed id in the URL **path** | `DELETE /node-group/not-a-uuid`, `GET /sld/not-a-uuid/node-groups` | Cannot be told apart. The QA CDN answers with the app's HTML page, which it does for 403/404. No JSON 400 came back, and the CDN hides whether the backend sent a 404 or a 500. Dev can confirm in the logs. |
| Site scope (same company) | PM reads `node-groups` for acme site "Test site A" (`b99f9c84-…`), which is not in the PM's 116 accessible sites. Positive control: the Super Admin staff seat makes the same call. | PASS. PM gets the HTML (404) page; staff gets 200 JSON. |
| Other company | PM and staff read `node-groups` for `f86052b7-…` (another company's site) | Refused for both on the acme host. Staff also gets 422 on `/api/sld/{id}`, so a cross-company read is closed to everyone. |
| Roles (write) | `POST /node-group` on my site from every seat | PM and Admin: 201 (Admin's probe group deleted straight away). **Client Portal: 422 `permission_denied`**, because CP has `slds.view` but not `nodes.manage`. AM, EE and FM have `nodes.manage` but my new site is not assigned to them, so they get 404 (site scope). Technician and Portal Sales: "Web Access Restricted". |
| Roles (UI) | Open the editor locked and unlocked | Grouping (G, the Groups button, the context menu) only works with the graph unlocked. CP has no lock toggle at all. |
| Performance | Network log while opening the site | `GET /sld/{id}/node-groups?view_id=all` runs **once** per load at 272–276 ms. No duplicates for the site. Membership assigns go out 4 at a time. |

## Bugs found
None.

## Enhancement suggestions
1. **Make the destructive delete safer.** "Delete Group & All Nodes" sits directly under "Delete Group Only" and removes every asset in the group with one click. Ask the user to type the group name or confirm a second time first.
2. **Show member counts in the Groups panel and API.** `GET /node-groups` returns no member list or count, so the "Groups (1)" panel cannot say how many assets each group holds.
3. **Make the shortcuts discoverable.** G and Shift+G are only hinted inside the right-click menu. Add a tooltip to the Groups button, for example "Select assets and press G".

## Mobile part for Nency
- On the iOS/iPad SLD viewer (after the OTA bundle republish), open site `bb593be6-…` in **All Nodes**. Create a group there on web first. Check that the group frame shows on iOS in All Nodes but **not** inside a named view.
- On iPad, ungroup with remove/delete and check that web sees the same membership after a refresh. Web All-Nodes membership lives on `nodes.node_group_id`.

## Not covered
- A real 500 vs 404 for malformed path ids (hidden by the CDN; needs backend logs).
- Single-head check of the `ngall_a1` migration. That is a deploy step, not visible in the app; the new `node_group_id` field is present in `/api/sld/{id}`.
- Role UI for AM/EE/FM on this exact site, because it is not assigned to them. Their permissions match the PM's (`nodes.manage`, `slds.manage`).

## Re-check 13 Oct on index-C-Cc1qD1.js
Live QA build `index-C-Cc1qD1.js` (deployed 13 Oct 15:21 IST, newer than `index-COeCBn0z.js`). Re-run 13 Oct 15:58–16:04 IST, PM seat, my site `QA-DEMO ZP-4594 SLD notes groups realtime v2.4 (delete me)` (`bb593be6-…`). Live ticket re-read: still In QA, no comments. Script: `.playwright-mcp/v24/sld-13oct/c.body.js`.

| Check | Result 13 Oct |
|---|---|
| Select 3 assets in **All Nodes**, press **G**, "Layout Group" dialog, **Create Group** | PASS. `POST /api/node-group` 201 with `sld_view_id:null`; dashed frame + title drawn (shot 1). |
| Refresh | PASS. Group still there; all 3 assets keep `node_group_id` in `GET /api/sld/{id}` (shot 2). |
| Named view not affected | PASS. `node-groups?view_id=<view>` is empty; every view node has `node_group_id:null`. |
| Right-click frame → Delete → **Delete Group Only** | PASS. `DELETE /api/node-group/{id}` 200; frame gone, the 3 assets stay, and ~9 s later all 3 read `node_group_id:null` (shot 3). |
| Bad input | PASS. Malformed group id on assign → 400 `invalid node_group_id`; `view_id=not-a-uuid` → 400; unknown `sld_view_id` on create → 400. No 500s. |
| Site scope (positive control) | PASS. PM on acme "Test site A" (`b99f9c84-…`, not assigned to PM) gets the HTML page; the Super Admin staff seat gets 200 JSON from the same `node-groups` call. |
| Client Portal write | PASS. `POST /api/node-group` → 422 `permission_denied`. |

What still holds: every 12 Oct claim. What changed: nothing found. Test group `QA-DEMO ZP-4597 13Oct group (delete me)` was deleted at the end (assets kept, ungrouped).
Out-of-scope bugs: none.

## Proposed Jira comment
```
h3. QA re-check (13 Oct, QA build index-C-Cc1qD1.js) — PASS

Tested as Project Manager on my own site *QA-DEMO ZP-4594 SLD notes groups realtime v2.4 (delete me)*.

||Check||Result||
|All Nodes: select 3 assets, press G, Create Group|PASS — group frame shows, saved with no view (sld_view_id null)|
|Refresh the page|PASS — group and its 3 assets are still there|
|Open the named view|PASS — the All Nodes group does not show there|
|Right-click the group > Delete > "Delete Group Only"|PASS — group gone, assets stay and are ungrouped|
|Bad ids (group id, view id)|PASS — clear 400 error, no 500|
|Project Manager on a site not assigned to them|PASS — refused (Super Admin gets the data on the same call)|
|Client Portal tries to create a group|PASS — refused (permission_denied)|

Suggestion (not blocking): ask for a second confirmation on "Delete Group & All Nodes", it deletes every asset in the group in one click.

!QA-13Oct-ZP-4597-1-PASS-group-created-in-all-nodes.png|width=720!
!QA-13Oct-ZP-4597-2-PASS-group-kept-after-refresh.png|width=720!
!QA-13Oct-ZP-4597-3-PASS-group-deleted-assets-kept-and-ungrouped.png|width=720!
```

## Recommended status
READY TO RELEASE
