# Web v2.2 re-check — the release grew to 69 tickets overnight (2026-09-22)

**Prompt:** "check v2.2 again for web."

Evidence: `docs/bug-evidence/2026-09-22-v22-recheck/` — 19 fresh captures + `NOTES.md`
Bundle under test: `index-C9NJAR1x.js` (yesterday's pass ran on `index-BV-phiFE.js`, then `index-BOaMecwk.js`)

## The headline: the release doubled while nobody said so

| | 21 Sep | 22 Sep |
|---|---|---|
| Tickets with fixVersion Web v2.2 | 42 | **69** |

27 tickets were added to the release between 23:56 last night and 05:11 this morning — none of them
tested before today. Nothing was removed. Release date is still 2026-09-25.

The new 27: ZP-3675, 3677, 3919, 3920, 3990, 4030, 4059, 4150, 4151, 4165, 4176, 4181, 4183, 4185,
4186, 4190, 4207, 4216, 4217, 4218, 4261, 4266, 4272, 4280, 4291, 4309, 4326.

Three of them are Sentry auto-files with a stack trace for a description (ZP-4183, 4185, 4186), one is a
branch-merge chore (ZP-4176), one is an Alembic graph fix (ZP-4216), one is an AI-pipeline PR review
(ZP-4190) — none of those is a web surface a tester can click.

## Re-verified on today's build

Every verdict below was produced today, in the browser, on `index-C9NJAR1x.js`.

### Still broken

1. **ZP-4042 — `/api/reporting/history` 500s for the fourth day.** Trace
   `2de0ac923d4f48288f8c36026a9d5ec8`. Positive control `/api/reporting/configs?limit=1` → 200 in the same
   session. Promotion blocker, unchanged across four bundles.
2. **Asset Classes renders "No rows" — regression survives the rebuild.** Admin → Asset Classes on a cold
   load shows "No rows / 0–0 of 0" while `/api/lookup/node-classes` returns 47 rows in the same session.
   *New today:* clicking the grid's refresh icon fires `GET /api/node_classes/user/{userId}` → 200 and the
   grid fills to 1–25 of 49. So the page's initial mount never makes the call. Two bundles, two days, still
   unfiled — it is not on any ticket.
3. **ZP-4189 dashboard LCP — still Poor after three merges.** PR #1500 landed 03:15 today. Measured LCP
   **7,176 ms** (ticket filed at 5,800 ms), element = the greeting heading. TTFB 1,363 ms.
4. **ZP-4322 tooltip over the donut — reproduces.** Hovering the Fuse slice draws "Fuse : 49 assets" inside
   the ring, abutting the "343 assets" centre total.
5. **ZP-3834 raw cloud error — reproduces, and there is a second leak.** The failed job's `error` is still a
   2,235-character ECS descriptor (subnet id, ENI, MAC, private IPv4, ARN, AWS account 165183897698).
   *New:* every job status response — including successful ones — also returns `execution_arn`
   = `arn:aws:states:us-east-2:165183897698:execution:eg-pz-qa-ai-form-fill-sfn-ohio:…`. That field is
   outside the ticket's scope and leaks the same account number on the happy path.
6. **ZP-4309 child-asset IR checkbox — reproduces ON WEB.** The ticket is filed as `[Backend]` from iOS
   manual testing and sits On Hold. In work order "IR WO 22 09", expanding the PNL parent shows children
   *Circuit Breaker 1* and *Circuit Breaker 2* with **no IR checkbox at all**, while sibling *PNL MCB*,
   which already carries IR photos, does show one. The fix must not be scoped to iOS.
7. **ZP-4272 column arrangement — the retention half is proven.** Hid the QR Code column via the column
   menu, navigated away and back: the column is back. No grid state in localStorage, no preferences
   endpoint in the bundle, MUI's `onColumnOrderChange` is wired to nothing. Exactly the customer's
   complaint, and the ticket is sitting in Ready for QA.
8. **ZP-4138 Portal Sales gate — nav half still leaks.** The admin seat (Super Admin + 4 roles, no Portal
   Sales) and the Facility Manager seat both still show the "Maintenance Portal" rail tile; clicking it
   lands on Access Denied. Route gated, menu not.

### Fixed or passing

9. **ZP-4212 defer error — FIXED.** Deferring 11N-H1-1 / Cleaning Services to 01/01/2020 now shows
   "The new due date must be in the future — pick tomorrow or later." No status code, no JSON, no
   "API call failed" prefix.
10. **ZP-4111 device quick-search — now deployed, and it matches the spec.** Yesterday this was "not on
    QA". Today: `sources=skm` → 30 rows all SKM; omitted → 31 rows blended skm+family (unchanged, as
    specified); `sources=bogus` → 400 naming the six valid sources; missing `type` → 400 naming the nine
    valid types.
11. **ZP-4165 customers tree — PASS.** `/customers/tree/v2` takes limit/offset/q and returns
    `{accounts, has_more, limit, offset, q, total}`. Typing "android" in the UI fires the v2 endpoint
    (never the legacy one), narrows 102 → 7, and matches on site names as well as account names with
    auto-expand.
