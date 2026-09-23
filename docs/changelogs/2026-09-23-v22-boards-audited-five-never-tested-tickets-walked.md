# Web v2.2 boards audited against the live release; the five never-tested tickets walked (2026-09-23)

**Prompt:** "we have this artifact did you testing all the ticket that are present in ready to qa for webv2.2
check in case if you miss anything. check this artifact up to date and everything is correct"
(links: the 21 Sep boards FD2esA3TgCg2cenoDEHP26 and S97U6jJcahXeEVx3bg4sXu, and the Jira release report for
version 14156)

Artifacts (republished at the SAME URLs, rebuilt from one 72-row verdict map):
- Web v2.2 QA Readiness — <https://claude.ai/artifact/FD2esA3TgCg2cenoDEHP26>
- Web v2.2 Deep Pass — <https://claude.ai/artifact/S97U6jJcahXeEVx3bg4sXu>

Evidence: `docs/bug-evidence/2026-09-23-v22-audit/` — 24 captures, 3 fetched files, `NOTES.md`.
Bundle: `index-CPjC9Hwo.js` (moved overnight from `index-C9NJAR1x.js`). Release: **72 tickets** (was 69 yesterday).

## The answer to the question

**Coverage.** Ready for QA = 41. Thirty-four were walked in the 22 Sep sweep, two (ZP-4189, ZP-4322) in the
release recheck. **Five had never been tested anywhere** — ZP-3919, ZP-4151, ZP-4181, ZP-4326, ZP-4344 — and two
more (ZP-3919, ZP-4030) had verdicts invalidated by code that merged after the evidence. All seven were tested
today. None is a clean pass.

**The boards.** Neither was safe to show: both described a 40/42-ticket release against 72 live tickets (30
tickets never appeared), anchored to a bundle three rebuilds old, and carried four verdicts overturned on 22 Sep
(ZP-4111 "not deployed", ZP-4212 "open", ZP-4159 "partial", ZP-4066 "other surface" — all READY TO RELEASE now)
plus one false green (ZP-4138 "hidden from the rail"). The morning board also gave a closing section to ZP-4145,
which is not in this release. Both pages are rebuilt; every tile is computed from the same ledger.

## How it was done

A 10-agent Workflow (ultracode) ran the read-only audit: one agent per artifact (every stale count, status,
contradicted verdict, missing ticket), six acceptance-criteria extractors for the untested tickets, one evidence
audit of the 19 READY TO RELEASE tickets (who moved them, on what bundle), and a completeness critic. Its
headline: **none of the 19 green tickets had evidence on today's bundle**, and two (ZP-4208, ZP-4266) had no QA
evidence at all. The browser work was mine: five never-tested tickets, two forced re-runs, eight READY TO RELEASE
re-confirmations, one API probe that settles ZP-4181.

## Verdicts taken today (index-CPjC9Hwo.js)

| Ticket | Verdict | The one line |
|---|---|---|
| ZP-4344 robots.txt | Partial | File live, 12 tokens, dated today. No `Sitemap:` line (ticket's own structure has one) and `/sitemap.xml` is the SPA shell. X-Robots-Tag on tokenised links unverifiable while report history 500s. |
| ZP-4326 same flag | Present (flag-on half) | Nav item `disabled:!flag` and Portal section `requiresFlag` both read `maintenance-portal`; acme has it on, so the OFF state cannot be shown. Route guard has no flag check. |
| ZP-3919 passwordless FE | **Defect** | Method picker follows company config (methods flipped `["password"]` → `["google","email_code","password"]` mid-session). Email typed on step 1 is **not carried** into the password form. Google offered but `google_not_configured` (400). Passkey not enabled; backup codes not implemented. Email-code path reaches the 8-box screen. |
| ZP-4151 multi-account sites | Partial | Multi-select saves; `/profile` and `/v3` return both `account_ids`; tree and Account B page list the site. Account B's **Assets tile reads 0** vs the site's 68 (primary reads 2,722). |
| ZP-4181 bulk-create 500 | **Not fixed for web** | Unknown node id → 500 "An internal error occurred." (trace ff6269b6…); web calls this from Quick Count + Session Detail; retry guard can never match. Only iOS PR #603 merged. |
| ZP-4030 deep links | Partial | `?sld_id=` auto-selects the site ✔. Invalid id → raw `Unexpected token '<'…` ✘. AASA now lists four appIDs incl. `com.ericehlert.SwiftDataTutorial` against "each environment lists only its own app id" ✘. |
| ZP-4208 journal note >200 | **Pass — first evidence** | 314-char note saves (PUT /api/site-walk 200) and renders. Promoted 17 Sep with no comment; now has one (44421). |
| ZP-4038 / 4044 / 4212 / 4059 / 4084 / 4111 / 4113 | Pass — re-confirmed | Re-confirmation comments 44422–44428. |
| ZP-4042 | Blocker, day five | history 500 (ba98b18d…, c73dfb76…), configs 200. |

## Jira changes made

Eight comments, all on tickets already in READY TO RELEASE, all pass evidence: ZP-4208 (44421), ZP-4038 (44422),
ZP-4044 (44423), ZP-4212 (44424), ZP-4059 (44425), ZP-4111 (44426), ZP-4113 (44427), ZP-4084 (44428).
No transitions. No new bugs filed (see "needs a yes").

## Needs the owner's yes

- File: ZP-3919 email-not-carried; ZP-4030/ZP-4159 raw parse-error message; AASA four-appID contradiction; the three
  Admin class grids opening empty; the missing Interpret control; the AWS account id on two success paths.
- Hold comments on Ready-for-QA tickets that did not pass (ZP-4344, 4326, 4151, 4181, 4030).
- Rule on ZP-4266 (moved 01:49 today with no comment, production-only) and ZP-4186 sitting in READY TO RELEASE.
- A Portal Sales seat on QA; go/no-go on ZP-4338.

## Test data

Site A linked to "QA-DEMO ZP3978 Acct B (delete me)" and unlinked again. One 314-char "QA-DEMO ZP-4208 delete me"
note on journal walk ac758601-… (left in place; it triggered a live interpretation). All probes 4xx/5xx, nothing
written. One email code sent to +fm@. Chrome's active site restored to Android Site 2.

## Lesson written to memory

Chrome page zoom ≠ 100 % breaks screenshot-pixel → click-coordinate mapping in claude-in-chrome; JS `.click()` on
the element or `find` → ref clicks are the reliable path. A chrome-devtools window opened in the foreground
occludes the user's Chrome and makes `visibilityState` "hidden", which times out screenshots — open devtools pages
with `background: true`.
