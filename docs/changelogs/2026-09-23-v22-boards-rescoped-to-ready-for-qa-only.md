# Web v2.2 boards re-scoped to the 41 Ready-for-QA tickets (2026-09-23, afternoon)

**Prompt:** "its okay if ticket already ready to release ignore that ticket and update my artifact too. right
now our focus is to test all ready to qa ticket only. you can ignore to do or in progress and ready to
release ticket."

Artifacts republished at the SAME URLs:
- Web v2.2 QA Readiness — <https://claude.ai/artifact/FD2esA3TgCg2cenoDEHP26>
- Web v2.2 Deep Pass — <https://claude.ai/artifact/S97U6jJcahXeEVx3bg4sXu>

## What changed

- **Scope.** Both pages now cover exactly the **41 tickets in Ready for QA** (fixVersion 14156 re-read live at
  13:45 IST: 74 tickets — 41 RFQ, 19 READY TO RELEASE, 8 To Do, 3 In Progress, 1 In QA, 1 On Hold,
  1 Resolved). The other 33 appear only in an "Out of scope" list by key, grouped by status, untested.
  Two To Do tickets (ZP-4346, ZP-4347) joined the release since the morning read; ZP-4315 moved To Do →
  In Progress. The Ready-for-QA set itself did not change.
- **Tiles** are computed from the same 41-row ledger: 12 passing-held · 7 partial/not located (6 + 1) ·
  8 open defects · 6 blocked on data or seat · 8 no web surface. Coverage strip = 41 cells.
- **Dropped from the boards** (out of scope by instruction): the eight READY TO RELEASE re-confirmations,
  the "promoted without QA evidence" band (ZP-4208 / ZP-4266), ZP-4338 and ZP-4292 blocks, the "what the
  old boards got wrong" bullets about RTR tickets. Their evidence stays in the 23 Sep NOTES.md, the
  morning changelog and Jira comments 44421–44428.
- **Kept / added for Ready-for-QA tickets:** today's walkthroughs for ZP-4344, 4326, 3919, 4151, 4181,
  4030; today's-bundle evidence re-labelled to the RFQ tickets it proves — ZP-4043 · ZP-4041 (Condition
  Assessment capture), ZP-4068 (Journal Review + live interpretation starting after the note save), ZP-4138
  (Maintenance Portal tile in the rail for a staff seat); 21–22 Sep blocks for ZP-4218, 4272, 4322/4189,
  3675, 4150, 4167, 4109/4110/4112, 4171/4086, 4039/4149/4062, 4045/4060, and the blocked / no-surface
  groups, with ten 22 Sep captures uploaded as `img22/`.
- **Decisions band** is RFQ-only: ZP-4042 blocker; a Portal Sales seat (ZP-4061 + portal halves of
  4042/4043); ZP-4150 needs the developer to name the screen; go-ahead to file the defects found on RFQ
  tickets that have no ticket of their own; hold comments on the five RFQ tickets that did not pass today.

## Verification before publishing

A 6-agent Workflow (three lenses × two pages: numbers-and-scope, claims-vs-evidence, owner presentation
rules) refuted the generated HTML against `live-v22.json` and the NOTES.md files. Findings and fixes are
listed below.

**Round 1 (6 agents, ~9 min): 17 must-fix, 44 should-fix.** All accepted except the doctype/viewport
item (the publish skeleton adds them). The material ones:

- **ZP-3919 was double-counted.** It had been tested on 22 Sep (email + password only) and re-run today because
  PR #1513 merged at 01:45 — so the never-tested set is **four** (ZP-4151, 4181, 4326, 4344) plus **two re-runs**
  (ZP-3919, ZP-4030); 34 + 3 + 4 = 41. Prose said "five" / "seven"; fixed everywhere.
- "12 of them never appeared" listed 16 keys → **16**.
- A Python quoting slip rendered the literal **`True`** in ZP-4030's "What happens" box (also on the published
  72-ticket deep pass) → real sentence.
- 25 Sep 2026 is a **Friday**; "before Thursday" → "before the 25 Sep release".
- **ZP-4030 relabelled Defect** (two acceptance lines fail — same rule applied to ZP-3919); **ZP-4045 / ZP-4060
  → Partial** (bars render with zeros); **ZP-4082 → "Not run"** (ticket says prod-only; not attempted);
  **ZP-4326 pill "Present" → "Partial · flag-on half"** with the bundle read moved to a developer line.
  Tiles now: 10 working-held · 7 partial (incl. 1 not located) · 9 open · 7 cannot be exercised · 7 no surface.
- Hold-comment list is **six** (adds ZP-3919). "were not re-tested" → "are not reported on this page".
- Bundle chain includes `index-DDSq5pRr.js` (14 Sep); ZP-4062's evidence is 21 Sep (BOaMecwk); ZP-4039 / 4112 /
  4167 bases split by build; Visualizer "no site has an applied program" contradiction removed.
- Every ledger row now links to its walkthrough anchor (`#zp-####`, 41 anchors); test-data records are clickable
  (plan `/plans/b567d2a6…`, WO `/sessions/1a9c5d13…`, IR WO `/sessions/bbe66d36…`, walk `/site-walks/ac758601…`,
  accounts). ZP-4181's header is plain words with an explicit "API proof only" pill.
- New captures taken for the blocks that lacked one: robots.txt head + tail (no `Sitemap:` line), `/sitemap.xml`
  rendering the app shell, the report-history error as the browser receives it, the portal Access Denied —
  all at ~14:00 IST on **`index-CH4p1H5S.js`** (QA rebuilt again this afternoon; both pages say so).
  ZP-4189 uses the 14 Sep capture filed with the ticket, captioned as such (copied server-side from the old board).

**Round 2 (2 agents):** ROUND2_PLACEHOLDER

## No Jira changes in this step

Nothing transitioned, no comments added. The five hold comments and the new bugs still need the owner's yes.

## Memory

`feedback_release_boards_ready_for_qa_only.md` — the scoping rule, so future boards start from
`status = "Ready for QA"` and never spend effort on READY TO RELEASE tickets unless asked.
