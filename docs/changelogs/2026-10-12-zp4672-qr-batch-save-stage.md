# ZP-4672 (Web v2.3.1 hotfix): QR Labels "Save codes" batch save, verified on stage

- **Ticket:** ZP-4672 "[Web] QR Labels: "Save codes" sends one request per asset; no batch endpoint, so a 50-asset batch is 50 requests" (Krunal).
- **Date:** 12 Oct 2026, 15:28–15:50 IST. **Env:** acme.stage.egalvanic.ai, build `index-Eltoc33D.js` (QR code in chunk `index-BHttju0v.js`). Seat `+acme@` (Super Admin).
- **Status:** moved Ready for QA → **In QA** at 15:28 IST (from abhiyant's Jira account, previous session).
  **Not yet moved to Ready to Release, and no comment posted:** the earlier session hit its usage limit mid-run. This session's
  Jira connector signs in as Shubham Goswami (712020:85fe…), and no browser here is signed in to Jira as abhiyant
  (Playwright: 401 on `/rest/api/3/myself`). Owner rule: status and comments only from abhiyant.singh@. Comment text is ready in
  `docs/bug-evidence/2026-10-12-zp4672-stage/ZP-4672-comment.md`.

## Result: PASS (all 4 QA Review items)

| QA Review item | What I did | Result |
|---|---|---|
| 1. Save 1, 10, 50 assets: one request each, time grows slowly | "test site" (1,907 assets, none coded). 1 via the row's own **Add code** button; 10 and 50 via ticked rows → **Edit codes** → Start number | **1 request each time**, `PUT /api/qr-labels/codes`, **0** `node/update`. Request time 0.68 s (1) · 0.95 s (10) · 2.17 s (50). Toasts "1 QR code saved" / "10 QR codes saved" / "50 QR codes saved" |
| 1b. Larger batch | site "test" (83 assets, 80 uncoded): Select all → Start number → Save | **1 request carrying all 80 codes**, 0 `node/update`, "80 QR codes saved" |
| 2. Every code persisted, none dropped | Reopened the editor (3-asset run); fresh page reload (1/10/50 run) | 3/3 codes back in the editor. "No QR code yet" list went **1,907 → 1,846** after reload (exactly −61). Search `523522` finds all 50 (TS-52352200…249) |
| 3. Double-click Save | Double-clicked Save codes on a 3-asset batch | **1** request, no duplicate write, three distinct codes |
| 4. "Change existing QR codes?" pop-up | Changed one existing code in a 3-asset batch | Pop-up appears (shows old → new code), closes after **Change codes**, then 1 batch request |

Response contract seen live: `{"success":true,"saved":[{node_id,qr_code}…],"failed":[],"unchanged":0}` (partial-failure
shape is there; a real partial failure was not forced).

**Before the fix (QA, 6 Oct, `index-CfC0Y-hD.js`):** 3 assets = 3 × `POST /api/node/update/<asset>`, 0.8 s each.

## Note for the developer (not a failure)
Server time grows about **30 ms per code** (0.68 s → 0.95 s → 2.17 s for 1 → 10 → 50). The client sends up to 1,000 codes per
request, so a 1,000-code save would take roughly 30 s in one request if that rate holds, which is near common gateway time-outs.
**Not tested** (it would code 1,000 assets on the shared stage "test site"). Worth one check once ZP-4671 makes 1,000-asset
selections normal.

## Not covered
- Real partial failure (`failed[]` non-empty) not forced.
- Mobile apps still use per-asset `node/update` (ticket says keep that contract). Not in scope for web.
- Prod: still pre-fix; re-check after the v2.3.1 release.

## Test data (stage)
- "Test uhhujh": 3 codes TU-A496561..63, then GEN 3 → TU-CHG0556 / Panel 1 → TU-DBL0556.
- "test": 80 codes T-50001…T-50080.
- "test site": TS-Q523521 (1), TS-52352100…109 (10), TS-52352200…249 (50).

## Evidence
`docs/bug-evidence/2026-10-12-zp4672-stage/`: 1–8 (3-asset, 80-asset, pop-up, double-click), 9–15 (1/10/50 sizes + reload/search).
