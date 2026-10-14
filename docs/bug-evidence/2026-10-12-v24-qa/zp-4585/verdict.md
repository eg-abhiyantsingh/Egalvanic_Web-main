# ZP-4585 — Cross-company guest access (invite guests with scoped roles + sites)

**Verdict:** PARTIAL / BLOCKED — build `index-D1IWZ6SI.js`. Live flag `feature-guest-access` = **OFF (false, v2)** for Acme QA (ticket: "dev-only target EG-ACME, no production targets"; QA is not targeted). The positive end-to-end path (invite→accept→switch→scoped sites→revoke) is **not testable without a real flag flip** — simulating ON only fakes the frontend; the backend `/guest-access/*` routes still deny when the flag is off, so an invite/accept/switch cannot actually complete.

## Checked
| Check | How | Result |
|---|---|---|
| Flag OFF — brand-mark switcher | all seats, `[data-company-switcher-anchor]` | Anchor exists but clicking it navigates Home (no popover) because `hasChoices` is false — only the home workspace. No cross-company switcher. PASS (matches "flag off: no switcher") |
| Flag OFF — Admin > People > **Guests** nav item | Super Admin / Admin panel | **Correctly hidden** (nav gate `de && users.manage`; `de`=guest-access flag). Only "Platform Users" + "Guest Portal Users" show. PASS (shot -1-admin-menu) |
| Flag OFF — `/guest-access/*` API | host: guests/roles/invite/patch/revoke; guest: accept/decline/leave; switch→foreign | all **deny** (masked-404 HTML, or 401 no-auth). `switch`→home company returns 200 (by design, switcher must work from home). PASS |
| **Flag OFF — typed `/guests` route** | Super Admin / Admin | **FAIL (minor): the Guests admin PAGE still renders** (header, "Invite guest" button, empty grid) because the `/guests` route guard is `features.settings.users.view`, NOT the guest-access flag. See B1. Shot -1-typed-guests |
| Flag OFF — `/guests` for non-admin | PM (no users.manage) | **Access Denied** ✓ |
| Invite dialog (flag OFF) | admin opens Invite | Dialog renders ("Invite a guest — Give someone from another company access to selected sites", Email/Role/Sites), but the Role dropdown is empty (`/guest-access/roles` denies) and Send invite would fail. |
| Simulated ON (evalx rewrite) | Super Admin | Guests page renders under Admin; PM still Access Denied. Frontend-only; backend still denies, so no real invite possible. Labelled SIMULATED. |
| Hardening (not flagged) — admin reaching another company's site | `@require_sld_access` family, foreign `sld_id=f86052b7…` from staff + PM | `/api/sld/{F}`, `/sld/v2`, `/sld/v3`, `/graph`, `/edges`, `/opportunity/by-sld` all **422 permission_denied** even for the staff (is_eg_admin) seat. PASS — matches "admin cannot reach any site id regardless of company" |
| Hardening — unscoped `GET /ir_session` removed | staff + PM | `GET /api/ir_session` and `/ir_session/` → masked-404 (no JSON list). Consistent with "deleted the unscoped route". PASS |
| `/auth/v2/me` guest block | PM | `is_guest:false`, `guest:null` present (block exists, empty for a non-guest). PASS |

## Bugs found
**B1 (Low, nav/route gate mismatch — same class as ZP-4138) — with `feature-guest-access` OFF, the typed `/guests` admin page still renders for a `features.settings.users.view` holder.**
- Steps: with the flag OFF (real QA state), sign in as Admin/Super Admin, type `/guests`.
- Actual: the "Guests" admin page renders with an "Invite guest" button and an empty grid. Expected (ticket QA Review): "Flag off … confirm there is no … Admin > People > Guests page." The nav item is correctly hidden and all `/guest-access/*` API routes deny (so no data crosses and an invite can't complete), but the page shell is reachable by typed URL.
- Severity: Low (no data leak — backend denies the list/invite; cosmetic/defense-in-depth gap). Evidence: `QA-12Oct-ZP-4585-1-superadmin-flagOFF-typed-guests.png`. Assignee: Krunal (frontend route guard — put `/guests` behind the guest-access FeatureGate like the nav item). No existing ticket.

No cross-tenant data leak found in the guest surface itself. The `@require_sld_access` hardening (foreign site refused even for staff) is solid.

## Enhancement suggestions
- Guests grid has no filter/search beyond the name search box and no status filter (invited/active/revoked) — add a status filter and a "Resend invite"/"Copy invite" action (the ticket notes resend was removed in favour of in-app toast; a copy-link would still help).

## Mobile part for Nency
Cross-company guest access is a shared backend; iOS sends Bearer tokens and swaps `X-Subdomain` itself (per `/guest-access/switch` doc). Nency should test on iOS/Android: the in-app invite toast, accept → switch to host (full reload), **only the scoped host sites visible**, menus/profile show ONLY the guest role, Customers hidden/Access-Denied for a guest, and sign-out returns to the home host — **but only after the flag is flipped ON for a target company** (can't be tested with the flag off).

## Not covered (why)
The entire positive lifecycle (invite an `abhiyant.singh+zp4585qa@egalvanic.com` alias → accept → switch → confirm scoped sites + guest-role-only menus → Customers denied → revoke) — **BLOCKED**: `feature-guest-access` is OFF on QA/Acme and the backend denies `/guest-access/*` while off, so no invite can be created. I did not invite anyone (would have failed anyway). Needs a real flag flip.

## Flag flip needed
**flag `feature-guest-access`, env QA, company Acme (d59d449b…), set OFF→ON** (ticket says the dev target is EG-ACME) so the invite→accept→switch→scoped-sites→revoke lifecycle, the brand-mark switcher with >1 workspace, and the guest-role isolation can be tested on a real switch. A second QA tenant/company to switch *into* also helps. Ask Shubham in eg-internal-dev.

## Proposed Jira comment
h3. QA — ZP-4585 PARTIAL (flag feature-guest-access OFF on QA/Acme) — build index-D1IWZ6SI.js

Flag-OFF behaviour (the live QA state):
* No cross-company switcher (brand mark just goes Home — only the home workspace).
* Admin > People > *Guests* nav item correctly hidden; all {{/guest-access/*}} API routes deny (host list/invite/revoke, guest accept/decline/leave, switch-to-foreign). {{switch}}→home returns 200 by design.
* Hardening verified: {{@require_sld_access}} refuses another company's {{sld_id}} *even for the staff seat* (422), and the unscoped {{GET /ir_session}} is gone (masked 404).

!QA-12Oct-ZP-4585-1-superadmin-flagOFF-admin-menu.png|width=720!

*Minor gap:* with the flag OFF, typing {{/guests}} still renders the Guests admin page (Invite button, empty grid) for a {{features.settings.users.view}} holder — the route guard isn't behind the guest-access flag, only the nav item is. No data leaks (backend denies), but the page shell shouldn't render. (Same nav-vs-route class as ZP-4138.)

!QA-12Oct-ZP-4585-1-superadmin-flagOFF-typed-guests.png|width=720!

The invite→accept→switch→scoped-sites→revoke lifecycle is BLOCKED until {{feature-guest-access}} is flipped ON for a target company on QA.

## Recommended status
**Stay In QA** — flag-off + hardening pass, but the core feature can't be verified until a real flag flip. File B1 (Low, route guard). After the flip, re-run the full lifecycle (invite only an `abhiyant.singh+zp4585qa@` alias, labelled QA-DEMO).
