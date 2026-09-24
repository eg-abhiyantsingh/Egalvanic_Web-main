# Ready-for-QA re-test: 31 tickets, three moved, the customer portal finally walked (2026-09-24)

**Prompt:** "check again test all ready to qa ticket and update artificate too" (Jira release 14156).

Boards republished at the same URLs: Deep Pass <https://claude.ai/artifact/S97U6jJcahXeEVx3bg4sXu> (v8) ·
Readiness <https://claude.ai/artifact/FD2esA3TgCg2cenoDEHP26> (v11). Evidence
`docs/bug-evidence/2026-09-24-rfq-retest/` — 28 captures + NOTES.md. QA build `index-CC4S9HsJ.js`.

## Release state
81 tickets read live 16:30 IST: 31 Ready for QA (8 new overnight: ZP-4280, 4315, 4338, 4351, 4352, 4353, 4360, 4363),
40 RTR, 3 In QA, 1 In Progress, 5 To Do, 1 Backlog. After this pass: **28 Ready for QA, 43 RTR**.

## Seats — what changed and how it was handled
- Chrome was signed in as **"guest user" (Client Portal role)**. That seat can open the customer Maintenance Portal —
  the first time ZP-4061/4042 could be walked as a customer. Used read-only.
- The **+admin seat now holds Portal Sales** (Eric's add) but lost Electrical Engineer / Project Manager / Account
  Manager. Staff Compliance and Maintenance Program pages answer Access Denied for it; the compliance checks ran
  through the portal on Interactive tier instead. Asked the owner to sign the browser back in as +admin (done).
- The devtools browser was signed out → used for the sign-in tickets. No password ever typed.
- **Google sign-in is now configured on acme** (was `google_not_configured` on 23 Sep).

## Moved to READY TO RELEASE (comments 44488–44490)
| Ticket | Proof on CC4S9HsJ |
|---|---|
| ZP-4338 | All six grids sort on the server: Assets Z→A row 1 = `yu`, Guest Portal Users row 1 = `zz ddd`, page 2 continues page 1, Panel Schedules keeps the arrow on paging, Forms page 1/2 = server list slices; sort params on every click/page change |
| ZP-4353 | "Use my password" is now the primary button, email kept after the failed device sign-in, email carried into the password form |
| ZP-4280 | Ring tooltip/aria "Marked complete: 0 of 6 (0%)", change-type dialog "Mark complete / Uncheck"; 0 "check off" strings in all 290 code chunks |

## Verdicts on the 28
- **Open defects (6):** ZP-4292 (new "Set Up Forms" sheet appears, but untick-all + Not Now still leaves 3 services —
  ticket scenario fails; Avani-sheet vs Eric-PRD conflict), ZP-4291, ZP-4218 (unchanged), **ZP-4061** (customer seat:
  WO list 422 company_data.view → "No rows", Create button offered to a view-only role), **ZP-4042** (history 500 from
  the customer seat too; Free also opens Assets), ZP-4138 (positive half fine now that +admin has Portal Sales; negative
  half not re-walked).
- **Owner's call (1):** ZP-4346.
- **Partial (8):** ZP-4045 (totals reconcile 852=852, future date refused by picker AND server; shutdown-rule pre-ack
  not testable — 0 "Never" assets), ZP-4060 (legend present; acknowledged segment unreachable — **new defect: ack →
  400 "1 key(s) do not match a current deviation"** because the ack POST omits pm_standard_id), ZP-4040, ZP-4272,
  ZP-4315 (web ring now 3 of 10; iOS not compared), ZP-4351 (server message differs from ticket text: "isn't
  available" vs "not recommended"; web never shows it), ZP-4352 (prompt=login present; needs a human Google account),
  ZP-4360 (0.3–0.8 s from India; server-side thresholds unreadable).
- **Cannot be exercised (5):** ZP-4153 (all 7 journal walks "Nothing interpreted yet"), 4082, 4086, 4127, 4185.
- **No web surface (8):** ZP-4363 (support request about a named customer account) + 4067, 4128, 4176, 4183, 4190, 4216, 4261.

## New defects awaiting the owner's go-ahead to file
1. Compliance → Acknowledge fails 400 with a raw toast when the standard comes from the picker (deviations computed
   with `pm_standard_id`, ack POST sends `dev_keys` only).
2. Customer portal Work Orders: list refused (company_data.view) for the Client Portal role; Create Work Order offered.

## Observations outside the RFQ scope (not filed)
- +admin (Super Admin + Admin) gets Access Denied on Site Data → Compliance / Maintenance Program while the menu offers
  them — a nav/route gate mismatch of the ZP-4138 kind, caused by today's role change.
- Portal Condition Assessment on Addtioanl Site: "Assets assessed 0 (0% of 10)" beside "Condition Distribution: 10
  assessed" — the two tiles disagree on what "assessed" means.

## Method notes
- Chrome throttles timers in a hidden tab: long scripted waits stall or time out. Ran the six-grid sort loop in the
  background (`window.__res`) and polled; kept each JS call under ~40 s.
- Grid "last row" readings on virtualised MUI grids are unreliable — read `data-rowindex` rows while scrolling, or
  compare with the server's own page slices (done for Forms).
- The server's name sort ignores spaces (ICU collation): "t new asset" sorts after "Test asset" in Z→A. Use
  `Intl.Collator('en',{ignorePunctuation:true})` when checking continuity.
