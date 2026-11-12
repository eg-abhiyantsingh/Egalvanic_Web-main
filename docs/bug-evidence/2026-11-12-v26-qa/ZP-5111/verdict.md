# ZP-5111 · Portal / BCES licensing: Client Portal on No-License accounts, BCES tiers, Connections toggle & titles, per-page sync + DevRev (QA, 12 Nov 2026)

**Verdict: PARTLY** on QA, Web v2.6 build `index-C1ZRIunF.js` (checked 16:01, 16:47 and 17:00 IST).

What passes on real data:
- The Client Portal user on a **No License** account uses the portal.
- The per-page `POST /api/features/sync` and `POST /api/devrev/session-token` calls now return **200**. They were 403 `portal_read_only` on 5 Nov, so this is fixed on today's build.

What passes only SIMULATED (BCES-Lite preview):
- The Connections List/Graph switch and the page titles.

What is blocked:
- Real BCES-Lite / BCES-Viewer gating. QA acme offers no BCES tier.

Seats used:
- Client Portal test seat: customer, `is_eg_admin` false, role Client Portal, tier T2, account `license_type` `no_license`.
- Super Admin staff seat, for the portal License preview.

## Checked
| Check | How | Result |
|---|---|---|
| Client Portal on a No-License account can use the portal | CP sign-in (Set up later on MFA) | PASS: lands on `/maintenance-portal/overview` (Site Health, real data: 620 assets, condition index). The menu is Site Overview · Site Health · Assets · Reports (screenshots 1, 2) |
| No-License gating | CP opens Connections / Attachments / Notes | PASS: each shows its title and the lock card "Not included in your plan — talk to your account manager" (screenshot 3). Reports shows "Included with a paid plan" cards plus work-order reports |
| Per-page features/sync + DevRev token | CP: sign-in, then full loads of Connections, Attachments, Notes and Assets (reload); captured every call | **PASS**: every `POST /api/features/sync` and `POST /api/devrev/session-token` = **200** (6 page loads). A direct `POST /api/features/sync` from the CP = 200 (`changed:[]`). 5 Nov: 403 `portal_read_only` |
| BCES tiers offered on QA | `GET /api/features/access` (SA) | `offered_license_types` = `read_only`, `no_license`. No BCES tier on acme, so real BCES gating is **BLOCKED** |
| BCES-Lite / BCES-Viewer gating | SIMULATED: features/access rewritten to offer `bces_lite`, `bces_free`; staff License preview | PASS (SIMULATED): the picker shows BCES-Lite, BCES-Viewer, Lite, No License. BCES-Lite opens Site Overview, Condition, Assets, Connections, Locations, Issues, Attachments, Notes. BCES-Viewer opens Site Overview + Assets only, and Issues shows "locked on the BCES-Viewer plan" |
| Connections List / Graph switch | SIMULATED BCES-Lite, site Test site A | PASS: List renders (1 connection between 2 QA-DEMO assets). Graph renders ("2 assets · 1 connections", 2 nodes + edge). Back to List works (screenshots 4, 5) |
| Page titles on Connections, Attachments, Notes | SIMULATED BCES-Lite | PASS: "Connections", "Attachments" (folders: Coordination Studies, As-Built Drawings, …) and "Notes" (screenshot 6) |
| Tenant scope (CP, customer seat) with a positive control | CP replays the portal Assets calls: `/lookup/v2/nodes/{sld}?…`, `/lookup/nodes/{sld}`, `/lookup/v2/nodes/{sld}/filter-options` with its own SLD (Android Site 2) vs another company's SLD `f86052b7…` | PASS: own = 200 JSON (125 KB / 3.9 MB / 15 KB); foreign = 404 JSON "SLD not found" on all three |
| Flags | `feature-maintenance-portal` = true (live) | ON only. The portal flag OFF was not re-run here (covered by earlier portal tickets). Ask for a real flip if needed |
| Performance | CP Assets page | Note: `GET /api/lookup/nodes/{sld}` returns **3.9 MB** for a 620-asset site on the portal Assets page. Nothing over 2 s was observed for sync / DevRev (0.2–0.7 s) |

## Bugs found
None new in this ticket's scope.

Two small notes (not filed):
- After switching the staff preview to BCES-Lite while on Site Health, the page stays on a now-locked page ("locked on the BCES-Lite plan") instead of moving to the BCES home. This is staff preview only.
- The Notes page shows the title three times (header, back-arrow title, section heading).

JQL run: `project = ZP AND (summary ~ "Collaborators" OR … OR summary ~ "BCES" OR summary ~ "guest access") ORDER BY created DESC`. No open ticket for these.

## Enhancement suggestions
- Portal Assets: page the `/lookup/nodes/{sld}` payload (3.9 MB for 620 assets). Large customer sites will feel slow on the portal.
- Staff License preview: after switching tier, jump to that tier's home page when the current page becomes locked.

## Mobile impact
None. The portal is web only.

## Not covered (and why)
Real BCES-Lite and BCES-Viewer accounts and logins:
- QA acme offers no BCES tier, and setting one is refused (`That license is not available on this instance`, seen 2 Nov).
- This needs either:
  - the BCES-IQ instance, or
  - a QA company that offers `bces_lite` / `bces_free`, with one account on each tier and a Client Portal login on each. That is a backend/DB change; we did not change licences on shared accounts.

## Proposed Jira comment
See `ZP-5111.txt`.

## Recommended status
**Stay In QA.** The No-License portal and the per-page calls pass on real data. The BCES parts pass only SIMULATED. Needed: a QA company (or acme) offering `bces_lite` + `bces_free`, plus one Client Portal seat on a BCES-Lite account and one on a BCES-Viewer account.
