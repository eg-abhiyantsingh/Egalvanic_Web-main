# ZP-5026: Rename the BCES-Free portal license tier to BCES-Viewer (label only)

**Verdict: stays In QA (BLOCKED).** There is no legitimate in-app path on QA to offer BCES tiers, so the real rows still can't run. With SIMULATED tiers the label is right on today's build. The ask for a dev/admin is at the bottom.

- **Build:** `index-CkkAeA9o.js`, 17 Nov 2026 14:46–14:56 IST.
- **Seat:** Super Admin (staff). The License preview, New Customer and Subscription screens are staff/admin only.
- **Settings changed:** none. No company or account setting was touched.

## Is there a legitimate in-app path to BCES tiers on QA? No.
- **What QA offers:** `GET /api/features/access` (SA, EG-ACME) → `offered_license_types: ["read_only","no_license"]`. Demo Company is the same (lead, 17 Nov).
- **Where the UI gets the tier list:**
  - The New Customer and account License pickers read this list through `useOfferedLicenseOptions`.
  - The Maintenance Portal staff License preview reads it from `features/access`.
  - No screen writes it: I checked Admin › Setup, Subscription, Customers and the License preview, plus a bundle search for every `offered_license_types` / `bces_*` use. The only other BCES list is a read-only filter on Guest Portal Users.
- **What the server does with other values:** it refuses them with 400 "That license is not available on this instance" (seen 2 Nov). So the list is a per-instance backend setting, not a company setting an SA can change.
- **Not used:** I did not try any back-door write, and did not use the "Egalvanic - Internal" tools.

## Checked today
| Check | How | Result |
|---|---|---|
| REAL: tiers offered on QA | `GET /api/features/access` (SA) | `read_only`, `no_license` only |
| REAL: portal License preview | Maintenance Portal › License picker | Lite, No License only (screenshot 1) |
| REAL: New Customer › License Type | Admin › Customers › New Customer (cancelled) | Lite, No License only (screenshot 3) |
| SIMULATED: portal License preview | tier list rewritten in this browser only | PASS: BCES-Lite, **BCES-Viewer**, Lite, No License (screenshot 2) |
| SIMULATED: BCES-Viewer preview | pick BCES-Viewer › Issues | "This page is locked on the BCES-Viewer plan". The label is used in the lock text too |
| Bundle text (supporting) | today's build | "BCES-Viewer" maps to `bces_free` in the tier map, the Subscription map and the EN/FR strings; the stored value is still `bces_free`; no "BCES-Free" text is left |
| Existing BCES-Free accounts keep their tier | — | **BLOCKED:** QA has no `bces_free` account and can't make one (see above) |

12 Nov rows (SIMULATED), not repeated today: French UI label and Subscription › T2 card label.

## Ask (for the lead, to Shubham / a dev)
1. **Enable BCES tiers on QA.** In the QA instance's backend config, add `bces_lite` and `bces_free` to `offered_license_types` for EG-ACME (or for a QA test company). This is the setting BCES-IQ uses; today `GET /api/features/access` returns only `read_only`, `no_license`.
2. **What QA will do then, on QA-DEMO data only:**
   - In Admin › Customers › License, set one QA-DEMO account to BCES-Viewer (`bces_free`) and one to BCES-Lite.
   - Attach a Client Portal login to each.
   - Run the real rows.
   - Set both accounts back to their original license.
3. **Alternative:** read-only QA access to a BCES-IQ non-prod instance that already has `bces_free` accounts.

## Cross-platform
- **Twins:** none. JQL "BCES" in the last 60 days finds only web and devops tickets; ZP-4418 is an older iOS BCES engineering back-port, unrelated.
- **iOS source** (release/qa 34bd67e) has no license-tier or portal code:
  - no `license_type`, `bces_lite` / `bces_free`, Client Portal or maintenance-portal strings;
  - "bces" appears only as the BCES-IQ host and the bus hide flags.
  - So the tier label is web-only: not on iOS.
- **Android:** no source access. The Maintenance Portal is web-only (12 Nov ZP-5111: "the portal is web only").

### MOBILE-CHECKS
1. **iOS and Android, EG-ACME QA: Client Portal test seat (CP_EMAIL, account on No License).**
   - Steps: sign in on the app.
   - Expected: the app does not offer a portal or any license-tier text. Record exactly what this customer-only role sees: blocked, or an empty site list. This tells us whether BCES portal users could reach mobile at all.
2. **iOS and Android, any staff seat: no license tier names.**
   - Steps: open a site's account details or Settings.
   - Expected: no tier name such as "BCES-Free" or "BCES-Viewer" appears. If one does, it must read "BCES-Viewer".
