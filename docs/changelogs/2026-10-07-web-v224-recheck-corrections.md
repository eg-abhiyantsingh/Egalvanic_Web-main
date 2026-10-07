# Web v2.2.4 (14277) — "are you sure testing is correct?" re-check on stage

- **Date:** 2026-10-07, 14:15–14:40 UTC · **Prompt:** "are you sure testing is correct right?"
- **Stage build:** `index-C9Xyd0R1.js` (13:19 UTC)

## Re-verified (unchanged verdicts)
- **ZP-4572 / ZP-4703** (READY TO RELEASE) on the NEW build: Maintenance Portal in the rail and the route opens for Admin,
  Super Admin, PM, AM, EE, FM, Client Portal; Technician gets "Web Access Restricted". Evidence `docs/bug-evidence/2026-10-07-v224-stage-recheck/`.
- **ZP-4701** (READY TO RELEASE, moved by the owner 08:28 CDT): Create Work Order shown for Admin, Super Admin, PM and the
  reporter's seat (dharmesh+acme+prod); AM/EE page denied (as before); FM no button (T2 by design); CP denied.
  Tier-switch steps 2–9 still unverified by QA (role edits not possible from this session).
- **ZP-4699 step 12** stands: no UI flow offers Full Access; PUT license_type=interactive → 200.

## Corrections made
1. **ZP-4699 step 5:** `/api/connections/v2/sld/{id}` returning 200 is CORRECT — the Client Portal's own read-only
   Connections list and Graph use it (network recorded on Site Health, Condition, Assets, asset page, Connections, Graph).
   The failure is only `/api/sld/{id}/graph`, `/api/sld/{id}`, `/api/sld/{id}/edges` (no CP screen calls them).
   Comment 44958 edited; page 9qMh3c9Sp3eh7MhLden37i v3 with a re-shot screenshot 1.
2. **ZP-4697 audit:** NOT "not recorded". Guest creations at 14:26 and 14:29 UTC are in **Admin › Activity Logs**
   (`/api/activity-logs`, request lines: method/path/status/actor/time, ~1 min ingestion lag, page size 100, only 88
   writes held for 24 h on stage). Admin › Audit Log is the Mutation Audit Log and never shows user changes.
   Comment 44959 edited; page XUHmw2KPM4KztorE1sKbCH v4.

## Test data added (stage, labelled)
Client Portal guests abhiyant.singh+zp4697audit@ (API) and +zp4697auditui@ (UI), "test account", site "test site".
