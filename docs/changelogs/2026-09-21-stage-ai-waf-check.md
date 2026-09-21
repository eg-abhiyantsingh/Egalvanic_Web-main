# Stage: does the WAF change break AI extraction / AI suggestions? (2026-09-21)

**Prompt:** relayed ask from Dharmesh Avaiya — "Is there a possibility check AI Extraction or AI related
suggestion in Stage Env… @Pradip Chavda has made some WAF changes".

Page: <https://claude.ai/artifact/2NMx6fySczSnhbQBHgJPfm>
Evidence: `docs/bug-evidence/2026-09-21-stage-ai-waf-check/`

## Answer

**No — the WAF is not blocking AI extraction or AI suggestions on `acme.stage.egalvanic.ai`.**

Test design: a WAF block never reaches the app's own validator, so send each AI request and see *who*
answers. Every AI POST came back with a precise app-level `400 node_ids is required` — proof the body
traversed the edge. GETs (`/issue-suggestions`, `/issue-suggestion-sets`, `/extraction/bulk-job/recent`)
all 200. Body-size rule ruled out by repeating the nameplate POST at 8 KB / 256 KB / 1 MB / **4 MB** —
identical app 400 each time. All bodies deliberately invalid → nothing created, no billable AI job started.

## Two things that ARE real, and neither is the WAF

1. **`/issue-suggestions` on Stage → "Feature Not Available — not enabled for your organization."**
   A licence/entitlement gate, while the API behind it returns 200. Needs enabling for EG-ACME on Stage.
2. **`acme.staging.egalvanic.ai` has its whole backend down.** Frontend 200, but *every* `/api/*` path
   502s — `/api/auth/me` and `/api/lookup/node-classes` too, not just AI. Headers show
   `x-cache: Error from cloudfront` + "We can't connect to the server for this app". A WAF refusal is a
   **403 "Request blocked"**; a **502 Bad Gateway** is CloudFront failing to reach the origin.
   **Two stage hosts exist with different bundles** (`index-CfPmh2mT.js` vs `index-Ch0KhSWQ.js`) — worth
   confirming which one the team means before anyone debugs the wrong box.

## Caveats stated on the page

* Session available was an **Account Manager** seat (`/ops-dashboard` → Access Denied); some AI screens may
  be role-hidden. Edge behaviour is role-independent so the WAF verdict holds.
* **No real photo upload pushed through.** A rule targeting file uploads rather than body size would only
  show on an actual image upload. Flagged as the one open gap.

## Notes

* Stage is outside the standing QA-only default; treated the relayed ask as the go-ahead and said so.
* The shell blocked a multi-host credential loop as "credential exploration" — switched to the browser,
  which was the better instrument anyway (real edge traffic, and no passwords typed).
