# Column sorting sorts the current page only — 28 pages swept, ZP-4338 filed (2026-09-22)

**Prompt:** "asset name if you click on arrow sorting is not working create bug assing to krunal and in this
single ticket add where its missing for all pages … test in deepth full and web. and update it dont follow
lazy approach."

Bug filed: <https://egalvanic.atlassian.net/browse/ZP-4338> — Bug · **High** · assignee **Krunal** ·
fixVersion Web v2.2 · sprint Z-26-09-S2 · Backlog → **To Do** · 9 screenshots attached.
Artifact (v4, same URL): <https://claude.ai/artifact/1co6Lj66t8uzZbCNqAaEcb> — new "Column sorting sweep" section.
Evidence: `docs/bug-evidence/2026-09-22-grid-sort-sweep/` — 20 captures + `NOTES.md`.
Bundle `index-C9NJAR1x.js`, site Android Site 2 (sld `aadcee4c-7dd0-45b3-81b9-309c5c166084`).

## Method — three checks per grid, so the verdict is not "the click did nothing"

1. **Truth from the API.** Pull the grid's own list endpoint across every page and compute the true
   alphabetical first and last for the sorted column.
2. **Header clicks.** Click once (A→Z) and twice (Z→A); read row 1 and count non-telemetry API calls per
   click (fetch hooked, DevRev/Sentry excluded).
3. **Page-2 continuity.** With Z→A applied, press next page. Under a correct global sort nothing on page 2
   may sort *after* page 1's last row. If it does, each page was sorted on its own.

Check 3 is decisive without any assumption about default order, which is what the owner's 10 Sep
correction asked for. Check 1 was run where the endpoint was known (Assets, Connections).

## The defect — six grids, one cause

| Page | Rows | Z→A showed | Proof |
|---|---|---|---|
| /assets | 343 | `5N-H1-2` first; **true last is `yu`**. Asset Class Z→A: `Transformer` first while `Utility` exists | truth + 0 calls |
| /connections | 166 | page 1 ends `Fuse19`, page 2 starts `U10` | continuity + 0 calls |
| /issues | 48 | page 1 ends `QA-DEMO SCCR issue…`, page 2 starts `Thermal Anomaly`; `POST /v2/issues/list` body has no sort field | continuity + 0 calls |
| /panel-schedules | 51 | page 1 ends `3N-H1-2`, page 2 starts `A78989`; **sort arrow resets on paging** | continuity + 0 calls |
| /eg-forms | 331 | page 1 ends `26 may abhiyant`, page 2 starts `Clean, Tighten, Torque — Cleaning` | continuity + 0 calls |
| /guest-portal-users | 63 | page 1 ends `Avani Patel`, page 2 starts **`zz ddd`** (the last name on the list) | continuity + 0 calls |

All six paginate on the server and sort in the browser over the loaded page; none sends a sort field.

## The controls — what correct looks like on the same build

* **Server-sorted, correct:** /sessions (147; every click `POST …/workorders/v2` with `sort_by`/`sort_dir`),
  /emps (206), /opportunities (205), /users (227), /tasks (33), /planned-work (9). Page-2 continuity
  consistent on each.
* **Fully loaded, so a local sort is complete:** /site-walks (50), /asset-classes (49), and the single-page
  grids /materials, /labor, /test-equipment, /attachments, /services, /offices.
* **No sort control offered:** /customers and /locations (trees); /reporting/builder (grid, no sortable column).

## Two older findings retired

The 25 Aug report said `Created` was stuck on /sessions and /emps. On this build both sort oldest-first
and newest-first, each click sending `sort_by:"created_at"`. **Do not re-file.**

## Seen on the way, deliberately kept out of ZP-4338

The "opens empty, refresh fills it" regression is wider than Asset Classes: **/connection-classes**
(0–0 of 0 → 1–3 of 3 on refresh) and **/issue-classes** (0–0 of 0 → 1–8 of 8) do the same. All three Admin
class grids. Still unfiled — needs the owner's go-ahead.

## Jira hygiene

Created exactly one ticket. Assignee Krunal (owner's rule: new web bugs go to Krunal or Avani, never Eric).
Priority, sprint, fix version and Backlog→To Do all set, per the create-a-bug checklist. Screenshots
attached through the Jira UI. No other ticket was touched.
