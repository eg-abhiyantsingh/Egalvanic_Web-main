# Web v2.3 — sanity round on the new QA build (6 Oct 2026)

**Prompt:** "https://egalvanic.atlassian.net/projects/ZP/versions/14236/tab/release-report-all-issues test all ready to qa ticket sanity testing"

**Where:** acme.qa.egalvanic.ai, web build `index-Dyz0HxpC.js` (deployed overnight; 5 Oct was `index-CdsUTPRH.js`, then `index-C913tyjW.js`).
**Page (same link, now version 6):** https://claude.ai/artifact/3e3VwLmEk5fDa7pQEyb24y
**Evidence:** `docs/bug-evidence/2026-10-06-v23-qa-r3/zp<number>/` (`results.txt` + screenshots), posted comment texts in `jira-comments/`.

## Scope
Jira version 14236 had **9 tickets in Ready for QA** at 13:20 IST: the 7 held on 5 Oct plus ZP-4421 (back from To Do with backend PR #1476) and
ZP-4590 (back from In Progress, no comment).
**ZP-4435** ([Web] Easier photo access from SLD) was moved to Ready for QA by Avani at 13:38 IST, after this list was taken; it is handled
as an addendum at the end of this file. Each was re-run on the new build the way a user would use it; every ticket got a Jira comment with
screenshots (the owner's standing rule), posted through the same render-checked path as on 5 Oct.

## Results
| Ticket | 6 Oct result | Jira |
|---|---|---|
| ZP-4421 | **PASS** — search now matches the "Applies to" text ("automatic" keeps only the Automatic Transfer Switch forms; "Any subtype" forms hidden; "transfer", "ATS", "bolt" behave) | **READY TO RELEASE** (moved) |
| ZP-4590 | **FAIL on QA** — Similar 95/90 °F → Delta T 5 °F → "Nominal"; the class expression on QA acme is still the old one, no "Minor" in any Thermal Anomaly class | Ready for QA, environment question asked |
| ZP-4148 | **FAIL as written, rule changed** — the new build drops the "Portal Sales" role-name gate; the portal follows features/access, so the Super Admin seat without Portal Sales now sees the whole portal (hidden on 5 Oct); PM unchanged (menu hidden, /condition still opens) | Ready for QA, duplicate of ZP-4138 per Dharmesh |
| ZP-4301 | PARTLY TESTED — server still names the view (re-proven with Fuse 2 in a new QA-DEMO view, single + bulk delete 400); web still sends allow_collapsed | Ready for QA |
| ZP-4305 | PARTLY TESTED — /pull-through-work still a blank page with the old heading, no route in the build | Ready for QA |
| ZP-4372 | PASS web part — 10 pages, 242 API calls, no 5xx; slowest 3.1 s (assets lookup, cold) | Ready for QA (DB checks with Dharmesh) |
| ZP-4394 | PARTLY TESTED — sections still done (section_states), Forms tab lists the Torque Record | Ready for QA |
| ZP-4423 | NOT TESTABLE from the web (pipeline fix) | Ready for QA |
| ZP-4529 | NOT CONFIRMED — simulated blank 200 reply still shows "HTTP 200"; no limit text in the new editor chunk | Ready for QA |

Also seen: ZP-4428 went READY TO RELEASE → Backlog at 21:19 IST on 5 Oct, moved by the Sentry integration (production error 2PP fired again;
the fix ships with ZP-4181). ZP-4665/4666 are new in the version (Backlog). ZP-4398 is still To Do.

## How it was done (fast path)
- One signed-in admin session (`QaDriverDaemon`, visible Chrome) ran scripted batches: Add Procedure dialog + real key presses for the
  four search terms; Create Issue with Thermal Anomaly (MUI selects open on mouse-down, not click); page sweep reading
  `performance` entries for 5xx; forms API; AI-edit reply simulation in the page.
- `V23RoleProbe` (own Chrome) re-ran the PM and Super Admin seats for ZP-4148 — that is what exposed the gate change; the old and new
  main bundles were diffed for the "Portal Sales" string to explain it (code, not data).
- ZP-4301 used an existing asset in a fresh view instead of creating a node: a hand-built `/node/create` body got a 500 (my payload, not a
  user flow; noted, not filed).
- Two independent reviewers checked every draft against the notes and the screenshot pixels before posting.

## Jira changes
- ZP-4421 → READY TO RELEASE (transition 7, status only).
- One QA comment per ticket with the screenshots attached (ids 44882–44891 in `docs/bug-evidence/2026-10-06-v23-qa-r3/jira-comments/POSTED.md`).
- **One wart:** ZP-4421's comment went out twice (44882 and 44883, identical, 1 ms apart). The first attempt uploaded the screenshots
  through Jira's own hidden file input, which froze the page renderer after the comment request had already left; the retry posted it
  again before the duplicate guard could see the first. Left in place for the owner to delete (the assistant does not delete comments).
  Lesson: never touch Jira's native `input[type=file]` from the extension; inject an own `<input type=file multiple>`, upload with
  `POST /rest/api/3/issue/<key>/attachments`, and run the duplicate guard before `POST …/comment`.
- Nothing else changed in Jira.

## Pending from earlier prompts
- ZP-3927 / ZP-3928 (iOS 1.57 / 1.56 automation): v1.69 of the QA app is in Downloads; the suite from 2 Oct needs a run on it. Not started
  in this session beyond reading the state.
- git push of the 5 commits (blocked for the assistant; owner runs `git push origin main`).

## Addendum — ZP-4435 (arrived in Ready for QA at 13:38 IST)
Testing in progress at the time of this commit; the result, its Jira comment and the page update (v7) follow in the next commit.
