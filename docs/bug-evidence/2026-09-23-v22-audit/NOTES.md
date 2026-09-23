# 2026-09-23 — Web v2.2 audit of the two 21 Sep boards + first-ever tests of the never-tested tickets

Bundle today: `index-CPjC9Hwo.js` (chain: DDSq5pRr 14 Sep → BV-phiFE 21 Sep am → BOaMecwk 21 Sep pm → C9NJAR1x 22 Sep → CPjC9Hwo 23 Sep).
Release: fixVersion 14156 = **72 tickets** at 12:33 IST (41 Ready for QA, 19 READY TO RELEASE, 7 To Do, 2 In Progress, 1 In QA, 1 On Hold, 1 Resolved (migrated)).
Seat: `+admin@` (Super Admin) in the owner's Chrome; logged-out checks in an isolated chrome-devtools context.
Site: Android Site 2 (`aadcee4c-7dd0-45b3-81b9-309c5c166084`, 352 assets today).

## Coverage answer
Ready for QA today = 41. In the 22 Sep 48-ticket checklist = 34. NOT in it = 7: ZP-3919, ZP-4151, ZP-4181, ZP-4189, ZP-4322, ZP-4326, ZP-4344.
ZP-4189 / ZP-4322 had verdicts from the v2.2 recheck (open defects). The other FIVE had never been tested anywhere → tested today (below).
Also re-tested because their code landed AFTER the 22 Sep evidence: ZP-3919 (PR #1513 merged 2026-09-23 01:45), ZP-4030 (AASA changed 607 → 850 B).

## Never-tested tickets — verdicts today

### ZP-4344 Robots txt changes recommendations — PARTIAL (file live, two AC lines not met/unverifiable)
- GET /robots.txt → 200 text/plain 3287 B, body = real directives, "Verified: 2026-09-23", source URL per vendor. Saved as zp4344-robots.txt.
- Tokens present: GPTBot, OAI-SearchBot, ChatGPT-User, OAI-AdsBot, ClaudeBot, Claude-User, Claude-SearchBot, Google-Extended, Google-CloudVertexBot, PerplexityBot, Perplexity-User, DuckAssistBot; tail `User-agent: * / Disallow:`.
- Bing: no token listed, with a documented rationale in the file (Microsoft publishes no robots.txt token). Judgement call, not a defect.
- GAP 1: the ticket's own "structure is:" block ends with `Sitemap: https://<host>/sitemap.xml`. The published file has NO Sitemap: line, and /sitemap.xml returns the SPA shell (200 text/html 3031 B) — no sitemap exists.
- GAP 2 (unverifiable): "X-Robots-Tag: noindex, nofollow, noarchive on tokenised report and share-link responses". No tokenised link can be minted: report links come from GET /reporting/history/{id}/download → report_url, and /api/reporting/history still 500s (ZP-4042). Probes of fake paths only hit the SPA fallback (meta robots noindex present there). Not proven either way.
- S3 upload / invalidation / "no Terraform" lines: not visible from a browser.

### ZP-4326 Maintenance Program should be under the same flag — PRESENT (positive half only)
- /api/auth/me company_features contains `maintenance-portal` (74 flags; no bare `maintenance`).
- Bundle: nav item Maintenance Program = `disabled: !ye`, tooltip `featureGating.maintenanceProgramDisabled`; rail section Maintenance Portal = `requiresFlag: ye`; both read `ye = x_e("maintenance-portal")`. Same flag, as the ticket asks.
- UI: with the flag on, both "Maintenance Program" (Site Data rail) and "Maintenance Portal" (icon rail) render → 01-zp4038-dashboard-today-bundle-CPjC9Hwo.jpg. /maintenance/program opens (280 of 352 assets). /maintenance-portal/reports → Access Denied (Portal Sales role gate, unchanged).
- Flag-OFF half cannot be produced on acme (no seat/tenant without the flag). Route guard for /maintenance/program still has no flag check (direct URL would open with the flag off) — nav-only gating.

### ZP-3919 Passwordless Sign-in — Frontend — DEFECT + partial
- Fresh isolated context /login on CPjC9Hwo: Email Address + "Continue with Google" + "Email me a code" (disabled until an email is typed) + "Use my password".
- /api/auth/v4/methods?subdomain=acme was `["password"]` at 12:38 and `["google","email_code","password"]` at 12:55 — acme's sign-in methods were switched on mid-session. The screen followed the config both times (disabled methods hidden) — PASS.
- Email me a code (+fm address): "Two-step verification — Enter the code we emailed you. a***@e***", 8 code boxes, Verify disabled, Start over → 05-zp3919-email-code-screen.jpg. Masked address, POST /auth/v4/email-code/start 200. Code not entered (no mailbox reader).
- DEFECT: type email → "Use my password" → password form's Email Address is EMPTY (input name=email value=""); reproduced with real keystrokes (chrome-devtools fill + click) → 09-zp3919-password-form-email-not-carried.jpg. "Back to faster options" also returns with the email cleared.
- Google: "Continue with Google" → interstitial "Taking you to Google to sign in…" (live region) → GET /api/auth/v4/google/authorize-url?subdomain=acme&consent=false → 400 `google_not_configured` → "We couldn't sign you in with Google. Try another way." Error state works; the method is offered while the OAuth client is not configured for acme.
- Passkey: not in methods → not offered → not testable. Backup codes: only string in the build is "There are no backup codes…" (AC item not implemented — per audit agent).
- The 22 Sep "email + password only" verdict is void: PR #1513 merged 01:45 on 23 Sep.

### ZP-4151 same site can link with multiple accounts — PASS on linkage, GAP on the account roll-up
- Admin → Customers → Abhiyant Singh (11 sites) → site "A" (68 assets) → ⋮ → Edit Site: field is "Accounts *", MUI Autocomplete multiple=true, chip "Abhiyant Singh" → 11-zp4151-edit-site-accounts-is-multiselect.jpg.
- Typed "QA-DEMO ZP3978 Acct B", picked the option → two chips → 12; Save Changes → PUT /api/sld/update/d0087e9f-1071-4df0-b23e-bfed1d28d67f body `account_id: ebec8929…, account_ids: [ebec8929…, a4499f71…]` → 200 echoing both → 13.
- Persistence: GET /api/sld/{id}/profile AND GET /api/sld/v3/{id} both return account_ids [ebec8929…, a4499f71…] (the contract avani.patel promised in the ticket comment).
- Customers tree search "QA-DEMO ZP3978 Acct B" → group "QA-DEMO ZP3978 Acct B (delete me) — 1 site" containing A (68 assets) → 16; Abhiyant Singh still 11 sites.
- /accounts/a4499f71-… → Sites (1) tab, Sites card "A (albany, NY)" → 14. BUT its Assets tile reads **0**, while the primary account /accounts/ebec8929-… reads Assets 2,722 across 11 sites → 15. The second acceptance line ("Respective Assets / issues / Task History to display accordingly") is NOT met for the account roll-up. "Task History" has no web surface.
- Test data: site A is the owner's own test site; link to Acct B (delete me) — to be reverted at the end of the run.

### ZP-4181 500 on POST /api/mapping/node-session/bulk-create — NOT FIXED for web
- Web calls it from QuickCountPage-ON9sYwbL.js and SessionDetail-C51qaZSE.js with `{session_id, node_ids}`; the SessionDetail retry guard matches "fk_node"/"ForeignKeyViolation" in the message.
- Today from +admin: session 1a9c5d13-… + node_ids [0000…0000] → **500** `{"error":"An internal error occurred.","trace_id":"ff6269b60ce043fe8dab52e4489dadab"}`; bogus session → 500 (trace 579122abb68e4b248603298599f97446); empty node_ids → 400 "node_ids is required and must not be empty".
- So the server still returns a sanitised 500 for a non-existent node id — the retry guard can never match, and no 202 `_mutation` envelope exists. The only merged fix is iOS (PR #603).

## Re-tests forced by later merges
### ZP-4030 Deep Linking — AASA contradicts its own contract; invalid-id message is a raw parse error
- /.well-known/apple-app-site-association → 200 JSON 850 B, appIDs: zplatform-Dev, -QA, -Stag AND `B7W4Z5Q6U8.com.ericehlert.SwiftDataTutorial` (a personal tutorial bundle id) — against the ticket comment "each environment lists only its own app id". assetlinks.json 2306 B lists dev/qa/staging AND prod `com.egalvanic.pz`. Saved copies in this folder.
- /sessions/00000000-0000-0000-0000-000000000000 → page body `Unexpected token '<', "<!DOCTYPE "... is not valid JSON` + "Back to Work Orders" → 17. FAIL vs "Invalid, expired, or inaccessible links show clear user-facing messages" (same string as the ZP-4159 refusal).
- /sessions/1a9c5d13-… (site "17 July 2026") without sld_id → renders the WO with site chip "17 July 2026" but localStorage.activeSiteId stays Android Site 2 → 18. With `?sld_id=652a9ba9-1fbb-499d-af06-2cefc634ac01` → see below.

## READY TO RELEASE re-confirmations on CPjC9Hwo (none had today's-bundle evidence)
- ZP-4038 Dashboard single-site, Open Issues by Type 87 (Repair Needed 10 … Replacement Needed 1) → 01.
- ZP-4044 Maintenance Program "Search assets…" typed "transformer" → "Showing 2 of 3 assets" (397 Transformer TF-Add, main switch 416) → 03.
- ZP-4212 Defer → 11N-H1-1 / service / 01/01/2026 → toast "The new due date must be in the future — pick tomorrow or later." from POST /api/asset-maintenance/defer 400 "a deferral must move the due date into the future" → 06. Nothing written.
- ZP-4208 (first-ever evidence; promoted 17 Sep with none): Journal Review /site-walks/ac758601-6cb0-49a7-8992-b1ff4806d167 → Add note → 314-character note → Save → PUT /api/site-walk/{id} 200, entry renders as NOTE "typed", "3 entries" → 10. Walk then flipped to "Read 0 of 1 · 1 to go / Interpreting / LIVE 0/1".
- ZP-4113 Reports catalog has the Issue Report card; Condition Assessment header has "Export issue report" + "Export assessment report" → 19, 20.
- ZP-4084 Condition Distribution: Condition 1 297 / 2 11 / 3 1 / Non-Serviceable 6 (one vocabulary) → 20. ZP-4043 Equipment Health band includes "Assessment expired". ZP-4041 "8 assets had their condition revised automatically in the last 30 days".
- ZP-4111 /api/equipment-catalog/quick-search?q=FA&type=circuit_breaker: sources=skm 200, family 200, omitted 200, bogus → 400 "sources must be from bus_model, cable, family, panel_type, skm, transformer".
- ZP-4059 /connections → row → Connection Details in place (Fuse2 → P34, Cable) → 21; back control → see below.
- ZP-4042 still the blocker: GET /api/reporting/history → 500 internal_error (traces ba98b18d52884761975c94ea4a0f622e, c73dfb7629e14895a9bc74d8b99c8e3f), /api/reporting/configs → 200 in the same session. Day five.
- ZP-4059 continued: header chevron collapses the Source/Target/Type/ID cards while Connection Details stays → 22; back arrow → grid returns 1–25 of 169 with the Overall Readiness card, no blank screen → 23. PASS on CPjC9Hwo.
- ZP-4030 continued: /sessions/1a9c5d13-…?sld_id=652a9ba9-1fbb-499d-af06-2cefc634ac01 → localStorage.activeSiteId became 652a9ba9… ("17 July 2026") and the WO rendered → 24. Site auto-select PASS with the parameter; without it the active site does not change. Logged-out redirect preservation not run (would need a password sign-in).

## Test-data writes today (all labelled, all reverted or harmless)
- Site "A" (d0087e9f-…): linked QA-DEMO ZP3978 Acct B (delete me), then removed again at the end of the run (PUT /api/sld/update → account_ids back to [ebec8929…]).
- Journal walk ac758601-… "14 augest abhiyant": one 314-character note "QA-DEMO ZP-4208 delete me…" left in place (triggered a live interpretation run).
- Maintenance Program Defer: POST returned 400, nothing written. bulk-create probes: 500/400, nothing written. Email OTP: one code mailed to +fm@.
- Chrome active site restored to Android Site 2 at the end.

## 14:00 IST — bundle moved again: `index-CH4p1H5S.js`
- Noticed while re-capturing for the Ready-for-QA-only boards. Everything above dated 23 Sep was taken on `index-CPjC9Hwo.js` (12:30–13:30 IST). Captures 25–29 are on CH4p1H5S.
- ZP-4344: /robots.txt head (25) and tail (29) — tail is `User-agent: * / Disallow:`, no `Sitemap:` line (innerText regex /^Sitemap:/m → false); /sitemap.xml renders the SPA shell (the 2FA prompt) → 26.
- ZP-4042: browser GET /api/reporting/history?limit=5 → 500 `{"error":"internal_error","trace_id":"8ebb82e9b2374d6db8c0db960f5c66f0"}` → 27 (real pixels of the failing call); /maintenance-portal/reports → Access Denied on CH4p1H5S → 28 (also ZP-4061 / ZP-4138 route half).
- Corrections from the refuter pass on the draft boards: ZP-3919 was NOT "never tested" — it was tested 22 Sep (email+password only, capture 04 of the recheck) and re-run today because PR #1513 merged 01:45; never-tested = FOUR (4151, 4181, 4326, 4344). ZP-4030 relabelled Defect (two acceptance lines fail: raw parse error for invalid ids; AASA lists a personal tutorial app id). ZP-4045/4060 relabelled Partial (bars render with zeros; behaviour unexercised). ZP-4082 relabelled "not run" (ticket says prod-only; Planned Work → Release not attempted). ZP-4062's positive evidence is 21 Sep (BOaMecwk). ZP-4039/4112/4167 bases split by build.

## Correction to my own labels above (refuter round 2)
- "Day five" / "fifth day" for ZP-4042 is not evidenced: probes exist for 14, 21, 22 and 23 Sep only (no 19/20 Sep record). Correct statement: 500 on every probe since first recorded 14 Sep, six bundles (DDSq5pRr, BV-phiFE, BOaMecwk, C9NJAR1x, CPjC9Hwo, CH4p1H5S). Today's traces: ba98b18d…, c73dfb76… (CPjC9Hwo), 8ebb82e9… (CH4p1H5S, in-browser capture 27).
- ZP-4171's Subtypes column and ZP-4109's flat All-methods grid are 21 Sep (BOaMecwk) evidence; the 22 Sep sweep saw only the Engineering core-attributes block and the By class / All methods toggle respectively.
