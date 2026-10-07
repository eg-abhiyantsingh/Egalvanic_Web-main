# ZP-4697 — domain-scoped user management (Web v2.2.4) — deep test on QA; stage has no build

- **Date:** 2026-10-07, 17:15–18:20 IST
- **Prompts:** "update the ticket test them few ticket are still in qa", "make sure to do sanity testing do deep testing and add
  comment too. test in stage ready to qa or in qa ticket", "i hope you are testing all this in stage"
- **Ticket:** [ZP-4697](https://egalvanic.atlassian.net/browse/ZP-4697) · Story · High · Avani · Ready for QA → **In QA** (Start
  Testing, first action) → **To Do** after the test
- **Builds:** QA `index-DLTfMdFs.js` (PR #1677 → cicd/qa 11:43 UTC, deployed 11:45); stage `index-aLJ0EMXb.js` (no ZP-4697
  code; no promote PR to cicd/stag; stage Guest Portal Users has no Create button / account filter — screenshot 9)
- **Evidence:** `docs/bug-evidence/2026-10-07-zp4697-qa/` (29 screenshots + `results.json`); workflow journal
  `wf_2464dfbb-64a` (3 sweeps + 9 skeptic verdicts, 1.5M tokens)
- **Artifact:** https://claude.ai/artifact/PPDrsvQMhpegdfGkZubbET
- **Jira:** QA comment 44948 posted 12:31 UTC through the Atlassian connector (author shows as the connector's account,
  Shubham Goswami). The browser extension used for inline images was disconnected, so the 9 screenshots are attached
  to the ticket and listed by file name in the comment. Ticket moved In QA → **To Do** (transition id 2) at 12:32 UTC.

## Stage vs QA
The owner wants hotfix tickets on stage. This ticket's code is only on QA (frontend merged to cicd/qa; stage unchanged).
Tested on QA, said so on the ticket, asked for the promote; re-run on stage when it lands.

