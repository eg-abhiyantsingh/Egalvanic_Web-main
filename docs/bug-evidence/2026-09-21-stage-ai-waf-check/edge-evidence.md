# Stage edge check — AI extraction / AI suggestions (2026-09-21)

Asked by Dharmesh Avaiya after Pradip Chavda's WAF changes: does AI Extraction / AI suggestion
still work on Stage?

## acme.stage.egalvanic.ai — edge healthy, AI reachable

Signed-in browser session (Account Manager seat, EG-ACME), bundle `index-CfPmh2mT.js`.

| Request | Result | Reading |
|---|---|---|
| GET `/api/issue-suggestions` | 200 JSON `{suggestions: []}` | reached app |
| GET `/api/issue-suggestion-sets` | 200 JSON, 16 classes | reached app, real data |
| GET `/api/extraction/bulk-job/recent` | 200 JSON `{jobs: []}` | reached app |
| POST `/api/extraction/extract-nameplate-data` | 400 `node_ids is required` | **app validator answered** |
| POST `/api/extraction/bulk-job/submit` | 400 `node_ids required` | app validator answered |
| POST `/api/extraction/classify-assets` | 400 `node_ids is required` | app validator answered |
| POST `/api/issue-suggestions/accept` | 400 `sld_id and node_ids required` | app validator answered |

A WAF block never reaches the application's own validator. Getting a precise `node_ids is required`
proves the request body traversed the edge intact.

**Body-size rule check** (photo extraction sends large payloads): JSON bodies of 8 KB, 256 KB, 1 MB and
**4 MB** all returned the same app-level 400. No size rule is tripping.

All POSTs used deliberately invalid bodies, so nothing was created and **no AI job was started**
(bulk extraction jobs are billable).

Unauthenticated contrast: POST to the same endpoints returns `401 {"error":"No authorization provided"}`
— the app's auth layer, not an edge page.

## acme.staging.egalvanic.ai — backend down, and it is NOT the WAF

| Request | Result |
|---|---|
| GET `/` | 200 (frontend serves fine) |
| POST `/api/extraction/extract-nameplate-data` | **502** |
| POST `/api/extraction/bulk-job/submit` | **502** |
| GET `/api/auth/me` | **502** |
| GET `/api/lookup/node-classes` | **502** |

Response headers: `x-cache: Error from cloudfront`, `via: … .cloudfront.net`, body
`<TITLE>ERROR: The request could not be satisfied</TITLE> … 502 Bad Gateway … We can't connect to the
server for this app`.

**Every** `/api/*` path 502s, not just the AI ones — so this is CloudFront failing to reach the origin,
not a WAF rule. A WAF refusal returns 403 with a "Request blocked" page; this is 502 Bad Gateway.

## The AI gate that IS real on stage

`/issue-suggestions` renders **"Feature Not Available — This feature is not enabled for your organization.
Please contact your administrator or Egalvanic support to get access."** — a licence / entitlement gate,
while the underlying API answers 200. Unrelated to the WAF.

Screenshot: `stage-issue-suggestions-feature-not-available.jpg`

## Caveats

* The available Stage session is an **Account Manager** seat (`/ops-dashboard` → Access Denied), so some
  AI surfaces may additionally be role-hidden. Edge behaviour is role-independent, so the WAF conclusion
  holds regardless.
* Multipart upload not conclusively exercised — the endpoint tried rejects multipart with an app-level
  415. If a WAF rule targets file uploads specifically, it would need a real photo upload through the UI
  on a seat that can reach it.