12. **ZP-3990 Reactivate icon — PASS.** On a closed work order, all four menu items carry an icon.
13. **ZP-4059 connection close — PASS.** Opening and closing Connection Details returns to the grid, which
    reloads (1–25 of 166) rather than hiding.
14. **ZP-4030 deep links — backend half present.** Both `.well-known/apple-app-site-association` and
    `.well-known/assetlinks.json` are served as JSON.

### Cannot be settled on acme QA

15. **ZP-3919 / ZP-3920 passwordless sign-in.** The login screen is email + password only. The bundle ships
    the WebAuthn strings and the tickets make methods per-company config, so acme has none enabled.
16. **ZP-4150 "add content directly from the issues".** Not found. The set editor's menu is
    Mark as Draft / Clear all rows / Delete this set; the Issues grid offers View / Edit / Delete; the issue
    detail has no "add to suggestions". Reported as *not located*, not as *not implemented* — the developer
    needs to name the screen.
17. **ZP-3783 option fields.** Both fill-from-photos jobs on the old test data are now `applied` with zero
    gaps, so the review dialog cannot be re-entered. Needs a fresh photo-fill run before the claim can be
    re-tested.

### Corrected from yesterday

18. **ZP-4159 FM direct-URL access — the hole is CLOSED, with a caveat.** Yesterday this carried an
    inherited "wider family still open". Run today with both controls in one Facility Manager session:
    the FM's own work order (`/sessions/92fa5fe0-…`, site A) renders fully, and the foreign work order
    (`/sessions/1a9c5d13-…`, site "17 July 2026", not among the FM's 11 sites) renders **no data at all** —
    just the line `Unexpected token '<', "<!DOCTYPE "... is not valid JSON` and a Back button. No asset,
    issue or customer data reaches an unassigned FM. The remaining defect is cosmetic: the refusal shows a
    developer error string instead of the Access Denied card the app already has.
19. **ZP-4315 completion mismatch — the web number moved again.** Yesterday the ring read 7 of 15 (47%);
    today the same work order reads **6 of 20 (30%)** with the asset count unchanged at 15. The web
    denominator is unstable on its own, independent of the iOS comparison in the ticket.

## Method note

An early attempt to settle ZP-4159 with `fetch` calls to `/api/ir_session/{id}` returned 200 text/html —
the known SPA-fallback trap for that route family. Those probes prove nothing about authorization and were
discarded in favour of navigating the browser, with a positive control first.

## Deliverables

* Artifact page: <https://claude.ai/artifact/8YkPAdmBz3ogiyCBbi7wbs> — "Web v2.2 Verification Board",
  19 records with numbered steps, plain-words actual/expected, a real screenshot each, and a
  *For the developer* line at the end of every record. A local copy sits at
  `docs/report-artifacts/2026-09-22-web-v2.2-verification-board.html`, which is a git-ignored folder —
  the artifact URL is the shareable copy.
* Evidence: `docs/bug-evidence/2026-09-22-v22-recheck/` — 19 captures + `NOTES.md`.

## Coverage ledger (adds to 69, nothing counted twice)

| Group | Count |
|---|---|
| Verdict today — open | 8 |
| Verdict today — passing | 6 |
| Verdict today — unsettled | 5 |
| Open, fix in progress (ZP-4292) | 1 |
| Walked 21 Sep, area unchanged | 19 |
| Blocked on data or environment | 12 |
| Added overnight, untested | 18 |

## Test data used

* Multi-service work order — <https://acme.qa.egalvanic.ai/sessions/1a9c5d13-530d-4b7d-beb7-6a52ac84ebfc>
* Infrared work order (ZP-4309) — <https://acme.qa.egalvanic.ai/sessions/bbe66d36-6537-418a-95a9-69ce25befccb>
* Closed work order (ZP-3990) — <https://acme.qa.egalvanic.ai/sessions/ae730a37-f8c9-472a-b9d8-8352269eb72d>
* Facility Manager's own work order (ZP-4159 control) — <https://acme.qa.egalvanic.ai/sessions/92fa5fe0-7cc7-420d-b2c6-81d650636e2a>
* Asset Classes — <https://acme.qa.egalvanic.ai/asset-classes> · Dashboard — <https://acme.qa.egalvanic.ai/dashboard>
* Maintenance Program — <https://acme.qa.egalvanic.ai/maintenance/program> · Customers — <https://acme.qa.egalvanic.ai/customers>
* Connections — <https://acme.qa.egalvanic.ai/connections> · Issue Suggestions — <https://acme.qa.egalvanic.ai/issue-suggestions>

Failed photo-fill job (ZP-3834): `bb8c21c8-752c-4e52-8415-a5a75a978b92`.

## Nothing was mutated

The only write attempted was the deferral in ZP-4212, deliberately set to a past date so the server
refuses it. No schedule, asset, work order or configuration was changed on the shared tenant.