## Verdict (section by section)
| Section | Result |
|---|---|
| 1 list | FAIL — `POST /api/users/company/{id}/platform-users` is a backend 404 on QA (masked HTML, same as an unknown route, with/without token; real routes give 401) → Platform Users 0–0 of 0 for Super Admin and Admin. Frontend deployed, backend PR #1488 not. |
| 1 create rejection | PASS server-side (`POST /api/users/` 400 "…ending in @acme.*, @xyz.com."); dialog shows raw "API call failed: 400 - {…}" (guest dialog parses errors properly); wording ≠ AC |
| 2 account filter | FAIL — `GET /api/users/guest-portal?account_id=…` ignored (all spellings); search honoured |
| 2 create flow | PASS — account → email helper "Allowed: @egalvanic.*" → sites (required, account's own) → role by license (Lite → CP only) → 201; listed; search works |
| 2 no-domain account | FAIL — not blocked; error "Please use an email ending in ." |
| 2 site scoping | FAIL (High) — FM and CP seats get foreign-site assets via `/api/nodes/sld/{id}`, `/api/lookup/v2/nodes`, `/api/lookup/node-class-counts`, `/api/graph/nodes/{n}/enriched` (200) while `/api/sld/{id}/graph` refuses (422). Reproduced twice, both seats. New guest's `/slds` = 1 (assignment itself works). |
| 3 domain config | not in build (only old one-label `allowed-domains`; company domains at `/api/companies/{id}/allowed-domains` = acme, xyz.com) |
| 4 mismatch badge | not in build (design replaces it with an "N users on unapproved domains not listed" caption, unseen because the route is missing) |
| 5 audit log | not recorded (Admin › Audit Log = offline-mutation log) |
| 6 portal Issues tab | not in build (no route, no nav item) |
| 7 read-only asset | frontend part = ZP-4699 (In Progress, linked) — not counted; server part not checkable (Lite temp password e-mailed) |

Refuted by the skeptics (not reported as defects): noRows text (cosmetic), 422-vs-403 (platform convention), empty-body
create "accepted" (async queue rejects: PERMANENT_FORBIDDEN_TENANT), Lite gating in AssetDetails (ZP-4699).

## Test data created on QA (labelled)
- `abhiyant.singh+zp4697cp@egalvanic.com` 81db7510-4011-7058-9869-7fc6b47bdeef — FM, "test tier", site "test tier site"
- `abhiyant.singh+zp4697lite@egalvanic.com` d1bbe540-2001-709e-076d-abcb37648ad9 — CP, "test" (read_only), site "24 auguest ABS"

## Traps
- Escape after an Autocomplete option is chosen closes the whole MUI dialog → click the dialog title instead.
- `zsh` globbing on `?` in curl URLs (quote them). `/api/users?x` 308 → `/api/users/?x`. `/api/accounts/` = SPA HTML.
- CloudFront masks backend 403/404 as 200 HTML; unknown routes mask even without a token, real routes return 401 → usable
  discriminator for "route missing".

## Stage check (12:42–12:55 UTC, after the owner asked "you need to check in stage?")
- Stage still on `index-aLJ0EMXb.js` (07:12 UTC); no ZP-4697 promote to cicd/stag (last frontend promote #1674 ZP-4572,
  07:09 UTC). QA itself moved to `index-DUAZDfXp.js` at 12:33 UTC.
- **Site-scoping leak reproduced on stage (High)**, API and UI: Facility Manager (+stagefm@) and Client Portal (+cpstage@)
  have 7 sites; "Test uhhujh" (3e3d11df…) is not one. Graph/connections 422, but `/api/nodes/sld/{id}` returns GEN 3,
  UTIL 2, Panel 1 byte-identical to the Super Admin's answer; lookups + enriched 200. UI: the FM opens
  `/maintenance-portal/assets/313339b4-cdd7-49e8-8b94-fcb2da4191ad` (GEN 3) and its Engineering tab. Controls: own site
  "Atest" all 200 / AST-2 opens; Super Admin on the foreign site all 200. Pre-existing backend behaviour, ships to prod.
- Platform Users route missing on stage too; guest-portal `account_id` ignored (6 users / 3 accounts, filter keeps 6);
  company domains acme, gmail.com, icloud.com; portal menu 10 items, no Issues.
- Not run on stage: create flows, sections 4/5/7 (code absent). Nothing created on stage.
- Evidence: `docs/bug-evidence/2026-10-07-zp4697-stage/` (7 png, results.json, api-probe-stage.json).
- Jira: comment 44952 (stage check) posted via the connector (shows as Shubham Goswami). The owner had re-posted the first
  QA comment under Abhiyant Singh's account (44951) and the connector copy 44948 is gone; 44952 left in place.
- Report page: old link PPDrsvQMhpegdfGkZubbET no longer reachable from this account; republished with the stage section
  at https://claude.ai/artifact/XUHmw2KPM4KztorE1sKbCH (private until shared). Wording fix: existing guests' site counts
  are reported neutrally, not as "unscoped".

## Client Portal–only re-test + live demo (13:00–13:40 UTC)
- Owner: "check for cp role only", "we are not doing any changes for fm role", "check in qa first", "show me in browser".
- Owner deleted both connector-posted comments (44948, 44952 — the stage one carried Facility Manager results);
  Abhiyant re-posted the first as 44951 and attached a video. Avani (44953, 08:18 CDT) confirmed the site-scoping
  finding: nodes/sld has no site check, lookups/enriched check company only; fix proposed; ~25 more routes to audit.
- Live demo on QA (Playwright, captions + green/red outlines) as +clientportal@: "Backword 27/8" → "No sites available";
  own "child 1" opens (control); "abhiyant 2" on "Backword 27/8 iOS" opens; in-page panel: graph 422 vs nodes 200 (14)
  + enriched 200. Browser left on the bug view.
- QA moved to `index-DUAZDfXp.js` (12:33, ZP-4699 #1678: CP has no SLD menu / Engineering tab). Stage promoted 13:17
  (PR #1679 "ZP-4697 + ZP-4699: promote to stag") → `index-C9Xyd0R1.js` (13:19); backend too.
- **Correction:** Platform Users now PASS on QA (138 listed, 31 hidden with caption) and stage (35, 20 hidden); account
  filter PASS on both. Earlier "route missing" was a late backend deploy.
- Still failing on both: no-domain account not blocked ("…ending in ."), CP site leak (High), audit log no entry;
  Issues tab not in build. Stage create flow with Client Portal role PASS (guest e14be520-f051-705d-3b41-2fba985d0be0).
- Report v2 (CP only): https://claude.ai/artifact/XUHmw2KPM4KztorE1sKbCH. Jira comment text NOT posted (owner deletes
  connector comments) → scratchpad jira4697/ZP-4697-cp-retest-comment.md.

## Avani's comment 44953 + DM checked (13:45–13:55 UTC)
- Avani (Jira 44953 + DM): the site leak is real but pre-existing, "not part of the changes we made for hotfix, its
  separate bug"; nodes/sld has no check, lookups/node-detail check company only; fix proposed; ~25 more routes to audit.
- Verified with the Client Portal seat: her four routes leak (QA + stage); of her wider list, lookup/tasks, lookup/issues
  and sld/{id}/library-designations leak, program-compliance is guarded, the rest inconclusive.
- **Her "likely another company's sites too" is CONFIRMED on QA**: /api/nodes/sld on "Saveland Avenue Lofts" (company
  fdcadc86) → 375 assets identical to staff; library-designations → 25 rows. Stage cross-company not proven.
- Evidence: docs/bug-evidence/2026-10-07-zp4697-qa/avani-claims-verification.json. Separate bug drafted, NOT filed
  (needs the owner's yes). The Jira connector now signs in as Abhiyant Singh.
