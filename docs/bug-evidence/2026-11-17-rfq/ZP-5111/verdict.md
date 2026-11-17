# ZP-5111: Portal / BCES licensing (Client Portal on No-License accounts, BCES tiers, Connections toggle and titles, per-page sync + DevRev)

**Verdict: stays In QA (BLOCKED on the BCES rows only).**
- **Pass on real data, today's build:** the Client Portal user on a No License account uses the portal, and the per-page `features/sync` and `devrev/session-token` calls return 200.
- **Pass only SIMULATED:** BCES-Lite / BCES-Viewer gating and the Connections List/Graph switch. No in-app path on QA offers BCES tiers (details and the ask in ZP-5026/verdict.md).

- **Build:** `index-CkkAeA9o.js`, 17 Nov 2026 14:50–14:58 IST.
- **Seats:**
  - Client Portal test seat (customer, `is_eg_admin` false, role Client Portal, account `license_type` `no_license`).
  - Super Admin, for the staff License preview.
- **Settings changed:** none.

## Checked today
| Row | How | Result |
|---|---|---|
| Client Portal on a No-License account can use the portal | CP sign-in | PASS (real): lands on `/maintenance-portal/site-overview`; Assets lists 487 rows; Connections and Notes show their title + "Not included in your plan" (screenshot 3) |
| Per-page features/sync + DevRev token | CP: full loads of overview, assets, connections, notes | PASS (real): every `POST /api/features/sync` and `POST /api/devrev/session-token` → **200** (4 page loads, 8 calls) |
| BCES tiers offered | `GET /api/features/access` (SA and CP) | `read_only`, `no_license`. Real BCES gating is **BLOCKED** |
| BCES-Lite gating | SIMULATED, staff preview | PASS (sim): BCES-Lite opens Site Overview, Condition, Assets, Connections, Locations, Issues, Attachments, Notes |
| BCES-Viewer gating | SIMULATED | PASS (sim): Issues shows "locked on the BCES-Viewer plan"; the asset has no Engineering tab |
| Connections List / Graph | SIMULATED BCES-Lite, Test site A | PASS (sim): title "Connections"; List shows 1 connection; Graph renders 2 nodes + the edge (screenshots 1, 2) |
| Page titles | Connections / Notes (sim and real lock page) | PASS: titles shown |

Not re-run today: the tenant-scope positive control from 12 Nov (own SLD 200 vs another company's SLD 404 on portal Assets calls).

## Ask
Same as ZP-5026:
- Enable `bces_lite` and `bces_free` in `offered_license_types` for EG-ACME on QA.
- QA then sets one QA-DEMO account to each tier and gives each a Client Portal login, tests Lite and Viewer gating plus the Connections switch for real, and restores both accounts.

## Cross-platform
- **Twins:** none. The Maintenance Portal is web-only.
- **iOS source** (release/qa 34bd67e) has no portal, license-tier or Client Portal role code, so this is not on iOS.
- **Android:** no source access.

### MOBILE-CHECKS
1. **iOS and Android, EG-ACME QA: Client Portal test seat (CP_EMAIL, No License account).**
   - Steps: sign in on the app.
   - Expected: no Maintenance Portal on mobile. Record what this customer-only role gets: blocked with a message, or a read-only site list. Data must stay within the account's sites (Android Site 2).
