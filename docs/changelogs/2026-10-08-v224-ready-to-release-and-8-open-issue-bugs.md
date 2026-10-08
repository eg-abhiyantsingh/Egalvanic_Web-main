# Web v2.2.4 (14277): hotfix tickets to READY TO RELEASE + 8 bugs filed for the open issues

- **Date:** 2026-10-08, 04:30–05:30 UTC · **Prompt:** "So it is hot fix ticket so right now are moving them ready to release and all the open issue that u found create a bug for all of them"
- **Stage build:** `index-B2ttTLdE.js` (deployed 18:59 UTC 7 Oct). It is BIRL7gG5 plus UI Task #137 (Cable/Busway in
  services, PR #1687). v2.2.4 was merged to prod at 16:41 UTC 7 Oct (PR #1683 "Merge Cicd/stag to PROD V2.2.4").

## Status moves
- ZP-4699 was already READY TO RELEASE (moved by the owner).
- ZP-4697: In QA → READY TO RELEASE (transition 10, QA-Complete).
- All 5 release tickets (4572, 4697, 4699, 4701, 4703) are now READY TO RELEASE.

## Bugs filed
All re-verified on the current stage build first, with a comparison case. Sprint Z-26-09-S3 (1223), fix version Web v2.3, To Do.

| Bug | Priority | Assignee | Issue | From |
|---|---|---|---|---|
| ZP-4723 | High | Avani | Client Portal: SLD page denied, but `/api/sld/{id}` (9 assets, 3 connections) and `/edges` still return the data | ZP-4699 step 5 |
| ZP-4724 | High | Avani | A non-staff Account Manager sets an account to Full Access (Interactive) via `PUT /api/account/{id}`; the UI offers only Lite/No License | ZP-4699 step 12 |
| ZP-4725 | Medium | Avani | Maintenance Portal has no Issues tab; typing /maintenance-portal/issues shows a blank page | ZP-4697 section 6 |
| ZP-4726 | Medium | Avani | No "Domain mismatch" badge and no audit report for non-compliant users | ZP-4697 section 4 |
| ZP-4727 | Medium | Avani | No-domain account still accepts @egalvanic.com guests (other domains refused) | ZP-4697 section 2 |
| ZP-4728 | High | Krunal | Account Manager + Electrical Engineer get Access Denied on Work Orders (they lack `features.site_visits.view`) | ZP-4701 step 3 |
| ZP-4729 | High | Avani | Client Portal sees "No rows" on Work Orders and "No work orders…" on Reports; admin sees 5 open + CLOSEDWO on the same site | found in the portal sweep |
| ZP-4730 | Medium | Avani | Create User shows the raw "API call failed: 400 - { "error": … }" for an outside email | ZP-4697 section 1 |

Screenshots are embedded in each bug as external images from the pushed evidence folder
`docs/bug-evidence/2026-10-08-v224-open-issues/`. The Rovo markdown `![](raw.githubusercontent URL)` became Jira external media.
The DevTools browser had no Jira session, so nothing was uploaded as an attachment.

## Not filed (observation only)
The Create User helper lists the company's allowed domains as @acme.\*, @gmail.com, @icloud.com. The Platform Users chips
count @egalvanic.com as approved. This is noted inside ZP-4730 as a side note, not a separate bug (not verified further).

## Test data added (stage, labelled)
- Guest "QA-DEMO open-issues 8 Oct (delete me)", abhiyant.singh+zp4697nodomain8oct@egalvanic.com, on Default EG-ACME Account.
- Account "QA-DEMO ZP-4699 Lite account (delete me)" (c9227d27…) is Full Access again, set by the Account Manager for ZP-4724.
