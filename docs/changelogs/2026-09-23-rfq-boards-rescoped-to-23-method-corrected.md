# Boards rescoped to the 23 Ready-for-QA tickets; testing method corrected; three passes moved (2026-09-23, night)

**Prompt:** "update the artificat check some ticket are ready to release now. as you are testing them wrong example
deep linking" — and mid-turn: "if ticket are working as expected then move them to ready to release".

Boards republished at the same URLs: Deep Pass <https://claude.ai/artifact/S97U6jJcahXeEVx3bg4sXu> (v7) ·
Readiness <https://claude.ai/artifact/FD2esA3TgCg2cenoDEHP26> (v10).
Evidence: `docs/bug-evidence/2026-09-23-rfq-method-corrected/` — 27 captures + NOTES.md. QA build: `index-vDJBGpU_.js`
(the fourth QA bundle today, after CPjC9Hwo → CH4p1H5S → 6NT43E1C).

## What the owner said was wrong, and what changed in how QA tests

The example was ZP-4030 Deep Linking. Its "Defect" rested on a hand-typed all-zeros work-order id (no user ever
clicks that) and on a developer comment about the association file — not an acceptance line. Four rules now apply to
every walk:

1. **Reach every screen the way a user does** — rail → list → search → click. No typed or fabricated URLs.
2. **Real records, realistic input**; anything created is labelled "QA-DEMO … delete me".
3. **Check the after-state the ticket names** — reload / reopen — not just the moment after the click.
4. **Judge against acceptance criteria + the design the ticket settled on** (its comments, the PRD, the developer's
   scope rule), after reading the whole ticket.

## Release state (read live ~19:40 IST)

75 tickets: **23 Ready for QA** · 40 READY TO RELEASE · 3 In QA · 4 In Progress · 5 To Do. Twelve left Ready for QA
since the morning board — three moved by QA (below), nine by the team (ZP-3675, 3920, 4030, 4150, 4181, 4322 →
READY TO RELEASE; ZP-3919, 4151 → In QA; ZP-4189 → To Do).

## Moved to READY TO RELEASE by QA (authorised mid-turn), each with steps / seen / not exercised

| Ticket | Seen on vDJBGpU_ | Comment |
|---|---|---|
| ZP-4347 | "First service to perform": General is option 1 of 18 | 44453 |
| ZP-4309 | Judged by the IR scope rule (panelboards always; breakers only COM 2/3 with ≥3 poles): CB2 (3P, COM 3) shows its IR box at 0 photos; CB1 has no pole count / condition → correctly no box. **Reverses QA's 22 Sep "reproduces on web".** | 44454 |
| ZP-4062 | Mark As.. lists exactly the asset's 5 services; PUT line-checks by node id → 200; tick survives reload; ring 6 → 7 of 20 | 44455 |

## Verdicts on the 23

| Group | Tickets |
|---|---|
| Open defects (5) | **ZP-4291** 5 services → 2 checkboxes; Checklist service → 0 · **ZP-4292** no services section, no tray, 3 services registered with none confirmed (vs Eric's PRD) · **ZP-4218** re-walked: Convert does not stick, no RFQ reachable · **ZP-4138** tile still shown, Access Denied · **ZP-4042** report history 500 (trace 2f301152…) |
| Owner's call (1) | **ZP-4346** Save service pinned bottom (sticky), always visible with 158 assets — ticket's purpose met, its "at the top" wording not |
| Partial (4) | ZP-4040 (IR view re-seen tonight), ZP-4045, ZP-4060, ZP-4272 |
| Cannot be exercised (6) | ZP-4061, 4082, 4086, 4127, **4153** (re-checked: both journals "Nothing interpreted yet"), 4185 |
| No web surface (7) | ZP-4067, 4128, 4176, 4183, 4190, 4216, 4261 |

## Board changes

- Scope 35 → 23; removed the walkthroughs of the twelve departed tickets; out-of-scope list now 52 by status.
- New "How testing changed today" band; ZP-4030 verdict withdrawn.
- Decisions band rewritten: (1) ZP-4346 move? (2) go-ahead to comment/send back ZP-4291, 4292, 4218 (no comment written on
  failures — only passes were authorised) (3) ZP-4042 backend (4) Portal Sales DB add (Eric).
- Dropped the stale "go-ahead to file" and "hold comments" items (every ticket they named left Ready for QA; the Admin
  class-grid "0–0 of 0" finding no longer reproduces on 6NT43E1C).
- ZP-4272 walkthrough replaced with the 23 Sep partial-fix evidence (the old "nothing persists" text contradicted the ledger).
- Readiness board gained a 4-capture strip (ZP-4291, 4292, 4218, 4346).

## Parked (not on the boards — not tied to a Ready-for-QA ticket)

- A completed journal walk says "Run Interpret to turn its entries into…" with no Interpret control on the page
  (journal 5b85ca51-…; the other journal has a Re-interpret button).
- An AWS account id is returned in two success-path API responses (from the 23 Sep audit).

## Caveats

- On WO 1a9c5d13-… the Forms boxes on "FDR-Test work order" and "Panelboard" were already ticked when first opened
  today; QA's 21 Sep session clicked in that grid, so treat them as possibly QA-made.
- ZP-4042 evidence tonight is request-level only (no seat can open the portal). ZP-4309 was checked on web only.
