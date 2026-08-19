# Cross-tenant CREATE IDOR on the offline/queued path · Jira ticket (ready to file)

Full QA verdict: `docs/bug-reports/2026-08-18-SECURITY-cross-tenant-offline-idor-CREATE-verdict.md`

---

## Title
[Backend/Security] IDOR: offline/queued CREATE bypasses tenant ownership — a Company-A user plants rows under a Company-B `sld_id` (the offline-path IDOR fix closes update/delete but not create)

## Environment
* Environment: **QA** (`acme.qa.egalvanic.ai` attacker / `demo.qa.egalvanic.ai` victim)
* Platform: **Web/Backend** — mutation middleware + queued-mutation worker
* Build: QA **V1.36** · 2026-08-18
* Auth: attacker is a plain customer admin, `is_eg_admin:false`

## Severity / Priority
**Medium** (server-side authorization hardening). It is a genuine cross-tenant write authz gap and the server should enforce it — but it is **not reachable by normal product use**: it needs an authenticated user to tamper the request *and* to know a foreign `sld_id` (obtainable by chaining the still-open by-scope read IDORs). Not a normal-user, click-to-reproduce bug, so not High on a user-facing scale; treat as a real authz gap to close, weighted by your threat model. (Escalate if foreign `sld_id`s are easily enumerable cross-tenant.)

## In one line
The **server** accepts a task-create request that names **another company's `sld_id`** and files the row under that other company — on **both** the offline/queued path *and* the online `x-direct-write:true` path. It is a **server-side authorization gap** (missing ownership check on create).

## Who can trigger it — and who can't (threat model — read this first)
- **A normal user clicking in the web app CANNOT do this.** The task-create form's site picker only lists *your own* company's sites (it's fed by `GET /company/{yourCompany}/slds`), so you can't choose a foreign site. Confirmed: demo's `sld_id`s are absent from acme's site list.
- **It requires request tampering.** An authenticated Company-A user who swaps the `sld_id` in the create request (via browser devtools / a proxy / direct API call) to a Company-B `sld_id` gets the row filed under Company B. Not a point-and-click bug.
- So this is a **defense-in-depth / server-side-authz** issue: the server must reject a create whose `sld_id` isn't the caller's, because "the client won't send it" is not a valid control (an authenticated attacker controls their client). It is **not** "any user can plant data by using the product normally."

## The visible consequence (this part IS real in the UI)
Once such a tampered create is applied, the row is genuinely Company B's data: log into **`https://demo.qa.egalvanic.ai`** as a demo user (`shubham.goswami@egalvanic.com` / `Shubham@123`) → **Tasks** → the task **"XT-CREATE-2 cross-tenant probe — QA delete me"** (Pending) is there, and acme can't see it. (Real screenshots attached.)

## Summary
The offline-path ownership fix (follow-up to ZP-3563) rejects cross-tenant **update/delete** because the canonical row exists at apply time and its owner is checked. A **create** has no canonical row yet, so the fix's "row absent → not-yet-applied → allow" carve-out (added so legitimate offline create-then-update isn't false-blocked) fires — and the create handler does **not** resolve the payload's `sld_id → SLD.company_id`. Result: a Company-A user creates rows under a Company-B-owned `sld_id`, and the row belongs to Company B (visible only in Company B's tenant).

## Preconditions
1. Two tenants; attacker authenticated to tenant A (acme, `is_eg_admin:false`).
2. A `sld_id` owned by tenant B. (In QA: demo SLD `24eb08b1-bb88-4f47-8ad0-f5b09326cf8d`. In the wild an attacker can enumerate/guess or obtain one via the still-open by-scope read leaks.)

## Steps to Reproduce
1. Log in to `acme.qa.egalvanic.ai`, get the acme bearer token.
2. Send the create on the **offline/queued path — omit the `x-direct-write` header** — with a client-generated id and the **victim's** `sld_id`:
   ```
   POST https://acme.qa.egalvanic.ai/api/task/create
   Authorization: Bearer <ACME token>
   Content-Type: application/json
   { "id":"<new uuid>", "sld_id":"24eb08b1-…(DEMO SLD)", "task_type":"General",
     "completed":false, "title":"XT-CREATE — QA delete me" }
   ```
   → HTTP 200 `{"_mutation":{"status":"received"}}` (queued).
3. Wait ~10–15s for the worker to apply.
4. Read the row back **tenant-scoped**:
   * as **demo** (victim token): `GET https://demo.qa.egalvanic.ai/api/task/<new uuid>` → **HTTP 200 JSON**, your title, `sld_id` = the demo SLD.
   * as **acme** (attacker token): `GET https://acme.qa.egalvanic.ai/api/task/<new uuid>` → 200 + SPA-shell (masked-404).

## Actual Result
The task is created and stored under the **victim** tenant's SLD (readable only from the victim session; masked-404 in the attacker's). Reproduced on both demo SLDs. The attacker has written into another company's site.

## Expected Result
The queued create is rejected (404, matching the ticket's stated intent for cross-tenant writes) and never applied. Ownership must be enforced for creates too: when the canonical row is absent, resolve the create payload's `sld_id → SLD.company_id` (and `get_company_id_for_quote` for quotes) and reject if it is not the caller's authoritative (X-Subdomain-resolved) company. The absent→allow carve-out should apply only when no ownable parent is referenced, not to every create.

## Scope of the offline-path fix as verified (context for the assignee)
* Cross-tenant **UPDATE** (task/ir_session/quote) and **DELETE** (task): **not applied** — the "present-but-foreign → reject" branch works. ✅
* Legit same-tenant create-then-update (fully queued): applies — no PR-#968 regression. ✅
* X-Subdomain spoof (acme token + `X-Subdomain: demo`): **422 refused**. ✅
* Cross-tenant **CREATE**: **applies → this ticket.** 🔴

## Frontend impact (demonstrated)
Logged into `demo.qa.egalvanic.ai` as a real demo employee (`shubham.goswami@egalvanic.com`, company `93611164`), the acme-planted task **appears in demo's own Tasks page** and is counted in the **PENDING: 1** tile. The victim sees a task they never created; being a real row, it also rides demo's reports, SLD sync, and mobile client. Not just an API artifact.

## Attachments
![Frontend proof (real screenshot) — the task detail in demo's live web UI showing the full title "XT-CREATE-2 cross-tenant probe — QA delete me", Status Pending, logged in as demo employee shubham.goswami](../bug-evidence/cross-tenant-offline-idor/idor-frontend-demo-task-detail-REAL.jpg)

![Frontend proof (real screenshot) — demo's Tasks list with the planted task and PENDING = 1, under demo user shubham.goswami](../bug-evidence/cross-tenant-offline-idor/idor-frontend-demo-tasks-REAL.jpg)

![Live cross-tenant CREATE — acme plants a task under demo's SLD via the queued path; readable only from the demo tenant](../bug-evidence/cross-tenant-offline-idor/idor-cross-tenant-CREATE-live.png)

![Update/delete matrix — cross-tenant blocked (with same-tenant controls that applied); create is the gap](../bug-evidence/cross-tenant-offline-idor/idor-offline-verify-matrix.png)

**Note for the assignee:** fix belongs in the same place as the update/delete gate (apply-time worker/handler). The check that exists for "row present" must also run for "row absent but payload names an ownable parent (`sld_id`/opportunity/quote)". Also worth confirming rejected cross-tenant mutations are terminal (PERMANENT_NOT_FOUND), not retried/parked, and closing the edge/issue coverage gap.
