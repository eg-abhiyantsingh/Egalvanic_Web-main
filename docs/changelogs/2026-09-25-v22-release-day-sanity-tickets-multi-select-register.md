# Web v2.2 release day — sanity sweep, ticket re-tests, multi-select checks, Defect Register update (2026-09-25)

**Prompts (in order):** "test our ticket in depth … do full depth website testing … if you find any major bug update our
artifact (QA Defect Register)"; "testing everything in web v2.2 for all the roles and also update the ticket status too …
if ticket pass then ready to qa to ready to release … sanity testing"; "for multiple selection … you can see history like
that [Issue → Status History] … if you select multiple … it can create lots of things"; "save me memory to not ask question,
figure by yourself".

## Builds
QA shipped two builds during the morning: `index-CC4S9HsJ.js` → **`index-C2Lqd_s1.js`** → **`index-S8OG5dN0.js`**.

## Sanity sweep (staff +admin seat, every rail link clicked)
| Build | Pages | Failed requests | Script errors | Flags |
|---|---|---|---|---|
| CC4S9HsJ | 51 | 0 | 0 | Access Denied on Site Data → Maintenance Program / Compliance / Reports (ZP-4390) |
| C2Lqd_s1 | 52 | 0 | 0 | same |
| S8OG5dN0 | 52 | 0 | 0 | same — until the seat's roles changed (see below) |
Detail pages (first row of Assets, Issues, Work Orders, Quotes, Site Walks, Services, PM Plans, Panel Schedules, Tasks; every
tab clicked): 0 failed requests, 0 script errors.

## Ticket status changes (comment + screenshot each)
| Ticket | Verdict | Comment |
|---|---|---|
| ZP-4218 | **PASS → READY TO RELEASE** — Convert labor to subcontracted → Sub = Yes + Generate RFQ, survives reload; RFQ dialog + templates | 44531 |
| ZP-4391 | **PASS → READY TO RELEASE** — full-width Back under Sign In (Retour in FR), returns to options with email kept (C2Lqd_s1 + S8OG5dN0) | 44534 |
| ZP-4390 | Stays RFQ — Access-Denied half no longer reproducible with +admin (roles restored); portal Free/Read-Only still shows padlocked entries instead of hiding them (owner: "instead of lock just hide") | 44537 |
| ZP-4272 / 4315 / 4040 / 4045 / 4042 | Re-checked on the new builds: unchanged (WO grid forgets hidden column; ring 3 of 10; type UI ✔ / NULL case invisible; ack 400 + withdraw 500; history 500) | — |

## Multi-select checks (owner's Status History screenshot)
- Issues → **Add to Work Order** (tick 2) → **Create new**: one WO per real click. A scripted same-instant double submit created
  two WOs; a **real mouse double-click created one** → not reported as a bug.
- The same issues can sit on several open WOs (3 here); each issue page names only the latest "Session", and **Status History
  records none of it** (still just "Open (initial status)"). Model is many-to-many (unlink restores the previous one) → owner's call.
- Issues → **Add to Quote**: 409 "No corrective service is configured for this account" / "Every issue needs an accepted
  resolution before it can be quoted" — shown as raw JSON toasts.
- Issue **Mark Resolved** opens a "Resolve Issue?" dialog requiring a resolution description (not submitted).

## Defect Register (https://claude.ai/artifact/LCpCWgsgYTaYzVQ4XpfdjB, v21) — new "Web v2.2 release day" section
1. High — Compliance Acknowledge always fails (400 raw toast) — re-run 25 Sep with a real click.
2. High — Withdraw automatic Shutdown-rule ack fails silently (500, traces f418ae51… / f378700c…).
3. High — Customer portal Work Orders empty for Client Portal role + Create offered (24 Sep; customer seat not signed in today).
4. Medium — Portal Reports has no history; /api/reporting/history 500 (traces 6e5b1420…, 83029af9…).
Plus "also seen, not filed" (issues on several WOs; same-instant double submit; compliance empty-state wording) and "fixed
today" (ZP-4218, ZP-4391, ZP-4338). Tally 12/22/16 → 15/23/17; sorting row marked fixed.

## Boards
Deep Pass v11 / Readiness v14: 16 Ready-for-QA (incl. ZP-4390), release-day sweep block, moves evidence for 4218/4391, decisions
list refreshed. Others moved ZP-4291/4292 (QA-failed), 4351/4352/4207 to RTR and ZP-4060/4138/4153/4176 on 24 Sep.

## Environment notes
- The +admin seat's roles changed back to 6 (EE/PM/AM restored) mid-morning; the QA session was later signed out (not by QA).
- Roles other than +admin: not walked — no other seat signed in and QA never types passwords. The portal LICENSE preview
  (Free/Read-Only/Interactive) was walked as the customer-tier check.
- Hidden-window freezing: Chrome froze the background QA window (timers + fetch stall). Fixes: close the devtools-MCP Chrome
  that covered it, create a fresh claude-in-chrome window, fetch-based sleeps, background jobs + polling.
- S8OG5dN0 wraps the input value setter ("Illegal invocation" on scripted value sets) → use `document.execCommand('insertText')`.

Evidence: `docs/bug-evidence/2026-09-25-v22-release-day-sanity/` (10 captures + NOTES.md).
