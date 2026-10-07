# ZP-4703 filed — Maintenance Portal should show for every role except Technician

- **Date:** 2026-10-07, ~14:30–14:45 IST
- **Prompt:** "maintance protol should be visible for all roles other than technician. create a bug and assing to krunal"
- **Filed:** [ZP-4703](https://egalvanic.atlassian.net/browse/ZP-4703) "[Web] Maintenance Portal is hidden from Project
  Manager, Account Manager and Electrical Engineer; it should show for every role except Technician"
- **Fields:** Bug · High · assignee Krunal lunagariya · sprint Z-26-09-S3 (1223) · fixVersion Web v2.2.4 · Backlog → To Do
  (matches ZP-4701, filed today for the same developer and release)
- **Screenshots attached (5):** PM menu without the section, PM `/maintenance-portal/overview` Access Denied, AM menu,
  EE menu, FM menu with the section (control). From `docs/bug-evidence/2026-10-07-zp4572-stage/`; stage build
  `index-aLJ0EMXb.js` re-checked unchanged before filing.
- **Artifact:** https://claude.ai/artifact/SM2S3LvuCqLmxyPBVAWCmZ (v3: "Bug filed" table, every ZP key linked)

## Why this is a bug now
Owner's product rule (7 Oct 2026): the Maintenance Portal is for **every role except Technician**. ZP-4572's build
limits it to T2 seats or `maintenance_portal.manage`, which stage's PM/AM/EE seats lack, so all three are refused.

| Role | Today on stage | Should be |
|---|---|---|
| Admin, Super Admin (staff seats) | shown | shown |
| Facility Manager, Client Portal (T2) | shown | shown |
| Project Manager, Account Manager, Electrical Engineer | hidden + Access Denied | **shown** |
| Technician | no web access | hidden |

## Side effect on ZP-4572's open question
Once ZP-4703 is fixed, PM/AM/EE are allowed into the portal, so `/maintenance-portal/condition` and `/sld` opening by
typed address stop being a gap for those roles. ZP-4572 itself was not changed (no comment, no transition).
