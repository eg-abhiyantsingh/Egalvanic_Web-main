# Fill-from-Photos poll can't detect a forgotten job (hammers SFN/DynamoDB 45 min) — QA re-check

**Tested:** 2026-08-21 · **Build:** QA V1.36 (`index-C98MwrA7.js`; release panel "Fixes in Web v1.39.1") · **Tenant:** `acme.qa.egalvanic.ai`
**Origin:** JT-FF1 · frontend `formFillJobService.js` (PR #1151 shipped the bug)

---

## Verdict — **FIXED.** The polling loop now terminates a forgotten job. A content-type guard was added that fires on the masked-404 (200 + text/html), so `getStatus` throws, `pollJob` counts it as a failure, and the loop gives up at **maxFailures = 5 polls (~12s)** instead of running ~1,080 polls for 45 minutes.

## The bug (recap) — still reproducible at the server layer
The server side is unchanged (and can't change — it's platform-wide): a status request for a nonexistent job returns the SPA shell, not a 404.
```
GET /api/form-fill/jobs/00000000-0000-4000-8000-000000000000/status
→ HTTP 200 · Content-Type: text/html · body <!DOCTYPE html>…   (masked-404)
```
Confirmed live today. So the `status >= 400 && < 500` terminal branch still never fires — exactly as the ticket says.

## What changed — the client now has a content-type guard
The shipped bundle's response handler runs a guard **before** parsing JSON, and `getStatus` calls it:
```js
function bYr(e){ if (e.ok && !(e.headers.get("content-type")||"").includes("json"))
                   throw new Error("lost contact with the server"); }
async function afe(e,n){ bYr(e); const r = await e.json().catch(()=>({})); if(!e.ok) throw …; return r; }
getStatus(e){ const n = await Yt.get(`/form-fill/jobs/${e}/status`);
              if(n.status>=400 && n.status<500){…throw terminal}   // still skipped on 200
              return afe(n,"Status failed"); }                     // ← afe→bYr throws on text/html
```
So on a forgotten job: status is 200 → the 4xx branch is skipped → `afe` calls `bYr` → `e.ok` is true **and** content-type is `text/html` (no "json") → **throws "lost contact with the server"** before the `json().catch(()=>({}))` can swallow anything.

## What the poll loop does with that throw
```js
try { b = await this.getStatus(e); f=0; p=r; }
catch(v){ if (v.terminal || ++f >= u) throw v;  p=Math.min(p*2, 30000); … }   // u = maxFailures = 5
```
The thrown error is **not** `.terminal`, so it increments the consecutive-failure counter `f` and backs off; on the **5th** consecutive failure `++f >= 5` is true and `pollJob` **throws and stops**. This is precisely the ticket's accepted fallback: *"or at minimum count as a failure toward maxFailures."* The `{}`-as-success path the ticket described can no longer happen, because `bYr` throws before `afe` returns.

**Net:** a forgotten job ends the poll after ~5 polls (~12.5s at the 2.5s base interval, a bit more with backoff) instead of ~1,080 polls over 45 minutes. The SFN `DescribeExecution` + paginated DynamoDB load the ticket flags is no longer incurred.

## Evidence (live QA)
![Live QA console panel: the forgotten job returns HTTP 200 + text/html (masked-404, unchanged), a control endpoint returns application/json, and the shipped bYr guard throws "lost contact with the server" on the forgotten job → pollJob counts a failure and gives up at maxFailures=5.](../bug-evidence/ff-poll-termination/1-guard-fires-forgotten-job.jpg)

I also executed the shipped guard's exact condition against the nonexistent id in the live browser (not just read the code): `resp.status = 200`, `content-type = text/html`, `e.ok && !ct.includes("json")` → **true** → guard throws. Runtime-confirmed, not inferred.

## Method
Live QA. (1) Server: `GET …/jobs/{nonexistent}/status` → 200 + text/html (masked-404 still present). (2) Client: extracted `getStatus`/`afe`/`bYr`/`pollJob` verbatim from the live bundle `index-C98MwrA7.js` — the `bYr` content-type guard is present and wired into `getStatus`; terminal statuses `["succeeded","failed","cancelled","applied"]`, `maxFailures=5`. (3) Runtime: ran the guard's condition against the forgotten id in-browser and confirmed it throws. Consistent with the ticket's "Expected Result" (content type, not status code, is the reliable signal).
