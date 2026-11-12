# ZP-5026 · Rename BCES-Free portal license tier to BCES-Viewer, label only (QA, 12 Nov 2026)

**Verdict: PARTLY** on QA, Web v2.6 build `index-C1ZRIunF.js` (checked 16:01 and 17:04 IST).

"BCES-Viewer" appears everywhere the ticket names, in English and French: the portal License preview, the New Customer license list and Subscription › T2 licenses. No "BCES-Free" text is left in the build.

QA acme offers no BCES tier, so every on-screen check had to be **SIMULATED**. I rewrote `/api/features/access` (`offered_license_types`), and for the Subscription card also `/api/subscription`, in the browser.

The ticket's second point is **BLOCKED** on QA: "existing BCES-Free accounts still resolve to the same tier". QA holds no `bces_free` account.

Seat: Super Admin (staff). Only staff can see the License preview, New Customer and Subscription.

## Checked
| Check | How | Result |
|---|---|---|
| BCES tiers offered on QA | `GET /api/features/access` | `offered_license_types` = `read_only`, `no_license`. No BCES tier on acme |
| Build text (supporting) | grep of all 356 chunks | PASS: `bces_free` → "BCES-Viewer" in the tier map, the Subscription map and the EN + FR i18n strings. **0** occurrences of "BCES-Free" |
| Maintenance Portal License preview | SIMULATED offered tiers | PASS: options BCES-Lite, BCES-Viewer, Lite, No License (screenshot 1). Selecting BCES-Viewer works (screenshot 2) |
| New Customer › License Type (EN) | SIMULATED | PASS: "BCES-Viewer — View-only BCES portal: Assets, with the rest of the BCES-Lite pages shown locked. Client Portal users only." (screenshot 3). Nothing saved |
| New Customer › Type de licence (FR) | SIMULATED, UI language French | PASS: "BCES-Viewer — Portail BCES en consultation : actifs, les autres pages BCES-Lite affichées verrouillées…" (screenshot 4) |
| Subscription › T2 licenses | SIMULATED `/api/subscription` (2 accounts given BCES tiers) | PASS: the summary reads "… 1 BCES-Lite · 74 No License · 2 BCES-Viewer", and the row chip reads "BCES-Viewer — BCES portal with Assets only (BCES-IQ)" (screenshot 5) |
| Negative: no other tier label changed | the same screens | PASS: Full Access, Lite, No License, BCES-Lite unchanged |
| Stored value unchanged | bundle: picker value `bces_free` | PASS (code): the value is still `bces_free`. A real account can't be shown on QA |
| Roles | the License preview, New Customer and Subscription are staff / admin screens | Not re-run for customer seats (label-only change) |
| Flags | no flag on this label | n/a |

## Bugs found
None.

Small wording question (not filed): the BCES-Viewer description says "Assets, with the rest … locked", but the Viewer preview also opens **Site Overview** (tier map `bces_free` = Site Overview + Assets). Either the copy or the gate should change. Owner: Eric / BCES.

JQL run: `project = ZP AND (… OR summary ~ "BCES" …) ORDER BY created DESC`. No ticket for this.

## Enhancement suggestions
None needed for a label rename.

## Mobile impact
Check whether the iOS and Android apps show a portal tier name anywhere (for example, the account license on a site). If they do, they should read "BCES-Viewer" too.

## Not covered (and why)
- Existing BCES-Free accounts keeping their tier and access. QA has no `bces_free` account, and acme refuses BCES tiers (400 "That license is not available on this instance", seen 2 Nov). We did not change licences on shared accounts.
- Needed for a real check, either of:
  - BCES-IQ read-only (the ticket says it is live in prod);
  - a QA company that offers `bces_free`, with one account on that tier.

## Proposed Jira comment
See `ZP-5026.txt`.

## Recommended status
**Stay In QA**, or the owner's call. The rename is fully present in the build and on every screen, but only with SIMULATED tiers. A real BCES-Free account (BCES-IQ, or a QA company offering `bces_free`) is needed for the "same tier, same access" point.
